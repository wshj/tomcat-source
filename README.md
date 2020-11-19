# tomcat-source
运行tomcat源码

> 版本：8.5.60

# 运行前准备

1. 添加 pom.xml 在里面加入tomcat源码中缺少的jar包
2. 根路径创建 logs 文件夹
3. 添加配置文件夹 conf（tomcat源码中的conf文件夹）。默认应该放在项目的根路径下。
4. 根路径创建 lib 文件夹，解决运行时报错：警告: Problem with directory [/Users/zhqy/workspace/zhqy/tomcat-source/lib], exists: [false], isDirectory: [false], canRead: [false]
5. 在 pom.xml 的 build 中加入如下配置，保证可以正常加载java中的配置文件
    ```xml

        <resources>
            <resource>
                <directory>src/main/java</directory>
            </resource>
        </resources>
    ```
6. 解决启动日志乱码问题，在 org.apache.tomcat.util.res.StringManager 第 135 行增加如下代码。原因是：在java中, 读取文件的默认格式是iso8859-1, 而我们中文存储的时候一般是UTF-8
    > 参考文章：https://blog.csdn.net/zhoutaoping1992/article/details/104751705/、https://www.jianshu.com/p/24483c3fc58c
    ```
       str = new String(str.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8); 
   ```
