### 此项目是关于mybatis部份源码分析的一个项目，主要设计到SqlSessionFactory、SqlSession的创建，以及SQL的执行流程

------------------------------------------------------------------------------------------

### 一、SqlSessionFactory的创建

![](https://github.com/heartccace/mybatis/blob/master/src/main/resources/images/sqlSessionFactory创建.jpg)

1. 首先根据SqlSessionFactoryBuilder对象加载mybatis-config.xml的config资源文件并创建XMLConfigBuilder来解析文件；（此处XMLConfigBuilder和XMLMapperBuilder使用的是建造者模式）

   ```
    public SqlSessionFactory build(InputStream inputStream, String environment, Properties properties) {
       try {
        //通过XMLConfigBuilder来解析文件
         XMLConfigBuilder parser = new XMLConfigBuilder(inputStream, environment, properties);
         //parser.parse()解析后产生一个Configuration对象
         // 在XMLConfigBuilder中使用XMLMapperBuilder解析Mapper文件
         // Configuration 包含配置文件的所有信息包含了mapper文件
         //其中Configuration中的mappedStatements属性存储着所有的sql语句
         return build(parser.parse());
       } catch (Exception e) {
         throw ExceptionFactory.wrapException("Error building SqlSession.", e);
       } finally {
         ErrorContext.instance().reset();
         try {
           inputStream.close();
         } catch (IOException e) {
           // Intentionally ignore. Prefer previous error.
         }
       }
     }
   ```

   mapper中的sql语句被组装成MappedStatement保存在MappedStatements（map类型）其中key为namespace+id，

   

2. SqlSession对象应用过程分析（是实现与数据库进行会话的入口）

   ![] (https://github.com/heartccace/mybatis/blob/master/src/main/resources/images/sqlsession.jpg)

3. 