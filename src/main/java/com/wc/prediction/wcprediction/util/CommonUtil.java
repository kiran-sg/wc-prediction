package com.wc.prediction.wcprediction.util;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public class CommonUtil {

    public static boolean isPredictionAllowed(String matchDateTime) {
        LocalDateTime matchTime;
        if (matchDateTime.contains("+") || matchDateTime.contains("Z")) {
            matchTime = OffsetDateTime.parse(matchDateTime).toLocalDateTime();
        } else if (matchDateTime.contains("T")) {
            matchTime = LocalDateTime.parse(matchDateTime);
        } else {
            matchTime = LocalDateTime.parse(matchDateTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        // Lock predictions at kickoff time
        return LocalDateTime.now().isBefore(matchTime);
    }
}
