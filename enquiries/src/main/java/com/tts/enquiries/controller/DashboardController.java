package com.tts.enquiries.controller;

import com.tts.enquiries.entity.Lead;
import com.tts.enquiries.service.LeadService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final LeadService leadService;

    @GetMapping
    public String dashboard(Model model, Authentication authentication) {
        String username = authentication.getName();
        model.addAttribute("username", username);

        try {
            List<Lead> leads = leadService.getAllActiveLeads();
            model.addAttribute("leads", leads);
            model.addAttribute("totalLeads", leads.size());

            // Calculate today's leads
            long todayLeads = leads.stream()
                    .filter(lead -> lead.getCreatedAt() != null && lead.getCreatedAt().toLocalDate().equals(LocalDate.now()))
                    .count();
            model.addAttribute("todayLeads", todayLeads);

        } catch (Exception e) {
            model.addAttribute("leads", List.of());
            model.addAttribute("totalLeads", 0);
            model.addAttribute("todayLeads", 0);
        }

        return "dashboard";
    }

    @GetMapping("/delete/{id}")
    public String softDeleteLead(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            leadService.softDeleteLead(id);
            redirectAttributes.addFlashAttribute("success", "Lead deleted successfully!");
            redirectAttributes.addFlashAttribute("deleted", true);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete lead: " + e.getMessage());
        }
        return "redirect:/dashboard";
    }

    @GetMapping("/offers")
    public String offers(Model model, Authentication authentication) {
        String username = authentication.getName();
        model.addAttribute("username", username);
        return "offers";
    }
}
