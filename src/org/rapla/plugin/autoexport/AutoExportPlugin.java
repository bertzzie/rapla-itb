/*--------------------------------------------------------------------------*
 | Copyright (C) 2006 Christopher Kohlhaas                                  |
 |                                                                          |
 | This program is free software; you can redistribute it and/or modify     |
 | it under the terms of the GNU General Public License as published by the |
 | Free Software Foundation. A copy of the license has been included with   |
 | these distribution in the COPYING file, if not go to www.fsf.org         |
 |                                                                          |
 | As a special exception, you are granted the permissions to link this     |
 | program with every library, which license fulfills the Open Source       |
 | Definition as published by the Open Source Initiative (OSI).             |
 *--------------------------------------------------------------------------*/
package org.rapla.plugin.autoexport;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.rapla.components.xmlbundle.I18nBundle;
import org.rapla.components.xmlbundle.impl.I18nBundleImpl;
import org.rapla.framework.Container;
import org.rapla.framework.PluginDescriptor;
import org.rapla.framework.StartupEnvironment;
import org.rapla.plugin.RaplaExtensionPoints;
import org.rapla.plugin.RaplaPluginMetaInfo;
import org.rapla.servletpages.DefaultHTMLMenuEntry;
import org.rapla.servletpages.HTMLMenuExtensionPoint;
import org.rapla.servletpages.RaplaResourcePageGenerator;

public class AutoExportPlugin extends AbstractLogEnabled implements PluginDescriptor
{
    public static final String RESOURCE_FILE = AutoExportPlugin.class.getPackage().getName() + ".AutoExportResources";
    public static final String PLUGIN_ENTRY = "org.rapla.plugin.autoexport";
    public static final String HTML_EXPORT= PLUGIN_ENTRY + ".selected";
    public static final String PLUGIN_CLASS = AutoExportPlugin.class.getName();
    public static final String SHOW_CALENDAR_LIST_IN_HTML_MENU = "show_calendar_list_in_html_menu";
    static boolean ENABLE_BY_DEFAULT = true;
    
    public AutoExportPlugin(Logger logger) {
        enableLogging( logger);
    }

    public String toString() {
        return "HMTL Export";
    }

    public void provideServices(Container container, Configuration config) {
        container.addContainerProvidedComponent( RaplaExtensionPoints.PLUGIN_OPTION_PANEL_EXTENSION, AutoExportPluginOption.class.getName(),PLUGIN_CLASS, config);
        container.addContainerProvidedComponent( I18nBundle.ROLE, I18nBundleImpl.class.getName(), RESOURCE_FILE,I18nBundleImpl.createConfig( RESOURCE_FILE ) );
        
        if ( !config.getAttributeAsBoolean("enabled", ENABLE_BY_DEFAULT) )
        	return;

        container.addContainerProvidedComponentInstance(PLUGIN_CLASS, Boolean.TRUE);
        //container.addContainerProvidedComponent( RaplaExtensionPoints.USER_OPTION_PANEL_EXTENSION, AutoExportOption.class.getName(),PLUGIN_CLASS, config);
        StartupEnvironment env = container.getStartupEnvironment();
        if ( env.getStartupMode() == StartupEnvironment.SERVLET) {
            container.addContainerProvidedComponent( RaplaExtensionPoints.SERVLET_PAGE_EXTENSION, CalendarPageGenerator.class.getName(),"export/calendar", config);
            container.addContainerProvidedComponent( RaplaExtensionPoints.SERVLET_PAGE_EXTENSION, CalendarPageGenerator.class.getName(),"calendar", config);
            try {
                RaplaResourcePageGenerator resourcePageGenerator = (RaplaResourcePageGenerator)container.getContext().lookup(RaplaExtensionPoints.SERVLET_PAGE_EXTENSION + "/resource");
                //container.addContainerProvidedComponent(RaplaPageGenerator.ROLE, ReportPageGenerator.class.getName(),"report", config);
                // registers the standard calendar files
                
                resourcePageGenerator.registerResource( "calendar.css", "text/css", this.getClass().getResource("/org/rapla/plugin/autoexport/calendar.css"));
                // look if we should add a menu entry of exported lists
                if (config.getAttributeAsBoolean(AutoExportPlugin.SHOW_CALENDAR_LIST_IN_HTML_MENU, false))
                {
                    container.addContainerProvidedComponent( RaplaExtensionPoints.SERVLET_PAGE_EXTENSION, CalendarListPageGenerator.class.getName(),"calendarlist", config);
                    HTMLMenuExtensionPoint mainMenu = (HTMLMenuExtensionPoint)container.getContext().lookup( RaplaExtensionPoints.HTML_MAIN_MENU_EXTENSION_POINT );
                    I18nBundle i18n = (I18nBundle)container.getContext().lookup( I18nBundle.ROLE + "/" + RESOURCE_FILE );
                    mainMenu.insert( new DefaultHTMLMenuEntry(i18n.getString( "calendar_list"),"rapla?page=calendarlist"));
                }
            } catch ( Exception ex) {
                getLogger().error("Could not initialize autoexportplugin on server" , ex);
            }
        }
    }

    public Object getPluginMetaInfos( String key )
    {
        if ( RaplaPluginMetaInfo.METAINFO_PLUGIN_ENABLED_BY_DEFAULT.equals( key )) {
            return new Boolean( ENABLE_BY_DEFAULT );
        }
        return null;
    }


}

