package com.zyt.consultant.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("foodrecommend") // 关键！必须指定表名
public class BodyStatus {

    private Integer id;

    private String type;  // 偏瘦 / 正常 / 肥胖

    private String meal;  // 早餐 / 午餐 / 晚餐

    private String food;  // 食物推荐
}