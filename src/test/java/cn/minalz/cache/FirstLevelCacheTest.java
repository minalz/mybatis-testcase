package cn.minalz.cache;

import cn.minalz.domain.Blog;
import cn.minalz.mapper.BlogMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

/**
 * @Author: minalz
 * @Description: 咕泡学院，只为更好的你
 */
public class FirstLevelCacheTest {
    /**
     * 测试一级缓存需要先关闭二级缓存，localCacheScope设置为SESSION
     * @throws IOException
     */
    @Test
    public void testCache() throws IOException {
        String resource = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

        SqlSession session1 = sqlSessionFactory.openSession();
        SqlSession session2 = sqlSessionFactory.openSession();
        try {
            BlogMapper mapper0 = session1.getMapper(BlogMapper.class);
            BlogMapper mapper1 = session1.getMapper(BlogMapper.class);
            Blog blog = mapper0.selectBlogById(1);
            System.out.println(blog);

            System.out.println("第二次查询，相同会话，获取到缓存了吗？");
            System.out.println(mapper1.selectBlogById(1));

            System.out.println("第三次查询，不同会话，获取到缓存了吗？");
            BlogMapper mapper2 = session2.getMapper(BlogMapper.class);
            System.out.println(mapper2.selectBlogById(1));

        } finally {
            session1.close();
        }
    }

    /**
     * 一级缓存失效
     * @throws IOException
     */
    @Test
    public void testCacheInvalid() throws IOException {
        String resource = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

        SqlSession session = sqlSessionFactory.openSession();
        try {
            BlogMapper mapper = session.getMapper(BlogMapper.class);
            System.out.println(mapper.selectBlogById(1));

            Blog blog = new Blog();
            blog.setBid(1);
            blog.setName("after modified 666");
            mapper.updateByPrimaryKey(blog);
            session.commit();

            // 相同会话执行了更新操作，缓存是否被清空？
            System.out.println("在[同一个会话]执行更新操作之后，是否命中缓存？");
            System.out.println(mapper.selectBlogById(1));

        } finally {
            session.close();
        }
    }

    /**
     * 因为缓存不能跨会话共享，导致过时的数据出现
     * @throws IOException
     */
    @Test
    public void testDirtyRead() throws IOException {
        String resource = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

        SqlSession session1 = sqlSessionFactory.openSession();
        SqlSession session2 = sqlSessionFactory.openSession();
        try {
            BlogMapper mapper1 = session1.getMapper(BlogMapper.class);
            System.out.println(mapper1.selectBlogById(1));

            // 会话2更新了数据，会话2的一级缓存更新
            Blog blog = new Blog();
            blog.setBid(1);
            blog.setName("after modified 333333333333333333");
            BlogMapper mapper2 = session2.getMapper(BlogMapper.class);
            mapper2.updateByPrimaryKey(blog);
            session2.commit();

            // 其他会话更新了数据，本会话的一级缓存还在么？
            System.out.println("会话1查到最新的数据了吗？");
            System.out.println(mapper1.selectBlogById(1));
        } finally {
            session1.close();
            session2.close();
        }
    }
}
