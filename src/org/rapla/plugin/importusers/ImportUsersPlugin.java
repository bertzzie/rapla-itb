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
package org.rapla.plugin.importusers;
import org.apache.avalon.framework.configuration.Configuration;
import org.rapla.components.xmlbundle.I18nBundle;
import org.rapla.components.xmlbundle.impl.I18nBundleImpl;
import org.rapla.framework.Container;
import org.rapla.framework.PluginDescriptor;
import org.rapla.plugin.RaplaExtensionPoints;
import org.rapla.plugin.RaplaPluginMetaInfo;


public class ImportUsersPlugin implements PluginDescriptor
{
	public static final String RESOURCE_FILE = ImportUsersPlugin.class.getPackage().getName() + ".ImportUsersResources";
    public static final String PLUGIN_CLASS = ImportUsersPlugin.class.getName();
    static boolean ENABLE_BY_DEFAULT = false;

    public String toString() {
        return "Import Users";
    }

    /**
     * @see org.rapla.framework.PluginDescriptor#provideServices(org.rapla.framework.general.Container)
     */
    public void provideServices(Container container, Configuration config) {
        if ( !config.getAttributeAsBoolean("enabled", ENABLE_BY_DEFAULT) )
        	return;

        container.addContainerProvidedComponent( I18nBundle.ROLE, I18nBundleImpl.class.getName(), RESOURCE_FILE,I18nBundleImpl.createConfig( RESOURCE_FILE ) );
        container.addContainerProvidedComponent( RaplaExtensionPoints.CLIENT_EXTENSION, ImportUsersPluginInitializer.class.getName(), PLUGIN_CLASS, config);
        //container.addContainerProvidedComponent( RaplaExtensionPoints.USER_OPTION_PANEL_EXTENSION, MyOption.class.getName(),PLUGIN_CLASS, config);

        
    }

    public Object getPluginMetaInfos( String key )
    {
        if ( RaplaPluginMetaInfo.METAINFO_PLUGIN_ENABLED_BY_DEFAULT.equals( key )) {
            return new Boolean( ENABLE_BY_DEFAULT );
        }
        return null;
    }
}