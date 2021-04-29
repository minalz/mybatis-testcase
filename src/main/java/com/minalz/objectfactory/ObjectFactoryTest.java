package com.minalz.objectfactory;

import com.minalz.domain.Blog;

/**
 * @Author: minalz
 */
public class ObjectFactoryTest {
    public static void main(String[] args) {
        GPObjectFactory factory = new GPObjectFactory();
        Blog myBlog = (Blog) factory.create(Blog.class);
        System.out.println(myBlog);
    }
}
