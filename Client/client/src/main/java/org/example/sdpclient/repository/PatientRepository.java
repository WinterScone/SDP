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

    @Query("""
        select p from Patient p
        where lower(p.firstName) like lower(concat('%', :q, '%'))
           or lower(p.lastName)  like lower(concat('%', :q, '%'))
           or lower(p.email)     like lower(concat('%', :q, '%'))
           or p.phone            like concat('%', :q, '%')
    """)
    List<Patient> searchByKeyword(@Param("q") String q);
}

