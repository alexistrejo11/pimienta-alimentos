package io.github.alexistrejo11.pimienta.module.account.auth.core.domain.entity;

public record UserManagerDashboard(
    int totalActiveEmployees,
    int totalActiveHeadquarters,
    int totalOpenTasks,
    int totalPendingTasks) {}
