package com.zhixiaojiang;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
    private final TeacherScope scope;
    private final ObjectMapper json = new ObjectMapper();
    private final String secret;
    private final boolean secureCookie;
    private final String aiBaseUrl;
    private final String aiModel;
    private final String aiApiKey;
    private final org.springframework.web.client.RestClient aiClient;

    public ApiController(JdbcTemplate db, PasswordEncoder encoder, SessionRevocationService revocations, TeacherScope scope, org.springframework.core.env.Environment env) {
        this.db = db;
        this.encoder = encoder;
        this.revocations = revocations;
        this.scope = scope;
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
        long teacher = scope.teacher(req);
        return ok(db.queryForMap("select id,username,display_name as displayName,role from sys_user where id=?", teacher));
    }

    @GetMapping("/dashboard/class")
    Map<String, Object> dashboard(HttpServletRequest req) {
        var classIds = scope.classIds(req);
        if (classIds.isEmpty())
            return ok(Map.of("class", Map.of("name", "未关联班级", "grade", "—"), "metrics", emptyMetrics(), "targets", List.of(), "warnings", List.of(), "demo", true));
        var first = db.queryForMap("select name,grade from class_room where id=?", classIds.get(0));
        Map<String, Object> classInfo = classIds.size() == 1
                ? Map.of("name", first.get("name"), "grade", first.get("grade"))
                : Map.of("name", "全部任教班级（" + classIds.size() + " 个）", "grade", String.valueOf(first.get("grade")) + " 等");
        Object[] ids = classIds.toArray();
        String in = inClause(classIds);
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("studentCount", Optional.ofNullable(db.queryForObject("select count(*) from student where class_id in " + in + " and status='ACTIVE'", Integer.class, ids)).orElse(0));
        LocalDate attendanceDate = db.queryForObject("select max(ar.attendance_date) from attendance_record ar join student s on s.id=ar.student_id where s.class_id in " + in + " and s.status='ACTIVE'", LocalDate.class, ids);
        metrics.put("attendanceDate", attendanceDate);
        if (attendanceDate == null) {
            metrics.put("activeRate", null);
            metrics.put("attendanceCompleteness", 0);
        } else {
            Map<String, Object> a = db.queryForMap("select count(*) registeredCount, round(100.0*sum(case when ar.status in ('PRESENT','LATE') then 1 else 0 end)/nullif(count(*),0),1) attendanceRate from attendance_record ar join student s on s.id=ar.student_id where s.class_id in " + in + " and s.status='ACTIVE' and ar.attendance_date=?", with(ids, attendanceDate));
            metrics.put("activeRate", a.get("attendanceRate"));
            metrics.put("attendanceCompleteness", a.get("registeredCount"));
        }
        var targets = db.queryForList("select name,target_value as targetValue,current_value as currentValue,unit,status from class_target where class_id in " + in, ids);
        var warnings = db.queryForList("select w.id,w.level,w.summary,s.name studentName from warning_record w join student s on s.id=w.student_id where s.class_id in " + in + " and w.status='OPEN' order by w.created_at desc limit 50", ids);
        return ok(Map.of("class", classInfo, "metrics", metrics, "targets", targets, "warnings", warnings, "demo", true));
    }

    @GetMapping("/students")
    Map<String, Object> students(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize, HttpServletRequest req) {
        int safePage = Math.max(1, page), safeSize = Math.min(100, Math.max(1, pageSize));
        var classIds = scope.classIds(req);
        if (classIds.isEmpty()) return ok(Map.of("items", List.of(), "page", safePage, "pageSize", safeSize, "total", 0));
        Object[] ids = classIds.toArray();
        String in = inClause(classIds);
        String like = "%" + q.trim() + "%";
        var items = db.queryForList("select s.id,s.student_no as studentNo,s.name,s.gender,s.status,coalesce(round(avg(g.score),1),0) growthIndex from student s left join growth_record g on g.student_id=s.id where s.class_id in " + in + " and s.status='ACTIVE' and (s.name like ? or s.student_no like ?) group by s.id order by s.id limit ? offset ?", with(ids, like, like, safeSize, (safePage - 1) * safeSize));
        Integer total = db.queryForObject("select count(*) from student s where s.class_id in " + in + " and s.status='ACTIVE' and (s.name like ? or s.student_no like ?)", Integer.class, with(ids, like, like));
        return ok(Map.of("items", items, "page", safePage, "pageSize", safeSize, "total", total == null ? 0 : total));
    }

    @GetMapping("/students/{id}")
    Map<String, Object> student(@PathVariable long id, HttpServletRequest req) {
        scope.requireStudent(id, req);
        var s = db.queryForMap("select id,student_no as studentNo,name,gender,status from student where id=?", id);
        return ok(Map.of("student", s, "growth", db.queryForList("select dimension,round(avg(score),1) score from growth_record where student_id=? group by dimension", id), "scores", db.queryForList("select subject,exam_name examName,score,full_score fullScore,occurred_on occurredOn from score_record where student_id=? order by occurred_on", id), "timeline", db.queryForList("select title,detail,occurred_on occurredOn,source from growth_record where student_id=? order by occurred_on desc", id)));
    }

    @GetMapping("/students/{id}/portrait")
    Map<String, Object> portrait(@PathVariable long id, HttpServletRequest req) {
        scope.requireStudent(id, req);
        return ok(Map.of("studentId", id, "dimensions", db.queryForList("select dimension,round(avg(score),1) score from growth_record where student_id=? group by dimension", id)));
    }

    @GetMapping("/students/{id}/timeline")
    Map<String, Object> timeline(@PathVariable long id, HttpServletRequest req) {
        scope.requireStudent(id, req);
        return ok(Map.of("items", db.queryForList("select title,detail,occurred_on occurredOn,source,created_by createdBy from growth_record where student_id=? order by occurred_on desc", id)));
    }

    @GetMapping("/students/{id}/attendance")
    Map<String, Object> attendance(@PathVariable long id, HttpServletRequest req) {
        scope.requireStudent(id, req);
        return ok(Map.of("items", db.queryForList("select id,attendance_date attendanceDate,status,note,created_by createdBy from attendance_record where student_id=? order by attendance_date desc", id)));
    }

    @GetMapping("/students/{id}/behavior")
    Map<String, Object> behavior(@PathVariable long id, HttpServletRequest req) {
        scope.requireStudent(id, req);
        return ok(Map.of("items", db.queryForList("select id,category,score,occurred_on occurredOn,detail,created_by createdBy from behavior_record where student_id=? order by occurred_on desc", id)));
    }

    @PostMapping("/students/{id}/behavior")
    Map<String, Object> recordBehavior(@PathVariable long id, @RequestBody Map<String, Object> b, HttpServletRequest req) {
        scope.requireStudent(id, req);
        long rid = insertReturningId("insert into behavior_record(student_id,category,score,occurred_on,detail,created_by) values(?,?,?,?,?,?)", id, reqString(b, "category", "日常表现"), decimalOrNull(b.get("score")), parseDate(b.get("occurredOn")), b.get("detail"), userId(req));
        audit(req, "CREATE", "behavior_record", rid, "记录学生行为表现");
        return ok(Map.of("id", rid));
    }

    @GetMapping("/students/{id}/skills")
    Map<String, Object> skills(@PathVariable long id, HttpServletRequest req) {
        scope.requireStudent(id, req);
        return ok(Map.of("items", db.queryForList("select id,skill_name skillName,score,level,occurred_on occurredOn,evidence,created_by createdBy from skill_record where student_id=? order by occurred_on desc", id)));
    }

    @GetMapping("/students/{id}/evaluations")
    Map<String, Object> evaluations(@PathVariable long id, HttpServletRequest req) {
        scope.requireStudent(id, req);
        return ok(Map.of("items", db.queryForList("select id,period_start periodStart,period_end periodEnd,moral_score moralScore,skill_score skillScore,thinking_score thinkingScore,smart_score smartScore,evidence,created_by createdBy from dimension_evaluation where student_id=? order by period_end desc", id)));
    }

    @PostMapping("/warnings/analyze")
    @Transactional
    Map<String, Object> analyze(HttpServletRequest req) {
        int created = 0;
        LocalDate today = LocalDate.now(), since = today.minusDays(13), priorSince = today.minusDays(27);
        for (Map<String, Object> s : db.queryForList("select s.id from student s join class_room c on c.id=s.class_id where c.teacher_id=? and s.status='ACTIVE'", scope.teacher(req))) {
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
    Map<String, Object> interventions(HttpServletRequest req) {
        long teacher = scope.teacher(req);
        return ok(Map.of("items", db.queryForList("select i.id,i.title,i.status,i.teacher_note teacherNote,s.name studentName,i.review_at reviewAt from intervention_plan i join student s on s.id=i.student_id join class_room c on c.id=s.class_id where c.teacher_id=? order by i.updated_at desc limit 200", teacher)));
    }

    @PostMapping("/interventions")
    Map<String, Object> createIntervention(@RequestBody Map<String, Object> b, HttpServletRequest req) {
        long sid = Long.parseLong(String.valueOf(b.get("studentId")));
        scope.requireStudent(sid, req);
        Long warningId = longOrNull(b.get("warningId"));
        if (warningId != null) requireWarningOfStudent(warningId, sid);
        String title = reqString(b, "title", "阶段成长支持方案");
        String suggestions = jsonValue(b.getOrDefault("suggestions", List.of("班主任个别谈话", "两周后复评")));
        long id = insertReturningId("insert into intervention_plan(student_id,warning_id,title,status,suggestions_json,teacher_note,review_at,created_by) values(?,?,?, 'DRAFT',?,?,?,?)", sid, warningId, title, suggestions, b.get("teacherNote"), parseDate(b.get("reviewAt")), userId(req));
        audit(req, "CREATE", "intervention_plan", id, warningId == null ? "创建帮扶草案" : "由预警 #" + warningId + " 创建帮扶草案");
        return ok(Map.of("id", id, "status", "DRAFT"));
    }

    /**
     * 帮扶方案详情：方案本身 + 执行过程记录，供教师审核与复评页面使用。
     *
     * <p>这里显式构造 camelCase 键，不用 SQL 别名：H2（demo profile）会把未加引号的别名折叠成小写，
     * 而 MySQL 保留大小写，直接依赖别名会让两套数据库返回的 JSON 键不一致。
     */
    @GetMapping("/interventions/{id}")
    Map<String, Object> intervention(@PathVariable long id, HttpServletRequest req) {
        scope.requirePlan(id, req, false);
        Map<String, Object> plan = db.queryForObject(
                "select i.id,i.student_id,i.warning_id,i.title,i.status,i.suggestions_json,i.teacher_note,i.review_at,i.created_at,s.name from intervention_plan i join student s on s.id=i.student_id where i.id=?",
                (rs, n) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    Object warningId = rs.getObject("warning_id");
                    Object reviewAt = rs.getObject("review_at");
                    row.put("id", rs.getLong("id"));
                    row.put("studentId", rs.getLong("student_id"));
                    row.put("warningId", warningId == null ? null : ((Number) warningId).longValue());
                    row.put("studentName", rs.getString("name"));
                    row.put("title", rs.getString("title"));
                    row.put("status", rs.getString("status"));
                    row.put("suggestions", jsonList(rs.getString("suggestions_json")));
                    row.put("teacherNote", rs.getString("teacher_note"));
                    row.put("reviewAt", reviewAt == null ? null : reviewAt.toString());
                    row.put("createdAt", rs.getTimestamp("created_at").toString());
                    return row;
                }, id);
        var records = db.query("select id,action,status,occurred_on,result,created_by from intervention_record where plan_id=? order by id",
                (rs, n) -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getLong("id"));
                    row.put("action", rs.getString("action"));
                    row.put("status", rs.getString("status"));
                    row.put("occurredOn", rs.getObject("occurred_on").toString());
                    row.put("result", rs.getString("result"));
                    row.put("createdBy", rs.getLong("created_by"));
                    return row;
                }, id);
        return ok(Map.of("plan", plan, "records", records));
    }

    @PutMapping("/interventions/{id}")
    Map<String, Object> updateIntervention(@PathVariable long id, @RequestBody Map<String, Object> b, HttpServletRequest req) {
        scope.requirePlan(id, req, false);
        db.update("update intervention_plan set title=coalesce(?,title),teacher_note=coalesce(?,teacher_note),review_at=coalesce(?,review_at) where id=?", b.get("title"), b.get("teacherNote"), parseDateOrNull(b.get("reviewAt")), id);
        audit(req, "UPDATE", "intervention_plan", id, "更新帮扶草案");
        return ok(Map.of("saved", true));
    }

    @PostMapping("/interventions/{id}/transition")
    Map<String, Object> transitionIntervention(@PathVariable long id, @RequestBody Map<String, Object> b, HttpServletRequest req) {
        Map<String, Object> plan = scope.requirePlan(id, req, true);
        String next = reqString(b, "status", "");
        String current = String.valueOf(plan.get("status"));
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
        scope.requirePlan(id, req, false);
        db.update("update intervention_plan set review_at=?,teacher_note=coalesce(?,teacher_note) where id=?", parseDate(b.get("reviewAt")), b.get("teacherNote"), id);
        audit(req, "REVIEW", "intervention_plan", id, "记录阶段复评");
        return ok(Map.of("saved", true));
    }

    @PostMapping("/interventions/{id}/records")
    Map<String, Object> interventionRecord(@PathVariable long id, @RequestBody Map<String, Object> b, HttpServletRequest req) {
        scope.requirePlan(id, req, false);
        db.update("insert into intervention_record(plan_id,action,status,occurred_on,result,created_by) values(?,?,?,?,?,?)", id, b.get("action"), "DONE", LocalDate.now(), b.get("result"), userId(req));
        db.update("update intervention_plan set status='IN_PROGRESS' where id=? and status='CONFIRMED'", id);
        audit(req, "CREATE", "intervention_record", id, "记录帮扶过程");
        return ok(Map.of("saved", true));
    }

    @PostMapping("/ai/student-analysis")
    Map<String, Object> ai(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        long studentId = body.get("studentId") instanceof Number n ? n.longValue() : 0;
        if (studentId > 0) scope.requireStudent(studentId, req);
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
            if (studentId > 0)
                db.update("insert into ai_analysis(student_id,source,request_json,response_json,created_by) values(?,?,?,?,?)", studentId, String.valueOf(result.get("source")), jsonValue(Map.of("whitelist", "scores,attendance,behavior,skills,tasks")), jsonValue(result), userId(req));
        } catch (Exception ignored) {
        }
        return ok(result);
    }

    private Map<String, Object> templateAi() {
        return new LinkedHashMap<>(Map.of("source", "TEMPLATE", "summary", "该生近期学业表现、活动参与和行为表现呈同步下降趋势，建议进一步了解其学习及生活状态。", "evidence", List.of("数学成绩连续下降", "活动参与度下降", "迟到次数增加"), "suggestions", List.of("班主任个别谈话", "数学教师一对一指导", "学习小组伙伴结对", "两周后成长复评"), "disclaimer", "AI辅助建议，仅供教师参考", "requiresTeacherConfirmation", true));
    }

    /** 六机任务是教师自建的班级任务，按发布者隔离；其他教师的班级不含这些任务。 */
    @GetMapping("/growth-tasks")
    Map<String, Object> tasks(HttpServletRequest req) {
        return ok(Map.of("items", db.queryForList("select id,module,title,description,due_on dueOn,point_reward pointReward,status from growth_task where created_by=? order by due_on", scope.teacher(req))));
    }

    @PostMapping("/growth-tasks")
    Map<String, Object> createTask(@RequestBody Map<String, Object> b, HttpServletRequest req) {
        long id = insertReturningId("insert into growth_task(module,title,description,due_on,point_reward,status,created_by) values(?,?,?,?,?,'PUBLISHED',?)", reqString(b, "module", "聚机力"), reqString(b, "title", "成长任务"), b.get("description"), parseDate(b.get("dueOn")), intValue(b.get("pointReward")), userId(req));
        audit(req, "CREATE", "growth_task", id, "发布六机成长任务");
        return ok(Map.of("id", id));
    }

    @PostMapping("/growth-tasks/{id}/assign")
    Map<String, Object> assignTask(@PathVariable long id, @RequestBody Map<String, Object> b, HttpServletRequest req) {
        scope.requireTask(id, req, false);
        Object raw = b.get("studentIds");
        if (raw instanceof Collection<?> ids) for (Object sid : ids) {
            long studentId = Long.parseLong(String.valueOf(sid));
            scope.requireStudent(studentId, req);
            db.update("insert ignore into student_task(task_id,student_id,status) values(?,?,'ASSIGNED')", id, studentId);
        }
        audit(req, "ASSIGN", "growth_task", id, "分配成长任务");
        return ok(Map.of("saved", true));
    }

    @GetMapping("/growth-tasks/{id}/students")
    Map<String, Object> taskStudents(@PathVariable long id, HttpServletRequest req) {
        long teacher = scope.teacher(req);
        scope.requireTask(id, req, false);
        return ok(Map.of("items", db.queryForList("select st.id,st.student_id studentId,s.name studentName,st.status,st.completed_on completedOn,st.teacher_note teacherNote from student_task st join student s on s.id=st.student_id join class_room c on c.id=s.class_id where st.task_id=? and c.teacher_id=?", id, teacher)));
    }

    @PostMapping("/student-tasks/{id}/complete")
    @Transactional
    Map<String, Object> completeTask(@PathVariable long id, @RequestBody(required = false) Map<String, Object> b, HttpServletRequest req) {
        scope.requireStudentTask(id, req, true);
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
        scope.requireStudentTask(id, req, false);
        db.update("update student_task set teacher_note=? where id=?", b.get("note"), id);
        audit(req, "EVALUATE", "student_task", id, "记录任务评价");
        return ok(Map.of("saved", true));
    }

    @GetMapping("/class-diagnoses")
    Map<String, Object> diagnoses(HttpServletRequest req) {
        var cycle = List.of("目标", "标准", "计划", "实施", "监测", "诊断", "改进", "优化");
        var classIds = scope.classIds(req);
        if (classIds.isEmpty()) return ok(Map.of("items", List.of(), "cycle", cycle));
        return ok(Map.of("items", db.queryForList("select id,name,target_value targetValue,current_value currentValue,unit,status,round(target_value-current_value,1) deviation from class_target where class_id in " + inClause(classIds) + " order by id", classIds.toArray()), "cycle", cycle));
    }

    @GetMapping("/class-diagnoses/{id}/records")
    Map<String, Object> diagnosisRecords(@PathVariable long id, HttpServletRequest req) {
        scope.requireTarget(id, req);
        return ok(Map.of("items", db.queryForList("select id,target_id targetId,measure,review_result reviewResult,recorded_on recordedOn,created_by createdBy from class_diagnosis_record where target_id=? order by recorded_on desc", id)));
    }

    @PostMapping("/class-diagnoses/{id}/records")
    Map<String, Object> createDiagnosisRecord(@PathVariable long id, @RequestBody Map<String, Object> b, HttpServletRequest req) {
        scope.requireTarget(id, req);
        long rid = insertReturningId("insert into class_diagnosis_record(target_id,measure,review_result,recorded_on,created_by) values(?,?,?,?,?)", id, reqString(b, "measure", "记录改进措施"), b.get("reviewResult"), parseDate(b.get("recordedOn")), userId(req));
        audit(req, "CREATE", "class_diagnosis_record", rid, "记录班级诊改措施");
        return ok(Map.of("id", rid));
    }

    @PostMapping("/class-diagnoses")
    Map<String, Object> createDiagnosis(@RequestBody Map<String, Object> b, HttpServletRequest req) {
        Object rawClass = b.get("classId");
        long classId = rawClass == null || String.valueOf(rawClass).isBlank() ? scope.defaultClass(req) : Long.parseLong(String.valueOf(rawClass));
        scope.requireClass(classId, req);
        long id = insertReturningId("insert into class_target(class_id,name,target_value,current_value,unit,status,created_by) values(?,?,?,?,?,'IN_PROGRESS',?)", classId, reqString(b, "name", "新诊改目标"), decimalValue(b.get("targetValue")), decimalValue(b.getOrDefault("currentValue", 0)), b.getOrDefault("unit", "%"), userId(req));
        audit(req, "CREATE", "class_target", id, "创建班级诊改目标");
        return ok(Map.of("id", id));
    }

    @PutMapping("/class-diagnoses/{id}")
    Map<String, Object> updateDiagnosis(@PathVariable long id, @RequestBody Map<String, Object> b, HttpServletRequest req) {
        scope.requireTarget(id, req);
        db.update("update class_target set target_value=coalesce(?,target_value),current_value=coalesce(?,current_value),status=coalesce(?,status) where id=?", decimalOrNull(b.get("targetValue")), decimalOrNull(b.get("currentValue")), b.get("status"), id);
        audit(req, "UPDATE", "class_target", id, "更新班级诊改指标");
        return ok(Map.of("saved", true));
    }

    @PostMapping("/class-diagnoses/{id}/review")
    Map<String, Object> reviewDiagnosis(@PathVariable long id, @RequestBody Map<String, Object> b, HttpServletRequest req) {
        scope.requireTarget(id, req);
        db.update("update class_target set current_value=?,status=coalesce(?,status) where id=?", decimalValue(b.get("currentValue")), b.get("status"), id);
        audit(req, "REVIEW", "class_target", id, "记录班级诊改复评");
        return ok(Map.of("saved", true));
    }

    @PostMapping("/students")
    @Transactional
    Map<String, Object> createStudent(@Valid @RequestBody StudentInput b, HttpServletRequest req) {
        long classId = b.classId() == null ? scope.defaultClass(req) : b.classId();
        scope.requireClass(classId, req);
        long id = insertReturningId("insert into student(class_id,student_no,name,gender,status) values(?,?,?,?,'ACTIVE')", classId, b.studentNo().trim(), b.name().trim(), b.gender());
        audit(req, "CREATE", "student", id, "新增学生档案");
        return ok(Map.of("id", id));
    }

    @PutMapping("/students/{id}")
    @Transactional
    Map<String, Object> updateStudent(@PathVariable long id, @Valid @RequestBody StudentInput b, HttpServletRequest req) {
        scope.requireStudent(id, req);
        db.update("update student set name=?,gender=?,student_no=? where id=?", b.name().trim(), b.gender(), b.studentNo().trim(), id);
        audit(req, "UPDATE", "student", id, "更新学生档案");
        return ok(Map.of("saved", true));
    }

    @PostMapping("/students/{id}/archive")
    @Transactional
    Map<String, Object> archiveStudent(@PathVariable long id, HttpServletRequest req) {
        scope.requireStudent(id, req);
        db.update("update student set status='ARCHIVED',archived_at=now() where id=?", id);
        audit(req, "ARCHIVE", "student", id, "归档学生档案");
        return ok(Map.of("saved", true));
    }

    /** 积分规则是全校共用的量纲目录，不含学生数据，因此按启用状态全局可读。 */
    @GetMapping("/point-rules")
    Map<String, Object> pointRules() {
        return ok(Map.of("items", db.queryForList("select id,name,category,amount,enabled,description from point_rule where enabled=true order by id")));
    }

    @PostMapping("/point-rules")
    Map<String, Object> createPointRule(@RequestBody Map<String, Object> b, HttpServletRequest req) {
        long id = insertReturningId("insert into point_rule(name,category,amount,enabled,description,created_by) values(?,?,?,?,?,?)", reqString(b, "name", "新积分规则"), reqString(b, "category", "MANUAL"), intValue(b.get("amount")), b.getOrDefault("enabled", true), b.get("description"), userId(req));
        audit(req, "CREATE", "point_rule", id, "创建积分规则");
        return ok(Map.of("id", id));
    }

    /** 预警必须属于同一个学生，避免把方案挂到其他班级的预警上。 */
    private void requireWarningOfStudent(long warningId, long studentId) {
        Integer owned = db.queryForObject("select count(*) from warning_record where id=? and student_id=?", Integer.class, warningId, studentId);
        if (owned == null || owned == 0)
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "预警不存在或不属于该学生");
    }

    private Long longOrNull(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return null;
        return Long.parseLong(String.valueOf(value));
    }

    /** 把 JSON 数组列解析成数组对象；解析失败时回退为空数组。 */
    private List<Object> jsonList(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        try {
            return json.readValue(raw, new TypeReference<List<Object>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    private Map<String, Object> emptyMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("studentCount", 0);
        metrics.put("attendanceDate", null);
        metrics.put("activeRate", null);
        metrics.put("attendanceCompleteness", 0);
        return metrics;
    }

    /** 生成 in (?,?,?) 片段；调用方必须传入数量相同的参数。 */
    private String inClause(List<Long> ids) {
        return "(" + String.join(",", Collections.nCopies(ids.size(), "?")) + ")";
    }

    private Object[] with(Object[] base, Object... extra) {
        Object[] merged = Arrays.copyOf(base, base.length + extra.length);
        System.arraycopy(extra, 0, merged, base.length, extra.length);
        return merged;
    }

    private long insertReturningId(String sql, Object... values) {
        var keys = new org.springframework.jdbc.support.GeneratedKeyHolder();
        db.update(connection -> {
            var statement = connection.prepareStatement(sql, new String[]{"id"});
            for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
            return statement;
        }, keys);
        return Objects.requireNonNull(keys.getKey()).longValue();
    }

    private String reqString(Map<String, Object> b, String key, String fallback) {
        Object v = b.get(key);
        return v == null || String.valueOf(v).isBlank() ? fallback : String.valueOf(v);
    }

    private LocalDate parseDate(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return LocalDate.now();
        return LocalDate.parse(String.valueOf(value));
    }

    /** 未提供日期时保留原值，供部分更新（coalesce）使用。 */
    private LocalDate parseDateOrNull(Object value) {
        if (value == null || String.valueOf(value).isBlank()) return null;
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

    /** 未登录不再降级为 1 号教师。 */
    private long userId(HttpServletRequest r) {
        return scope.teacher(r);
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
                               @jakarta.validation.constraints.Size(max = 16) String gender,
                               Long classId) {
    }
}
