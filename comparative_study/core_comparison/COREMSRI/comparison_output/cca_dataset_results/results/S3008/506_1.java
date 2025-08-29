package org.exparity.hamcrest.date.core;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.Year;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAccessor;
import java.util.Date;

import org.exparity.hamcrest.date.core.types.DayOfMonth;
import org.exparity.hamcrest.date.core.types.Hour;
import org.exparity.hamcrest.date.core.types.Millisecond;
import org.exparity.hamcrest.date.core.types.Minute;
import org.exparity.hamcrest.date.core.types.Second;

/**
 * Static repository of {@link TemporalConverter} instances which convert a temporal type from another temporal type
 * e.g. given a {@link LocalDate} returns the hour, or given a {@link java.sql.Date} returns a {@link LocalDate}.
 * No-operation conversions e.g. LocalDate to LocalDate are present to keep a consistent usage pattern in the
 * {@link TemporalMatcher} implementations.
 * </p>
 * The temporal converters generally "down-cast" a temporal type to another and are used to support testing the actual
 * type against the reference type where the reference type is an equal or less accurate temporal unit e.g. comparing if
 * a LocalDateTime is on a given year. There should not be "up-casting" converters because these would be making up
 * absent information e.g. converting a {@link LocalDate} to a {@link LocalDateTime}
 */
public class TemporalConverters {
    
    private TemporalConverters() {};

	public static final String UNSUPPORTED_SQL_DATE_UNIT = "java.sql.Date does not support time-based comparisons. Prefer SqlDateMatchers for java.sql.Date appropriate matchers";
	
	/**
	 * SQL Date Converters
	 */
	public static TemporalConverter<java.sql.Date, LocalDate> sqlDateAsLocalDate = (date, zone) -> date.toLocalDate();
	public static TemporalConverter<java.sql.Date, java.sql.Date> sqlDateAsSqlDate = (date, zone) -> date;
	public static TemporalConverter<java.sql.Date, Year> sqlDateAsYear = (date, zone) -> Year.from(sqlDateAsLocalDate.apply(date, zone));
	public static TemporalConverter<java.sql.Date, Month> sqlDateAsMonth = (date, zone) -> sqlDateAsLocalDate.apply(date, zone).getMonth();
	public static TemporalConverter<java.sql.Date, DayOfMonth> sqlDateAsDayOfMonth = (date, zone) -> DayOfMonth.from(sqlDateAsLocalDate.apply(date, zone));
	public static TemporalConverter<java.sql.Date, DayOfWeek> sqlDateAsDayOfWeek = (date, zone) -> sqlDateAsLocalDate.apply(date, zone).getDayOfWeek();

	/**
	 * Java Date Converters
	 */

	public static TemporalConverter<Date, Instant> javaDateAsInstant = (date, zone) -> {
		if (date instanceof java.sql.Date) {
			throw new TemporalConversionException(UNSUPPORTED_SQL_DATE_UNIT);
		} else {
			return date.toInstant();
		}
	};

	public static TemporalConverter<Date, ZonedDateTime> javaDateAsZonedDateTime = (date, zone) -> javaDateAsInstant.apply(date, zone).atZone(zone.orElse(ZoneId.systemDefault()));
	public static TemporalConverter<Date, LocalDateTime> javaDateAsLocalDateTime = (date, zone) -> javaDateAsZonedDateTime.apply(date, zone).toLocalDateTime();

	public static TemporalConverter<Date, LocalDate> javaDateAsLocalDate = (date, zone) -> {
		if (date instanceof java.sql.Date) {
			return ((java.sql.Date) date).toLocalDate();
		} else {
			return javaDateAsZonedDateTime.apply(date, zone).toLocalDate();
		}
	};
	
	public static TemporalConverter<Date, TemporalAccessor> javaDateAsTemporal = (date, zone) -> {
		if (date instanceof java.sql.Date) {
			return javaDateAsLocalDate.apply(date, zone);
		} else {
			return javaDateAsZonedDateTime.apply(date, zone);
		}
	};
	
