package cn.minalz;

import cn.minalz.domain.Blog;
import cn.minalz.domain.BlogExample;
import cn.minalz.mapper.BlogMapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * @Author: minalz
 * Example演示
 */
public class BlogExampleTest {
    /**
     * 自动生成的Example
     * @throws IOException
     */
    @Test
    public void TestExample() throws IOException {
        String resource = "mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);

        SqlSession session = sqlSessionFactory.openSession();
        try {
            BlogMapper mapper = session.getMapper(BlogMapper.class);
            BlogExample example = new BlogExample();
            BlogExample.Criteria criteria = example.createCriteria();
            criteria.andBidEqualTo(1);
            List<Blog> list = mapper.selectByExample(example);
            System.out.println(list);
        } finally {
            session.close();
        }
    }


}
