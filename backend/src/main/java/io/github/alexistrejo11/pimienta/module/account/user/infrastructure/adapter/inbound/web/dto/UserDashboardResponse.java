package io.github.alexistrejo11.pimienta.module.account.user.infrastructure.adapter.inbound.web.dto;

public record UserDashboardResponse(
    int totalActiveEmployees,
    int totalActiveHeadquarters,
    int totalOpenTasks,
    int totalPendingTasks) {}
