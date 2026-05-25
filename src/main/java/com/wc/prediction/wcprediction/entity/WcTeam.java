package com.wc.prediction.wcprediction.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "wc_teams")
public class WcTeam {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "short_name", unique = true)
    private String shortName;

    @Column(name = "team_name")
    private String teamName;

    @Column(name = "logo_url")
    private String logoUrl;
}
