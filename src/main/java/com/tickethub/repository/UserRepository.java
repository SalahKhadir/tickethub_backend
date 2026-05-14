package com.tickethub.repository;

import com.tickethub.model.Role;
import com.tickethub.model.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findAllByEnabledFalse();

    @Query("select distinct u from User u join u.roles r where r = :role and u.enabled = true")
    List<User> findByRole(@Param("role") Role role);
}
