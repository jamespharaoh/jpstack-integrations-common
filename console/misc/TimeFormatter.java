package wbs.platform.console.misc;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Instant;
import org.joda.time.LocalDate;

public
interface TimeFormatter {

	String instantToTimestampString (
			DateTimeZone timeZone,
			Instant instant);

	String instantToDateStringLong (
			DateTimeZone timeZone,
			Instant instant);

	String instantToTimeString (
			DateTimeZone timeZone,
			Instant instant);

	String instantToDateStringShort (
			DateTimeZone timeZone,
			Instant instant);

	String instantToHttpTimestampString (
			Instant instant);

	Instant timestampStringToInstant (
			DateTimeZone timeZone,
			String string);

	String dateTimeToTimestampTimezoneString (
			DateTime dateTime);

	String localDateToDateString (
			LocalDate localDate);

	LocalDate dateStringToLocalDate (
			String string);

	DateTime timestampTimezoneToDateTime (
			String string);

	DateTimeZone defaultTimezone ();

	DateTimeZone timezone (
			String name);

}
