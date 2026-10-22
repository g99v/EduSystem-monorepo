package com.springboot.EduSystem.auth;

public record CsrfResponse(String token, String headerName) {
}
