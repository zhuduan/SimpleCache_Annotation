package org.zhuduan.utils;


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
