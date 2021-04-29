package com.minalz.dbutils;

import com.minalz.dbutils.dao.BlogDao;

/**
 * @Author: minalz
 */
public class QueryRunnerTest {
    public static void main(String[] args) throws Exception {
        HikariUtil.init();
        System.out.println(BlogDao.selectBlog(1));
        // Language Level 设置成 Java 8
        BlogDao.selectList();
    }
}
