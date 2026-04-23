package com.zyt.consultant.tools;

import com.zyt.consultant.entity.BodyStatus;
import com.zyt.consultant.mapper.BodyStatusMapper;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DietRecommendationTool {

    @Autowired
    private BodyStatusMapper bodyStatusMapper;

    /**
     * 根据 BMI 推荐饮食，返回结构化数据，便于前端展示
     *
     * @param bmiObj 用户当前 BMI
     * @return Map 格式 { "bmi": 22.5, "type": "正常", "meals": { "早餐": "xxx", "午餐": "xxx", "晚餐": "xxx" } }
     */
    @Tool("根据BMI推荐饮食")
    public Map<String, Object> recommendByBmi(BigDecimal bmiObj) {

        if (bmiObj == null) {
            throw new IllegalArgumentException("BMI 不能为空");
        }

        double bmi = bmiObj.doubleValue();

        // 判断体型
        String type = getBodyType(bmi);

        // 调用自定义 Mapper 方法
        List<BodyStatus> list = bodyStatusMapper.findByType(type);

        // 转成 Map<meal, food>
        Map<String, String> meals = list.stream()
                .collect(Collectors.toMap(BodyStatus::getMeal, BodyStatus::getFood));

        // 返回结构化结果
        return Map.of(
                "bmi", bmi,
                "type", type,
                "meals", meals
        );
    }

    private String getBodyType(double bmi) {
        if (bmi < 18.5) return "偏瘦";
        else if (bmi < 24) return "正常";
        else return "肥胖";
    }
}