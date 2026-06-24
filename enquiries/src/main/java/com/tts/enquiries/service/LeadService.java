package com.tts.enquiries.service;

import com.tts.enquiries.dto.LeadDTO;
import com.tts.enquiries.entity.CampaignSetting;
import com.tts.enquiries.entity.Lead;
import com.tts.enquiries.entity.Participant;
import com.tts.enquiries.exception.ResourceNotFoundException;
import com.tts.enquiries.repository.CampaignSettingRepository;
import com.tts.enquiries.repository.LeadRepository;
import com.tts.enquiries.repository.ParticipantRepository;
import com.tts.enquiries.repository.specification.LeadSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeadService {

    private final LeadRepository leadRepository;
    private final CampaignSettingRepository campaignSettingRepository;
    private final ParticipantRepository participantRepository;

    @Transactional
    public Lead createLead(LeadDTO leadDTO) {
        log.info("Creating new lead for email: {}", leadDTO.getEmail());

        // Check active campaign setting and record as participant if within active range
        boolean offerActive = false;
        try {
            Optional<CampaignSetting> settingsOpt = campaignSettingRepository.findById(1L);
            if (settingsOpt.isPresent()) {
                CampaignSetting settings = settingsOpt.get();
                LocalDateTime now = LocalDateTime.now();
                if (!settings.getIsEmergencyLocked() &&
                    !now.isBefore(settings.getOfferStart()) &&
                    !now.isAfter(settings.getOfferEnd())) {
                    offerActive = true;
                }
            }
        } catch (Exception e) {
            log.error("Failed to check campaign settings: {}", e.getMessage());
        }

        if (offerActive) {
            String cleanMobile = com.tts.enquiries.util.MobileUtils.normalizeMobile(leadDTO.getMobile());
            String cleanEmail = leadDTO.getEmail().trim().toLowerCase();

            // Check duplicate mobile or email in participants
            boolean exists = participantRepository.existsByMobileOrEmail(cleanMobile, cleanEmail);
            if (exists) {
                throw new IllegalArgumentException("Mobile number or Email is already registered for this active offer.");
            }

            Participant participant = new Participant();
            participant.setName(leadDTO.getName());
            participant.setMobile(cleanMobile);
            participant.setEmail(cleanEmail);
            participant.setCourseSelected(leadDTO.getService());
            participant.setStatus("ENQUIRED");
            Participant savedParticipant = participantRepository.save(participant);
            log.info("Recorded lead ONLY as Participant: {}", cleanMobile);

            // Return a dummy Lead object so controller / frontend references don't fail
            Lead dummyLead = new Lead();
            dummyLead.setId(savedParticipant.getId());
            dummyLead.setName(leadDTO.getName());
            dummyLead.setMobile(cleanMobile);
            dummyLead.setEmail(cleanEmail);
            dummyLead.setService(leadDTO.getService());
            dummyLead.setCreatedAt(savedParticipant.getCreatedAt());
            return dummyLead;
        } else {
            // Offer is not active, save to lead table as is
            Lead lead = new Lead();
            lead.setName(leadDTO.getName());
            lead.setMobile(com.tts.enquiries.util.MobileUtils.normalizeMobile(leadDTO.getMobile()));
            lead.setEmail(leadDTO.getEmail());
            lead.setService(leadDTO.getService());
            lead.setQualification(leadDTO.getQualification());
            lead.setBatch(leadDTO.getBatch());
            lead.setSource(leadDTO.getSource());
            lead.setStatus(leadDTO.getStatus() != null ? leadDTO.getStatus() : "New");
            lead.setMessage(leadDTO.getMessage());
            lead.setIsDeleted(false);

            Lead savedLead = leadRepository.save(lead);
            log.info("Lead created successfully in Lead table with ID: {}", savedLead.getId());
            return savedLead;
        }
    }


    @Transactional(readOnly = true)
    public List<Lead> getAllActiveLeads() {
        log.info("Fetching all active leads");
        return leadRepository.findAllActive();
    }

    @Transactional(readOnly = true)
    public Lead getLeadById(Long id) {
        log.info("Fetching lead with ID: {}", id);
        return leadRepository.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found with ID: " + id));
    }

    @Transactional
    public void softDeleteLead(Long id) {
        log.info("Soft deleting lead with ID: {}", id);
        Lead lead = getLeadById(id);
        lead.setIsDeleted(true);
        lead.setDeletedAt(LocalDateTime.now());
        leadRepository.save(lead);
        log.info("Lead soft deleted successfully with ID: {}", id);
    }

    @Transactional
    public Lead updateLead(Long id, LeadDTO leadDTO) {
        log.info("Updating lead with ID: {}", id);
        Lead lead = getLeadById(id);

        lead.setName(leadDTO.getName());
        lead.setMobile(com.tts.enquiries.util.MobileUtils.normalizeMobile(leadDTO.getMobile()));
        lead.setEmail(leadDTO.getEmail());
        lead.setService(leadDTO.getService());
        lead.setQualification(leadDTO.getQualification());
        lead.setBatch(leadDTO.getBatch());
        lead.setSource(leadDTO.getSource());
        if (leadDTO.getStatus() != null && !leadDTO.getStatus().trim().isEmpty()) {
            lead.setStatus(leadDTO.getStatus());
        }
        lead.setMessage(leadDTO.getMessage());

        Lead updatedLead = leadRepository.save(lead);
        log.info("Lead updated successfully with ID: {}", updatedLead.getId());

        return updatedLead;
    }

    @Transactional(readOnly = true)
    public Page<Lead> searchLeads(
            String search,
            String course,
            String status,
            LocalDate dateFrom,
            LocalDate dateTo,
            Pageable pageable
    ) {
        log.info("Searching leads with pagination. Search: {}, Course: {}, Status: {}, DateRange: {} to {}",
                search, course, status, dateFrom, dateTo);
        Specification<Lead> spec = LeadSpecification.filterLeads(search, course, status, dateFrom, dateTo);
        return leadRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public List<Lead> searchLeadsList(
            String search,
            String course,
            String status,
            LocalDate dateFrom,
            LocalDate dateTo,
            Sort sort
    ) {
        log.info("Searching leads list for export. Search: {}, Course: {}, Status: {}, DateRange: {} to {}",
                search, course, status, dateFrom, dateTo);
        Specification<Lead> spec = LeadSpecification.filterLeads(search, course, status, dateFrom, dateTo);
        return leadRepository.findAll(spec, sort);
    }
}
