package org.zhuduan.cache.storage.impl.guava;

import org.zhuduan.cache.storage.CacheStorageService;

import com.google.common.cache.Cache;

public class CacheStorageServiceExpireGuavaImpl implements CacheStorageService {

	
	@Override
	public String getCache(String cacheKey) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean setCache(String cacheKey, String cacheValue, int expireTimeSeconds) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean isCacheKeyExists(String cacheKey) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean deleteCache(String cacheKey) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Long incrCacheKey(String cacheKey, long incrStep, int expireTimeSeconds) {
		// TODO Auto-generated method stub
		return null;
	}

}
