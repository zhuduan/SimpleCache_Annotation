package org.zhuduan.cache.test;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Service;
import org.zhuduan.cache.SimpleCache;
import org.zhuduan.cache.SimpleCacheFactory;


/***
 * 
 * 用于测试缓存的类 
 * 
 * @author	zhuhaifeng
 * @date	2017年2月17日
 *
 */
@Service
public class CacheMain {

	public static void main(String[] args) throws InterruptedException {
		@SuppressWarnings("resource")
		AbstractApplicationContext cxt = new ClassPathXmlApplicationContext("applicationContext.xml");
		CacheMain service = (CacheMain) cxt.getBean(CacheMain.class);  
		SimpleCacheFactory factory = cxt.getBean(SimpleCacheFactory.class);  
		
		System.out.println("isUseGuava "+factory.isUseGuava());
		
		System.out.println("start!");
		for(int i=0;i<6;i++){
			service.testCache();
			Thread.sleep(1000);
		}
	}
	
	@SimpleCache(expire=2)
	public String testCache(){
		return "hello world";
	}

}
