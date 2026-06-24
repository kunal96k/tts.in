package com.tts.enquiries.controller;

import com.tts.enquiries.dto.LeadDTO;
import com.tts.enquiries.entity.Lead;
import com.tts.enquiries.service.LeadService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leads")
@RequiredArgsConstructor
@Slf4j
public class LeadController {

    private final LeadService leadService;

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> createLead(@Valid @RequestBody LeadDTO leadDTO, BindingResult bindingResult) {
        log.info("Received lead creation request for email: {}", leadDTO.getEmail());

        // Handle validation errors
        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error ->
                    errors.put(error.getField(), error.getDefaultMessage())
            );
            log.warn("Validation errors in lead creation: {}", errors);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("errors", errors);
            response.put("message", "Please fix the validation errors");

            return ResponseEntity.badRequest().body(response);
        }

        try {
            Lead lead = leadService.createLead(leadDTO);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Thank you! We'll contact you soon.");
            response.put("leadId", lead != null ? lead.getId() : 0L);

            log.info("Lead created successfully with ID: {}", lead != null ? lead.getId() : null);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.warn("Validation/Duplicate check failed: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            log.error("Error creating lead: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to submit your request. Please try again later.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<org.springframework.data.domain.Page<Lead>> getLeads(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String course,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate dateFrom,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate dateTo
    ) {
        log.info("REST request to get a page of leads with search: {}, course: {}, status: {}", search, course, status);

        org.springframework.data.domain.Sort sort = sortDir.equalsIgnoreCase("asc")
                ? org.springframework.data.domain.Sort.by(sortBy).ascending()
                : org.springframework.data.domain.Sort.by(sortBy).descending();

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, sort);

        org.springframework.data.domain.Page<Lead> leadsPage = leadService.searchLeads(search, course, status, dateFrom, dateTo, pageable);
        return ResponseEntity.ok(leadsPage);
    }

    @GetMapping(value = "/export", produces = "text/csv")
    public void exportLeadsToCsv(
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String course,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate dateFrom,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate dateTo,
            jakarta.servlet.http.HttpServletResponse response
    ) throws java.io.IOException {
        log.info("REST request to export filtered leads to CSV");

        org.springframework.data.domain.Sort sort = sortDir.equalsIgnoreCase("asc")
                ? org.springframework.data.domain.Sort.by(sortBy).ascending()
                : org.springframework.data.domain.Sort.by(sortBy).descending();

        List<Lead> leads = leadService.searchLeadsList(search, course, status, dateFrom, dateTo, sort);

        // Setup response headers
        String filename = "TechnoKraft_Enquiries_" + java.time.LocalDate.now() + ".csv";
        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");
        response.setHeader(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        // Write CSV content
        try (java.io.PrintWriter writer = response.getWriter()) {
            // Write BOM for UTF-8 compatibility in Excel
            writer.write('\ufeff');

            // Header row
            writer.println("Enquiry No.,Student Name,Mobile No.,Course,Enquiry Source,Enquiry Date,Assign To,Enquiry Status");

            java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a");

            for (Lead lead : leads) {
                String formattedDate = "";
                if (lead.getCreatedAt() != null) {
                    formattedDate = lead.getCreatedAt().format(dateFormatter);
                }

                writer.println(String.join(",",
                        String.valueOf(lead.getId()),
                        escapeCsvField(lead.getName()),
                        escapeCsvField(lead.getMobile()),
                        escapeCsvField(lead.getService()),
                        escapeCsvField(lead.getSource() != null ? lead.getSource() : "Direct"),
                        escapeCsvField(formattedDate),
                        escapeCsvField("-- system"),
                        escapeCsvField(lead.getStatus())
                ));
            }
            writer.flush();
        }
    }

    @GetMapping(value = "/export/excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public void exportLeadsToExcel(
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String course,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate dateFrom,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate dateTo,
            jakarta.servlet.http.HttpServletResponse response
    ) throws java.io.IOException {
        log.info("REST request to export filtered leads to Excel");

        org.springframework.data.domain.Sort sort = sortDir.equalsIgnoreCase("asc")
                ? org.springframework.data.domain.Sort.by(sortBy).ascending()
                : org.springframework.data.domain.Sort.by(sortBy).descending();

        List<Lead> leads = leadService.searchLeadsList(search, course, status, dateFrom, dateTo, sort);

        // Setup response headers
        String filename = "TechnoKraft_Enquiries_" + java.time.LocalDate.now() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

        try (org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Enquiries");

            // Create header row style
            org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);

            // Header Row
            String[] headers = {"Enquiry No.", "Student Name", "Mobile No.", "Course", "Enquiry Source", "Enquiry Date", "Assign To", "Enquiry Status"};
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Create normal cell styles
            org.apache.poi.ss.usermodel.CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.LEFT);

            // Populate data rows
            int rowNum = 1;
            java.time.format.DateTimeFormatter dateFormatter = java.time.format.DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a");

            for (Lead lead : leads) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(rowNum++);

                row.createCell(0).setCellValue(lead.getId());
                row.createCell(1).setCellValue(lead.getName());
                row.createCell(2).setCellValue(lead.getMobile());
                row.createCell(3).setCellValue(lead.getService());
                row.createCell(4).setCellValue(lead.getSource() != null ? lead.getSource() : "Direct");

                String dateStr = "";
                if (lead.getCreatedAt() != null) {
                    dateStr = lead.getCreatedAt().format(dateFormatter);
                }
                row.createCell(5).setCellValue(dateStr);
                
                row.createCell(6).setCellValue("-- system");
                row.createCell(7).setCellValue(lead.getStatus());

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

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Lead> getLeadById(@PathVariable Long id) {
        log.info("Fetching lead with ID: {}", id);
        Lead lead = leadService.getLeadById(id);
        return ResponseEntity.ok(lead);
    }

    @PutMapping(
            value = "/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> updateLead(@PathVariable Long id,
                                         @Valid @RequestBody LeadDTO leadDTO,
                                         BindingResult bindingResult) {
        log.info("Updating lead with ID: {}", id);

        if (bindingResult.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            bindingResult.getFieldErrors().forEach(error ->
                    errors.put(error.getField(), error.getDefaultMessage())
            );

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("errors", errors);
            return ResponseEntity.badRequest().body(response);
        }

        try {
            Lead updatedLead = leadService.updateLead(id, leadDTO);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Lead updated successfully");
            response.put("lead", updatedLead);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating lead: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to update lead");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteLead(@PathVariable Long id) {
        log.info("Soft deleting lead with ID: {}", id);
        try {
            leadService.softDeleteLead(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Lead deleted successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error deleting lead: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to delete lead");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping(value = "/stats", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> getStats() {
        log.info("Fetching dashboard statistics");
        List<Lead> leads = leadService.getAllActiveLeads();
        long total = leads.size();
        
        java.time.LocalDate today = java.time.LocalDate.now();
        long todayCount = leads.stream()
                .filter(l -> l.getCreatedAt() != null && l.getCreatedAt().toLocalDate().equals(today))
                .count();
        
        long enrolled = leads.stream()
                .filter(l -> "Enrolled".equalsIgnoreCase(l.getStatus()))
                .count();
        
        long pending = leads.stream()
                .filter(l -> "New".equalsIgnoreCase(l.getStatus()) || "Contacted".equalsIgnoreCase(l.getStatus()))
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("today", todayCount);
        stats.put("enrolled", enrolled);
        stats.put("pending", pending);
        
        return ResponseEntity.ok(stats);
    }

    @PatchMapping(value = "/{id}/status", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestParam String status) {
        log.info("Patching status of lead ID: {} to {}", id, status);
        try {
            Lead lead = leadService.getLeadById(id);
            
            // We use the service update to modify status
            Lead updatedLead = leadService.updateLead(id, new LeadDTO(
                    lead.getName(),
                    lead.getMobile(),
                    lead.getEmail(),
                    lead.getService(),
                    lead.getQualification(),
                    lead.getBatch(),
                    lead.getSource(),
                    status,
                    lead.getMessage()
            ));
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Status updated successfully");
            response.put("lead", updatedLead);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error updating status for lead ID {}: {}", id, e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to update status");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
