/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cyberiantiger.minecraft.log.cmd;

/**
 *
 * @author antony
 */
public class PermissionException extends CommandException {
    private final String permission;

    public PermissionException(String permission) {
        this.permission = permission;
    }

    public String getPermission() {
        return permission;
    }
}
