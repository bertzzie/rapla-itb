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
package org.rapla.client.internal;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.rapla.client.ClientService;
import org.rapla.client.RaplaClientListener;
import org.rapla.components.calendar.DateRenderer;
import org.rapla.components.iolayer.DefaultIO;
import org.rapla.components.iolayer.IOInterface;
import org.rapla.components.iolayer.WebstartIO;
import org.rapla.components.util.Mutex;
import org.rapla.components.xmlbundle.I18nBundle;
import org.rapla.components.xmlbundle.LocaleSelector;
import org.rapla.entities.RaplaObject;
import org.rapla.entities.User;
import org.rapla.entities.configuration.Preferences;
import org.rapla.entities.configuration.RaplaConfiguration;
import org.rapla.entities.configuration.internal.CalendarModelConfigurationImpl;
import org.rapla.entities.dynamictype.ClassificationFilter;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.storage.DynamicTypeDependant;
import org.rapla.facade.AllocationChangeListener;
import org.rapla.facade.ClientFacade;
import org.rapla.facade.ModificationEvent;
import org.rapla.facade.ModificationListener;
import org.rapla.facade.UpdateErrorListener;
import org.rapla.facade.internal.FacadeImpl;
import org.rapla.framework.PluginDescriptor;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaContextException;
import org.rapla.framework.RaplaException;
import org.rapla.framework.RaplaLocale;
import org.rapla.framework.StartupEnvironment;
import org.rapla.framework.internal.ContainerImpl;
import org.rapla.gui.CalendarModel;
import org.rapla.gui.EditController;
import org.rapla.gui.InfoFactory;
import org.rapla.gui.MenuExtensionPoint;
import org.rapla.gui.MenuFactory;
import org.rapla.gui.ReservationController;
import org.rapla.gui.TreeFactory;
import org.rapla.gui.internal.CalendarOption;
import org.rapla.gui.internal.ConnectionOption;
import org.rapla.gui.internal.LocaleOption;
import org.rapla.gui.internal.MainFrame;
import org.rapla.gui.internal.MenuFactoryImpl;
import org.rapla.gui.internal.RaplaDateRenderer;
import org.rapla.gui.internal.RaplaStartOption;
import org.rapla.gui.internal.UserOption;
import org.rapla.gui.internal.action.ToolTipAction;
import org.rapla.gui.internal.common.CalendarModelImpl;
import org.rapla.gui.internal.common.CalendarSelectionModel;
import org.rapla.gui.internal.common.InternMenus;
import org.rapla.gui.internal.common.RaplaClipboard;
import org.rapla.gui.internal.edit.EditControllerImpl;
import org.rapla.gui.internal.edit.reservation.ReservationControllerImpl;
import org.rapla.gui.internal.view.InfoFactoryImpl;
import org.rapla.gui.internal.view.LicenseInfoUI;
import org.rapla.gui.internal.view.TreeFactoryImpl;
import org.rapla.gui.toolkit.ErrorDialog;
import org.rapla.gui.toolkit.FrameControllerList;
import org.rapla.gui.toolkit.MenuInterface;
import org.rapla.gui.toolkit.RaplaFrame;
import org.rapla.gui.toolkit.RaplaMenu;
import org.rapla.gui.toolkit.RaplaMenubar;
import org.rapla.gui.toolkit.RaplaSeparator;
import org.rapla.plugin.RaplaExtensionPoints;

/** Implemention of the ClientService.
*/
public class RaplaClientServiceImpl extends ContainerImpl implements ClientService,UpdateErrorListener
{
    Vector<RaplaClientListener> listenerList = new Vector<RaplaClientListener>();
    LoadingProgress progress;
    I18nBundle i18n;
    ClientFacade facade;
    boolean started;
    boolean restartingGUI;
    String facadeName;
    boolean defaultLanguageChoosen;
    FrameControllerList frameControllerList;
    Configuration config;

    public RaplaClientServiceImpl(RaplaContext parentContext, Configuration config) throws RaplaException {
        super(parentContext, config);
        this.config = config;
    }

