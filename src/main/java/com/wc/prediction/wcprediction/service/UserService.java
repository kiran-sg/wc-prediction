package com.wc.prediction.wcprediction.service;

import com.wc.prediction.wcprediction.dto.WcUserDto;
import com.wc.prediction.wcprediction.entity.WcUser;
import com.wc.prediction.wcprediction.response.UserResponse;

import java.util.List;

public interface UserService {
    List<WcUser> getAllUsers();

    WcUser saveUser(WcUser user);

    UserResponse validateUser(String userId);

    void uploadData(List<WcUserDto> users);
}
