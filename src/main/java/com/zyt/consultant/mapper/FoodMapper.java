package com.zyt.consultant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyt.consultant.entity.Food;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Mapper
public interface FoodMapper extends BaseMapper<Food> {
    // 根据食物名模糊查询，多条返回
    @Select("SELECT id, name, calories, protein, fat, carbs FROM food WHERE name LIKE CONCAT('%', #{name}, '%')")
    List<Food> findLikeName(String name);

}
