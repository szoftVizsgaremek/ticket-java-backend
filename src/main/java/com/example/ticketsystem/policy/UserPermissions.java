package com.example.ticketsystem.policy;

import com.example.ticketsystem.model.Roles;
import com.example.ticketsystem.model.User;

public final class UserPermissions {
    
    private UserPermissions() {}

    public static boolean canManage(User caller, User target) {
        String role = caller.getRole();
        if(Roles.SUPERADMIN.equals(role)) {
            return true;
        }
        if (Roles.ADMIN.equals(role)) {
            boolean isSelf = caller.getId().equals(target.getId());
            return !isSelf && Roles.USER.equals(target.getRole());
        }
        return false;
    }

}
