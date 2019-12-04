<center><h3> MyBatis源码解析</h3></center>
### 一、Resources资源处理、类加载的工具类（涉及代理模式代理类的创建）

​		MyBatis基于配置文件运行的入口，即通过配置config.xml和xxxMapper.xml。MyBatis首先会将配置文件加载到内存，并对配置文件进行解析，而Resours就是将文件读取到内存的工具类。

1.1 读取配置转换为流对象

​	Resours.getResourceAsStream("config.xml")将当前类路径下的config.xml配置文件转化成流返回InputStream

### 二、SqlSessionFactoryBuilder解析配置文件

SqlSessionFactoryBuilder完成对SqlSessionFactory对象的建造

1.1 SqlSessionFactoryBuilder创建SqlSessionFactory

​	SqlSessionFactoryBuilder通过build方法根据传入的流创建SqlSessionFactory对象。

```
// reader为传入的配置文件的流对象
public SqlSessionFactory build(Reader reader, String environment, Properties properties) {
    try {
     // XMLConfigBuilder主要负责对config.xml文件内容进行解析
     // 1.2详解
      XMLConfigBuilder parser = new XMLConfigBuilder(reader, environment, properties);
      // 1.3详解
      return build(parser.parse());
    } catch (Exception e) {
      throw ExceptionFactory.wrapException("Error building SqlSession.", e);
    } finally {
      ErrorContext.instance().reset();
      try {
        reader.close();
      } catch (IOException e) {
        // Intentionally ignore. Prefer previous error.
      }
    }
  }
```

1.2 XMLConfigBuilder对象的创建，其中XPathParser实现具体节点的解析工作，XMLMapperEntityResolver是MyBatis DTD的离线实体解析器,XMLConfigBuilder初始化时会创建Configuration对象，Configuration对象初始化时会初始化TypeAliasRegistry成员变量，让里面加入一些Mybatis的默认类比如：Mysql 事务/数据源/缓存等

```
 // XMLConfigBuilder构造函数
 public XMLConfigBuilder(Reader reader, String environment, Properties props) {
 // 创建具体执行解析的解析
    this(new XPathParser(reader, true, props, new XMLMapperEntityResolver()), 	  environment, props);
  }
  // XMLConfigBuilder最终调用的构造函数
  private XMLConfigBuilder(XPathParser parser, String environment, Properties props) {	
  	// 创建Configuration对象（重点），Configuration对象更像是一个容器，里面包含了配置文件    
  	// 里面很多重要的信息
  // 调用父类BaseBuilder的构造函数，对父类成员变量 protected final Configuration 
  // configuration进行赋值，由于configuration是protected，故子类(即本类)可以访问该成员变量
    super(new Configuration());
    ErrorContext.instance().resource("SQL Mapper Configuration");
    this.configuration.setVariables(props);
    // 标记为未解析
    this.parsed = false;
    // 为当前的成员变量赋值，但是environment可能为null
    this.environment = environment;
    // 为当前成员变量XPathParser parser赋值
    this.parser = parser;
  }
```

```
// 具体解析器XPathParser的构造函数
public XPathParser(InputStream inputStream, boolean validation, Properties variables, EntityResolver entityResolver) {
   // 为当前解析器初始化变量
    commonConstructor(validation, variables, entityResolver);
    // 流对象转换成文档对象
    this.document = createDocument(new InputSource(inputStream));
  }
```

到此为止XMLConfigBuilder对象完成初始化工作

1.3 SqlSessionFactoryBuilder的build方法的build(parser.parse())详解，parser对象类型为XMLConfigBuilder，并且parser对象里面同时维护了一个XPathParser parser对象

