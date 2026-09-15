package com.zhixiaojiang.controller;

import com.zhixiaojiang.common.ApiResult;
import com.zhixiaojiang.model.dto.ProfileEditRequest;
import com.zhixiaojiang.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 教师资料：当前账号、显示名称与任教班级。 */
@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {
    private final ProfileService profile;

    public ProfileController(ProfileService profile) {
        this.profile = profile;
    }

    @GetMapping
    Map<String, Object> profile() {
        return ApiResult.ok(profile.load());
    }

    @PutMapping
    Map<String, Object> update(@Valid @RequestBody ProfileEditRequest body) {
        profile.update(body);
        return ApiResult.ok(Map.of("saved", true));
    }
}
