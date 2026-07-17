package com.syde.mealplanner.mapper;

import com.syde.mealplanner.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    int insert(User user);

    User selectById(@Param("id") Long id);

    User selectByEmail(@Param("email") String email);

    int updateStatusById(@Param("id") Long id, @Param("status") Integer status);
}
