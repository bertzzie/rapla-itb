package org.rapla.plugin.export2ical;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.configuration.MutableConfiguration;
import org.rapla.components.util.DateTools;
import org.rapla.entities.configuration.Preferences;
import org.rapla.entities.configuration.RaplaConfiguration;
import org.rapla.entities.domain.Reservation;
import org.rapla.facade.ClientFacade;
import org.rapla.facade.RaplaComponent;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.CalendarModel;

public class Export2iCalChangeWatcher extends RaplaComponent  {

	private static Date firstPluginStartDate;
	private static ClientFacade facade;
	private Configuration config;
	private static Calendar calendar;
	//private TimeZone pluginTimeZone;
	private static int lastModifiedIntervall;

	public Export2iCalChangeWatcher(RaplaContext context, Configuration config) throws RaplaException {
		super(context);

		this.config = config;

		lastModifiedIntervall = config.getChild(Export2iCalPlugin.LAST_MODIFIED_INTERVALL).getValueAsInteger(10);
		//pluginTimeZone = TimeZone.getTimeZone(config.getAttribute(Export2iCalPlugin.TIMEZONE, "Etc/UTC"));

		calendar = getRaplaLocale().createCalendar();

		facade = getClientFacade();
		readConfig();

	}

	private void readConfig() {
		SimpleDateFormat serializationDateFormat = new SimpleDateFormat("yyyy-MM-dd");

		try {
			firstPluginStartDate = serializationDateFormat.parse(config.getChild(Export2iCalPlugin.FIRST_PLUGIN_STARTDATE).getValue());
		} catch (ConfigurationException e) {
			getLogger().warn("The Export2iCal config could not be read - maybe the plugin was started for the first time\nThe config has been re-initialised");
			firstPluginStartDate = initStartupConfig(serializationDateFormat);
		} catch (ParseException e) {
			getLogger().error("The Export2iCal config could not be read - The timestamp could not be parsed.\nThe config has been re-initialised");
			firstPluginStartDate = initStartupConfig(serializationDateFormat);
		}
	}

	private Date initStartupConfig(SimpleDateFormat serializationDateFormat) {
		Date now = facade.today();

		try {
		    
		    Preferences preferences = (Preferences) facade.edit(facade.getPreferences(null));
		    RaplaConfiguration config = (RaplaConfiguration) preferences.getEntry("org.rapla.plugin");
	        String className =  Export2iCalPlugin.PLUGIN_CLASS;
	        //getDescritorClassName()
	        
	        final Configuration oldClass = config.find("class", className);
	        RaplaConfiguration newConfig;
	        if ( oldClass != null )
	        {
		        final DefaultConfiguration newChild = new DefaultConfiguration( oldClass);
		        MutableConfiguration conf = newChild.getMutableChild(Export2iCalPlugin.FIRST_PLUGIN_STARTDATE, true);
	            conf.setValue(serializationDateFormat.format(now));
	            newConfig = config.replace(oldClass, newChild);
	        }
	        else
	        {
	        	final DefaultConfiguration newChild = new DefaultConfiguration( "plugin");
	        	newChild.setAttribute("class", className);
	        	newChild.setAttribute("enabled", true);
	        	MutableConfiguration conf = newChild.getMutableChild(Export2iCalPlugin.FIRST_PLUGIN_STARTDATE, true);
	        	conf.setValue(serializationDateFormat.format(now));
	        	newConfig = config.add( newChild);
	        }
	        preferences.putEntry( "org.rapla.plugin",newConfig);
	        facade.store(preferences);
		} catch (Exception e) {
			getLogger().error("Error writing Export2iCal Startup Config!\n" + e.getMessage(), e);
		}
		return now;
	}

	/**
	 * Calculates Global-Lastmod By modulo operations, this returns a fresh
	 * last-mod every n days n can be set for all users in the last-modified
	 * intervall settings
	 * 
	 * @return
	 */
	public static Date getGlobalLastModified() {

		if (lastModifiedIntervall == -1) {
			calendar.setTimeInMillis(0);
			return calendar.getTime();
		}

		long nowInMillis = DateTools.cutDate(new Date()).getTime();
		long daysSinceStart = (nowInMillis - firstPluginStartDate.getTime()) / DateTools.MILLISECONDS_PER_DAY;

		calendar.setTimeInMillis(nowInMillis - (daysSinceStart % lastModifiedIntervall) * DateTools.MILLISECONDS_PER_DAY);

		return calendar.getTime();
	}

	/**
	 * Get last modified if a list of allocatables
	 * 
	 * @param allocatable
	 * @return
	 */
	public static Date getLastModified(CalendarModel calModel) throws RaplaException {

		if(calModel==null){
			return getGlobalLastModified();
		}
		
		Date endDate = null;
        Date startDate = facade.today();
        final Reservation[] reservations = calModel.getReservations(startDate, endDate);
		
		// set to minvalue
		Date maxDate = new Date();
		maxDate.setTime(0);

		for (Reservation r:reservations) 
		{
			Date lastMod = r.getLastChangeTime();

			if (lastMod != null && maxDate.before(lastMod)) {
				maxDate = lastMod;
			}
		}

		if (lastModifiedIntervall != -1 && DateTools.countDays(maxDate, new Date()) < lastModifiedIntervall) {
			return maxDate;
		} else {
			return getGlobalLastModified();
		}

	}

}
