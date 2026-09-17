package com.zhixiaojiang.service;

import com.zhixiaojiang.auth.StudentScope;
import com.zhixiaojiang.common.util.RowMaps;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@Service
public class StudentMessageService {
 private final JdbcTemplate db;
 private final StudentScope scope;
 public StudentMessageService(JdbcTemplate db,StudentScope scope){this.db=db;this.scope=scope;}
 /** Call inside the business transaction: no message can precede a committed result. */
 public void send(long studentId,String key,String title,String content,String destination){
  db.update("insert ignore into student_message(student_id,event_key,title,content,destination) values(?,?,?,?,?)",studentId,key,title,content,destination);
 }
 public Map<String,Object> list(int page,int pageSize){
  long id=scope.studentId();int size=Math.max(1,Math.min(100,pageSize)),p=Math.max(1,Math.min(100000,page));
  return Map.of("items",db.query("select id,title,content,destination,created_at,read_at from student_message where student_id=? order by id desc limit ? offset ?",RowMaps.mapper(),id,size,(p-1)*size),"total",db.queryForObject("select count(*) from student_message where student_id=?",Long.class,id),"unread",db.queryForObject("select count(*) from student_message where student_id=? and read_at is null",Long.class,id));
 }
 public Map<String,Object> read(long id){
  long student=scope.studentId();
  if(db.queryForObject("select count(*) from student_message where id=? and student_id=?",Integer.class,id,student)==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"消息不存在");
  db.update("update student_message set read_at=coalesce(read_at,current_timestamp) where id=? and student_id=?",id,student);
  return Map.of("saved",true);
 }
}
