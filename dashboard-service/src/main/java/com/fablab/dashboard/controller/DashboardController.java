package com.fablab.dashboard.controller;

import com.fablab.dashboard.dto.AuthPrincipal;
import com.fablab.dashboard.dto.DashboardSummary;
import com.fablab.dashboard.service.DashboardService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agregação do App Shell: {@code GET /dashboard/summary}.
 */
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummary> summary(@AuthenticationPrincipal AuthPrincipal principal,
                                                    HttpServletRequest request) {
        String bearer = bearerFor(request);
        return ResponseEntity.ok(dashboardService.resumo(principal, bearer));
    }

    private String bearerFor(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        return header != null && header.startsWith("Bearer ") ? header.substring(7) : "";
    }
}