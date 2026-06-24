package com.tts.enquiries.repository;

import com.tts.enquiries.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParticipantRepository extends JpaRepository<Participant, Long>, JpaSpecificationExecutor<Participant> {
    Optional<Participant> findByMobile(String mobile);
    Optional<Participant> findByEmail(String email);
    boolean existsByMobileOrEmail(String mobile, String email);
    boolean existsByEmailAndStatusNot(String email, String status);
    Optional<Participant> findByEmailAndStatusNot(String email, String status);
    Optional<Participant> findByMobileAndStatusNot(String mobile, String status);
    Optional<Participant> findFirstByMobileOrderByIdDesc(String mobile);
    Optional<Participant> findFirstByEmailAndStatusNotOrderByIdDesc(String email, String status);
}
