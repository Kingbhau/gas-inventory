export interface CacheConfig {
  ttl?: number; // Time to live in milliseconds (0 = session-only)
  strategy?: 'memory' | 'session' | 'local';
}
