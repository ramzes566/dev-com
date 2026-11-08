package com.y1.community.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.y1.community.user.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {}