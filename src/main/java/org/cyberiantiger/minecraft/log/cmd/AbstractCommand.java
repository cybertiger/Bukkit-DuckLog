/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cyberiantiger.minecraft.log.cmd;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import org.bukkit.command.CommandSender;
import org.cyberiantiger.minecraft.log.Main;
import org.joda.time.Period;
import org.joda.time.PeriodType;
import org.joda.time.chrono.FixedMonthChronology;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

/**
 *
 * @author antony
 */
public abstract class AbstractCommand {

    protected final Main main;
    private static PeriodFormatter formatter;
    protected static PeriodType type = PeriodType.yearMonthDayTime().withMillisRemoved();

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
		return new Period( period, type, FixedMonthChronology.getInstance());
    }
    
    protected PeriodFormatter myFormatter() {
    	
    	if ( formatter == null ) {
    		Properties p = new Properties();
    		try {
				p.load(main.openDataFile(Main.FORMATS));
			} catch ( IOException e ) {
				main.getLogger().log(Level.WARNING, "Could not load " + Main.FORMATS, e);
			}
//            ResourceBundle b = ResourceBundle.getBundle("format_strings", Locale.getDefault());
    		if (!p.isEmpty()) {
    			formatter =  new PeriodFormatterBuilder()
					        .appendYears()
					        .appendSuffix(p.getProperty("PeriodFormat.year"), p.getProperty("PeriodFormat.years"))
					        .appendSeparator(", ")
					        .appendMonths()
					        .appendSuffix(p.getProperty("PeriodFormat.month"), p.getProperty("PeriodFormat.months"))
					        .appendSeparator(", ")
					        .appendDays()
					        .appendSuffix(p.getProperty("PeriodFormat.day"), p.getProperty("PeriodFormat.days"))
					        .appendSeparator(" & ")
					        .appendHours()
					        .appendSuffix(p.getProperty("PeriodFormat.hour"))
					        .appendSeparator(" ")
					        .appendMinutes()
					        .appendSuffix(p.getProperty("PeriodFormat.minute"))
					        .appendSeparator(" ")
					        .appendSeconds()
					        .appendSuffix(p.getProperty("PeriodFormat.second"))
					        .toFormatter();
			//		        .withLocale(Locale.getDefault());
    		}
    	}
    	return formatter;
	}
}