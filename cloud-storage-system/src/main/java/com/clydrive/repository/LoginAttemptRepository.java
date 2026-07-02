package com.clydrive.repository;

import com.clydrive.module.LoginAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    List<LoginAttempt> findByUserIdOrderByAttemptTimeDesc(Long userId);
}
