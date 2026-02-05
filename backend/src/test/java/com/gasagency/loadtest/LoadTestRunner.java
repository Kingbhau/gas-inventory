package com.gasagency.loadtest;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;

/**
 * Simple HTTP load test runner for local performance checks.
 *
 * Usage (from backend directory):
 *   mvn -q -DskipTests test-compile
 *   java -cp target/test-classes;target/classes com.gasagency.loadtest.LoadTestRunner \
 *     --base-url=http://localhost:8080 --username=owner --password=owner \
 *     --duration-seconds=60 --concurrency=10 --sleep-ms=0
 */
public class LoadTestRunner {
    private static final String DEFAULT_PAYMENT_MODE = "CASH";
    private static final String DEFAULT_DEPOSIT_PAYMENT_MODE = "CASH";

    public static void main(String[] args) throws Exception {
        Args config = Args.parse(args);
        CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpClient client = HttpClient.newBuilder()
            .cookieHandler(cookieManager)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

        login(client, config);

        DataCache dataCache = loadReferenceData(client, config);
        List<Operation> operations = buildOperations(config, dataCache);
        WeightedPicker picker = new WeightedPicker(operations, config);
        Stats overall = new Stats();
        Map<String, Stats> perEndpoint = new ConcurrentHashMap<>();
        operations.forEach(operation -> perEndpoint.put(operation.name, new Stats()));

        long durationNanos = TimeUnit.SECONDS.toNanos(config.durationSeconds);
        long endAt = System.nanoTime() + durationNanos;
        AtomicLong totalSent = new AtomicLong(0);

        ExecutorService pool = Executors.newFixedThreadPool(config.concurrency);
        for (int i = 0; i < config.concurrency; i++) {
            pool.submit(() -> {
                while (System.nanoTime() < endAt) {
                    long current = totalSent.incrementAndGet();
                    if (config.maxRequests > 0 && current > config.maxRequests) {
                        break;
                    }
                    Operation operation = picker.pick();
                    long start = System.nanoTime();
                    boolean ok = false;
                    try {
                        HttpRequest request = operation.factory.build(config.baseUrl, dataCache, config);
                        if (request == null) {
                            continue;
                        }
                        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                        ok = response.statusCode() >= 200 && response.statusCode() < 300;
                    } catch (Exception ex) {
                        ok = false;
                    } finally {
                        long duration = System.nanoTime() - start;
                        overall.record(duration, ok);
                        perEndpoint.get(operation.name).record(duration, ok);
                    }
                    if (config.sleepMs > 0) {
                        try {
                            Thread.sleep(config.sleepMs);
                        } catch (InterruptedException ignored) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            });
        }

        pool.shutdown();
        pool.awaitTermination(config.durationSeconds + 30, TimeUnit.SECONDS);

        printSummary("OVERALL", overall, config.durationSeconds);
        operations.forEach(operation -> printSummary(operation.name, perEndpoint.get(operation.name), config.durationSeconds));
        picker.printMix();
    }

    private static void login(HttpClient client, Args config) throws Exception {
        String payload = "{\"username\":\"" + config.username + "\",\"password\":\"" + config.password + "\"}";
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(config.baseUrl + "/api/auth/login"))
            .timeout(Duration.ofSeconds(config.requestTimeoutSeconds))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Login failed: status=" + response.statusCode());
        }
    }

    private static void printSummary(String label, Stats stats, long durationSeconds) {
        long count = stats.count.sum();
        if (count == 0) {
            System.out.println(label + " -> no requests recorded");
            return;
        }
        long ok = stats.ok.sum();
        long errors = stats.errors.sum();
        double avgMs = stats.sumNanos.sum() / 1_000_000.0 / count;
        double minMs = stats.min.get() / 1_000_000.0;
        double maxMs = stats.max.get() / 1_000_000.0;
        Percentiles p = stats.percentiles();
        double rps = count / (double) durationSeconds;

        System.out.println("==== " + label + " ====");
        System.out.println("requests=" + count + " ok=" + ok + " errors=" + errors + " rps=" + String.format("%.2f", rps));
        System.out.println("min=" + String.format("%.2f", minMs) + "ms" +
            " avg=" + String.format("%.2f", avgMs) + "ms" +
            " p95=" + String.format("%.2f", p.p95) + "ms" +
            " p99=" + String.format("%.2f", p.p99) + "ms" +
            " max=" + String.format("%.2f", maxMs) + "ms");
    }

