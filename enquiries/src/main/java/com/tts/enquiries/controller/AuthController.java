package com.tts.enquiries.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    @GetMapping("/auth/login")
    public String login() {
        return "auth/login";
    }

    @GetMapping("/login")
    public String redirectToLogin() {
        return "redirect:/auth/login";
    }
}
