package org.zhuduan.cache.storage.impl.local;

import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.zhuduan.cache.storage.CacheStorageService;
import org.zhuduan.cache.storage.impl.redis.CacheStorageServiceRedisImpl;
import org.zhuduan.model.CacheInfoModel;
import org.zhuduan.utils.CacheException;
import org.zhuduan.utils.Log4jUtil;

/***
 * 
 * CacheStorageService的本地实现，由自己维护ConcurrentHashMap来实现
 * 		实现思路较为简单：使用cacheMap来缓存对象，cacheCountDown来做缓存时间的策略差异实现
 * 						采用了SoftReference来防止内存极限情况奔溃的问题
 *                      虽然ConcurrentHashMap的桶策略保证了写的一致性问题，但是读的时候是否会因为写锁导致的脏读依赖于ConcurrentHashMap的实现
 *                      （但是考虑到一般场景下的读取数据准确性要求，暂时不考虑这个场景的问题）
 *      相对Guava的LocalCache实现还是存在很大差距，比如数据的刷新机制、hit命中率统计、LRU等策略等
 *      但是优点是实现比较简单，无需其它第三方包引用
 *      可以作为缺省的实现方案（在初始化参数错误或者无更多配置信息时使用）
 * 
 * @author	zhuhaifeng
 * @date	2017年2月21日
 *
 */
public class CacheStorageServiceLocalImpl implements CacheStorageService {
		
	private static final Logger		sysLog		=	Log4jUtil.sysLog;		// 系统日志
	private static final Logger		svcLog		=	Log4jUtil.svcLog;		// service日志
	
	// 实际用于缓存的Map
	private static final ConcurrentHashMap<SoftReference<String>, SoftReference<String>> cacheMap = new ConcurrentHashMap<>();	
	
	// 用于对不同的Key做不同的expire策略
	private static final ConcurrentHashMap<SoftReference<String>, SoftReference<CacheInfoModel>> cacheCountDown = new ConcurrentHashMap<>();		
	
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
		SoftReference<String> cacheValue = cacheMap.get(cacheKey);

		// 如果软引用已经被已经被释放，或者本身就没有缓存进去的话，直接返回null即可
		if(cacheValue==null){
			return null;
		}
		return cacheValue.get();
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
    		SoftReference<String> key = new SoftReference<String>(cacheKey);
    		SoftReference<String> value = new SoftReference<String>(cacheValue);
    		cacheMap.put(key, value);
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
