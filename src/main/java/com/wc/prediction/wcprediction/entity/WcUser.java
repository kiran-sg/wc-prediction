package com.wc.prediction.wcprediction.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "wc_users")
public class WcUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private String userId;

    private String name;

    private String location;

    @Column(name = "is_admin")
    private Boolean isAdmin = false;

    @Column(name = "is_super_admin")
    private Boolean isSuperAdmin = false;
}
