package com.zhixiaojiang.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

/**
 * 当前登录教师的提供者。
 *
 * <p>JwtFilter 校验会话后把教师 id 写入请求属性。业务层通过本类取用，不必层层传递
 * {@link HttpServletRequest}，Service 与 DAO 因此不依赖 Web 类型。
 */
@Component
public class CurrentTeacher {
    /** 请求属性名，由 JwtFilter 写入。 */
    public static final String USER_ID_ATTRIBUTE = "userId";

    /** 当前教师 id；无请求上下文或未登录时抛 401。 */
    public long id() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        Object actor = attributes == null ? null : attributes.getAttribute(USER_ID_ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (actor == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        return Long.parseLong(actor.toString());
    }
}
