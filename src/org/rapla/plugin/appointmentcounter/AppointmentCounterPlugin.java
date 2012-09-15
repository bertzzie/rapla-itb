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
package org.rapla.plugin.appointmentcounter;
import org.apache.avalon.framework.configuration.Configuration;
import org.rapla.framework.Container;
import org.rapla.framework.PluginDescriptor;
import org.rapla.plugin.RaplaExtensionPoints;
import org.rapla.plugin.RaplaPluginMetaInfo;

public class AppointmentCounterPlugin implements PluginDescriptor
{
    public static final String PLUGIN_CLASS = AppointmentCounterPlugin.class.getName();
 
    public String toString() {
        return "Appointment Counter";
    }

    public void provideServices(Container container, Configuration config) {
        
        if ( !config.getAttributeAsBoolean("enabled", true) )
        	return;

        container.addContainerProvidedComponent( RaplaExtensionPoints.APPOINTMENT_STATUS, AppointmentCounterFactory.class.getName(), PLUGIN_CLASS, config);
    }

    public Object getPluginMetaInfos( String key )
    {
        if ( RaplaPluginMetaInfo.METAINFO_PLUGIN_ENABLED_BY_DEFAULT.equals( key )) {
            return Boolean.TRUE;
        }
        return null;
    }


}

