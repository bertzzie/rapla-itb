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
import java.awt.Font;
import java.net.URL;
import java.util.Map;

import javax.swing.UIDefaults;
import javax.swing.UIManager;

import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.logger.ConsoleLogger;
import org.apache.avalon.framework.logger.Logger;
import org.rapla.client.ClientService;
import org.rapla.client.RaplaClientListenerAdapter;
import org.rapla.framework.Container;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.StartupEnvironment;
import org.rapla.framework.internal.ConfigTools;
import org.rapla.gui.images.Images;
import org.rapla.storage.ImportExportManager;

/** This class is used to start rapla from the command-line.
 <PRE>
 Usage :
 [-?|-c PATH_TO_CONFIG_FILE] [ACTION]
 Possible actions:
 rapla   : Starts the rapla-gui (this is the default)
 client  : Starts the rapla-gui in client/server mode
 </PRE>
 You can also display the list with the -? argument.
*/

final public class Main  {

    /** The default config filename for standalone-mode is rapla.xconf*/
    public final static String DEFAULT_CONFIG_NAME = "rapla.xconf";
    /** The default config filename for client-mode raplaclient.xconf*/
    public final static String DEFAULT_CLIENT_CONFIG_NAME = "raplaclient.xconf";

    public final static String DEFAULT_SERVER_CONFIG_NAME = "raplaserver.xconf";


    public static final String USAGE = new String (
        "Usage : \n"
        + "[-?|-c PATH_TO_CONFIG_FILE] [ACTION]\n"
        + "Possible actions:\n"
        + "  rapla   : Starts the rapla-gui (this is the default)\n"
        + "  client  : Starts the rapla-client (this is the default)\n"
        + "  import  : Import from file into the database\n"
        + "            See importexport entry in " + DEFAULT_SERVER_CONFIG_NAME + "\n"
        + "  export  : Export from database into file\n"
        + "            See importexport entry in " + DEFAULT_SERVER_CONFIG_NAME + "\n"
        );


    private Logger logger = new ConsoleLogger(ConsoleLogger.LEVEL_WARN).getChildLogger("init");
    RaplaStartupEnvironment env = new RaplaStartupEnvironment();
    Container raplaContainer;

    Main() {

    }

    void init(URL configURL,int mode) throws Exception {
        env.setStartupMode( mode );
        env.setConfigURL( configURL );
        env.setBootstrapLogger( getLogger() );
    }

    /** starts Rapla with the default controller-class: org.rapla.gui.usecase.defaults.MainController
     You can replace this with your custom controller (you need to add a plugin. See plugins for details).*/
    void startRapla() throws Exception {
        setLookandFeel();
        raplaContainer = new RaplaMainContainer( env);
        RaplaContext sm =  raplaContainer.getContext();
        ClientService client = (ClientService) sm.lookup(ClientService.class.getName());
        client.addRaplaClientListener(new RaplaClientListenerAdapter() {
                public void clientClosed(boolean restart) {
                    if ( restart) {
                        ContainerUtil.dispose( raplaContainer);
                        try {
                            startRapla();
                        } catch (Exception ex) {
                            getLogger().error("Error restarting client",ex);
                            exit();
                        }
                    } else {
                        exit();
                    }
                }
                public void clientAborted()
                {
                    exit();
                }
            });
    }

    private void setLookandFeel() {
        UIDefaults defaults = UIManager.getDefaults();
        Font textFont = defaults.getFont("Label.font");
        if ( textFont == null)
        {
        	textFont = new Font("SansSerif", Font.PLAIN, 12);
        } 
        else 
        {
        	textFont = textFont.deriveFont( Font.PLAIN );
        }
        defaults.put("Label.font", textFont);
        defaults.put("Button.font", textFont);
        defaults.put("Menu.font", textFont);
        defaults.put("MenuItem.font", textFont);
        defaults.put("RadioButton.font", textFont);
        defaults.put("CheckBoxMenuItem.font", textFont);
        defaults.put("CheckBox.font", textFont);
        defaults.put("ComboBox.font", textFont);
        defaults.put("Tree.expandedIcon",Images.getIcon("/org/rapla/gui/images/eclipse-icons/tree_minus.gif"));
        defaults.put("Tree.collapsedIcon",Images.getIcon("/org/rapla/gui/images/eclipse-icons/tree_plus.gif"));
        defaults.put("TitledBorder.font", textFont.deriveFont(Font.PLAIN,(float)10.));
        
    }
    
