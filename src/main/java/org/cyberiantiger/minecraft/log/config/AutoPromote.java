/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cyberiantiger.minecraft.log.config;

import java.util.Collections;
import java.util.List;

/**
 *
 * @author antony
 */
public class AutoPromote {
    private List<String> hasGroup;
    private List<String> missingGroup;
    private List<String> removeGroup;
    private List<String> addGroup;
    private String message;
    private long after;

    public List<String> getHasGroup() {
        return hasGroup == null ? Collections.emptyList() : hasGroup;
    }

    public List<String> getMissingGroup() {
        return missingGroup == null ? Collections.emptyList() : missingGroup;
    }

    public List<String> getRemoveGroup() {
        return removeGroup == null ? Collections.emptyList() : removeGroup;
    }

    public List<String> getAddGroup() {
        return addGroup == null ? Collections.emptyList() : addGroup;
    }

    public long getAfter() {
        return after;
    }
}