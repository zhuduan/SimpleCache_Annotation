package org.zhuduan.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

/***
 * 
 * 用于选择日志类型
 * 并提供相关日志记录工具的功能类
 * 
 * 
 * @author	zhuhaifeng
 * @date	2017年2月16日
 *
 */
public class Log4jUtil {
	
	public static final Logger	sysLog		= Logger.getLogger("SYS");				// 系统日志, 记录异常信息;	
	public static final Logger	appLog		= Logger.getLogger("APP");				// 应用日志, 记录接口的访问信息;	
	public static final Logger	svcLog		= Logger.getLogger("SVC");				// 服务日志, 记录服务日志的情况;	
	public static final Logger	errorLog	= Logger.getLogger("ERROR");			// 应用日志, 记录接口的访问信息;	
	public static final Logger	cacheLog	= Logger.getLogger("CACHE");			// 缓存日志，记录缓存操作异常信息


	/**
	 * 
	 * 通过logName来获取对应的Logger
	 * 
	 * @param logName
	 * @return
	 */
	public static Logger getLogger(String logName) {
		if (logName == null || logName.trim().length() == 0) {
			return null;
		}
		try {
			return Logger.getLogger(logName);
		} catch (Exception excp) {
			excp.printStackTrace();
			Log4jUtil.errorLog.error(excp);
		}
		return null;
	}

	
	/***
	 *
	 * 获取调用方法的相关信息
	 * 类名#方法名(不带形参列表)#代码行数	
	 * 
	 * @return 
	 * 		1.调用此方法的 类名#方法名(不带形参列表)#代码行数， 
	 * 		2.or EMPTY String if exception occur
	 */
	public static String getCallLocation() {
		try {
			StackTraceElement ste = Thread.currentThread().getStackTrace()[2];
			return ste.getClassName() + "#" + ste.getMethodName() + "#" + ste.getLineNumber();
		} catch (Exception e) {
			return StringUtils.EMPTY;
		}
	}
	
	
	/**
	 * 
	 * 获取异常发生处的 EXCEPTION 信息
	 *     信息组成为: e.getMessage() ON 类名#方法名(不带形参列表)#代码行数	    
	 *     
	 * @param Exception
	 * @return 异常信息，or EMPTY String if exception occur
	 * 
	 */
	public static String getExceptionLocation(Exception exception) {
		try {
			StackTraceElement ste = exception.getStackTrace()[0];
			StringBuilder message = new StringBuilder(128);
			message.append("EXCEPTION : ").append(exception.getMessage())
				   .append(" ON ").append(ste.getClassName())
				   .append("#").append(ste.getMethodName())
				   .append("#").append(ste.getLineNumber());
			return message.toString();
		} catch (Exception ex) {
			return StringUtils.EMPTY;
		}
	}
	
	
	// 用于测试的主方法
	public static void main(String[] args) {
		try {
			throw new Exception("this is a test exception");
		} catch (Exception e) {
			System.out.println(getCallLocation());
			System.out.println(getExceptionLocation(e));
		}
	}
}