```
 // XMLConfigBuilder对象的parse方法
 public Configuration parse() {
 // 初始化的时候为parsed赋值为false，一
    // 赋值false表示已经做过解析
    parsed = true;
    // 具体解析由XPathParser负责
    // parser.evalNode("/configuration")将configuration标签里面的所以内容以节点形式得到
    parseConfiguration(parser.evalNode("/configuration"));
    return configuration;
  }
  
   private void parseConfiguration(XNode root) {
    try {
      // 获取properties标签的内容，将所有的property标签使用Properties类进行封装，包括使用       
      // jdbc.propertites里面的配置（因为在mybatis的config.xml里面可以使用properties标签导入外部的数据库配置	// 文件），并将得到的properties对象封装到Configuration的protected Properties variables成员变量里面
      propertiesElement(root.evalNode("properties"));
      // 获取settings标签的内容，将所有的setting封装到一个Properties对象里面
      Properties settings = settingsAsProperties(root.evalNode("settings"));
      loadCustomVfs(settings);
      // 用户自定义日志
      loadCustomLogImpl(settings);
      // 向Configuration的protected final TypeAliasRegistry typeAliasRegistry属性注册别名
      // 如果没有别名，就以类名作为别名。别名方式除了通过xml进行配置，还可以通过Alias.class注解
      typeAliasesElement(root.evalNode("typeAliases"));
      // 创建向Configuration添加插件（即拦截器） 并将当前的key - value存到当前的Interceptor
  	  pluginElement(root.evalNode("plugins"));
       // 创建并向Configuration添加对象工厂，MyBatis 每次创建结果对象的新实例时，它都会使用一个对象工厂		 		//（ObjectFactory）实例来完成。 默认的对象工厂需要做的仅仅是实例化目标类，要么通过默认构造方法，要么在参       // 数映射存在的时候通过参数构造方法来实例化。
      objectFactoryElement(root.evalNode("objectFactory"));
      objectWrapperFactoryElement(root.evalNode("objectWrapperFactory"));
      reflectorFactoryElement(root.evalNode("reflectorFactory"));
      // 将settings里面的配置赋值给Configuration测成员变量
      settingsElement(settings);
      // 根据配置选择当前的environment创建事务工厂和数据源工厂
      environmentsElement(root.evalNode("environments"));
      databaseIdProviderElement(root.evalNode("databaseIdProvider"));
      // 将javaType、JdbcType、handler封装到TypeHandlerRegistry的typeHandlerMap，其中以javaType为key
      // value为一个map，并且该map以JdbcType为key，handler为value，最后将Handler放入allTypeHandlersMap中
      typeHandlerElement(root.evalNode("typeHandlers"));
      // 加载并解析mappers节点，即对xxxmapper.xml文件的加载解析（重点）
      mapperElement(root.evalNode("mappers"));
    } catch (Exception e) {
      throw new BuilderException("Error parsing SQL Mapper Configuration. Cause: " + e, e);
    }
  }
```

 pluginElement(root.evalNode("plugins"))详解

```
 private void pluginElement(XNode parent) throws Exception {
    if (parent != null) {
    // 获取并遍历plugins节点的所有子节点
      for (XNode child : parent.getChildren()) {
      // 获取类名
        String interceptor = child.getStringAttribute("interceptor");
        // 获取当前子节点的key-value封装成Properties对象
        Properties properties = child.getChildrenAsProperties();
        // 通过反射创建当前节点对应的Interceptor对象
        Interceptor interceptorInstance = (Interceptor) resolveClass(interceptor).getDeclaredConstructor().newInstance();
        // 当前节点对象保存当前节点信息
        interceptorInstance.setProperties(properties);
        // 将当前节点拦截器加入到Configuration的拦截器链protected final InterceptorChain interceptorChain
        configuration.addInterceptor(interceptorInstance);
      }
    }
  }
```

mapperElement(root.evalNode("mappers"))详解

mapper的配置方式：

![avatar](mapper配置方式.png)

```
private void mapperElement(XNode parent) throws Exception {
    if (parent != null) {
    // 遍历mappers下的子节点
      for (XNode child : parent.getChildren()) {
      // 处理采用包配置形式
        if ("package".equals(child.getName())) {
          String mapperPackage = child.getStringAttribute("name");
          configuration.addMappers(mapperPackage);
        } else {
        // 获取采用路径方式的配置文件路径
          String resource = child.getStringAttribute("resource");
          // 获取采用完全限定资源符方式的配置文件路径
          String url = child.getStringAttribute("url");
          // 获取采用实现类方式的配置文件路径
          String mapperClass = child.getStringAttribute("class");
          // 解析采用resource方式配置格式的mapper文件
          if (resource != null && url == null && mapperClass == null) {
            ErrorContext.instance().resource(resource);
            InputStream inputStream = Resources.getResourceAsStream(resource);
            XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, resource, configuration.getSqlFragments());
            mapperParser.parse();
            // 解析采用url方式配置格式的mapper文件
          } else if (resource == null && url != null && mapperClass == null) {
            ErrorContext.instance().resource(url);
            InputStream inputStream = Resources.getUrlAsStream(url);
            XMLMapperBuilder mapperParser = new XMLMapperBuilder(inputStream, configuration, url, configuration.getSqlFragments());
            mapperParser.parse();
            // 解析采用class方式配置格式的mapper文件
          } else if (resource == null && url == null && mapperClass != null) {
            Class<?> mapperInterface = Resources.classForName(mapperClass);
            configuration.addMapper(mapperInterface);
          } else {
          // 采用多种格式就会抛出异常
            throw new BuilderException("A mapper element may only specify a url, resource or class, but not more than one.");
          }
        }
      }
    }
  }
```

