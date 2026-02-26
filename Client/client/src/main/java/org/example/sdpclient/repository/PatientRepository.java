package org.example.sdpclient.repository;

import org.example.sdpclient.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByUsername(String username);
    boolean existsByUsername(String username);

    @Query("SELECT p FROM Patient p WHERE LOWER(p.username) = LOWER(:username)")
    Optional<Patient> findByUsernameIgnoreCase(@Param("username") String username);

    @Query("SELECT COUNT(p) > 0 FROM Patient p WHERE LOWER(p.username) = LOWER(:username)")
    boolean existsByUsernameIgnoreCase(@Param("username") String username);

    @Query("""
        select p from Patient p
        where lower(p.firstName) like lower(concat('%', :q, '%'))
           or lower(p.lastName)  like lower(concat('%', :q, '%'))
           or lower(p.email)     like lower(concat('%', :q, '%'))
           or p.phone            like concat('%', :q, '%')
    """)
    List<Patient> searchByKeyword(@Param("q") String q);

    @Query("""
        select p from Patient p
        where p.linkedAdminId = :adminId
        and (lower(p.firstName) like lower(concat('%', :q, '%'))
           or lower(p.lastName)  like lower(concat('%', :q, '%'))
           or lower(p.email)     like lower(concat('%', :q, '%'))
           or p.phone            like concat('%', :q, '%'))
    """)
    List<Patient> searchByKeywordAndLinkedAdmin(@Param("q") String q, @Param("adminId") Long adminId);

    List<Patient> findByLinkedAdminId(Long linkedAdminId);
}

