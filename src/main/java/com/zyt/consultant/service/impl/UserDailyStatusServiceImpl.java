package com.zyt.consultant.service.impl;

import com.zyt.consultant.entity.UserDailyStatus;
import com.zyt.consultant.mapper.UserDailyStatusMapper;
import com.zyt.consultant.service.UserDailyStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class UserDailyStatusServiceImpl implements UserDailyStatusService {
    private static final Logger log = LoggerFactory.getLogger(UserDailyStatusServiceImpl.class);

    @Autowired
    private UserDailyStatusMapper userDailyStatusMapper;

    @Override
    @Transactional
    public UserDailyStatus getOrCreateByUserId(Long userId) {
        if (userId == null || userId <= 0) {
            log.warn("daily getOrCreate rejected, invalid userId={}", userId);
            return null;
        }
        log.info("daily getOrCreate start, userId={}", userId);
        UserDailyStatus status = userDailyStatusMapper.findByUserId(userId);
        if (status != null) {
            log.info("daily getOrCreate hit existing, userId={}, hydrationMl={}, sleepHour={}, activityMinute={}",
                    status.getUserId(), status.getHydrationMl(), status.getSleepHour(), status.getActivityMinute());
            return status;
        }
        try {
            LocalDateTime now = LocalDateTime.now();
            UserDailyStatus created = new UserDailyStatus();
            created.setUserId(userId);
            created.setHydrationMl(1350);
            created.setSleepHour(new BigDecimal("7.2"));
            created.setActivityMinute(42);
            created.setCreatedAt(now);
            created.setUpdatedAt(now);
            log.info("daily insert params, userId={}, hydrationMl={}, sleepHour={}, activityMinute={}",
                    created.getUserId(), created.getHydrationMl(), created.getSleepHour(), created.getActivityMinute());
            int inserted = userDailyStatusMapper.insertStatus(created);
            log.info("daily insert affectedRows={}, userId={}", inserted, created.getUserId());
            if (inserted <= 0) {
                return null;
            }
            UserDailyStatus latest = userDailyStatusMapper.findByUserId(userId);
            log.info("daily insert latest, userId={}, hydrationMl={}, sleepHour={}, activityMinute={}",
                    latest == null ? null : latest.getUserId(),
                    latest == null ? null : latest.getHydrationMl(),
                    latest == null ? null : latest.getSleepHour(),
                    latest == null ? null : latest.getActivityMinute());
            return latest;
        } catch (Exception exception) {
            log.error("daily getOrCreate failed, userId={}", userId, exception);
            return null;
        }
    }

    @Override
    @Transactional
    public UserDailyStatus updateByUserId(UserDailyStatus status) {
        if (status == null || status.getUserId() == null || status.getUserId() <= 0) {
            log.warn("daily update rejected, invalid status or userId, status={}", status);
            return null;
        }
        log.info("daily update start, userId={}, hydrationMl={}, sleepHour={}, activityMinute={}",
                status.getUserId(), status.getHydrationMl(), status.getSleepHour(), status.getActivityMinute());
        try {
            UserDailyStatus existing = userDailyStatusMapper.findByUserId(status.getUserId());
            if (existing == null) {
                UserDailyStatus created = new UserDailyStatus();
                created.setUserId(status.getUserId());
                created.setHydrationMl(status.getHydrationMl());
                created.setSleepHour(status.getSleepHour());
                created.setActivityMinute(status.getActivityMinute());
                created.setCreatedAt(LocalDateTime.now());
                created.setUpdatedAt(LocalDateTime.now());
                log.info("daily update fallback insert params, userId={}, hydrationMl={}, sleepHour={}, activityMinute={}",
                        created.getUserId(), created.getHydrationMl(), created.getSleepHour(), created.getActivityMinute());
                int inserted = userDailyStatusMapper.insertStatus(created);
                log.info("daily update fallback insert affectedRows={}, userId={}", inserted, created.getUserId());
                if (inserted <= 0) {
                    return null;
                }
                UserDailyStatus latest = userDailyStatusMapper.findByUserId(status.getUserId());
                log.info("daily update fallback latest, userId={}, hydrationMl={}, sleepHour={}, activityMinute={}",
                        latest == null ? null : latest.getUserId(),
                        latest == null ? null : latest.getHydrationMl(),
                        latest == null ? null : latest.getSleepHour(),
                        latest == null ? null : latest.getActivityMinute());
                return latest;
            }
            existing.setHydrationMl(status.getHydrationMl());
            existing.setSleepHour(status.getSleepHour());
            existing.setActivityMinute(status.getActivityMinute());
            existing.setUpdatedAt(LocalDateTime.now());
            log.info("daily update params, userId={}, hydrationMl={}, sleepHour={}, activityMinute={}",
                    existing.getUserId(), existing.getHydrationMl(), existing.getSleepHour(), existing.getActivityMinute());
            int updated = userDailyStatusMapper.updateByUserId(existing);
            log.info("daily update affectedRows={}, userId={}", updated, existing.getUserId());
            if (updated <= 0) {
                UserDailyStatus latestWhenZero = userDailyStatusMapper.findByUserId(status.getUserId());
                log.info("daily update affectedRows=0 latest, userId={}, hydrationMl={}, sleepHour={}, activityMinute={}",
                        latestWhenZero == null ? null : latestWhenZero.getUserId(),
                        latestWhenZero == null ? null : latestWhenZero.getHydrationMl(),
                        latestWhenZero == null ? null : latestWhenZero.getSleepHour(),
                        latestWhenZero == null ? null : latestWhenZero.getActivityMinute());
                return latestWhenZero;
            }
            UserDailyStatus latest = userDailyStatusMapper.findByUserId(status.getUserId());
            log.info("daily update latest, userId={}, hydrationMl={}, sleepHour={}, activityMinute={}",
                    latest == null ? null : latest.getUserId(),
                    latest == null ? null : latest.getHydrationMl(),
                    latest == null ? null : latest.getSleepHour(),
                    latest == null ? null : latest.getActivityMinute());
            return latest;
        } catch (Exception exception) {
            log.error("daily update failed, userId={}", status.getUserId(), exception);
            return null;
        }
    }
}
