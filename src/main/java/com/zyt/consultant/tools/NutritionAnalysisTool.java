package com.zyt.consultant.tools;

import com.zyt.consultant.entity.Food;
import com.zyt.consultant.mapper.FoodMapper;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class NutritionAnalysisTool {

    private static final Logger log = LoggerFactory.getLogger(NutritionAnalysisTool.class);
    private static final Pattern WEIGHT_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*(kg|公斤|g|克|ml|毫升|l|升)");
    private static final String[] NOISE_PHRASES = {
            "帮我算一下", "帮我算算", "帮我计算", "帮忙算一下", "帮忙计算", "请帮我算一下", "请帮我计算",
            "多少卡", "多少热量", "多少卡路里", "热量多少", "卡路里多少", "营养怎么样", "营养如何",
            "我今天吃了", "我今天喝了", "今天吃了", "今天喝了", "我中午吃了", "我晚上吃了", "我早上吃了",
            "早餐吃了", "午餐吃了", "晚餐吃了", "中午吃了", "晚上吃了", "早上吃了", "刚刚吃了",
            "吃了", "喝了", "还有", "以及", "然后", "再加", "外加", "顺便", "一共", "最终", "最后"
    };
    private static final Map<String, FoodNutrition> ROUGH_NUTRITION_PER_100G = buildRoughNutrition();

    @Autowired
    private FoodMapper foodMapper;

    @Tool("分析用户饮食，返回热量和营养建议。支持中文自然语言饮食描述，例如“中午吃了米饭、鸡胸肉和可乐，帮我算多少卡”，也支持直接传“米饭 鸡胸肉 可乐”")
    public String analyze(String foodText) {
        try {
            return analyzeWithFoodDatabase(foodText);
        } catch (Exception ex) {
            log.warn("nutrition analysis fallback triggered, foodText={}", foodText, ex);
            return buildFallbackResponse(foodText);
        }
    }

    private String analyzeWithFoodDatabase(String foodText) {
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

            FoodNutrition nutrition = resolveNutrition(parsedFood.name());
            if (nutrition != null) {
                int calories = nutrition.calories() * parsedFood.weight() / 100;
                double protein = nutrition.protein() * parsedFood.weight() / 100;
                double fat = nutrition.fat() * parsedFood.weight() / 100;
                double carbs = nutrition.carbs() * parsedFood.weight() / 100;

                totalCalories += calories;
                totalProtein += protein;
                totalFat += fat;
                totalCarbs += carbs;

                result.append("🍽️ ").append(nutrition.name()).append(" (").append(parsedFood.weight()).append("g")
                        .append(nutrition.rough() ? "，估算" : "")
                        .append(")：")
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

        result.append("\n💡 建议：");
        if (totalCalories <= 0) {
            result.append("暂未得到有效营养数据，请补充更常见的食物名称或具体克重。\n");
        } else if (totalProtein < 50) {
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

    private String buildFallbackResponse(String foodText) {
        List<String> foods;
        try {
            foods = extractFoodItems(foodText);
        } catch (Exception ex) {
            foods = List.of();
        }
        String foodSummary = foods.isEmpty() ? "暂未识别到明确食物" : String.join("、", foods);
        return "营养分析工具暂时无法查询食物营养数据库，不能给出精确热量。"
                + "已识别内容：" + foodSummary + "。"
                + "你可以先记录每种食物的大致克重或份量，主食、蛋白质、蔬菜和饮料分开写；"
                + "饮食上优先少油少糖、保证优质蛋白和蔬菜摄入。稍后工具恢复后可再做精确计算。";
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
            if (WEIGHT_PATTERN.matcher(part.trim().toLowerCase(Locale.ROOT)).find()) {
                items.add(part.trim());
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

    private FoodNutrition resolveNutrition(String rawName) {
        if (!StringUtils.hasText(rawName)) {
            return null;
        }
        String normalizedName = normalizeFoodName(rawName);
        Food dbFood = findFirstFood(rawName);
        if (dbFood == null && !rawName.equals(normalizedName)) {
            dbFood = findFirstFood(normalizedName);
        }
        if (dbFood != null) {
            return new FoodNutrition(dbFood.getName(), dbFood.getCalories(), dbFood.getProtein(), dbFood.getFat(), dbFood.getCarbs(), false);
        }

        FoodNutrition rough = findRoughNutrition(rawName);
        if (rough == null) {
            rough = findRoughNutrition(normalizedName);
        }
        return rough;
    }

    private Food findFirstFood(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        try {
            List<Food> matchedFoods = foodMapper.findLikeName(name);
            if (matchedFoods != null && !matchedFoods.isEmpty()) {
                return matchedFoods.get(0);
            }
        } catch (Exception ex) {
            log.warn("food database lookup failed, name={}", name, ex);
        }
        return null;
    }

    private FoodNutrition findRoughNutrition(String name) {
        if (!StringUtils.hasText(name)) {
            return null;
        }
        String normalized = normalizeFoodName(name);
        FoodNutrition exact = ROUGH_NUTRITION_PER_100G.get(normalized);
        if (exact != null) {
            return exact;
        }
        for (Map.Entry<String, FoodNutrition> entry : ROUGH_NUTRITION_PER_100G.entrySet()) {
            if (normalized.contains(entry.getKey()) || entry.getKey().contains(normalized)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private String normalizeFoodName(String name) {
        if (!StringUtils.hasText(name)) {
            return "";
        }
        return name.trim()
                .replaceAll("[\\s·•]+", "")
                .replace("（去皮）", "")
                .replace("(去皮)", "")
                .replace("烤", "")
                .replace("煎", "")
                .replace("炸", "")
                .replace("炖", "")
                .replace("蒸", "")
                .replace("水煮", "")
                .replace("清炒", "")
                .replace("红烧", "")
                .replace("熟", "")
                .trim();
    }

    private static Map<String, FoodNutrition> buildRoughNutrition() {
        Map<String, FoodNutrition> values = new LinkedHashMap<>();
        values.put("鸡胸肉", new FoodNutrition("鸡胸肉", 165, 31.0, 3.6, 0.0, true));
        values.put("蓝莓", new FoodNutrition("蓝莓", 57, 0.7, 0.3, 14.0, true));
        values.put("草莓", new FoodNutrition("草莓", 32, 0.7, 0.3, 7.7, true));
        values.put("葡萄", new FoodNutrition("葡萄", 69, 0.7, 0.2, 18.1, true));
        values.put("燕麦", new FoodNutrition("燕麦", 389, 16.9, 6.9, 66.3, true));
        values.put("土豆", new FoodNutrition("土豆", 77, 2.0, 0.1, 17.0, true));
        values.put("青菜", new FoodNutrition("青菜", 20, 1.6, 0.2, 3.4, true));
        values.put("生菜", new FoodNutrition("生菜", 15, 1.4, 0.2, 2.9, true));
        values.put("虾", new FoodNutrition("虾", 99, 24.0, 0.3, 0.2, true));
        values.put("三文鱼", new FoodNutrition("三文鱼", 208, 20.0, 13.0, 0.0, true));
        values.put("豆腐", new FoodNutrition("豆腐", 76, 8.1, 4.8, 1.9, true));
        return values;
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

    private record FoodNutrition(String name, int calories, double protein, double fat, double carbs, boolean rough) {
    }
}
