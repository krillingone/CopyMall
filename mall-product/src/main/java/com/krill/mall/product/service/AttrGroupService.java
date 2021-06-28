package com.krill.mall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.krill.common.utils.PageUtils;
import com.krill.mall.product.entity.AttrGroupEntity;
import com.krill.mall.product.vo.AttrGroupWithAttrsVo;
import com.krill.mall.product.vo.SpuItemGroupAttrVo;

import java.util.List;
import java.util.Map;

/**
 * 属性分组
 *
 * @author krill9594
 * @email krilling.one@gmail.com
 * @date 2021-03-16 21:35:08
 */
public interface AttrGroupService extends IService<AttrGroupEntity> {

    PageUtils queryPage(Map<String, Object> params);

    PageUtils queryPage(Map<String, Object> params, Long catelogId);

    List<AttrGroupWithAttrsVo> getAttrGroupWithAttrsByCatelogId(Long catelogId);

    List<SpuItemGroupAttrVo> getAttrGroupWithAttrsBySpuId(Long spuId, Long catalogId);
}

