package com.wc.prediction.wcprediction.response;

import com.wc.prediction.wcprediction.dto.WcUserDto;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserResponse {
    private String message;
    private boolean validUser;
    private WcUserDto user;
}
