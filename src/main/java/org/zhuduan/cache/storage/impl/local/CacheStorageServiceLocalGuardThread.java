package org.zhuduan.cache.storage.impl.local;

import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.zhuduan.model.CacheInfoModel;
import org.zhuduan.utils.Log4jUtil;

import com.google.common.base.Strings;


/***
 * 
 * 声明为守护线程：  用于周期性清理cacheMap中的过期数据
 * 
 * 		得益于ConcurrentHashMap的弱一致性，可以较容易的不影响正常使用的情况下进行清理
 * 		设计上为了保证LocalImpl的访问性，这里通过反射来获取map对象
 * 		（不直接声明在LocalImpl里是考虑到要升级为守护线程，而且更好实现细节的隐藏） 		
 * 
 */
public class CacheStorageServiceLocalGuardThread extends Thread {
	
	private static final Logger		sysLog		=	Log4jUtil.sysLog;		// 系统日志
	private static final Logger		svcLog		=	Log4jUtil.svcLog;		// service日志	
	
	private static final int 	SLEEP_SECONDS		=	3600;			// 周期性执行线程（不用太过频繁，因为这里主要是清理一些长期不同的对象）
	@SuppressWarnings("unused")
	private static final long	OBJ_CLEAN_THREDHOLD	=	10000000;		// 需要开始清理的阈值（小于该阈值则不用开始清理： 可选）	

	private boolean isClean = true;			// 用于标识守护线程是否周期性对Map进行清理
	
	private ConcurrentHashMap<String, SoftReference<CacheInfoModel>> cacheMap = null;		// 指向localImpl中的Map
	
	
	public CacheStorageServiceLocalGuardThread(){
		initial();
	}
	
	
	@Override
	public void run(){
		// 如果initial失败，或者设置为不启动，则直接略过不执行清除方法
		if (false == isClean){
			return;
		}
		
		while(true==isClean){			
			try {
				// 作出清理的动作
				svcLog.info("this time clean obj num : " +removeExpireObj());
				Thread.sleep(SLEEP_SECONDS * 1000L);
			} catch (Exception exception) {
				// 如果出错，则需要catch到错误，避免影响主流程
				sysLog.error(Log4jUtil.getCallLocation() + " run method fail for : " + exception.getMessage());
			}
		}
	}
	
	private Integer removeExpireObj(){
		if(cacheMap==null || cacheMap.isEmpty()){
			return 0;
		}
		
		// 完成周期性删除过期的元素
		// 利用了concurrentHashMap的Iterator弱一致性
		// Iterator的性能没有Entry的高，但是可以相对而言做二次校验，因此采用Iterator
		int cleanObjCount = 0;
		Iterator<String> keys = cacheMap.keySet().iterator();
		while(keys.hasNext()){
			String tempKey = keys.next();
			
			// 空字串的值不应该存在，可能存在某些错误，但是为了健壮性直接继续
			if(Strings.isNullOrEmpty(tempKey)){				
				svcLog.warn(" map has emptyStr ");
				continue;
			}
			
			SoftReference<CacheInfoModel> tempValueReference = cacheMap.get(tempKey);
			if(tempValueReference==null){
				cacheMap.remove(tempKey);
				svcLog.info("expire clean for " + tempKey);
				cleanObjCount++;
			}
			
			CacheInfoModel tempValue = tempValueReference.get();
			if ( tempValue==null 
				 || tempValue.getCacheValue()==null 
				 || tempValue.getCacheBeginTimeLong()==null
				 || tempValue.getCacheExpireTimeLong()==null
				 || ((System.currentTimeMillis()-tempValue.getCacheBeginTimeLong())>tempValue.getCacheExpireTimeLong()) )
			{
				cacheMap.remove(tempKey);
				svcLog.info("expire clean for " + tempKey);
				cleanObjCount++;
			}
		}		
		
		// 返回最后清楚的数据总量
		return cleanObjCount;
	}
	
	/**
	 * 1.将这里使用的Map通过反射指向localImpl里面的cacheMap
	 * 
	 */
	@SuppressWarnings("unchecked")
	private void initial(){
		try {
			Field mapField = CacheStorageServiceLocalImpl.class.getDeclaredField("cacheMap");
			mapField.setAccessible(true);
			this.cacheMap = (ConcurrentHashMap<String, SoftReference<CacheInfoModel>>) mapField.get(CacheStorageServiceLocalImpl.class);
			svcLog.info(Log4jUtil.getCallLocation() + " successfully set map in reflection ");
			isClean = true;
		} catch (Exception exception) {
			// 如果反射出错，则废除守护线程
			isClean = false;
			sysLog.error(Log4jUtil.getCallLocation() + " reflection get map failed for : " + exception.getMessage());
		} 		
	}
	
	
	// 用于测试的main方法
	public static void main(String[] args) throws Exception {
		CacheStorageServiceLocalGuardThread thread = new CacheStorageServiceLocalGuardThread();
		System.out.println("test for removeExpireObj: " + thread.removeExpireObj());
	}
}
