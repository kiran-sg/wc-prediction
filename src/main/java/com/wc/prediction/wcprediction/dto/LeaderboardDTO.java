package com.wc.prediction.wcprediction.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LeaderboardDTO {
    private String userId;
    private String userName;
    private String location;
    private Integer totalPoints;
    private Integer position;

    public LeaderboardDTO(String userId, String userName, String location,
                          Integer totalPoints, Integer position) {
        this.userId = userId;
        this.userName = userName;
        this.location = location;
        this.totalPoints = totalPoints;
        this.position = position;
    }

}
