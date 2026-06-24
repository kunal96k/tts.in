package com.tts.enquiries.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeadDTO {

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s]+$", message = "Name should contain only letters and spaces")
    private String name;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Please enter a valid 10-digit Indian mobile number")
    private String mobile;

    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email address")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;

    @Size(max = 1000, message = "Courses selection must not exceed 1000 characters")
    private String service;

    @Size(max = 100, message = "Qualification must not exceed 100 characters")
    private String qualification;

    @Size(max = 100, message = "Preferred batch must not exceed 100 characters")
    private String batch;

    @Size(max = 100, message = "Referral source must not exceed 100 characters")
    private String source;

    @Size(max = 50, message = "Status must not exceed 50 characters")
    private String status;

    @Size(max = 1000, message = "Message must not exceed 1000 characters")
    private String message;
}
