package org.zhuduan.utils;

import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.SerializeWriter;
import com.alibaba.fastjson.serializer.SerializerFeature;

/***
 * 
 * 用于处理序列化和反序列化的功能类
 * 采用的是fastJson的工具类
 * 
 * 注意：序列化和反序列化需要使用同一种工具，不然会存在问题
 * 
 * @author	zhuhaifeng
 * @date	2017年1月16日
 *
 */
public class SerializeUtils {

	/**
	 * 
	 * 将缓存数据序列化成String
	 * 
	 * @param cacheObject
	 * @return
	 */
	public static final String serialize(final Object cacheObject){
		SerializeWriter writer = new SerializeWriter();
		try {
			JSONSerializer serializer = new JSONSerializer(writer);
			
			serializer.config(SerializerFeature.SkipTransientField, false);
			serializer.config(SerializerFeature.WriteClassName, true);
			
			serializer.write(cacheObject);
			return writer.toString();
		} finally {
			// try-finally 主要是用于将writer进行关闭
			//			      并不会实际上catch到Exception
			writer.close();
		}
	}
	
	
	/**
	 * 缓存数据反序列化
	 * 
	 * @param cacheValue
	 * @param clazz
	 * @return
	 */
	public static final <T> T deserialize(String cacheValue, Class<T> clazz) {
        return com.alibaba.fastjson.JSON.parseObject(cacheValue, clazz);
    }	
}
