package com.wc.prediction.wcprediction.service.impl;

import com.wc.prediction.wcprediction.dto.WcUserDto;
import com.wc.prediction.wcprediction.entity.WcUser;
import com.wc.prediction.wcprediction.repository.UserRepository;
import com.wc.prediction.wcprediction.response.UserResponse;
import com.wc.prediction.wcprediction.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public List<WcUser> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public WcUser saveUser(WcUser user) {
        return userRepository.save(user);
    }

    @Override
    public UserResponse validateUser(String userId) {
        UserResponse response = new UserResponse();
        WcUser user = userRepository.findByUserId(userId);
        boolean validUser = user != null && user.getUserId().equals(userId);
        response.setValidUser(validUser);
        if (validUser) {
            if ("superadmin".equals(user.getUserId()) && !Boolean.TRUE.equals(user.getIsSuperAdmin())) {
                user.setIsSuperAdmin(true);
                userRepository.save(user);
            }
            WcUserDto dto = new WcUserDto();
            dto.setUserId(user.getUserId());
            dto.setName(user.getName());
            dto.setLocation(user.getLocation());
            dto.setIsAdmin(user.getIsAdmin());
            dto.setIsSuperAdmin(user.getIsSuperAdmin());
            response.setUser(dto);
        }
        return response;
    }

    @Override
    public void uploadData(List<WcUserDto> dtos) {
        List<WcUser> users = new ArrayList<>();
        dtos.forEach(dto -> {
            if (userRepository.findByUserId(dto.getUserId()) == null) {
                WcUser user = new WcUser();
                user.setUserId(dto.getUserId());
                user.setName(dto.getName());
                user.setLocation(dto.getLocation());
                user.setIsAdmin(false);
                users.add(user);
            }
        });
        userRepository.saveAll(users);
    }
}
