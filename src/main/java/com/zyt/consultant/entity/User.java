package com.zyt.consultant.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("user") // 对应数据库表名
public class User {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("name")
    private String name;

    @TableField("age")
    private Integer age;

    @TableField("weight")
    private BigDecimal weight; // kg

    @TableField("height")
    private BigDecimal height; // m

    @TableField("bmi")
    private BigDecimal bmi; // 自动计算并存储

    @TableField("phone")
    private String phone;

    @TableField("password")
    private String password;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /**
     * 根据身高和体重计算 BMI
     * height 单位：米，weight 单位：千克
     */
    public void calculateBmi() {
        if (height != null && height.compareTo(BigDecimal.ZERO) > 0
                && weight != null && weight.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal normalizedHeight = height;
            if (normalizedHeight.compareTo(new BigDecimal("3")) > 0) {
                normalizedHeight = normalizedHeight.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            }
            this.bmi = weight.divide(normalizedHeight.multiply(normalizedHeight), 2, RoundingMode.HALF_UP);
        } else {
            this.bmi = null;
        }
    }
}