    public static TemporalConverter<Date, java.sql.Date> javaDateAsSqlDate = (date, zone) -> {
        if (date instanceof java.sql.Date) {
            return (java.sql.Date) date;
        } else {
            return new java.sql.Date(date.getTime());
        }
    };

	public static TemporalConverter<Date, Date> javaDateAsJavaDate = (date, zone) -> date;
	public static TemporalConverter<Date, Year> javaDateAsYear = (date, zone) -> Year.from(javaDateAsLocalDate.apply(date, zone));
	public static TemporalConverter<Date, Month> javaDateAsMonth = (date, zone) -> javaDateAsLocalDate.apply(date, zone).getMonth();
	public static TemporalConverter<Date, DayOfMonth> javaDateAsDayOfMonth = (date, zone) -> DayOfMonth.from(javaDateAsLocalDate.apply(date, zone));
	public static TemporalConverter<Date, DayOfWeek> javaDateAsDayOfWeek = (date, zone) -> javaDateAsLocalDate.apply(date, zone).getDayOfWeek();
	public static TemporalConverter<Date, Hour> javaDateAsHour = (date, zone) -> Hour.from(javaDateAsLocalDateTime.apply(date, zone));
	public static TemporalConverter<Date, Minute> javaDateAsMinute = (date, zone) -> Minute.from(javaDateAsLocalDateTime.apply(date, zone));
	public static TemporalConverter<Date, Second> javaDateAsSecond = (date, zone) -> Second.from(javaDateAsLocalDateTime.apply(date, zone));
	public static TemporalConverter<Date, Millisecond> javaDateAsMillisecond = (date, zone) -> Millisecond.from(javaDateAsInstant.apply(date, zone));

	/**
	 * LocalTime Converters
	 */
	public static TemporalConverter<LocalTime, LocalTime> localTimeAsLocalTime = (time, zone) -> time;
	public static TemporalConverter<LocalTime, Hour> localTimeAsHour = (time, zone) -> Hour.from(time);
	public static TemporalConverter<LocalTime, Minute> localTimeAsMinute = (time, zone) -> Minute.from(time);
	public static TemporalConverter<LocalTime, Second> localTimeAsSecond = (time, zone) -> Second.from(time);

	/**
	 * LocalDate Converters
	 */
	public static TemporalConverter<LocalDate, LocalDate> localDateAsLocalDate = (date, zone) -> date;
	public static TemporalConverter<LocalDate, Year> localDateAsYear = (date, zone) -> Year.from(date);
	public static TemporalConverter<LocalDate, Month> localDateAsMonth = (date, zone) -> date.getMonth();
	public static TemporalConverter<LocalDate, DayOfMonth> localDateAsDayOfMonth = (date, zone) -> DayOfMonth.from(date);
	public static TemporalConverter<LocalDate, DayOfWeek> localDateAsDayOfWeek = (date, zone) -> date.getDayOfWeek();
	
	/**
	 * LocalDateTime Converters
	 */
	public static TemporalConverter<LocalDateTime, LocalDateTime> localDateTimeAsLocalDateTime = (date, zone) -> date;
	public static TemporalConverter<LocalDateTime, LocalDate> localDateTimeAsLocalDate = (date, zone) -> date.toLocalDate();
	public static TemporalConverter<LocalDateTime, Year> localDateTimeAsYear = (date, zone) -> Year.from(date);
	public static TemporalConverter<LocalDateTime, Month> localDateTimeAsMonth = (date, zone) -> date.getMonth();
	public static TemporalConverter<LocalDateTime, DayOfMonth> localDateTimeAsDayOfMonth = (date, zone) -> DayOfMonth.from(date);
	public static TemporalConverter<LocalDateTime, DayOfWeek> localDateTimeAsDayOfWeek = (date, zone) -> date.getDayOfWeek();
	public static TemporalConverter<LocalDateTime, Hour> localDateTimeAsHour = (date, zone) -> Hour.from(date);
	public static TemporalConverter<LocalDateTime, Minute> localDateTimeAsMinute = (date, zone) -> Minute.from(date);
	public static TemporalConverter<LocalDateTime, Second> localDateTimeAsSecond = (date, zone) -> Second.from(date);

