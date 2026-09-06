package com.techpulse.taskmanager.exception;

/**
 * Thrown whenever a user tries to access or modify a resource that does not
 * belong to their own tenant (cross-tenant access attempt) or an action not
 * permitted for their role on a resource (e.g. USER updating someone else's task).
 */
public class TenantAccessDeniedException extends RuntimeException {
    public TenantAccessDeniedException(String message) {
        super(message);
    }
}
