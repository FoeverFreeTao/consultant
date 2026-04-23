package com.zyt.consultant.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

@Component
public class BusinessMetrics {

    private final MeterRegistry meterRegistry;
    private final Counter chatRequestCounter;
    private final Counter chatSuccessCounter;
    private final Counter chatErrorCounter;
    private final DistributionSummary chatMessageLengthSummary;
    private final DistributionSummary hydrationMlSummary;
    private final DistributionSummary sleepHourSummary;
    private final DistributionSummary activityMinuteSummary;

    public BusinessMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.chatRequestCounter = Counter.builder("diet_helper_chat_requests_total").register(meterRegistry);
        this.chatSuccessCounter = Counter.builder("diet_helper_chat_success_total").register(meterRegistry);
        this.chatErrorCounter = Counter.builder("diet_helper_chat_error_total").register(meterRegistry);
        this.chatMessageLengthSummary = DistributionSummary.builder("diet_helper_chat_message_length")
                .baseUnit("chars")
                .register(meterRegistry);
        this.hydrationMlSummary = DistributionSummary.builder("diet_helper_daily_hydration_ml")
                .baseUnit("ml")
                .register(meterRegistry);
        this.sleepHourSummary = DistributionSummary.builder("diet_helper_daily_sleep_hour")
                .baseUnit("hour")
                .register(meterRegistry);
        this.activityMinuteSummary = DistributionSummary.builder("diet_helper_daily_activity_minute")
                .baseUnit("minute")
                .register(meterRegistry);
    }

    public void recordChatRequest(String message) {
        chatRequestCounter.increment();
        if (message != null) {
            chatMessageLengthSummary.record(message.length());
        }
    }

    public void recordChatResult(boolean success, long durationNanos) {
        if (success) {
            chatSuccessCounter.increment();
        } else {
            chatErrorCounter.increment();
        }
        Timer.builder("diet_helper_chat_latency")
                .tag("outcome", success ? "success" : "error")
                .register(meterRegistry)
                .record(durationNanos, TimeUnit.NANOSECONDS);
    }

    public void recordUserAction(String action, String outcome) {
        Counter.builder("diet_helper_user_action_total")
                .tag("action", action)
                .tag("outcome", outcome)
                .register(meterRegistry)
                .increment();
    }

    public void recordDailyStatus(Integer hydrationMl, BigDecimal sleepHour, Integer activityMinute) {
        if (hydrationMl != null) {
            hydrationMlSummary.record(hydrationMl);
        }
        if (sleepHour != null) {
            sleepHourSummary.record(sleepHour.doubleValue());
        }
        if (activityMinute != null) {
            activityMinuteSummary.record(activityMinute);
        }
    }
}
