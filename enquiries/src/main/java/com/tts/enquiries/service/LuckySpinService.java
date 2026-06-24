package com.tts.enquiries.service;

import com.tts.enquiries.entity.CampaignSetting;
import com.tts.enquiries.entity.Lead;
import com.tts.enquiries.entity.Participant;
import com.tts.enquiries.entity.Prize;
import com.tts.enquiries.entity.SpinDraw;
import com.tts.enquiries.repository.CampaignSettingRepository;
import com.tts.enquiries.repository.LeadRepository;
import com.tts.enquiries.repository.ParticipantRepository;
import com.tts.enquiries.repository.PrizeRepository;
import com.tts.enquiries.repository.SpinDrawRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import com.tts.enquiries.repository.specification.ParticipantSpecification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class LuckySpinService {

    private final LeadRepository leadRepository;
    private final SpinDrawRepository spinDrawRepository;
    private final PrizeRepository prizeRepository;
    private final CampaignSettingRepository campaignSettingRepository;
    private final ParticipantRepository participantRepository;

    /**
     * Checks if a mobile number is eligible to participate in the lucky spin.
     */
    public Lead checkEligibility(String mobile) {
        String cleanMobile = com.tts.enquiries.util.MobileUtils.normalizeMobile(mobile);

        // 1. Check campaign status (active vs closed)
        boolean isEmergencyLocked = false;
        boolean campaignActive = false;
        Optional<CampaignSetting> settingsOpt = campaignSettingRepository.findById(1L);
        if (settingsOpt.isPresent()) {
            CampaignSetting settings = settingsOpt.get();
            if (settings.getIsEmergencyLocked()) {
                isEmergencyLocked = true;
            } else {
                LocalDateTime now = LocalDateTime.now();
                if (!now.isBefore(settings.getOfferStart()) && !now.isAfter(settings.getOfferEnd())) {
                    campaignActive = true;
                }
            }
        }

        if (isEmergencyLocked) {
            throw new IllegalStateException("The spin wheel campaign is temporarily paused for maintenance. Please check back later.");
        }

        if (campaignActive) {
            // 2. Look up participant by mobile (fetch latest entry)
            Optional<Participant> partOpt = participantRepository.findFirstByMobileOrderByIdDesc(cleanMobile);
            if (!partOpt.isPresent()) {
                throw new IllegalArgumentException("Mobile number is not registered. Please submit the enquiry form on the home page first.");
            }
            Participant participant = partOpt.get();
            String studentEmail = participant.getEmail().trim().toLowerCase();

            // 3. Duplicate checks: check both email and mobile
            boolean mobileSpun = participantRepository.findByMobileAndStatusNot(cleanMobile, "ENQUIRED").isPresent();
            boolean emailSpun = participantRepository.existsByEmailAndStatusNot(studentEmail, "ENQUIRED");

            if (mobileSpun || emailSpun) {
                String couponCode = participant.getCouponCode();
                if (couponCode == null || couponCode.isBlank() || couponCode.equals("-")) {
                    // Find if another record with same email has a coupon code
                    Optional<Participant> otherSpunOpt = participantRepository.findFirstByEmailAndStatusNotOrderByIdDesc(studentEmail, "ENQUIRED");
                    if (otherSpunOpt.isPresent()) {
                        couponCode = otherSpunOpt.get().getCouponCode();
                    }
                }
                throw new IllegalStateException("Already participated. Your coupon code is: " + (couponCode != null ? couponCode : "Better Luck"));
            }

            // Return Lead details mapped directly from the Participant record (no leadRepository query)
            Lead participantLead = new Lead();
            participantLead.setName(participant.getName());
            participantLead.setMobile(participant.getMobile());
            participantLead.setEmail(participant.getEmail());
            participantLead.setService(participant.getCourseSelected());
            return participantLead;
        } else {
            // Campaign is closed/inactive: fall back to the leads database
            Optional<Lead> leadOpt = leadRepository.findFirstByMobileAndIsDeletedFalseOrderByIdDesc(cleanMobile);
            if (!leadOpt.isPresent()) {
                throw new IllegalArgumentException("Mobile number is not registered. Please submit the enquiry form on the home page first.");
            }
            throw new IllegalStateException("The spin wheel campaign is currently not active.");
        }
    }

    /**
     * Performs the draw for a given mobile number and persists it.
     */
    @Transactional
    public SpinDraw performDraw(String mobile) {
        String cleanMobile = com.tts.enquiries.util.MobileUtils.normalizeMobile(mobile);
        Lead lead = checkEligibility(cleanMobile);
        String studentName  = lead.getName();
        String studentEmail = lead.getEmail();
        String courseSelected = lead.getService();

        String prizeWon   = null;
        String couponCode = null;

        // ── Step 1: Resolve which free-course prize matches this student's selected course ──
        String matchedFreeCourse = resolveFreeCourse(courseSelected);

        boolean freeCourseEligible = false;
        if (matchedFreeCourse != null) {
            // Check DB: has this specific free-course slot already been won once?
            long wonCount = spinDrawRepository.countByPrizeWon(matchedFreeCourse);
            // totalVolume for each free-course prize is 1 in the DB
            java.util.Optional<Prize> prizeOpt = prizeRepository.findByName(matchedFreeCourse);
            int limit = prizeOpt.map(Prize::getTotalVolume).orElse(1);
            if (wonCount < limit && prizeOpt.map(p -> p.getStatus().equalsIgnoreCase("Active")).orElse(false)) {
                freeCourseEligible = true;
            }
        }

        // ── Step 2: Random roll 1-100 ──
        int roll = (int) (Math.random() * 100) + 1;

        // ── Step 3: Try to win Free Course (5% chance) ──
        if (freeCourseEligible && roll <= 5) {
            prizeWon = matchedFreeCourse;
        }

        // ── Step 4: Try to win 40% Discount (10% window, max 2 winners) ──
        if (prizeWon == null) {
            java.util.Optional<Prize> prize40Opt = prizeRepository.findByName("40% Discount");
            int limit40 = prize40Opt.map(Prize::getTotalVolume).orElse(2);
            boolean prize40Active = prize40Opt.map(p -> p.getStatus().equalsIgnoreCase("Active")).orElse(false);
            long won40Count = spinDrawRepository.countByPrizeWon("40% Discount");

            if (prize40Active && won40Count < limit40 && roll > 5 && roll <= 15) {
                prizeWon = "40% Discount";
            }
        }

        // ── Step 5: Fallback roll: 10% Discount (80%) vs Better Luck (20%) ──
        if (prizeWon == null) {
            int secondRoll = (int) (Math.random() * 100) + 1;
            if (secondRoll <= 80) {
                prizeWon = "10% Discount";
            } else {
                prizeWon = "Better Luck Next Time";
            }
        }

        // ── Step 6: Generate coupon code ──
        String status = "WON";
        if (!prizeWon.equals("Better Luck Next Time")) {
            int num = (int) (1000 + Math.random() * 9000);
            String prefix = "TTS-DIS";
            if (prizeWon.contains("CCNA"))    prefix = "TTS-MON";
            else if (prizeWon.contains("RedHat")) prefix = "TTS-RH";
            else if (prizeWon.contains("C &"))    prefix = "TTS-CCP";
            else if (prizeWon.contains("Data"))   prefix = "TTS-DA";
            else if (prizeWon.contains("40%"))    prefix = "TTS-40";
            couponCode = prefix + "-" + num;
        } else {
            status = "BETTER LUCK";
        }

        SpinDraw draw = new SpinDraw();
        draw.setStudentName(studentName);
        draw.setStudentMobile(cleanMobile);
        draw.setStudentEmail(studentEmail);
        draw.setCourseSelected(courseSelected);
        draw.setPrizeWon(prizeWon);
        draw.setCouponCode(couponCode != null ? couponCode : "-");
        draw.setStatus(status);
        draw.setSpinDate(LocalDateTime.now());
        
        int graceDays = 15;
        Optional<CampaignSetting> settingsOpt = campaignSettingRepository.findById(1L);
        if (settingsOpt.isPresent()) {
            graceDays = settingsOpt.get().getGracePeriod();
        }
        draw.setExpiryDate(LocalDateTime.now().plusDays(graceDays));
        draw.setRedeemed(false);

        // Update the participant status, prize won and coupon code!
        Optional<Participant> partOpt = participantRepository.findFirstByMobileOrderByIdDesc(cleanMobile);
        if (partOpt.isPresent()) {
            Participant part = partOpt.get();
            part.setPrizeWon(prizeWon);
            part.setCouponCode(couponCode != null ? couponCode : "-");
            part.setStatus(status);
            participantRepository.save(part);
        }

        return spinDrawRepository.save(draw);
    }

    /**
     * Maps a student's selected course string to the exact free-course prize name stored in the DB.
     * Returns null if no matching free course exists.
     */
    private String resolveFreeCourse(String courseSelected) {
        if (courseSelected == null || courseSelected.isBlank()) return null;
        String upper = courseSelected.toUpperCase();
        if (upper.contains("CCNA") || upper.contains("CISCO"))                          return "CCNA Free Course";
        if (upper.contains("REDHAT") || upper.contains("RED HAT") || upper.contains("LINUX")) return "RedHat Free Course";
        if (upper.contains("C++") || upper.contains("C &") || upper.contains("C AND C"))      return "C & C++ Course with Free Course";
        if (upper.contains("DATA ANALYTICS") || upper.contains("ANALYTICS") || upper.contains("SQL") || upper.contains("POWERBI")) return "Data Analytics Course";
        return null;
    }

    /**
     * Verifies a coupon code.
     */
    public Optional<SpinDraw> verifyCoupon(String couponCode) {
        return spinDrawRepository.findByCouponCode(couponCode.trim().toUpperCase());
    }

    /**
     * Redeems a coupon code.
     */
    @Transactional
    public SpinDraw redeemCoupon(String couponCode) {
        String cleanCoupon = couponCode.trim().toUpperCase();
        Optional<SpinDraw> drawOpt = spinDrawRepository.findByCouponCode(cleanCoupon);
        if (!drawOpt.isPresent()) {
            throw new IllegalArgumentException("Coupon code not found.");
        }
        SpinDraw draw = drawOpt.get();
        if (draw.getRedeemed()) {
            throw new IllegalStateException("Coupon already redeemed.");
        }
        draw.setRedeemed(true);
        draw.setRedeemedDate(LocalDateTime.now());
        draw.setStatus("REDEEMED");

        // Update participant status as well!
        Optional<Participant> partOpt = participantRepository.findFirstByMobileOrderByIdDesc(draw.getStudentMobile());
        if (partOpt.isPresent()) {
            Participant part = partOpt.get();
            part.setStatus("REDEEMED");
            participantRepository.save(part);
        }

        return spinDrawRepository.save(draw);
    }

    /**
     * Returns lists of prizes with dynamic awarded counts from the database.
     */
    public List<Map<String, Object>> getPrizesReport() {
        List<Prize> dbPrizes = prizeRepository.findAll();
        List<Map<String, Object>> report = new ArrayList<>();

        for (Prize p : dbPrizes) {
            long won = spinDrawRepository.countByPrizeWon(p.getName());
            
            // For 10% Discount, both '10% Discount' and other similar references might match
            long redeemed = 0;
            long pending = 0;
            long expired = 0;

            // Fetch actual draws for this prize to count statuses
            List<SpinDraw> draws = spinDrawRepository.findAll().stream()
                    .filter(d -> d.getPrizeWon().equalsIgnoreCase(p.getName()))
                    .toList();

            for (SpinDraw d : draws) {
                if (d.getRedeemed()) {
                    redeemed++;
                } else if (d.getExpiryDate() != null && d.getExpiryDate().isBefore(LocalDateTime.now())) {
                    expired++;
                } else {
                    pending++;
                }
            }

            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("name", p.getName());
            map.put("type", p.getType());
            map.put("prob", p.getWinProbability());
            map.put("total", p.getTotalVolume());
            map.put("used", won);
            map.put("won", won);
            map.put("redeemed", redeemed);
            map.put("pending", pending);
            map.put("expired", expired);
            map.put("remaining", Math.max(0, p.getTotalVolume() - won));
            map.put("status", p.getStatus());
            
            String progressClass = "bg-primary";
            if (p.getName().contains("CCNA")) progressClass = "bg-primary";
            else if (p.getName().contains("RedHat")) progressClass = "bg-success";
            else if (p.getName().contains("C &")) progressClass = "bg-info";
            else if (p.getName().contains("Data")) progressClass = "bg-warning";
            else if (p.getName().contains("40%")) progressClass = "bg-danger";
            else if (p.getName().contains("20%") || p.getName().contains("10%")) progressClass = "bg-secondary";
            else progressClass = "bg-dark";

            map.put("progressClass", progressClass);
            report.add(map);
        }

        return report;
    }

    /**
     * Get live statistics of the lucky spin campaign.
     */
    public Map<String, Object> getLiveStats() {
        List<SpinDraw> allDraws = spinDrawRepository.findAll();
        long totalParticipants = participantRepository.count();
        
        long totalWinners = allDraws.stream()
                .filter(d -> !d.getPrizeWon().equalsIgnoreCase("Better Luck Next Time"))
                .count();

        long redeemed = allDraws.stream()
                .filter(SpinDraw::getRedeemed)
                .count();

        long expired = allDraws.stream()
                .filter(d -> !d.getRedeemed() && d.getExpiryDate() != null && d.getExpiryDate().isBefore(LocalDateTime.now()))
                .count();

        long pending = allDraws.stream()
                .filter(d -> !d.getRedeemed() && !d.getPrizeWon().equalsIgnoreCase("Better Luck Next Time") && (d.getExpiryDate() == null || d.getExpiryDate().isAfter(LocalDateTime.now())))
                .count();

        double conversion = totalWinners > 0 ? ((double) redeemed / totalWinners) * 100 : 0.0;

        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        long todayEntries = participantRepository.findAll().stream()
                .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(startOfToday))
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalParticipants", totalParticipants);
        stats.put("totalWinners", totalWinners);
        stats.put("redeemed", redeemed);
        stats.put("pending", pending);
        stats.put("expired", expired);
        stats.put("conversionRate", String.format(Locale.US, "%.1f%%", conversion));
        stats.put("todayEntries", todayEntries);
        stats.put("duplicatesBlocked", getDuplicatesBlocked());
        stats.put("activeCampaigns", 1);

        return stats;
    }

    private long getDuplicatesBlocked() {
        List<Lead> leads = leadRepository.findAll();
        Set<String> uniqueMobiles = new HashSet<>();
        Set<String> uniqueEmails = new HashSet<>();
        long duplicates = 0;
        for (Lead lead : leads) {
            String mobile = lead.getMobile() != null ? lead.getMobile().trim() : "";
            String email = lead.getEmail() != null ? lead.getEmail().trim().toLowerCase() : "";
            boolean mobileDuplicate = !mobile.isEmpty() && !uniqueMobiles.add(mobile);
            boolean emailDuplicate = !email.isEmpty() && !uniqueEmails.add(email);
            if (mobileDuplicate || emailDuplicate) {
                duplicates++;
            }
        }
        return duplicates;
    }

    @Transactional(readOnly = true)
    public Page<Participant> searchParticipants(
            String search,
            String course,
            String prize,
            String status,
            LocalDate dateFrom,
            LocalDate dateTo,
            Pageable pageable
    ) {
        Specification<Participant> spec = ParticipantSpecification.filterParticipants(search, course, prize, status, dateFrom, dateTo);
        return participantRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public List<Participant> searchParticipantsList(
            String search,
            String course,
            String prize,
            String status,
            LocalDate dateFrom,
            LocalDate dateTo,
            Sort sort
    ) {
        Specification<Participant> spec = ParticipantSpecification.filterParticipants(search, course, prize, status, dateFrom, dateTo);
        return participantRepository.findAll(spec, sort);
    }

    @Transactional(readOnly = true)
    public Page<Participant> searchWinners(
            String search,
            String status,
            LocalDate dateFrom,
            LocalDate dateTo,
            Pageable pageable
    ) {
        Specification<Participant> spec = ParticipantSpecification.filterWinners(search, status, dateFrom, dateTo);
        return participantRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public List<Participant> searchWinnersList(
            String search,
            String status,
            LocalDate dateFrom,
            LocalDate dateTo,
            Sort sort
    ) {
        Specification<Participant> spec = ParticipantSpecification.filterWinners(search, status, dateFrom, dateTo);
        return participantRepository.findAll(spec, sort);
    }
}
