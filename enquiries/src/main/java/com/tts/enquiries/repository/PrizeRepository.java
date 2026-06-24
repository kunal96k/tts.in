package com.tts.enquiries.repository;

import com.tts.enquiries.entity.Prize;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrizeRepository extends JpaRepository<Prize, Long> {
    Optional<Prize> findByName(String name);
}
