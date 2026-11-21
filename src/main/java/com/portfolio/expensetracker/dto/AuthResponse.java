package com.portfolio.expensetracker.dto;

public record AuthResponse(String token, String tokenType, String email, String fullName) {

    public static AuthResponse bearer(String token, String email, String fullName) {
        return new AuthResponse(token, "Bearer", email, fullName);
    }
}
