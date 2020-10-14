package com.example.mvcframework.servlet;

import com.example.mvcframework.annotation.Autowired;
import com.example.mvcframework.annotation.Controller;
import com.example.mvcframework.annotation.RequestMapping;
import com.example.mvcframework.annotation.Service;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @see https://www.cnblogs.com/linlf03/p/9995135.html
 *
 * 该类调用过程说明：
 * DispatcherServlet 多次请求只实例化一次
 * init() 方法在构造方法之后调用
 *
 *
 * Servlet生命周期：
 * @ https://how2j.cn/k/servlet/servlet-lifecycle/550.html
 *
 * 一个Servlet的生命周期由 实例化，初始化，提供服务，销毁，被回收 几个步骤组成
 *
 * 当用户通过浏览器输入一个路径，这个路径对应的servlet被调用的时候，该Servlet就会被实例化
 * 无论访问了多少次LoginServlet，LoginServlet构造方法 只会执行一次，所以Servlet是单实例的
 *
 *
 * LoginServlet 继承了HttpServlet，同时也继承了init(ServletConfig) 方法
 * init 方法是一个实例方法，所以会在构造方法执行后执行。
 * 无论访问了多少次LoginSerlvet，init初始化 只会执行一次
 *
 *
 * @author: kaiyi
 * @create: 2020-10-14 00:46
 */
public class DispatcherServlet extends HttpServlet {

  /**
   * 获取web.xml init-param  application.properties
   */
  private Properties contextConfig = new Properties();

  /**
   * 根据 scanPackage 获取该扫描包路径下的所有类文件名
   */
  private List<String> classNames = new ArrayList<String>();


  /**
   * 说明：
   * ioc容器中保存两种键类型的对象
   * 第一种：底层类简称key(如：person)->对象
   * 第二种：实体名称key(com.example.Person,这种是属于接口类的，即父类名）->对象
   */
  private Map<String, Object> ioc = new HashMap<String, Object>();

