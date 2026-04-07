package com.zyt.consultant.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("food")
public class Food {
        @TableId
        private Long id;
        private String name;
        private Integer calories;
        private Double protein;
        private Double fat;
        private Double carbs;
}