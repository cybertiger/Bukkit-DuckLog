/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cyberiantiger.minecraft.log.cmd;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.cyberiantiger.minecraft.log.Main;

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

    // constants that provide the number of millis in the named period (assuming a constant length month)
    private static final long SECOND = 1000;
    private static final long MINUTE = 60 * SECOND;
    private static final long HOUR = 60 * MINUTE;
    private static final long DAY = 24 * HOUR;
    private static final long YEAR = (long) (365.25 * DAY);
    private static final long MONTH = YEAR / 12;

    protected String getFormattedTime(long millis) {
    	
    	/* This Object[] will later be sent to main.getMessage(), before it is passed as the argument, the
    	 *  values will be replaced with the numbers of years, months, days, hours, minutes, and seconds. */
    	Object[] segmentedDateTime = { 0, 0, 0, 0, 0, 0 };
    	
    	if ( millis > YEAR ) {
    		segmentedDateTime[0] = (int) ( millis / YEAR );
    		millis %= YEAR;
    	}
    	if ( millis > MONTH ) {
    		segmentedDateTime[1] = (int) ( millis / MONTH );
    		millis %= MONTH;
    	}
    	if ( millis > DAY ) {
    		segmentedDateTime[2] = (int) ( millis / DAY );
    		millis %= DAY;
    	}
    	if ( millis > HOUR ) {
    		segmentedDateTime[3] = (int) ( millis / HOUR );
    		millis %= HOUR;
    	}
    	if ( millis > MINUTE ) {
    		segmentedDateTime[4] = (int) ( millis / MINUTE );
    		millis %= MINUTE;
    	}
    	segmentedDateTime[5] = (int) ( millis / SECOND );
		
    	/* This loop will exit on the first non-zero value, so selecting the largest time unit to display
    	*  from the options in the switch statement that follows. */ 
    	int i = 0;
    	for ( ; i <= 5; i++ ) {
    		if ( (int) segmentedDateTime[i] > 0 ) {
    			break;
    		}
    	}
    	switch(i) {
    		case 1:
    			return main.getMessage( "duration.format.months", segmentedDateTime );
    		case 2:
    			return main.getMessage( "duration.format.days", segmentedDateTime );
    		case 3:
    			return main.getMessage( "duration.format.hours", segmentedDateTime );
    		case 4:
    			return main.getMessage( "duration.format.minutes", segmentedDateTime );
    		case 5:
    			return main.getMessage( "duration.format.seconds", segmentedDateTime );
    		case 0:
    		default:
    			return main.getMessage( "duration.format.years", segmentedDateTime );
    	}
    }
}