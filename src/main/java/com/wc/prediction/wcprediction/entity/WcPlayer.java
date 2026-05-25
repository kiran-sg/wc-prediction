package com.wc.prediction.wcprediction.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "wc_players")
public class WcPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_name")
    private String playerName;

    @Column(name = "team")
    private String team;

    @Column(name = "position")
    private String position; // GK, DEF, MID, FWD
}
