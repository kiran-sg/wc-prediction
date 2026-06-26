package com.wc.prediction.wcprediction.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "wc_matches")
public class WcMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_no")
    private String matchNo;

    @Column(name = "date_time")
    private String dateTime;

    @Column(name = "team_a")
    private String teamA;

    @Column(name = "team_b")
    private String teamB;

    @Column(name = "group_name")
    private String groupName;

    @Column(name = "venue")
    private String venue;

    @Column(name = "stage")
    private String stage;
}
