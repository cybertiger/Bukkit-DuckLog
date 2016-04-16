/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cyberiantiger.minecraft.log.cmd;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.bukkit.command.CommandSender;
import org.cyberiantiger.minecraft.log.Main;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

/**
 *
 * @author antony
 */
public abstract class AbstractCommand {

    protected final Main main;
    protected static PeriodFormatter formatter;

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
    
    protected PeriodFormatter myFormatter() {
    	
    	if ( formatter == null ) {
            ResourceBundle b = ResourceBundle.getBundle("format_strings", Locale.getDefault());
            formatter =  new PeriodFormatterBuilder()
					        .appendYears()
					        .appendSuffix(b.getString("PeriodFormat.year"), b.getString("PeriodFormat.years"))
					        .appendSeparator(", ")
					        .appendMonths()
					        .appendSuffix(b.getString("PeriodFormat.month"), b.getString("PeriodFormat.months"))
					        .appendSeparator(", ")
					        .appendWeeks()
					        .appendSuffix(b.getString("PeriodFormat.week"), b.getString("PeriodFormat.weeks"))
					        .appendSeparator(", ")
					        .appendDays()
					        .appendSuffix(b.getString("PeriodFormat.day"))
					        .appendSeparator(" & ")
					        .appendHours()
					        .appendSuffix(b.getString("PeriodFormat.hour"))
					        .appendSeparator(" ")
					        .appendMinutes()
					        .appendSuffix(b.getString("PeriodFormat.minute"))
					        .toFormatter()
					        .withLocale(Locale.getDefault());
    	}
    	return formatter;
	}
}