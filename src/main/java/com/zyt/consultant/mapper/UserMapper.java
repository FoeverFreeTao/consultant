package com.zyt.consultant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyt.consultant.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Component;

@Component
@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Select("select count(1) from user where phone = #{phone}")
    Long countByPhone(String phone);

    @Select("select count(1) from user where id = #{id}")
    Long countById(Long id);

    @Select("select id, name, age, weight, height, bmi, phone, password, created_at, updated_at from user where phone = #{phone} limit 1")
    User findByPhone(String phone);

    @Insert("insert into user(name, age, weight, height, bmi, phone, password, created_at, updated_at) values(#{name}, #{age}, #{weight}, #{height}, #{bmi}, #{phone}, #{password}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertUser(User user);

    @Update("update user set name = #{name}, age = #{age}, weight = #{weight}, height = #{height}, bmi = #{bmi}, updated_at = #{updatedAt} where phone = #{phone}")
    int updateProfileByPhone(User user);
}
