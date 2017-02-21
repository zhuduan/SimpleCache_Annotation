package org.zhuduan.model;

public class CacheInfoModel {

	private long cacheBeginTimeLong;
	
	private long cacheExpireTimeLong;

	public long getCacheBeginTimeLong() {
		return cacheBeginTimeLong;
	}

	public void setCacheBeginTimeLong(long cacheBeginTimeLong) {
		this.cacheBeginTimeLong = cacheBeginTimeLong;
	}

	public long getCacheExpireTimeLong() {
		return cacheExpireTimeLong;
	}

	public void setCacheExpireTimeLong(long cacheExpireTimeLong) {
		this.cacheExpireTimeLong = cacheExpireTimeLong;
	}	
}
