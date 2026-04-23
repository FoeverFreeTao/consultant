package com.zyt.consultant.service.impl;

import com.zyt.consultant.entity.User;
import com.zyt.consultant.mapper.UserMapper;
import com.zyt.consultant.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    @Transactional
    public User register(User user, String rawPassword) {
        if (user == null
                || !StringUtils.hasText(user.getPhone())
                || !StringUtils.hasText(rawPassword)
                || rawPassword.length() < 6) {
            return null;
        }

        Long count = userMapper.countByPhone(user.getPhone());
        long exists = count == null ? 0L : count;
        if (exists > 0) {
            return null;
        }

        LocalDateTime now = LocalDateTime.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        user.setPassword(hashPassword(rawPassword));
        user.calculateBmi();

        int inserted = userMapper.insertUser(user);
        if (inserted <= 0) {
            return null;
        }

        User created = userMapper.findByPhone(user.getPhone());
        if (created == null) {
            return null;
        }

        created.setPassword(null);
        return created;
    }

    @Override
    public User login(String phone, String rawPassword) {
        if (!StringUtils.hasText(phone) || !StringUtils.hasText(rawPassword)) {
            return null;
        }

        User user = userMapper.findByPhone(phone);
        if (user == null) {
            return null;
        }

        String encrypted = hashPassword(rawPassword);
        if (!encrypted.equals(user.getPassword())) {
            return null;
        }

        user.setPassword(null);
        return user;
    }

    @Override
    public User findByPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return null;
        }

        User user = userMapper.findByPhone(phone);
        if (user == null) {
            return null;
        }

        user.setPassword(null);
        return user;
    }

    @Override
    public User findById(Long id) {
        if (id == null || id <= 0) {
            return null;
        }

        User user = userMapper.findById(id);
        if (user == null) {
            return null;
        }

        user.setPassword(null);
        return user;
    }

    @Override
    @Transactional
    public User updateProfile(User user) {
        if (user == null || !StringUtils.hasText(user.getPhone())) {
            return null;
        }

        User existing = userMapper.findByPhone(user.getPhone());
        if (existing == null) {
            return null;
        }

        existing.setName(user.getName());
        existing.setAge(user.getAge());
        existing.setHeight(user.getHeight());
        existing.setWeight(user.getWeight());
        existing.calculateBmi();
        existing.setUpdatedAt(LocalDateTime.now());

        int updated = userMapper.updateProfileByPhone(existing);
        if (updated <= 0) {
            return null;
        }

        User latest = userMapper.findByPhone(existing.getPhone());
        if (latest == null) {
            return null;
        }
        latest.setPassword(null);
        return latest;
    }

    private String hashPassword(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }
}
