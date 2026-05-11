INSERT INTO aimap_user (user_type, account, password) VALUES
('PERSON', 'demo', '123456'),
('COMPANY', 'demo', '123456'),
('ADMIN', 'admin', '123456');

INSERT INTO aimap_job_catalog (record_id, title, org, riasec_json, skills_json) VALUES
('job-001', '前端开发（Vue3）', '星云科技',
 '{"r":28,"i":42,"a":38,"s":40,"e":35,"c":45}',
 '["Vue 3","TypeScript","Vite","ECharts"]'),
('job-002', '全栈开发（Java+Vue）', '银河平台团队',
 '{"r":32,"i":48,"a":30,"s":38,"e":40,"c":42}',
 '["Java","Spring Boot","Vue 3","SQL"]'),
('job-003', '数据工程师', '极光数据',
 '{"r":25,"i":55,"a":22,"s":35,"e":30,"c":58}',
 '["SQL","Spark","Python","数据建模"]'),
('job-004', '产品经理（B端）', '智联产品',
 '{"r":22,"i":40,"a":35,"s":52,"e":48,"c":38}',
 '["需求分析","原型","数据分析","跨部门沟通"]'),
('job-005', 'UX 设计师', '创艺设计室',
 '{"r":20,"i":35,"a":62,"s":48,"e":28,"c":32}',
 '["Figma","用户研究","交互设计","设计系统"]'),
('job-006', '运维工程师（信创）', '麒麟运维中心',
 '{"r":48,"i":45,"a":18,"s":40,"e":32,"c":62}',
 '["Linux","Shell","监控","自动化部署"]');

INSERT INTO aimap_candidate_catalog (record_id, title, org, riasec_json, skills_json) VALUES
('cand-001', '候选人：陈一', '5 年 Java / 微服务',
 '{"r":30,"i":50,"a":28,"s":42,"e":36,"c":48}',
 '["Java","Spring Cloud","MySQL","Redis"]'),
('cand-002', '候选人：李二', '3 年前端 / 可视化',
 '{"r":26,"i":40,"a":42,"s":45,"e":32,"c":40}',
 '["Vue 3","TypeScript","G6","ECharts"]'),
('cand-003', '候选人：王三', '数据方向',
 '{"r":22,"i":58,"a":25,"s":38,"e":28,"c":55}',
 '["Python","SQL","机器学习基础"]'),
('cand-004', '候选人：赵四', '产品 / 运营复合',
 '{"r":20,"i":38,"a":36,"s":55,"e":50,"c":35}',
 '["需求文档","SQL","A/B 测试"]'),
('cand-005', '候选人：周五', '设计 / 品牌',
 '{"r":18,"i":32,"a":65,"s":50,"e":25,"c":30}',
 '["Figma","品牌视觉","动效"]'),
('cand-006', '候选人：吴六', '运维 / 安全',
 '{"r":52,"i":44,"a":20,"s":38,"e":30,"c":58}',
 '["Linux","K8s","安全加固"]');
