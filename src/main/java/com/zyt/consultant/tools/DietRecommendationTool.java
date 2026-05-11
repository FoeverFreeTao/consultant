package com.zyt.consultant.tools;

import com.zyt.consultant.entity.BodyStatus;
import com.zyt.consultant.mapper.BodyStatusMapper;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DietRecommendationTool {

    private static final Logger log = LoggerFactory.getLogger(DietRecommendationTool.class);

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
            return buildFallbackRecommendation(null, "BMI 不能为空，请提供身高体重或 BMI 后再生成更准确的推荐。");
        }

        double bmi = bmiObj.doubleValue();

        // 判断体型
        String type = getBodyType(bmi);

        try {
            // 调用自定义 Mapper 方法
            List<BodyStatus> list = bodyStatusMapper.findByType(type);
            if (list == null || list.isEmpty()) {
                return buildFallbackRecommendation(bmi, "暂未查询到匹配的饮食模板，已返回通用推荐。");
            }

            // 转成 Map<meal, food>
            Map<String, String> meals = list.stream()
                    .collect(Collectors.toMap(
                            BodyStatus::getMeal,
                            BodyStatus::getFood,
                            (first, ignored) -> first,
                            LinkedHashMap::new
                    ));

            // 返回结构化结果
            return Map.of(
                    "bmi", bmi,
                    "type", type,
                    "meals", meals,
                    "fallback", false
            );
        } catch (Exception ex) {
            log.warn("diet recommendation fallback triggered, bmi={}", bmi, ex);
            return buildFallbackRecommendation(bmi, "饮食推荐工具暂时无法查询数据库，已返回通用推荐。");
        }
    }

    private String getBodyType(double bmi) {
        if (bmi < 18.5) return "偏瘦";
        else if (bmi < 24) return "正常";
        else return "肥胖";
    }

    private Map<String, Object> buildFallbackRecommendation(Double bmi, String message) {
        String type = bmi == null ? "未知" : getBodyType(bmi);
        Map<String, String> meals = switch (type) {
            case "偏瘦" -> Map.of(
                    "早餐", "牛奶或豆浆 + 鸡蛋 + 全麦面包",
                    "午餐", "米饭 + 鱼肉/鸡肉 + 深色蔬菜",
                    "晚餐", "杂粮主食 + 豆制品 + 蔬菜"
            );
            case "肥胖" -> Map.of(
                    "早餐", "无糖豆浆 + 鸡蛋 + 少量全谷物",
                    "午餐", "半份主食 + 瘦肉/鱼虾 + 足量蔬菜",
                    "晚餐", "清淡蛋白质 + 蔬菜，少油少糖"
            );
            default -> Map.of(
                    "早餐", "鸡蛋 + 牛奶/豆浆 + 全谷物",
                    "午餐", "主食 + 优质蛋白 + 两种蔬菜",
                    "晚餐", "清淡主食 + 蛋白质 + 蔬菜"
            );
        };
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("bmi", bmi);
        result.put("type", type);
        result.put("meals", meals);
        result.put("fallback", true);
        result.put("message", message);
        return result;
    }
}
