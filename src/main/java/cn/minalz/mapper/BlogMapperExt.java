package cn.minalz.mapper;

import cn.minalz.domain.Blog;

/**
 *
 * 扩展类继承了MBG生成的接口和Statement
 * @Author: minalz
 */
public interface BlogMapperExt extends BlogMapper {
    /**
     * 根据名称查询文章
     * @param name
     * @return
     */
    public Blog selectBlogByName(String name);
}
