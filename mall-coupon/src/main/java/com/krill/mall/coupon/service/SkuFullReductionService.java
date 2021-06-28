package com.krill.mall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.krill.common.to.SkuReductionTo;
import com.krill.common.utils.PageUtils;
import com.krill.mall.coupon.entity.SkuFullReductionEntity;

import java.util.Map;

/**
 * 商品满减信息
 *
 * @author krill9594
 * @email krilling.one@gmail.com
 * @date 2021-04-11 16:32:57
 */
public interface SkuFullReductionService extends IService<SkuFullReductionEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void saveSkuReduction(SkuReductionTo skuReductionTo);
}

