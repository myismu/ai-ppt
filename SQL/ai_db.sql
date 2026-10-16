/*
 Navicat Premium Dump SQL

 Source Server         : 本地
 Source Server Type    : MySQL
 Source Server Version : 50737 (5.7.37)
 Source Host           : localhost:3306
 Source Schema         : ai_db

 Target Server Type    : MySQL
 Target Server Version : 50737 (5.7.37)
 File Encoding         : 65001

 Date: 12/04/2026 17:07:54
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ai_generation_record
-- ----------------------------
DROP TABLE IF EXISTS `ai_generation_record`;
CREATE TABLE `ai_generation_record`  (
  `id` bigint(20) NOT NULL COMMENT '记录 ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户 ID',
  `task_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '任务类型: outline, content, polish, draw',
  `prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户输入的原始指令',
  `model_used` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '使用的模型 (如 mimo-v2-pro)',
  `tokens_consumed` int(11) NOT NULL DEFAULT 0 COMMENT '消耗的 Token 数量 (用于成本核算)',
  `status` tinyint(2) NOT NULL DEFAULT 1 COMMENT '状态: 0-失败, 1-成功',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '调用时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  INDEX `idx_create_time`(`create_time`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'AI 调用统计与审计表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_generation_record
-- ----------------------------

-- ----------------------------
-- Table structure for ai_model_config
-- ----------------------------
DROP TABLE IF EXISTS `ai_model_config`;
CREATE TABLE `ai_model_config`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '配置 ID',
  `provider` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '供应商名称(如: xiaomi, doubao)',
  `scene_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '适用场景(如: text, image)',
  `api_format` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT 'API协议格式: anthropic, openai',
  `model_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '模型实际调用名称',
  `base_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '接口基础地址',
  `api_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'API Key',
  `extra_config` json NULL COMMENT '其他JSON扩展配置',
  `is_default` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否为对应场景的默认模型: 0-否, 1-是',
  `is_active` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用: 0-禁用, 1-启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_scene_active`(`scene_type`, `is_active`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 3 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'AI模型动态配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_model_config
-- ----------------------------
INSERT INTO `ai_model_config` VALUES (1, 'xiaomi', 'text', 'anthropic', 'mimo-v2-flash', 'https://api.xiaomimimo.com/anthropic/v1/messages', 'your_text_model_api_key', '{\"anthropicVersion\": \"2023-06-01\"}', 1, 1, '2026-04-12 16:26:07', '2026-04-12 16:56:06');
INSERT INTO `ai_model_config` VALUES (2, 'doubao', 'image', NULL, 'doubao-image-v2', 'https://ark.cn-beijing.volces.com/api/v3/images/generations', 'your_image_model_api_key', '{}', 1, 1, '2026-04-12 16:26:07', '2026-04-12 16:26:07');

-- ----------------------------
-- Table structure for ai_prompt_template
-- ----------------------------
DROP TABLE IF EXISTS `ai_prompt_template`;
CREATE TABLE `ai_prompt_template`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '提示词 ID',
  `scene_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '场景编码 (如: outline_gen, img_prompt_enhance)',
  `system_prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '系统级人设指令',
  `user_prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户输入模板 (含 ${变量} 占位符)',
  `is_active` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用: 0-禁用, 1-启用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_scene_code`(`scene_code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '系统内置提示词表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_prompt_template
-- ----------------------------

-- ----------------------------
-- Table structure for undo_log
-- ----------------------------
DROP TABLE IF EXISTS `undo_log`;
CREATE TABLE `undo_log`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `branch_id` bigint(20) NOT NULL,
  `xid` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `context` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `rollback_info` longblob NOT NULL,
  `log_status` int(11) NOT NULL,
  `log_created` datetime NOT NULL,
  `log_modified` datetime NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `ux_undo_log`(`xid`, `branch_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'Seata AT 模式回滚日志表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of undo_log
-- ----------------------------

SET FOREIGN_KEY_CHECKS = 1;
