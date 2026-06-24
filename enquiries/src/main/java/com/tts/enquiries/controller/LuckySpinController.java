package com.tts.enquiries.controller;

import com.tts.enquiries.entity.CampaignSetting;
import com.tts.enquiries.entity.Lead;
import com.tts.enquiries.entity.Participant;
import com.tts.enquiries.entity.Prize;
import com.tts.enquiries.entity.SpinDraw;
import com.tts.enquiries.repository.CampaignSettingRepository;
import com.tts.enquiries.repository.ParticipantRepository;
import com.tts.enquiries.repository.SpinDrawRepository;
import com.tts.enquiries.service.LuckySpinService;
import com.tts.enquiries.service.PrizeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/lucky-spin")
@RequiredArgsConstructor
@Slf4j
public class LuckySpinController {

    private final LuckySpinService luckySpinService;
    private final PrizeService prizeService;
    private final SpinDrawRepository spinDrawRepository;
    private final CampaignSettingRepository campaignSettingRepository;
    private final ParticipantRepository participantRepository;

    /**
     * Public endpoint to check if a mobile number is eligible.
     */
    @PostMapping("/check-eligibility")
    public ResponseEntity<?> checkEligibility(@RequestBody Map<String, String> request) {
        String mobile = request.get("mobile");
        if (mobile == null || mobile.trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Mobile number is required.");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            String cleanMobile = com.tts.enquiries.util.MobileUtils.normalizeMobile(mobile);
            Lead lead = luckySpinService.checkEligibility(cleanMobile);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("name", lead.getName());
            response.put("email", lead.getEmail());
            response.put("course", lead.getService());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("status", "NOT_REGISTERED");
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("status", "ALREADY_SPUN");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An error occurred checking eligibility: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Public endpoint to check if a participant with the given email or mobile number already exists
     * during active offer periods.
     */
    @PostMapping("/check-duplicate")
    public ResponseEntity<?> checkDuplicate(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String mobile = request.get("mobile");

        boolean isDuplicate = false;
        String message = "No duplicate found.";

        try {
            // Determine if the offer period is active
            boolean offerActive = false;
            Optional<CampaignSetting> settingsOpt = campaignSettingRepository.findById(1L);
            if (settingsOpt.isPresent()) {
                CampaignSetting settings = settingsOpt.get();
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                if (!settings.getIsEmergencyLocked() &&
                    !now.isBefore(settings.getOfferStart()) &&
                    !now.isAfter(settings.getOfferEnd())) {
                    offerActive = true;
                }
            }

            // Only check duplication if offer is active
            if (offerActive) {
                if (mobile != null && !mobile.trim().isEmpty()) {
                    String cleanMobile = com.tts.enquiries.util.MobileUtils.normalizeMobile(mobile);
                    if (participantRepository.findByMobile(cleanMobile).isPresent()) {
                        isDuplicate = true;
                        message = "This WhatsApp number is already registered for the active offer.";
                    }
                }
                if (!isDuplicate && email != null && !email.trim().isEmpty()) {
                    String cleanEmail = email.trim().toLowerCase();
                    if (participantRepository.findByEmail(cleanEmail).isPresent()) {
                        isDuplicate = true;
                        message = "This Email is already registered for the active offer.";
                    }
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("duplicate", isDuplicate);
            response.put("message", message);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error checking duplicate: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "An error occurred checking duplicate: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Public endpoint to perform the spin draw.
     */
    @PostMapping("/spin")
    public ResponseEntity<?> spin(@RequestBody Map<String, String> request) {
        String mobile = request.get("mobile");
        if (mobile == null || mobile.trim().isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "Mobile number is required.");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            String cleanMobile = com.tts.enquiries.util.MobileUtils.normalizeMobile(mobile);
            SpinDraw draw = luckySpinService.performDraw(cleanMobile);
            
            // Map prize won to frontend wheel index:
            // 0: CCNA Free Course
            // 1: 10% Discount (First instance)
            // 2: RedHat Free Course
            // 3: Better Luck Next Time
            // 4: Data Analytics Course
            // 5: 10% Discount (Second instance)
            // 6: C & C++ Course with Free Course
            // 7: 40% Discount
            int wheelIndex = 3; // default Better Luck
            String prize = draw.getPrizeWon();
            if (prize.equalsIgnoreCase("CCNA Free Course")) wheelIndex = 0;
            else if (prize.equalsIgnoreCase("RedHat Free Course")) wheelIndex = 2;
            else if (prize.equalsIgnoreCase("Better Luck Next Time")) wheelIndex = 3;
            else if (prize.equalsIgnoreCase("Data Analytics Course")) wheelIndex = 4;
            else if (prize.equalsIgnoreCase("C & C++ Course with Free Course")) wheelIndex = 6;
            else if (prize.equalsIgnoreCase("40% Discount")) wheelIndex = 7;
            else if (prize.equalsIgnoreCase("10% Discount")) {
                // Alternating between index 1 and 5
                wheelIndex = Math.random() < 0.5 ? 1 : 5;
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("prizeName", draw.getPrizeWon());
            response.put("wheelIndex", wheelIndex);
            response.put("couponCode", draw.getCouponCode());
            response.put("status", draw.getStatus());
            
            if (draw.getExpiryDate() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy");
                response.put("expiryDate", draw.getExpiryDate().format(formatter));
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Draw failed for mobile {}: {}", mobile, e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Draw failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Admin endpoint: get all prizes config with live won/remaining counts.
     */
    @GetMapping("/prizes")
    public ResponseEntity<List<Map<String, Object>>> getPrizes() {
        return ResponseEntity.ok(luckySpinService.getPrizesReport());
    }

    /**
     * Admin endpoint: create a new prize.
     */
    @PostMapping("/prizes")
    public ResponseEntity<?> createPrize(@RequestBody Prize prize) {
        try {
            Prize saved = prizeService.createPrize(prize);
            return ResponseEntity.ok(Map.of("success", true, "prize", saved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating prize: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Failed to create prize: " + e.getMessage()));
        }
    }

    /**
     * Admin endpoint: update an existing prize.
     */
    @PutMapping("/prizes/{id}")
    public ResponseEntity<?> updatePrize(@PathVariable Long id, @RequestBody Prize prize) {
        try {
            Prize updated = prizeService.updatePrize(id, prize);
            return ResponseEntity.ok(Map.of("success", true, "prize", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating prize {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Failed to update prize: " + e.getMessage()));
        }
    }

    /**
     * Admin endpoint: toggle prize active/inactive status.
     */
    @PostMapping("/prizes/{id}/toggle-status")
    public ResponseEntity<?> togglePrizeStatus(@PathVariable Long id) {
        try {
            Prize updated = prizeService.togglePrizeStatus(id);
            return ResponseEntity.ok(Map.of("success", true, "prize", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error toggling prize {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Failed to toggle prize status."));
        }
    }

    /**
     * Admin endpoint: delete a prize.
     */
    @DeleteMapping("/prizes/{id}")
    public ResponseEntity<?> deletePrize(@PathVariable Long id) {
        try {
            prizeService.deletePrize(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Prize deleted successfully."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting prize {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Failed to delete prize."));
        }
    }

    /**
     * Admin endpoint: get all draws list.
     */
    @GetMapping("/draws")
    public ResponseEntity<List<SpinDraw>> getDraws() {
        return ResponseEntity.ok(spinDrawRepository.findAll());
    }

    /**
     * Admin endpoint: get dashboard stats.
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(luckySpinService.getLiveStats());
    }

    /**
     * Admin endpoint: verify a coupon code.
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyCoupon(@RequestBody Map<String, String> request) {
        String code = request.get("couponCode");
        if (code == null || code.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Coupon code is required."));
        }

        Optional<SpinDraw> drawOpt = luckySpinService.verifyCoupon(code);
        if (drawOpt.isPresent()) {
            SpinDraw draw = drawOpt.get();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("studentName", draw.getStudentName());
            response.put("studentMobile", draw.getStudentMobile());
            response.put("prizeWon", draw.getPrizeWon());
            response.put("couponCode", draw.getCouponCode());
            response.put("redeemed", draw.getRedeemed());
            response.put("status", draw.getStatus());
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM d, yyyy");
            if (draw.getSpinDate() != null) {
                response.put("spinDate", draw.getSpinDate().format(formatter));
            }
            if (draw.getExpiryDate() != null) {
                response.put("expiryDate", draw.getExpiryDate().format(formatter));
            }
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.ok(Map.of("success", false, "message", "Coupon code not found."));
        }
    }

    /**
     * Admin endpoint: redeem a coupon code.
     */
    @PostMapping("/redeem")
    public ResponseEntity<?> redeemCoupon(@RequestBody Map<String, String> request) {
        String code = request.get("couponCode");
        if (code == null || code.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Coupon code is required."));
        }

        try {
            SpinDraw draw = luckySpinService.redeemCoupon(code);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Coupon code successfully redeemed.");
            response.put("draw", draw);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * Admin endpoint: get campaign settings.
     */
    @GetMapping("/settings")
    public ResponseEntity<?> getSettings() {
        return ResponseEntity.ok(campaignSettingRepository.findById(1L).orElse(new CampaignSetting()));
    }

    /**
     * Admin endpoint: update campaign settings.
     */
    @PostMapping("/settings")
    public ResponseEntity<?> updateSettings(@RequestBody CampaignSetting settings) {
        settings.setId(1L);
        CampaignSetting saved = campaignSettingRepository.save(settings);
        return ResponseEntity.ok(Map.of("success", true, "settings", saved));
    }

    /**
     * Public endpoint: get public campaign settings status.
     */
    @GetMapping("/settings-public")
    public ResponseEntity<?> getSettingsPublic() {
        Optional<CampaignSetting> opt = campaignSettingRepository.findById(1L);
        if (opt.isPresent()) {
            CampaignSetting s = opt.get();
            Map<String, Object> map = new HashMap<>();
            map.put("offerStart", s.getOfferStart());
            map.put("offerEnd", s.getOfferEnd());
            map.put("isEmergencyLocked", s.getIsEmergencyLocked());
            map.put("emergencyReason", s.getEmergencyReason());
            map.put("gracePeriod", s.getGracePeriod());
            return ResponseEntity.ok(map);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("success", false, "message", "Settings not seeded."));
    }

    /**
     * Admin endpoint: get all participants.
     */
    @GetMapping("/participants")
    public ResponseEntity<?> getParticipants() {
        return ResponseEntity.ok(participantRepository.findAll());
    }

    /**
     * Admin endpoint: get paged, filtered and sorted participants.
     */
    @GetMapping(value = "/participants-paged", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Page<Participant>> getParticipantsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String course,
            @RequestParam(required = false) String prize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        log.info("REST request to get a page of participants with search: {}, course: {}, prize: {}, status: {}", search, course, prize, status);
        
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
                
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<Participant> participantPage = luckySpinService.searchParticipants(search, course, prize, status, dateFrom, dateTo, pageable);
        return ResponseEntity.ok(participantPage);
    }

    /**
     * Admin endpoint: export filtered participants to CSV.
     */
    @GetMapping(value = "/participants/export", produces = "text/csv")
    public void exportParticipantsToCsv(
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String course,
            @RequestParam(required = false) String prize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            HttpServletResponse response
    ) throws IOException {
        log.info("REST request to export filtered participants to CSV");

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        List<Participant> participants = luckySpinService.searchParticipantsList(search, course, prize, status, dateFrom, dateTo, sort);

        String filename = "TechnoKraft_Participants_" + LocalDate.now() + ".csv";
        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        try (PrintWriter writer = response.getWriter()) {
            writer.write('\ufeff'); // Write BOM for UTF-8 compatibility in Excel

            // Header row
            writer.println("Participant ID,Date & Time,Student Name,Mobile No.,Email,Course Selected,Prize Won,Coupon Code,Status");

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a");

            for (Participant p : participants) {
                String formattedDate = "";
                if (p.getCreatedAt() != null) {
                    formattedDate = p.getCreatedAt().format(dateFormatter);
                }

                writer.println(String.join(",",
                        "PAR-" + p.getId(),
                        escapeCsvField(formattedDate),
                        escapeCsvField(p.getName()),
                        escapeCsvField(p.getMobile()),
                        escapeCsvField(p.getEmail()),
                        escapeCsvField(p.getCourseSelected() != null ? p.getCourseSelected() : "-"),
                        escapeCsvField(p.getPrizeWon() != null ? p.getPrizeWon() : "-"),
                        escapeCsvField(p.getCouponCode() != null ? p.getCouponCode() : "-"),
                        escapeCsvField(p.getStatus())
                ));
            }
            writer.flush();
        }
    }

    /**
     * Admin endpoint: export filtered participants to Excel.
     */
    @GetMapping(value = "/participants/export/excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public void exportParticipantsToExcel(
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String course,
            @RequestParam(required = false) String prize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            HttpServletResponse response
    ) throws IOException {
        log.info("REST request to export filtered participants to Excel");

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        List<Participant> participants = luckySpinService.searchParticipantsList(search, course, prize, status, dateFrom, dateTo, sort);

        String filename = "TechnoKraft_Participants_" + LocalDate.now() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        try (org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Participants");

            // Header row style
            org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);

            // Header Row
            String[] headers = {"Participant ID", "Date & Time", "Student Name", "Mobile No.", "Email", "Course Selected", "Prize Won", "Coupon Code", "Status"};
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Normal cell style
            org.apache.poi.ss.usermodel.CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.LEFT);

            // Populate data rows
            int rowNum = 1;
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a");

            for (Participant p : participants) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue("PAR-" + p.getId());
                
                String dateStr = "";
                if (p.getCreatedAt() != null) {
                    dateStr = p.getCreatedAt().format(dateFormatter);
                }
                row.createCell(1).setCellValue(dateStr);
                row.createCell(2).setCellValue(p.getName());
                row.createCell(3).setCellValue(p.getMobile());
                row.createCell(4).setCellValue(p.getEmail());
                row.createCell(5).setCellValue(p.getCourseSelected() != null ? p.getCourseSelected() : "-");
                row.createCell(6).setCellValue(p.getPrizeWon() != null ? p.getPrizeWon() : "-");
                row.createCell(7).setCellValue(p.getCouponCode() != null ? p.getCouponCode() : "-");
                row.createCell(8).setCellValue(p.getStatus());

                for (int i = 0; i < headers.length; i++) {
                    row.getCell(i).setCellStyle(dataStyle);
                }
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(response.getOutputStream());
        }
    }

    private String escapeCsvField(String field) {
        if (field == null) {
            return "";
        }
        String clean = field.replace("\"", "\"\"");
        if (clean.contains(",") || clean.contains("\n") || clean.contains("\r") || clean.contains("\"")) {
            return "\"" + clean + "\"";
        }
        return clean;
    }

    /**
     * Admin endpoint: get all winners.
     */
    @GetMapping("/winners")
    public ResponseEntity<?> getWinners() {
        return ResponseEntity.ok(participantRepository.findAll().stream()
                .filter(p -> p.getStatus().equalsIgnoreCase("WON") || p.getStatus().equalsIgnoreCase("REDEEMED"))
                .toList());
    }
}
