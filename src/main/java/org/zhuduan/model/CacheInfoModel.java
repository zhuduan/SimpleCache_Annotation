package org.zhuduan.model;

import java.io.Serializable;

/***
 * 
 * 用于对整个cacheValue做一个封装，增加过期时间等信息从而实现差异化的过期配置
 * 同时在本地内存模型中可以通过SoftReference或者WeakReference来做引用从而增加程序的健壮性
 * 
 * 
 * @author	zhuhaifeng
 * @date	2017年2月23日
 *
 */
public class CacheInfoModel implements Serializable{

	private static final long serialVersionUID = 367498860451494489L;

	private Long cacheBeginTimeLong;		// set缓存的开始时间
	
	private Long cacheExpireTimeLong;		// 该对象的过期时间
	
	private String cacheValue;				// 序列化成String的实际存储对象
	

	public Long getCacheBeginTimeLong() {
		return cacheBeginTimeLong;
	}

	public void setCacheBeginTimeLong(Long cacheBeginTimeLong) {
		this.cacheBeginTimeLong = cacheBeginTimeLong;
	}

	public Long getCacheExpireTimeLong() {
		return cacheExpireTimeLong;
	}

	public void setCacheExpireTimeLong(Long cacheExpireTimeLong) {
		this.cacheExpireTimeLong = cacheExpireTimeLong;
	}

	public String getCacheValue() {
		return cacheValue;
	}

	public void setCacheValue(String cacheValue) {
		this.cacheValue = cacheValue;
	}
}
