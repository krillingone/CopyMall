package com.krill.mall.product.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.krill.common.utils.PageUtils;
import com.krill.mall.product.entity.CommentReplayEntity;

import java.util.Map;

/**
 * 商品评价回复关系
 *
 * @author krill9594
 * @email krilling.one@gmail.com
 * @date 2021-03-16 21:35:08
 */
public interface CommentReplayService extends IService<CommentReplayEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

