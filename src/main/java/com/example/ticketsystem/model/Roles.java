package com.example.ticketsystem.model;

import java.util.Set;

public final class Roles {

    public static final String USER = "user";
    public static final String ADMIN = "admin";
    public static final String SUPERADMIN = "superadmin";

    private static final Set<String> ALL = Set.of(USER, ADMIN, SUPERADMIN);

    private Roles() {}

    public static boolean isValid(String role) {
        return role != null && ALL.contains(role);
    }

    public static boolean isAdminOrAbove(String role) {
        return ADMIN.equals(role) || SUPERADMIN.equals(role);
    }
}
