package org.rapla.plugin.export2ical;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ValidationException;

import org.apache.avalon.framework.configuration.Configuration;
import org.rapla.entities.User;
import org.rapla.entities.configuration.Preferences;
import org.rapla.entities.domain.Reservation;
import org.rapla.facade.RaplaComponent;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.framework.RaplaLocale;
import org.rapla.gui.CalendarModel;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.internal.common.CalendarModelImpl;
import org.rapla.servletpages.RaplaPageGenerator;

public class Export2iCalServlet extends RaplaComponent implements RaplaPageGenerator {

	private int global_daysBefore;
	private int global_daysAfter;

//	private String username;
//	private String filename;
	
	private boolean global_interval;
	//private HttpServletResponse response;
	private TimeZone timezone;
	
	private SimpleDateFormat rfc1123DateFormat;

//  private SimpleTimeZone gmt = new SimpleTimeZone(0, "GMT");

	//private java.util.Calendar calendar;
    //private Preferences preferences;

    public Export2iCalServlet(RaplaContext context, Configuration config) throws RaplaException {
		super(context);

		global_interval = config.getChild(Export2iCalPlugin.GLOBAL_INTERVAL).getValueAsBoolean(Export2iCalPlugin.DEFAULT_globalIntervall);

		global_daysBefore = config.getChild(Export2iCalPlugin.DAYS_BEFORE).getValueAsInteger(Export2iCalPlugin.DEFAULT_daysBefore);
		global_daysAfter = config.getChild(Export2iCalPlugin.DAYS_AFTER).getValueAsInteger(Export2iCalPlugin.DEFAULT_daysAfter);

		timezone = TimeZone.getTimeZone(config.getChild(Export2iCalPlugin.TIMEZONE).getValue(Export2iCalPlugin.DEFAULT_timezone));

		rfc1123DateFormat = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.US);
		rfc1123DateFormat.setTimeZone(new SimpleTimeZone(0, "GMT"));
	}

	public synchronized void generatePage(ServletContext servletContext, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

		//this.response = response;

		final String filename = request.getParameter("file");
		final String username = request.getParameter("user");
        getLogger().debug("File: "+filename);
        getLogger().debug("User: "+username);

		boolean getAllAppointments = request.getParameter("complete") != null;
		// if param COMPLETE is given, retrieve all appointments

		try {
            final User user = getQuery().getUser(username);
            final Preferences preferences = getQuery().getPreferences(user);

			final CalendarModel calModel = getCalendarModel(preferences, user, filename);

            if (calModel == null) {
                response.getWriter().println("The calendar '" + filename + "' you tried to retrieve is not published or available!");
                return;
            }

			response.setHeader("Last-Modified", rfc1123DateFormat.format(Export2iCalChangeWatcher.getLastModified(calModel)));
			final Object isSet = calModel.getOption(Export2iCalPlugin.ICAL_EXPORT);
            
			if((isSet == null || isSet.equals("false")))
			{
				response.getWriter().println("The calendar '" + filename + "' you tried to retrieve is not published or available!");
				return;
			}
			
			if (request.getMethod().equals("HEAD")) {
				return;
			}

			final Reservation[] reserv = getAllAppointments ? getAllReservations(calModel) : calModel.getReservations();
			
			write(response, reserv, filename, null);
		} catch (Exception e) {
			response.getWriter().println(("An error occured giving you the Calendarview for user " + username + " named " + filename));
			response.getWriter().println();
			e.printStackTrace(response.getWriter());
		}
	}


	/**
	 * Retrieves CalendarModel by username && filename, sets appropriate before
	 * and after times (only if global intervall is false)
	 * 
	 * @param user
	 *            the Rapla-User
	 * @param filename
	 *            the Filename of the exported view
	 * @return
	 */
	private synchronized CalendarModel getCalendarModel(Preferences preferences, User user, String filename) {
		try {
			final CalendarModelImpl calModel = new CalendarModelImpl(getContext(), user);
			calModel.load(filename);

			int daysBefore = global_interval ? global_daysBefore : preferences.getEntryAsInteger(Export2iCalPlugin.PREF_BEFORE_DAYS, 11);
			int daysAfter = global_interval ? global_daysAfter : preferences.getEntryAsInteger(Export2iCalPlugin.PREF_AFTER_DAYS, global_daysAfter);

			final Date now = new Date();

			//calModel.getReservations(startDate, endDate)

            final RaplaLocale raplaLocale = getRaplaLocale();
            final java.util.Calendar calendar = raplaLocale.createCalendar();

			// set start Date
            calendar.setTime(now);
			calendar.add(java.util.Calendar.DAY_OF_YEAR, -daysBefore);
			calModel.setStartDate(calendar.getTime());

			// set end Date
			calendar.setTime(now);
			calendar.add(java.util.Calendar.DAY_OF_YEAR, daysAfter);
			calModel.setEndDate(calendar.getTime());
			
			//debug sysout
			//System.out.println("startdate - before  "+ calModel.getStartDate() + " - " + daysBefore);
			//System.out.println("enddate - after "+ calModel.getStartDate() + " - " + daysAfter);

			return calModel;

		} catch (RaplaException e) {
			getLogger().error("The Calendarmodel " + filename + " could not be read for the user " + user);
			return null;
		} catch (NullPointerException e) {
			getLogger().error("There is no Calendarmodel named " + filename + " for the user: " + user);
			return null;
		}
	}

	private synchronized Reservation[] getAllReservations(final CalendarModel calModel) throws RaplaException {
        final RaplaLocale raplaLocale = getRaplaLocale();
        final java.util.Calendar calendar = raplaLocale.createCalendar();

		calendar.set(calendar.getMinimum(java.util.Calendar.YEAR), calendar.getMinimum(java.util.Calendar.MONTH), calendar.getMinimum(java.util.Calendar.DAY_OF_MONTH));
		calModel.setStartDate(calendar.getTime());

		// Calendar.getMaximum doesn't work with iCal4j. Using 9999
		calendar.set(9999, calendar.getMaximum(java.util.Calendar.MONTH), calendar.getMaximum(java.util.Calendar.DAY_OF_MONTH));
		calModel.setEndDate(calendar.getTime());

		return calModel.getReservations();
	}

	private synchronized void write(final HttpServletResponse response, final Reservation[] reserv, final String filename, final Preferences preferences) throws RaplaException, IOException {

		response.setContentType("text/calendar; charset=" + getRaplaLocale().getCharsetNonUtf());
		response.setHeader("Content-Disposition", "attachment; filename=" + filename + ".ics");

		if (reserv == null) {
			throw new RaplaException("Error with returning '" + filename);
		}
		final RaplaContext context = getContext();
		final Export2iCalConverter converter = new Export2iCalConverter(context,timezone, preferences);
        final RaplaGUIComponent comp = new RaplaGUIComponent(context);
		converter.setiCalendarWorktime(comp.getCalendarOptions(), getRaplaLocale());
		final Calendar iCal = converter.createiCalender(reserv);
		final CalendarOutputter calOutputter = new CalendarOutputter();
		final PrintWriter responseWriter = response.getWriter();
		try {
			calOutputter.output(iCal, responseWriter);
		} catch (ValidationException e) {
			getLogger().error("The calendar file is invalid!\n" + e);
		}
	}

}
