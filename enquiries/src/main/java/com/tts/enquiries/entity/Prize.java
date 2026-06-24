package com.tts.enquiries.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "prizes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Prize {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(name = "total_volume", nullable = false)
    private Integer totalVolume;

    @Column(name = "win_probability", nullable = false)
    private Integer winProbability;

    @Column(nullable = false, length = 50)
    private String status = "Active";
}
