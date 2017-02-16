package org.zhuduan.cache;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/***
 * 
 * SimpleCache的注解, 需要Cache的时候放一个此注解到方法之上
 * 
 * 1. 此方法必须是public的(cglib是extends目标类进行代理);
 * 2. 此方法必须有返回值(非void)
 * 3. 如果是类的内部方法调用缓存方法, 必须实现MochaBeanSelfAware接口, 并使用selfSpringProxy进行内部缓存方法调用
 * 
 * @author	zhuhaifeng
 * @date	2017年2月16日
 * 
 */
@Inherited
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SimpleCache {
	
    /**
     * 缓存中的key
     * 如果为空(NULL | ""), 使用@Cache注解的类名 & 方法名 & 参数生成
     * 
     * @return
     */
    String key() default "";
	
    /**
     * 缓存时间, 单位秒! 默认60秒
     * 
     * @return
     */
    int expire() default 60;    
}