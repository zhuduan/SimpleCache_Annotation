package org.zhuduan.cache;

import java.lang.reflect.Method;
import java.util.Collections;

import org.apache.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.zhuduan.cache.storage.CacheStorageService;
import org.zhuduan.cache.storage.impl.guava.CacheStorageServiceExpireGuavaImpl;
import org.zhuduan.cache.storage.impl.guava.CacheStorageServiceOriginGuavaImpl;
import org.zhuduan.cache.storage.impl.local.CacheStorageServiceLocalImpl;
import org.zhuduan.cache.storage.impl.redis.CacheStorageServiceRedisImpl;
import org.zhuduan.utils.CacheException;
import org.zhuduan.utils.Log4jUtil;
import org.zhuduan.utils.SerializeUtils;

import redis.clients.jedis.JedisCluster;


/***
 * 
 * 配置@SimpleCache 注解的切面, 在方法上使用了@SimpleCache表示就使用了该切面 
 * 切面使用了Around的方式
 * 
 * 
 * @author	zhuhaifeng
 * @date	2017年2月16日
 *
 */
@Aspect
@Component
public class SimpleCacheAspect {
	
	private static final Logger cacheLog = Log4jUtil.cacheLog;	
	
	private volatile boolean 		useLocalCache	=	false;			// 使用的是否是本地缓存？（推荐有限使用在线缓存如Redis等）
	
	private volatile boolean 		useGuava		=	false;			// 本地缓存是否使用guava
	
	private volatile boolean 		useGuavaOrigin	=	false;			// 是否直接使用原生的guava（key和value都是weakReference，但是不支持差异化的expiretime）
	
	private volatile JedisCluster	jedisCluster	=	null;			// 可以使用的JedisCluster（如果没有则会选择其他方式）
	