采用resource格式和采用url格式配置的mapper文件解析方式都走的XMLMapperBuilder的mapperParser.parse()

```
  // 解析
  public void parse() {
  // 判断当前Configuration对象是否已经对该mapper进行过解析了，如果没有，就解析
    if (!configuration.isResourceLoaded(resource)) {
    // 解析获取mapper节点并解析
      configurationElement(parser.evalNode("/mapper"));
      // 将解析过的文件加入到Configurationd的loadedResources成员变量里面
      configuration.addLoadedResource(resource);
      bindMapperForNamespace();
    }

    parsePendingResultMaps();
    parsePendingCacheRefs();
    parsePendingStatements();
  }

```

```
private void configurationElement(XNode context) {
    try {
    // 获取当前命名空间
      String namespace = context.getStringAttribute("namespace");
      // 命名空间为空则抛出异常
      if (namespace == null || namespace.equals("")) {
        throw new BuilderException("Mapper's namespace cannot be empty");
      }
      // MapperBuilderAssistant builderAssistant对象在初始化XMLMapperBuilder对象时被创建，设置当前命名空间
      builderAssistant.setCurrentNamespace(namespace);
     // cache-ref作用是对其他命名空间缓存配置的引用
     // 解析cache-ref节点，将当前命名空间与相关联的引用命名空间放到Configuration里面的cacheRefMap对象中
     // CacheRefResolver对象调用MapperBuilderAssistant对象获取当前关联缓存 如果出现异常CacheRefResolver会	// 被放入到Configuration的incompleteCacheRefs成员变量里面。MapperBuilderAssistant里的currentCache为关	// 联cache
      cacheRefElement(context.evalNode("cache-ref"));
      // cache作用是对给定命名空间的缓存配置
      cacheElement(context.evalNode("cache"));
      // 已废弃 遍历所有的parameterMap标签进行解析
      parameterMapElement(context.evalNodes("/mapper/parameterMap"));
      resultMapElements(context.evalNodes("/mapper/resultMap"));
      sqlElement(context.evalNodes("/mapper    sql"));
      buildStatementFromContext(context.evalNodes("select|insert|update|delete"));
    } catch (Exception e) {
      throw new BuilderException("Error parsing Mapper XML. The XML location is '" + resource + "'. Cause: " + e, e);
    }
  }
```

```
 // mybatis缓存设置
 private void cacheElement(XNode context) {
    if (context != null) {
     // 获取缓存实现类，未配置情况下默认采用Mybatis的org.apache.ibatis.cache.impl.PerpetualCache
      String type = context.getStringAttribute("type", "PERPETUAL");
      // typeAliasRegistry里面获取对应的类
      Class<? extends Cache> typeClass = typeAliasRegistry.resolveAlias(type);
      // 缓存的回收策略 默认最近最少使用：移除最长时间不被使用的对象。具体实现见Configuration的构造函数
      String eviction = context.getStringAttribute("eviction", "LRU");
      // 加载缓存失效策略实现类
      Class<? extends Cache> evictionClass = typeAliasRegistry.resolveAlias(eviction);
      // 获取配置间隔刷新时间
      Long flushInterval = context.getLongAttribute("flushInterval");
      // 获取缓存配置大小
      Integer size = context.getIntAttribute("size");
      // 获取缓存读取状态，默认可读写
      boolean readWrite = !context.getBooleanAttribute("readOnly", false);
      // 获取缓存是否是阻塞，默认为false
      boolean blocking = context.getBooleanAttribute("blocking", false);
     // 获取其他配置的property属性
      Properties props = context.getChildrenAsProperties();
      // 通过MapperBuilderAssistant builderAssistant创建缓存对象，以当前命名空间为id，并设置currentCache指		// 向当前缓存，并加入到Configuration的caches中
      builderAssistant.useNewCache(typeClass, evictionClass, flushInterval, size, readWrite, blocking, props);
    }
  }
```

