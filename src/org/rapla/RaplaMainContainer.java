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
package org.rapla;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.logger.Logger;
import org.rapla.components.util.IOUtil;
import org.rapla.components.xmlbundle.I18nBundle;
import org.rapla.components.xmlbundle.LocaleSelector;
import org.rapla.components.xmlbundle.impl.I18nBundleImpl;
import org.rapla.entities.domain.AppointmentFormater;
import org.rapla.framework.*;
import org.rapla.framework.internal.ComponentInfo;
import org.rapla.framework.internal.ConfigTools;
import org.rapla.framework.internal.ContainerImpl;
import org.rapla.framework.internal.LogManagerAdapter;
import org.rapla.gui.CalendarOptions;
import org.rapla.gui.CalendarOptionsImpl;
import org.rapla.server.ShutdownService;
import org.rapla.server.internal.ShutdownServiceImpl;
/**
The Rapla Main Container class for the basic container for Rapla specific services and the rapla plugin architecture.
The rapla container has only one instance at runtime. Configuration of the RaplaMainContainer is done in the rapla*.xconf
files. Typical configurations of the MainContainer are

 <ol>
 <li>Standalone (rapla.xconf): A ClientService, one facade (with preconfigured auto admin login) and a storage (file).</li>
 <li>Client (raplaclient.xconf ): A ClientService, one facade (without auto login) and a remote storage ( automaticaly pointing to the download server in webstart mode)</li>
 <li>Server (raplaserver.xconf ): A ServerService (providin a facade) a messaging server for handling the connections with the clients, a storage (file or db) and an extra service for importing and exporting in the db</li>
 <li>Embedded: Configuration example follows.</li>
 </ol>
<p>
The Main Container provides the following Services to all RaplaComponents
<ul>
<li>I18nBundle</li>
<li>AppointmentFormater</li>
<li>RaplaLocale</li>
<li>LocaleSelector</li>
<li>RaplaMainContainer.PLUGIN_LIST (A list of all available plugins)</li>
</ul>
</p>

  @see I18nBundle
  @see RaplaLocale
  @see AppointmentFormater
  @see LocaleSelector
 */
final public class RaplaMainContainer extends ContainerImpl
{
    public RaplaMainContainer() throws Exception {
        this(new RaplaStartupEnvironment());
    }

    public RaplaMainContainer(RaplaStartupEnvironment env) throws Exception {
        this( createRaplaContext(env,env.getLoggerConfig()),env  );
    }

    public RaplaMainContainer(StartupEnvironment env) throws Exception {
        this( createRaplaContext(env, null),env  );
    }

    RaplaMainContainer( RaplaContext context, StartupEnvironment env) throws Exception{
        super( context, env.getStartupConfiguration() );
        addContainerProvidedComponentInstance( StartupEnvironment.class.getName(), env);
        addContainerProvidedComponentInstance( "download-server", env.getDownloadURL().getHost());
        addContainerProvidedComponentInstance( "download-url",  env.getDownloadURL());
        if (env.getContextRootURL() != null)
        {
            addContainerProvidedComponentInstance( "context-root", IOUtil.getFileFrom( env.getContextRootURL()));
            addContainerProvidedComponentInstance( "timestamp", new Object() {
				
				public String toString() {
				    DateFormat formatter = new SimpleDateFormat("yyyyMMdd-HHmmss");
		            String formatNow = formatter.format(new Date());
					return formatNow;
				}

			});
        }
        initialize();
        
    }


