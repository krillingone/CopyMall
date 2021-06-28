package com.krill.mall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.krill.common.utils.PageUtils;
import com.krill.mall.coupon.entity.SpuBoundsEntity;

import java.util.Map;

/**
 * 商品spu积分设置
 *
 * @author krill9594
 * @email krilling.one@gmail.com
 * @date 2021-04-11 16:32:57
 */
public interface SpuBoundsService extends IService<SpuBoundsEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

