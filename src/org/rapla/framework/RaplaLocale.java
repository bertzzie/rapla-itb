package org.rapla.framework;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;


/** This class contains all locale specific information for Rapla. Like
<ul>
  <li>Selected language.</li>
  <li>Selected country.</li>
  <li>Available languages (if the user has the possibility to choose a language)</li>
  <li>TimeZone for appointments (This is always GMT+0)</li>
</ul>
<p>
Also it provides basic formating information for the dates.
</p>
<p>
Configuration is done in the rapla.xconf:
<pre>
&lt;locale>
 &lt;languages default="de">
   &lt;language>de&lt;/language>
   &lt;language>en&lt;/language>
 &lt;/languages>
 &lt;country>US&lt;/country>
&lt;/locale>
</pre>
If languages default is not set, the system default wil be used.<br>
If country code is not set, the system default will be used.<br>
</p>
 */

public interface RaplaLocale
{
    String ROLE = RaplaLocale.class.getName();
    String LANGUAGE_ENTRY = "org.rapla.language";
    
    String[] getAvailableLanguages();

    /** creates a calendar initialized with the Rapla timezone ( that is always GMT+0 for Rapla  )  and the selected locale*/
    Calendar createCalendar();

    String formatTime( Date date );

    /** sets time to 0:00:00 or 24:00:00 */
    Date toDate( Date date, boolean fillDate );

    /** sets time to 0:00:00  */
    Date toDate( int year, int month, int date );

     /** sets date to 0:00:00  */
    Date toTime( int hour, int minute, int second );

    /** Uses the first date parameter for year, month, date information and
     the second for hour, minutes, second, millisecond information.*/
    Date toDate( Date date, Date time );

    /** format long with the local NumberFormat */
    String formatNumber( long number );

    /** format without year */
    String formatDateShort( Date date );

    /** format with locale DateFormat.SHORT */
    String formatDate( Date date );

    /** format with locale DateFormat.MEDIUM */
    String formatDateLong( Date date );

    /** Abbreviation of locale weekday name of date. */
    String getWeekday( Date date );

     /** Monthname of date. */
    String getMonth( Date date );

    String getCharsetNonUtf();
    
    TimeZone getTimeZone();
    
    TimeZone getSystemTimezone();

    Locale getLocale();

}