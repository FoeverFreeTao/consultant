package com.zyt.consultant.tools;

import com.zyt.consultant.entity.UserDailyStatus;
import com.zyt.consultant.mapper.UserDailyStatusMapper;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DailyStatusHealthTool {

    @Autowired
    private UserDailyStatusMapper userDailyStatusMapper;

    @Tool("根据当前会话memoryId判断用户当天饮水量、睡眠时长和运动时长是否健康并给出建议，输入memoryId例如user-4")
    public String evaluateTodayStatusByMemoryId(String memoryId) {
        Long userId = parseUserId(memoryId);
        if (userId == null || userId <= 0) {
            return "无法评估：memoryId无效或未包含用户ID。";
        }
        UserDailyStatus status = userDailyStatusMapper.findByUserId(userId);
        if (status == null) {
            return "无法评估：未找到该用户今日日常数据。";
        }
        return evaluate(status.getHydrationMl(), status.getSleepHour(), status.getActivityMinute());
    }

    @Tool("根据当前登录用户当天饮水量、睡眠时长和运动时长判断日常状态是否健康并给出建议，无需输入用户id")
    public String evaluateTodayStatusByCurrentUser() {
        return "请改为调用 evaluateTodayStatusByMemoryId 并传入当前会话memoryId。";
    }
    @Tool("根据饮水量(ml)、睡眠时长(小时)和运动时长(分钟)评估今日日常状态是否健康并给出建议")
    public String evaluateByValues(Integer hydrationMl, BigDecimal sleepHour, Integer activityMinute) {
        return evaluate(hydrationMl, sleepHour, activityMinute);
    }

    private Long parseUserId(String memoryId) {
        try {
            if (memoryId == null || !memoryId.startsWith("user-")) {
                return null;
            }
            return Long.parseLong(memoryId.substring(5));
        } catch (Exception exception) {
            return null;
        }
    }

    private String evaluate(Integer hydrationMl, BigDecimal sleepHour, Integer activityMinute) {
        if (hydrationMl == null || sleepHour == null || activityMinute == null) {
            return "无法评估：饮水量、睡眠时长、运动时长任一为空。";
        }

        int score = 0;
        StringBuilder advice = new StringBuilder();

        if (hydrationMl >= 1500 && hydrationMl <= 2500) {
            score += 1;
        } else if (hydrationMl < 1500) {
            advice.append("饮水偏少，建议分次补水，目标提升到 1500-2000ml；");
        } else {
            advice.append("饮水偏多，注意结合口渴与出汗量，避免短时大量饮水；");
        }

        double sleep = sleepHour.doubleValue();
        if (sleep >= 7.0 && sleep <= 9.0) {
            score += 1;
        } else if (sleep < 7.0) {
            advice.append("睡眠不足，建议提前入睡并保持固定作息，尽量达到 7-9 小时；");
        } else {
            advice.append("睡眠时长偏长，建议关注白天精神状态并优化作息节律；");
        }

        if (activityMinute >= 30 && activityMinute <= 120) {
            score += 1;
        } else if (activityMinute < 30) {
            advice.append("运动不足，建议每天至少 30 分钟中等强度活动；");
        } else {
            advice.append("运动时长较高，注意补水、拉伸与恢复，避免过度疲劳；");
        }

        String level;
        if (score == 3) {
            level = "整体状态良好";
        } else if (score == 2) {
            level = "整体状态一般";
        } else {
            level = "整体状态需重点改善";
        }

        String summary = String.format("今日数据：饮水 %dml，睡眠 %.1f 小时，运动 %d 分钟。评估结果：%s。", hydrationMl, sleep, activityMinute, level);
        if (advice.length() == 0) {
            return summary + "建议：继续保持当前节奏。";
        }
        return summary + "建议：" + advice;
    }
}
