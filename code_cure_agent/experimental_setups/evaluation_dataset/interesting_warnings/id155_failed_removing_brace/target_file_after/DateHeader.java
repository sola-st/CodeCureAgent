package net.sourceforge.MSGViewer.factory.mbox.headers;

import com.auxilii.msgparser.Message;
import java.text.ParseException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.ZoneId;

import java.util.Locale;

/**
 *
 * @author martin
 */
public class DateHeader extends HeaderParser
{
    /**
     * http://tools.ietf.org/html/rfc5322#section-3.6.1
     *  date-time       =   [ day-of-week "," ] date time [CFWS]
     *  day-of-week     =   ([FWS] day-name) / obs-day-of-week
     *  day-name        =   "Mon" / "Tue" / "Wed" / "Thu" /
     *                      "Fri" / "Sat" / "Sun"
     *  date            =   day month year
     *  day             =   ([FWS] 1*2DIGIT FWS) / obs-day
     *  month           =   "Jan" / "Feb" / "Mar" / "Apr" /
     *                      "May" / "Jun" / "Jul" / "Aug" /
     *                      "Sep" / "Oct" / "Nov" / "Dec"
     *  year            =   (FWS 4*DIGIT FWS) / obs-year
     *  time            =   time-of-day zone
     *  time-of-day     =   hour ":" minute [ ":" second ]
     *  hour            =   2DIGIT / obs-hour
     *  minute          =   2DIGIT / obs-minute
     *  second          =   2DIGIT / obs-second
     *  zone            =   (FWS ( "+" / "-" ) 4DIGIT) / obs-zone
     *
     * eg: Fri, 1 Jul 2011 20:18:36 +0200
     */
    public static final DateTimeFormatter date_format = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss ZZZZ", Locale.US);

    public DateHeader()
    {
        super("Date");
    }

    @Override
    public void parse(Message message, String line) throws ParseException
    {
        if( !line.isEmpty() ) {
            try {
                ZonedDateTime zdt = ZonedDateTime.parse(line, date_format);
                message.setDate(Date.from(zdt.toInstant()));
            } catch (DateTimeParseException e) {
                throw new ParseException(e.getMessage(), 0);
            }
        }
    }
    }

}
