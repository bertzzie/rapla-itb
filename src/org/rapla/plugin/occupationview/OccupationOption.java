/*--------------------------------------------------------------------------*
 | Copyright (C) 2011 Bob Jordaens                                          |
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
package org.rapla.plugin.occupationview;

import java.awt.Component;
import java.util.Locale;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import org.rapla.components.calendar.RaplaNumber;
import org.rapla.components.layout.TableLayout;
import org.rapla.entities.configuration.Preferences;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.OptionPanel;
import org.rapla.gui.RaplaGUIComponent;

public class OccupationOption extends RaplaGUIComponent implements OptionPanel {
	
	public final static String MONTHS = "org.rapla.plugin.occupation.Months";
    JPanel panel = new JPanel();
    RaplaNumber months = new RaplaNumber(new Double(1),new Double(0),new Double(12), false);

    Preferences preferences;
    
// BJO 00000101
	public final static String START_DAY  = "org.rapla.plugin.occupation.StartDay";
    public final static String FIRSTDAY = "FirstDay";
    public final static String TODAY = "ToDay";
    JComboBox startDaySelector = new JComboBox( new String[] {
    		  														  FIRSTDAY
																	, TODAY
																	}
    );
// BJO 00000101
    public OccupationOption(RaplaContext sm) throws RaplaException {
        super( sm);
        setChildBundleName( OccupationPlugin.RESOURCE_FILE);
    }

    public void create() throws RaplaException {
    	
        double pre = TableLayout.PREFERRED;
        double fill = TableLayout.FILL;
        // rows = 1 columns = 2
        panel.setLayout( new TableLayout(new double[][] {
        												  {pre, 5, pre, 5, pre}
        												, {pre, 5, pre, 5, fill}
        												}
        ));
      
        panel.add( new JLabel(getString("horizon")),"0,0"  );
        panel.add( months,"2,0");
        
// BJO 00000101
        ListRenderer listRenderer = new ListRenderer();
        panel.add( new JLabel(getString("startday")),"0,2"  );
        panel.add(startDaySelector,"2,2");
        startDaySelector.setRenderer( listRenderer );
// BJO 00000101
    }

    public JComponent getComponent() {
        return panel;
    }
    
    public String getName(Locale locale) {
        return getString("occupation");
    }

    public void setPreferences( Preferences preferences) {
        this.preferences = preferences;
    }

    public void show() throws RaplaException {
        int times = preferences.getEntryAsInteger( MONTHS,0);
        months.setNumber(times);
        
        String day = preferences.getEntryAsString( START_DAY, TODAY );
        startDaySelector.setSelectedItem(  day.equals(FIRSTDAY) ? FIRSTDAY : TODAY );
        
        create();
    }

    public void commit() {
        int times = months.getNumber().intValue();
        preferences.putEntry( MONTHS,"" + times);
        
        String day = (String) startDaySelector.getSelectedItem();
        preferences.putEntry( START_DAY, day);
    }
    
	private class ListRenderer extends DefaultListCellRenderer  {
		  
		private static final long serialVersionUID = 1L;
		
		  public ListRenderer() {
		  }

		public Component getListCellRendererComponent(JList list,Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if ( value != null) {
                setText(getString( (String) value ));

            }
            return this;
        }
	}
}