package org.zhuduan.cache;

import org.apache.log4j.Logger;
import org.zhuduan.utils.Log4jUtil;

import redis.clients.jedis.JedisCluster;

public class SimpleCacheFactory {
	
	private static final Logger 			cacheLog 	= 	Log4jUtil.cacheLog;	
	private static final Logger 			svcLog 		= 	Log4jUtil.svcLog;	
	private static final Logger 			sysLog 		= 	Log4jUtil.sysLog;	
	
	// inner object，用于承载实际实现缓存的类（但是factory中需要根据配置来选择使用存储结构）
	private static final SimpleCacheAspect	cacheAspect	=	new SimpleCacheAspect();		
	

	private boolean 		useLocalCache	=	false;		// 使用的是否是本地缓存？（推荐有限使用在线缓存如Redis等）
	
	private boolean 		useGuava		=	false;		// 本地缓存是否使用guava
	
	private boolean 		useGuavaOrigin	=	true;		// 是否直接使用原生的guava（key和value都是weakReference，但是不知道差异化的expiretime）
	
	private JedisCluster	jedisCluster	=	null;		// 可以使用的JedisCluster（如果没有则会选择其他方式）
	
	
	/***
	 * 实际的构造器： 会根据不同properities参数来装配不同的Storage实现、
	 * 				由于核心的SimpleCacheAspect类采用了static的方式来声明，这里不需要将factory类声明成单例模式
	 * 				（当然，由于Spring的默认IOC模式为单例模型，因此这里并不冲突）
	 * 
	 */
	public SimpleCacheFactory(){
		//TODO: 根据不同的参数进行cacheAspect中的storage装配
		//		重复调用该构造参数，可能造成storage实现更换导致的部分缓存数据丢失（不过在该场景下问题不大）
		synchronized (SimpleCacheFactory.class) {
			
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
