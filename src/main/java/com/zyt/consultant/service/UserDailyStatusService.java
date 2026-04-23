package com.zyt.consultant.service;

import com.zyt.consultant.entity.UserDailyStatus;

public interface UserDailyStatusService {
    UserDailyStatus getOrCreateByUserId(Long userId);
    UserDailyStatus updateByUserId(UserDailyStatus status);
}
