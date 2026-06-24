package com.tts.enquiries.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class PageController {

    // Home page
    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/about-us")
    public String aboutUs() {
        return "tts/about-us";
    }

    @GetMapping("/careers")
    public String careers() {
        return "tts/careers";
    }

    @GetMapping("/faq")
    public String faq() {
        return "tts/faq";
    }

    @GetMapping("/hire-developers")
    public String hireDevelopers() {
        return "tts/hire-developers";
    }

    @GetMapping("/internships")
    public String internships() {
        return "tts/internships";
    }

    @GetMapping("/portfolio-details")
    public String portfolioDetails() {
        return "tts/portfolio-details";
    }

    @GetMapping("/pricing")
    public String pricing() {
        return "tts/pricing";
    }

    @GetMapping("/privacy-policy")
    public String privacyPolicy() {
        return "tts/privacy-policy";
    }

    @GetMapping("/projects")
    public String projects() {
        return "tts/projects";
    }

    @GetMapping("/service-details")
    public String serviceDetails() {
        return "tts/service-details";
    }

    @GetMapping("/starter-page")
    public String starterPage() {
        return "tts/starter-page";
    }

    @GetMapping("/team")
    public String team() {
        return "tts/team";
    }

    @GetMapping("/terms-conditions")
    public String termsConditions() {
        return "tts/terms-conditions";
    }

    // Public Spin & Win promotional landing page
    @GetMapping("/lucky-spin")
    public String luckySpin() {
        return "lucky-spin";
    }

    // Error pages preview routes
    @GetMapping("/error/400")
    public String error400() {
        return "error/400";
    }

    @GetMapping("/error/404")
    public String error404() {
        return "error/404";
    }

    @GetMapping("/error/500")
    public String error500() {
        return "error/500";
    }
}
