package com.tts.enquiries.config;

import com.tts.enquiries.entity.CampaignSetting;
import com.tts.enquiries.entity.Prize;
import com.tts.enquiries.repository.CampaignSettingRepository;
import com.tts.enquiries.repository.PrizeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Seeds the database with default Prize and Campaign settings on first startup.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final PrizeRepository prizeRepository;
    private final CampaignSettingRepository campaignSettingRepository;

    @Override
    public void run(ApplicationArguments args) {
        // Seed campaign settings
        if (campaignSettingRepository.count() == 0) {
            log.info("No campaign settings found — seeding default settings...");
            CampaignSetting settings = new CampaignSetting();
            settings.setId(1L);
            settings.setOfferStart(LocalDateTime.of(2026, 6, 24, 0, 0));
            settings.setOfferEnd(LocalDateTime.of(2026, 7, 24, 23, 59));
            settings.setSendEmail(true);
            settings.setGracePeriod(15);
            settings.setIsEmergencyLocked(false);
            settings.setEmergencyReason("");
            campaignSettingRepository.save(settings);
            log.info("Seeded default campaign settings.");
        }

        // Seed prizes
        long count = prizeRepository.count();
        if (count == 0) {
            log.info("No prizes found — seeding default prize configuration...");

            List<Prize> defaultPrizes = List.of(
                // ── Free Course prizes (1 winner per course category) ──
                buildPrize("CCNA Free Course",                 "Free Course", 5,  1,    "Active"),
                buildPrize("RedHat Free Course",               "Free Course", 5,  1,    "Active"),
                buildPrize("C & C++ Course with Free Course",  "Free Course", 5,  1,    "Active"),
                buildPrize("Data Analytics Course",            "Free Course", 5,  1,    "Active"),

                // ── Discount prizes ──
                buildPrize("40% Discount",          "Discount", 10,   2,    "Active"),
                buildPrize("10% Discount",          "Discount", 80,   999,  "Active"),

                // ── Consolation ──
                buildPrize("Better Luck Next Time", "No Win",   20,   9999, "Active")
            );

            prizeRepository.saveAll(defaultPrizes);
            log.info("Seeded {} default prizes.", defaultPrizes.size());
        } else {
            log.info("Prize table already has {} rows — skipping seed.", count);
        }
    }

    private Prize buildPrize(String name, String type, int winProbability, int totalVolume, String status) {
        Prize p = new Prize();
        p.setName(name);
        p.setType(type);
        p.setWinProbability(winProbability);
        p.setTotalVolume(totalVolume);
        p.setStatus(status);
        return p;
    }
}

