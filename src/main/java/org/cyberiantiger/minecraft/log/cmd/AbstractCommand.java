/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cyberiantiger.minecraft.log.cmd;

import java.util.List;
import org.bukkit.command.CommandSender;
import org.cyberiantiger.minecraft.log.Main;
import org.joda.time.Period;

/**
 *
 * @author antony
 */
public abstract class AbstractCommand {

    protected final Main main;

    public AbstractCommand(Main main) {
        this.main = main;
    }

    public abstract void execute(CommandSender sender, String label, String[] args) throws CommandException;

    public abstract List<String> tabComplete(String label, String[] args);

    private static final long SECOND = 1000;
    private static final long MINUTE = 60 * SECOND;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;

    public static Period trimPeriod(long period) {
        period -= period % SECOND; // Trim milliseconds.
        return new Period(period);
    }
}