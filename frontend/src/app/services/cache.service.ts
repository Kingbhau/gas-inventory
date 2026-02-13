import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { CacheConfig } from '../models/cache-config.model';

interface CacheEntry<T> {
  data: T;
  timestamp: number;
  ttl: number;
}

/**
 * Client-side caching service for reference data
 * Reduces unnecessary API calls by caching customers, variants, suppliers, etc.
 * 
 * Strategies:
 * - memory: Cached only during session, lost on page refresh
 * - session: Stored in sessionStorage, persists during session
 * - local: Stored in localStorage, persists across sessions
 */
@Injectable({
  providedIn: 'root'
})
export class CacheService {
  private memoryCache = new Map<string, CacheEntry<any>>();
  private cacheSubject = new BehaviorSubject<string | null>(null);
  public cacheUpdated$ = this.cacheSubject.asObservable();

  constructor() {
    this.initializeMemoryCache();
  }

  /**
   * Initialize memory cache from sessionStorage (session persistence)
   */
  private initializeMemoryCache(): void {
    try {
      const sessionCacheStr = sessionStorage.getItem('_app_cache');
      if (sessionCacheStr) {
        const sessionCache = JSON.parse(sessionCacheStr);
        Object.entries(sessionCache).forEach(([key, entry]: [string, any]) => {
          this.memoryCache.set(key, entry);
        });
      }
    } catch (e) {
    }
  }

  /**
   * Get cached data if valid, otherwise return null
   */
  get<T>(key: string): T | null {
    const entry = this.memoryCache.get(key);
    
    if (!entry) {
      return null;
    }

    // Check if cache has expired
    if (entry.ttl > 0) {
      const now = Date.now();
      const age = now - entry.timestamp;
      if (age > entry.ttl) {
        this.memoryCache.delete(key);
        this.persistCache();
        return null;
      }
    }

    return entry.data as T;
  }

  /**
   * Set cache value
   * @param key Cache key
   * @param data Data to cache
   * @param config Cache configuration (ttl in ms, storage strategy)
   */
  set<T>(key: string, data: T, config: CacheConfig = {}): void {
    const { ttl = 0, strategy = 'memory' } = config;

    const entry: CacheEntry<T> = {
      data,
      timestamp: Date.now(),
      ttl
    };

    this.memoryCache.set(key, entry);
    this.cacheSubject.next(key);

    if (strategy === 'session' || strategy === 'local') {
      this.persistCache();
    }
  }

  /**
   * Get or load from observable
   * Returns cached value if valid, otherwise subscribes to observable and caches result
   */
  getOrLoad<T>(
    key: string,
    loader: () => Observable<T>,
    config: CacheConfig = {}
  ): Observable<T> {
    const cached = this.get<T>(key);
    if (cached !== null) {
      return of(cached);
    }

    return new Observable(observer => {
      loader().subscribe({
        next: (data: T) => {
          this.set(key, data, config);
          observer.next(data);
          observer.complete();
        },
        error: (err) => observer.error(err)
      });
    });
  }

  /**
   * Clear specific cache entry
   */
  invalidate(key: string): void {
    this.memoryCache.delete(key);
    this.persistCache();
    this.cacheSubject.next(null);
  }

  /**
   * Clear all cache entries
   */
  clear(): void {
    this.memoryCache.clear();
    sessionStorage.removeItem('_app_cache');
    localStorage.removeItem('_app_cache');
    this.cacheSubject.next(null);
  }

  /**
   * Clear cache entries matching pattern
   */
  invalidatePattern(pattern: string | RegExp): void {
    const regex = typeof pattern === 'string' ? new RegExp(pattern) : pattern;
    const keysToDelete: string[] = [];

    this.memoryCache.forEach((_, key) => {
      if (regex.test(key)) {
        keysToDelete.push(key);
      }
    });

    keysToDelete.forEach(key => this.memoryCache.delete(key));
    this.persistCache();
    this.cacheSubject.next(null);
  }

  /**
   * Get cache statistics
   */
  getStats(): { size: number; keys: string[] } {
    const keys = Array.from(this.memoryCache.keys());
    return { size: this.memoryCache.size, keys };
  }

  /**
   * Persist cache to storage
   */
  private persistCache(): void {
    try {
      const cacheObj: Record<string, any> = {};
      this.memoryCache.forEach((entry, key) => {
        cacheObj[key] = entry;
      });
      sessionStorage.setItem('_app_cache', JSON.stringify(cacheObj));
    } catch (e) {
    }
  }
}

/**
 * Predefined cache keys for common reference data
 */
export const CACHE_KEYS = {
  CUSTOMERS: 'customers_all',
  VARIANTS: 'variants_all',
  SUPPLIERS: 'suppliers_all',
  WAREHOUSES: 'warehouses_all',
  USERS: 'users_all',
  PAYMENT_MODES: 'payment_modes_all',
  BANK_ACCOUNTS: 'bank_accounts_all',
  EXPENSE_CATEGORIES: 'expense_categories_all',
  CUSTOMER_PRICES: 'customer_prices_',
  MONTHLY_PRICES: 'monthly_prices_',
  BUSINESS_INFO: 'business_info'
};

/**
 * Cache configuration presets
 */
export const CACHE_CONFIG = {
  REFERENCE_DATA: { ttl: 15 * 60 * 1000, strategy: 'session' as const }, // 15 minutes
  CUSTOMER_DATA: { ttl: 10 * 60 * 1000, strategy: 'session' as const }, // 10 minutes
  PRICE_DATA: { ttl: 30 * 60 * 1000, strategy: 'session' as const }, // 30 minutes
  BUSINESS_INFO: { ttl: 60 * 60 * 1000, strategy: 'session' as const }, // 1 hour
  SESSION: { ttl: 0, strategy: 'session' as const } // Until page refresh
};
