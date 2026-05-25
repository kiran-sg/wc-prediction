package com.wc.prediction.wcprediction.model;

import com.opencsv.bean.CsvBindByName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WcPlayerCsv {
    @CsvBindByName(column = "player_name")
    private String playerName;
    @CsvBindByName(column = "team")
    private String team;
    @CsvBindByName(column = "position")
    private String position;
}