    class DefaultMenuInsertPoint implements MenuExtensionPoint {
        MenuInterface menu;
        String insertId;
        public  DefaultMenuInsertPoint( MenuInterface menu, String insertId) {
            this.menu = menu;
            this.insertId = insertId;
        }
        public void insert( JMenuItem item )
        {
            menu.insertAfterId( item, insertId );
        }

        public void insert( JSeparator seperator )
        {
            menu.insertAfterId( seperator,insertId );
        }

    }

    protected void init() throws RaplaException {
        defaultLanguageChoosen = true;
        
        //      !!Warning: Theres a trivial patent for the use of Progress Bars see http://swpat.ffii.org/patents/txt/ep/0394/160/
        // You can uncomment the following two lines to disable the progressBar
        progress = new LoadingProgress();
        progress.start(1,5);

        super.init( );

        facadeName = m_config.getChild("facade").getValue("*");

        addContainerProvidedComponent( "org.rapla.gui.WelcomeField", LicenseInfoUI.class.getName()  );
        addContainerProvidedComponent( MAIN_COMPONENT, RaplaFrame.class.getName());
            
        addContainerProvidedComponent( RaplaClipboard.ROLE, RaplaClipboard.class.getName() );
        addContainerProvidedComponent( TreeFactory.class.getName(), TreeFactoryImpl.class.getName() );
        addContainerProvidedComponent( MenuFactory.ROLE, MenuFactoryImpl.class.getName() );
        addContainerProvidedComponent( InfoFactory.ROLE, InfoFactoryImpl.class.getName() );
        addContainerProvidedComponent( EditController.ROLE, EditControllerImpl.class.getName() );
        addContainerProvidedComponent( ReservationController.ROLE, ReservationControllerImpl.class.getName() );
        addContainerProvidedComponent( DateRenderer.class.getName(), RaplaDateRenderer.class.getName() );
        addContainerProvidedComponent ( RaplaExtensionPoints.USER_OPTION_PANEL_EXTENSION, UserOption.class.getName() );
        addContainerProvidedComponent ( RaplaExtensionPoints.USER_OPTION_PANEL_EXTENSION, CalendarOption.class.getName() );
        addContainerProvidedComponent ( RaplaExtensionPoints.USER_OPTION_PANEL_EXTENSION, LocaleOption.class.getName() );
        addContainerProvidedComponent ( RaplaExtensionPoints.SYSTEM_OPTION_PANEL_EXTENSION, CalendarOption.class.getName() );
        addContainerProvidedComponent ( RaplaExtensionPoints.SYSTEM_OPTION_PANEL_EXTENSION, RaplaStartOption.class.getName() );   

        frameControllerList = new FrameControllerList();
        frameControllerList.enableLogging(getLogger().getChildLogger("framelist"));
        addContainerProvidedComponentInstance(FrameControllerList.ROLE,frameControllerList);

        RaplaMenubar menuBar = new RaplaMenubar();

        RaplaMenu systemMenu =  new RaplaMenu( InternMenus.FILE_MENU_ROLE );
        RaplaMenu editMenu = new RaplaMenu( InternMenus.EDIT_MENU_ROLE );
        RaplaMenu viewMenu = new RaplaMenu( InternMenus.VIEW_MENU_ROLE );
        RaplaMenu helpMenu = new RaplaMenu( InternMenus.EXTRA_MENU_ROLE );

        RaplaMenu newMenu = new RaplaMenu( InternMenus.NEW_MENU_ROLE );
        RaplaMenu settingsMenu = new RaplaMenu( InternMenus.CALENDAR_SETTINGS);
        RaplaMenu adminMenu = new RaplaMenu( InternMenus.ADMIN_MENU_ROLE );
        RaplaMenu importMenu = new RaplaMenu( InternMenus.IMPORT_MENU_ROLE);
        RaplaMenu exportMenu = new RaplaMenu( InternMenus.EXPORT_MENU_ROLE);
        
        menuBar.add( systemMenu );
        menuBar.add( editMenu );
        menuBar.add( viewMenu );
        menuBar.add( helpMenu );

        
        addContainerProvidedComponentInstance( SESSION_MAP, new HashMap<Object,Object>());
        addContainerProvidedComponentInstance( RaplaExtensionPoints.ADMIN_MENU_EXTENSION_POINT, new DefaultMenuInsertPoint( adminMenu, null));
        addContainerProvidedComponentInstance( RaplaExtensionPoints.IMPORT_MENU_EXTENSION_POINT, new DefaultMenuInsertPoint( importMenu, null));
        addContainerProvidedComponentInstance( RaplaExtensionPoints.EXPORT_MENU_EXTENSION_POINT, new DefaultMenuInsertPoint( exportMenu, null));
        addContainerProvidedComponentInstance( RaplaExtensionPoints.HELP_MENU_EXTENSION_POINT, new DefaultMenuInsertPoint( helpMenu, null));
        addContainerProvidedComponentInstance( RaplaExtensionPoints.VIEW_MENU_EXTENSION_POINT, new DefaultMenuInsertPoint( viewMenu, null));
        addContainerProvidedComponentInstance( RaplaExtensionPoints.EDIT_MENU_EXTENSION_POINT, new DefaultMenuInsertPoint( editMenu, "EDIT_END"));

        addContainerProvidedComponentInstance( InternMenus.MENU_BAR,  menuBar);
        addContainerProvidedComponentInstance( InternMenus.FILE_MENU_ROLE, systemMenu );
        addContainerProvidedComponentInstance( InternMenus.EDIT_MENU_ROLE,  editMenu);
        addContainerProvidedComponentInstance( InternMenus.VIEW_MENU_ROLE,  viewMenu);
        addContainerProvidedComponentInstance( InternMenus.ADMIN_MENU_ROLE,  adminMenu);
        addContainerProvidedComponentInstance( InternMenus.IMPORT_MENU_ROLE, importMenu );
        addContainerProvidedComponentInstance( InternMenus.EXPORT_MENU_ROLE, exportMenu );
        addContainerProvidedComponentInstance( InternMenus.NEW_MENU_ROLE, newMenu );
        addContainerProvidedComponentInstance( InternMenus.CALENDAR_SETTINGS, settingsMenu );
        addContainerProvidedComponentInstance( InternMenus.EXTRA_MENU_ROLE, helpMenu );

        editMenu.add( new RaplaSeparator("EDIT_BEGIN"));
        editMenu.add( new RaplaSeparator("EDIT_END"));

        
        boolean webstartEnabled =((StartupEnvironment)getContext().lookup(StartupEnvironment.class.getName())).getStartupMode() == StartupEnvironment.WEBSTART;

        if (webstartEnabled) {
            addContainerProvidedComponent( IOInterface.class.getName(),WebstartIO.class.getName() );
        } else {
            addContainerProvidedComponent( IOInterface.class.getName(),DefaultIO.class.getName() );
        }

        if ( progress != null)
        {
            progress.advance();
        }
    }

