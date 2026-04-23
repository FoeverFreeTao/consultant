package com.zyt.consultant.tools;

import com.zyt.consultant.entity.Food;
import com.zyt.consultant.mapper.FoodMapper;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class NutritionAnalysisTool {

    private static final Pattern WEIGHT_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(kg|公斤|g|克|ml|毫升|l|升)");
    private static final String[] NOISE_PHRASES = {
            "帮我算一下", "帮我算算", "帮我计算", "帮忙算一下", "帮忙计算", "请帮我算一下", "请帮我计算",
            "多少卡", "多少热量", "多少卡路里", "热量多少", "卡路里多少", "营养怎么样", "营养如何",
            "我今天吃了", "我今天喝了", "今天吃了", "今天喝了", "我中午吃了", "我晚上吃了", "我早上吃了",
            "早餐吃了", "午餐吃了", "晚餐吃了", "中午吃了", "晚上吃了", "早上吃了", "刚刚吃了",
            "吃了", "喝了", "还有", "以及", "然后", "再加", "外加", "顺便", "一共", "最终", "最后"
    };

    @Autowired
    private FoodMapper foodMapper;

    @Tool("分析用户饮食，返回热量和营养建议。支持中文自然语言饮食描述，例如“中午吃了米饭、鸡胸肉和可乐，帮我算多少卡”，也支持直接传“米饭 鸡胸肉 可乐”")
    public String analyze(String foodText) {
        List<String> foods = extractFoodItems(foodText);
        if (foods.isEmpty()) {
            return "未识别到明确的食物名称，请直接告诉我吃了哪些食物，例如：米饭、鸡胸肉、可乐。";
        }

        int totalCalories = 0;
        double totalProtein = 0;
        double totalFat = 0;
        double totalCarbs = 0;
        StringBuilder result = new StringBuilder();

        for (String item : foods) {
            if (!StringUtils.hasText(item)) {
                continue;
            }

            ParsedFood parsedFood = parseFoodItem(item);
            if (!StringUtils.hasText(parsedFood.name())) {
                continue;
            }

            List<Food> matchedFoods = foodMapper.findLikeName(parsedFood.name());
            if (matchedFoods != null && !matchedFoods.isEmpty()) {
                Food food = matchedFoods.get(0);

                int calories = food.getCalories() * parsedFood.weight() / 100;
                double protein = food.getProtein() * parsedFood.weight() / 100;
                double fat = food.getFat() * parsedFood.weight() / 100;
                double carbs = food.getCarbs() * parsedFood.weight() / 100;

                totalCalories += calories;
                totalProtein += protein;
                totalFat += fat;
                totalCarbs += carbs;

                result.append("🍽️ ").append(food.getName()).append(" (").append(parsedFood.weight()).append("g)：")
                        .append(calories).append(" kcal, ")
                        .append("蛋白质 ").append(String.format("%.1f", protein)).append(" g, ")
                        .append("脂肪 ").append(String.format("%.1f", fat)).append(" g, ")
                        .append("碳水 ").append(String.format("%.1f", carbs)).append(" g\n");
            } else {
                result.append("❓ 未找到食物：").append(parsedFood.name()).append("\n");
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

    private List<String> extractFoodItems(String foodText) {
        if (!StringUtils.hasText(foodText)) {
            return List.of();
        }
        String normalized = normalizeFoodText(foodText);
        if (!StringUtils.hasText(normalized)) {
            return List.of();
        }

        String[] parts = normalized.split("[,，、；;\\n\\t]+");
        List<String> items = new ArrayList<>();
        for (String part : parts) {
            if (!StringUtils.hasText(part)) {
                continue;
            }
            String[] inlineParts = part.trim().split("\\s+");
            for (String inlinePart : inlineParts) {
                if (StringUtils.hasText(inlinePart)) {
                    items.add(inlinePart.trim());
                }
            }
        }
        return items;
    }

    private String normalizeFoodText(String foodText) {
        String normalized = foodText == null ? "" : foodText.trim();
        for (String noisePhrase : NOISE_PHRASES) {
            normalized = normalized.replace(noisePhrase, " ");
        }
        normalized = normalized
                .replace("和", "，")
                .replace("跟", "，")
                .replace("加上", "，")
                .replace("配上", "，")
                .replace("搭配", "，")
                .replace("并且", "，")
                .replace("。", " ")
                .replace("？", " ")
                .replace("?", " ")
                .replace("！", " ")
                .replace("!", " ")
                .replace("：", " ")
                .replace(":", " ")
                .replace("（", " ")
                .replace("）", " ")
                .replace("(", " ")
                .replace(")", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return normalized;
    }

    private ParsedFood parseFoodItem(String item) {
        String normalized = item.trim();
        int weight = 100;

        Matcher matcher = WEIGHT_PATTERN.matcher(normalized.toLowerCase(Locale.ROOT));
        if (matcher.find()) {
            double rawWeight = Double.parseDouble(matcher.group(1));
            String unit = matcher.group(2);
            if ("kg".equals(unit) || "公斤".equals(unit) || "l".equals(unit) || "升".equals(unit)) {
                weight = (int) Math.round(rawWeight * 1000);
            } else {
                weight = (int) Math.round(rawWeight);
            }
            normalized = matcher.replaceAll("").trim();
        } else {
            String nameWithoutDigits = normalized.replaceAll("\\d+", "").trim();
            if (StringUtils.hasText(nameWithoutDigits)) {
                normalized = nameWithoutDigits;
            }
        }

        normalized = normalized
                .replace("一份", "")
                .replace("一杯", "")
                .replace("一瓶", "")
                .replace("一个", "")
                .replace("一碗", "")
                .replace("一盘", "")
                .trim();

        return new ParsedFood(normalized, weight);
    }

    private record ParsedFood(String name, int weight) {
    }
}
