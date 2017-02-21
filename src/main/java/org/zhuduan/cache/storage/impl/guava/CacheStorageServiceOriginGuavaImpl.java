package org.zhuduan.cache.storage.impl.guava;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.zhuduan.cache.storage.CacheStorageService;
import org.zhuduan.cache.storage.impl.redis.CacheStorageServiceRedisImpl;
import org.zhuduan.utils.CacheConstants;
import org.zhuduan.utils.CacheException;
import org.zhuduan.utils.Log4jUtil;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class CacheStorageServiceOriginGuavaImpl implements CacheStorageService {
	
	private static final Logger		sysLog		=	Log4jUtil.sysLog;		// 系统日志
	private static final Logger		svcLog		=	Log4jUtil.svcLog;		// service日志

	private static final long CACHE_OBJECT_NUM_MAX			=	1000000000L;		// 可以缓存的最大个数，默认 1亿个
	private static final long CACHE_ACCESS_EXPIRE_SECONDS	=	600L;				// 缓存的Access过期时间，默认 600s
	private static final long CACHE_WRITE_EXPIRE_SECONDS	=	600L;				// 缓存的Write过期时间，默认 600s
	
	private volatile LoadingCache<String,String> cahceBuilder;						// 内部使用的Guava缓存
	
	private volatile static CacheStorageServiceOriginGuavaImpl INSTANCE; 			// 单例模式，声明成 volatile 的实例	
	
	
	/***
	 * 通过单例模式来获取CacheStorageServiceOriginGuavaImpl的实例
	 * 
	 * @return
	 * @throws CacheException
	 */
    public static CacheStorageServiceOriginGuavaImpl getInstance() {
        // 二重锁检验，来防止多线程导致的线程安全问题
    	if (INSTANCE == null) {                         
            synchronized (CacheStorageServiceRedisImpl.class) {
                if (INSTANCE == null) {
                	INSTANCE = new CacheStorageServiceOriginGuavaImpl(CACHE_OBJECT_NUM_MAX,
                													CACHE_ACCESS_EXPIRE_SECONDS,
                													CACHE_WRITE_EXPIRE_SECONDS);
                }
            }
        }
        return INSTANCE;
    }
    
    
    /***
     * 通过单例模式来获取CacheStorageServiceOriginGuavaImpl的实例
     * 
     * @param objectNumMax
     * @param accessExpireSeconds
     * @param writeExpireSeconds
     * @return
     */
    public static CacheStorageServiceOriginGuavaImpl getInstance(Long objectNumMax, Long accessExpireSeconds, Long writeExpireSeconds) {
    	// 检验所有的入参
    	if (null==objectNumMax || objectNumMax.longValue()<=0L){
    		svcLog.warn("origin guava中objectNumMax传入值有错误,使用了默认值");
			objectNumMax = CACHE_OBJECT_NUM_MAX;
		}
		if (null==accessExpireSeconds || accessExpireSeconds.longValue()<=0L){
			svcLog.warn("origin guava中accessExpireSeconds传入值有错误,使用了默认值");
			accessExpireSeconds = CACHE_ACCESS_EXPIRE_SECONDS;
		}
		if (null==writeExpireSeconds || writeExpireSeconds.longValue()<=0L){
			svcLog.warn("origin guava中writeExpireSeconds传入值有错误,使用了默认值");
			writeExpireSeconds = CACHE_WRITE_EXPIRE_SECONDS;
		}
    	
        // 二重锁检验，来防止多线程导致的线程安全问题
    	if (INSTANCE == null) {                         
            synchronized (CacheStorageServiceRedisImpl.class) {
                if (INSTANCE == null) {
                	INSTANCE = new CacheStorageServiceOriginGuavaImpl(objectNumMax,
										                			accessExpireSeconds,
										                			writeExpireSeconds);
                }
            }
        }
        return INSTANCE;
    }
    
		
	/***
	 * 
	 * 带参数的构造器
	 * 如果入参不正确，则使用默认的缺省值
	 * 		由于为了充分利用guava的特性，默认使用weakKeys和weakValues来处理
	 * 
	 * @param objectNumMax
	 * @param accessExpireSeconds
	 * @param writeExpireSeconds
	 */
	private CacheStorageServiceOriginGuavaImpl(Long objectNumMax, Long accessExpireSeconds, Long writeExpireSeconds){				
		this.cahceBuilder =CacheBuilder.newBuilder()
									.maximumSize(CACHE_OBJECT_NUM_MAX)
									.expireAfterAccess(CACHE_ACCESS_EXPIRE_SECONDS, TimeUnit.SECONDS)
									.expireAfterWrite(CACHE_WRITE_EXPIRE_SECONDS, TimeUnit.SECONDS)
									.weakKeys()
									.weakValues()
							        .build(new CacheLoader<String, String>(){
									            @Override
									            public String load(String key) throws Exception {
									                return key;
									            }							            
							        });  
	}
	
	
	@Override
	public String getCache(String cacheKey) {
		try {
			return cahceBuilder.get(cacheKey);
		} catch (ExecutionException exp) {
			// 防止缓存崩溃,影响主业务逻辑
    		sysLog.error(Log4jUtil.getCallLocation() + " origin guava error for: " + exp.getMessage());
    		return null;
		}		
	}

	
	@Override
	public Boolean setCache(String cacheKey, String cacheValue, int expireTimeSeconds) {
		// 这里的expireTimeSeconds实际上是不生效的
		cahceBuilder.put(cacheKey, cacheValue);
		return true;
	}

	@Override
	public Boolean isCacheKeyExists(String cacheKey) {
		String cacheValue = cahceBuilder.getIfPresent(cacheKey);
		if (null == cacheValue){
			return false;
		}
		return true;
	}

	
	@Override
	public Boolean deleteCache(String cacheKey) {
		cahceBuilder.invalidate(cacheKey);
		return true;
	}

	
	@Override
	public Long incrCacheKey(String cacheKey, long incrStep, int expireTimeSeconds) throws CacheException {
		throw new CacheException(CacheConstants.EXCEPTION_NOT_SUPPORT_METHOD, "OriginGuava的实现中不支持值的增加功能");
	}

}
