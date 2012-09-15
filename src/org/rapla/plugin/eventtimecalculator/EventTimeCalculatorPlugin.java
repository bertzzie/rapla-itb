package org.rapla.plugin.eventtimecalculator;

import org.apache.avalon.framework.configuration.Configuration;
import org.rapla.components.xmlbundle.I18nBundle;
import org.rapla.components.xmlbundle.impl.I18nBundleImpl;
import org.rapla.framework.Container;
import org.rapla.framework.PluginDescriptor;
import org.rapla.plugin.RaplaExtensionPoints;
import org.rapla.plugin.RaplaPluginMetaInfo;

public class EventTimeCalculatorPlugin implements PluginDescriptor {


    public static final String RESOURCE_FILE = EventTimeCalculatorPlugin.class.getPackage().getName() + ".EventTimeCalculatorResources";

    public static final String PLUGIN_CLASS = EventTimeCalculatorPlugin.class.getName();
    static boolean ENABLE_BY_DEFAULT = false;

    // public static String PREF_LUNCHBREAK_NUMBER = "eventtimecalculator_lunchbreak_number";

    public static final String INTERVAL_NUMBER = "interval_number";
    public static final String BREAK_NUMBER = "break_number";
    // public static final String LUNCHBREAK_NUMBER = "lunchbreak_number";
    public static final String TIME_UNIT = "time_unit";
    public static final String TIME_FORMAT = "time_format";

    public static final int DEFAULT_intervalNumber = 60;
    public static final int DEFAULT_timeUnit = 60;
    public static final String DEFAULT_timeFormat = "{0},{1}";
    public static final int DEFAULT_breakNumber = 0;
    //public static final int DEFAULT_lunchbreakNumber = 30;

    public String toString() {
        return "Event Time Calculator";
    }

    /**
     * provides the resource file of the plugin.
     * uses the extension points to provide the different services of the plugin.
     */
    public void provideServices(Container container, Configuration config) {
        if (!config.getAttributeAsBoolean("enabled", ENABLE_BY_DEFAULT))
            return;
        container.addContainerProvidedComponent(I18nBundle.ROLE, I18nBundleImpl.class.getName(), RESOURCE_FILE, I18nBundleImpl.createConfig(RESOURCE_FILE));
        container.addContainerProvidedComponent(RaplaExtensionPoints.PLUGIN_OPTION_PANEL_EXTENSION, EventTimeCalculatorAdminOption.class.getName(), PLUGIN_CLASS, config);
        //      container.addContainerProvidedComponent(RaplaExtensionPoints.USER_OPTION_PANEL_EXTENSION, EventTimeCalculatorUserOption.class.getName(), PLUGIN_CLASS, config);
        container.addContainerProvidedComponent(RaplaExtensionPoints.APPOINTMENT_STATUS, EventTimeCalculatorFactory.class.getName(), PLUGIN_CLASS, config);

        container.addContainerProvidedComponent(RaplaExtensionPoints.APPOINTMENT_TABLE_COLUMN, DurationColumnAppoimentBlock.class.getName(), PLUGIN_CLASS, config);
        container.addContainerProvidedComponent(RaplaExtensionPoints.RESERVATION_TABLE_COLUMN, DurationColumnReservation.class.getName(), PLUGIN_CLASS, config);

        container.addContainerProvidedComponent(RaplaExtensionPoints.RESERVATION_TABLE_SUMMARY, DurationCounter.class.getName(), PLUGIN_CLASS, config);
        container.addContainerProvidedComponent(RaplaExtensionPoints.APPOINTMENT_TABLE_SUMMARY, DurationCounter.class.getName(), PLUGIN_CLASS, config);


    }

    public Object getPluginMetaInfos(String key) {
        if (RaplaPluginMetaInfo.METAINFO_PLUGIN_ENABLED_BY_DEFAULT.equals(key)) {
            return new Boolean(ENABLE_BY_DEFAULT);
        }
        return null;
    }
}