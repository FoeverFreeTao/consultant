package com.zyt.consultant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zyt.consultant.tools.NutritionAnalysisTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class VisionMealAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(VisionMealAnalysisService.class);
    private static final String DEFAULT_MODEL = "qwen-vl-plus";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NutritionAnalysisTool nutritionAnalysisTool;

    @Value("${langchain4j.open-ai.chat-model.base-url}")
    private String baseUrl;

    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String apiKey;

    @Value("${app.vision.model-name:" + DEFAULT_MODEL + "}")
    private String visionModelName;

    @Value("${app.vision.max-image-bytes:8388608}")
    private long maxImageBytes;

    public String analyzeMealImage(MultipartFile image, String userMessage) {
        if (image == null || image.isEmpty()) {
            return "请先上传一张清晰的餐食照片，我才能帮你识别食物和估算营养。";
        }
        if (image.getSize() > maxImageBytes) {
            return "图片太大了，请上传 8MB 以内的餐食照片。";
        }
        String contentType = image.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.toLowerCase().startsWith("image/")) {
            return "当前文件不是可识别的图片格式，请上传 jpg、png 或 webp 等餐食照片。";
        }

        try {
            MealVisionResult visionResult = callVisionModel(image, userMessage);
            if (visionResult.foods().isEmpty()) {
                return buildNoFoodFallback(userMessage);
            }
            String foodText = buildFoodText(visionResult.foods());
            String nutritionResult = nutritionAnalysisTool.analyze(foodText);
            return buildFinalAnswer(visionResult, foodText, nutritionResult);
        } catch (Exception ex) {
            log.warn("meal image analysis fallback triggered, filename={}", image.getOriginalFilename(), ex);
            return "我暂时无法完成图片识别。你可以先用文字告诉我这张餐食里有哪些食物和大致份量，例如“米饭 150g、鸡胸肉 100g、青菜 200g”，我会继续帮你估算热量和营养。";
        }
    }

    private MealVisionResult callVisionModel(MultipartFile image, String userMessage) throws Exception {
        String imageDataUrl = "data:" + image.getContentType() + ";base64," + Base64.getEncoder().encodeToString(image.getBytes());
        String endpoint = trimTrailingSlash(baseUrl) + "/chat/completions";
        WebClient webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        Map<String, Object> request = buildVisionRequest(imageDataUrl, userMessage);
        String response = webClient.post()
                .uri(endpoint)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        String content = extractMessageContent(response);
        return parseVisionResult(content);
    }

    private Map<String, Object> buildVisionRequest(String imageDataUrl, String userMessage) {
        String prompt = """
                请识别这张餐食照片中的食物类型，并估算每种食物的可食用重量克数。

                要求：
                1. 只输出 JSON，不要输出 Markdown、解释或额外文字。
                2. foods 数组最多返回 6 个主要食物。
                3. estimatedGrams 必须是整数，无法判断时给出保守估计。
                4. confidence 取值 0 到 1。
                5. notes 简短说明判断依据或不确定点。
                6. 不要给医疗诊断。

                JSON 格式：
                {
                  "foods": [
                    {"name": "米饭", "estimatedGrams": 150, "confidence": 0.82, "notes": "约一小碗"}
                  ],
                  "overallNote": "整体估算说明"
                }

                用户补充说明：
                %s
                """.formatted(StringUtils.hasText(userMessage) ? userMessage.trim() : "无");

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of(
                "role", "system",
                "content", "你是一个谨慎的餐食图片识别助手，只负责识别食物类型和估算份量。"
        ));
        messages.add(Map.of(
                "role", "user",
                "content", List.of(
                        Map.of("type", "text", "text", prompt),
                        Map.of("type", "image_url", "image_url", Map.of("url", imageDataUrl))
                )
        ));

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", StringUtils.hasText(visionModelName) ? visionModelName : DEFAULT_MODEL);
        request.put("messages", messages);
        request.put("temperature", 0.1);
        return request;
    }

    private String extractMessageContent(String rawResponse) throws Exception {
        if (!StringUtils.hasText(rawResponse)) {
            throw new IllegalStateException("empty vision response");
        }
        JsonNode root = objectMapper.readTree(rawResponse);
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        if (!contentNode.isTextual() || !StringUtils.hasText(contentNode.asText())) {
            throw new IllegalStateException("empty vision message content");
        }
        return contentNode.asText();
    }

    private MealVisionResult parseVisionResult(String content) throws Exception {
        String json = extractJson(content);
        JsonNode root = objectMapper.readTree(json);
        List<RecognizedFood> foods = new ArrayList<>();
        JsonNode foodNodes = root.path("foods");
        if (foodNodes.isArray()) {
            for (JsonNode node : foodNodes) {
                String name = node.path("name").asText("");
                int estimatedGrams = node.path("estimatedGrams").asInt(0);
                double confidence = node.path("confidence").asDouble(0);
                String notes = node.path("notes").asText("");
                if (!StringUtils.hasText(name) || estimatedGrams <= 0) {
                    continue;
                }
                foods.add(new RecognizedFood(name.trim(), Math.min(estimatedGrams, 2000), confidence, notes));
            }
        }
        return new MealVisionResult(foods, root.path("overallNote").asText(""));
    }

    private String extractJson(String content) {
        String cleaned = content == null ? "" : content.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```(?:json)?", "").replaceFirst("```$", "").trim();
        }
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1);
        }
        return cleaned;
    }

    private String buildFoodText(List<RecognizedFood> foods) {
        List<String> items = new ArrayList<>();
        for (RecognizedFood food : foods) {
            items.add(food.name() + " " + food.estimatedGrams() + "g");
        }
        return String.join("，", items);
    }

    private String buildFinalAnswer(MealVisionResult visionResult, String foodText, String nutritionResult) {
        StringBuilder answer = new StringBuilder();
        answer.append("图片识别结果（估算）：\n");
        for (RecognizedFood food : visionResult.foods()) {
            answer.append("- ")
                    .append(food.name())
                    .append("：约 ")
                    .append(food.estimatedGrams())
                    .append("g");
            if (food.confidence() > 0) {
                answer.append("，置信度 ").append(String.format("%.0f%%", food.confidence() * 100));
            }
            if (StringUtils.hasText(food.notes())) {
                answer.append("，").append(food.notes());
            }
            answer.append('\n');
        }
        if (StringUtils.hasText(visionResult.overallNote())) {
            answer.append("\n识别说明：").append(visionResult.overallNote()).append('\n');
        }
        answer.append("\n用于营养估算的食物清单：").append(foodText).append("\n\n");
        answer.append(nutritionResult);
        answer.append("\n提示：图片识别和份量估算会受拍摄角度、餐具大小、遮挡影响，结果适合做日常记录参考；如果你能补充实际克重，我可以再帮你校准。");
        return answer.toString().trim();
    }

    private String buildNoFoodFallback(String userMessage) {
        String suffix = StringUtils.hasText(userMessage) ? "你补充的信息是：“" + userMessage.trim() + "”。" : "";
        return "我没有从图片中识别到明确餐食。" + suffix + "请换一张更清晰、光线更充足、食物完整入镜的照片，或直接用文字告诉我食物名称和份量。";
    }

    private String trimTrailingSlash(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private record MealVisionResult(List<RecognizedFood> foods, String overallNote) {
    }

    private record RecognizedFood(String name, int estimatedGrams, double confidence, String notes) {
    }
}
