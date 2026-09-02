-- 数据库结构定义（database: crs，MySQL 5.7 语法）
-- 字符集/存储引擎可按部署环境调整

SET NAMES utf8mb4;

-- Create database (safe to re-run)
CREATE DATABASE IF NOT EXISTS `crs` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `crs`;

CREATE TABLE `department` (
  `id` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `user` (
  `code` varchar(255) NOT NULL,
  `name` varchar(255) NOT NULL,
  `password` varchar(255) NOT NULL,
  `dep_id` varchar(255) NOT NULL,
  `role` int(11) NOT NULL,
  `extra` text NULL,
  PRIMARY KEY (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `competition` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `introduce` text NOT NULL,
  `reg_begin_time` datetime NOT NULL,
  `reg_end_time` datetime NOT NULL,
  `submit_begin_time` datetime NOT NULL,
  `submit_end_time` datetime NOT NULL,
  `review_begin_time` datetime NOT NULL,
  `review_end_time` datetime NOT NULL,
  `table` text NULL,
  `type` tinyint(4) NOT NULL,
  `min_team_members` int(11) NULL,
  `max_team_members` int(11) NULL,
  `user_code` varchar(255) NOT NULL,
  `is_review` tinyint(4) NOT NULL,
  `review_settings` longtext NULL,
  `cover` varchar(255) NULL,
  `is_white_list` tinyint(4) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `team` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `com_id` bigint(20) NOT NULL,
  `name` varchar(255) NULL,
  `captain` varchar(255) NOT NULL,
  `member` text NULL,
  `teacher` text NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `work` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `com_id` int(11) NOT NULL,
  `user_code` varchar(255) NOT NULL,
  `work_name` varchar(255) NULL,
  `schema_content` text NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `file` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `com_id` int(11) NULL,
  `user_code` varchar(255) NULL,
  `input` varchar(255) NULL,
  `url` varchar(255) NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `judge` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `com_id` int(11) NULL,
  `judge_code` varchar(255) NULL,
  `user_code` varchar(255) NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `review` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `review_code` varchar(255) NULL,
  `com_id` int(11) NULL,
  `user_code` varchar(255) NULL,
  `accept` tinyint(4) NULL,
  `opinion` text NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `score` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `judge_code` varchar(255) NOT NULL,
  `user_code` varchar(255) NOT NULL,
  `com_id` int(11) NOT NULL,
  `score` int(11) NOT NULL,
  `opinion` text NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `notice` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `com_id` int(11) NOT NULL,
  `title` varchar(255) NOT NULL,
  `content` text NULL,
  `role` int(11) NOT NULL,
  `time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `white_list` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `com_id` bigint(20) NULL,
  `code_list` longtext NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
