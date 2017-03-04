package org.zhuduan.utils;

/***
 * 
 * 自定义的错误类型，用于在出错时抛出特性错误，供上层程序处理
 * 		用户可以捕获该种类型的错误做一些自己的个性化处理
 * 
 * @author	zhuhaifeng
 * @date	2017年3月4日
 *
 */
public class CacheException extends Exception {
	
	private static final long	serialVersionUID 	=	520001L;	
	 
	private int errCode;
	 
	private String errMessage;
	 
	 
	public CacheException(int errCode, String errMessage){
		super(errMessage);
		this.errCode = errCode;
		this.errMessage = errMessage;
	}
	
	
	@Override
	public String toString(){
		return ("#Error " + errCode + " : " + errMessage);
	}


	public int getErrCode() {
		return errCode;
	}


	public void setErrCode(int errCode) {
		this.errCode = errCode;
	}


	public String getErrMessage() {
		return errMessage;
	}


	public void setErrMessage(String errMessage) {
		this.errMessage = errMessage;
	}
}
