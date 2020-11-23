# tomcat-source
运行tomcat源码

> 版本：8.5.60

# 运行前准备

1. 添加 pom.xml 在里面加入tomcat源码中缺少的jar包
2. 根路径创建 logs 文件夹
3. 添加配置文件夹 conf（tomcat源码中的conf文件夹）。默认应该放在项目的根路径下。
4. 根路径创建 lib 文件夹，解决运行时报错：警告: Problem with directory [/Users/xxxxx/workspace/xxxxx/tomcat-source/lib], exists: [false], isDirectory: [false], canRead: [false]
5. 在 pom.xml 的 build 中加入如下配置，保证可以正常加载java中的配置文件
    ```xml

        <resources>
            <resource>
                <directory>src/main/java</directory>
            </resource>
        </resources>
    ```
6. 解决启动日志乱码问题，在 org.apache.tomcat.util.res.StringManager 第 135 行增加如下代码。原因是：在java中, 读取文件的默认格式是iso8859-1, 而我们中文存储的时候一般是UTF-8
    > 参考文章：
    > - https://blog.csdn.net/zhoutaoping1992/article/details/104751705/
    > - https://www.jianshu.com/p/24483c3fc58c
    
   ```
   org.apache.tomcat.util.res.StringManager.getString(java.lang.String) 方法
   if (bundle != null) {
       str = bundle.getString(key);
       // 增加此段，解决启动时，控制台乱码问题
       str = new String(str.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
   }
   
   
   org.apache.jasper.compiler.Localizer.getMessage(java.lang.String) 方法
   if (bundle != null) {
       errMsg = bundle.getString(errCode);
       // 增加此段，解决 50x 错误页面乱码问题
       errMsg = new String(errMsg.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
   }
   ```
7. 解决无法解析jsp页面问题
   ```
   org.apache.catalina.startup.ContextConfig.configureStart 方法中，找到 webConfig(); 在此下加入下方代码：
   
   context.addServletContainerInitializer(new JasperInitializer(), null);
   ```

## 启动流程
详见：/doc/architecture/startup.html 或者 官方文档：https://tomcat.apache.org/tomcat-8.5-doc/architecture/startup.html。下面的内容是从文档中翻译，并加了一下我的理解。

项目从 org.apache.catalina.startup.Bootstrap.main 方法启动，启动流程可以分为两个大部分

1. 使用 org.apache.catalina.startup.Bootstrap 进行初始化操作
    1. 初始化 classLoaders。commonLoader (common)、catalinaLoader(server)、sharedLoader (shared)
    2. 当前线程的 contextClassloader 设置为 catalinaLoader
    3. （反射）加载启动类（org.apache.catalina.startup.Catalina）
    4. （反射）Catalina.parentClassLoader 设置为 sharedLoader
    5. catalinaDaemon 设置为 Catalina 对象
2. 守护进程设置为 Bootstrap 对象
3. 使用守护进程，处理命令行参数
    ```
   daemon.setAwait(true);
   daemon.load(args);
   daemon.start();
   ```
   1. daemon.load(args); -> （反射）调用 Catalina.load();
        1. 加载 server.xml
   2. daemon.start(); -> （反射）调用 Catalina.start();
        1. 处理默认的web.xml（conf/web.xml），然后处理应用程序web.xml（WEB-INF/web.xml）
        2. 开启最多 2 个轮训线程，详见：org.apache.tomcat.util.net.NioEndpoint.startInternal 中的代码 `pollers = new Poller[getPollerThreadCount()];` 【无限循环】
        2. 调用 org.apache.tomcat.util.net.AbstractEndpoint.startAcceptorThreads 开启 server.xml 中配置的 acceptorThreadCount 数量的 Acceptor 线程，用来监听 TCP 连接【无限循环】
        3. Tomcat receives a request on an HTTP port。最终在 Acceptor 中阻塞到代码 `socket = serverSock.accept();` 部分。Acceptor 根据 server.xml 配置决定，默认是 NioEndpoint 类中的 Acceptor 的对象。 

# NIO
## 接收请求的流程
1. 进入 Accept 的 run 方法
    1. 从 `SynchronizedStack<NioChannel> nioChannels;` （栈大小：128）获取缓存的 NioChannel，没有就创建一个。
    1. 在 NioChannel 中绑定一个 Poller
    1. 创建 NioSocketWrapper 对象，并绑定到 NioChannel 中
    1. 从 `SynchronizedStack<PollerEvent> eventCache` （栈大小：128）获取缓存的 PollerEvent，没有就创建一个。
    1. 将 PollerEvent 对象放到队列中，等待 Poller 依次取出调用。
1. 通过方法 countUpOrAwaitConnection 判断是否达到最大链接（server.xml 中配置的 maxConnections），如果到了就等待。

1. Poller 从 `SynchronizedQueue<PollerEvent> events` 中取出每一个任务，调用 `org.apache.tomcat.util.net.AbstractEndpoint.processSocket` 放入到 tomcat 自定义的 ThreadPoolExecutor线程池中等待执行。

# NIO2
## 接收请求的流程
1. 进入 Accept 的 run 方法
    1. 从 `SynchronizedStack<Nio2Channel> nioChannels;` （栈大小：128）获取缓存的 Nio2Channel，没有就创建一个。
    1. 创建 Nio2SocketWrapper 对象，并绑定到 Nio2Channel 中
    1. 从 `SynchronizedStack<SocketProcessorBase<S>> processorCache` （栈大小：128）获取缓存的 SocketProcessorBase（实际是 Nio2Endpoint.SocketProcessor），没有就创建一个。
    1. 将 SocketProcessorBase 对象交给 tomcat 自定义的 ThreadPoolExecutor。
1. 通过方法 countUpOrAwaitConnection 判断是否达到最大链接（server.xml 中配置的 maxConnections），如果到了就等待。

1. Poller 从 `SynchronizedQueue<PollerEvent> events` 中取出每一个任务，调用 `org.apache.tomcat.util.net.AbstractEndpoint.processSocket` 放入到 tomcat 自定义的 ThreadPoolExecutor线程池中等待执行。

## tomcat 自定义的 ThreadPoolExecutor 线程池
**特别说明，tomcat的 ThreadPoolExecutor 和 java的 ThreadPoolExecutor 在入队操作中有一定的区别，所以tomcat会优先将线程提到最大，然后才会考虑入队。** 具体说明如下：
> 详见代码：org.apache.tomcat.util.threads.TaskQueue.offer 部分
1. 继承了 java 的 ThreadPoolExecutor
2. 自定义了 TaskQueue 继承了 LinkedBlockingQueue，并重写了 offer、poll 方法
3. 提交任务时（execute），会执行如下操作：
    1. 如果 当前线程数 小于 核心线程数，启动一个新线程
    2. 将新任务直接放入队列中，如果入队失败，则尝试启动新线程
    3. 如果启动新线程失败，则拒绝此任务 
4. 将任务放入队列时，具体操作如下：
    1. 如果 线程池为空，则将入队操作交给 LinkedBlockingQueue.offer
    2. 如果 线程池当前线程数 达到了最大线程，则将入队操作交给 LinkedBlockingQueue.offer
    3. 如果 [有空闲线程] 当前提交的任务总数（每次提交任务后，会在 execute 方法中将此数 +1） 小于等于 线程池当前的线程数，则将入队操作交给 LinkedBlockingQueue.offer
    4. 如果 线程池当前线程数 小于 最大线程，则返回 false。**这里是区别，这个写法，会把新的任务直接交给新线程执行**
    5. 其他情况，则将入队操作交给 LinkedBlockingQueue.offer