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
public class LastSeen {
    private final Action type;
    private final String server;
    private final String ip;
    private final long time;
    private final String world;
    private final int x;
    private final int y;
    private final int z;

    public LastSeen(Action type, String server, String ip, long time, String world, int x, int y, int z) {
        this.type = type;
        this.server = server;
        this.ip = ip;
        this.time = time;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Action getType() {
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

    public boolean hasLocation() {
        return world != null;
    }

    public String getWorld() {
        return world;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }
}
