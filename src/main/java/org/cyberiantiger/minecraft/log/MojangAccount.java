/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cyberiantiger.minecraft.log;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 *
 * @author antony
 */
public class MojangAccount {

    private final UUID id;
    private final Map <String,Long> lastSeen;

    public MojangAccount(UUID id, Map<String, Long> lastSeen) {
        this.id = id;
        this.lastSeen = lastSeen;
    }

    public UUID getId() {
        return id;
    }

    public Map<String, Long> getLastSeen() {
        return lastSeen;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Objects.hashCode(this.id);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MojangAccount other = (MojangAccount) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        return true;
    }
}
