package com.zyt.consultant.service;

import com.zyt.consultant.entity.User;

public interface UserService {
    User register(User user, String rawPassword);
    User login(String phone, String rawPassword);
    User findByPhone(String phone);
    User updateProfile(User user);
}