    private static final class Operation {
        final String name;
        final RequestFactory factory;

        Operation(String name, RequestFactory factory) {
            this.name = name;
            this.factory = factory;
        }
    }

    private interface RequestFactory {
        HttpRequest build(String baseUrl, DataCache dataCache, Args config);
    }

    private static final class DataCache {
        final List<Long> customerIds;
        final List<Long> customerIdsWithDue;
        final List<Long> warehouseIds;
        final List<Long> variantIds;
        final List<Long> bankAccountIds;
        final List<Long> expenseCategoryIds;

        DataCache(List<Long> customerIds, List<Long> customerIdsWithDue, List<Long> warehouseIds,
                  List<Long> variantIds, List<Long> bankAccountIds, List<Long> expenseCategoryIds) {
            this.customerIds = customerIds;
            this.customerIdsWithDue = customerIdsWithDue;
            this.warehouseIds = warehouseIds;
            this.variantIds = variantIds;
            this.bankAccountIds = bankAccountIds;
            this.expenseCategoryIds = expenseCategoryIds;
        }
    }

    private static List<Operation> buildOperations(Args config, DataCache dataCache) {
        return List.of(
            new Operation("GET_customers_due", (baseUrl, cache, cfg) ->
                simpleGet(baseUrl, "/api/customers/active?page=0&size=10&sortBy=name&direction=ASC&minDueAmount=0.01", cfg)),
            new Operation("GET_sales", (baseUrl, cache, cfg) ->
                simpleGet(baseUrl, "/api/sales?page=0&size=10&sortBy=saleDate&direction=DESC", cfg)),
            new Operation("GET_bank_accounts", (baseUrl, cache, cfg) ->
                simpleGet(baseUrl, "/api/bank-accounts/active/list", cfg)),
            new Operation("GET_payment_modes", (baseUrl, cache, cfg) ->
                simpleGet(baseUrl, "/api/payment-modes/active", cfg)),
            new Operation("POST_sale", LoadTestRunner::buildSaleRequest),
            new Operation("POST_empty_return", LoadTestRunner::buildEmptyReturnRequest),
            new Operation("POST_expense", LoadTestRunner::buildExpenseRequest),
            new Operation("POST_payment", LoadTestRunner::buildPaymentRequest),
            new Operation("POST_bank_deposit", LoadTestRunner::buildBankDepositRequest),
            new Operation("POST_warehouse_transfer", LoadTestRunner::buildWarehouseTransferRequest)
        );
    }

