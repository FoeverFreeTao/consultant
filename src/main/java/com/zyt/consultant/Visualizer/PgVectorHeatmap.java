package com.zyt.consultant.Visualizer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.SymbolAxis;
import org.jfree.chart.labels.StandardXYZToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.data.xy.XYDataset;

import javax.swing.JFrame;
import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.awt.Paint;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class PgVectorHeatmap {

    private static final String[] NUTRITION_FIELDS = {"kcal", "carbohydrate_g", "protein_g", "fat_g", "vitamin_c_mg", "potassium_mg"};
    private static final String[] NUTRITION_LABELS = {"热量(kcal)", "碳水(g)", "蛋白质(g)", "脂肪(g)", "维生素C(mg)", "钾(mg)"};

    public static void main(String[] args) throws Exception {
        String contentDir = args.length > 0 ? args[0] : "src/main/resources/content";
        int maxRows = args.length > 1 ? Integer.parseInt(args[1]) : Integer.MAX_VALUE;
        String output = args.length > 2 ? args[2] : "e:/code/javasepro/consultant/src/main/resources/redisheatmap/nutrition-heatmap.png";

        ObjectMapper mapper = new ObjectMapper();
        List<ObjectNode> items = loadContentItems(contentDir, mapper);

        List<double[]> nutritionRows = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        for (ObjectNode item : items) {
            if (nutritionRows.size() >= maxRows) {
                break;
            }
            JsonNode nutritionNode = item.get("nutrition_per_100g");
            if (!isValidNutritionNode(nutritionNode)) {
                continue;
            }
            String name = item.path("name").asText("").trim();
            if (name.isEmpty()) {
                continue;
            }
            String category = item.path("category").asText("").trim();
            String label = category.isEmpty() ? name : (name + "（" + category + "）");
            labels.add(label);
            nutritionRows.add(extractNutritionRow(nutritionNode));
        }

        if (nutritionRows.isEmpty()) {
            throw new IllegalStateException("未在 content 数据中读取到可用营养数据。");
        }

        JFreeChart chart = buildNutritionHeatmap(labels, nutritionRows);
        File outputFile = new File(output);
        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        int width = Math.max(1100, NUTRITION_FIELDS.length * 180);
        int height = Math.max(800, labels.size() * 20);
        ChartUtils.saveChartAsPNG(outputFile, chart, width, height);

        System.out.println(String.format(Locale.ROOT,
                "营养热力图已生成: %s; 食物数=%d, 指标数=%d",
                output, labels.size(), NUTRITION_FIELDS.length));

        if (!GraphicsEnvironment.isHeadless()) {
            JFrame frame = new JFrame("营养热力图（pgvector）");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            ChartPanel panel = new ChartPanel(chart);
            panel.setDomainZoomable(true);
            panel.setRangeZoomable(true);
            frame.add(panel);
            frame.setSize(1300, 900);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        }
    }

    private static List<ObjectNode> loadContentItems(String contentDir, ObjectMapper mapper) throws IOException {
        Path dirPath = Paths.get(contentDir);
        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            return List.of();
        }
        List<Path> files = new ArrayList<>();
        try (var stream = Files.list(dirPath)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .forEach(files::add);
        }
        files.sort(Comparator.comparing(path -> path.getFileName().toString()));

        List<ObjectNode> items = new ArrayList<>();
        for (Path file : files) {
            JsonNode root = mapper.readTree(file.toFile());
            if (!(root instanceof ArrayNode arrayNode)) {
                continue;
            }
            for (JsonNode item : arrayNode) {
                if (!(item instanceof ObjectNode objectNode)) {
                    continue;
                }
                ObjectNode copy = objectNode.deepCopy();
                copy.put("_source_file", file.getFileName().toString());
                items.add(copy);
            }
        }
        return items;
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

    private static double[] extractNutritionRow(JsonNode nutritionNode) {
        double[] raw = new double[NUTRITION_FIELDS.length];
        for (int i = 0; i < NUTRITION_FIELDS.length; i++) {
            raw[i] = nutritionNode.path(NUTRITION_FIELDS[i]).asDouble(0);
        }
        return raw;
    }

    private static List<double[]> normalizeByDimension(List<double[]> vectors) {
        if (vectors.isEmpty()) {
            return List.of();
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
        dataset.addSeries("nutrition", new double[][]{x, y, z});

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
                        + "：原始=" + new DecimalFormat("#0.###").format(rawValue)
                        + "，归一化=" + new DecimalFormat("#0.###").format(normalized);
            }
        });

        SymbolAxis xAxis = new SymbolAxis("营养指标", NUTRITION_LABELS);
        xAxis.setGridBandsVisible(false);
        SymbolAxis yAxis = new SymbolAxis("食物", labels.toArray(new String[0]));
        yAxis.setGridBandsVisible(false);

        XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);
        return new JFreeChart("食物营养维度热力图", JFreeChart.DEFAULT_TITLE_FONT, plot, false);
    }
}
