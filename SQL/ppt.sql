-- PPT 服务数据库脚本

CREATE DATABASE IF NOT EXISTS `ppt_service` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `ppt_service`;

-- 1. ppt_project (用户 PPT 项目主表)
CREATE TABLE IF NOT EXISTS `ppt_project` (
  `id` bigint(20) NOT NULL COMMENT '项目唯一标识',
  `user_id` bigint(20) NOT NULL COMMENT '归属用户 ID',
  `title` varchar(128) DEFAULT NULL COMMENT 'PPT 标题',
  `cover_url` varchar(512) DEFAULT NULL COMMENT '封面图地址',
  `content_json` longtext COMMENT '幻灯片完整 JSON 数据',
  `status` tinyint(1) DEFAULT '0' COMMENT '状态: 0-草稿, 1-已发布, 2-已归档, -1-已删除',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户 PPT 项目主表';

-- 2. ppt_template (PPT 模板库表)
CREATE TABLE IF NOT EXISTS `ppt_template` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '模板 ID',
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
  `id` bigint(20) NOT NULL COMMENT '素材 ID',
  `user_id` bigint(20) NOT NULL COMMENT '用户 ID',
  `type` varchar(16) DEFAULT NULL COMMENT '素材类型: image, audio, video',
  `url` varchar(512) DEFAULT NULL COMMENT '资源实际地址 (OSS)',
  `size` int(11) DEFAULT NULL COMMENT '文件大小 (字节)',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户素材库表';

-- 插入一些初始模板数据
INSERT INTO `ppt_template` (`name`, `category`, `preview_url`, `file_url`, `is_vip`, `sort_order`) VALUES
('极简极夜黑', 'business', 'http://localhost:8084/templates/preview/minimal-dark.png', 'http://localhost:8084/templates/json/minimal-dark.json', 0, 1),
('商务天空蓝', 'business', 'http://localhost:8084/templates/preview/business-blue.png', 'http://localhost:8084/templates/json/business-blue.json', 0, 2),
('学术清新绿', 'education', 'http://localhost:8084/templates/preview/academic-green.png', 'http://localhost:8084/templates/json/academic-green.json', 0, 3);