    private static HttpRequest simpleGet(String baseUrl, String path, Args config) {
        return HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + path))
            .method("GET", HttpRequest.BodyPublishers.noBody())
            .timeout(Duration.ofSeconds(config.requestTimeoutSeconds))
            .build();
    }

    private static HttpRequest buildSaleRequest(String baseUrl, DataCache cache, Args config) {
        if (cache.customerIds.isEmpty() || cache.warehouseIds.isEmpty() || cache.variantIds.isEmpty()) {
            return null;
        }
        long customerId = randomFrom(cache.customerIds);
        long warehouseId = randomFrom(cache.warehouseIds);
        long variantId = randomFrom(cache.variantIds);
        long qtyIssued = 1 + ThreadLocalRandom.current().nextInt(3);
        long qtyEmpty = ThreadLocalRandom.current().nextInt(2);
        String payload = "{"
            + "\"customerId\":" + customerId + ","
            + "\"warehouseId\":" + warehouseId + ","
            + "\"amountReceived\":0,"
            + "\"modeOfPayment\":\"" + DEFAULT_PAYMENT_MODE + "\","
            + "\"items\":[{"
            + "\"variantId\":" + variantId + ","
            + "\"qtyIssued\":" + qtyIssued + ","
            + "\"qtyEmptyReceived\":" + qtyEmpty + ","
            + "\"discount\":0"
            + "}]"
            + "}";
        return HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/sales"))
            .timeout(Duration.ofSeconds(config.requestTimeoutSeconds))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();
    }

    private static HttpRequest buildEmptyReturnRequest(String baseUrl, DataCache cache, Args config) {
        if (cache.customerIds.isEmpty() || cache.warehouseIds.isEmpty() || cache.variantIds.isEmpty()) {
            return null;
        }
        long customerId = randomFrom(cache.customerIds);
        long warehouseId = randomFrom(cache.warehouseIds);
        long variantId = randomFrom(cache.variantIds);
        long emptyIn = 1 + ThreadLocalRandom.current().nextInt(2);
        String payload = "{"
            + "\"customerId\":" + customerId + ","
            + "\"warehouseId\":" + warehouseId + ","
            + "\"variantId\":" + variantId + ","
            + "\"transactionDate\":\"" + LocalDate.now() + "\","
            + "\"emptyIn\":" + emptyIn + ","
            + "\"amountReceived\":0"
            + "}";
        return HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/ledger/empty-return"))
            .timeout(Duration.ofSeconds(config.requestTimeoutSeconds))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();
    }

    private static HttpRequest buildExpenseRequest(String baseUrl, DataCache cache, Args config) {
        if (cache.expenseCategoryIds.isEmpty()) {
            return null;
        }
        long categoryId = randomFrom(cache.expenseCategoryIds);
        double amount = 200 + ThreadLocalRandom.current().nextInt(800);
        String payload = "{"
            + "\"description\":\"Load test expense\","
            + "\"amount\":" + String.format("%.2f", amount) + ","
            + "\"categoryId\":" + categoryId + ","
            + "\"expenseDate\":\"" + LocalDate.now() + "\","
            + "\"paymentMode\":\"" + DEFAULT_PAYMENT_MODE + "\""
            + "}";
        return HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/expenses"))
            .timeout(Duration.ofSeconds(config.requestTimeoutSeconds))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();
    }

    private static HttpRequest buildPaymentRequest(String baseUrl, DataCache cache, Args config) {
        if (cache.customerIdsWithDue.isEmpty()) {
            return null;
        }
        long customerId = randomFrom(cache.customerIdsWithDue);
        String payload = "{"
            + "\"customerId\":" + customerId + ","
            + "\"amount\":1,"
            + "\"paymentDate\":\"" + LocalDate.now() + "\","
            + "\"paymentMode\":\"" + DEFAULT_PAYMENT_MODE + "\""
            + "}";
        return HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/ledger/payment"))
            .timeout(Duration.ofSeconds(config.requestTimeoutSeconds))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();
    }

    private static HttpRequest buildBankDepositRequest(String baseUrl, DataCache cache, Args config) {
        if (cache.bankAccountIds.isEmpty()) {
            return null;
        }
        long bankAccountId = randomFrom(cache.bankAccountIds);
        double amount = 5000 + ThreadLocalRandom.current().nextInt(15000);
        String payload = "{"
            + "\"bankAccountId\":" + bankAccountId + ","
            + "\"depositDate\":\"" + LocalDate.now() + "\","
            + "\"depositAmount\":" + String.format("%.2f", amount) + ","
            + "\"referenceNumber\":\"LD-" + System.currentTimeMillis() + "\","
            + "\"paymentMode\":\"" + DEFAULT_DEPOSIT_PAYMENT_MODE + "\","
            + "\"notes\":\"Load test deposit\""
            + "}";
        return HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/bank-deposits"))
            .timeout(Duration.ofSeconds(config.requestTimeoutSeconds))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();
    }

    private static HttpRequest buildWarehouseTransferRequest(String baseUrl, DataCache cache, Args config) {
        if (cache.warehouseIds.size() < 2 || cache.variantIds.isEmpty()) {
            return null;
        }
        long fromWarehouseId = randomFrom(cache.warehouseIds);
        long toWarehouseId = randomFrom(cache.warehouseIds);
        if (fromWarehouseId == toWarehouseId) {
            int index = cache.warehouseIds.indexOf(fromWarehouseId);
            toWarehouseId = cache.warehouseIds.get((index + 1) % cache.warehouseIds.size());
        }
        long variantId = randomFrom(cache.variantIds);
        long filledQty = 1 + ThreadLocalRandom.current().nextInt(5);
        long emptyQty = ThreadLocalRandom.current().nextInt(3);
        String payload = "{"
            + "\"fromWarehouseId\":" + fromWarehouseId + ","
            + "\"toWarehouseId\":" + toWarehouseId + ","
            + "\"variantId\":" + variantId + ","
            + "\"filledQty\":" + filledQty + ","
            + "\"emptyQty\":" + emptyQty + ","
            + "\"transferDate\":\"" + LocalDate.now() + "\","
            + "\"notes\":\"Load test transfer\""
            + "}";
        return HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/warehouse-transfers"))
            .timeout(Duration.ofSeconds(config.requestTimeoutSeconds))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(payload))
            .build();
    }

    private static long randomFrom(List<Long> ids) {
        int idx = ThreadLocalRandom.current().nextInt(ids.size());
        return ids.get(idx);
    }

    private static DataCache loadReferenceData(HttpClient client, Args config) throws Exception {
        List<Long> customerIds = fetchIds(client, config.baseUrl + "/api/customers/active/list", ""); 
        List<Long> customersWithDue = fetchIds(
            client,
            config.baseUrl + "/api/customers/active?page=0&size=200&minDueAmount=1",
            "content");
        List<Long> warehouseIds = fetchIds(client, config.baseUrl + "/api/warehouses/active", "data");
        List<Long> variantIds = fetchIds(client, config.baseUrl + "/api/variants/active/list", "");
        List<Long> bankAccountIds = fetchIds(client, config.baseUrl + "/api/bank-accounts/active/list", "");
        List<Long> expenseCategoryIds = fetchIds(client, config.baseUrl + "/api/expense-categories/active", "");
        return new DataCache(customerIds, customersWithDue, warehouseIds, variantIds, bankAccountIds, expenseCategoryIds);
    }

    private static List<Long> fetchIds(HttpClient client, String url, String key) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(15))
            .GET()
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return List.of();
        }
        String body = response.body();
        String target = key != null && !key.isBlank() ? extractArraySection(body, key) : body;
        return JsonIdExtractor.extractIds(target);
    }

    private static String extractArraySection(String body, String key) {
        int keyIndex = body.indexOf("\"" + key + "\"");
        if (keyIndex < 0) {
            return body;
        }
        int arrayStart = body.indexOf('[', keyIndex);
        if (arrayStart < 0) {
            return body;
        }
        int depth = 0;
        for (int i = arrayStart; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 0) {
                    return body.substring(arrayStart, i + 1);
                }
            }
        }
        return body;
    }

    private static final class JsonIdExtractor {
        static List<Long> extractIds(String body) {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\"id\"\\s*:\\s*(\\d+)");
            java.util.regex.Matcher matcher = pattern.matcher(body);
            java.util.List<Long> ids = new java.util.ArrayList<>();
            while (matcher.find()) {
                ids.add(Long.parseLong(matcher.group(1)));
            }
            return ids;
        }
    }

    private static final class WeightedPicker {
        private final List<Operation> operations;
        private final long[] weights;
        private final long totalWeight;
        private final Map<String, LongAdder> hits = new ConcurrentHashMap<>();

        WeightedPicker(List<Operation> operations, Args config) {
            this.operations = operations;
            this.weights = new long[operations.size()];
            long total = 0;
            for (int i = 0; i < operations.size(); i++) {
                Operation op = operations.get(i);
                long weight = config.weights.getOrDefault(op.name, config.defaultWeightFor(op.name));
                weight = Math.max(weight, 0);
                weights[i] = weight;
                total += weight;
                hits.put(op.name, new LongAdder());
            }
            if (total == 0) {
                Arrays.fill(weights, 1L);
                total = operations.size();
            }
            this.totalWeight = total;
        }

        Operation pick() {
            long r = ThreadLocalRandom.current().nextLong(totalWeight);
            long acc = 0;
            for (int i = 0; i < operations.size(); i++) {
                acc += weights[i];
                if (r < acc) {
                    Operation op = operations.get(i);
                    hits.get(op.name).increment();
                    return op;
                }
            }
            Operation fallback = operations.get(operations.size() - 1);
            hits.get(fallback.name).increment();
            return fallback;
        }

        void printMix() {
            System.out.println("==== MIX ====");
            hits.forEach((name, count) -> System.out.println(name + "=" + count.sum()));
        }
    }

    private static final class Stats {
        final LongAdder count = new LongAdder();
        final LongAdder ok = new LongAdder();
        final LongAdder errors = new LongAdder();
        final LongAdder sumNanos = new LongAdder();
        final LongAccumulator min = new LongAccumulator(Long::min, Long.MAX_VALUE);
        final LongAccumulator max = new LongAccumulator(Long::max, 0L);
        final ConcurrentLinkedQueue<Long> samples = new ConcurrentLinkedQueue<>();

        void record(long nanos, boolean success) {
            count.increment();
            if (success) {
                ok.increment();
            } else {
                errors.increment();
            }
            sumNanos.add(nanos);
            min.accumulate(nanos);
            max.accumulate(nanos);
            samples.add(nanos);
        }

        Percentiles percentiles() {
            Long[] values = samples.toArray(new Long[0]);
            if (values.length == 0) {
                return new Percentiles(0, 0);
            }
            Arrays.sort(values, Comparator.naturalOrder());
            double p95 = percentile(values, 0.95) / 1_000_000.0;
            double p99 = percentile(values, 0.99) / 1_000_000.0;
            return new Percentiles(p95, p99);
        }

        private long percentile(Long[] values, double pct) {
            int index = (int) Math.ceil(pct * values.length) - 1;
            index = Math.min(Math.max(index, 0), values.length - 1);
            return values[index];
        }
    }

    private static final class Percentiles {
        final double p95;
        final double p99;

        Percentiles(double p95, double p99) {
            this.p95 = p95;
            this.p99 = p99;
        }
    }

    private static final class Args {
        final String baseUrl;
        final String username;
        final String password;
        final int concurrency;
        final int durationSeconds;
        final int sleepMs;
        final long maxRequests;
        final int requestTimeoutSeconds;
        final Map<String, Long> weights;

        private Args(String baseUrl, String username, String password, int concurrency,
                     int durationSeconds, int sleepMs, long maxRequests, int requestTimeoutSeconds,
                     Map<String, Long> weights) {
            this.baseUrl = baseUrl;
            this.username = username;
            this.password = password;
            this.concurrency = concurrency;
            this.durationSeconds = durationSeconds;
            this.sleepMs = sleepMs;
            this.maxRequests = maxRequests;
            this.requestTimeoutSeconds = requestTimeoutSeconds;
            this.weights = weights;
        }

        static Args parse(String[] args) {
            Map<String, String> map = new ConcurrentHashMap<>();
            for (String arg : args) {
                if (arg.startsWith("--") && arg.contains("=")) {
                    String[] parts = arg.substring(2).split("=", 2);
                    map.put(parts[0], parts[1]);
                }
            }
            String baseUrl = map.getOrDefault("base-url", "http://localhost:8080");
            String username = map.getOrDefault("username", "owner");
            String password = map.getOrDefault("password", "owner");
            int concurrency = parseInt(map.get("concurrency"), 10);
            int duration = parseInt(map.get("duration-seconds"), 60);
            int sleepMs = parseInt(map.get("sleep-ms"), 0);
            long maxRequests = parseLong(map.get("max-requests"), -1);
            int timeout = parseInt(map.get("request-timeout-seconds"), 15);
            Map<String, Long> weights = parseWeights(map.get("weights"));
            return new Args(baseUrl, username, password, concurrency, duration, sleepMs, maxRequests, timeout, weights);
        }

        long defaultWeightFor(String name) {
            return switch (name) {
                case "POST_sale" -> 35;
                case "POST_payment" -> 10;
                case "POST_empty_return" -> 10;
                case "POST_expense" -> 10;
                case "POST_bank_deposit" -> 5;
                case "POST_warehouse_transfer" -> 5;
                case "GET_customers_due" -> 10;
                case "GET_sales" -> 10;
                case "GET_bank_accounts" -> 3;
                case "GET_payment_modes" -> 2;
                default -> 1;
            };
        }

        private static int parseInt(String value, int fallback) {
            if (value == null || value.trim().isEmpty()) {
                return fallback;
            }
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException ex) {
                return fallback;
            }
        }

        private static long parseLong(String value, long fallback) {
            if (value == null || value.trim().isEmpty()) {
                return fallback;
            }
            try {
                return Long.parseLong(value.trim());
            } catch (NumberFormatException ex) {
                return fallback;
            }
        }

        private static Map<String, Long> parseWeights(String raw) {
            Map<String, Long> weights = new ConcurrentHashMap<>();
            if (raw == null || raw.trim().isEmpty()) {
                return weights;
            }
            String[] pairs = raw.split(",");
            for (String pair : pairs) {
                String[] parts = pair.split(":");
                if (parts.length != 2) {
                    continue;
                }
                String key = parts[0].trim();
                String value = parts[1].trim();
                try {
                    weights.put(key, Long.parseLong(value));
                } catch (NumberFormatException ignored) {
                    // skip invalid weight
                }
            }
            return weights;
        }
    }
}
