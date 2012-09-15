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
package org.rapla.plugin.periodview;

import org.apache.avalon.framework.configuration.Configuration;
import org.rapla.framework.Container;
import org.rapla.plugin.RaplaExtensionPoints;
import org.rapla.plugin.RaplaPluginMetaInfo;

/** Plugin no longer supported to enable uncomment PluginDescriptor Interface
@deprecated
*/
public class PeriodViewPlug_in
{
    static boolean ENABLE_BY_DEFAULT = false;

    public String toString() {
        return "Period View (not supported)";
    }

    public void provideServices(Container container, Configuration config) {
        if ( !config.getAttributeAsBoolean("enabled", ENABLE_BY_DEFAULT) )
        	return;

        container.addContainerProvidedComponent
        (
         RaplaExtensionPoints.CALENDAR_VIEW_EXTENSION
         ,PeriodViewFactory.class.getName()
         ,PeriodViewFactory.PERIOD_VIEW
         ,null
         );
    }


    public Object getPluginMetaInfos( String key )
    {
        if ( RaplaPluginMetaInfo.METAINFO_PLUGIN_ENABLED_BY_DEFAULT.equals( key )) {
            return new Boolean( ENABLE_BY_DEFAULT );
        }
        return null;
    }


}

