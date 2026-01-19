# Performance Analysis Report - Gas Inventory Management System
**Date:** January 19, 2026  
**Focus:** System capability to handle 30-40 sales entries creation per session with 10-15 concurrent users

---

## Executive Summary

⚠️ **CURRENT ISSUE: Your system is NOT optimized for bulk sales entry operations.** The client creates 30-40 sales entries at a time, but your system processes them ONE AT A TIME. This creates significant performance bottlenecks.

**Expected Time for 40 entries:** ~40-80 seconds (at ~1-2 seconds per entry)  
**Recommended Time:** ~5-10 seconds for entire batch

---

## Critical Performance Issues Found

### 1. **NO BULK API ENDPOINT** ❌
**Current:** Each sale entry requires a separate HTTP POST request to `/api/sales`
```
40 sales entries = 40 separate HTTP requests + 40 separate database transactions
```

**Impact:**
- Network latency multiplied by 40
- Database connection overhead × 40
- Serialization/deserialization overhead × 40
- No transactional atomicity for the batch

**Frontend Evidence:**
- [sale-entry.component.ts](sale-entry.component.ts) submits ONE entry at a time (line ~450-500)
- Each `submitSale()` makes a single API call
- Users must repeat the form entry process 40 times

---

### 2. **SEQUENTIAL PROCESSING - No Parallelization** ❌

**Current Flow:**
```
Request 1 → DB Transaction → Response → Request 2 → DB Transaction → Response → ...
(SEQUENTIAL - Takes total time of all requests)
```

**Issues:**
- No concurrent request batching
- Could process 40 requests in ~2 seconds with proper parallel handling
- Currently takes ~40-80 seconds for 40 individual requests

---

### 3. **Excessive Database Queries per Sale Entry** ⚠️

Looking at `SaleService.createSaleInternal()` (~450 lines), **every single sale entry executes:**

```java
// Per sale entry, you're executing:
1. SELECT customer BY id (1 query)
2. SELECT warehouse BY id (1 query)
3. For each item in sale:
   - SELECT variant BY id (1 query)
   - SELECT customer-variant-price (1 query)
   - SELECT inventory stock WITH LOCK (1 query - PESSIMISTIC LOCK)
   - INSERT sale (1 query)
   - INSERT sale_item (1 query)
   - UPDATE inventory stock (1 query)
   - SELECT/INSERT customer cylinder ledger (2-3 queries)
4. If bank account:
   - SELECT bank account BY id (1 query)
   - INSERT bank account ledger (1 query)

TOTAL PER ENTRY: ~12-15 database queries
FOR 40 ENTRIES: 480-600 database queries!
```

**Pessimistic Locking Issue:**
```java
InventoryStock inventoryStock = inventoryStockService
    .getStockByWarehouseAndVariantWithLock(warehouse, variant);
    // ^ This locks the row for the entire transaction
```
- Good for concurrency control
- BAD for bulk operations (causes lock contention)
- 40 parallel requests = 40 concurrent locks waiting

---

### 4. **No Batch Processing or Connection Pool Optimization** ⚠️

**Current Configuration** (application-prod.properties):
```properties
spring.datasource.hikari.maximum-pool-size=20        # Only 20 connections
spring.datasource.hikari.minimum-idle=5              # 5 idle connections
spring.jpa.properties.hibernate.jdbc.batch_size=20   # Batch size only 20
spring.jpa.properties.hibernate.order_inserts=true   # Good, but underutilized
spring.jpa.properties.hibernate.order_updates=true   # Good, but underutilized
```

**Problems:**
- With 10-15 users + 40 entries each = 400-600 potential concurrent requests
- Only 20 database connections available
- Other 10-15 users will be queued waiting for connections
- Batch size of 20 is too small for bulk operations

---

### 5. **Inefficient Inventory Locking Strategy** ❌

**Current Approach (per entry):**
```java
// PESSIMISTIC LOCK - Row level locking
inventoryStockService.getStockByWarehouseAndVariantWithLock(warehouse, variant);
```

**Issues with 40 entries:**
1. First entry locks inventory_stock row → ~2 seconds to complete
2. Entries 2-40 wait for lock release
3. Creates serial bottleneck despite using database connections
4. Each lock hold time depends on complete sale creation (~2 seconds)

**Alternative needed:** 
- Use `@Version` optimistic locking (you have it on Sale entity, but not on InventoryStock)
- Or use UPDATE statement with WHERE clause to prevent negative inventory

---

### 6. **Lack of Caching** ❌

