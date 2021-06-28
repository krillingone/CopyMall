package com.krill.mall.product.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.krill.mall.product.entity.AttrEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 商品属性
 * 
 * @author krill9594
 * @email krilling.one@gmail.com
 * @date 2021-03-16 21:35:08
 */
@Mapper
public interface AttrDao extends BaseMapper<AttrEntity> {

    List<Long> selectSearchableAttrIds(@Param("attrIds") List<Long> attrIds);
}
