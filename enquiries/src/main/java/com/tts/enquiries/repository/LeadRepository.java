package com.tts.enquiries.repository;

import com.tts.enquiries.entity.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeadRepository extends JpaRepository<Lead, Long>, JpaSpecificationExecutor<Lead> {

    // Find all non-deleted leads
    @Query("SELECT l FROM Lead l WHERE l.isDeleted = false ORDER BY l.createdAt DESC")
    List<Lead> findAllActive();

    // Find non-deleted lead by id
    @Query("SELECT l FROM Lead l WHERE l.id = :id AND l.isDeleted = false")
    Optional<Lead> findActiveById(Long id);

    // Find by email (active leads only)
    @Query("SELECT l FROM Lead l WHERE l.email = :email AND l.isDeleted = false")
    Optional<Lead> findByEmail(String email);

    // Find by mobile (active leads only)
    @Query("SELECT l FROM Lead l WHERE l.mobile = :mobile AND l.isDeleted = false")
    Optional<Lead> findByMobile(String mobile);

    // Find first non-deleted lead by mobile
    Optional<Lead> findFirstByMobileAndIsDeletedFalseOrderByIdDesc(String mobile);
}
