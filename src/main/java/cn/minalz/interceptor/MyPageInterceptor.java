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
