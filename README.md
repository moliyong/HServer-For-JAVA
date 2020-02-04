# HServer

#### 介绍
    HServer是一个基于Netty的一个高并发Webserver,它不仅仅是一个webserver，我们可以直接在这个基础上进行开发
    它提供了相关的注解和一些方法，完全能够完成我们大大小小的项目。作为一名Java程序员写web程序spring是我们项目99%会用的。
    spring的优点就不过多讲。Hserver是一个“tomcat”+“spring”的这样的小玩意。
    它的qps比tomcat更快，这个是因为Hserver使用的Netty,注解比spring更简，这是因为，它还不够强大（滑稽，我会努力让它更智能点）
    如果你是一个phper或者喜欢php，可以关注下 
[![黑小马工作室/HServer](https://gitee.com/heixiaomas/HServer/widgets/widget_card.svg?colors=ffffff,ffffff,,e3e9ed,666666,9b9b9b)](https://gitee.com/heixiaomas/HServer)
    
### 最新中央仓库版本
    <dependency>
        <groupId>top.hserver</groupId>
        <artifactId>HServer</artifactId>
        <version>2.9</version>
    </dependency>

    
### 压测说明
请查看 [PM.md](doc/PM.md) 查看说明


### 更新历史
请查看 [CHANGELOG.md](doc/CHANGELOG.md) 了解近期更新情况。


### 文档地址
文档说明请查看 [点我](https://hserver.top) 

文档源码地址 [点我](https://gitee.com/heixiaomas_admin/hserver-for-java-doc) 

Redis操作源码案例地址(Jedis) [点我](https://gitee.com/heixiaomas_admin/hserver-for-java-redis)

MYSQL操作源码案例地址(Neo) [点我](https://gitee.com/heixiaomas_admin/hserver-for-java-mysql)




## 快速开始
#### 1.文件结构框架架构说明
![AB测试](https://gitee.com/heixiaomas_admin/HServer/raw/master/doc/架构说明.png)
#### 2.注解认识
    以下注解基本模拟Spring的功能
    
    
    @Bean
    将Bean对象加入IOC容器中比如
        //按默名字加入IOC容器
        @Bean
        class TestService{}
        //指定名字加入容器，装配的时候就只能通过名字装配了
        @Bean("testService")
        class Test{}
        
        
    @Autowired
    自动装配注解
        //按类型注入
        @Autowired
        private TestService testService;
        //按Bean名字注入
        @Autowired("testServer1")
        private TestService testService; 
        
           
    @Controller
    控制器注解，将控制器加入IOC容器中，类似Spring mvc
    注解在类上面直接加上即可比如
        //Index控制器
        @Controller
        class IndexController{}
        
           
    @GET,@POST
    方法注解，在@Controller注解类类中使用，标注一个方法为GET或者POST方法，例如
        @GET("/index")
        public void index(){}  
        @POST("/index")
        public void index(){}
        
        //url规则匹配
        @GET("/url1/{url}")
        public String url(HttpRequest httpRequest){
            String url = httpRequest.query("url");
            System.out.println(url);
            return url;
        }
    
        @GET("/url/{url}")
        public String url(String url){
            return "匹配到的URL:"+url;
        }
    
        @POST("/a/{url}/bb")
        public String ab(String url){
            return "匹配到的URL:"+url;
        }  
        
        
    @Filter
    拦截器注解，标注一个类为拦截器，和JavaEE的Filter类似
        @Filter(1)//1表示拦截优先级，越小越优先
        public class MyFilter1 implements FilterAdapter {}
        //需要实现FilterAdapter接口
        
        
    @Hook
    hook注解就是Aop
        @Hook(value = Test.class, method = "show")
        public class HookTest implements HookAdapter {}
        //value表示aop的类,method要hook的方法，必须实现HookAdapter
        
        
    @Task
     定时任务
        @Task(name = "测试定时任务Cron", time ="*/5 * * * * ?")
        //标记在方法上，同时该类需要被@Bean 标记
        @Task(name = "测试定时任务1", time ="2000")
        public void timerTask() {}
        
        
    @WebSocket
    实现websocket通信
        @WebSocket("/ws")
        public class WebSocketTest implements WebSocketHandler {}
        //这样就可以完成基本的通信了
        
        
    @Configuration
    自定配置注解，需要配合@Bean注解一起使用，最后会把方法里面的返回的对象
    存储到IOC容器中，同时可以通过Autowired注解注入
        @Configuration
        public class DataConfig {
        
            //自定义名字（用例：比如多数据源注入）
            @Bean("createUser")
            public User createUser(){
                User user = new User();
                user.setAge(999);
                user.setName("我是配置类自定义名字的数据");
                user.setSex("未知");
                return user;
            }
        
           //按类型存储 
            @Bean
            public User createUser1(){
                User user = new User();
                user.setAge(999);
                user.setName("我是配置类的默认数据");
                user.setSex("未知");
                return user;
            }
        
        }
        
        
    @RpcService
    标注一个Bean对象是一个rpc服务,也可以分配一个名字
        @Bean
        @RpcService
        public class RpcServiceTest {
            public String test(String name){
                return name+"我是RPC";
            }
        }  
        
        
    @Resource
    注入一个Rpc服务，也可以通过名字注入。详情，请看文档介绍   
        @Resource
        private RpcServiceTest rpcServiceTest;
        
        
    @Sign("MD5")
    @RequiresRoles("角色")
    @RequiresPermissions(value = {"/权限1","/权限2"}, logical=Logical.OR)
    该注解用于标注控制器里面的方法，方便自己实现sign签名算法，
    角色检查，权限检查，实现token等，详情下面的对应接口。
    
                
#### 3.完成Hello World项目
     #第一步pom依赖引入
    
    <dependency>
        <groupId>top.hserver</groupId>
        <artifactId>HServer</artifactId>
        <version>最新版</version>
    </dependency>

    public class WebApp {
        public static void main(String[] args) {
            //运行官方例子,直接运行既可以了，默认自带了一些例子。
            HServerApplication.run(TestWebApp.class, 8888);
        }
    }


    #第二步搞一个主函数
    public class WebApp {
        public static void main(String[] args) {
            HServerApplication.run(WebApp.class, 8888);
        }
    }
    
    #第三步同主函数建立一个包文件夹比如controller
    
    @Controller
    public class Hello {
    
        @GET("/hello")
        public Map index(HttpRequest request, String name) {
            Map<String, Object> res = new HashMap<>();
            res.put("code", 200);
            res.put("res", request.getRequestParams());
            res.put("msg", "Hello");
            return res;
        }
        
        /**
         * 模板测试
         * @param httpResponse
         */
        @GET("/template")
        public void template(HttpResponse httpResponse) {
            User user = new User();
            user.setAge(20);
            user.setName("xx");
            user.setSex("男");
            Map<String,Object> obj=new HashMap<>();
            obj.put("user",user);
    //        httpResponse.sendTemplate("/admin/user/list.ftl", obj);
            httpResponse.sendTemplate("a.ftl", obj);
        }
    }
    #就这样你就完成了一个简单得get请求定义，更多例子，可以参考包top.test下面的例子
    
#### 4.文件上传下载操作
          
          #File类型得
          @GET("/downFile")
          public void downFile(HttpRequest request, HttpResponse response) {
              response.setDownloadFile(new File("D:\\Java\\HServer\\README.md"));
          }
          #InputStream 类型得
          @GET("/downInputStream")
          public void downInputStream(HttpRequest request, HttpResponse response) throws Exception {
              File file = new File("D:\\Java\\HServer\\README.md");
              InputStream fileInputStream = new FileInputStream(file);
              response.setDownloadFile(fileInputStream,"README.md");
          }
#### 5.Aop操作

        #必须实现HookAdapter的接口
        #同时被@Hook注解标注
        @Slf4j
        @Hook(value = Test.class, method = "show")
        public class HookTest implements HookAdapter {
        
            @Override
            public void before(Object[] objects) {
                log.info("aop.-前置拦截：" + objects[0]);
                objects[0]="666";
            }
        
            @Override
            public Object after(Object object) {
                return object + "aop-后置拦截";
            }
        }
#### 6.Filter操作

        #必须实现FilterAdapter接口，同时被@Filter标注，数字越小，优先级越高，切不要重复
        @Slf4j
        @Filter(1)
        public class MyFilter2 implements FilterAdapter {
            @Override
            public void doFilter(FilterChain chain, Webkit webkit) {
                log.info("MyFilter->1");
                chain.doFilter(webkit);
            }
        }
#### 7.定时任务操作
    
    #需要被@Bean注解标注,可以通过TaskManager类进行定时任务的控制，动态添加和删除
    @Bean
    public class TaskTest {
        
        @Autowired
        private TestService testService;
    
        private boolean flag = true;
    
        public void dynamicAddTimer() {
            System.out.println("动态添加定时任务");
            TaskManager.addTask("测试任务2", "2000", TestTask.class,"666");
        }
        
        
        @Task(name = "测试定时任务1", time ="*/5 * * * * ?")
        public void timerTask() {
            System.out.println("测试定时任务，注入的对象调用结果:" + testService.testa());
            if (flag) {
                dynamicAddTimer();
                flag = false;
            }
        }
    
        @Task(name = "测试定时任务2", time = "2000")
        public void removeTask() {
            //干掉方法注解版本
            boolean task1 = TaskManager.removeTask("测试定时任务1");
            //干掉动态添加的
            boolean task2 = TaskManager.removeTask("测试任务2");
            //干掉自己
            boolean task3 = TaskManager.removeTask("测试定时任务2");
            //结果
            System.out.println("任务已经被干掉了 tash1=" + task1 + ",task2=" + task2 + ",task3=" + task3);
        }
    
    }
    
    //动态添加定时任务的实现类必须要实现一个TaskJob,样才能被TaskManager管理
    //添加任务 TaskManager.addTask("测试任务2", "2000", TestTask.class,"666");
    //删除任务  boolean is_success = TaskManager.removeTask("测试任务2");
    public class TestTask implements TaskJob {
    
        @Override
        public void exec(Object... args) {
            String args_ = "";
            for (Object arg : args) {
                args_ += arg.toString();
            }
            System.out.println("测试定时器动态添加任务，参数是：" + args_);
        }
    }
    
#### 8.websocket操作
    
    #需要被@WebSocket标注同时给一个连接地址，最后实现WebSocketHandler接口，
    #Ws类定义了简单的发送方法，如果有其他的业务操作，可以获取ChannelHandlerContext，进行操作

    @WebSocket("/ws")
    public class WebSocketTest implements WebSocketHandler {
    
        @Autowired
        private TestService testService;
    
        @Override
        public void onConnect(Ws ws) {
            System.out.println("连接成功,分配的UID：" + ws.getUid());
        }
    
        @Override
        public void onMessage(Ws ws) {
            ws.send("666" + testService.testa() + ws.getUid());
            System.out.println("收到的消息,"+ws.getMessage()+",UID：" + ws.getUid());
        }
    
        @Override
        public void disConnect(Ws ws) {
            System.out.println("断开连接,UID:" + ws.getUid());
        }
    }
#### 9.自带监控操作
    application.properties文件配置
        #开启访问统计
        statistics=true
        #统计规则:以逗号分割的正则表达式
        statisticalRules=/hel.*,/admin/.*
    #StatisticsHandler操作
        #获取所有的IP地址
        StatisticsHandler.getIpMap()
        #最近50个请求队列（调用的URI，(发送大小，接收大小)宽带监视，耗时时间）
        StatisticsHandler.getLogRequestQue()
        #唯一IP请求的数量 uv
        StatisticsHandler.getUniqueIpCount()
        #请求总数   pv
        StatisticsHandler.getCount()
        #uri记录 被访问的记录（）
        StatisticsHandler.getUriData()
    #提示：
    1，如果自己要做统计，完全可以自定义一个定时器，动态保存数据哦
    2，StatisticsHandler,提供了一个remove方法,remove，用来清除，或者保存数据用，它会返回一个最新的数据同时清除自己
    3，如果开启统计，请务必，执行Remove方法，不然，内存可能就会蹦

#### 10.全局异常处理
    
    类必须要被@Bean注解，同时实现GlobalException接口，
    一个项目中最多只有一个GlobalException实现哦，可以没有.没有异常处理，同时又报错了，那么直接显示错误
    @Bean
    public class WebException implements GlobalException {
    
        @Override
        public void handler(Exception exception, Webkit webkit) {
            exception.printStackTrace();
            System.out.println(webkit.httpRequest.getUri() + "--->" + exception.getMessage());
            webkit.httpResponse.sendHtml("全局异常处理");
        }
    }

#### 11.服务器启动完成是执行的方法
     类必须要被@Bean注解，同时实现InitRunner接口，
     @Bean
     public class RunInit implements InitRunner {
     
         @Autowired
         private User user;
     
         @Override
         public void init(String[] args) {
             System.out.println("初始化方法：注入的User对象的名字是-->"+user.getName());
         }
     }

#### 12.鉴权认证相关操作
    
    //请使用相关注解对控制器的方法做标记，这样在执行到被注解标记的方法就会执行下面的相关方法
    //  List<RouterPermission> routerPermissions = PermissionAdapter.getRouterPermissions();
    // 通过上面的代码可以获取到所有标记的注解，他可以干嘛？
    // 同步后台数据库里面的权限，后台管理面里面可以动态给角色分配权限。
    // 自己做一个下拉选择列表，创建角色分配权限时，多选即可。

    /**
     * 验证逻辑请自己实现哦
     */
    @Bean
    public class TestPermission implements PermissionAdapter {
    
        @Override
        public void requiresPermissions(RequiresPermissions requiresPermissions, Webkit webkit) {
            //这里你可以实现一套自己的权限检查算法逻辑，判断，
            //如果满足权限，不用其他操作，如果不满足权限，那么你可以通过，Webkit里面的方法直接输出相关内容
            //或者自定义一个异常类，在全局异常类做相关操作
            System.out.println(requiresPermissions.value()[0]);
        }
    
        @Override
        public void requiresRoles(RequiresRoles requiresRoles, Webkit webkit) {
            //这里你可以实现一套自己的角色检查算法逻辑，判断，
            //其他逻辑同上
            System.out.println(requiresRoles.value()[0]);
        }
    
        @Override
        public void sign(Sign sign, Webkit webkit) {
           //这里你可以实现一套自己的接口签名算法检查算法逻辑，判断，
           //其他逻辑同上
           Map<String, String> requestParams = webkit.httpRequest.getRequestParams();
           String sign1 = webkit.httpRequest.getHeader("sign");
           System.out.println(sign.value());
        }
    }       
#### 13.技巧篇
    1. Linux 内核版本大于 2.5.44，(目前云服务器都有了，没有的话自己升级内核)的Linux默认使用epoll
    2.待更新
