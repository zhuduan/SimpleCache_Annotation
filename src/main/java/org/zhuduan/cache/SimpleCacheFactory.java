package org.zhuduan.cache;

import org.apache.log4j.Logger;
import org.zhuduan.cache.storage.CacheStorageService;
import org.zhuduan.cache.storage.impl.guava.CacheStorageServiceExpireGuavaImpl;
import org.zhuduan.cache.storage.impl.guava.CacheStorageServiceOriginGuavaImpl;
import org.zhuduan.cache.storage.impl.local.CacheStorageServiceLocalImpl;
import org.zhuduan.cache.storage.impl.redis.CacheStorageServiceRedisImpl;
import org.zhuduan.utils.CacheException;
import org.zhuduan.utils.Log4jUtil;

import redis.clients.jedis.JedisCluster;


/***
 * 
 * 
 * 
 * @author	zhuhaifeng
 * @date	2017年2月24日
 *
 */
public class SimpleCacheFactory {
	
	private static final Logger 			cacheLog 	= 	Log4jUtil.cacheLog;	
	
	// TODO:inner object，直接采用静态类的方式加载起来（会造成一定的内存浪费，之后可以通过静态内部类之类的改进）
	// but 用于承载实际实现缓存的类（但是factory中需要根据配置来选择使用存储结构）
	@SuppressWarnings("unused")
	private static final SimpleCacheAspect	cacheAspect	=	new SimpleCacheAspect();		
	

	private volatile boolean 		useLocalCache	=	false;		// 使用的是否是本地缓存？（推荐有限使用在线缓存如Redis等）
	
	private volatile boolean 		useGuava		=	false;		// 本地缓存是否使用guava
	
	private volatile boolean 		useGuavaOrigin	=	false;		// 是否直接使用原生的guava（key和value都是weakReference，但是不支持差异化的expiretime）
	
	private volatile JedisCluster	jedisCluster	=	null;		// 可以使用的JedisCluster（如果没有则会选择其他方式）
	
	
	/***
	 * 实际的构造器： 会根据不同properities参数来装配不同的Storage实现、
	 * 				由于核心的SimpleCacheAspect类采用了static的方式来声明，这里不需要将factory类声明成单例模式
	 * 				（当然，由于Spring的默认IOC模式为单例模型，因此这里并不冲突）
	 * 
	 */
	public SimpleCacheFactory(Boolean useLocalCache, Boolean useGuava, Boolean useGuavaOrigin, JedisCluster jedisclustr){
		// 从配置文件中读入配置参数
		this.useLocalCache = useLocalCache;
		this.useGuava = useGuava;
		this.useGuavaOrigin = useGuavaOrigin;
		this.jedisCluster = jedisclustr;
		
		// 进行初始化
		initial();
	}	
	
	
	public SimpleCacheFactory(Boolean useLocalCache, Boolean useGuava, Boolean useGuavaOrigin){
		// 从配置文件中读入配置参数
		this.useLocalCache = useLocalCache;
		this.useGuava = useGuava;
		this.useGuavaOrigin = useGuavaOrigin;
		
		// 进行初始化
		initial();
	}
	
	
	public SimpleCacheFactory(JedisCluster jedisclustr){
		// 从配置文件中读入配置参数
		this.jedisCluster = jedisclustr;
		
		// 进行初始化
		initial();
	}
	
	
	public SimpleCacheFactory(){		
		// 进行初始化
		initial();
	}
	
	
	// 完成对参数的初始化
	private void initial(){
		// 根据不同的参数进行cacheAspect中的storage装配 --- 采用策略模式
		// 		重复调用该构造参数，可能造成storage实现更换导致的部分缓存数据丢失
		// 		（不过在该场景下问题不大，一般只会在spring注入时一起完成构造）
		synchronized (SimpleCacheFactory.class) {
			CacheStorageService cacheStorageService = null;			
			// 如果采用本地缓存方案
			if ( true == useLocalCache ){
				if ( true == useGuava ){
					// 使用原生Guava方案
					if ( true == useGuavaOrigin ){
						//TODO: 做一个带有重载参数的
						cacheStorageService = CacheStorageServiceOriginGuavaImpl.getInstance();
						cacheLog.info("采用了原生的GuavaCache方案");
					}
					
					// 使用可以支持不同Expire策略的Guava方案
					cacheStorageService = CacheStorageServiceExpireGuavaImpl.getInstance();
					cacheLog.info("采用了定义expireTime的GuavaCache方案");
				} else {				
					// 使用默认的本地缓存方案
					cacheStorageService = CacheStorageServiceLocalImpl.getInstance();
					cacheLog.info("不使用Guava，采用了默认的LocalImpl方案");
				}
			}
			
			// 不采用本地方案，则顺序去遍历各种客户端：
			//		Redis > Memcache(未实现) > others(未实现) > default
			// Redis的装配
			else{
				if ( jedisCluster != null ){			
					try {
						cacheStorageService = CacheStorageServiceRedisImpl.getInstance(jedisCluster);
					} catch (CacheException e) {
						cacheStorageService = CacheStorageServiceLocalImpl.getInstance();
						cacheLog.error("捕获到jedisCluster为空，退化为默认的LocalImpl方案");
					}
				}
			
				// TODO: MemCache的装配
				//		 暂未实现
			}
			
			// 最后做重复检查，如果都没有匹配到，则采用默认的本地实现
			if ( null == cacheStorageService){
				cacheStorageService = CacheStorageServiceLocalImpl.getInstance();
				cacheLog.info("非本地缓存实例未找到，采用了默认的LocalImpl方案");
			}
			
			// 将cacheStorageService注入到实际使用的SimpleCacheAspect类中去
			SimpleCacheAspect.setCacheStorageService(cacheStorageService);
			cacheLog.info("完成对SimpleCacheAspect中CacheStorageService的注入~");
		}
	}
	
	
	//getter & setter
	public boolean isUseLocalCache() {
		return useLocalCache;
	}

	public void setUseLocalCache(boolean useLocalCache) {
		this.useLocalCache = useLocalCache;
	}

	public boolean isUseGuava() {
		return useGuava;
	}

	public void setUseGuava(boolean useGuava) {
		this.useGuava = useGuava;
	}

	public boolean isUseGuavaOrigin() {
		return useGuavaOrigin;
	}

	public void setUseGuavaOrigin(boolean useGuavaOrigin) {
		this.useGuavaOrigin = useGuavaOrigin;
	}

	public JedisCluster getJedisCluster() {
		return jedisCluster;
	}

	public void setJedisCluster(JedisCluster jedisCluster) {
		this.jedisCluster = jedisCluster;
	}
}
