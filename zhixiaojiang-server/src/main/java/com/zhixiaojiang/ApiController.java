package com.zhixiaojiang;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.Duration;
import java.math.BigDecimal;
import java.util.*;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.web.csrf.CsrfToken;

@RestController
@RequestMapping("/api/v1")
public class ApiController {
    private final JdbcTemplate db;
    private final PasswordEncoder encoder;
    private final SessionRevocationService revocations;
    private final ObjectMapper json = new ObjectMapper();
    private final String secret;
    private final boolean secureCookie;
    private final String aiBaseUrl;
    private final String aiModel;
    private final String aiApiKey;
    private final org.springframework.web.client.RestClient aiClient;

    public ApiController(JdbcTemplate db, PasswordEncoder encoder, SessionRevocationService revocations, org.springframework.core.env.Environment env) {
        this.db = db;
        this.encoder = encoder;
        this.revocations = revocations;
        secret = env.getProperty("app.jwt-secret", "change-me-in-local-env-please-32-chars");
        secureCookie = env.getProperty("app.cookie-secure", Boolean.class, false);
        aiBaseUrl = env.getProperty("AI_BASE_URL", "");
        aiModel = env.getProperty("AI_MODEL", "");
        aiApiKey = env.getProperty("AI_API_KEY", "");
        aiClient = org.springframework.web.client.RestClient.builder().build();
    }

    @GetMapping("/auth/csrf")
    Map<String, Object> csrf(CsrfToken token) {
        return ok(Map.of("token", token.getToken()));
    }

    @PostMapping("/auth/login")
    Map<String, Object> login(@Valid @RequestBody Login body, HttpServletResponse response) {
        var rows = db.queryForList("select id,username,password_hash,display_name,role from sys_user where username=?", body.username());
        if (rows.isEmpty() || !encoder.matches(body.password(), (String) rows.get(0).get("password_hash")))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        var u = rows.get(0);
        Cookie c = new Cookie("ZJ_SESSION", SecurityConfig.token(((Number) u.get("id")).longValue(), body.username(), (String) u.get("role"), secret));
        c.setHttpOnly(true);
        c.setSecure(secureCookie);
        c.setPath("/");
        c.setMaxAge(28800);
        response.addCookie(c);
        return ok(Map.of("id", u.get("id"), "displayName", u.get("display_name"), "role", u.get("role")));
    }

    @PostMapping("/auth/logout")
    Map<String, Object> logout(HttpServletRequest req, HttpServletResponse r) {
        if (req.getCookies() != null) for (Cookie existing : req.getCookies())
            if ("ZJ_SESSION".equals(existing.getName()))
                revocations.revoke(existing.getValue(), java.time.Duration.ofHours(8));
        Cookie c = new Cookie("ZJ_SESSION", "");
        c.setMaxAge(0);
        c.setPath("/");
        r.addCookie(c);
        return ok(Map.of());
    }

