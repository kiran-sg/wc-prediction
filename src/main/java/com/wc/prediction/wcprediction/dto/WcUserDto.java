package com.wc.prediction.wcprediction.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class WcUserDto {
    private Long id;
    private String userId;
    private String name;
    private String location;
    private String pwd;
    private Boolean isAdmin;
    private Boolean isSuperAdmin;
}
