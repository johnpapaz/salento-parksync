package com.parksync.auth;

public record LoginResponse(String accessToken, long expiresInSeconds) {}
