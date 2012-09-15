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
package org.rapla.plugin.jndi;

import org.apache.avalon.framework.configuration.Configuration;
import org.rapla.framework.Container;
import org.rapla.framework.PluginDescriptor;
import org.rapla.plugin.RaplaExtensionPoints;
import org.rapla.plugin.jndi.internal.JNDIOption;
import org.rapla.plugin.jndi.internal.JNDITest;
import org.rapla.plugin.jndi.internal.RaplaJNDITestOnLocalhost;
import org.rapla.plugin.jndi.internal.RaplaJNDITestOnServer;
import org.rapla.server.ServerService;
import org.rapla.storage.AuthenticationStore;

public class JNDIPlugin implements PluginDescriptor {
    public static final String PLUGIN_CLASS = JNDIPlugin.class.getName();
    public static final String PLUGIN_NAME = "Ldap or other JNDI Authentication";

    public static final String JNDI_ON_SERVER = JNDIPlugin.class.getPackage().getName() + ".JNDIOnServer";
    public static final String JNDI_ON_LOCALHOST = JNDIPlugin.class.getPackage().getName() + ".JNDIOnLocalhost";
   
    public final static String USERGROUP_CONFIG = "org.rapla.plugin.jndi.newusergroups";
    
    public String toString() {
        return PLUGIN_NAME;
    }
    
    public void provideServices(Container container, Configuration config) 
    {
    	if ( container.getContext().has( ServerService.ROLE) ){
            // only add mail service on localhost
            container.addContainerProvidedComponent( JNDITest.ROLE, RaplaJNDITestOnLocalhost.class.getName(), JNDI_ON_LOCALHOST , config);
        } else {
            // the following order is important for resolving,
            // first add the service on the server
            // then on localhost
    //        container.addContainerProvidedComponent( JNDITest.ROLE, RaplaJNDITestOnLocalhost.class.getName(), JNDI_ON_LOCALHOST , config);
            container.addContainerProvidedComponent( JNDITest.ROLE, RaplaJNDITestOnServer.class.getName(), JNDI_ON_SERVER , config);
        }
    	container.addContainerProvidedComponent( RaplaExtensionPoints.PLUGIN_OPTION_PANEL_EXTENSION,JNDIOption.class.getName(),JNDIPlugin.class.getName(), config);
    	   
    	if ( !config.getAttributeAsBoolean("enabled", false) )
        	return;

        container.addContainerProvidedComponent( AuthenticationStore.class.getName(), JNDIAuthenticationStore.class.getName(), PLUGIN_CLASS, config);
    }

    public Object getPluginMetaInfos( String key )
    {
        return null;
    }

    
    
}

