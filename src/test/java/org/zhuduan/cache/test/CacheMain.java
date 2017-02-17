package org.zhuduan.cache.test;

import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Service;
import org.zhuduan.cache.SimpleCache;


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

	public static void main(String[] args) {
		AbstractApplicationContext cxt = new ClassPathXmlApplicationContext("applicationContext.xml");
		CacheMain service = (CacheMain) cxt.getBean(CacheMain.class);  
		
		System.out.println("start!");
		for(int i=0;i<3;i++){
			System.out.println(i+": "+service.testCache());
		}
	}
	
	@SimpleCache(expire=60)
	public String testCache(){
		return "hello world";
	}

}
