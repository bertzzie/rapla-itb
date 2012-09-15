package org.rapla.plugin.export2ical;

import org.apache.avalon.framework.configuration.Configuration;
import org.rapla.components.xmlbundle.I18nBundle;
import org.rapla.components.xmlbundle.impl.I18nBundleImpl;
import org.rapla.framework.Container;
import org.rapla.framework.PluginDescriptor;
import org.rapla.plugin.RaplaExtensionPoints;
import org.rapla.plugin.RaplaPluginMetaInfo;
import org.rapla.server.ServerService;


public class Export2iCalPlugin implements PluginDescriptor {

	public static final String RESOURCE_FILE = Export2iCalPlugin.class.getPackage().getName() + ".Export2iCalResources";

	public static final String PLUGIN_CLASS = Export2iCalPlugin.class.getName();
	public static final String PLUGIN_ENTRY = "org.rapla.plugin.export2ical";
	public static final String ICAL_EXPORT = PLUGIN_ENTRY+".selected";
	public static final String DAYS_BEFORE = "days_before";
	public static final String DAYS_AFTER = "days_after";

	public static String PREF_BEFORE_DAYS	= "export2iCal_before_days";
	public static String PREF_AFTER_DAYS	= "export2iCal_after_days";
	
	public static final String GLOBAL_INTERVAL = "global_interval";
	public static final String EXPORT_ATTENDEES = "export_attendees";
	public static final String EXPORT_ATTENDEES_EMAIL_ATTRIBUTE = "export_attendees_email_attribute";
	public static final String TIMEZONE = "timezone";
	public static final String FIRST_PLUGIN_STARTDATE = "first_plugin_startdate";
	public static final String LAST_MODIFIED_INTERVALL = "last_modified_intervall";
	
	public static final String DEFAULT_timezone = "Etc/UTC";
	public static final int DEFAULT_daysBefore = 30;
	public static final int DEFAULT_daysAfter = 30;
	public static final boolean DEFAULT_basedOnAutoExport = true;
	public static final boolean DEFAULT_globalIntervall = true;
	public static final int DEFAULT_lastModifiedIntervall = 5;
    public static String DEFAULT_attendee_resource_attribute = "email";
    public static String DEFAULT_attendee_participation_status;


	static boolean ENABLE_BY_DEFAULT = true;
    public static boolean DEFAULT_exportAttendees = false;
    public static final String EXPORT_ATTENDEES_PARTICIPATION_STATUS = "export_attendees_participation_status";

    public String toString() {
		return "Export2iCal Plugin";
	}

	public void provideServices(Container container, Configuration config) {

		if (!config.getAttributeAsBoolean("enabled", ENABLE_BY_DEFAULT))
			return;

        try {
          //  Class.forName("net.fortuna.ical4j.model.parameter.PartStat");
            DEFAULT_attendee_participation_status = "TENTATIVE";
        } catch (Throwable ex ) {
        }

		
		container.addContainerProvidedComponentInstance(PLUGIN_CLASS, Boolean.TRUE);
		container.addContainerProvidedComponent(I18nBundle.ROLE, I18nBundleImpl.class.getName(), RESOURCE_FILE, I18nBundleImpl.createConfig(RESOURCE_FILE));
		
		  if ( container.getContext().has( ServerService.ROLE) ){
		      container.addContainerProvidedComponent(RaplaExtensionPoints.SERVLET_PAGE_EXTENSION, Export2iCalServlet.class.getName(), "iCal", config);
		      container.addContainerProvidedComponent(ICalExport.class.getName(), RaplaICalExport.class.getName(), ICalExport.class.getName(),config);
		      container.addContainerProvidedComponent(RaplaExtensionPoints.SERVER_EXTENSION, Export2iCalChangeWatcher.class.getName(), PLUGIN_CLASS, config);
	     } else {
	         container.addContainerProvidedComponent(RaplaExtensionPoints.CLIENT_EXTENSION, Export2iCalDialogInitializer.class.getName(), PLUGIN_CLASS, config);
	         container.addContainerProvidedComponent(RaplaExtensionPoints.USER_OPTION_PANEL_EXTENSION, Export2iCalUserOption.class.getName(), PLUGIN_CLASS, config);
	 		 container.addContainerProvidedComponent(RaplaExtensionPoints.PLUGIN_OPTION_PANEL_EXTENSION, Export2iCalAdminOption.class.getName(), PLUGIN_CLASS, config);
	     }
		
	}

	

	public Object getPluginMetaInfos(String key) {
	    if ( RaplaPluginMetaInfo.METAINFO_PLUGIN_ENABLED_BY_DEFAULT.equals( key )) {
            return new Boolean( ENABLE_BY_DEFAULT );
        }
        return null;
	}
}
