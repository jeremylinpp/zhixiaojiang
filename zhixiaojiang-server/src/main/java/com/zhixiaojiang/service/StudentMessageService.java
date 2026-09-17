package com.zhixiaojiang.service;

import com.zhixiaojiang.auth.StudentScope;
import com.zhixiaojiang.dao.StudentMessageMapper;
import com.zhixiaojiang.model.po.StudentMessage;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@Service
public class StudentMessageService {
 private final StudentMessageMapper messages;
 private final StudentScope scope;
 public StudentMessageService(StudentMessageMapper messages,StudentScope scope){this.messages=messages;this.scope=scope;}
 /** Call inside the business transaction: no message can precede a committed result. */
 public void send(long studentId,String key,String title,String content,String destination){
  StudentMessage message=new StudentMessage();
  message.setStudentId(studentId);message.setEventKey(key);message.setTitle(title);message.setContent(content);message.setDestination(destination);
  messages.insertIfAbsent(message);
 }
 public Map<String,Object> list(int page,int pageSize){
  long id=scope.studentId();int size=Math.max(1,Math.min(100,pageSize)),p=Math.max(1,Math.min(100000,page));
  return Map.of("items",messages.page(id,size,(p-1)*size),"total",messages.count(id),"unread",messages.unreadCount(id));
 }
 public Map<String,Object> read(long id){
  long student=scope.studentId();
  if(messages.countOwned(id,student)==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"消息不存在");
  messages.markRead(id,student);
  return Map.of("saved",true);
 }
}
