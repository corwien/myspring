## 手写Spring框架学习笔记

在学习完Spring框架之后, 我们知道了 Spring容器, IOC, AOP, 事务管理, 
SpringMVC 这些Spring的核心内容, 也知道了它们底层实现的基本原理, 
比如Spring容器就是个Map映射, IOC底层就是反射机制, AOP底层是动态代理, 
SpringMVC不就是对Servlet进行了下封装嘛! 哈哈, 当然这些只是些皮毛, Spring除此之外还有更加复杂的设计,
但我们完全可以抛弃那些复杂的设计, 通过这些底层原理自己来写个Spring框架. 写完之后, 
相信我们会对Spring框架有个更加深刻的理解。

相关文档：
手写Spring框架学习笔记(跑沽学院)：https://www.cnblogs.com/linlf03/p/9995135.html
手写Spring框架(AOP,事务管理) https://blog.csdn.net/litianxiang_kaola/article/details/86646947