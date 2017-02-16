package org.zhuduan.cache.storage.impl.redis;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;
import org.zhuduan.cache.storage.CacheStorageService;
import org.zhuduan.utils.Log4jUtil;

import redis.clients.jedis.JedisCluster;

/***
 * 
 * CacheStorageService的redis实现
 * 
 * 
 * @author	zhuhaifeng
 * @date	2017年2月16日
 *
 */
@Service
public class CacheStorageServiceRedisImpl implements CacheStorageService{
	
	private static final Logger		sysLog		=	Log4jUtil.sysLog;		// 系统日志
	private static final Logger		svcLog		=	Log4jUtil.svcLog;		// service日志
	
	
	private boolean useCache	= 	true;					// 是否使用缓存
	
	private JedisCluster jedisCluster;

	
	/**
     * 设置缓存: 返回true成功, false失败
     * 
     * @param cacheKey 缓存key
     * @param cacheValue 缓存value
     * @param expireTimeSeconds 过期时间, 单位秒!
     * @return
     */
    public Boolean setCache(String cacheKey, String cacheValue, int expireTimeSeconds){
		if ( !useCache ){
			return false;
		}
		
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
    		jedisCluster.set(cacheKey, cacheValue);
    		jedisCluster.expire(cacheKey, expireTimeSeconds);
			return true;
    	} catch (Exception exp){ 
    		// 防止缓存崩溃,影响主业务逻辑
    		sysLog.error(Log4jUtil.getCallLocation() + " redis error for: " + exp.getMessage());
    	}
    	return false;
    }

    
    /**
     * 获取缓存
     * 
     * @param cacheKey
     * @return null if error occur
     */
    public String getCache(String cacheKey){
    	if ( !useCache ){
			return null;
		}
    	
    	try{
    		return jedisCluster.get(cacheKey);
    	} catch (Exception exp){ 
    		// 防止缓存崩溃,影响主业务逻辑
    		sysLog.error(Log4jUtil.getCallLocation() + " redis error for: " + exp.getMessage());
    	}
    	return null;
    }

    
    /**
     * 检查KEY是否存在
     * 
     * @param cacheKey
     * @return
     */
	public Boolean isCacheKeyExists(String cacheKey) {
		if ( !useCache ){
			return false;
		}
		
		try{
			return jedisCluster.exists(cacheKey);
    	} catch (Exception exp){ 
    		// 防止缓存崩溃,影响主业务逻辑
    		sysLog.error(Log4jUtil.getCallLocation() + " redis error for: " + exp.getMessage());
    	}
    	return false;
	}

	
	/**
     * 删除指定cacheKey
     * 
     * @param cacheKey
     * @return
     */
	public Boolean deleteCache(String cacheKey) {
		if ( !useCache ){
			return false;
		}
		
		try{
			if (isCacheKeyExists(cacheKey)) {
				boolean delResult = jedisCluster.del(cacheKey) > 0;
	            return delResult;
	        }
	        return false;
    	} catch (Exception exp){ 
    		// 防止缓存崩溃,影响主业务逻辑
    		sysLog.error(Log4jUtil.getCallLocation() + " redis error for: " + exp.getMessage());
    	}
    	return false;
	}

	
    /**
     * 以step步长, cacheKey的自增, 过期时间为expireTimeSeconds秒
     * 
     * @param cacheKey
     * @param incrStep  以incrStep步长自增
     * @param expireTimeSeconds  过期时间, 单位秒!
     * @return 返回增长后的值, or 0 if error occur
     */
	public Long incrCacheKey(String cacheKey, long incrStep, int expireTimeSeconds) {
		if ( !useCache ){
			return 0L;
		}
		
		if(StringUtils.isEmpty(cacheKey)){
			// 防止业务奔溃，直接返回失败值
    		svcLog.warn(Log4jUtil.getCallLocation() + " empty key ");
    		return 0L;
    	}
    	if(expireTimeSeconds <= 0){
    		svcLog.warn(Log4jUtil.getCallLocation() + " expireTimeSeconds <= 0! ");
    		return 0L;
    	} else if (expireTimeSeconds > MAX_EXPIRE_SECONDS){
    		svcLog.warn(Log4jUtil.getCallLocation() + " expireTimeSeconds > MAX_EXPIRE_SECONDS! ");
    		return 0L;
    	}
    	
    	try{
    		final long result = jedisCluster.incrBy(cacheKey, incrStep);
    		jedisCluster.expire(cacheKey, expireTimeSeconds);
    		return result;
    	} catch (Exception exp){
    		// 防止缓存崩溃,影响主业务逻辑
    		sysLog.error(Log4jUtil.getCallLocation() + " redis error for: " + exp.getMessage());
    	}
    	
		return 0L; // 需要业务程序手动处理!!!
	}


	public JedisCluster getJedisCluster() {
		return jedisCluster;
	}

	public void setJedisCluster(JedisCluster jedisCluster) {
		this.jedisCluster = jedisCluster;
	}

	public boolean isUseCache() {
		return useCache;
	}

	public void setUseCache(boolean useCache) {
		this.useCache = useCache;
	}
}
