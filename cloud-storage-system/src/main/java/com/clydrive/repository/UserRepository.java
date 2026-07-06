package com.clydrive.repository;

import com.clydrive.enums.Role;
import com.clydrive.enums.UserStatus;
import com.clydrive.module.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByRole(Role role);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByPhoneNumber(String phoneNumber);

    Optional<User> findByUsernameOrEmailOrPhoneNumber(String username, String email, String phoneNumber);

    @Query("SELECT COUNT(u) FROM User u WHERE u.status = :status")
    long countByStatus(UserStatus status);
}
