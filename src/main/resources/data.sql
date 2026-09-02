SET NAMES utf8mb4;

-- Departments
INSERT INTO `department` (`id`, `name`) VALUES
  ('1', '通信与信息工程学院'),
  ('2', '电子与光学工程学院、柔性电子(未来技术)学院'),
  ('3', '集成电路科学与工程学院（产教融合学院）'),
  ('4', '计算机学院、软件学院、网络空间安全学院'),
  ('5', '自动化学院'),
  ('6', '人工智能学院'),
  ('7', '材料科学与工程学院'),
  ('8', '化学与生命科学学院'),
  ('9', '物联网学院'),
  ('10', '理学院'),
  ('11', '现代邮政学院，智慧交通学院'),
  ('12', '数字媒体与设计艺术学院'),
  ('13', '管理学院'),
  ('14', '经济学院'),
  ('15', '马克思主义学院'),
  ('16', '社会与人口学院、社会工作学院'),
  ('17', '外国语学院'),
  ('18', '教育科学与技术学院'),
  ('19', '贝尔英才学院'),
  ('20', '波特兰学院')
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`);

-- 演示种子账号（仅用于本地开发），密码统一为 sastSu（MD5 存储）
INSERT INTO `user` (`code`, `name`, `password`, `dep_id`, `role`, `extra`) VALUES
  ('SAST', '超级管理员账号', '66464ea92069cff7dd09dfeb6d45d5ca', '1', 3, NULL),
  ('SAST20', '审批', '66464ea92069cff7dd09dfeb6d45d5ca', '1', 1, NULL),
  ('SAST30', '评委', '66464ea92069cff7dd09dfeb6d45d5ca', '1', 2, NULL),
  ('SAST31', '评委2', '66464ea92069cff7dd09dfeb6d45d5ca', '1', 2, NULL),
  ('SAST40', '学生', '66464ea92069cff7dd09dfeb6d45d5ca', '1', 0, NULL)
ON DUPLICATE KEY UPDATE
  `name` = VALUES(`name`),
  `password` = VALUES(`password`),
  `dep_id` = VALUES(`dep_id`),
  `role` = VALUES(`role`),
  `extra` = VALUES(`extra`);