	/**
	 * ZonedDateTime Converters
	 */
	public static TemporalConverter<ZonedDateTime, ZonedDateTime> zonedDateTimeAsZonedDateTime = (date, zone) -> zone.map(z -> date.withZoneSameInstant(z)).orElse(date);
	public static TemporalConverter<ZonedDateTime, LocalDate> zonedDateTimeAsLocalDate = (date, zone) -> zonedDateTimeAsZonedDateTime.apply(date, zone).toLocalDate();
	public static TemporalConverter<ZonedDateTime, Year> zonedDateTimeAsYear = (date, zone) -> Year.from(zonedDateTimeAsLocalDate.apply(date, zone));
	public static TemporalConverter<ZonedDateTime, Month> zonedDateTimeAsMonth = (date, zone) -> zonedDateTimeAsLocalDate.apply(date, zone).getMonth();
	public static TemporalConverter<ZonedDateTime, DayOfMonth> zonedDateTimeAsDayOfMonth = (date, zone) -> DayOfMonth.from(zonedDateTimeAsLocalDate.apply(date, zone));
	public static TemporalConverter<ZonedDateTime, DayOfWeek> zonedDateTimeAsDayOfWeek = (date, zone) -> zonedDateTimeAsLocalDate.apply(date, zone).getDayOfWeek();
	public static TemporalConverter<ZonedDateTime, Hour> zonedDateTimeAsHour = (date, zone) -> Hour.from(zonedDateTimeAsZonedDateTime.apply(date, zone));
	public static TemporalConverter<ZonedDateTime, Minute> zonedDateTimeAsMinute = (date, zone) -> Minute.from(zonedDateTimeAsZonedDateTime.apply(date, zone));
	public static TemporalConverter<ZonedDateTime, Second> zonedDateTimeAsSecond = (date, zone) -> Second.from(zonedDateTimeAsZonedDateTime.apply(date, zone));
	
	/**
	 * DayOfWeek Converters
	 */
	public static TemporalConverter<DayOfWeek, DayOfWeek> dayOfWeekToDayOfWeek = (date, zone) -> date;
	
	/**
     * {@link OffsetDateTime} Converters
     */
	public static TemporalConverter<OffsetDateTime, OffsetDateTime> offsetDateTimeAsOffsetDateTime = (date, zone) -> zone.map(z -> date.withOffsetSameInstant(z.getRules().getOffset(date.toLocalDateTime()))).orElse(date);    
    public static TemporalConverter<OffsetDateTime, LocalDate> offsetDateTimeAsLocalDate = (date, zone) -> offsetDateTimeAsOffsetDateTime.apply(date, zone).toLocalDate();
    public static TemporalConverter<OffsetDateTime, Year> offsetDateTimeAsYear = (date, zone) -> Year.from(offsetDateTimeAsLocalDate.apply(date, zone));
    public static TemporalConverter<OffsetDateTime, Month> offsetDateTimeAsMonth = (date, zone) -> offsetDateTimeAsLocalDate.apply(date, zone).getMonth();
    public static TemporalConverter<OffsetDateTime, DayOfMonth> offsetDateTimeAsDayOfMonth = (date, zone) -> DayOfMonth.from(offsetDateTimeAsLocalDate.apply(date, zone));
    public static TemporalConverter<OffsetDateTime, DayOfWeek> offsetDateTimeAsDayOfWeek = (date, zone) -> offsetDateTimeAsLocalDate.apply(date, zone).getDayOfWeek();
    public static TemporalConverter<OffsetDateTime, Hour> offsetDateTimeAsHour = (date, zone) -> Hour.from(offsetDateTimeAsOffsetDateTime.apply(date, zone));
    public static TemporalConverter<OffsetDateTime, Minute> offsetDateTimeAsMinute = (date, zone) -> Minute.from(offsetDateTimeAsOffsetDateTime.apply(date, zone));
    public static TemporalConverter<OffsetDateTime, Second> offsetDateTimeAsSecond = (date, zone) -> Second.from(offsetDateTimeAsOffsetDateTime.apply(date, zone));    
}
