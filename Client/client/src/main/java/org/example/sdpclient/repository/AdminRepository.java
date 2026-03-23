package org.example.sdpclient.repository;

import org.example.sdpclient.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {
    Optional<Admin> findByUsername(String username);

    @Query("SELECT a FROM Admin a WHERE LOWER(a.username) = LOWER(:username)")
    Optional<Admin> findByUsernameIgnoreCase(@Param("username") String username);
}
