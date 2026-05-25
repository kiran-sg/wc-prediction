package com.wc.prediction.wcprediction.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PredictionRequest {
    private String userId;
    private String matchId;
    private List<String> matchIds;
}
