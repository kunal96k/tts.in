package com.tts.enquiries.repository;

import com.tts.enquiries.entity.SpinDraw;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpinDrawRepository extends JpaRepository<SpinDraw, Long> {
    Optional<SpinDraw> findByStudentMobile(String studentMobile);
    Optional<SpinDraw> findByCouponCode(String couponCode);
    List<SpinDraw> findByRedeemed(Boolean redeemed);
    long countByPrizeWon(String prizeWon);
    long countByRedeemed(Boolean redeemed);
}
