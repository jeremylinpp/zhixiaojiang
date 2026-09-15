INSERT IGNORE INTO sys_user (id, username, password_hash, display_name, role) VALUES (1, 'teacher', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '林老师', 'TEACHER');
INSERT IGNORE INTO class_room (id, name, grade, teacher_id, is_demo) VALUES (1, '机智班', '2025级', 1, TRUE);
INSERT IGNORE INTO student (id, class_id, student_no, name, gender) VALUES (1,1,'JZ001','小智','男'),(2,1,'JZ002','陈同学','女'),(3,1,'JZ003','周同学','男'),(4,1,'JZ004','林同学','女');
INSERT IGNORE INTO student (id, class_id, student_no, name, gender) VALUES
(5,1,'JZ005','演示学生05','男'),(6,1,'JZ006','演示学生06','女'),(7,1,'JZ007','演示学生07','男'),(8,1,'JZ008','演示学生08','女'),
(9,1,'JZ009','演示学生09','男'),(10,1,'JZ010','演示学生10','女'),(11,1,'JZ011','演示学生11','男'),(12,1,'JZ012','演示学生12','女'),
(13,1,'JZ013','演示学生13','男'),(14,1,'JZ014','演示学生14','女'),(15,1,'JZ015','演示学生15','男'),(16,1,'JZ016','演示学生16','女'),
(17,1,'JZ017','演示学生17','男'),(18,1,'JZ018','演示学生18','女'),(19,1,'JZ019','演示学生19','男'),(20,1,'JZ020','演示学生20','女'),
(21,1,'JZ021','演示学生21','男'),(22,1,'JZ022','演示学生22','女'),(23,1,'JZ023','演示学生23','男'),(24,1,'JZ024','演示学生24','女'),
(25,1,'JZ025','演示学生25','男'),(26,1,'JZ026','演示学生26','女'),(27,1,'JZ027','演示学生27','男'),(28,1,'JZ028','演示学生28','女'),
(29,1,'JZ029','演示学生29','男'),(30,1,'JZ030','演示学生30','女'),(31,1,'JZ031','演示学生31','男'),(32,1,'JZ032','演示学生32','女'),
(33,1,'JZ033','演示学生33','男'),(34,1,'JZ034','演示学生34','女'),(35,1,'JZ035','演示学生35','男'),(36,1,'JZ036','演示学生36','女'),
(37,1,'JZ037','演示学生37','男'),(38,1,'JZ038','演示学生38','女'),(39,1,'JZ039','演示学生39','男'),(40,1,'JZ040','演示学生40','女'),
(41,1,'JZ041','演示学生41','男'),(42,1,'JZ042','演示学生42','女');
INSERT IGNORE INTO attendance_record (student_id,attendance_date,status,created_by) VALUES
(1,'2026-09-14','PRESENT',1),(2,'2026-09-14','PRESENT',1),(3,'2026-09-14','PRESENT',1),(4,'2026-09-14','PRESENT',1),
(5,'2026-09-14','PRESENT',1),(6,'2026-09-14','PRESENT',1),(7,'2026-09-14','PRESENT',1),(8,'2026-09-14','PRESENT',1),
(9,'2026-09-14','PRESENT',1),(10,'2026-09-14','PRESENT',1),(11,'2026-09-14','PRESENT',1),(12,'2026-09-14','PRESENT',1),
(13,'2026-09-14','PRESENT',1),(14,'2026-09-14','PRESENT',1),(15,'2026-09-14','PRESENT',1),(16,'2026-09-14','PRESENT',1),
(17,'2026-09-14','PRESENT',1),(18,'2026-09-14','PRESENT',1),(19,'2026-09-14','PRESENT',1),(20,'2026-09-14','PRESENT',1),
(21,'2026-09-14','PRESENT',1),(22,'2026-09-14','PRESENT',1),(23,'2026-09-14','PRESENT',1),(24,'2026-09-14','PRESENT',1),
(25,'2026-09-14','PRESENT',1),(26,'2026-09-14','PRESENT',1),(27,'2026-09-14','PRESENT',1),(28,'2026-09-14','PRESENT',1),
(29,'2026-09-14','PRESENT',1),(30,'2026-09-14','PRESENT',1),(31,'2026-09-14','PRESENT',1),(32,'2026-09-14','PRESENT',1),
(33,'2026-09-14','PRESENT',1),(34,'2026-09-14','PRESENT',1),(35,'2026-09-14','PRESENT',1),(36,'2026-09-14','PRESENT',1),
(37,'2026-09-14','PRESENT',1),(38,'2026-09-14','PRESENT',1),(39,'2026-09-14','PRESENT',1),(40,'2026-09-14','PRESENT',1),
(41,'2026-09-14','PRESENT',1),(42,'2026-09-14','ABSENT',1);
INSERT IGNORE INTO point_rule (id,name,category,amount,enabled,description,created_by) VALUES
(1,'完成成长任务','TASK',2,TRUE,'教师确认完成六机任务后发放',1),(2,'班级正向贡献','CONTRIBUTION',2,TRUE,'记录可追溯的班级贡献',1),(3,'迟到','BEHAVIOR',-1,TRUE,'按班级规则记录扣分',1);
INSERT IGNORE INTO activity_record (student_id,activity_date,activity_type,status,detail,created_by) VALUES
(1,'2026-08-01','班级活动','PARTICIPATED','参加暑期技能分享',1),(1,'2026-08-05','社团活动','PARTICIPATED','完成机器人社团展示',1),
(1,'2026-08-12','志愿服务','PARTICIPATED','参与校园志愿服务',1),(1,'2026-09-03','班级活动','PARTICIPATED','参加班会',1);
INSERT IGNORE INTO class_target (id, class_id, name, target_value, current_value, unit, created_by) VALUES (1,1,'数学及格率',85,76,'% ',1),(2,1,'职业规划清晰度',80,78,'% ',1),(3,1,'任务完成率',90,89,'% ',1);
INSERT IGNORE INTO growth_record (id,student_id,dimension,score,title,detail,occurred_on,source,created_by) VALUES (1,1,'SKILL',91,'机器人基础操作实训','完成机械臂示教与轨迹调试','2026-05-12','实训评价',1),(2,1,'THINKING',72,'数学月考','连续下降趋势待关注','2026-06-12','月考',1),(3,1,'MORAL',88,'志愿服务','机器人知识小讲堂','2026-06-18','活动记录',1),(4,1,'SMART',78,'AI工具应用','完成数字作品初稿','2026-06-24','成长任务',1);
INSERT IGNORE INTO score_record (id,student_id,subject,exam_name,score,full_score,occurred_on,created_by) VALUES (1,1,'数学','阶段测验1',78,100,'2026-04-01',1),(2,1,'数学','阶段测验2',70,100,'2026-05-01',1),(3,1,'数学','阶段测验3',63,100,'2026-06-01',1),(4,1,'数学','阶段测验4',58,100,'2026-07-01',1);
INSERT IGNORE INTO warning_record (id,student_id,level,rule_code,summary,evidence_json,status) VALUES (1,1,'FOCUS','SCORE_DECLINE','近期学业、活动参与和行为表现呈同步下降趋势，建议班主任进一步了解近期学习及生活状态。','["数学成绩 78 → 70 → 63 → 58","活动参与 4 → 3 → 2 → 1","迟到次数 0 → 1 → 2 → 3"]','OPEN');
INSERT IGNORE INTO intervention_plan (id,student_id,warning_id,title,status,suggestions_json,teacher_note,created_by) VALUES (1,1,1,'小智阶段帮扶方案','CONFIRMED','["班主任个别谈话","数学教师一对一指导","学习小组伙伴结对","两周后成长复评"]','已完成教师人工确认，持续跟进。',1);
INSERT IGNORE INTO intervention_record (id,plan_id,action,status,occurred_on,result,created_by) VALUES (1,1,'班主任个别谈话','DONE','2026-09-01','了解近期学习状态',1),(2,1,'数学教师一对一指导','DONE','2026-09-03','已完成',1),(3,1,'学习小组伙伴结对','DONE','2026-09-05','已完成',1);
INSERT IGNORE INTO growth_task (id,module,title,description,due_on,point_reward,status,created_by) VALUES (1,'铸机魂','名匠故事研读','完成一篇工匠精神读后反思','2026-09-20',2,'PUBLISHED',1),(2,'立机规','实训安全规范','完成机器人专项安全测验','2026-09-18',2,'PUBLISHED',1),(3,'淬机质','AI创意小赛','提交一份智能制造创意作品','2026-09-30',5,'PUBLISHED',1),(4,'铺机路','个人职业规划','完成阶段职业路径卡片','2026-09-25',3,'PUBLISHED',1);
UPDATE sys_user SET password_hash='$2a$10$n4DBvIrKTMNgcMfJ/bTzqOUTdU51cNN7GneXJ7t8OV3xgPxT13lhW' WHERE username='teacher';
