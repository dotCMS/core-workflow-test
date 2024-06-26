package com.dotcms.cache.lettuce;

import com.dotcms.util.DotCloneable;
import com.dotmarketing.business.cache.provider.CacheProvider;
import com.dotmarketing.business.cache.provider.CacheProviderStats;
import com.dotmarketing.business.cache.provider.CacheStats;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.util.StringPool;
import io.lettuce.core.ScriptOutputType;
import io.lettuce.core.api.StatefulRedisConnection;
import io.vavr.Lazy;
import io.vavr.control.Try;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Redis Cache implementation Notes: 1) This redis implementation does not have the clusted id, b/c redis handles each
 * member, key, hash, incr and channel by using the cluster id so it is not need to repeat it here.
 * <p>
 * The cache handles a ttl, one global by default REDIS_SERVER_DEFAULT_TTL, but can prefix a group to the same key in
 * order to have a specific ttl for a section.
 * <p>
 * 2) When a call is running on transaction to avoid dirty saves or fetches, the cache does not put or retrieve
 * anything.
 * <p>
 * 3) Since the information is being stored as a java byte (binary) only can use serializable values, non-serializable
 * objects will be skipped from the cache.
 * <p>
 * 4) Objects that implements {@link DotCloneable}, the cache will returns a Clone of the object stored on the cache
 * instead of the actual copy on the cache this will helps
 */
public class RedisCache extends CacheProvider {

    private static final long serialVersionUID = -855583393078878276L;

    private static final Long ZERO = 0L;

    protected final String REDIS_GROUP_KEY;
    protected final String REDIS_PREFIX_KEY;
    private final Lazy<RedisClient<String, Object>> client;

    private final int keyBatchingSize = Config.getIntProperty("REDIS_SERVER_KEY_BATCH_SIZE", 1000);
    private final long defaultTTL = Config.getLongProperty("REDIS_SERVER_DEFAULT_TTL", -1);
    private final Map<String, Long> groupTTLMap = new ConcurrentHashMap<>();

    public RedisCache(final Lazy<RedisClient<String, Object>> client) {

        this.client = client;
        this.REDIS_GROUP_KEY = "GROUP_KEY";
        this.REDIS_PREFIX_KEY = "PREFIX_KEY";
    }

    public RedisCache() {
        this(Lazy.of(() -> RedisClientFactory.getClient("cache")));
    }

    @Override
    public String getName() {
        return "Redis Provider";
    }

    @Override
    public String getKey() {
        return "Redis";
    }

    @Override
    public boolean isDistributed() {
        return true;
    }

    protected RedisClient<String, Object> getClient() {

        return client.get();
    }

    /**
     * returns a cache key
     *
     * @param group
     * @param key
     * @return
     */
    @VisibleForTesting
    String cacheKey(final String group, final String key) {
        return this.REDIS_PREFIX_KEY
                + "." +
                (
                        group != null && key != null
                                ? group + "." + key
                                : group != null
                                ? group
                                : ""
                )
                + ".";
    }

    @VisibleForTesting
    String cacheKey(final String group) {
        return cacheKey(group, null);
    }

    @Override
    public void init() {

        Logger.info(this.getClass(), "*** Initializing [" + getName() + "].");
        Logger.info(this.getClass(), "          prefix [" + this.REDIS_PREFIX_KEY + "]");
        Logger.info(this.getClass(), "          inited [" + this.isInitialized() + "]");
        Logger.info(this.getClass(), "*** Initialized  [" + getName() + "].");
    }

    @Override
    public boolean isInitialized() {

        return Try.of(() -> this.client.get().ping()).onFailure(e -> Logger.warn(RedisCache.class, e.getMessage()))
                .getOrElse(false);
    }

    @Override
    public void put(final String group, final String key, final Object content) {

        // this avoid mutability and dirty cache issues
        if (DbConnectionFactory.inTransaction()) {
            Logger.debug(this, () -> "In Transaction, Skipping the put to Redis cache for group: "
                    + group + "key: " + key);
            return;
        }
        if (key == null || group == null || !(content instanceof Serializable)) {
            Logger.debug(this, () -> "The content: " + (null != content ? content.getClass() : "unknown") +
                    " is not serialize, Skipping the put to Redis cache for group: "
                    + group + "key: " + key);
            return;
        }

        Logger.debug(this, () -> "Redis, putting group: " + group + "key" + key);
        final long ttl = this.getTTL(group);
        final String cacheKey = this.cacheKey(group, key);
        final Future<String> future = this.getClient().setAsync(cacheKey, content, ttl);
        this.getClient().addAsyncMembers(REDIS_GROUP_KEY, group);
        if (Logger.isDebugEnabled(this.getClass())) {

            String msg = "Error";
            try {
                msg = future.get();
            } catch (InterruptedException | ExecutionException e) {
                msg = e.getMessage();
            }
            if (!"OK".equalsIgnoreCase(msg)) {
                Logger.debug(this, "Redis, putting group: " + group +
                        "key" + key + "result: " + msg);
            }
        }


    }

    private long getTTL(final String group) {

        // try to figured out if any time out by group, otherwise uses the default ttl
        final String groupTTLKey = "REDIS_SERVER_DEFAULT_TTL_" + group;
        final long ttl = this.groupTTLMap.computeIfAbsent(groupTTLKey,
                k -> Config.getLongProperty(groupTTLKey, -1));
        return -1 == ttl ? this.defaultTTL : ttl;
    }

