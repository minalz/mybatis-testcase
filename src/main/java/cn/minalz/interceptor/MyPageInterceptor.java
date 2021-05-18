package cn.minalz.interceptor;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.lang.reflect.Field;
import java.util.Properties;
/**
 * @Author: minalz
 * 编写拦截器：三个步骤
 * 1.实现Interceptor接口
 * 2.实现响应的方法，最关键的是intercept()方法，里面是拦截的逻辑，需要增强的功能代码就写在这里
 * 3.在拦截器类上加上朱姐，注解签名指定了需要拦截的对象、拦截的方法、参数(因为方法有不同的重载，所以要指定具体的参数)
 * 原理：
 * 代理模式+责任链模式
 * 可以进行拦截的四个对象：
 * 1.Executor 上层的对象，SQL执行全过程，包括组装参数，组装结果集返回和执行SQL过程
 * --> 因为Executor有可能被二级缓存装饰，那么是先代理再装饰，还是先装饰再代理呢？
 * Executor会先拦截到CachingExecutor或者BaseExecutor
 * DefaultSqlSessionFactory.openSessionFromDataSource():
 * 看执行步骤后得知：先创建基本类型，再二级缓存装饰，最后插件拦截。所以这里拦截的是CachingExecutor.
 * 2.StatementHandler 执行SQL的过程，最常用的拦截对象
 * 3.ParameterHandler SQL参数组装的过程
 * 4.ResultSetHandler 结果集的组装
 */

/**
 * 插件实现原理：
 * 1.代理类怎么创建？动态代理是JDK Proxy还是Cglib？怎么样创建代理？
 * -> 调用InterceptorChain的pluginAll()方法，做了什么事？
 * 遍历InterceptorChain，使用Interceptor实现类的plugin()方法，对目标核心对象进行代理
 * 这个plugin()方法是我们自己实现的，要返回一个代理对象
 * 如果是JDK动态代理，那我们必须要写一个实现了InvocationHandler接口的触发管理类。然后用Proxy.newProxyInstance()创建一个代理对象
 * MyBatis的插件机制已经把这些类封装好了，它已经提供了一个触发管理类Plugin，实现了InvocationHandler
 * 创建代理对象的newProxyInstance()在这个类里面也进行了封装，就是wrap()方法
 * 2.代理类在什么时候创建？实在解析配置的时候创建，还是获取会话的时候创建，还是在调用的时候创建？
 * -> 对Executor拦截的代理类是openSession()的时候创建的
 * StatementHandler是SimpleExecutor.doQuery()创建的，里面包含了ParamterHandler和ResultSetHandler的创建和代理
 * 3.核心对象被代理之后，调用的流程是怎么样的？怎么依次执行多个插件的逻辑？在执行完了插件的逻辑之后，怎么执行原来的逻辑？
 * -> 如果对象被调用了多次，会继续调用下一个拆案的逻辑，再走一次Plugin的invoke()方法
 * 配置的顺序和执行的顺序是相反的。InterceptorChain的List是按照插件从上往下的顺序解析、添加的，而创建代理的时候也是按照List的顺序代理。
 * 那么执行的时候当然是从最后代理的对象开始
 * 插件定义顺序  代理顺序   代理执行顺序(invoke())
 * 插件1          1          3
 * 插件2          2          2
 * 插件3          3          1
 * 总结:
 * Interceptor: 自定义插件需要实现接口，实现4个方法
 * InterceptorChain: 配置的插件解析后会保存在Configuration的InterceptorChain中
 * Plugin: 触发管理类，还可以用来创建代理对象
 * Invocation: 对被代理类进行包装，可以调用proceed()调用到被拦截的方法
 */
@Intercepts({@Signature(type = Executor.class,method = "query",
        args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
public class MyPageInterceptor implements Interceptor {
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        System.out.println("将逻辑分页改为物理分页");
        Object[] args = invocation.getArgs();
        MappedStatement ms = (MappedStatement) args[0]; // MappedStatement
        BoundSql boundSql = ms.getBoundSql(args[1]); // Object parameter
        RowBounds rb = (RowBounds) args[2]; // RowBounds
        // RowBounds为空，无需分页
        if (rb == RowBounds.DEFAULT) {
            // 可以继续执行原方法
            return invocation.proceed();
        }

        // 将原 RowBounds 参数设为 RowBounds.DEFAULT，关闭 MyBatis 内置的分页机制
        //args[2] = RowBounds.DEFAULT;

        // 在SQL后加上limit语句
        String sql = boundSql.getSql();
        String limit = String.format("LIMIT %d,%d", rb.getOffset(), rb.getLimit());
        sql = sql + " " + limit;

        // 自定义sqlSource
        SqlSource sqlSource = new StaticSqlSource(ms.getConfiguration(), sql, boundSql.getParameterMappings());

        // 修改原来的sqlSource
        Field field = MappedStatement.class.getDeclaredField("sqlSource");
        field.setAccessible(true);
        field.set(ms, sqlSource);

        // 执行被拦截方法
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }
}
