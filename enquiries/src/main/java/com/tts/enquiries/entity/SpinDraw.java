package com.tts.enquiries.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "spin_draws", indexes = {
    @Index(name = "idx_mobile", columnList = "student_mobile", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpinDraw {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_name", nullable = false, length = 100)
    private String studentName;

    @Column(name = "student_mobile", nullable = false, length = 15)
    private String studentMobile;

    @Column(name = "student_email", nullable = false, length = 100)
    private String studentEmail;

    @Column(name = "course_selected", nullable = false, length = 1000)
    private String courseSelected;

    @Column(name = "prize_won", nullable = false, length = 100)
    private String prizeWon;

    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    @Column(nullable = false, length = 50)
    private String status = "WON";

    @CreationTimestamp
    @Column(name = "spin_date", nullable = false, updatable = false)
    private LocalDateTime spinDate;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Column(nullable = false)
    private Boolean redeemed = false;

    @Column(name = "redeemed_date")
    private LocalDateTime redeemedDate;
}
