/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cyberiantiger.minecraft.log;

/**
 *
 * @author antony
 */
public class LoginEvent {
    public enum Type {
        LOGIN,
        LOGOUT;
    }
    private final Type type;
    private final String server;
    private final String ip;
    private final long time;

    public LoginEvent(Type type, String server, String ip, long time) {
        this.type = type;
        this.server = server;
        this.ip = ip;
        this.time = time;
    }

    public Type getType() {
        return type;
    }

    public String getServer() {
        return server;
    }

    public String getIp() {
        return ip;
    }

    public long getTime() {
        return time;
    }
}
