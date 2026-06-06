package com.trading.platform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private final String type = "Bearer";
    private String username;
    private String role;
}
