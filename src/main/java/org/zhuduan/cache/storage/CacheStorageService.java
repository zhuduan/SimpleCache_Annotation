package org.zhuduan.cache.storage;

import org.zhuduan.utils.CacheException;

/***
 * 
 * 缓存接口，定义了具体的缓存调用方法:
 * 具体存储可以通过redis、memcache、或者local内存（或者guava）进行实现
 * （具体使用哪个实现会由工厂类决定）
 * 
 * 
 * @author	zhuhaifeng
 * @date	2017年2月16日
 *
 */
public interface CacheStorageService {

    /**
     * 最大缓存时间31天;
     */
    static final int MAX_EXPIRE_SECONDS = 60*60*24*31;

    
    /**
     * 获取缓存
     * @param cacheKey
     * @return
     */
    String getCache(String cacheKey);
    
    
    /**
     * 设置缓存, 返回true成功, false失败!
     * 
     * @param cacheKey
     * @param cacheValue
     * @param expireTimeSeconds 过期时间, 单位秒!
     * @return
     */
    Boolean setCache(String cacheKey, String cacheValue, int expireTimeSeconds);
    
    
    /**
     * 检查KEY是否存在
     * 
     * @param cacheKey
     * @return
     */
    Boolean isCacheKeyExists(String cacheKey);

    
    /**
     * 删除指定cacheKey
     * 
     * @param cacheKey
     * @return
     */
    Boolean deleteCache(String cacheKey);

    
    /**
     * 以step步长, cacheKey的自增, 过期时间为expireTimeSeconds秒
     * 
     * @param cacheKey
     * @param incrStep  以incrStep步长自增
     * @param expireTimeSeconds  过期时间, 单位秒!
     * @return 返回增长后的值
     * @throws CacheException 
     */
    Long incrCacheKey(String cacheKey, long incrStep, int expireTimeSeconds) throws CacheException;
}