  /**
   * 路径和方法对象映射表
   *
   * "/account+/findAccount" -> findAccount()
   */
  private Map<String, Method> handlerMapping = new HashMap<String, Method>();


  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    this.doPost(req, resp);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    //运行阶段，根据用户请求的URL进行自动分发和调度
    try {
      doDispatch(req,resp);
    } catch (Exception e) {
      e.printStackTrace();
      resp.getWriter().write("500 Detail:" + Arrays.toString(e.getStackTrace()));
    }
  }

  private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws Exception {

    if(this.handlerMapping.isEmpty()){
      return ;
    }

    //绝对路径
    String url  = req.getRequestURI();

    //处理成相对路径
    String contextPath = req.getContextPath();
    url = url.replace(contextPath, "").replaceAll("/+", "/");
    if(!this.handlerMapping.containsKey(url)){
      resp.getWriter().write("404 Not found");
      return;
    }

    // 通过访问的url拿到方法对象，"/account+/findAccount" -> findAccount()
    Method method = this.handlerMapping.get(url);
    //如何拿到实例？
    //唯一的方式从IOC容器中拿
    //继续投机取巧
    String beanName =  lowerFirstCase(method.getDeclaringClass().getSimpleName());

    System.out.println("beanName:" + beanName);
    Map<String,String[]> params = req.getParameterMap();

    // 这里的this,表示哪个类调用继承父类，就是哪个子类的对象，如CategoryServlet调用，则this指代的就是该类CategoryServlet
    //Method m = this.getClass().getMethod(method, javax.servlet.http.HttpServletRequest.class,
    // javax.servlet.http.HttpServletResponse.class, Page.class);

    // 方法对象.invoke(类对象，名字string)。  反射机制
    // String redirect = m.invoke(this, request, response, page).toString();
    method.invoke(ioc.get(beanName), new Object[]{req, resp,params.get("name")[0]});

    System.out.println(method);

  }

  @Override
  public void init(ServletConfig config) throws ServletException {
    //1、加载配置文件
    doLoadConfig(config.getInitParameter("contextConfigLocation"));

    //2. 扫描所有相关的类
    doScanner(contextConfig.getProperty("scanPackage"));

    //3.初始化刚刚扫描到所有相关的类，并且把它保存在IOC容器中
    doInstance();

    //4. 实现依赖注入 DI 自动赋值
    doAutowired();

    //5. 初始化handlerMapping
    initHandleMapping();

    System.out.println("Spring is init");
  }

  private void doLoadConfig(String contextConfigLocation) {
    InputStream is = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
    try {
      contextConfig.load(is);
    } catch (IOException e) {
      e.printStackTrace();
    }finally {
      if(is != null){
        try {
          is.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  private void doScanner(String scanPackage) {
    URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
    File classDir = new File(url.getFile());
    for(File file : classDir.listFiles()){
      if(file.isDirectory()){
        doScanner(scanPackage + "." + file.getName());
      }else{
        if(file.getName().endsWith(".class")){
          String classname = (scanPackage + "." +file.getName()).replace(".class","");
          classNames.add(classname);
        }
      }
    }
  }

  /**
   * 根据类名实例化对象，并封装在map
   *
   * //使用反射的方式创建对象
   *             String className = "charactor.Hero";
   *             //类对象
   *             Class pClass=Class.forName(className);
   *             //构造器
   *             Constructor c= pClass.getConstructor();
   *             //通过构造器实例化
   *             Hero h2= (Hero) c.newInstance();
   *             h2.name="gareen";
   *             System.out.println(h2);
   *
   */
  private void doInstance() {
    if(classNames.isEmpty()){
      return;
    }

    try{
      for (String className: classNames) {

        // 反射（三种获取类对象方法，类对象，就是用于描述这种类，都有什么属性，什么方法的，获取类对象的时候，会导致类属性被初始化）
        // 1、Class pClass1=Class.forName(className);
        // 2、Class pClass2=Hero.class;
        // 3、Class pClass3=new Hero().getClass();

        // 打印：class tmall.servlet.CategoryServlet
        // System.out.println(this.getClass());

        // 反射机制学习：https://how2j.cn/k/reflection/reflection-method/109.html#nowhere
        // Hero h = new Hero();
        // 获取这个名字叫做setName，参数类型是String的方法
        // Method m = h.getClass().getMethod("setName", String.class);
        // 对h对象，调用这个方法
        // m.invoke(h, "盖伦");
        // 使用传统的方式，调用getName方法
        // System.out.println(h.getName());

        /**
         *
         * 方法对象.invoke(类对象，名字string)。  反射
         * 类对象.方法(名字string)   正常
         *
         * 与传统的通过new 来获取对象的方式不同
         * 反射机制，会先拿到Hero的“类对象”,然后通过类对象获取“构造器对象”
         * 再通过构造器对象创建一个对象
         */

        Class<?> clazz = Class.forName(className);

        //实例化，把实例化的对象保存到IOC容器之中

        // 例：A.isAnnotationPresent(B.class),大白话：B类型的注解是否在A类上。
        // https://blog.csdn.net/w362501266/article/details/97392452
        if(clazz.isAnnotationPresent(Controller.class)){

          // 实例化对象
          Object instance = clazz.newInstance();

          // getSimpleName() 得到类的简写名称（如：Sample, 而sample.class 得到对象的全路径，包含包路径 ）
          String beanName = lowerFirstCase(clazz.getSimpleName());

          // 将获取的类名及实例化的对象按照键值对封装在map
          ioc.put(beanName, instance);
        } else if(clazz.isAnnotationPresent(Service.class)){

          // 如果类是Service注解，则判断是否有自定义的beanName(即：@Service("heroService")),如果没有，则获取默认的类名作为ioc的key
          //因为Service有可能注入的不是它本身，有可能是它的实现类
          //1、默认类名首字母小写

          //2、自定义的beanName,此方法返回该元素的注解在此元素的指定注释类型（如果存在），否则返回null。
          Service service = clazz.getAnnotation(Service.class);
          String beanName = service.value();
          if("".equals(beanName)){
            beanName = lowerFirstCase(clazz.getSimpleName());
          }
          Object instance = clazz.newInstance();
          ioc.put(beanName, instance);

          //3、如果是接口，投机取巧的方式，用它的接口类型作为key
          // getInterfaces()方法和Java的反射机制有关。它能够获得这个对象所实现的接口。
          // @see https://blog.csdn.net/handsome_fan/article/details/54837937
          Class<?>[] interfaces = clazz.getInterfaces();
          for(Class<?> i : interfaces){

           // Class.getName()：以String的形式，返回Class对象的“实体”名称；
           // Class.getSimpleName()：获取源代码中给出的“底层类”简称。
            // getName ----“实体名称” ---- com.se7en.test.Person
            //getSimpleName ---- “底层类简称” ---- Person

            if(ioc.containsKey(i.getName())){
              throw  new Exception("The beanName is exists");
            }
            ioc.put(i.getName(), instance);    // 类继承的接口类名称（父类com.se7en.test.People）-》对象
          }

        }else{
          continue;
        }

      }
    }catch (Exception e){
      e.printStackTrace();
    }

    /**
     * 说明：
     * ioc容器中保存两种键类型的对象
     * 第一种：底层类简称key(如：person)->对象
     * 第二种：实体名称key(com.example.Person,这种是属于接口类的，即父类名）->对象
     */

  }


  /**
   * 实现依赖注入 DI 自动赋值
   */
  private void doAutowired() {
    if(ioc.isEmpty()){
      return;
    }

    /**
     * 由于Map中存放的元素均为键值对，故每一个键值对必然存在一个映射关系。
     * Map中采用Entry内部类来表示一个映射项，映射项包含Key和Value (我们总说键值对键值对, 每一个键值对也就是一个Entry)
     * Map.Entry里面包含getKey()和getValue()方法
     */
    // 通过entrySet()方法将map集合中的映射关系取出（这个关系就是Map.Entry类型）
    for(Map.Entry<String, Object> entry: ioc.entrySet()){

      // getFields()：返回一个数组，其中包含反映该类对象表示的类或接口的所有可访问公共字段的字段，包括继承的字段，但是只能是public。
      // getDeclaredFields()：返回反映由类对象表示的类或接口声明的所有字段的字段对象数组。这包括公共、受保护、默认（包）访问和专用字段，但不包括继承的字段。

      Field[] fields = entry.getValue().getClass().getDeclaredFields();  // 字段对象
      for(Field field: fields){

        // 例：A.isAnnotationPresent(B.class),大白话：B类型的注解是否在A类上。
        // 这里的是：Autowired注解是否在该字段上
        if(!field.isAnnotationPresent(Autowired.class)){
          continue;
        }

        // 获取注解对象，主要用来判断是否该注解对象是用自定义的值还是类名来从ioc获取对象
        Autowired autowired = field.getAnnotation(Autowired.class);
        String beanName = autowired.value().trim();
        if("".equals(beanName)){

          // 获取字段对象类型class,然后通过类对象获取类名
          beanName = field.getType().getName();
        }

        field.setAccessible(true); // 强制暴力访问，可以访问私有变量
        try {
          // Field.set()向对象的这个Field属性设置新值value
          // set(Object obj, Object value)
          // 将指定对象变量上此 Field 对象表示的字段设置为指定的新值.

          /**
           * 示例：
           * //根据属性名设置它的值
           * A a = new A();
           * Field field = a.getClass().getDeclaredField("x");
           * field.setAccessible(true);
           * field.set(a, 1);
           */
          field.set(entry.getValue(), ioc.get(beanName));
        } catch (IllegalAccessException e) {
          e.printStackTrace();
          continue;
        }

      }
    }
  }

  /**
   * 初始化handlerMapping
   *
   * @see https://blog.csdn.net/qq_41425382/article/details/100123183
   */
  private void initHandleMapping() {
    if(ioc.isEmpty()){
      return;
    }

    for(Map.Entry<String, Object> entry: ioc.entrySet()) {
      Class<?> clazz = entry.getValue().getClass();
      if(!clazz.isAnnotationPresent(Controller.class)){
        continue;
      }

      // 类上是否有该注解
      String baseUrl = "";
      if(clazz.isAnnotationPresent(RequestMapping.class)){
        RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);

        // 示例：@RequestMapping("/appointments")
        // 获取的值为：/appointments
        baseUrl = requestMapping.value();
      }

      Method[] methods = clazz.getMethods(); // 只认public的方法
      for(Method method : methods){
        // 方法上是否有该RequestMapping注解， @RequestMapping("/findAccount")
        if(!method.isAnnotationPresent(RequestMapping.class)){
          continue;
        }
        RequestMapping requestMapping =  method.getAnnotation(RequestMapping.class);

        // 将类注解的基url和方法上注解的具体url拼接在一起，称为完整的访问路径：/account+/findAccount
        String url = ("/" + baseUrl + "/" + requestMapping.value()).replaceAll("/+", "/");
        handlerMapping.put(url,method);
        System.out.println("Mapped:" + url +"," + method);
      }

      Field[] fields = entry.getValue().getClass().getDeclaredFields();
      for (Field field : fields) {
        if (!field.isAnnotationPresent(Autowired.class)) {
          continue;
        }
      }
    }
  }


  /**
   * 类名首字母小写
   */
  private String lowerFirstCase(String simpleName) {
    char[] chars = simpleName.toCharArray();
    chars[0] += 32;
    return  String.valueOf(chars);
  }




}
