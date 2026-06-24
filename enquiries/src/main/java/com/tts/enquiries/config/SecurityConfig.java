package com.tts.enquiries.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.name}")
    private String adminName;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        // Public lead creation (POST only)
                        .requestMatchers(HttpMethod.POST, "/api/leads").permitAll()

                        // Public Lucky Spin endpoints
                        .requestMatchers("/lucky-spin").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/lucky-spin/check-eligibility", "/api/lucky-spin/spin", "/api/lucky-spin/check-duplicate").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/lucky-spin/settings-public").permitAll()

                        // Public pages
                        .requestMatchers(
                                "/",
                                "/about-us",
                                "/careers",
                                "/faq",
                                "/hire-developers",
                                "/internships",
                                "/portfolio-details",
                                "/pricing",
                                "/privacy-policy",
                                "/projects",
                                "/service-details",
                                "/starter-page",
                                "/team",
                                "/terms-conditions",
                                "/auth/login",
                                "/login",
                                "/css/**",
                                "/js/**",
                                "/fonts/**",
                                "/assets/**",
                                "/images/**",
                                "/favicon.ico",
                                "/error",
                                "/error/**",
                                "/error_pages/**"
                        ).permitAll()

                        // Protected pages - require authentication
                        .requestMatchers("/dashboard", "/dashboard/**").authenticated()
                        
                        // Protected API - require ADMIN
                        .requestMatchers("/api/leads", "/api/leads/**").hasRole("ADMIN")
                        .requestMatchers("/api/lucky-spin/prizes", "/api/lucky-spin/prizes/**").hasRole("ADMIN")
                        .requestMatchers("/api/lucky-spin/draws", "/api/lucky-spin/stats").hasRole("ADMIN")
                        .requestMatchers("/api/lucky-spin/verify", "/api/lucky-spin/redeem").hasRole("ADMIN")
                        .requestMatchers("/api/lucky-spin/settings", "/api/lucky-spin/settings/**").hasRole("ADMIN")
                        .requestMatchers("/api/lucky-spin/participants", "/api/lucky-spin/participants-paged", "/api/lucky-spin/participants/**").hasRole("ADMIN")
                        .requestMatchers("/api/lucky-spin/winners", "/api/lucky-spin/winners-paged", "/api/lucky-spin/winners/**").hasRole("ADMIN")

                        // All other requests require authentication
                        .anyRequest().authenticated()
                )

                // Form login configuration
                .formLogin(form -> form
                        .loginPage("/auth/login")
                        .loginProcessingUrl("/auth/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/auth/login?error=true")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .permitAll()
                )

                // Logout configuration
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/auth/login?logout=true")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .addLogoutHandler(new SecurityContextLogoutHandler())
                        .permitAll()
                )

                // Session management
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .invalidSessionUrl("/auth/login?session=expired")
                        .maximumSessions(1)
                        .expiredUrl("/auth/login?session=expired")
                        .maxSessionsPreventsLogin(false)
                )

                // CSRF configuration - ignore all API endpoints
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/api/**")
                )

                // Exception handling
                .exceptionHandling(exception -> exception
                        .accessDeniedPage("/auth/login?error=access-denied")
                )

                // Security headers with comprehensive CSP for all CDNs and Google Analytics
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())  // Allow iframes from same origin
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives(
                                        "default-src 'self'; " +

                                                // Scripts: Allow CDNs, Google Analytics, and inline scripts
                                                "script-src 'self' 'unsafe-inline' 'unsafe-eval' " +
                                                "https://cdn.jsdelivr.net " +
                                                "https://cdnjs.cloudflare.com " +
                                                "https://code.jquery.com " +
                                                "https://www.googletagmanager.com " +
                                                "https://www.google-analytics.com " +
                                                "https://ssl.google-analytics.com; " +

                                                // Styles: Allow CDNs and inline styles
                                                "style-src 'self' 'unsafe-inline' " +
                                                "https://cdn.jsdelivr.net " +
                                                "https://cdnjs.cloudflare.com " +
                                                "https://fonts.googleapis.com; " +

                                                // Fonts: Allow CDNs and data URIs
                                                "font-src 'self' data: " +
                                                "https://fonts.gstatic.com " +
                                                "https://cdn.jsdelivr.net " +
                                                "https://cdnjs.cloudflare.com; " +

                                                // Images: Allow all sources including external and data URIs
                                                "img-src 'self' data: https: http: " +
                                                "https://www.googletagmanager.com " +
                                                "https://www.google-analytics.com " +
                                                "https://ssl.google-analytics.com " +
                                                "https://images.unsplash.com " +
                                                "https://ui-avatars.com; " +

                                                // Connect: Allow API calls to CDNs and Google Analytics
                                                "connect-src 'self' " +
                                                "https://cdn.jsdelivr.net " +
                                                "https://cdnjs.cloudflare.com " +
                                                "https://www.google-analytics.com " +
                                                "https://ssl.google-analytics.com " +
                                                "https://analytics.google.com " +
                                                "https://www.googletagmanager.com; " +

                                                // Frames: Allow Google Maps and same origin
                                                "frame-src 'self' " +
                                                "https://www.google.com " +
                                                "https://maps.google.com; " +

                                                // Media: Allow self
                                                "media-src 'self'; " +

                                                // Workers: Allow web workers from same origin and blob URLs
                                                "worker-src 'self' blob:; " +

                                                // Object: Deny plugins
                                                "object-src 'none'; " +

                                                // Base URI: Restrict to self
                                                "base-uri 'self'; " +

                                                // Form action: Allow self and production domain
                                                "form-action 'self' https://info.ttsnasik.com;"
                                )
                        )
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000)
                        )
                        .xssProtection(xss -> xss
                                .headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK)
                        )
                );

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails admin = User.builder()
                .username(adminUsername)
                .password(passwordEncoder().encode(adminPassword))
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(admin);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
