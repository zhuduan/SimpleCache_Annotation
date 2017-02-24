package org.zhuduan.cache.storage.impl.local;

import java.lang.ref.SoftReference;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.zhuduan.cache.storage.CacheStorageService;
import org.zhuduan.model.CacheInfoModel;
import org.zhuduan.utils.CacheConstants;
import org.zhuduan.utils.CacheException;
import org.zhuduan.utils.Log4jUtil;

import com.google.common.base.Strings;

/***
 * 
 * CacheStorageService的本地实现，由自己维护ConcurrentHashMap来实现
 * 		实现思路较为简单：使用cacheMap来缓存对象，cacheCountDown来做缓存时间的策略差异实现
 * 						因为concurrentHashMap的原因，也不能简单的采用了SoftReference来处理
 *                      虽然ConcurrentHashMap的桶策略保证了写的一致性问题，但是读的时候可能会因为写锁导致的脏读（因为读未加锁）
 *                      （但是考虑到一般场景下的读取数据准确性要求，暂时不考虑这个场景的问题）
 *      相对Guava的LocalCache实现还是存在很大差距，比如数据的刷新机制、WeakReference等、hit命中率统计、LRU等策略等
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
	
	private volatile static CacheStorageServiceLocalImpl INSTANCE; 			// 声明成 volatile 的实例	
		 
	/***
	 * 
	 * 实际用于缓存的Map
	 * 采用cacheInfoModel做封装，能针对不同的ExpireTime做策略
	 * 		注意ConcurrentHashMap的一些特点，可能需要结合做业务场景的策略调整：
	 * 				1.分段可重入锁：put,remove,contains都会加锁
	 * 				2.读未加锁（可能脏读）
	 * 				3.put的时候虽然通过入参检验防止value为null，但是本例中GC释放SoftReference时会导致value为null（该场景下remove直接比对key就删除了）
	 * 				4.Iterator的弱一致性：考虑到缓存本身的应用场景（能容忍一些脏读），是可以接受的
	 */
	private static final ConcurrentHashMap<String, SoftReference<CacheInfoModel>> cacheMap = new ConcurrentHashMap<>();	
	
	
	/***
	 * 通过单例模式来获取CacheStorageServiceLocalImpl的实例
	 * 
	 * @return
	 * @throws CacheException
	 */
    public static CacheStorageServiceLocalImpl getInstance() {
        // 二重锁检验，来防止多线程导致的线程安全问题
    	if (INSTANCE == null) {                         
            synchronized (CacheStorageServiceLocalImpl.class) {
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
		if(Strings.isNullOrEmpty(cacheKey)){
			svcLog.warn(Log4jUtil.getCallLocation() + " empty key ");
			return null;
		}
		
		SoftReference<CacheInfoModel> cacheValueReference = cacheMap.get(cacheKey);
		
		// 如果本身key就不存在，就会返回一个null
		if(cacheValueReference==null){			
			return null;
		}
		
		CacheInfoModel cacheInfoModel = cacheValueReference.get();
		
		// 去拉取实际的对象，如果被GC释放了，则可能为空
		if(cacheInfoModel==null || cacheInfoModel.getCacheValue()==null){
			svcLog.info(Log4jUtil.getCallLocation() + " GC release the model for key: " + cacheKey);
			cacheMap.remove(cacheKey);
			return null;
		}
		
		//  如果过期了则需要直接清理
		if( (System.currentTimeMillis() - cacheInfoModel.getCacheBeginTimeLong()) > cacheInfoModel.getCacheExpireTimeLong() ){
			svcLog.info(Log4jUtil.getCallLocation() + " expired for key: " + cacheKey);
			cacheMap.remove(cacheKey);
			return null;
		}
		
		// 正常返回缓存值
		return cacheInfoModel.getCacheValue();
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
    		// 将缓存信息封装起来
    		CacheInfoModel cacheInfoModel = new CacheInfoModel();
    		cacheInfoModel.setCacheValue(cacheValue);
    		cacheInfoModel.setCacheExpireTimeLong(expireTimeSeconds*1000L);
    		cacheInfoModel.setCacheBeginTimeLong(System.currentTimeMillis());
    		SoftReference<CacheInfoModel> cacheValueReference = new SoftReference<CacheInfoModel>(cacheInfoModel);
    		
    		cacheMap.put(cacheKey, cacheValueReference);
			return true;
    	} catch (Exception exp){ 
    		// 防止缓存崩溃,影响主业务逻辑
    		sysLog.error(Log4jUtil.getCallLocation() + " local impl error for: " + exp.getMessage());
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
		if(Strings.isNullOrEmpty(cacheKey)){
			svcLog.warn(Log4jUtil.getCallLocation() + " empty key ");
			return false;
		}
		return cacheMap.containsKey(cacheKey);
	}


	/**
     * 删除指定cacheKey
     * 
     * @param cacheKey
     * @return
     */
	@Override
	public Boolean deleteCache(String cacheKey) {
		if(Strings.isNullOrEmpty(cacheKey)){
			svcLog.warn(Log4jUtil.getCallLocation() + " empty key ");
			return false;
		}
		cacheMap.remove(cacheKey);
		return true;
	}

	
    /**
     * 以step步长, cacheKey的自增, 过期时间为expireTimeSeconds秒
     * 
     * @param cacheKey
     * @param incrStep  以incrStep步长自增
     * @param expireTimeSeconds  过期时间, 单位秒!
     * @return 返回增长后的值, or 0 if error occur
     * @throws CacheException 
     */
	@Override
	public Long incrCacheKey(String cacheKey, long incrStep, int expireTimeSeconds) throws CacheException {
		throw new CacheException(CacheConstants.EXCEPTION_NOT_SUPPORT_METHOD, "Local Cache的实现中不支持值的增加功能");
	}

	/***
	 * 私有的构造器
	 * 
	 */
	private CacheStorageServiceLocalImpl(){
		initial();
	}
	
	/***
	 * 初始化service的相关内容
	 * 在构造时需要调用
	 * 
	 */
	private void initial(){
		// 1. 启动一个清理数据的守护线程
		CacheStorageServiceLocalGuardThread expireGuaradThread = new CacheStorageServiceLocalGuardThread();
		expireGuaradThread.setDaemon(true);
		expireGuaradThread.start();
	}
}
