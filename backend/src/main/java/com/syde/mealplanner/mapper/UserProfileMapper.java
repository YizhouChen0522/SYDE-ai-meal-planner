package com.syde.mealplanner.mapper;

import com.syde.mealplanner.entity.UserProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserProfileMapper {

    int insert(UserProfile userProfile);

    UserProfile selectByUserId(@Param("userId") Long userId);

    int updateByUserId(UserProfile userProfile);
}