    private static CacheStorageService cacheStorageService;				// 实际上用于缓存存储的实例类            
    
    
    /***
	 * 实际的构造器： 会根据不同properities参数来装配不同的Storage实现
	 * 
	 */
	public SimpleCacheAspect(Boolean useLocalCache, Boolean useGuava, Boolean useGuavaOrigin, JedisCluster jedisclustr){
		this.useLocalCache = useLocalCache;
		this.useGuava = useGuava;
		this.useGuavaOrigin = useGuavaOrigin;
		this.jedisCluster = jedisclustr;
		
		initial();
	}	
	
	
	public SimpleCacheAspect(Boolean useLocalCache, Boolean useGuava, Boolean useGuavaOrigin){
		this.useLocalCache = useLocalCache;
		this.useGuava = useGuava;
		this.useGuavaOrigin = useGuavaOrigin;
		
		initial();
	}
	
	
	public SimpleCacheAspect(JedisCluster jedisclustr){
		this.jedisCluster = jedisclustr;
		
		initial();
	}
	
	
	public SimpleCacheAspect(){		
		initial();
	}
	
	
	// 完成对参数的初始化
	private void initial(){
		// 根据不同的参数进行cacheAspect中的storage装配 --- 采用策略模式
		synchronized (SimpleCacheAspect.class) {
			// 如果采用本地缓存方案
			if ( true == useLocalCache ){
				if ( true == useGuava ){
					if ( true == useGuavaOrigin ){
						//TODO: 做一个带有重载参数的
						cacheStorageService = CacheStorageServiceOriginGuavaImpl.getInstance();
						cacheLog.info("采用了原生的GuavaCache方案");
					} else{					
						cacheStorageService = CacheStorageServiceExpireGuavaImpl.getInstance();
						cacheLog.info("采用了定义expireTime的GuavaCache方案");
					}
				} else {				
					// 使用默认的本地缓存方案
					cacheStorageService = CacheStorageServiceLocalImpl.getInstance();
					cacheLog.info("不使用Guava，采用了默认的LocalImpl方案");
				}
			}
			
			// 不采用本地方案，则顺序去遍历各种客户端：
			//		Redis > Memcache(未实现) > others(未实现) > default
			else {
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
    
	
	/** 
	 * 以@SimpleCache 注解作为aop切点
	 * 即将所有包含了@SimpleCache注解的地方作为AOP织入点
	 *  
	 */
	@Pointcut("@annotation(org.zhuduan.cache.SimpleCache)")
	public void pointcut(){ 
    }

	
	/***
	 * 切点Around方法实现
	 * 
	 * @param pjp
	 * @return
	 * @throws Throwable
	 */
	@Around("pointcut()")
	public Object doAround(final ProceedingJoinPoint pjp) throws Throwable {
		final long time_1 = System.currentTimeMillis();
		final MethodSignature ms = (MethodSignature) pjp.getSignature();
		final Method method = ms.getMethod();
		final SimpleCache cacheAnnotation = method.getAnnotation(SimpleCache.class);

		// 获取注解信息
		final String cacheKey = generateCacheKey(cacheAnnotation.key(), pjp);
		final int expire = cacheAnnotation.expire();
		final Class<?> cacheClazz = ((MethodSignature) pjp.getSignature()).getReturnType();
		
		final String cacheValue = cacheStorageService.getCache(cacheKey);
		if (cacheValue != null) {
			final Object cacheObj = SerializeUtils.deserialize(cacheValue, cacheClazz);
			final long time_2 = System.currentTimeMillis();
			cacheLog.info("hit cacheKey:" + cacheKey + ", cacheValueAlready:" + cacheValue+", ms:" + (time_2-time_1));
			return cacheObj;
		} 
		
		// 未命中缓存，查询结果，并放到缓存中
		final long time_3 = System.currentTimeMillis();
		final Object dbExecuteValue = pjp.proceed();
		if (dbExecuteValue != null) {
			final long time_4 = System.currentTimeMillis();
			String cacheValueSave = SerializeUtils.serialize(dbExecuteValue);
			
			// 过滤java.util.Collections$EmptyMap、EmptyIterator、EmptyListIterator等
			// 主要是因为序列化的时候会造成问题
			if(cacheValueSave.indexOf("java.util.Collections$Empty") >= 0){
				cacheValueSave = cacheValueSave.replaceAll("java.util.Collections$EmptyListIterator", "java.util.LinkedList$ListItr");
				cacheValueSave = cacheValueSave.replaceAll("java.util.Collections$EmptyMap", "java.util.HashMap");
				cacheValueSave = cacheValueSave.replaceAll("java.util.Collections$EmptyIterator", "java.util.HashMap$KeyIterator");
			}
	
			cacheStorageService.setCache(cacheKey, cacheValueSave, expire);
			final long time_5 = System.currentTimeMillis();
			cacheLog.info("set cacheKey:" + cacheKey+", cacheValueSave:"+cacheValueSave + ", expire s:" + expire 
						+ ", setCache ms:" + (time_5 - time_4) + ", db ms:" + (time_4 - time_3));
		}
		return dbExecuteValue;
	}


	// getter & setter
	public static CacheStorageService getCacheStorageService() {
		return cacheStorageService;
	}

	public static void setCacheStorageService(CacheStorageService cacheStorageService) {
		SimpleCacheAspect.cacheStorageService = cacheStorageService;
	}

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

	public static Logger getCachelog() {
		return cacheLog;
	}


	/**
	 * 用类名、方法名、参数值作为缓存的key
	 * 注意：这里依赖的是参数值的ToString方法，也就是说如果不同参数值的ToString方法如果打印值相关，则会产生冲突
	 * 
	 * @param pjp
	 * @return
	 */
	private static final String generateCacheKey(final String configKey, final ProceedingJoinPoint pjp){
		final Object[] methodArgs = pjp.getArgs();  
		if(configKey!=null && configKey.length()>0){
			return generateCacheKey(configKey, methodArgs);
		}
		StringBuilder sb = new StringBuilder();
		String className = pjp.getTarget().getClass().getSimpleName();  
        String methodName = pjp.getSignature().getName();  
        sb.append("cache.");  //以cache打头!
        sb.append(className);
        sb.append("_");
        sb.append(methodName);
        for(Object arg : methodArgs) {  
            if(arg != null) {  
            	// 参数名依赖于参数的toString方法，如果需要可以重载toString来处理
                sb.append("_").append(arg.toString());  
            }  
        }  
        return sb.toString(); 
	}
	
	
	/**
	 * 用类名、方法名、参数名作为缓存的key
	 * 
	 * @param pjp
	 * @return
	 */
	private static final String generateCacheKey(String configKey, Object[] methodArgs){
		if(methodArgs == null || methodArgs.length==0){
			return configKey;
		}
        StringBuilder sb = new StringBuilder();
        sb.append(configKey);
        for(Object arg : methodArgs) {  
            if(arg != null) {  
                sb.append("_").append(arg.toString());  
            }  
        }  
        return sb.toString();
	}
	
	
	public static void main(String[] args) {
//		List<Integer> list = new ArrayList<>();
//		List<Integer> list2 = (List)Collections.emptyList();
//		System.out.println(Collections.emptyList().getClass().isAssignableFrom(list.getClass()));
//		System.out.println(Collections.emptyList().getClass().isAssignableFrom(list2.getClass()));
		System.out.println(SerializeUtils.serialize(Collections.emptySet()));
		System.out.println(SerializeUtils.serialize(Collections.emptyList()));
		System.out.println(SerializeUtils.serialize(Collections.emptyEnumeration()));
		System.out.println(SerializeUtils.serialize(Collections.emptyIterator()));
		System.out.println(SerializeUtils.serialize(Collections.emptyListIterator()));
		System.out.println(SerializeUtils.serialize(Collections.emptyMap()));
	}
}