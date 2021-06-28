package com.krill.mall.coupon.dao;

import com.krill.mall.coupon.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author krill9594
 * @email krilling.one@gmail.com
 * @date 2021-04-11 16:32:57
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