    @GetMapping("/auth/me")
    Map<String, Object> me(HttpServletRequest req) {
        String id = String.valueOf(req.getAttribute("userId"));
        if ("null".equals(id))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED);
        return ok(db.queryForMap("select id,username,display_name as displayName,role from sys_user where id=?", Long.valueOf(id)));
    }

    @GetMapping("/dashboard/class")
    Map<String, Object> dashboard() {
        int studentCount = Optional.ofNullable(db.queryForObject("select count(*) from student where class_id=1 and status='ACTIVE'", Integer.class)).orElse(0);
        LocalDate attendanceDate = db.queryForObject("select max(ar.attendance_date) from attendance_record ar join student s on s.id=ar.student_id where s.class_id=1 and s.status='ACTIVE'", LocalDate.class);
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("studentCount", studentCount);
        metrics.put("attendanceDate", attendanceDate);
        if (attendanceDate == null) {
            metrics.put("activeRate", null);
            metrics.put("attendanceCompleteness", 0);
        } else {
            Map<String, Object> a = db.queryForMap("select count(*) registeredCount, round(100.0*sum(case when ar.status in ('PRESENT','LATE') then 1 else 0 end)/nullif(count(*),0),1) attendanceRate from attendance_record ar join student s on s.id=ar.student_id where s.class_id=1 and s.status='ACTIVE' and ar.attendance_date=?", attendanceDate);
            metrics.put("activeRate", a.get("attendanceRate"));
            metrics.put("attendanceCompleteness", a.get("registeredCount"));
        }
        var targets = db.queryForList("select name,target_value as targetValue,current_value as currentValue,unit,status from class_target where class_id=1");
        var warnings = db.queryForList("select w.id,w.level,w.summary,s.name studentName from warning_record w join student s on s.id=w.student_id where w.status='OPEN' order by w.created_at desc");
        return ok(Map.of("class", Map.of("name", "机智班", "grade", "2025级"), "metrics", metrics, "targets", targets, "warnings", warnings, "demo", true));
    }

    @GetMapping("/students")
    Map<String, Object> students(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) {
        int safePage = Math.max(1, page), safeSize = Math.min(100, Math.max(1, pageSize)), offset = (safePage - 1) * safeSize;
        String like = "%" + q + "%";
        var items = db.queryForList("select s.id,s.student_no as studentNo,s.name,s.gender,s.status,coalesce(round(avg(g.score),1),0) growthIndex from student s left join growth_record g on g.student_id=s.id where s.class_id=1 and s.status='ACTIVE' and (s.name like ? or s.student_no like ?) group by s.id order by s.id limit ? offset ?", like, like, safeSize, offset);
        Integer total = db.queryForObject("select count(*) from student s where s.class_id=1 and s.status='ACTIVE' and (s.name like ? or s.student_no like ?)", Integer.class, like, like);
        return ok(Map.of("items", items, "page", safePage, "pageSize", safeSize, "total", total == null ? 0 : total));
    }

    @GetMapping("/students/{id}")
    Map<String, Object> student(@PathVariable long id) {
        var s = db.queryForMap("select id,student_no as studentNo,name,gender,status from student where id=? and class_id=1", id);
        return ok(Map.of("student", s, "growth", db.queryForList("select dimension,round(avg(score),1) score from growth_record where student_id=? group by dimension", id), "scores", db.queryForList("select subject,exam_name examName,score,full_score fullScore,occurred_on occurredOn from score_record where student_id=? order by occurred_on", id), "timeline", db.queryForList("select title,detail,occurred_on occurredOn,source from growth_record where student_id=? order by occurred_on desc", id)));
    }

    @GetMapping("/students/{id}/portrait")
    Map<String, Object> portrait(@PathVariable long id) {
        ensureStudent(id);
        return ok(Map.of("studentId", id, "dimensions", db.queryForList("select dimension,round(avg(score),1) score from growth_record where student_id=? group by dimension", id)));
    }

    @GetMapping("/students/{id}/timeline")
    Map<String, Object> timeline(@PathVariable long id) {
        ensureStudent(id);
        return ok(Map.of("items", db.queryForList("select title,detail,occurred_on occurredOn,source,created_by createdBy from growth_record where student_id=? order by occurred_on desc", id)));
    }

    @GetMapping("/students/{id}/attendance")
    Map<String, Object> attendance(@PathVariable long id) {
        ensureStudent(id);
        return ok(Map.of("items", db.queryForList("select id,attendance_date attendanceDate,status,note,created_by createdBy from attendance_record where student_id=? order by attendance_date desc", id)));
    }


    @GetMapping("/students/{id}/behavior")
    Map<String, Object> behavior(@PathVariable long id) {
        ensureStudent(id);
        return ok(Map.of("items", db.queryForList("select id,category,score,occurred_on occurredOn,detail,created_by createdBy from behavior_record where student_id=? order by occurred_on desc", id)));
    }

    @PostMapping("/students/{id}/behavior")
    Map<String, Object> recordBehavior(@PathVariable long id, @RequestBody Map<String, Object> b, HttpServletRequest req) {
        ensureStudent(id);
        db.update("insert into behavior_record(student_id,category,score,occurred_on,detail,created_by) values(?,?,?,?,?,?)", id, reqString(b, "category", "日常表现"), decimalOrNull(b.get("score")), parseDate(b.get("occurredOn")), b.get("detail"), userId(req));
        Long rid = db.queryForObject("select max(id) from behavior_record where student_id=?", Long.class, id);
        audit(req, "CREATE", "behavior_record", rid, "记录学生行为表现");
        return ok(Map.of("id", rid));
    }

    @GetMapping("/students/{id}/skills")
    Map<String, Object> skills(@PathVariable long id) {
        ensureStudent(id);
        return ok(Map.of("items", db.queryForList("select id,skill_name skillName,score,level,occurred_on occurredOn,evidence,created_by createdBy from skill_record where student_id=? order by occurred_on desc", id)));
    }


    @GetMapping("/students/{id}/evaluations")
    Map<String, Object> evaluations(@PathVariable long id) {
        ensureStudent(id);
        return ok(Map.of("items", db.queryForList("select id,period_start periodStart,period_end periodEnd,moral_score moralScore,skill_score skillScore,thinking_score thinkingScore,smart_score smartScore,evidence,created_by createdBy from dimension_evaluation where student_id=? order by period_end desc", id)));
    }



    @PostMapping("/warnings/analyze")
    @Transactional
    Map<String, Object> analyze(HttpServletRequest req) {
        int created = 0;
        LocalDate today = LocalDate.now(), since = today.minusDays(13), priorSince = today.minusDays(27);
        for (Map<String, Object> s : db.queryForList("select s.id from student s join class_room c on c.id=s.class_id where c.teacher_id=? and s.status='ACTIVE'", userId(req))) {
            long sid = ((Number) s.get("id")).longValue();
            List<Map<String, Object>> scores = db.queryForList("select score/full_score ratio,score from score_record where student_id=? and subject='数学' order by occurred_on desc limit 4", sid);
            boolean decline = scores.size() >= 4 && ((Number) scores.get(3).get("ratio")).doubleValue() > ((Number) scores.get(2).get("ratio")).doubleValue() && ((Number) scores.get(2).get("ratio")).doubleValue() > ((Number) scores.get(1).get("ratio")).doubleValue() && ((Number) scores.get(1).get("ratio")).doubleValue() > ((Number) scores.get(0).get("ratio")).doubleValue();
            boolean low = scores.size() >= 2 && scores.stream().limit(2).allMatch(x -> ((Number) x.get("ratio")).doubleValue() < 0.6);
            if (decline || low) {
                int n = db.update("insert ignore into warning_record(student_id,level,rule_code,summary,evidence_json,status) values(?,?,?,?,?, 'OPEN')", sid, decline ? "FOCUS" : "ATTENTION", decline ? "SCORE_DECLINE" : "SCORE_LOW", decline ? "同科目最近四次考试连续下降" : "最近两次考试低于及格线", jsonValue(List.of("数学成绩趋势由规则引擎计算")));
                created += n;
            }
            Integer late = db.queryForObject("select count(*) from attendance_record where student_id=? and status='LATE' and attendance_date>=?", Integer.class, sid, since);
            if (late != null && late >= 3) {
                created += db.update("insert ignore into warning_record(student_id,level,rule_code,summary,evidence_json,status) values(?,?,?,?,?, 'OPEN')", sid, "ATTENTION", "LATE_14D", "最近14天迟到至少3次", jsonValue(List.of("迟到次数=" + late)));
            }
            Integer overdue = db.queryForObject("select count(*) from student_task st join growth_task gt on gt.id=st.task_id where st.student_id=? and gt.due_on<? and st.status<>'COMPLETED'", Integer.class, sid, today);
            if (overdue != null && overdue >= 2) {
                created += db.update("insert ignore into warning_record(student_id,level,rule_code,summary,evidence_json,status) values(?,?,?,?,?, 'OPEN')", sid, "ATTENTION", "TASK_OVERDUE", "至少2项到期任务未完成", jsonValue(List.of("未完成到期任务=" + overdue)));
            }
            Integer recentActivity = db.queryForObject("select count(*) from activity_record where student_id=? and activity_date>=?", Integer.class, sid, since);
            Integer priorActivity = db.queryForObject("select count(*) from activity_record where student_id=? and activity_date>=? and activity_date<?", Integer.class, sid, priorSince, since);
            if (priorActivity != null && priorActivity >= 2 && recentActivity != null && recentActivity * 2 <= priorActivity) {
                created += db.update("insert ignore into warning_record(student_id,level,rule_code,summary,evidence_json,status) values(?,?,?,?,?, 'OPEN')", sid, "ATTENTION", "ACTIVITY_DROP", "最近14天活动参与较前14天下降至少一半", jsonValue(List.of("前期=" + priorActivity, "近期=" + recentActivity)));
            }
        }
        audit(req, "ANALYZE", "warning_record", 0, "执行规则预警分析");
        return ok(Map.of("source", "TEMPLATE", "message", "已按规则完成趋势筛查", "created", created, "disclaimer", "AI辅助建议，仅供教师参考"));
    }



    @GetMapping("/interventions")
    Map<String, Object> interventions() {
        return ok(Map.of("items", db.queryForList("select i.id,i.title,i.status,i.teacher_note teacherNote,s.name studentName,i.review_at reviewAt from intervention_plan i join student s on s.id=i.student_id order by i.updated_at desc")));
    }

    @PostMapping("/interventions")
    Map<String, Object> createIntervention(@RequestBody Map<String, Object> b, HttpServletRequest req) {
        long sid = Long.parseLong(String.valueOf(b.get("studentId")));
        ensureStudent(sid);
        String title = reqString(b, "title", "阶段成长支持方案");
        String suggestions = jsonValue(b.getOrDefault("suggestions", List.of("班主任个别谈话", "两周后复评")));
        db.update("insert into intervention_plan(student_id,title,status,suggestions_json,teacher_note,review_at,created_by) values(?,?, 'DRAFT',?,?,?,?)", sid, title, suggestions, b.get("teacherNote"), parseDate(b.get("reviewAt")), userId(req));
        Long id = db.queryForObject("select max(id) from intervention_plan where student_id=?", Long.class, sid);
        audit(req, "CREATE", "intervention_plan", id, "创建帮扶草案");
        return ok(Map.of("id", id, "status", "DRAFT"));
    }

    @PutMapping("/interventions/{id}")
    Map<String, Object> updateIntervention(@PathVariable long id, @RequestBody Map<String, Object> b, HttpServletRequest req) {
        db.update("update intervention_plan set title=coalesce(?,title),teacher_note=coalesce(?,teacher_note),review_at=coalesce(?,review_at) where id=?", b.get("title"), b.get("teacherNote"), parseDate(b.get("reviewAt")), id);
        audit(req, "UPDATE", "intervention_plan", id, "更新帮扶草案");
        return ok(Map.of("saved", true));
    }

    @PostMapping("/interventions/{id}/transition")
    Map<String, Object> transitionIntervention(@PathVariable long id, @RequestBody Map<String, Object> b, HttpServletRequest req) {
        String next = reqString(b, "status", "");
        String current = db.queryForObject("select status from intervention_plan where id=?", String.class, id);
        Set<String> allowed = switch (current) {
            case "DRAFT" -> Set.of("CONFIRMED", "CLOSED");
            case "CONFIRMED" -> Set.of("IN_PROGRESS", "CLOSED");
            case "IN_PROGRESS" -> Set.of("COMPLETED", "CLOSED");
            default -> Set.of();
        };
        if (!allowed.contains(next))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "非法帮扶状态转换");
        db.update("update intervention_plan set status=? where id=?", next, id);
        audit(req, "TRANSITION", "intervention_plan", id, "帮扶状态变更为" + next);
        return ok(Map.of("saved", true, "status", next));
    }

    @PostMapping("/interventions/{id}/review")
    Map<String, Object> reviewIntervention(@PathVariable long id, @RequestBody Map<String, Object> b, HttpServletRequest req) {
        db.update("update intervention_plan set review_at=?,teacher_note=coalesce(?,teacher_note) where id=?", parseDate(b.get("reviewAt")), b.get("teacherNote"), id);
        audit(req, "REVIEW", "intervention_plan", id, "记录阶段复评");
        return ok(Map.of("saved", true));
    }

    @PostMapping("/interventions/{id}/records")
    Map<String, Object> interventionRecord(@PathVariable long id, @RequestBody Map<String, Object> b, HttpServletRequest req) {
        db.update("insert into intervention_record(plan_id,action,status,occurred_on,result,created_by) values(?,?,?,?,?,?)", id, b.get("action"), "DONE", LocalDate.now(), b.get("result"), userId(req));
        db.update("update intervention_plan set status='IN_PROGRESS' where id=? and status='CONFIRMED'", id);
        audit(req, "CREATE", "intervention_record", id, "记录帮扶过程");
        return ok(Map.of("saved", true));
    }

    @PostMapping("/ai/student-analysis")
    Map<String, Object> ai(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        Map<String, Object> result = templateAi();
        if (!aiBaseUrl.isBlank() && !aiModel.isBlank() && !aiApiKey.isBlank()) {
            try {
                Map<String, Object> safe = new LinkedHashMap<>();
                for (String key : List.of("scores", "attendance", "behavior", "skills", "tasks"))
                    if (body.get(key) != null) safe.put(key, body.get(key));
                String prompt = "请根据以下匿名成长数据输出 JSON，字段必须包含 summary、evidence、suggestions：" + jsonValue(safe);
                Map<String, Object> requestBody = Map.of("model", aiModel, "temperature", 0.2, "messages", List.of(Map.of("role", "system", "content", "你是班主任成长分析助手，只做趋势归纳和教育建议，不做心理或医学诊断。"), Map.of("role", "user", "content", prompt)));
                String raw = aiClient.post().uri(aiBaseUrl.endsWith("/chat/completions") ? aiBaseUrl : aiBaseUrl + "/chat/completions").header("Authorization", "Bearer " + aiApiKey).contentType(org.springframework.http.MediaType.APPLICATION_JSON).body(requestBody).retrieve().body(String.class);
                String content = json.readTree(raw).path("choices").path(0).path("message").path("content").asText("");
                Map<String, Object> parsed = json.readValue(content, new TypeReference<Map<String, Object>>() {
                });
                if (parsed.get("summary") != null && parsed.get("evidence") instanceof Collection<?> && parsed.get("suggestions") instanceof Collection<?>) {
                    result = new LinkedHashMap<>(parsed);
                    result.put("source", "MODEL");
                    result.put("disclaimer", "AI辅助建议，仅供教师参考");
                    result.put("requiresTeacherConfirmation", true);
                }
            } catch (Exception ignored) {/* fall back to the explicit template result */}
        }
        try {
            long studentId = body.get("studentId") instanceof Number n ? n.longValue() : 0;
            db.update("insert into ai_analysis(student_id,source,request_json,response_json,created_by) values(?,?,?,?,?)", studentId, String.valueOf(result.get("source")), jsonValue(Map.of("whitelist", "scores,attendance,behavior,skills,tasks")), jsonValue(result), userId(req));
        } catch (Exception ignored) {
        }
        return ok(result);
    }

    private Map<String, Object> templateAi() {
        return new LinkedHashMap<>(Map.of("source", "TEMPLATE", "summary", "该生近期学业表现、活动参与和行为表现呈同步下降趋势，建议进一步了解其学习及生活状态。", "evidence", List.of("数学成绩连续下降", "活动参与度下降", "迟到次数增加"), "suggestions", List.of("班主任个别谈话", "数学教师一对一指导", "学习小组伙伴结对", "两周后成长复评"), "disclaimer", "AI辅助建议，仅供教师参考", "requiresTeacherConfirmation", true));
    }

    @GetMapping("/growth-tasks")
    Map<String, Object> tasks() {
        return ok(Map.of("items", db.queryForList("select id,module,title,description,due_on dueOn,point_reward pointReward,status from growth_task order by due_on")));
    }

    @PostMapping("/growth-tasks")
    Map<String, Object> createTask(@RequestBody Map<String, Object> b, HttpServletRequest req) {
        db.update("insert into growth_task(module,title,description,due_on,point_reward,status,created_by) values(?,?,?,?,?,'PUBLISHED',?)", reqString(b, "module", "聚机力"), reqString(b, "title", "成长任务"), b.get("description"), parseDate(b.get("dueOn")), intValue(b.get("pointReward")), userId(req));
        Long id = db.queryForObject("select max(id) from growth_task", Long.class);
        audit(req, "CREATE", "growth_task", id, "发布六机成长任务");
        return ok(Map.of("id", id));
    }

    @PostMapping("/growth-tasks/{id}/assign")
    Map<String, Object> assignTask(@PathVariable long id, @RequestBody Map<String, Object> b, HttpServletRequest req) {
        Object raw = b.get("studentIds");
        if (raw instanceof Collection<?> ids) for (Object sid : ids) {
            ensureStudent(Long.parseLong(String.valueOf(sid)));
            db.update("insert ignore into student_task(task_id,student_id,status) values(?,?,'ASSIGNED')", id, Long.parseLong(String.valueOf(sid)));
        }
        audit(req, "ASSIGN", "growth_task", id, "分配成长任务");
        return ok(Map.of("saved", true));
    }

    @GetMapping("/growth-tasks/{id}/students")
    Map<String, Object> taskStudents(@PathVariable long id) {
        return ok(Map.of("items", db.queryForList("select st.id,st.student_id studentId,s.name studentName,st.status,st.completed_on completedOn,st.teacher_note teacherNote from student_task st join student s on s.id=st.student_id where st.task_id=?", id)));
    }

    @PostMapping("/student-tasks/{id}/complete")
    @Transactional
    Map<String, Object> completeTask(@PathVariable long id, @RequestBody(required = false) Map<String, Object> b, HttpServletRequest req) {
        int changed = db.update("update student_task set status='COMPLETED',completed_on=coalesce(completed_on,curdate()),teacher_note=? where id=? and status<>'COMPLETED'", b == null ? null : b.get("note"), id);
        if (changed == 1) {
            Map<String, Object> t = db.queryForMap("select st.student_id,g.point_reward,g.title from student_task st join growth_task g on g.id=st.task_id where st.id=?", id);
            String key = "task:" + id;
            db.update("insert ignore into point_ledger(student_id,amount,category,reason,idempotency_key,created_by) values(?,?, 'TASK',?,?,?)", t.get("student_id"), t.get("point_reward"), "完成任务：" + t.get("title"), key, userId(req));
            audit(req, "COMPLETE", "student_task", id, "确认任务完成并发放机智币");
        }
        return ok(Map.of("saved", true, "awarded", changed == 1));
    }

    @PostMapping("/student-tasks/{id}/evaluate")
    Map<String, Object> evaluateTask(@PathVariable long id, @RequestBody Map<String, Object> b, HttpServletRequest req) {
        db.update("update student_task set teacher_note=? where id=?", b.get("note"), id);
        audit(req, "EVALUATE", "student_task", id, "记录任务评价");
        return ok(Map.of("saved", true));
    }

    @GetMapping("/class-diagnoses")
    Map<String, Object> diagnoses() {
        return ok(Map.of("items", db.queryForList("select id,name,target_value targetValue,current_value currentValue,unit,status,round(target_value-current_value,1) deviation from class_target where class_id=1"), "cycle", List.of("目标", "标准", "计划", "实施", "监测", "诊断", "改进", "优化")));
    }

    @GetMapping("/class-diagnoses/{id}/records")
    Map<String, Object> diagnosisRecords(@PathVariable long id) {
        return ok(Map.of("items", db.queryForList("select id,target_id targetId,measure,review_result reviewResult,recorded_on recordedOn,created_by createdBy from class_diagnosis_record where target_id=? order by recorded_on desc", id)));
    }

    @PostMapping("/class-diagnoses/{id}/records")
    Map<String, Object> createDiagnosisRecord(@PathVariable long id, @RequestBody Map<String, Object> b, HttpServletRequest req) {
        db.queryForObject("select id from class_target where id=? and class_id=1", Long.class, id);
        db.update("insert into class_diagnosis_record(target_id,measure,review_result,recorded_on,created_by) values(?,?,?,?,?)", id, reqString(b, "measure", "记录改进措施"), b.get("reviewResult"), parseDate(b.get("recordedOn")), userId(req));
        Long rid = db.queryForObject("select max(id) from class_diagnosis_record where target_id=?", Long.class, id);
        audit(req, "CREATE", "class_diagnosis_record", rid, "记录班级诊改措施");
        return ok(Map.of("id", rid));
    }

    @PostMapping("/class-diagnoses")
    Map<String, Object> createDiagnosis(@RequestBody Map<String, Object> b, HttpServletRequest req) {
        db.update("insert into class_target(class_id,name,target_value,current_value,unit,status,created_by) values(1,?,?,?,?,'IN_PROGRESS',?)", reqString(b, "name", "新诊改目标"), decimalValue(b.get("targetValue")), decimalValue(b.getOrDefault("currentValue", 0)), b.getOrDefault("unit", "%"), userId(req));
        Long id = db.queryForObject("select max(id) from class_target", Long.class);
        audit(req, "CREATE", "class_target", id, "创建班级诊改目标");
        return ok(Map.of("id", id));
    }

    @PutMapping("/class-diagnoses/{id}")
    Map<String, Object> updateDiagnosis(@PathVariable long id, @RequestBody Map<String, Object> b, HttpServletRequest req) {
        db.update("update class_target set target_value=coalesce(?,target_value),current_value=coalesce(?,current_value),status=coalesce(?,status) where id=? and class_id=1", decimalOrNull(b.get("targetValue")), decimalOrNull(b.get("currentValue")), b.get("status"), id);
        audit(req, "UPDATE", "class_target", id, "更新班级诊改指标");
        return ok(Map.of("saved", true));
    }

    @PostMapping("/class-diagnoses/{id}/review")
    Map<String, Object> reviewDiagnosis(@PathVariable long id, @RequestBody Map<String, Object> b, HttpServletRequest req) {
        db.update("update class_target set current_value=?,status=coalesce(?,status) where id=? and class_id=1", decimalValue(b.get("currentValue")), b.get("status"), id);
        audit(req, "REVIEW", "class_target", id, "记录班级诊改复评");
        return ok(Map.of("saved", true));
    }

    @PostMapping("/students")
    @Transactional
    Map<String, Object> createStudent(@Valid @RequestBody StudentInput b, HttpServletRequest req) {
        var keys = new org.springframework.jdbc.support.GeneratedKeyHolder();
        db.update(connection -> {
            var statement = connection.prepareStatement("insert into student(class_id,student_no,name,gender,status) values(1,?,?,?,'ACTIVE')", new String[]{"id"});
            statement.setString(1, b.studentNo().trim());
            statement.setString(2, b.name().trim());
            statement.setString(3, b.gender());
            return statement;
        }, keys);
        long id = Objects.requireNonNull(keys.getKey()).longValue();
        audit(req, "CREATE", "student", id, "新增学生档案");
        return ok(Map.of("id", id));
    }

    @PutMapping("/students/{id}")
    @Transactional
    Map<String, Object> updateStudent(@PathVariable long id, @Valid @RequestBody StudentInput b, HttpServletRequest req) {
        ensureStudent(id);
        db.update("update student set name=?,gender=?,student_no=? where id=? and class_id=1", b.name().trim(), b.gender(), b.studentNo().trim(), id);
        audit(req, "UPDATE", "student", id, "更新学生档案");
        return ok(Map.of("saved", true));
    }

    @PostMapping("/students/{id}/archive")
    @Transactional
    Map<String, Object> archiveStudent(@PathVariable long id, HttpServletRequest req) {
        ensureStudent(id);
        db.update("update student set status='ARCHIVED',archived_at=now() where id=? and class_id=1", id);
        audit(req, "ARCHIVE", "student", id, "归档学生档案");
        return ok(Map.of("saved", true));
    }


    @GetMapping("/point-rules")
    Map<String, Object> pointRules() {
        return ok(Map.of("items", db.queryForList("select id,name,category,amount,enabled,description from point_rule where enabled=true order by id")));
    }

    @PostMapping("/point-rules")
    Map<String, Object> createPointRule(@RequestBody Map<String, Object> b, HttpServletRequest req) {
        db.update("insert into point_rule(name,category,amount,enabled,description,created_by) values(?,?,?,?,?,?)", reqString(b, "name", "新积分规则"), reqString(b, "category", "MANUAL"), intValue(b.get("amount")), b.getOrDefault("enabled", true), b.get("description"), userId(req));
        Long id = db.queryForObject("select max(id) from point_rule", Long.class);
        audit(req, "CREATE", "point_rule", id, "创建积分规则");
        return ok(Map.of("id", id));
    }

    private void ensureStudent(long id) {
        if (db.queryForObject("select count(*) from student where id=? and class_id=1", Integer.class, id) == 0)
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "学生档案不存在或不属于当前班级");
    }

    private String reqString(Map<String, Object> b, String key, String fallback) {
        Object v = b.get(key);
        return v == null || String.valueOf(v).isBlank() ? fallback : String.valueOf(v);
    }

    private LocalDate parseDate(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return LocalDate.now();
        return LocalDate.parse(String.valueOf(value));
    }

    private int intValue(Object value) {
        if (value == null) return 0;
        return Integer.parseInt(String.valueOf(value));
    }

    private BigDecimal decimalValue(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return BigDecimal.ZERO;
        return new BigDecimal(String.valueOf(value));
    }

    private BigDecimal decimalOrNull(Object value) {
        return value == null || String.valueOf(value).isBlank() ? null : new BigDecimal(String.valueOf(value));
    }

    private String jsonValue(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (Exception e) {
            return "[]";
        }
    }

    private long userId(HttpServletRequest r) {
        Object v = r.getAttribute("userId");
        return v == null ? 1 : Long.parseLong(v.toString());
    }

    private void audit(HttpServletRequest r, String action, String entity, long id, String summary) {
        db.update("insert into audit_log(actor_id,action,entity_type,entity_id,summary) values(?,?,?,?,?)", userId(r), action, entity, id, summary);
    }

    private Map<String, Object> ok(Object data) {
        return Map.of("code", "0", "message", "success", "data", data, "requestId", UUID.randomUUID().toString());
    }

    record Login(@NotBlank String username, @NotBlank String password) {
    }

    public record StudentInput(@NotBlank @jakarta.validation.constraints.Size(max = 80) String name,
                               @NotBlank @jakarta.validation.constraints.Size(max = 32) String studentNo,
                               @jakarta.validation.constraints.Size(max = 16) String gender) {
    }
}
