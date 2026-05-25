package com.wc.prediction.wcprediction.response;

import com.wc.prediction.wcprediction.dto.MatchResultDto;
import com.wc.prediction.wcprediction.dto.PredictionDto;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AdminResponse {
    private String message;
    private boolean status;
    private List<PredictionDto> predictions;
    private MatchResultDto matchResult;
}