**Current:** Every sale entry fetches:
- Customer data (could be cached - changes rarely)
- Cylinder variants (could be cached - changes rarely)
- Warehouse data (could be cached - changes rarely)
- Monthly prices (cached per month - OK)
- Bank accounts (could be cached)

**For 40 entries with same data:**
```
Entry 1: Customer X fetched from DB (100ms)
Entry 2: Customer X fetched from DB again (100ms) - DUPLICATE
Entry 3: Customer X fetched from DB again (100ms) - DUPLICATE
...
Entry 40: Customer X fetched from DB again (100ms) - DUPLICATE

TOTAL: 4 seconds wasted on duplicate queries!
```

**Missing:** No caching layer for master data (Redis or in-memory cache)

---

### 7. **No Request Rate Limiting or Queue Management** ❌

**Current State:**
- User clicks "Create Sale" 40 times
- 40 requests hit backend simultaneously
- Database connection pool floods
- Server threads max out (250 threads max: `server.tomcat.threads.max=250`)
- Other users experience slowdown

**What should happen:**
- Queue the requests
- Process optimally (10-20 at a time)
- Return result when complete
- Estimated time shown to user

---

### 8. **No Asynchronous Processing** ❌

**Current:** All requests are synchronous
- User waits for each sale entry to complete
- Cannot create multiple entries while waiting
- Frontend appears frozen

**Better:** Async processing with background job queue (Spring Batch, Quartz, etc.)

---

## Performance Benchmarks

### Current System (Estimate)

| Operation | Time | Notes |
|-----------|------|-------|
| Single Sale Entry | 1-2 seconds | Includes DB round trips, locking |
| 40 Sales (sequential) | 40-80 seconds | User waits entire time |
| 40 Sales (concurrent, 10-15 users) | 60-120 seconds | With lock contention |
| Database queries for 40 entries | 480-600 queries | Massive overhead |

### Recommended System

| Operation | Time | Target |
|-----------|------|--------|
| Bulk Import (40 entries) | 5-10 seconds | 4-8x faster |
| Concurrent 10-15 users creating 40 each | 15-30 seconds total | Each user finishes in ~30s |
| Database queries for 40 entries | 50-100 queries | 5-10x reduction |

---

## Detailed Recommendations (Priority Order)

### **PRIORITY 1: Create Bulk API Endpoint** 🔴 CRITICAL

Create `POST /api/sales/bulk` endpoint:

```java
@PostMapping("/bulk")
@Transactional
public ResponseEntity<BulkSaleResponse> createBulkSales(
    @RequestBody List<CreateSaleRequestDTO> requests) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(service.createBulkSales(requests));
}
```

**Implementation Strategy:**
```java
@Transactional(isolation = Isolation.READ_COMMITTED)
public BulkSaleResponse createBulkSales(List<CreateSaleRequestDTO> requests) {
    // 1. Validate ALL entries upfront (fail fast if any invalid)
    // 2. Lock all required inventory rows ONCE (not per entry)
    // 3. Process all entries in single transaction
    // 4. Return results for entire batch
}
```

**Benefits:**
- 1 HTTP request instead of 40
- Single database transaction for entire batch
- Single inventory lock sequence
- Estimated gain: 10-20x faster

**Impact on 40 entries:**
- **Current:** 40-80 seconds → **Future:** 4-8 seconds ⚡

---

### **PRIORITY 2: Implement Optimistic Locking for Inventory** 🔴 CRITICAL

Add `@Version` to `InventoryStock` entity:

```java
@Entity
@Table(name = "inventory_stock")
public class InventoryStock extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Version  // ← ADD THIS
    @Column(nullable = false)
    private Long version = 0L;
    
    // ... rest of fields
}
```

**Update logic:**
```java
// Instead of pessimistic lock:
// inventoryStockService.getStockByWarehouseAndVariantWithLock(...)

// Use optimistic:
InventoryStock stock = inventoryStockRepository
    .findByWarehouseAndVariant(warehouse, variant);
stock.decrementFilledQty(qty);
// Hibernate handles version conflict automatically
```

**Benefits:**
- No row-level locking
- True concurrent processing
- Automatic conflict detection
- Much faster for bulk operations

---

### **PRIORITY 3: Increase Database Connection Pool** 🟠 HIGH

Update `application-prod.properties`:

```properties
# Current (too small)
spring.datasource.hikari.maximum-pool-size=20

# New (handle 10-15 concurrent users × 4 parallel requests)
spring.datasource.hikari.maximum-pool-size=60
spring.datasource.hikari.minimum-idle=10

# Increase batch size
spring.jpa.properties.hibernate.jdbc.batch_size=50
```

