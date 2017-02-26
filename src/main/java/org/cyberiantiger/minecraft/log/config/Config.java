/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cyberiantiger.minecraft.log.config;

import java.util.Collections;
import java.util.Map;

/**
 *
 * @author antony
 */
public class Config {
    private boolean asyncPromote;
    private String dbUrl;
    private String dbUser;
    private String dbPassword;
    private Map<String,AutoPromote> autopromote;

    public String getDbUrl() {
        return dbUrl;
    }

    public String getDbUser() {
        return dbUser;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public boolean getAsyncPromote() {
        return asyncPromote;
    }

    public Map<String, AutoPromote> getAutopromote() {
        return autopromote  == null ? Collections.emptyMap() : autopromote;
    }
}
