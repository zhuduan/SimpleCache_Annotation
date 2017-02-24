# SimpleCache_Annotation
基于AOP注解实现的缓存插件
在需要缓存的地方使用@SimpleCache注解即可实现缓存

---

##为什么做这个
在生产环境中，经常会用到缓存相关的东西。

为了最快速和方便的使用缓存，我们引入了通过AOP和Annotation的方式，想以最简单的方式来实现自己个性化的缓存需求（在自己的生产环境中已经引入了类似的机制）。

这个插件的优点主要可以体现在：

1. 唯快不破：快速上手（无需了解细节），只需要在需要的地方使用@SimpleCache(key,expireTime)在方法上进行注解，即可以实现缓存
2. 可变的缓存存储介质，可以选择如Redis、guavaCache、hashMap等多种实现，且通过xml的配置构造器来进行选择
3. 对guava cache进行了一定的封装，使得可以针对不同的key，实现不同的expire设置（为了保持guava cache的性能和优点，也提供了完全使用guava cache，仅做了AOP封装的实现）

---

##快速上手
1. 引入SimpleCache_Annotation包

2. 在application.xml中进行配置   （如下所示：）
```
<!-- 为了通过Spring AOP找到织入点，需要扫描插件包来装配@Aspect类  -->
<aop:aspectj-autoproxy proxy-target-class="true" />
<context:annotation-config />
<context:component-scan base-package="org.zhuduan.cache" />

<!-- 用户配属缓存插件 -->
<bean id="SimpleCacheFactory" class="org.zhuduan.cache.SimpleCacheFactory">
  <constructor-arg index="0" value="true"></constructor-arg><!-- useLocalCache  -->
  <constructor-arg index="1" value="true"></constructor-arg><!-- useGuava  -->
  <constructor-arg index="2" value="true"></constructor-arg><!-- useGuavaOrigin  -->
  <constructor-arg index="3" ref="jedisClusterCache"></constructor-arg><!-- jedisCluster  -->		
</bean>
```

3. 在需要缓存的地方使用 **@SimpleCache(300)**   （其中300是过期的秒数,具体如下所示：）
```
@SimpleCache(300)
public Integer getSomething(String arg){
  // do the real action
  return xxx;
}
```

---

##实现原理
1. （核心）通过@SimpleCache注解来标注切点，从而实现以AOP的方式，在实际数据存储前通过缓存来获取对应值
2. 通过CacheStorageService接口来定义了实际存储过程中需要涉及的方法（根据需求，可以选择不同的存储实现）
3. 通过Factory类的不同构造方法（application.xml中配置），来实现存储实现的选取

---

##框架设计
//TODO: 生成结构图

---

##CacheStorage的不同实现
目前主要提供了三种存储实现：
1. 基于Redis的实现（推荐）
2. 基于Guava Cache的实现
3. 基于ConcurrentHashMap的实现

###基于Redis的实现
- 优点： 利用了Redis的特性，能够很好的支持集群，并提供Master/Slave、持久化等，性能非常好，且完全不影响服务器本身的运行
- 不足:  需要提供和配属相关的Redis机器集群，并引入Jedis包
- 备注： 是生产环境推荐的存储实现，Redis的优点很多，而且性能和稳定性都是非常有保证的。

###基于Guava Cache的实现
- 优点: Guava Cache的性能很好，是本地化缓存中使用非常广泛的一个方案，而且类似于ConcurrentHashMap的分段锁等也极大的提高了并发性（还提供了内存刷新，读取保护等防止了缓存击穿、脏读等问题），同时还有多种清理机制（LRU、GC主动释放、最大Object数目限制）在内存有限时能够很好的避免对原程序产生不良影响。
- 不足： 因为是本地缓存方案，不可避免性能比不上Redis的实现，且无法很好的支持集群，而且需要引入Guava包。对Guava的封装不是很灵活，如采用了固定的Reference方式、最大Object数目等（TODO:这一点可以后面改进）
- 备注： 为了同时保持扩展性，一共提供了两个实现：
  - 1. 直接在Guava Cache的基础上实现了封装，基本就完全使用的Guava Cache（暂未提供可变的输出参数配置），**无法实现差异化的过期时间** 设置（默认Access和Write的过期时间都是固定值）
  - 2. 在Guava Cache的基础上，将缓存值包装成了CacheInfoModel类，从而能够实现 **差异化的过期时间** 配置

###基于ConcurrentHashMap的实现
- 优点： 无第三方包，实现简单，可以实现 **差异化的过期时间配置** ，且通过SoftReference来保证原程序的健壮性
- 不足： 会有一个守护线程定时清理死数据（因此有一定的消耗，但是理论上这个消耗很小，而且间隔周期很大），由于基于ConcurrentHashMap可能在极端场景存在一定的脏读（但是在一般的缓存业务场景下认为是可以接受的），而且Iterator的弱一致性虽然保证了高效的清理但是可能高并发写场景会带来部分数据清理的延迟性
- 备注： 是在发生异常场景和缺省构造参数情况下的默认实现


---

##TODO List
1. 考虑为了更好的实现易用性，需要将插件打release包到maven中央库
2. 部分Storage的实现需要更丰富的测试和性能分析（目前生产环境还是以Redist的使用为主）
3. 考虑支持Memcache的实现

---

##写在最后的废话
希望这个插件能够帮助大家更快更好的完整项目中的一些需求，从而尽早下班，回家吃饭 :)

同时也希望有兴趣的同学多多交流，大神多给意见 ಥ_ಥ

have fun~
