-- PPT 服务数据库脚本

CREATE DATABASE IF NOT EXISTS `ppt_service` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `ppt_service`;

-- 1. ppt_project (用户 PPT 项目主表)
CREATE TABLE IF NOT EXISTS `ppt_project` (
  `id` bigint(20) NOT NULL COMMENT '项目唯一标识 (雪花算法)',
  `user_id` bigint(20) NOT NULL COMMENT '归属用户 ID',
  `title` varchar(128) DEFAULT NULL COMMENT 'PPT 标题',
  `cover_url` varchar(512) DEFAULT NULL COMMENT '封面图地址',
  `template_id` bigint(20) DEFAULT NULL COMMENT '关联模板 ID（可为空，表示未使用模板）',
  `content_url` varchar(512) DEFAULT NULL COMMENT '幻灯片 JSON 文件路径 (存储在 file-service，如 /projects/{id}/content.json)',
  `theme_url` varchar(512) DEFAULT NULL COMMENT '主题配置 JSON 文件路径',
  `ref_documents` text DEFAULT NULL COMMENT '参考文档（JSON 数组，包含 name 和 url）',
  `slide_count` int(11) DEFAULT '0' COMMENT '幻灯片页数',
  `is_star` tinyint(1) DEFAULT '0' COMMENT '是否收藏: 0-否, 1-是',
  `status` tinyint(1) DEFAULT '0' COMMENT '状态: 0-草稿, 1-已发布, 2-已归档, -1-已删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户 PPT 项目主表';

-- 2. ppt_template (PPT 模板库表)
CREATE TABLE IF NOT EXISTS `ppt_template` (
  `id` bigint(20) NOT NULL COMMENT '模板 ID (雪花算法)',
  `name` varchar(64) NOT NULL COMMENT '模板名称 (如: 极简极夜黑)',
  `category` varchar(32) NOT NULL COMMENT '类别标识 (business, education 等)',
  `preview_url` varchar(512) DEFAULT NULL COMMENT '模板预览图 URL',
  `file_url` varchar(512) NOT NULL COMMENT '模板标准元素定义 JSON 文件所在的 OSS/静态资源 URL',
  `is_vip` tinyint(1) NOT NULL DEFAULT '0' COMMENT '是否付费/VIP 模板: 0-免费, 1-VIP',
  `sort_order` int(11) NOT NULL DEFAULT '0' COMMENT '排序权重',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PPT 模板库表';

-- 3. ppt_material (用户素材库表)
CREATE TABLE IF NOT EXISTS `ppt_material` (
  `id` bigint(20) NOT NULL COMMENT '素材 ID (雪花算法)',
  `user_id` bigint(20) NOT NULL COMMENT '用户 ID',
  `type` varchar(16) DEFAULT NULL COMMENT '素材类型: image, audio, video',
  `url` varchar(512) DEFAULT NULL COMMENT '资源实际地址 (OSS)',
  `size` int(11) DEFAULT NULL COMMENT '文件大小 (字节)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户素材库表';

-- 插入初始模板数据 (手动给定雪花风格 ID)
-- preview_url 和 file_url 存储相对路径，前端根据环境拼接 base URL
-- 数据与 file-service/upload/templates/ 目录下的实际文件一一对应
INSERT INTO `ppt_template` (`id`, `name`, `category`, `preview_url`, `file_url`, `is_vip`, `sort_order`) VALUES
(1900000000000000001, '山河映红',     'business',  '/templates/preview/template_1.webp', '/templates/template_1.json', 0, 1),
(1900000000000000002, '都市蓝调',     'business',  '/templates/preview/template_2.webp', '/templates/template_2.json', 0, 2),
(1900000000000000003, '智感几何',     'business',  '/templates/preview/template_3.webp', '/templates/template_3.json', 0, 3),
(1900000000000000004, '柔光莫兰迪',   'business',  '/templates/preview/template_4.webp', '/templates/template_4.json', 0, 4),
(1900000000000000005, '简约绿意',     'education', '/templates/preview/template_5.webp', '/templates/template_5.json', 0, 5),
(1900000000000000006, '暖色复古',     'business',  '/templates/preview/template_6.webp', '/templates/template_6.json', 0, 6),
(1900000000000000007, '深邃沉稳',     'business',  '/templates/preview/template_7.webp', '/templates/template_7.json', 0, 7),
(1900000000000000008, '浅蓝小清新',   'education', '/templates/preview/template_8.webp', '/templates/template_8.json', 0, 8),
(1900000000000000009, '自定义测试模板', 'other',     '/templates/preview/template_9.webp', '/templates/template_9.json', 0, 9);

-- 4. ppt_config_option (PPT 创作引擎配置选项表)
CREATE TABLE IF NOT EXISTS `ppt_config_option` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `category` varchar(32) NOT NULL COMMENT '配置分类: language / style / recommend',
  `label` varchar(128) NOT NULL COMMENT '展示文案',
  `sort_order` int(11) DEFAULT '0' COMMENT '排序权重 (越大越靠前)',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否启用: 1-是, 0-否',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PPT 创作引擎配置选项表';

-- 语言选项
INSERT INTO `ppt_config_option` (`category`, `label`, `sort_order`) VALUES
('language', '中文', 100),
('language', 'English', 90),
('language', '日本語', 80);

-- 风格选项
INSERT INTO `ppt_config_option` (`category`, `label`, `sort_order`) VALUES
('style', '通用', 100),
('style', '学术风', 90),
('style', '职场风', 80),
('style', '营销风', 70);

-- 启发式主题推荐
INSERT INTO `ppt_config_option` (`category`, `label`, `sort_order`) VALUES
('recommend', '2025科技前沿动态', 100),
('recommend', '大数据如何改变世界', 90),
('recommend', '餐饮市场调查研究', 80),
('recommend', 'AIGC在教育领域的应用', 70),
('recommend', '社交媒体品牌营销', 60),
('recommend', '年度工作总结展望', 50);

-- 5. undo_log (Seata AT 模式回滚日志表)
CREATE TABLE IF NOT EXISTS `undo_log` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `branch_id` bigint(20) NOT NULL,
  `xid` varchar(100) NOT NULL,
  `context` varchar(128) NOT NULL,
  `rollback_info` longblob NOT NULL,
  `log_status` int(11) NOT NULL,
  `log_created` datetime NOT NULL,
  `log_modified` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `ux_undo_log` (`xid`,`branch_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Seata AT 模式回滚日志表';
