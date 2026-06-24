package com.tts.enquiries.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "participants", indexes = {
    @Index(name = "idx_participant_mobile", columnList = "mobile", unique = true),
    @Index(name = "idx_participant_email", columnList = "email", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 15)
    private String mobile;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(name = "course_selected", length = 1000)
    private String courseSelected;

    @Column(name = "prize_won", length = 100)
    private String prizeWon;

    @Column(name = "coupon_code", length = 50)
    private String couponCode;

    @Column(nullable = false, length = 50)
    private String status = "ENQUIRED";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
