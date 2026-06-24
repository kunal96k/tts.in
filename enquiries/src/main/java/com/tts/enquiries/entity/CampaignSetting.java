package com.tts.enquiries.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "campaign_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CampaignSetting {

    @Id
    private Long id = 1L;

    @Column(name = "offer_start", nullable = false)
    private LocalDateTime offerStart;

    @Column(name = "offer_end", nullable = false)
    private LocalDateTime offerEnd;

    @Column(name = "send_email", nullable = false)
    private Boolean sendEmail = true;

    @Column(name = "grace_period", nullable = false)
    private Integer gracePeriod = 15;

    @Column(name = "is_emergency_locked", nullable = false)
    private Boolean isEmergencyLocked = false;

    @Column(name = "emergency_reason", length = 1000)
    private String emergencyReason = "";
}
