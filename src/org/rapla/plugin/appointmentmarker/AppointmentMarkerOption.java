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
package org.rapla.plugin.appointmentmarker;

import java.awt.BorderLayout;
import java.util.Locale;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.rapla.components.layout.TableLayout;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.DefaultPluginOption;
import org.rapla.gui.OptionPanel;

/** Not used yet */
public class AppointmentMarkerOption extends DefaultPluginOption implements OptionPanel {
    JTextField markerLabel = new JTextField();
    
    public AppointmentMarkerOption( RaplaContext sm ) throws RaplaException
    {
        super( sm );
    }

    protected JPanel createPanel() throws RaplaException {
        JPanel panel = super.createPanel();
        JPanel content = new JPanel();
        double[][] sizes = new double[][] {
            {5,TableLayout.PREFERRED, 5,TableLayout.PREFERRED,TableLayout.FILL,5}
            ,{TableLayout.PREFERRED,5,TableLayout.PREFERRED,5,TableLayout.PREFERRED}
        };
        addCopyPaste( markerLabel);
        markerLabel.setColumns( 10);
        TableLayout tableLayout = new TableLayout(sizes);
        content.setLayout(tableLayout);
        content.add(new JLabel("Add a new attribute (hidden) with the type text and the key 'appointmentmarker' to an eventtype."), "1,0,4,0");
        content.add(new JLabel("Then you can mark events of that type by right clicking in the calendar view."), "1,2,4,2");
         content.add(new JLabel("Marker Menu Label:"), "1,4");
        content.add( markerLabel, "3,4");
        panel.add( content, BorderLayout.CENTER);
        return panel;
    }

        
    protected void addChildren( DefaultConfiguration newConfig) {
        DefaultConfiguration markerLabelConf = new DefaultConfiguration("marker-label");
        markerLabelConf.setValue( markerLabel.getText() );
        newConfig.addChild( markerLabelConf );
    }

    protected void readConfig( Configuration config)   {
        String markerLabelText = config.getChild("marker-label").getValue("mark");
        markerLabel.setText( markerLabelText );
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
        return AppointmentMarkerPlugin.class.getName();
    }
    
    public String getName(Locale locale) {
        return "Appointment Marker Plugin";
    }

}
