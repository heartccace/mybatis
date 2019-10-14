### 此项目是关于mybatis部份源码分析的一个项目，主要设计到SqlSessionFactory、SqlSession的创建，以及SQL的执行流程

------------------------------------------------------------------------------------------

### 一、SqlSessionFactory的创建

一、SqlSessionFactory的创建

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

### 二、SqlSession对象应用过程分析（是实现与数据库进行会话的入口）

​		![](https://github.com/heartccace/mybatis/blob/master/src/main/resources/images/sqlsession.jpg)

- 首先通过SqlSession(默认实现时DefaultSqlSession)直接查询时创建Executor(cachedExecutor),通过cachedExecutor进行查询（以selectList为例）

  ```
   DefaultSqlSession.class -> selectList
   // 从configuration中找到相应的MappedStatement(封装sql对象)
   // 参数statement为mapper文件的命名空间 + sqlid
   MappedStatement ms = configuration.getMappedStatement(statement);
        return executor.query(ms, wrapCollection(parameter), rowBounds, Executor.NO_RESULT_HANDLER);
  ```

- CachedExecutor调用SimpleExecutor(此处采用装饰者模式)创建StamentHandler

  ```
  SimpleExcutor.java -> doquery(.....)方法 此处省略参数
  Configuration configuration = ms.getConfiguration();
        StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, resultHandler, boundSql);
        stmt = prepareStatement(handler, ms.getStatementLog());
        // 此处调用PreparedStatementHandler下的query方法
        return handler.<E>query(stmt, resultHandler);
  
  
  PreparedStatementHandler  -> query()
  public <E> List<E> query(Statement statement, ResultHandler resultHandler) throws SQLException {
      PreparedStatement ps = (PreparedStatement) statement;
      ps.execute();
      return resultSetHandler.<E> handleResultSets(ps);
    }
    
  ```

- PreparedStatementHandler调用ParameterHandler对参数进行处理

- ParameterHandler则调用相应的参数处理器

- 最后PreparedStatementHandler调用DefaultResultSetHandler处理查询结果（可能也会调用参参数处理器将jdbc类型转换成java数据类型，发生在结果处理之前）

  

### 三、类型转换器(TypeHandler)的应用

​			作用：java对象数据类型与数据库对象类型之间的转换

​			mybatis通过TypeHandlerRegistry注册了大部分的类型转换器

​			自定义实现：通过继承TypeHandler接口或者继承BaseTypeHandler

​			使用：1）通过resultMap标签中的result标签指定typeHandler

​						2）通过#{字段，typeHandler=全路径类名}			

### 四、拦截器(Interceptor)的应用（使用代理模式、责任链模式实现）

​		拦截器的实现：实现接口Interceptor并对拦截对象进行描述

​		使用：在mybatis的配置文件中通过plugin标签进行配置

​		创建：Mybatis框架初始化时会创建拦截器对象然后添加到拦截器链

​		拦截器对象应用过程分析：

​			Mybatis会在Executor、StatementHandler、ResultSetHandler、ParameterHandler对象 创			建时，加入拦截器的定义指定为对象创建代理对象，并在执行代理对象业务时，会执行				Inteceptor的intercept方法