    @Override
    public Object get(final String group, final String key) {

        // this avoid mutability and dirty cache issues
        if (DbConnectionFactory.inTransaction()) {

            Logger.debug(this, () -> "In Transaction, Skipping the get to Redis cache for group: "
                    + group + "key" + key);
            return null;
        }

        if (key == null || group == null) {
            return null;
        }

        final String cacheKey = this.cacheKey(group, key);
        try {

            return this.extractObject(this.getClient().get(cacheKey));
        } catch (Exception e) {

            Logger.debug(this, "Timeout error on getting Redis cache for group: "
                    + group + "key" + key + " msg: " + e.getMessage());
            return null;
        }

    }

    private Object extractObject(final Object o) {
        return o != null && o instanceof DotCloneable ?
                this.extractObject(DotCloneable.class.cast(o)) : o;
    }

    private Object extractObject(final DotCloneable o) {

        return Try.of(() -> o.clone()).getOrElse(o);
    }


    /**
     * removes cache keys async and resets the get timer that reenables get functions
     *
     * @param keys
     */
    private void removeKeys(final String... keys) {

        if (UtilMethods.isSet(keys)) {

            this.getClient().deleteNonBlocking(keys);
        }
    }

    /**
     * removes cache keys async and resets the get timer that reenables get functions
     *
     * @param keys
     */
    private void removeKeysRaw(final String... keys) {

        if (UtilMethods.isSet(keys)) {

            this.getClient().deleteNonBlocking(keys);
        }
    }

    @Override
    public void remove(final String group, final String key) {

        final String cacheKey = this.cacheKey(group, key);
        this.removeKeys(cacheKey);
    }

    @Override
    public void remove(final String group) {

        if (!UtilMethods.isEmpty(group)) {

            // todo: this should be a wildcard deletion instead of keys scan

            final String prefix =  StringPool.STAR + cacheKey(group) + StringPool.STAR;
            // Getting all the keys for the given groups
            /*DotConcurrentFactory.getInstance().getSingleSubmitter
                    (CacheWiper.class.getSimpleName()).submit(new CacheWiper(prefix));*/
            this.getClient().deleteFromPattern(prefix);
        }
    }

    @Override
    public void removeAll() {

        final String prefix = StringPool.STAR + this.REDIS_PREFIX_KEY + "." + StringPool.STAR;
        this.getClient().deleteFromPattern(prefix);
    }

    @Override
    public Set<String> getKeys(final String group) {

        final String prefix = this.cacheKey(group);
        final String matchesPattern = prefix + StringPool.STAR;
        final Set<String> keys = new LinkedHashSet<>();
        this.getClient().scanKeys(matchesPattern, this.keyBatchingSize, //keys::addAll);
                redisKeys -> redisKeys.stream().map(redisKey ->  // we remove the prefix in order to have the real key
                        redisKey.replace(prefix, StringPool.BLANK)).forEach(keys::add));

        return keys;
    }


    @VisibleForTesting
    Set<String> getAllKeys() {

        return getKeys(null);
    }


    /**
     * returns the number of cache keys in any given group
     *
     * @param group
     * @return
     */
    private long keyCount(final String group) {

        final String prefix = LettuceAdapter.getMasterReplicaLettuceClient(this.getClient())
                .wrapKey(this.cacheKey(group) + StringPool.STAR);
        final String script = "return #redis.pcall('keys', '" + prefix + "')";
        Object keyCount = ZERO;

        try (StatefulRedisConnection<String, Object> conn = LettuceAdapter.getStatefulRedisConnection(
                this.getClient())) {

            if (conn.isOpen()) {

                keyCount = conn.sync().eval(script, ScriptOutputType.INTEGER, "0");
            }
        }

        return (Long) keyCount;
    }


    @Override
    public Set<String> getGroups() {

        return this.getClient().getMembers(REDIS_GROUP_KEY).stream()
                .map(k -> k.toString()).collect(Collectors.toSet());
    }

    @Override
    public CacheProviderStats getStats() {

        final CacheStats providerStats = new CacheStats();
        final CacheProviderStats cacheProviderStats = new CacheProviderStats(providerStats, getName());
        String memoryStats = null;

        try (StatefulRedisConnection<String, Object> conn =
                     LettuceAdapter.getStatefulRedisConnection(this.getClient())) {

            if (!conn.isOpen()) {

                return cacheProviderStats;
            }

            memoryStats = conn.sync().info();
        }

        // Read the total memory usage
        final Map<String, String> redis = getRedisProperties(memoryStats);

        for (final Map.Entry<String, String> entry : redis.entrySet()) {

            final CacheStats stats = new CacheStats();
            stats.addStat(CacheStats.REGION, "redis: " + entry.getKey());
            stats.addStat(CacheStats.REGION_SIZE, entry.getValue());
            // ret.addStatRecord(stats);
        }

        final NumberFormat nf = DecimalFormat.getInstance();
        // Getting the list of groups
        final Set<String> currentGroups = getGroups();

        for (final String group : currentGroups) {

            final CacheStats stats = new CacheStats();
            stats.addStat(CacheStats.REGION, "dotCMS: " + group);
            stats.addStat(CacheStats.REGION_SIZE, nf.format(keyCount(group)));

            cacheProviderStats.addStatRecord(stats);
        }

        return cacheProviderStats;
    }

    @Override
    public void shutdown() {

        Logger.info(this.getClass(), "*** Shutdown [" + getName() + "] .");

    }

    /**
     * Reads and parses the string report generated for the INFO Redis command in order to return any specific required
     * property.
     *
     * @param redisReport
     * @return Map
     */
    private Map<String, String> getRedisProperties(final String redisReport) {

        final Map<String, String> map = new LinkedHashMap<>();
        final String[] readLines = redisReport.split("\r\n");

        for (final String readLine : readLines) {

            final String[] lineValues = readLine.split(":", 2);

            // First check if it is a property or a header
            if (lineValues.length > 1) {

                map.put(lineValues[0], lineValues[1]);
            }
        }

        return map;
    }


}
