package com.company.amsbackend.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class MdcEmployeeIdFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            String employeeId = "unknown";
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                // Try to get employeeId from principal or request attribute
                Object principal = auth.getPrincipal();
                if (principal instanceof org.springframework.security.core.userdetails.User userDetails) {
                    // You can fetch employeeId from DB if needed, or from JWT claims if available
                    employeeId = userDetails.getUsername(); // If username is email, map to employeeId
                    // Optionally, look up employeeId by email using your EmployeeService
                }
            }
            MDC.put("employeeId", employeeId);
            chain.doFilter(request, response);
        } finally {
            MDC.remove("employeeId");
        }
    }
}