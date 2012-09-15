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
package org.rapla.gui;

import java.awt.BorderLayout;
import java.util.Locale;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.rapla.entities.configuration.Preferences;
import org.rapla.entities.configuration.RaplaConfiguration;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.plugin.RaplaPluginMetaInfo;

abstract public class DefaultPluginOption extends RaplaGUIComponent implements OptionPanel {
    public DefaultPluginOption(RaplaContext sm) throws RaplaException {
        super(sm);
    }

    protected JCheckBox activate = new JCheckBox("Aktivieren");
    protected Configuration config;
    protected Preferences preferences;
    JComponent container;

    abstract public String getDescriptorClassName();
    
    protected JPanel createPanel() throws RaplaException {
        JPanel panel = new JPanel();
        panel.setLayout( new BorderLayout());
        panel.add( activate, BorderLayout.NORTH );
        return panel;
    }

    /**
     * @see org.rapla.gui.OptionPanel#setPreferences(org.rapla.entities.configuration.Preferences)
     */
    public void setPreferences(Preferences preferences) {
       this.preferences = preferences;
    }

    /**
     * @see org.rapla.gui.OptionPanel#commit()
     */
    public void commit() throws RaplaException {
        RaplaConfiguration config = (RaplaConfiguration) preferences.getEntry("org.rapla.plugin");
        String className = getDescriptorClassName();
        //getDescritorClassName()
        
        DefaultConfiguration newChild = new DefaultConfiguration("plugin" );
        newChild.setAttribute( "enabled", activate.isSelected());
        newChild.setAttribute( "class", className);
        addChildren( newChild );
        RaplaConfiguration newConfig = config.replace(config.find("class", className), newChild);
        preferences.putEntry( "org.rapla.plugin",newConfig);
    }
    
    protected void addChildren( DefaultConfiguration newConfig) {
    }

    protected void readConfig( Configuration config)   {
    	
    }


    /**
     * @see org.rapla.gui.OptionPanel#show()
     */
    public void show() throws RaplaException {
        activate.setText( getString("selected"));
        container = createPanel();
        RaplaConfiguration raplaConfig = (RaplaConfiguration)preferences.getEntry("org.rapla.plugin");
        if ( raplaConfig == null) { 
            throw new RaplaException("No PluginConfiguration found. Please try a reimport!"  );
        }
        config = raplaConfig.find( "class" ,getDescriptorClassName());
        if ( config == null) {
        	config = new DefaultConfiguration("plugin");
        }
        
        Boolean defaultValueAsBoolean = (Boolean) findDescriptor( getDescriptorClassName()).getPluginMetaInfos(RaplaPluginMetaInfo.METAINFO_PLUGIN_ENABLED_BY_DEFAULT);
        boolean defaultSelection = defaultValueAsBoolean != null ? defaultValueAsBoolean.booleanValue() : false;
        activate.setSelected( config.getAttributeAsBoolean("enabled", defaultSelection));
        readConfig( config );
    }


    /**
     * @see org.rapla.gui.toolkit.RaplaWidget#getComponent()
     */
    public JComponent getComponent() {
        return container;
    }



    /**
     * @see org.rapla.entities.Named#getName(java.util.Locale)
     */
    public String getName(Locale locale) {
        return getDescriptorClassName();
    }
    
       
}


