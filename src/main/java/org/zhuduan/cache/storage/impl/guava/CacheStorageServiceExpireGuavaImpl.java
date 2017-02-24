package org.zhuduan.cache.storage.impl.guava;

import java.util.concurrent.ExecutionException;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.zhuduan.cache.storage.CacheStorageService;
import org.zhuduan.config.SimpleCacheConfig;
import org.zhuduan.model.CacheInfoModel;
import org.zhuduan.utils.CacheConstants;
import org.zhuduan.utils.CacheException;
import org.zhuduan.utils.Log4jUtil;

import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/***
 * 
 * 使用GuavaCache来缓存信息
 * 但是通过将String包装成CacheInfoModel实现了get前先验证数据是否过期，来实现不同key可以用不同的过期时间设置
 * 其中在使用GuavaCache时：
 * 		1.因为自定义了过期时间，因此没有涉及guava自身的expire time（Access和Write都是）
 * 		2.为了应对极端场景，使用了guava的WeakReference设置（可能会带来缓存命中稍差，但是能有效在缓存吃紧情况下保证程序健壮性）
 * 
 * 
 * @author	zhuhaifeng
 * @date	2017年2月23日
 *
 */
public class CacheStorageServiceExpireGuavaImpl implements CacheStorageService {

	private static final Logger		sysLog		=	Log4jUtil.sysLog;		// 系统日志
	private static final Logger		svcLog		=	Log4jUtil.svcLog;		// service日志

	private volatile LoadingCache<String,CacheInfoModel> cahceBuilder;				// 内部使用的Guava缓存
	
	private volatile static CacheStorageServiceExpireGuavaImpl INSTANCE; 			// 单例模式，声明成 volatile 的实例	
	
	
	/***
	 * 通过单例模式来获取CacheStorageServiceExpireGuavaImpl的实例
	 * 
	 * @return
	 * @throws CacheException
	 */
    public static CacheStorageServiceExpireGuavaImpl getInstance() {
        // 二重锁检验，来防止多线程导致的线程安全问题
    	if (INSTANCE == null) {                         
            synchronized (CacheStorageServiceExpireGuavaImpl.class) {
                if (INSTANCE == null) {
                	INSTANCE = new CacheStorageServiceExpireGuavaImpl();
                }
            }
        }
        return INSTANCE;
    }
	
    
	@Override
	public String getCache(String cacheKey) {
		if(Strings.isNullOrEmpty(cacheKey)){
			svcLog.warn(Log4jUtil.getCallLocation() + " empty key ");
			return null;
		}
		
		try {
			CacheInfoModel infoModel = cahceBuilder.get(cacheKey);
			
			// key本身不存在
			if ( infoModel==null ){
				return null;
			}
			
			// 如果信息不存在或者已经过期
			if ( infoModel.getCacheValue()==null
					|| infoModel.getCacheBeginTimeLong()==null
					|| infoModel.getCacheExpireTimeLong()==null 
					|| ((System.currentTimeMillis() - infoModel.getCacheBeginTimeLong())>infoModel.getCacheExpireTimeLong()))
			{
				cahceBuilder.invalidate(cacheKey);
				svcLog.info("clean obj in expire guava for key : " + cacheKey);
				return null;
			}
			return infoModel.getCacheValue();
		} catch (ExecutionException exp) {
			// 防止缓存崩溃,影响主业务逻辑
    		sysLog.error(Log4jUtil.getCallLocation() + " expire guava error for: " + exp.getMessage());
    		return null;
		}	
	}

	
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
    		cacheInfoModel.setCacheExpireTimeLong((expireTimeSeconds*1000L));
    		cacheInfoModel.setCacheBeginTimeLong(System.currentTimeMillis());
    		
    		cahceBuilder.put(cacheKey, cacheInfoModel);
    		return true;
    	} catch (Exception exp){ 
    		// 防止缓存崩溃,影响主业务逻辑
    		sysLog.error(Log4jUtil.getCallLocation() + " expire guava error for: " + exp.getMessage());
    	}
    	return false;
	}

	
	@Override
	public Boolean isCacheKeyExists(String cacheKey) {
		CacheInfoModel infoModel = cahceBuilder.getIfPresent(cacheKey);
		if (null == infoModel || null == infoModel.getCacheValue()){
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
		throw new CacheException(CacheConstants.EXCEPTION_NOT_SUPPORT_METHOD, "ExpireGuava的实现中不支持值的增加功能");
	}

	
	// 私有的构造器
	public CacheStorageServiceExpireGuavaImpl(){
		this.cahceBuilder =CacheBuilder.newBuilder()
				.maximumSize(SimpleCacheConfig.EXPIRE_GUAVACACHE_OBJECT_NUM_MAX)
				.weakKeys()
				.weakValues()
		        .build(new CacheLoader<String, CacheInfoModel>(){
				            @Override
				            public CacheInfoModel load(String key) throws Exception {
				            	// TODO: 怎么处理load事件
				            	return null;
				            }							            
		        });  
	}
}
