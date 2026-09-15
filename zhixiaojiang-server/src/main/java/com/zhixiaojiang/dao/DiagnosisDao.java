package com.zhixiaojiang.dao;

import com.zhixiaojiang.common.util.JdbcInsert;
import com.zhixiaojiang.common.util.RowMaps;
import com.zhixiaojiang.common.util.SqlParams;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 班级诊改目标与改进措施记录的数据访问。
 *
 * <p>新建目标状态固定为 IN_PROGRESS；取值域尚未定义，暂不建枚举。
 */
@Repository
public class DiagnosisDao {
    private final JdbcTemplate db;

    public DiagnosisDao(JdbcTemplate db) {
        this.db = db;
    }

    public List<Map<String, Object>> targets(List<Long> classIds) {
        return db.query("select id,name,target_value,current_value,unit,status,round(target_value-current_value,1) deviation from class_target where class_id in " + SqlParams.inClause(classIds) + " order by id", RowMaps.mapper(), classIds.toArray());
    }

    public List<Map<String, Object>> records(long targetId) {
        return db.query("select id,target_id,measure,review_result,recorded_on,created_by from class_diagnosis_record where target_id=? order by recorded_on desc", RowMaps.mapper(), targetId);
    }

    public long insertRecord(long targetId, String measure, String reviewResult, LocalDate recordedOn, long actor) {
        return JdbcInsert.returningId(db, "insert into class_diagnosis_record(target_id,measure,review_result,recorded_on,created_by) values(?,?,?,?,?)", targetId, measure, reviewResult, recordedOn, actor);
    }

    public long insertTarget(long classId, String name, BigDecimal targetValue, BigDecimal currentValue, String unit, long actor) {
        return JdbcInsert.returningId(db, "insert into class_target(class_id,name,target_value,current_value,unit,status,created_by) values(?,?,?,?,?,'IN_PROGRESS',?)", classId, name, targetValue, currentValue, unit, actor);
    }

    public int update(long targetId, BigDecimal targetValue, BigDecimal currentValue, String status) {
        return db.update("update class_target set target_value=coalesce(?,target_value),current_value=coalesce(?,current_value),status=coalesce(?,status) where id=?", targetValue, currentValue, status, targetId);
    }

    public int review(long targetId, BigDecimal currentValue, String status) {
        return db.update("update class_target set current_value=?,status=coalesce(?,status) where id=?", currentValue, status, targetId);
    }
}
