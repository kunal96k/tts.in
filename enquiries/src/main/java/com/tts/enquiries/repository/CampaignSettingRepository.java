package com.tts.enquiries.repository;

import com.tts.enquiries.entity.CampaignSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CampaignSettingRepository extends JpaRepository<CampaignSetting, Long> {
}