    public ClientFacade getFacade() {
        return facade;
    }

    public void start() throws Exception {
        if (started)
            return;

        try {
            if (progress != null)
                progress.advance();

            getLogger().debug("RaplaClient started");
            i18n = (I18nBundle) getContext().lookup(I18nBundle.ROLE );
            facade = (ClientFacade) getContext().lookup(ClientFacade.ROLE + "/" + facadeName);
            facade.addUpdateErrorListener(this);

            if (progress != null)
                progress.advance();

            if (!facade.isSessionActive()) {
                startLogin();
            } else {
                beginRaplaSession();
            }
        } catch (Exception ex) {
            throw ex;
        } finally {
            if (progress != null)
                progress.close();
        }
    }

    /**
     * @throws RaplaException
     *
     */
    private void beginRaplaSession() throws RaplaException {
        initLanguage();
         
        final CalendarModelImpl model = createCalendarModel();
        addContainerProvidedComponentInstance( CalendarModel.class.getName(), model );
        addContainerProvidedComponentInstance( CalendarSelectionModel.class.getName(), model );
        ((FacadeImpl)getFacade()).addDirectModificationListener( new ModificationListener() {
			
			public boolean isInvokedOnAWTEventQueue() {
				return false;
			}

			public void dataChanged(ModificationEvent evt) throws RaplaException {
				if ( evt.isModified(DynamicType.TYPE))
				{
					boolean needsChange = false;
					for (RaplaObject obj: evt.getChanged())
					{
						if ( obj.getRaplaType() == DynamicType.TYPE)
						{
							int modelRuleCount = 0;
							for (ClassificationFilter filter:model.getReservationFilter())
							{
								modelRuleCount += filter.ruleSize();
							}
							for (ClassificationFilter filter:model.getAllocatableFilter())
							{
								modelRuleCount += filter.ruleSize();
							}
			
							
							CalendarModelConfigurationImpl calendarConfig = (CalendarModelConfigurationImpl)model.createConfiguration();
							DynamicType type = (DynamicType) obj;
							int ruleCount = 0;
							for (ClassificationFilter filter:calendarConfig.getFilter())
							{
								ruleCount += filter.ruleSize();
							}
							if (calendarConfig.needsChange(type) || modelRuleCount != ruleCount)
							{
								needsChange =true;
							}
						}	
					}
					if ( needsChange)
					{
						// We need to reload the model when the dynamictype is modified because the values can be changed
						model.load(null);
					}
				}
				
			}
        });
        if ( getFacade().isClientForServer() )
        {
            addContainerProvidedComponent ( RaplaExtensionPoints.SYSTEM_OPTION_PANEL_EXTENSION, ConnectionOption.class.getName() );
        } 
        
        List<PluginDescriptor> pluginList;
        try {
            Object lookup = getContext().lookup( PluginDescriptor.PLUGIN_LIST);
			pluginList = (List<PluginDescriptor>) lookup;
        } catch (RaplaContextException ex) {
            throw new RaplaException (ex );
        }
        initializePlugins( pluginList, getFacade().getPreferences( null) );
    
        started = true;
        User user = model.getUser();
        boolean showToolTips = getFacade().getPreferences( user ).getEntryAsBoolean( ToolTipAction.CONFIG_ENTRY, true);
        javax.swing.ToolTipManager.sharedInstance().setEnabled(showToolTips);
        //javax.swing.ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);
        javax.swing.ToolTipManager.sharedInstance().setInitialDelay( 1000 );
        javax.swing.ToolTipManager.sharedInstance().setDismissDelay( 10000 );
        javax.swing.ToolTipManager.sharedInstance().setReshowDelay( 0 );
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);

