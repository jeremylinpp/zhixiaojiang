package com.zhixiaojiang.controller;

import com.zhixiaojiang.common.ApiResult;
import com.zhixiaojiang.service.StudentAccountService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class StudentAccountController {
    private final StudentAccountService accounts;
    public StudentAccountController(StudentAccountService accounts){this.accounts=accounts;}
    public record CreateAccount(String username,String initialPassword){}
    public record ChangePassword(String currentPassword,String newPassword){}
    @GetMapping("/students/{id}/account")
    public Map<String,Object> account(@PathVariable long id){return ApiResult.ok(accounts.account(id));}
    @PostMapping("/students/{id}/account")
    public Map<String,Object> create(@PathVariable long id,@RequestBody CreateAccount body){return ApiResult.ok(accounts.create(id,body.username(),body.initialPassword()));}
    @PostMapping("/student-portal/password")
    public Map<String,Object> password(@RequestBody ChangePassword body){return ApiResult.ok(accounts.changePassword(body.currentPassword(),body.newPassword()));}
}
