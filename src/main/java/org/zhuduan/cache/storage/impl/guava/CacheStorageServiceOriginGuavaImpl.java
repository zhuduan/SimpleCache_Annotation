package org.zhuduan.cache.storage.impl.guava;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.zhuduan.cache.storage.CacheStorageService;
import org.zhuduan.cache.storage.impl.redis.CacheStorageServiceRedisImpl;
import org.zhuduan.config.SimpleCacheConfig;
import org.zhuduan.utils.CacheConstants;
import org.zhuduan.utils.CacheException;
import org.zhuduan.utils.Log4jUtil;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;


/***
 * 
 * 使用原生的GuavaCache来缓存信息，只是在这个基础上进行了简单的使用包装
 * 考虑到GuavaCache的一些特性，采用如下配置：
 * 		1.采用SoftReference
 * 		2.Access和Write过期时间在SimpleCacheConfig中进行统一配置
 * 
 * 
 * @author	zhuhaifeng
 * @date	2017年2月23日
 *
 */
public class CacheStorageServiceOriginGuavaImpl implements CacheStorageService {
	
	private static final Logger		svcLog		=	Log4jUtil.svcLog;		// service日志

	private volatile Cache<String,String> guavaCahce;						// 内部使用的Guava缓存
	
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
                	INSTANCE = new CacheStorageServiceOriginGuavaImpl(SimpleCacheConfig.ORIGIN_GUAVACACHE_OBJECT_NUM_MAX,
										                			SimpleCacheConfig.ORIGIN_GUAVACACHE_ACCESS_EXPIRE_SECONDS,
										                			SimpleCacheConfig.ORIGIN_GUAVACACHE_WRITE_EXPIRE_SECONDS);
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
			objectNumMax = SimpleCacheConfig.ORIGIN_GUAVACACHE_OBJECT_NUM_MAX;
		}
		if (null==accessExpireSeconds || accessExpireSeconds.longValue()<=0L){
			svcLog.warn("origin guava中accessExpireSeconds传入值有错误,使用了默认值");
			accessExpireSeconds = SimpleCacheConfig.ORIGIN_GUAVACACHE_ACCESS_EXPIRE_SECONDS;
		}
		if (null==writeExpireSeconds || writeExpireSeconds.longValue()<=0L){
			svcLog.warn("origin guava中writeExpireSeconds传入值有错误,使用了默认值");
			writeExpireSeconds = SimpleCacheConfig.ORIGIN_GUAVACACHE_WRITE_EXPIRE_SECONDS;
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
		this.guavaCahce =CacheBuilder.newBuilder()
									.maximumSize(SimpleCacheConfig.ORIGIN_GUAVACACHE_OBJECT_NUM_MAX)
									.expireAfterAccess(SimpleCacheConfig.ORIGIN_GUAVACACHE_ACCESS_EXPIRE_SECONDS, TimeUnit.SECONDS)
									.expireAfterWrite(SimpleCacheConfig.ORIGIN_GUAVACACHE_WRITE_EXPIRE_SECONDS, TimeUnit.SECONDS)
									.softValues()
							        .build();  
	}
	
	
	@Override
	public String getCache(String cacheKey) {
		return guavaCahce.getIfPresent(cacheKey);		
	}

	
	@Override
	public Boolean setCache(String cacheKey, String cacheValue, int expireTimeSeconds) {
		// 这里的expireTimeSeconds实际上是不生效的
		guavaCahce.put(cacheKey, cacheValue);
		return true;
	}

	@Override
	public Boolean isCacheKeyExists(String cacheKey) {
		String cacheValue = guavaCahce.getIfPresent(cacheKey);
		if (null == cacheValue){
			return false;
		}
		return true;
	}

	
	@Override
	public Boolean deleteCache(String cacheKey) {
		guavaCahce.invalidate(cacheKey);
		return true;
	}

	
	@Override
	public Long incrCacheKey(String cacheKey, long incrStep, int expireTimeSeconds) throws CacheException {
		throw new CacheException(CacheConstants.EXCEPTION_NOT_SUPPORT_METHOD, "OriginGuava的实现中不支持值的增加功能");
	}

}
