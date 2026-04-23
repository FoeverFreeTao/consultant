package com.zyt.consultant.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zyt.consultant.entity.BodyStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.List;
@Component
@Mapper
public interface BodyStatusMapper extends BaseMapper<BodyStatus> {

    @Select("SELECT id, type, meal, food FROM foodrecommend WHERE type = #{type}")
    List<BodyStatus> findByType(String type);
}
