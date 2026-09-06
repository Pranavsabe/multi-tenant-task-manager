package com.techpulse.taskmanager.security;

/**
 * Holds the current request's tenantId and role, resolved exclusively from the JWT.
 * Backed by a ThreadLocal so it is available anywhere during the request lifecycle
 * (controllers, services, repositories) without passing it explicitly.
 *
 * IMPORTANT: clear() must always be called at the end of the request (done in
 * JwtAuthenticationFilter's finally block) to avoid leaking tenant context between
 * requests on the same worker thread (thread pool reuse).
 */
public final class TenantContext {

    private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();
    private static final ThreadLocal<Long> CURRENT_USER = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_ROLE = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantId(Long tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static Long getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void setUserId(Long userId) {
        CURRENT_USER.set(userId);
    }

    public static Long getUserId() {
        return CURRENT_USER.get();
    }

    public static void setRole(String role) {
        CURRENT_ROLE.set(role);
    }

    public static String getRole() {
        return CURRENT_ROLE.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
        CURRENT_USER.remove();
        CURRENT_ROLE.remove();
    }
}
