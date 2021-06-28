package com.krill.mall.product.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.krill.mall.product.entity.AttrGroupEntity;
import com.krill.mall.product.vo.SpuItemGroupAttrVo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 属性分组
 * 
 * @author krill9594
 * @email krilling.one@gmail.com
 * @date 2021-03-16 21:35:08
 */
@Mapper
public interface AttrGroupDao extends BaseMapper<AttrGroupEntity> {

    List<SpuItemGroupAttrVo> getAttrGroupWithAttrsBySpuId(@Param("spuId") Long spuId, @Param("catalogId") Long catalogId);
}
