package org.zhuduan.cache.storage.impl.local;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.zhuduan.cache.storage.CacheStorageService;
import org.zhuduan.cache.storage.impl.redis.CacheStorageServiceRedisImpl;
import org.zhuduan.utils.CacheException;
import org.zhuduan.utils.Log4jUtil;


public class CacheStorageServiceLocalImpl implements CacheStorageService {
		
	private static final Logger		sysLog		=	Log4jUtil.sysLog;		// 系统日志
	private static final Logger		svcLog		=	Log4jUtil.svcLog;		// service日志
	
	private static final ConcurrentHashMap<String, String> cacheMap	= new ConcurrentHashMap<>();		// 实际用于缓存的Map
	
	private volatile static CacheStorageServiceLocalImpl INSTANCE; 			// 声明成 volatile 的实例	
	
	
	/***
	 * 通过单例模式来获取CacheStorageServiceLocalImpl的实例
	 * 
	 * @return
	 * @throws CacheException
	 */
    public static CacheStorageServiceLocalImpl getInstance() {
        // 二重锁检验，来防止多线程导致的线程安全问题
    	if (INSTANCE == null) {                         
            synchronized (CacheStorageServiceRedisImpl.class) {
                if (INSTANCE == null) {
                	INSTANCE = new CacheStorageServiceLocalImpl();
                }
            }
        }
        return INSTANCE;
    }
	
	
    /**
     * 获取缓存
     * 
     * @param cacheKey
     * @return null if error occur
     */
	@Override
	public String getCache(String cacheKey) {
		return cacheMap.get(cacheKey);
	}

	
	/**
     * 设置缓存: 返回true成功, false失败
     * 
     * @param cacheKey 缓存key
     * @param cacheValue 缓存value
     * @param expireTimeSeconds 过期时间, 单位秒!
     * @return
     */
	@Override
	public Boolean setCache(String cacheKey, String cacheValue, int expireTimeSeconds) {
		if(StringUtils.isEmpty(cacheKey)){
    		// 直接返回设置不成功，避免导致业务逻辑出错
    		svcLog.warn(Log4jUtil.getCallLocation() + " empty key ");
    		return false;
    	}
    	if(StringUtils.isEmpty(cacheValue)){
    		svcLog.warn(Log4jUtil.getCallLocation() + " empty value for key: " + cacheKey);
    		return false;
    	}
    	if(expireTimeSeconds <= 0){
    		svcLog.warn(Log4jUtil.getCallLocation() + " too small expire time for key: " + cacheKey);
    		return false;
    	} else if (expireTimeSeconds > MAX_EXPIRE_SECONDS){
    		svcLog.warn(Log4jUtil.getCallLocation() + " too high expire time for key: " + cacheKey);
    		return false;
    	}
    	try{
    		cacheMap.put(cacheKey, cacheValue);
    		// TODO: 需要增加expireTime 
			return true;
    	} catch (Exception exp){ 
    		// 防止缓存崩溃,影响主业务逻辑
    		sysLog.error(Log4jUtil.getCallLocation() + " redis error for: " + exp.getMessage());
    	}
    	return false;
	}


    /**
     * 检查KEY是否存在
     * 
     * @param cacheKey
     * @return
     */
	@Override
	public Boolean isCacheKeyExists(String cacheKey) {
		// TODO Auto-generated method stub
		return null;
	}


	/**
     * 删除指定cacheKey
     * 
     * @param cacheKey
     * @return
     */
	@Override
	public Boolean deleteCache(String cacheKey) {
		// TODO Auto-generated method stub
		return null;
	}

	
    /**
     * 以step步长, cacheKey的自增, 过期时间为expireTimeSeconds秒
     * 
     * @param cacheKey
     * @param incrStep  以incrStep步长自增
     * @param expireTimeSeconds  过期时间, 单位秒!
     * @return 返回增长后的值, or 0 if error occur
     */
	@Override
	public Long incrCacheKey(String cacheKey, long incrStep, int expireTimeSeconds) {
		// TODO Auto-generated method stub
		return null;
	}

	/***
	 * 私有的构造器
	 * 
	 */
	private CacheStorageServiceLocalImpl(){
		
	}
}
