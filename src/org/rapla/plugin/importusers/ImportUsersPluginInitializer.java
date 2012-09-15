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
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;

import org.rapla.components.iolayer.FileContent;
import org.rapla.components.iolayer.IOInterface;
import org.rapla.components.util.Tools;
import org.rapla.entities.Category;
import org.rapla.entities.User;
import org.rapla.facade.ClientFacade;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.MenuExtensionPoint;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.toolkit.DialogUI;
import org.rapla.plugin.RaplaExtensionPoints;


public class ImportUsersPluginInitializer extends RaplaGUIComponent
{

    public ImportUsersPluginInitializer(RaplaContext sm) throws RaplaException {
        super(sm);
        //MenuExtensionPoint helpMenu = (MenuExtensionPoint) getService( RaplaExtensionPoints.HELP_MENU_EXTENSION_POINT);
        //helpMenu.insert(createInfoMenu() );
        if(!getUser().isAdmin())
        	return;
        setChildBundleName( ImportUsersPlugin.RESOURCE_FILE);
        MenuExtensionPoint importMenu = (MenuExtensionPoint) getService( RaplaExtensionPoints.IMPORT_MENU_EXTENSION_POINT);
        importMenu.insert( createImportMenu());

        //MenuExtensionPoint export = (MenuExtensionPoint) getService( RaplaExtensionPoints.EXPORT_MENU_EXTENSION_POINT);
        //export.insert(createExportMenu() );
    }


    private JMenuItem createImportMenu( ) {
        JMenuItem item = new JMenuItem( "Users from CSV" );
        item.setIcon( getIcon("icon.import") );
        item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    try {
                        final Frame frame = (Frame) SwingUtilities.getRoot(getMainComponent());
                        IOInterface io =  getService( IOInterface.class);
                        FileContent file = io.openFile( frame, null, new String[] {".csv"});
                        if ( file != null) {
                            Reader reader = new InputStreamReader( file.getInputStream());
                            String msg = ImportUsers( getClientFacade(), reader);
                            confirmImport(frame, msg);
                        }
                     } catch (Exception ex) {
                        showException( ex, getMainComponent() );
                    }
                }
        });
        return item;
    }
    
	 private void confirmImport(final Component parentComponent, String msg) throws RaplaException {
		 DialogUI dialog2 = DialogUI.create(
                 getContext()
                 ,parentComponent
                 ,true
                 ,getString("info")
                 ,msg
                 ,new String[] {getString("back")}
        );
         dialog2.setIcon(getIcon("icon.info"));
         dialog2.setDefault(0);
         dialog2.start();
         return;
	    }
    
    public String ImportUsers(ClientFacade facade, Reader reader) throws IOException, RaplaException {
        String[][] entries = Tools.csvRead( reader, 5 ); // read first 5 colums per input row and store in memory
        //Category rootCategory = facade.getUserDefinedGroupsCategory();
        Category rootCategory = facade.getUserGroupsCategory();
        Category group = null;
        int errorCnt = 0;
        for ( int i=0;i<entries.length; i++ ) {
            String[] lineEntries = entries[i];
            String username = lineEntries[0].trim();
            String password = lineEntries[1].trim();
            String name 	= lineEntries[2].trim();
            String email 	= lineEntries[3].trim();
            String groupKey = lineEntries[4].trim();
            
			try {
				User user = facade.newUser();
	            user.setUsername( username );
	            user.setName ( name );
	            user.setEmail( email );
	            if(groupKey.length() != 0) {
		            group = findCategory( rootCategory, groupKey );
		            if (group != null) 
		                user.addGroup(  group );
		            else {
			            getLogger().info("KO-AddUser: " + username + " GroupKey " + groupKey + " not found.");
			            errorCnt++;
		            	continue;
		            }
	            }
	            facade.store(user);
	            facade.changePassword( user, new char[] {} ,password.toCharArray());
	            getLogger().info("OK-AddUser: " + username);
			} catch (RaplaException ex) {
	            getLogger().info("KO-AddUser: " + username + " Msg: " + ex.getMessage());
	            errorCnt++;
			}
        }
        return getI18n().format("checklog", errorCnt, entries.length);
    }
   
    private Category findCategory( Category rootCategory, String groupPath) {
        Category group = rootCategory;
        String[] groupKeys = Tools.split( groupPath, '/');
        for ( int i=0;i<groupKeys.length; i++) {
            group = group.getCategory( groupKeys[i]);
        }
        return group;
    }
}