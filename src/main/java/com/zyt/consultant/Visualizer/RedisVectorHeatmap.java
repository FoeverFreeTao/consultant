package com.zyt.consultant.Visualizer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.api.RKeys;
import org.redisson.api.RScript;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.labels.StandardXYZToolTipGenerator;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.data.xy.XYDataset;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class RedisVectorHeatmap {
    private static final String[] NUTRITION_FIELDS = {"kcal", "carbohydrate_g", "protein_g", "fat_g", "vitamin_c_mg", "potassium_mg"};
    private static final String[] NUTRITION_LABELS = {"热量(kcal)", "碳水(g)", "蛋白质(g)", "脂肪(g)", "维生素C(mg)", "钾(mg)"};

    public static void main(String[] args) throws Exception {
        String redisAddress = args.length > 0 ? args[0] : "redis://127.0.0.1:6379";
        String keyPattern = args.length > 1 ? args[1] : "*";
        int maxVectors = args.length > 3 ? Integer.parseInt(args[3]) : Integer.MAX_VALUE;
        String redisPassword = args.length > 4 ? args[4] : null;
        String nutritionOutputFile = args.length > 7 ? args[7] : "e:/code/javasepro/consultant/src/main/resources/redisheatmap/nutrition-heatmap.png";

        Config config = new Config();
        if (redisPassword == null || redisPassword.isEmpty()) {
            config.useSingleServer().setAddress(redisAddress);
        } else {
            config.useSingleServer().setAddress(redisAddress).setPassword(redisPassword);
        }
        RedissonClient redisson = Redisson.create(config);

        ObjectMapper mapper = new ObjectMapper();
        List<double[]> nutritionRows = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        RKeys keys = redisson.getKeys();
        List<String> matchedKeys = new ArrayList<>();
        for (String key : keys.getKeysByPattern(keyPattern)) {
            matchedKeys.add(key);
        }
        matchedKeys.sort(Comparator.naturalOrder());

        for (String key : matchedKeys) {
            if (nutritionRows.size() >= maxVectors) {
                break;
            }

            String json = jsonGet(redisson, key);
            if (json == null || json.trim().isEmpty()) {
                continue;
            }

            JsonNode rootNode;
            try {
                rootNode = mapper.readTree(json);
            } catch (Exception ex) {
                continue;
            }
            JsonNode payloadNode = normalizeRoot(rootNode);
            JsonNode nutritionNode = findNutritionNode(payloadNode, mapper);
            if (nutritionNode == null) {
                continue;
            }
            nutritionRows.add(extractNutritionRow(nutritionNode));
            labels.add(resolveLabel(payloadNode, key, nutritionRows.size()));
        }

        redisson.shutdown();

        int n = nutritionRows.size();
        if (n == 0) {
            throw new IllegalStateException("未读取到可用营养数据，请检查 Redis 键模式或文档结构");
        }
        JFreeChart nutritionChart = buildNutritionHeatmap(labels, nutritionRows);
        File nutritionFile = new File(nutritionOutputFile);
        File parentDir = nutritionFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        int nutritionWidth = Math.max(900, NUTRITION_FIELDS.length * 140);
        int nutritionHeight = Math.max(700, n * 26);
        ChartUtils.saveChartAsPNG(nutritionFile, nutritionChart, nutritionWidth, nutritionHeight);
        System.out.println(String.format(Locale.ROOT,
                "营养维度热力图已生成: %s; 食物数量=%d, 指标数量=%d",
                nutritionOutputFile, n, NUTRITION_FIELDS.length));

        if (!GraphicsEnvironment.isHeadless()) {
            JFrame nutritionFrame = new JFrame("Nutrition Heatmap");
            nutritionFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            ChartPanel nutritionPanel = new ChartPanel(nutritionChart);
            nutritionPanel.setDomainZoomable(true);
            nutritionPanel.setRangeZoomable(true);
            nutritionFrame.add(nutritionPanel);
            nutritionFrame.setSize(1100, 900);
            nutritionFrame.setLocationRelativeTo(null);
            nutritionFrame.setVisible(true);
        }
    }

    private static List<ObjectNode> loadContentItems(String contentDir, ObjectMapper mapper) throws IOException {
        Path dirPath = Paths.get(contentDir);
        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            return Collections.emptyList();
        }
        List<Path> files = new ArrayList<>();
        Files.list(dirPath)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                .forEach(files::add);
        files.sort(Comparator.comparing(path -> path.getFileName().toString()));

        List<ObjectNode> items = new ArrayList<>();
        for (Path file : files) {
            JsonNode root = mapper.readTree(file.toFile());
            if (!(root instanceof ArrayNode)) {
                continue;
            }
            for (JsonNode item : root) {
                if (!(item instanceof ObjectNode)) {
                    continue;
                }
                ObjectNode copy = ((ObjectNode) item).deepCopy();
                copy.put("_source_file", file.getFileName().toString());
                items.add(copy);
            }
        }
        return items;
    }

    private static double[] extractNutritionVector(JsonNode item) {
        JsonNode nutrition = item == null ? null : item.get("nutrition_per_100g");
        double[] vector = new double[NUTRITION_FIELDS.length];
        for (int i = 0; i < NUTRITION_FIELDS.length; i++) {
            vector[i] = nutrition == null ? 0 : nutrition.path(NUTRITION_FIELDS[i]).asDouble(0);
        }
        return vector;
    }

    private static List<double[]> normalizeByDimension(List<double[]> vectors) {
        if (vectors.isEmpty()) {
            return Collections.emptyList();
        }
        int dim = vectors.get(0).length;
        double[] min = new double[dim];
        double[] max = new double[dim];
        for (int d = 0; d < dim; d++) {
            min[d] = Double.POSITIVE_INFINITY;
            max[d] = Double.NEGATIVE_INFINITY;
        }
        for (double[] vector : vectors) {
            for (int d = 0; d < dim; d++) {
                min[d] = Math.min(min[d], vector[d]);
                max[d] = Math.max(max[d], vector[d]);
            }
        }

        List<double[]> normalized = new ArrayList<>();
        for (double[] vector : vectors) {
            double[] copy = new double[dim];
            for (int d = 0; d < dim; d++) {
                double range = max[d] - min[d];
                copy[d] = range == 0 ? 0 : (vector[d] - min[d]) / range;
            }
            normalized.add(copy);
        }
        return normalized;
    }

    private static ObjectNode buildRedisVectorDoc(ObjectNode source, double[] vector, ObjectMapper mapper) {
        ObjectNode doc = mapper.createObjectNode();
        doc.put("id", source.path("id").asText(""));
        doc.put("name", source.path("name").asText(""));
        doc.put("category", source.path("category").asText(""));
        doc.put("source_file", source.path("_source_file").asText(""));
        doc.put("text", source.path("text_for_embedding").asText(""));
        JsonNode nutritionNode = source.get("nutrition_per_100g");
        if (nutritionNode != null && nutritionNode.isObject()) {
            doc.set("nutrition_per_100g", nutritionNode.deepCopy());
        }
        ArrayNode embedding = doc.putArray("embedding");
        for (double value : vector) {
            embedding.add(value);
        }
        return doc;
    }

    private static void jsonSet(RedissonClient redisson, String key, String json) {
        String script = "return redis.call('JSON.SET', KEYS[1], ARGV[1], ARGV[2]);";
        redisson.getScript(StringCodec.INSTANCE).eval(RScript.Mode.READ_WRITE, script, RScript.ReturnType.VALUE,
                Collections.singletonList(key), "$", json);
    }

    private static String jsonGet(RedissonClient redisson, String key) {
        String script = "local t=redis.call('TYPE',KEYS[1])['ok'];"
                + "if t=='ReJSON-RL' then return redis.call('JSON.GET',KEYS[1]); end;"
                + "if t=='string' then return redis.call('GET',KEYS[1]); end;"
                + "return nil;";
        Object result = redisson.getScript(StringCodec.INSTANCE).eval(RScript.Mode.READ_ONLY, script, RScript.ReturnType.VALUE,
                Collections.singletonList(key));
        return result == null ? null : String.valueOf(result);
    }

    private static JsonNode normalizeRoot(JsonNode rootNode) {
        if (rootNode == null) {
            return null;
        }
        if (rootNode.isArray() && rootNode.size() > 0 && rootNode.get(0).isObject()) {
            return rootNode.get(0);
        }
        return rootNode;
    }

    private static JsonNode findEmbeddingArray(JsonNode node) {
        if (node == null) {
            return null;
        }
        String[] embeddingFieldNames = {"embedding", "vector", "values"};
        for (String fieldName : embeddingFieldNames) {
            JsonNode field = node.get(fieldName);
            if (field != null && field.isArray()) {
                return field;
            }
        }
        JsonNode metadata = node.get("metadata");
        if (metadata != null && metadata.isObject()) {
            JsonNode nested = findEmbeddingArray(metadata);
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }

    private static String resolveLabel(JsonNode node, String fallbackKey, int index) {
        if (node != null) {
            JsonNode nameNode = node.get("name");
            if (nameNode != null && !nameNode.isNull() && !nameNode.asText().isEmpty()) {
                return nameNode.asText();
            }
            JsonNode idNode = node.get("id");
            if (idNode != null && !idNode.isNull()) {
                return idNode.asText();
            }
        }
        return fallbackKey + "#" + index;
    }

    private static double[] extractNutritionRow(JsonNode nutritionNode) {
        double[] raw = new double[NUTRITION_FIELDS.length];
        for (int i = 0; i < NUTRITION_FIELDS.length; i++) {
            raw[i] = nutritionNode.path(NUTRITION_FIELDS[i]).asDouble(0);
        }
        return raw;
    }

    private static JsonNode findNutritionNode(JsonNode node, ObjectMapper mapper) {
        if (node == null || !node.isObject()) {
            return null;
        }
        JsonNode n1 = node.get("nutrition_per_100g");
        if (isValidNutritionNode(n1)) {
            return n1;
        }
        JsonNode n2 = node.get("nutrition");
        if (isValidNutritionNode(n2)) {
            return n2;
        }
        JsonNode metadata = node.get("metadata");
        if (metadata != null && metadata.isObject()) {
            JsonNode nested = findNutritionNode(metadata, mapper);
            if (nested != null) {
                return nested;
            }
        }
        JsonNode textForEmbedding = node.get("text_for_embedding");
        if (textForEmbedding != null && textForEmbedding.isTextual()) {
            JsonNode parsed = tryParseNutritionFromText(textForEmbedding.asText(), mapper);
            if (parsed != null) {
                return parsed;
            }
        }
        JsonNode text = node.get("text");
        if (text != null && text.isTextual()) {
            JsonNode parsed = tryParseNutritionFromText(text.asText(), mapper);
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    private static JsonNode tryParseNutritionFromText(String text, ObjectMapper mapper) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        if (!(trimmed.startsWith("{") && trimmed.endsWith("}"))) {
            return null;
        }
        try {
            JsonNode maybeJson = mapper.readTree(trimmed);
            return findNutritionNode(maybeJson, mapper);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean isValidNutritionNode(JsonNode nutritionNode) {
        if (nutritionNode == null || !nutritionNode.isObject()) {
            return false;
        }
        for (String field : NUTRITION_FIELDS) {
            if (nutritionNode.has(field)) {
                return true;
            }
        }
        return false;
    }

    private static JFreeChart buildNutritionHeatmap(List<String> labels, List<double[]> nutritionRows) {
        int foodCount = labels.size();
        List<double[]> normalizedRows = normalizeByDimension(nutritionRows);
        int metricCount = NUTRITION_FIELDS.length;
        double[] x = new double[foodCount * metricCount];
        double[] y = new double[foodCount * metricCount];
        double[] z = new double[foodCount * metricCount];
        int idx = 0;
        for (int i = 0; i < foodCount; i++) {
            for (int j = 0; j < metricCount; j++) {
                x[idx] = j;
                y[idx] = i;
                z[idx] = normalizedRows.get(i)[j];
                idx++;
            }
        }

        DefaultXYZDataset dataset = new DefaultXYZDataset();
        dataset.addSeries("Nutrition", new double[][]{x, y, z});
        XYBlockRenderer renderer = new XYBlockRenderer();
        renderer.setBlockWidth(1.0);
        renderer.setBlockHeight(1.0);
        renderer.setPaintScale(new PaintScale() {
            @Override
            public double getLowerBound() {
                return 0;
            }

            @Override
            public double getUpperBound() {
                return 1;
            }

            @Override
            public Paint getPaint(double value) {
                float v = (float) value;
                return new Color(1f, 1f - v, 1f - v);
            }
        });
        renderer.setDefaultToolTipGenerator(new StandardXYZToolTipGenerator() {
            @Override
            public String generateToolTip(XYDataset dataset, int series, int item) {
                int metricIndex = (int) x[item];
                int foodIndex = (int) y[item];
                double rawValue = nutritionRows.get(foodIndex)[metricIndex];
                double normalized = z[item];
                return labels.get(foodIndex) + " - " + NUTRITION_LABELS[metricIndex]
                        + " : 原始值=" + new DecimalFormat("#0.###").format(rawValue)
                        + ", 归一化=" + new DecimalFormat("#0.###").format(normalized);
            }
        });

        SymbolAxis xAxis = new SymbolAxis("营养指标", NUTRITION_LABELS);
        xAxis.setGridBandsVisible(false);
        SymbolAxis yAxis = new SymbolAxis("食物", labels.toArray(new String[0]));
        yAxis.setGridBandsVisible(false);
        XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);
        return new JFreeChart("食物营养维度热力图",
                JFreeChart.DEFAULT_TITLE_FONT, plot, false);
    }
}
