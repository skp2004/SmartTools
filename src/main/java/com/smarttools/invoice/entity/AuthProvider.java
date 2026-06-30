package com.smarttools.invoice.entity;

/**
 * Authentication provider for a user account.
 * LOCAL = email/password registration.
 * GOOGLE = Google OAuth2 SSO.
 */
public enum AuthProvider {
    LOCAL,
    GOOGLE
}
