package com.zyt.consultant.controller;

import com.zyt.consultant.entity.User;
import com.zyt.consultant.entity.UserDailyStatus;
import com.zyt.consultant.metrics.BusinessMetrics;
import com.zyt.consultant.service.UserDailyStatusService;
import com.zyt.consultant.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {
    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private UserDailyStatusService userDailyStatusService;
    @Autowired
    private BusinessMetrics businessMetrics;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        if (request == null || !StringUtils.hasText(request.getName()) || !StringUtils.hasText(request.getPhone()) || !StringUtils.hasText(request.getPassword())) {
            businessMetrics.recordUserAction("register", "invalid");
            return ResponseEntity.badRequest().body(buildResponse(false, "注册参数不完整", null));
        }
        User user = new User();
        user.setName(request.getName().trim());
        user.setPhone(request.getPhone().trim());
        user.setAge(request.getAge());
        user.setHeight(request.getHeight());
        user.setWeight(request.getWeight());
        User created = userService.register(user, request.getPassword());
        if (created == null) {
            businessMetrics.recordUserAction("register", "failed");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(buildResponse(false, "注册失败，手机号已存在或密码不符合要求", null));
        }
        businessMetrics.recordUserAction("register", "success");
        return ResponseEntity.ok(buildResponse(true, "注册成功", sanitizeUser(created)));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        if (request == null || !StringUtils.hasText(request.getPhone()) || !StringUtils.hasText(request.getPassword())) {
            businessMetrics.recordUserAction("login", "invalid");
            return ResponseEntity.badRequest().body(buildResponse(false, "登录参数不完整", null));
        }
        User user = userService.login(request.getPhone().trim(), request.getPassword());
        if (user == null) {
            businessMetrics.recordUserAction("login", "failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(buildResponse(false, "手机号或密码错误", null));
        }
        businessMetrics.recordUserAction("login", "success");
        return ResponseEntity.ok(buildResponse(true, "登录成功", sanitizeUser(user)));
    }

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> profile(@RequestParam("phone") String phone) {
        User user = userService.findByPhone(phone);
        if (user == null) {
            businessMetrics.recordUserAction("profile", "not_found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(buildResponse(false, "用户不存在", null));
        }
        businessMetrics.recordUserAction("profile", "success");
        return ResponseEntity.ok(buildResponse(true, "获取成功", sanitizeUser(user)));
    }

    @PostMapping("/profile/update")
    public ResponseEntity<Map<String, Object>> updateProfile(@RequestBody UpdateProfileRequest request) {
        if (request == null || !StringUtils.hasText(request.getPhone()) || !StringUtils.hasText(request.getName())) {
            businessMetrics.recordUserAction("profile_update", "invalid");
            return ResponseEntity.badRequest().body(buildResponse(false, "修改参数不完整", null));
        }
        User user = new User();
        user.setPhone(request.getPhone().trim());
        user.setName(request.getName().trim());
        user.setAge(request.getAge());
        user.setHeight(request.getHeight());
        user.setWeight(request.getWeight());
        User updated = userService.updateProfile(user);
        if (updated == null) {
            businessMetrics.recordUserAction("profile_update", "failed");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(buildResponse(false, "用户不存在或修改失败", null));
        }
        businessMetrics.recordUserAction("profile_update", "success");
        return ResponseEntity.ok(buildResponse(true, "修改成功", sanitizeUser(updated)));
    }

    @GetMapping("/daily")
    public ResponseEntity<Map<String, Object>> daily(@RequestParam("userId") Long userId) {
        log.info("daily query request, userId={}", userId);
        if (userId == null || userId <= 0) {
            businessMetrics.recordUserAction("daily_query", "invalid");
            return ResponseEntity.ok(buildResponse(false, "用户不存在", null));
        }
        UserDailyStatus status = userDailyStatusService.getOrCreateByUserId(userId);
        if (status == null) {
            businessMetrics.recordUserAction("daily_query", "not_found");
            return ResponseEntity.ok(buildResponse(false, "用户不存在", null));
        }
        businessMetrics.recordUserAction("daily_query", "success");
        return ResponseEntity.ok(buildResponse(true, "获取成功", sanitizeDailyStatus(status)));
    }

    @PostMapping("/daily/update")
    public ResponseEntity<Map<String, Object>> updateDaily(@RequestBody UpdateDailyRequest request) {
        log.info("daily update request, userId={}, hydrationMl={}, sleepHour={}, activityMinute={}",
                request == null ? null : request.getUserId(),
                request == null ? null : request.getHydrationMl(),
                request == null ? null : request.getSleepHour(),
                request == null ? null : request.getActivityMinute());
        if (request == null || request.getUserId() == null || request.getUserId() <= 0) {
            businessMetrics.recordUserAction("daily_update", "invalid");
            return ResponseEntity.badRequest().body(buildResponse(false, "修改参数不完整", null));
        }
        UserDailyStatus status = new UserDailyStatus();
        status.setUserId(request.getUserId());
        status.setHydrationMl(request.getHydrationMl());
        status.setSleepHour(request.getSleepHour());
        status.setActivityMinute(request.getActivityMinute());
        UserDailyStatus updated = userDailyStatusService.updateByUserId(status);
        if (updated == null) {
            businessMetrics.recordUserAction("daily_update", "failed");
            return ResponseEntity.ok(buildResponse(false, "用户不存在或修改失败", null));
        }
        businessMetrics.recordUserAction("daily_update", "success");
        businessMetrics.recordDailyStatus(updated.getHydrationMl(), updated.getSleepHour(), updated.getActivityMinute());
        return ResponseEntity.ok(buildResponse(true, "修改成功", sanitizeDailyStatus(updated)));
    }

    private Map<String, Object> buildResponse(boolean success, String message, Map<String, Object> data) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", success);
        response.put("message", message);
        response.put("data", data);
        return response;
    }

    private Map<String, Object> sanitizeUser(User user) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.getId());
        data.put("name", user.getName());
        data.put("phone", user.getPhone());
        data.put("age", user.getAge());
        data.put("height", user.getHeight());
        data.put("weight", user.getWeight());
        data.put("bmi", user.getBmi());
        return data;
    }

    private Map<String, Object> sanitizeDailyStatus(UserDailyStatus status) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", status.getId());
        data.put("userId", status.getUserId());
        data.put("hydrationMl", status.getHydrationMl());
        data.put("sleepHour", status.getSleepHour());
        data.put("activityMinute", status.getActivityMinute());
        return data;
    }

    public static class RegisterRequest {
        private String name;
        private String phone;
        private String password;
        private Integer age;
        private BigDecimal weight;
        private BigDecimal height;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        public BigDecimal getWeight() {
            return weight;
        }

        public void setWeight(BigDecimal weight) {
            this.weight = weight;
        }

        public BigDecimal getHeight() {
            return height;
        }

        public void setHeight(BigDecimal height) {
            this.height = height;
        }
    }

    public static class UpdateProfileRequest {
        private String name;
        private String phone;
        private Integer age;
        private BigDecimal weight;
        private BigDecimal height;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        public BigDecimal getWeight() {
            return weight;
        }

        public void setWeight(BigDecimal weight) {
            this.weight = weight;
        }

        public BigDecimal getHeight() {
            return height;
        }

        public void setHeight(BigDecimal height) {
            this.height = height;
        }
    }

    public static class UpdateDailyRequest {
        private Long userId;
        private Integer hydrationMl;
        private BigDecimal sleepHour;
        private Integer activityMinute;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Integer getHydrationMl() {
            return hydrationMl;
        }

        public void setHydrationMl(Integer hydrationMl) {
            this.hydrationMl = hydrationMl;
        }

        public BigDecimal getSleepHour() {
            return sleepHour;
        }

        public void setSleepHour(BigDecimal sleepHour) {
            this.sleepHour = sleepHour;
        }

        public Integer getActivityMinute() {
            return activityMinute;
        }

        public void setActivityMinute(Integer activityMinute) {
            this.activityMinute = activityMinute;
        }
    }

    public static class LoginRequest {
        private String phone;
        private String password;

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
