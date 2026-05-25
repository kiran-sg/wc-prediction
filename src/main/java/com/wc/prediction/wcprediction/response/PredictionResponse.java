package com.wc.prediction.wcprediction.response;

import com.wc.prediction.wcprediction.dto.PredictionDto;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PredictionResponse {
    private String message;
    private boolean status;
    private boolean invalidUser;
    private PredictionDto prediction;
    private List<PredictionDto> predictions;
}
