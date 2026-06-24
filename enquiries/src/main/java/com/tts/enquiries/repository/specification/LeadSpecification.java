package com.tts.enquiries.repository.specification;

import com.tts.enquiries.entity.Lead;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class LeadSpecification {

    public static Specification<Lead> filterLeads(
            String search,
            String course,
            String status,
            LocalDate dateFrom,
            LocalDate dateTo
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Exclude soft-deleted leads
            predicates.add(cb.equal(root.get("isDeleted"), false));

            // Search filter (name, email, mobile, or course/service)
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), searchPattern),
                        cb.like(cb.lower(root.get("email")), searchPattern),
                        cb.like(cb.lower(root.get("mobile")), searchPattern),
                        cb.like(cb.lower(root.get("service")), searchPattern)
                ));
            }

            // Course / Service filter
            if (course != null && !course.trim().isEmpty() && !course.equalsIgnoreCase("all")) {
                predicates.add(cb.like(root.get("service"), "%" + course.trim() + "%"));
            }

            // Status filter
            if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("all")) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // Date Range: From Date
            if (dateFrom != null) {
                LocalDateTime startOfDay = dateFrom.atStartOfDay();
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startOfDay));
            }

            // Date Range: To Date
            if (dateTo != null) {
                LocalDateTime endOfDay = dateTo.atTime(LocalTime.MAX);
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endOfDay));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
