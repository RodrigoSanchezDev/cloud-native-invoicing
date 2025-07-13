package com.sanchezdev.fileservice.config;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class InternalIpBypassFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, 
                                    @NonNull HttpServletResponse response, 
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        
        String remoteAddr = getClientIpAddress(request);
        
        // Allow access from Docker internal networks and localhost
        if (isInternalIp(remoteAddr)) {
            // Set a custom header to indicate this is an internal request
            request.setAttribute("INTERNAL_REQUEST", true);
        }
        
        filterChain.doFilter(request, response);
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    private boolean isInternalIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        
        // Docker internal networks typically use these ranges
        // 172.16.0.0 - 172.31.255.255 (Docker default bridge networks)
        // 10.0.0.0 - 10.255.255.255 (Private networks)
        // 192.168.0.0 - 192.168.255.255 (Private networks)
        // 127.0.0.1 (localhost)
        
        return ip.equals("127.0.0.1") ||
               ip.equals("localhost") ||
               ip.equals("0:0:0:0:0:0:0:1") || // IPv6 localhost
               ip.startsWith("172.") ||
               ip.startsWith("10.") ||
               ip.startsWith("192.168.");
    }
}
