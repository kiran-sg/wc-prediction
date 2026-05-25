package com.wc.prediction.wcprediction.repository;

import com.wc.prediction.wcprediction.entity.WcUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<WcUser, Long> {

    @Query("SELECT u FROM WcUser u WHERE u.userId = ?1")
    WcUser findByUserId(String userId);
}
