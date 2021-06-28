package com.krill.mall.ware.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.krill.common.utils.PageUtils;
import com.krill.mall.ware.entity.UndoLogEntity;

import java.util.Map;

/**
 * 
 *
 * @author krill9594
 * @email krilling.one@gmail.com
 * @date 2021-03-16 23:36:29
 */
public interface UndoLogService extends IService<UndoLogEntity> {

    PageUtils queryPage(Map<String, Object> params);
}