        RaplaFrame mainComponent = (RaplaFrame) getContext().lookup( MAIN_COMPONENT );
        mainComponent.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                try {
                    if ( !isRestartingGUI()) {
                        stop();
                    } else {
                        restartingGUI = false;
                    }
                } catch (Exception ex) {
                    getLogger().error(ex.getMessage(),ex);
                }
            }
    
        });
        MainFrame mainFrame = new MainFrame( getContext());
    
        fireClientStarted();
        mainFrame.show();
    }

    private void initLanguage() throws RaplaException, RaplaContextException
    {
        if ( !defaultLanguageChoosen)
        {
            Preferences prefs = (Preferences)facade.edit(facade.getPreferences());
            RaplaLocale raplaLocale = (RaplaLocale) getContext().lookup(RaplaLocale.ROLE );
            String currentLanguage = raplaLocale.getLocale().getLanguage();
            prefs.putEntry( RaplaLocale.LANGUAGE_ENTRY, currentLanguage);
            try
            {
                facade.store( prefs);
            }
            catch (Exception e)
            {
                getLogger().error("Can't  store language change", e);
            }
        }
        else
        {
            String language = facade.getPreferences().getEntryAsString( RaplaLocale.LANGUAGE_ENTRY, null);
            if ( language != null)
            {
                LocaleSelector localeSelector = (LocaleSelector) getContext().lookup(LocaleSelector.ROLE);
                localeSelector.setLanguage( language );
            }
        }
    }

    protected void initializePlugins(List<PluginDescriptor> pluginList, Preferences preferences) throws RaplaException {
        RaplaConfiguration raplaConfig =(RaplaConfiguration)preferences.getEntry("org.rapla.plugin");
        // Add plugin configs
        for ( Iterator<PluginDescriptor> it = pluginList.iterator(); it.hasNext();  ) {
            PluginDescriptor pluginDescriptor = (PluginDescriptor)it.next();
            String pluginClassname = pluginDescriptor.getClass().getName();
            Configuration pluginConfig = null;
            if ( raplaConfig != null) {
                pluginConfig = raplaConfig.find("class", pluginClassname);
            }
            if ( pluginConfig == null) {
                pluginConfig = new DefaultConfiguration("plugin");
            }
            pluginDescriptor.provideServices( this, pluginConfig );
        }


        Collection<?> clientPlugins = getAllServicesFor(RaplaExtensionPoints.CLIENT_EXTENSION);
        // start plugins
        for (Iterator<?> it = clientPlugins.iterator();it.hasNext();) {
            String hint = (String) it.next();
            try {
                getContext().lookup( RaplaExtensionPoints.CLIENT_EXTENSION  + "/" + hint);
                getLogger().info( "Initialize " + hint );
            } catch (RaplaContextException ex ) {
                getLogger().error( "Can't initialize " + hint, ex );
            }
        }
    }

    public boolean isRestartingGUI() {
        return restartingGUI;
    }

    public void addRaplaClientListener(RaplaClientListener listener) {
        listenerList.add(listener);
    }

    public void removeRaplaClientListener(RaplaClientListener listener) {
        listenerList.remove(listener);
    }

    public RaplaClientListener[] getRaplaClientListeners() {
        return (RaplaClientListener[])listenerList.toArray(new RaplaClientListener[]{});
    }

    protected void fireClientClosed(boolean restart) {
        RaplaClientListener[] listeners = getRaplaClientListeners();
        for (int i=0;i<listeners.length;i++)
            listeners[i].clientClosed(restart);
    }

    protected void fireClientStarted() {
        RaplaClientListener[] listeners = getRaplaClientListeners();
        for (int i=0;i<listeners.length;i++)
            listeners[i].clientStarted();
    }
    
    protected void fireClientAborted() {
        RaplaClientListener[] listeners = getRaplaClientListeners();
        for (int i=0;i<listeners.length;i++)
            listeners[i].clientAborted();
    }

    public boolean isRunning() {
        return started;
    }

    public void restartGUI() {
        started = false;
        try {
            restartingGUI = true;
            frameControllerList.closeAll();
            removeAllComponents();
            init();
            start();
        } catch (Exception ex) {
            getLogger().error( ex.getMessage(), ex);
            stop();
        }
    }

    public void stop() {
        stop( false );
    }

    public void stop(boolean restart) {
        if (!started)
            return;

        try {
            facade.logout();
        } catch (RaplaException ex) {
            getLogger().error("Clean logout failed",ex);
        }
        started = false;
        fireClientClosed(restart);
    }

    public void dispose() {
        if (frameControllerList != null)
            frameControllerList.closeAll();
        super.dispose();
        getLogger().debug("RaplaClient disposed");
    }


    private void startLogin()  throws Exception {
        Thread loginThread = new Thread() {
            public void run() {
                startLoginInThread();
            }
        };
        loginThread.setDaemon( false );
        loginThread.start();
    }

    private void startLoginInThread()  {
        final Mutex loginMutex = new Mutex();
        try {
            final LanguageChooser languageChooser = new LanguageChooser( getLogger(), getContext());
            
            final LoginDialog dlg = LoginDialog.create(getContext(), languageChooser.getComponent());
            
            Action languageChanged = new AbstractAction()
            {
                private static final long serialVersionUID = 1L;

                public void actionPerformed(ActionEvent evt) {
                    try {
                        String lang = languageChooser.getSelectedLanguage();
                        if (lang == null)
                        {
                            defaultLanguageChoosen = true;
                        }
                        else
                        {
                            defaultLanguageChoosen = false;
                            getLogger().debug("Language changing to " + lang );
                            LocaleSelector localeSelector = (LocaleSelector) getContext().lookup( LocaleSelector.ROLE );
                            localeSelector.setLanguage(lang);
                            getLogger().info("Language changed " + localeSelector.getLanguage() );
                        }
                    } catch (Exception ex) {
                        getLogger().error("Can't change language",ex);
                    }
                }
                
            };
            languageChooser.setChangeAction( languageChanged);
            
            //dlg.setIcon( i18n.getIcon("icon.rapla-small"));
            if (progress != null)
                progress.close();
            Action loginAction = new AbstractAction() {
                private static final long serialVersionUID = 1L;

                public void actionPerformed(ActionEvent evt) {
                    String username = dlg.getUsername();
                    char[] password = dlg.getPassword();
                    boolean success = false;
                    try {
                        success = login(username,password);
                        if ( !success )
                        {
                            dlg.resetPassword();
                            showWarning(i18n.getString("error.login"), dlg);
                        }
                    } 
                    catch (RaplaException ex) 
                    {
                        dlg.resetPassword();
                        showException(ex,dlg);
                    }
                    if ( success) {
                        dlg.close();
                        loginMutex.release();
                        try {
                            beginRaplaSession();
                        } catch (RaplaException ex) {
                            showException(ex, null);
                            stop();
                            fireClientAborted();
                        }
                    } // end of else
                }
                
            };
            Action exitAction = new AbstractAction() {
                private static final long serialVersionUID = 1L;
                public void actionPerformed(ActionEvent evt) {
                    dlg.close();
                    loginMutex.release();
                    stop();
                    fireClientAborted();
                }
            };
            loginAction.putValue(Action.NAME,i18n.getString("login"));
            exitAction.putValue(Action.NAME,i18n.getString("exit"));
            dlg.setIconImage(i18n.getIcon("icon.rapla_small").getImage());
            dlg.setLoginAction( loginAction);
            dlg.setExitAction( exitAction );
            //dlg.setSize( 480, 270);
            FrameControllerList.centerWindowOnScreen( dlg) ;
            dlg.setVisible( true );

            loginMutex.aquire();
        } catch (Exception ex) {
            getLogger().error("Error during Login ", ex);
            stop();
            fireClientAborted();
        } finally {
            loginMutex.release();
        }
    }

    public void updateError(RaplaException ex) {
        getLogger().error("Error updating data", ex);
    }

    /**
     * @see org.rapla.facade.UpdateErrorListener#disconnected()
     */
    public void disconnected() {
        if ( started )
            stop( true );
    }


    public void restart()  {
        stop( true);
    }

    public void showException(Exception ex,Component component) 
    {
        try {
            ErrorDialog dialog = new ErrorDialog(getContext());
            dialog.showExceptionDialog(ex,component);
        } catch (RaplaException ex2) {
            getLogger().error(ex2.getMessage(),ex2);
        }
    }

    private boolean login(String username, char[] password) throws RaplaException 
    {
        if (facade.login(username,password)) {
            return true;
        } else {
            return false;
        }
    }

    public void showWarning(String warning,Component owner) 
    {
        try {
            ErrorDialog dialog = new ErrorDialog(getContext());
            dialog.showWarningDialog(warning,owner);
        } catch (RaplaException ex2) {
            getLogger().error(ex2.getMessage(),ex2);
        }
    }


    private CalendarModelImpl createCalendarModel() throws RaplaException {
        User user = getFacade().getUser();
        CalendarModelImpl model = new CalendarModelImpl( getContext(),user);
        model.load( null );
        return model;
    }

}
