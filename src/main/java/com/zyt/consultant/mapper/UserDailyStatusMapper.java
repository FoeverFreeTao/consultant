package com.zyt.consultant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyt.consultant.entity.UserDailyStatus;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Component;

@Component
@Mapper
public interface UserDailyStatusMapper extends BaseMapper<UserDailyStatus> {

    @Select("select id, user_id as userId, hydration_ml as hydrationMl, sleep_hour as sleepHour, activity_minute as activityMinute, created_at as createdAt, updated_at as updatedAt from user_daily_status where user_id = #{userId} limit 1")
    UserDailyStatus findByUserId(Long userId);

    @Insert("insert into user_daily_status(user_id, hydration_ml, sleep_hour, activity_minute, created_at, updated_at) values(#{userId}, #{hydrationMl}, #{sleepHour}, #{activityMinute}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertStatus(UserDailyStatus status);

    @Update("update user_daily_status set hydration_ml = #{hydrationMl}, sleep_hour = #{sleepHour}, activity_minute = #{activityMinute}, updated_at = #{updatedAt} where user_id = #{userId}")
    int updateByUserId(UserDailyStatus status);
}