    private void exit() {
        ContainerUtil.dispose( raplaContainer);
        if (env.getStartupMode() != StartupEnvironment.APPLET)
            System.exit(0);
    }

    Logger getLogger() {
        return logger;
    }

    /** Read the data out of the dest-operator and write it into the source operator. The necessary
        properties should be specified in the config-file.
    */
    public void startExport() throws Exception {
        startExportImport(true);
    }
    
    /** Read the data out of the source-operator and write it into the dest operator. The necessary
        properties should be specified in the config-file.
     */
    public void startImport() throws Exception {
        startExportImport(false);
    }
    
    private void startExportImport(boolean export) throws Exception {
        raplaContainer = new RaplaMainContainer( env);
        RaplaContext sm =  raplaContainer.getContext();
        ImportExportManager conv =  (ImportExportManager) sm.lookup(ImportExportManager.ROLE);
        if (export)
            conv.doExport();
        else
            conv.doImport();
    }


    /** This is the entry point for the client-application.
     *  For a complete list of the arguments see the description
     *  of this class. */
    public static void main(String[] args) {
        Main main = new Main();
        Map<String,String> paramMap = ConfigTools.parseParams(args);
        if (paramMap == null) {
            System.out.println(USAGE);
            return;
        }
        String action =  paramMap.get("action");
        String config =   paramMap.get("config");
        if (action == null)
            action = "rapla";
        try {
            if ( action.equals("build-script")) {
                main.init( ConfigTools.configFileToURL(config, DEFAULT_CONFIG_NAME)
                           ,StartupEnvironment.CONSOLE);
                main.startRapla();
            } else if ( action.equals("webstart")) {
                main.init( ConfigTools.webstartConfigToURL(config, "webclient/" + DEFAULT_CLIENT_CONFIG_NAME)
                           ,StartupEnvironment.WEBSTART);
                main.startRapla();
            } else if ( action.equals("rapla")) {
                main.init( ConfigTools.configFileToURL(config, DEFAULT_CONFIG_NAME)
                           , StartupEnvironment.CONSOLE);
                main.startRapla();
                main.getLogger().info("Exit");
            } else if ( action.equals("client")) {
                URL configFileToURL = ConfigTools.configFileToURL(config,  DEFAULT_CLIENT_CONFIG_NAME);
			    main.init( configFileToURL
                           , StartupEnvironment.CONSOLE);
                main.startRapla();
                main.getLogger().info("Exit");
            } else if ( action.equals("import")) {
                main.init( ConfigTools.configFileToURL(config, DEFAULT_SERVER_CONFIG_NAME)
                        ,StartupEnvironment.CONSOLE);
                main.startImport();
            } else if ( action.equals("export")) {
                main.init( ConfigTools.configFileToURL(config, DEFAULT_SERVER_CONFIG_NAME) 
                    ,StartupEnvironment.CONSOLE);
                main.startExport();
            } else {
                System.out.println(USAGE);
            }
            
        } catch (Throwable ex) {
            main.getLogger().error("Couldn't start Rapla",ex);
            if (main != null)
                ContainerUtil.dispose(main.raplaContainer);
            System.out.flush();
            try
            {
                Thread.sleep( 2000 );
            }
            catch ( InterruptedException e )
            {
            }
            System.exit(1);
           
        }
    }
}
