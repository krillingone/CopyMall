package com.krill.mall.member.dao;

import com.krill.mall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author krill9594
 * @email krilling.one@gmail.com
 * @date 2021-03-16 22:54:14
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
