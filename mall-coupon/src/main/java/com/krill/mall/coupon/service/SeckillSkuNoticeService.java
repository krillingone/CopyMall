package com.krill.mall.coupon.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.krill.common.utils.PageUtils;
import com.krill.mall.coupon.entity.SeckillSkuNoticeEntity;

import java.util.Map;

/**
 * 秒杀商品通知订阅
 *
 * @author krill9594
 * @email krilling.one@gmail.com
 * @date 2021-04-11 16:32:57
 */
public interface SeckillSkuNoticeService extends IService<SeckillSkuNoticeEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

