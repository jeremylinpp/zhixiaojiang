package com.zhixiaojiang.controller;
import com.zhixiaojiang.common.ApiResult;
import com.zhixiaojiang.service.TaskAttachmentService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
@RestController
@RequestMapping("/api/v1")
public class TaskAttachmentController {
 private final TaskAttachmentService files;
 public TaskAttachmentController(TaskAttachmentService files){this.files=files;}
 @GetMapping("/student-portal/tasks/{id}/attachments") public Map<String,Object> staged(@PathVariable long id){return ApiResult.ok(files.staged(id));}
 @PostMapping(value="/student-portal/tasks/{id}/attachments",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
 public Map<String,Object> upload(@PathVariable long id,@RequestParam String requestKey,@RequestParam MultipartFile file)throws IOException{return ApiResult.ok(files.upload(id,requestKey,file));}
 @DeleteMapping("/student-portal/attachments/{id}") public Map<String,Object> remove(@PathVariable long id){return ApiResult.ok(files.remove(id));}
 @GetMapping("/student-portal/attachments/{id}") public ResponseEntity<byte[]> student(@PathVariable long id){return download(id,false);}
 @GetMapping("/task-attachments/{id}") public ResponseEntity<byte[]> teacher(@PathVariable long id){return download(id,true);}
 private ResponseEntity<byte[]> download(long id,boolean teacher){var result=files.download(id,teacher);return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM).header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(result.name(),StandardCharsets.UTF_8).build().toString()).header("X-Content-Type-Options","nosniff").cacheControl(CacheControl.noStore()).body(result.bytes());}
}
