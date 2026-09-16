package com.zhixiaojiang.service;

import com.zhixiaojiang.auth.TeacherScope;
import com.zhixiaojiang.common.constant.WarningStatus;
import com.zhixiaojiang.dao.AssistantDao;
import com.zhixiaojiang.dao.WarningDao;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 智小匠助手页的数据来源：AI 能力状态 + 当前已录入的数据量，全部来自真实记录。
 */
@Service
public class AssistantService {
    private final AssistantDao assistant;
    private final WarningDao warnings;
    private final AiAnalysisService ai;
    private final TeacherScope scope;

    public AssistantService(AssistantDao assistant, WarningDao warnings, AiAnalysisService ai, TeacherScope scope) {
        this.assistant = assistant;
        this.warnings = warnings;
        this.ai = ai;
        this.scope = scope;
    }

    public Map<String, Object> status() {
        long teacher = scope.teacher();
        var classIds = scope.classIds();
        Map<String, Object> data = new LinkedHashMap<>();
        if (classIds.isEmpty()) {
            data.put("students", 0);
            data.put("growthRecords", 0);
            data.put("scores", 0);
            data.put("skills", 0);
            data.put("openWarnings", 0);
        } else {
            data.put("students", assistant.countStudents(classIds));
            data.put("growthRecords", assistant.countForStudents(classIds, "growth_record"));
            data.put("scores", assistant.countForStudents(classIds, "score_record"));
            data.put("skills", assistant.countForStudents(classIds, "skill_record"));
            data.put("openWarnings", warnings.count(teacher, WarningStatus.OPEN.name(), "%%"));
        }
        data.put("lastRuleAnalysisAt", assistant.lastRuleAnalysisAt(teacher));
        return Map.of("ai", ai.status(), "data", data, "boundaries", List.of(
                "结论由班主任作出，助手只做数据归纳与建议",
                "不生成心理或医学诊断结论",
                "送模型的数据不含姓名、学号与联系方式",
                "生成的建议必须经教师确认后才能进入帮扶方案"));
    }
}
