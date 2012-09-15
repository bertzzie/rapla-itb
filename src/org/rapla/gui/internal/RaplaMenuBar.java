
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
package org.rapla.gui.internal;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;

import org.rapla.client.ClientService;
import org.rapla.entities.User;
import org.rapla.entities.configuration.Preferences;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.EditController;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.internal.action.RestartRaplaAction;
import org.rapla.gui.internal.action.RestartServerAction;
import org.rapla.gui.internal.action.ShowConflictsAction;
import org.rapla.gui.internal.action.ShowSelectionAction;
import org.rapla.gui.internal.action.ToolTipAction;
import org.rapla.gui.internal.action.user.UserAction;
import org.rapla.gui.internal.common.CalendarSelectionModel;
import org.rapla.gui.internal.common.InternMenus;
import org.rapla.gui.internal.print.PrintAction;
import org.rapla.gui.toolkit.DialogUI;
import org.rapla.gui.toolkit.HTMLView;
import org.rapla.gui.toolkit.RaplaFrame;
import org.rapla.gui.toolkit.RaplaMenu;
import org.rapla.gui.toolkit.RaplaMenuItem;
import org.rapla.gui.toolkit.RaplaSeparator;
import org.rapla.gui.toolkit.RaplaWidget;

public class RaplaMenuBar extends RaplaGUIComponent
{
    public RaplaMenuBar(RaplaContext context) throws RaplaException {
        super(context);
        JMenu systemMenu = (JMenu)getService( InternMenus.FILE_MENU_ROLE );
        systemMenu.setText(  getString("file"));

        JMenu editMenu = (JMenu)getService( InternMenus.EDIT_MENU_ROLE );
        editMenu.setText( getString("edit"));

        JMenu exportMenu = (JMenu)getService( InternMenus.EXPORT_MENU_ROLE );
        exportMenu.setText( getString("export"));

        JMenu importMenu = (JMenu)getService( InternMenus.IMPORT_MENU_ROLE );
        importMenu.setText( getString("import"));

        JMenuItem newMenu = (JMenuItem)getService( InternMenus.NEW_MENU_ROLE );
        newMenu.setText( getString("new"));

        JMenuItem calendarSettings = (JMenuItem)getService( InternMenus.CALENDAR_SETTINGS );
        calendarSettings.setText( getString("calendar"));
        
        JMenu extraMenu =  (JMenu)getService( InternMenus.EXTRA_MENU_ROLE);
        extraMenu.setText( getString("help"));

        JMenu adminMenu = (JMenu)getService( InternMenus.ADMIN_MENU_ROLE );
        adminMenu.setText(  getString("admin"));

        RaplaMenu viewMenu = (RaplaMenu)getService( InternMenus.VIEW_MENU_ROLE );
        viewMenu.setText( getString("view"));

        viewMenu.add( new RaplaSeparator("view_save"));
     
     

        systemMenu.add( newMenu);
        systemMenu.add( calendarSettings);
        
        systemMenu.add( new JSeparator());

        systemMenu.add( exportMenu );
        systemMenu.add( importMenu );
        systemMenu.add( adminMenu);

        
        JSeparator printSep = new JSeparator();
        printSep.setName(getString("calendar"));
        systemMenu.add( printSep);

        JMenuItem printMenu = new JMenuItem( getString("print"));
        PrintAction printAction =  new PrintAction(getContext());
        printMenu.setAction( printAction );
        printAction.setEnabled( true );
        CalendarSelectionModel model = getService(CalendarSelectionModel.class);
        printAction.setModel(model);
        systemMenu.add( printMenu );

        systemMenu.add( new JSeparator());

        if ( getUserModule().canSwitchBack() ) {
            JMenuItem switchBack = new JMenuItem();
            switchBack.setAction( new UserAction(getContext(),null,null).setSwitchToUser());
            adminMenu.add( switchBack );
        }

        boolean server = getUpdateModule().isClientForServer();
        if ( server  && isAdmin() ) {
            JMenuItem restartServer = new JMenuItem();
            restartServer.setAction( new RestartServerAction(getContext()));
            adminMenu.add( restartServer );
        }

        if ( isAdmin() )
        {
            JMenuItem restart = new JMenuItem();
            restart.setAction( new RestartRaplaAction(getContext()));
            adminMenu.add( restart );
        }

        systemMenu.setMnemonic('F');
        JMenuItem logout = new JMenuItem(getString("exit"));
        logout.setAccelerator( KeyStroke.getKeyStroke( KeyEvent.VK_Q, ActionEvent.CTRL_MASK ) );
        logout.setMnemonic('x');
        logout.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                RaplaFrame mainComponent = (RaplaFrame) getService( ClientService.MAIN_COMPONENT);
                mainComponent.close();
            }
        });
        systemMenu.add( logout );

        RaplaMenuItem userOptions = new RaplaMenuItem("userOptions");
        editMenu.add( userOptions );

        if ( isModifyPreferencesAllowed() ) {
            userOptions.setAction( createOptionAction( getQuery().getPreferences( )));
        } else {
            userOptions.setVisible( false );
        }
        
        final User user = getUser();
        final Preferences preferences = getQuery().getPreferences( user );
        {
            RaplaMenuItem menu = new RaplaMenuItem("show_tips");
            ToolTipAction action = new ToolTipAction( context );
            menu.setAction( action );
            boolean showToolTips = preferences.getEntryAsBoolean( ToolTipAction.CONFIG_ENTRY, true);
            if(showToolTips) {
                menu.setSelected(true);
                menu.setIcon(getIcon("icon.checked"));
            }
            else {
                menu.setSelected(false);
                menu.setIcon(getIcon("icon.unchecked"));
            }
            viewMenu.insertBeforeId( menu, "view_save" );
        }
        {
        	{
	        	RaplaMenuItem menu = new RaplaMenuItem("show_conflicts");
	            ShowConflictsAction action = new ShowConflictsAction( context );
	            menu.setAction( action );
	            boolean showConflicts = preferences.getEntryAsBoolean( ShowConflictsAction.CONFIG_ENTRY, true);
	            if(showConflicts) {
	                menu.setSelected(true);
	                menu.setIcon(getIcon("icon.checked"));
	            }
	            else {
	                menu.setSelected(false);
	                menu.setIcon(getIcon("icon.unchecked"));
	            }
	            viewMenu.insertBeforeId( menu, "view_save" );
        	}
        	{
	        	RaplaMenuItem menu = new RaplaMenuItem("show_selection");
	            ShowSelectionAction action = new ShowSelectionAction( context );
	            menu.setAction( action );
	            boolean showConflicts = preferences.getEntryAsBoolean( ShowSelectionAction.CONFIG_ENTRY, true);
	            if(showConflicts) {
	                menu.setSelected(true);
	                menu.setIcon(getIcon("icon.checked"));
	            }
	            else {
	                menu.setSelected(false);
	                menu.setIcon(getIcon("icon.unchecked"));
	            }
	            viewMenu.insertBeforeId( menu, "view_save" );

        	}
        }

        if ( isAdmin() ) {
        	RaplaMenuItem adminOptions = new RaplaMenuItem("adminOptions");
        	adminOptions.setAction( createOptionAction( getQuery().getPreferences( null )));
            adminMenu.add( adminOptions );
        }
        extraMenu.addSeparator();

        RaplaMenuItem info = new RaplaMenuItem("info");
        info.setAction( createInfoAction( getContext()));
        extraMenu.add( info );

		// within the help menu we need another point for the license
		RaplaMenuItem license = new RaplaMenuItem("license");
		// give this menu item an action to perform on click
		license.setAction(createLicenseAction(getContext()));
		// add the license dialog below the info entry
		extraMenu.add(license);        
        
        adminMenu.setEnabled( adminMenu.getMenuComponentCount() != 0 );
        exportMenu.setEnabled( exportMenu.getMenuComponentCount() != 0);
        importMenu.setEnabled( importMenu.getMenuComponentCount() != 0);
    }

    private Action createOptionAction( final Preferences preferences) {
        AbstractAction action = new AbstractAction() {
            private static final long serialVersionUID = 1L;
            
            public void actionPerformed(ActionEvent arg0) {
                try {
                	EditController editContrl =(EditController) getService(EditController.ROLE);
                    editContrl.edit( preferences, getMainComponent());
                } catch (RaplaException ex) {
                    showException( ex, getMainComponent());
                }
            }

        };
    	action.putValue( Action.SMALL_ICON, getIcon("icon.options") );
        action.putValue( Action.NAME, getString("options"));
        return action;
    }

    private Action createInfoAction( RaplaContext context) throws RaplaException {
        final String name = getString("info");
        final Icon icon = getIcon("icon.info_small");
        
        AbstractAction action = new AbstractAction() {
            private static final long serialVersionUID = 1L;
            
            public void actionPerformed( ActionEvent e )
            {
                try {
                    HTMLView infoText = new HTMLView();
                    infoText.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
                    String javaversion;
                    try {
                        javaversion = System.getProperty("java.version");
                    } catch (SecurityException ex) {
                        javaversion = "-";
                        getLogger().warn("Permission to system properties denied!");
                    }
                    
                    boolean isSigned = isSigned();
                    String signed = getString( isSigned ? "yes": "no");
                    
                    
                    infoText.setBody(getI18n().format("info.text",signed,javaversion));
                    DialogUI dialog = DialogUI.create( getContext(),getMainComponent(),true, new JScrollPane(infoText), new String[] {getString("ok")});
                    dialog.setTitle( name);
                    dialog.setSize( 550, 300);
                    dialog.startNoPack();
                } catch (RaplaException ex) {
                    showException( ex, getMainComponent());
                }
            }

           

        };
        action.putValue( Action.SMALL_ICON, icon );
        action.putValue( Action.NAME, name);
        return action;
    }
    
	/**
	 * the action to perform when someone clicks on the license entry in the
	 * help section of the menu bar
	 * 
	 * this method is a modified version of the existing method createInfoAction()
	 */
	private Action createLicenseAction(RaplaContext context) throws RaplaException
	{
		final String name = getString("licensedialog.title");
		final Icon icon = getIcon("icon.info_small");
		
		// overwrite the cass AbstractAction to design our own
		AbstractAction action = new AbstractAction()
		{
			private static final long	serialVersionUID	= 1L;
			
			// overwrite the actionPerformed method that is called on click
			public void actionPerformed(ActionEvent e)
			{
				try
				{
					// we need a new instance of HTMLView to visualize the short
					// version of the license text including the two links
					HTMLView licenseText = new HTMLView();
					// giving the gui element some borders
					licenseText.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
					// we look up the text was originally meant for the welcome field
					// and put it into a new instance of RaplaWidget
					RaplaWidget welcomeField = (RaplaWidget) getContext().lookup("org.rapla.gui.WelcomeField");
					// the following creates the dialog that pops up, when we click
					// on the license entry within the help section of the menu bar
					// we call the create Method of the DialogUI class and give it all necessary things
					DialogUI dialog = DialogUI.create(getContext(), getMainComponent(), true, new JScrollPane(welcomeField.getComponent()), new String[] { getString("ok") });
					// setting the dialog's title 
					dialog.setTitle(name);
					// and the size of the popup window
					dialog.setSize(550, 250);
					// but I honestly have no clue what this startNoPack() does
					dialog.startNoPack();
				}
				catch (RaplaException ex)
				{
					showException(ex, getMainComponent());
				}
			}
		};
		
		action.putValue(Action.SMALL_ICON, icon);
		action.putValue(Action.NAME, name);
		return action;
	}
	 
	
}



