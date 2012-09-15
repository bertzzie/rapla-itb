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
package org.rapla.plugin.archiver;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.rapla.components.calendar.RaplaNumber;
import org.rapla.components.layout.TableLayout;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.DefaultPluginOption;
import org.rapla.gui.OptionPanel;

public class ArchiverOption extends DefaultPluginOption implements OptionPanel {
   
    RaplaNumber dayField = new RaplaNumber(new Integer(25), new Integer(0),null,false);
    JCheckBox removeOlderYesNo = new JCheckBox();
    
    
    
    public ArchiverOption(RaplaContext sm) throws RaplaException {
        super(sm);
    }

    protected JPanel createPanel() throws RaplaException {
        JPanel panel = super.createPanel();
        JPanel content = new JPanel();
        double[][] sizes = new double[][] {
            {5,TableLayout.PREFERRED, 5,TableLayout.PREFERRED,5, TableLayout.PREFERRED}
            ,{TableLayout.PREFERRED,5,TableLayout.PREFERRED, 5, TableLayout.PREFERRED, 5, TableLayout.PREFERRED}
        };
        TableLayout tableLayout = new TableLayout(sizes);
        content.setLayout(tableLayout);
        content.add(new JLabel("Remove old events"), "1,0");
        content.add( removeOlderYesNo, "3,0");
        content.add(new JLabel("Older than"), "1,2");
        content.add( dayField, "3,2");
        content.add( new JLabel("days"), "5,2");
        removeOlderYesNo.addActionListener( new ActionListener(){

            public void actionPerformed( ActionEvent e )
            {
                dayField.setEnabled( removeOlderYesNo.isSelected());
            }
            
        });
        panel.add( content, BorderLayout.CENTER);
        return panel;
    }

        
    protected void addChildren( DefaultConfiguration newConfig) {
        if ( removeOlderYesNo.isSelected())
        {
            DefaultConfiguration smtpPort = new DefaultConfiguration("remove-older-than");
            smtpPort.setValue(dayField.getNumber().intValue() );
            newConfig.addChild( smtpPort );
        }
    }

    protected void readConfig( Configuration config)   {
        int days = config.getChild("remove-older-than").getValueAsInteger(-20);
        boolean isEnabled = days != -20;
        removeOlderYesNo.setSelected( isEnabled );
        dayField.setEnabled( isEnabled);
        if ( days == -20 )
        {
            days = 30;
        }
        dayField.setNumber( new Integer(days));
    }

    public void show() throws RaplaException  {
        super.show();
    }
  
    public void commit() throws RaplaException {
        super.commit();
    }


    /**
     * @see org.rapla.gui.DefaultPluginOption#getDescriptorClassName()
     */
    public String getDescriptorClassName() {
        return ArchiverPlugin.class.getName();
    }
    
    public String getName(Locale locale) {
        return "Archiver Plugin";
    }

}
