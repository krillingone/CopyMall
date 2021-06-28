package com.krill.mall.member.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.krill.common.utils.PageUtils;
import com.krill.mall.member.entity.MemberCollectSpuEntity;

import java.util.Map;

/**
 * 会员收藏的商品
 *
 * @author krill9594
 * @email krilling.one@gmail.com
 * @date 2021-03-16 22:54:14
 */
public interface MemberCollectSpuService extends IService<MemberCollectSpuEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

