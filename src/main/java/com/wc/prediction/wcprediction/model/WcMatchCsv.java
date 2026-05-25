package com.wc.prediction.wcprediction.model;

import com.opencsv.bean.CsvBindByName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WcMatchCsv {
    @CsvBindByName(column = "match_no")
    private String matchNo;
    @CsvBindByName(column = "date_time")
    private String dateTime;
    @CsvBindByName(column = "team_a")
    private String teamA;
    @CsvBindByName(column = "team_b")
    private String teamB;
    @CsvBindByName(column = "group_name")
    private String groupName;
    @CsvBindByName(column = "venue")
    private String venue;
}
