package com.zyt.consultant.tools;

import com.zyt.consultant.entity.Food;
import com.zyt.consultant.mapper.FoodMapper;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class NutritionAnalysisTool {

    @Autowired
    private FoodMapper foodMapper;

    @Tool("分析用户饮食，返回热量和营养建议")
    public String analyze(String foodText) {

        // 拆分食物，支持逗号、空格或中文逗号
        String[] foods = foodText.split("[,， ]+");
        int totalCalories = 0;
        double totalProtein = 0;
        double totalFat = 0;
        double totalCarbs = 0;
        StringBuilder result = new StringBuilder();

        for (String f : foods) {
            if (f.trim().isEmpty()) continue;

            // 解析食物名和重量，如 "苹果150" 或 "鸡胸肉"
            String name = f.replaceAll("\\d+", "").trim();
            int weight = 100; // 默认100g
            String numPart = f.replaceAll("\\D+", "");
            if (!numPart.isEmpty()) {
                weight = Integer.parseInt(numPart);
            }

            // 模糊查询食物，取第一条匹配
            List<Food> matchedFoods = foodMapper.findLikeName(name);
            if (matchedFoods != null && !matchedFoods.isEmpty()) {
                Food food = matchedFoods.get(0);

                int calories = food.getCalories() * weight / 100;
                double protein = food.getProtein() * weight / 100;
                double fat = food.getFat() * weight / 100;
                double carbs = food.getCarbs() * weight / 100;

                totalCalories += calories;
                totalProtein += protein;
                totalFat += fat;
                totalCarbs += carbs;

                result.append("🍽️ ").append(food.getName()).append(" (").append(weight).append("g)：")
                        .append(calories).append(" kcal, ")
                        .append("蛋白质 ").append(String.format("%.1f", protein)).append(" g, ")
                        .append("脂肪 ").append(String.format("%.1f", fat)).append(" g, ")
                        .append("碳水 ").append(String.format("%.1f", carbs)).append(" g\n");
            } else {
                result.append("❓ 未找到食物：").append(f).append("\n");
            }
        }

        result.append("\n🔥 总热量：").append(totalCalories).append(" kcal\n")
                .append("总蛋白质：").append(String.format("%.1f", totalProtein)).append(" g\n")
                .append("总脂肪：").append(String.format("%.1f", totalFat)).append(" g\n")
                .append("总碳水：").append(String.format("%.1f", totalCarbs)).append(" g\n");

        // 可根据总热量和营养比提供简单建议
        result.append("\n💡 建议：");
        if (totalProtein < 50) {
            result.append("蛋白质略低，可适量增加鸡蛋、鱼、豆类。\n");
        }
        if (totalFat > 70) {
            result.append("脂肪较高，注意油脂摄入。\n");
        }
        if (totalCarbs < 130) {
            result.append("碳水偏低，可适量增加米饭、面食、蔬菜等。\n");
        }

        return result.toString();
    }
}