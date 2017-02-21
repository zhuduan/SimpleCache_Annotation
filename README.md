# SimpleCache_Annotation
基于AOP注解实现的缓存插件，仅需在使用的地方使用@SimpleCache即可实现缓存

---

##为什么做这个
在生产环境中，经常会用到缓存相关的东西。
为了最快速和方便的使用缓存，我们引入了通过AOP和Annotation的方式，想以最简单的方式来实现自己个性化的缓存需求（在自己的生产环境中已经引入了类似的机制）。
这个插件的优点主要可以体现在：
1. 天下武功唯快不破：快速上手（无需了解细节），只需要在需要的地方使用@SimpleCache(key,expireTime)在方法上进行注解，即可以实现缓存
2. 可变的缓存存储，可以选择如Redis、guavaCache、hashMap等多种实现，且通过xml的配置文件进行选择
3. 相对已有的一些基于guava的缓存框架，可以提供不同方法不同key，有不同的expire时间的选择（当然本身guava的cache十分的优秀，本插件的写作过程中也参考了很多，而且提供来一个原生的guavaCache的应用实现）

---

##快速上手

---

##实现原理
1. 通过


##框架设计


##CacheStorage的不同实现

---

##TODO List
1. 考虑为了更好的实现易用性，需要将插件打release包到maven中央库
2. 部分Storage的实现需要更丰富的测试和性能分析（目前生产环境还是以Redist的使用为主）
3. 考虑支持Memecache的实现
4. 本地HashMap的实现，对key和CountDown的支持，并没有Guava中考虑的周全，要多思考完善一下

---

##写在最后的废话
希望这个插件能够帮助大家更快更好的完整项目中的一些需求，从而尽早下班，回家吃饭 :)
同时也希望有兴趣的同学多多交流，大神多给意见，帮助我多学习进步

最后，你都看到这里了，不考虑留下点啥痕迹么 ಥ_ಥ
