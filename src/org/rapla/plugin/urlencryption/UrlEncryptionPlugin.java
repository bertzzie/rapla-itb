package org.rapla.plugin.urlencryption;

import org.apache.avalon.framework.configuration.Configuration;
import org.rapla.components.xmlbundle.I18nBundle;
import org.rapla.components.xmlbundle.impl.I18nBundleImpl;
import org.rapla.framework.Container;
import org.rapla.framework.PluginDescriptor;
import org.rapla.plugin.RaplaExtensionPoints;
import org.rapla.plugin.RaplaPluginMetaInfo;
import org.rapla.server.ServerService;

/**
 * This plugin provides a service to secure the publishing function of a calendar by encrypting the source parameters.
 * This class initializes the Option panel for the administrator, the UrlEncryptionService on the server and the 
 * Server Stub on the Client for using the encryption service. 
 * 
 * @author Jonas Kohlbrenner
 * 
 */
public class UrlEncryptionPlugin
implements PluginDescriptor
{
	public static final String RESOURCE_FILE = UrlEncryptionPlugin.class.getPackage().getName() + ".UrlEncryption";
    public static final String PLUGIN_ENTRY = "org.rapla.plugin.urlencryption";
	public static final String URL_ENCRYPTION = PLUGIN_ENTRY+".selected";
    public static final String PLUGIN_CLASS = UrlEncryptionPlugin.class.getName();
    static final boolean ENABLE_BY_DEFAULT = false;


    public String toString()
    {
        return "URL Encryption";
    }

	public void provideServices(Container container, Configuration configuration)
	{
		if(!configuration.getAttributeAsBoolean("enabled", ENABLE_BY_DEFAULT))
		{
			return;
		}

		// Adding option panel for the administrators
		container.addContainerProvidedComponent(
				RaplaExtensionPoints.PLUGIN_OPTION_PANEL_EXTENSION,
				UrlEncryptionOption.class.getName(),
				PLUGIN_CLASS,
				configuration);

        container.addContainerProvidedComponentInstance(PLUGIN_CLASS, Boolean.TRUE);

        container.addContainerProvidedComponent( I18nBundle.ROLE,
                I18nBundleImpl.class.getName(),
                RESOURCE_FILE,
                I18nBundleImpl.createConfig( RESOURCE_FILE ) );


        if(container.getContext().has(ServerService.ROLE))
		{
			// Adding Service implementation on Server
			container.addContainerProvidedComponent(
					UrlEncryption.ROLE,
					UrlEncryptionService.class.getName(),
                    UrlEncryption.ROLE,
					configuration);

            container.addContainerProvidedComponent(
                    RaplaExtensionPoints.SERVLET_REQUEST_RESPONSE_PREPROCESSING_POINT,
                    UrlEncryptionServletRequestResponsePreprocessor.class.getName(),
                    RaplaExtensionPoints.SERVLET_REQUEST_RESPONSE_PREPROCESSING_POINT,
                    configuration
                    );
		}
		else
		{
			// Adding remote Stub to Client
            container.addContainerProvidedComponent(
					UrlEncryption.ROLE,
					UrlEncryptionRemote.class.getName(),
                    PLUGIN_CLASS,
					configuration);

		}
	}
	
	public Object getPluginMetaInfos(String key)
	{
        if ( RaplaPluginMetaInfo.METAINFO_PLUGIN_ENABLED_BY_DEFAULT.equals( key )) {
            return new Boolean( ENABLE_BY_DEFAULT );
        }
        return null;
	}
	
}
