package org.joda.time.chrono;

import org.joda.time.Chronology;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeFieldType;
import org.joda.time.DateTimeZone;
import org.joda.time.DurationFieldType;
import org.joda.time.field.PreciseDateTimeField;
import org.joda.time.field.PreciseDurationField;


public class FixedMonthChronology extends BasicFixedMonthChronology {

	private static final long serialVersionUID = 3143606329200719322L;
	
	@SuppressWarnings( "hiding" )
	static final int MONTH_LENGTH = 31;
	
	private static final FixedMonthChronology INSTANCE;
	static {
		INSTANCE = new FixedMonthChronology( null, null, 1 );
	}
	
	private FixedMonthChronology( Chronology base, Object param, int minDaysInFirstWeek ) {
		super( base, param, minDaysInFirstWeek );
	}
	
	public static Chronology getInstance() { return INSTANCE; }
	
    @Override
	protected void assemble(Fields fields) {

        fields.seconds = new PreciseDurationField
                (DurationFieldType.seconds(), DateTimeConstants.MILLIS_PER_SECOND);
        fields.minutes = new PreciseDurationField
                (DurationFieldType.minutes(), DateTimeConstants.MILLIS_PER_MINUTE);
        fields.hours = new PreciseDurationField
                (DurationFieldType.hours(), DateTimeConstants.MILLIS_PER_HOUR);
        fields.days = new PreciseDurationField
                (DurationFieldType.days(), DateTimeConstants.MILLIS_PER_DAY);
        fields.months = new PreciseDurationField
                (DurationFieldType.months(), MILLIS_PER_MONTH);
        fields.years = new PreciseDurationField
                (DurationFieldType.years(), MILLIS_PER_YEAR);
        
        fields.secondOfMinute = new PreciseDateTimeField
                (DateTimeFieldType.secondOfMinute(), fields.seconds, fields.minutes);
        fields.minuteOfHour = new PreciseDateTimeField
                (DateTimeFieldType.minuteOfHour(), fields.minutes, fields.hours);
        fields.hourOfDay = new PreciseDateTimeField
                (DateTimeFieldType.hourOfDay(), fields.hours, fields.days);
        fields.dayOfMonth = new PreciseDateTimeField
                (DateTimeFieldType.dayOfMonth(), fields.days, fields.months);
        fields.monthOfYear = new PreciseDateTimeField
        		(DateTimeFieldType.monthOfYear(), fields.months, fields.years);
        
    }   
	@Override
	long calculateFirstDayOfYearMillis( int year ) {
		return (long) (year * 365.25 * DateTimeConstants.MILLIS_PER_DAY);
	}

	@Override
	int getMinYear() { return -292275054; }

	@Override
	int getMaxYear() { return 292278993; }

	@Override
	long getApproxMillisAtEpochDividedByTwo() {	return 0; }

	@Override
	public Chronology withUTC() { return INSTANCE; }

	@Override
	public Chronology withZone( DateTimeZone zone ) { return INSTANCE; }
}
