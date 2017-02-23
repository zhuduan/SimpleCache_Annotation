package org.zhuduan.config;

public class SimpleCacheConfig {

	public static final int 	GUARD_THREAD_SLEEP_SECONDS				=	3600;			// 守护线程的sleep时间（不用太过频繁，因为这里主要是清理一些长期不同的对象）
	public static final long	OBJ_CLEAN_THREDHOLD						=	10000000;		// 需要开始清理的阈值（小于该阈值则不用开始清理： 可选）	
	
	
	public static final long 	ORIGIN_GUAVACACHE_OBJECT_NUM_MAX		=	1000000000L;		// 可以缓存的最大个数，默认 1亿个
	public static final long 	ORIGIN_GUAVACACHE_ACCESS_EXPIRE_SECONDS	=	3600L;				// 缓存的Access过期时间，默认 3600s
	public static final long 	ORIGIN_GUAVACACHE_WRITE_EXPIRE_SECONDS	=	3600L;				// 缓存的Write过期时间，默认 3600s
	
	
	public static final long 	EXPIRE_GUAVACACHE_OBJECT_NUM_MAX		=	1000000000L;		// 可以缓存的最大个数，默认 1亿个
	
}
