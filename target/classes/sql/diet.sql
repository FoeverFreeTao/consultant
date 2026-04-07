/*
 Navicat Premium Data Transfer

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80042 (8.0.42)
 Source Host           : localhost:3306
 Source Schema         : diet

 Target Server Type    : MySQL
 Target Server Version : 80042 (8.0.42)
 File Encoding         : 65001

 Date: 07/04/2026 16:57:09
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for food
-- ----------------------------
DROP TABLE IF EXISTS `food`;
CREATE TABLE `food`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `calories` int NULL DEFAULT NULL,
  `protein` double NULL DEFAULT NULL,
  `fat` double NULL DEFAULT NULL,
  `carbs` double NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 29 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of food
-- ----------------------------
INSERT INTO `food` VALUES (1, '米饭', 116, 2.6, 0.3, 25.9);
INSERT INTO `food` VALUES (2, '面条', 138, 4.5, 0.6, 27);
INSERT INTO `food` VALUES (3, '馒头', 223, 7, 1.1, 47);
INSERT INTO `food` VALUES (4, '全麦面包', 247, 13, 4.2, 41);
INSERT INTO `food` VALUES (5, '红薯', 86, 1.6, 0.1, 20.1);
INSERT INTO `food` VALUES (6, '鸡胸肉', 165, 31, 3.6, 0);
INSERT INTO `food` VALUES (7, '鸡腿肉', 215, 18, 15, 0);
INSERT INTO `food` VALUES (8, '牛肉', 250, 26, 15, 0);
INSERT INTO `food` VALUES (9, '猪肉', 242, 27, 14, 0);
INSERT INTO `food` VALUES (10, '鱼肉', 206, 22, 12, 0);
INSERT INTO `food` VALUES (11, '鸡蛋', 155, 13, 11, 1.1);
INSERT INTO `food` VALUES (12, '牛奶', 60, 3.2, 3.3, 5);
INSERT INTO `food` VALUES (13, '酸奶', 72, 3.5, 3, 8);
INSERT INTO `food` VALUES (14, '西兰花', 34, 2.8, 0.4, 6.6);
INSERT INTO `food` VALUES (15, '黄瓜', 16, 0.7, 0.1, 3.6);
INSERT INTO `food` VALUES (16, '番茄', 18, 0.9, 0.2, 3.9);
INSERT INTO `food` VALUES (17, '胡萝卜', 41, 0.9, 0.2, 9.6);
INSERT INTO `food` VALUES (18, '菠菜', 23, 2.9, 0.4, 3.6);
INSERT INTO `food` VALUES (19, '苹果', 52, 0.3, 0.2, 14);
INSERT INTO `food` VALUES (20, '香蕉', 89, 1.1, 0.3, 23);
INSERT INTO `food` VALUES (21, '橙子', 47, 0.9, 0.1, 12);
INSERT INTO `food` VALUES (22, '可乐', 42, 0, 0, 10.6);
INSERT INTO `food` VALUES (23, '果汁', 45, 0.5, 0.1, 11);
INSERT INTO `food` VALUES (24, '炸鸡', 260, 20, 18, 8);
INSERT INTO `food` VALUES (25, '薯条', 312, 3.4, 15, 41);
INSERT INTO `food` VALUES (26, '汉堡', 295, 17, 14, 30);
INSERT INTO `food` VALUES (27, '蛋白粉', 400, 80, 5, 8);
INSERT INTO `food` VALUES (28, '坚果', 607, 20, 54, 21);

-- ----------------------------
-- Table structure for foodrecommend
-- ----------------------------
DROP TABLE IF EXISTS `foodrecommend`;
CREATE TABLE `foodrecommend`  (
  `id` int NOT NULL AUTO_INCREMENT,
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `meal` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `food` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 19 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of foodrecommend
-- ----------------------------
INSERT INTO `foodrecommend` VALUES (1, '偏瘦', '早餐', '牛奶 + 鸡蛋 + 全麦面包');
INSERT INTO `foodrecommend` VALUES (2, '偏瘦', '早餐', '豆浆 + 包子 + 鸡蛋');
INSERT INTO `foodrecommend` VALUES (3, '偏瘦', '午餐', '米饭 + 红烧牛肉 + 西兰花');
INSERT INTO `foodrecommend` VALUES (4, '偏瘦', '午餐', '米饭 + 鸡胸肉 + 土豆');
INSERT INTO `foodrecommend` VALUES (5, '偏瘦', '晚餐', '面条 + 鸡蛋 + 青菜');
INSERT INTO `foodrecommend` VALUES (6, '偏瘦', '晚餐', '米饭 + 鱼肉 + 胡萝卜');
INSERT INTO `foodrecommend` VALUES (7, '正常', '早餐', '燕麦 + 牛奶 + 水果');
INSERT INTO `foodrecommend` VALUES (8, '正常', '早餐', '鸡蛋 + 全麦面包 + 牛奶');
INSERT INTO `foodrecommend` VALUES (9, '正常', '午餐', '米饭 + 鸡胸肉 + 西兰花');
INSERT INTO `foodrecommend` VALUES (10, '正常', '午餐', '米饭 + 鱼肉 + 青菜');
INSERT INTO `foodrecommend` VALUES (11, '正常', '晚餐', '粥 + 鸡蛋 + 蔬菜');
INSERT INTO `foodrecommend` VALUES (12, '正常', '晚餐', '全麦面包 + 牛奶 + 水果');
INSERT INTO `foodrecommend` VALUES (13, '肥胖', '早餐', '燕麦 + 水煮蛋');
INSERT INTO `foodrecommend` VALUES (14, '肥胖', '早餐', '全麦面包 + 黑咖啡');
INSERT INTO `foodrecommend` VALUES (15, '肥胖', '午餐', '鸡胸肉 + 西兰花 + 少量米饭');
INSERT INTO `foodrecommend` VALUES (16, '肥胖', '午餐', '鱼肉 + 青菜 + 少油');
INSERT INTO `foodrecommend` VALUES (17, '肥胖', '晚餐', '水果沙拉 + 酸奶');
INSERT INTO `foodrecommend` VALUES (18, '肥胖', '晚餐', '蔬菜沙拉 + 鸡蛋');

-- ----------------------------
-- Table structure for user
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `age` int NOT NULL,
  `weight` decimal(5, 2) NOT NULL,
  `height` decimal(5, 2) NOT NULL,
  `bmi` decimal(5, 2) NOT NULL,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `phone`(`phone` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user
-- ----------------------------
INSERT INTO `user` VALUES (1, '涛', 22, 65.00, 181.00, 17.00, '15312339659', '75e2da41e44187a01bb78e87e810fc5352b4a872c75fad3e6e46f431ac79be10', '2026-04-03 18:29:20', '2026-04-03 18:42:16');
INSERT INTO `user` VALUES (4, '兰', 20, 47.00, 1.62, 17.91, '19102665523', '75e2da41e44187a01bb78e87e810fc5352b4a872c75fad3e6e46f431ac79be10', '2026-04-03 18:49:30', '2026-04-06 17:00:23');

-- ----------------------------
-- Table structure for user_daily_status
-- ----------------------------
DROP TABLE IF EXISTS `user_daily_status`;
CREATE TABLE `user_daily_status`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `hydration_ml` int NOT NULL DEFAULT 1350,
  `sleep_hour` decimal(4, 1) NOT NULL DEFAULT 7.2,
  `activity_minute` int NOT NULL DEFAULT 42,
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_daily_status_user_id`(`user_id` ASC) USING BTREE,
  CONSTRAINT `fk_user_daily_status_user_id` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of user_daily_status
-- ----------------------------
INSERT INTO `user_daily_status` VALUES (1, 4, 1200, 8.0, 50, '2026-04-06 16:32:44', '2026-04-06 17:04:06');

SET FOREIGN_KEY_CHECKS = 1;