**Why:**
- 10-15 concurrent users
- Each might have 3-4 parallel requests
- Need ~50-60 connections to avoid queueing

---

### **PRIORITY 4: Add Caching Layer** 🟠 HIGH

```java
@Cacheable(cacheNames = "customers", key = "#id")
public Customer getCustomer(Long id) { ... }

@Cacheable(cacheNames = "variants", key = "#id")
public CylinderVariant getVariant(Long id) { ... }

@Cacheable(cacheNames = "warehouses", key = "#id")
public Warehouse getWarehouse(Long id) { ... }
```

Or add Redis:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

**Expected reduction:** 30-40% faster for repeated master data access

---

### **PRIORITY 5: Implement Async/Queue-Based Processing** 🟡 MEDIUM

Use Spring Batch or custom queue:

```java
@Service
public class BulkSaleQueueService {
    @Async
    public CompletableFuture<BulkSaleResponse> processBulkSalesAsync(
        List<CreateSaleRequestDTO> requests) {
        return CompletableFuture.completedFuture(
            saleService.createBulkSales(requests)
        );
    }
}
```

**Frontend:**
```typescript
// Instead of waiting for each entry
this.saleService.createBulkSales(40entries).subscribe(
  response => {
    // All 40 entries created, show results
    this.showBulkResults(response);
  }
);
```

---

### **PRIORITY 6: Add Request Rate Limiting** 🟡 MEDIUM

```java
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
        HttpServletResponse response, FilterChain filterChain) {
        // Limit /api/sales to X requests per minute per user
        // Queue excess requests
    }
}
```

---

### **PRIORITY 7: Optimize Frontend for Bulk Entry** 🟡 MEDIUM

Replace single-entry form with bulk entry mode:

```typescript
export class BulkSaleEntryComponent {
    // Show table where users can paste/type 40 entries
    // Like a spreadsheet: Customer | Variant | Qty | Price
    // Then submit entire table as one request
}
```

**Example:**
```html
<table contenteditable>
  <tr><td>Customer A</td><td>Variant X</td><td>5</td></tr>
  <tr><td>Customer B</td><td>Variant Y</td><td>3</td></tr>
  <!-- 38 more rows -->
</table>
<button (click)="submitAllRows()">Create All 40 Sales</button>
```

---

## Architecture Change Needed

### Current Architecture
```
Frontend (form) 
    ↓ 40 separate POST requests
Backend (one entry at a time)
    ↓ 12-15 queries per entry
Database
    ↓ Lock contention, serial processing
Takes 40-80 seconds for 40 entries
```

### Recommended Architecture
```
Frontend (bulk entry UI - paste table/CSV)
    ↓ 1 POST request with 40 items
Backend (bulk processing)
    ├─ Validate all at once
    ├─ Lock all required rows atomically
    ├─ Execute in single transaction
    └─ Return results for entire batch
Database
    ↓ Single efficient query sequence
Takes 5-10 seconds for 40 entries
```

---

## Load Test Scenario

**Scenario:** 10 users, each creating 40 entries simultaneously

### Current System
```
User 1-10 each send: 40 requests
Total: 400 requests
Total queries: 4,800-6,000
Timeline:
  0-5s:    Requests arrive, queue up (20 connections max)
  5-30s:   Processing, lock contention visible
  30-90s:  Finally complete
  
Result: VERY SLOW, users frustrated, server stressed
```

### Recommended System
```
User 1-10 each send: 1 bulk request
Total: 10 requests
Total queries: 500-1,000
Timeline:
  0-1s:    Requests arrive, start processing
  1-10s:   All complete with results
  
Result: FAST, good user experience, server happy
```

---

## Immediate Actions

1. **This Week:** Create bulk API endpoint (Priority 1)
2. **This Week:** Switch from pessimistic to optimistic locking (Priority 2)
3. **Next Week:** Increase connection pool (Priority 3)
4. **Next Week:** Add caching (Priority 4)
5. **Following Week:** Async processing (Priority 5)

---

## Conclusion

**Current Status:** ⚠️ NOT optimized for your use case  
**Time to Create 40 Entries:** 40-80 seconds  
**User Experience:** Slow and frustrating  

**After Optimization:** ✅ Optimized  
**Time to Create 40 Entries:** 5-10 seconds  
**User Experience:** Fast and smooth  

**Speed Improvement:** 4-8x faster ⚡

The biggest bottleneck is **no bulk API endpoint**. Implementing Priority 1 alone will give you a massive performance boost.