    private static RaplaContext createRaplaContext( StartupEnvironment env, Configuration loggerConfig ) throws RaplaException
    {
        Logger logger = env.getBootstrapLogger();
        LogManagerAdapter logManager = null;
        if ( loggerConfig != null ) {
            if ( checkForAvalonLogManager()) {
                logManager = new RaplaLogKitAdapater( env, loggerConfig );
                // Replace bootstrap logger with new logkit logger
                logger = logManager.getDefaultLogger();
            } else {
                logger.warn("avalon-logging.jar not found. Using bootstrap logger.");
            }
        }

        RaplaDefaultContext context = new RaplaDefaultContext();
        if ( logManager != null)
        {
            context.put("logger-manager", logManager);
        }
        context.put( Logger.class.getName(),  logger);
        final Logger loggerToUse = logger;
        int startupMode = env.getStartupMode();
		if ( startupMode != StartupEnvironment.APPLET && startupMode != StartupEnvironment.WEBSTART)
        {
			try
			{
	        	Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
	        		public void uncaughtException(Thread t, Throwable e) {
	        			loggerToUse.error("uncaught exception", e);
	        		}
	        	});
			}
			catch (Throwable ex)
			{
				logger.error("Can't set default exception handler-", ex);
			}
        }
        return context;
    }

    private static boolean checkForAvalonLogManager()
    {
        try {
            Class.forName("org.apache.avalon.excalibur.logger.LogKitLoggerManager");
            return true;
        } catch (Throwable ex ) {
            return false;
        }
    }

    protected Map<String,ComponentInfo> getComponentInfos() {
        return new RaplaMetaConfigInfo();

    }

    private void initialize() throws Exception {
        addContainerProvidedComponent( ShutdownService.ROLE, ShutdownServiceImpl.class.getName());
        
        RaplaLocaleImpl raplaLocale = new RaplaLocaleImpl(m_config.getChild("locale"), getLogger());

        CalendarOptions calendarOptions = new CalendarOptionsImpl(m_config.getChild("locale"));
        addContainerProvidedComponentInstance( CalendarOptions.ROLE, calendarOptions );

        // Startup mode= EMBEDDED = 0, CONSOLE = 1, WEBSTART = 2, APPLET = 3, SERVLET = 4
        getLogger().info("----------- Rapla startup mode = " +  getStartupEnvironment().getStartupMode());
        getLogger().info("Default Locale= " + Locale.getDefault().toString());
        getLogger().info("Configured Locale= " + raplaLocale.getLocaleSelector().getLocale().toString());
        addContainerProvidedComponentInstance( RaplaLocale.ROLE,raplaLocale);
        addContainerProvidedComponentInstance( LocaleSelector.ROLE,raplaLocale.getLocaleSelector());

        Configuration[] defaultBundles = m_config.getChildren("default-bundle");
        for ( int i=0;i< defaultBundles.length;i++) {
        	String defaultBundleName = defaultBundles[i].getValue();
            addContainerProvidedComponent
            (
             I18nBundle.ROLE
             ,I18nBundleImpl.class.getName()
             ,defaultBundleName
             ,I18nBundleImpl.createConfig( defaultBundleName )
             );
        }

        addContainerProvidedComponent
        (
         I18nBundle.ROLE
         ,I18nBundleImpl.class.getName()
         ,"org.rapla.RaplaResources"
         ,I18nBundleImpl.createConfig( "org.rapla.RaplaResources" )
         );

        addContainerProvidedComponentInstance( AppointmentFormater.ROLE, new AppointmentFormaterImpl( getContext()));

        // Override the intern Resource Bundle with user provided
        for ( int i=0;i< defaultBundles.length;i++) {
        	String defaultBundleName = defaultBundles[i].getValue();
        	I18nBundleImpl i18n = (I18nBundleImpl)getContext().lookup( I18nBundle.ROLE + "/" + defaultBundleName);
            String parentId = i18n.getParentId();
            if ( parentId != null) {
            	addContainerProvidedComponentInstance(I18nBundle.ROLE,parentId, i18n);
            }
        }

        // Discover and register the plugins for Rapla
        
        Enumeration<URL> pluginEnum =  ConfigTools.class.getClassLoader().getResources("META-INF/rapla-plugin.list");
        Set<String> pluginNames = new LinkedHashSet<String>();
        if (!pluginEnum.hasMoreElements())
        { 
        	URL mainDir = getClass().getResource("/");
        	if ( mainDir != null)
        	{
             	
        		String classpath = System.getProperty("java.class.path");
        		final String[] split;
        		if (classpath != null)
        		{
        			split = classpath.split(""+File.pathSeparatorChar);
        		}
        		else
        		{
        			split = new String[] { mainDir.toExternalForm()};
        		}
        		for ( String path: split)
        		{
        			File pluginPath = new File(path);
            		pluginNames.addAll(ServiceListCreator.findPluginClasses(pluginPath));
           	
        		}
        	}
        }
        	
        while ( pluginEnum.hasMoreElements() ) {
            BufferedReader reader = new BufferedReader(new InputStreamReader((pluginEnum.nextElement()).openStream()));
            while ( true ) {
                String plugin = reader.readLine();
                if ( plugin == null)
                    break;
                pluginNames.add(plugin);
            }
        }
        List<PluginDescriptor> pluginList = new ArrayList<PluginDescriptor>( );
        for ( String plugin:pluginNames)
        {
            try {
                pluginList.add((PluginDescriptor) instanciate(plugin, null, getLogger()));
                getLogger().info("Installed plugin "+plugin);
            } catch (RaplaContextException e) {
                if (e.getCause() instanceof ClassNotFoundException) {
                    getLogger().error("Could not instanciate plugin "+ plugin, e);
                }
            }
        }
        addContainerProvidedComponentInstance( PluginDescriptor.PLUGIN_LIST, pluginList);
        getLogger().info("Config=" + getStartupEnvironment().getConfigURL());
        getLogger().info("Config=" + getStartupEnvironment().getLoggerConfigURL());
        
        I18nBundleImpl m_i18n = (I18nBundleImpl)getContext().lookup(I18nBundle.ROLE +  "/org.rapla.RaplaResources");
        String version = m_i18n.getString( "rapla.version" );
        getLogger().info("Rapla.Version=" + version);
        version = m_i18n.getString( "rapla.build" );
        getLogger().info("Rapla.Build=" + version);
        try {
            version = System.getProperty("java.version");
            getLogger().info("Java.Version=" + version);
        } catch (SecurityException ex) {
            version = "-";
            getLogger().warn("Permission to system property java.version is denied!");
        }
    }

    public void dispose() {
        getLogger().info("Shutting down rapla-container"); //BJO
        super.dispose();
    }
 }

