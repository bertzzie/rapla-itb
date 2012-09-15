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
package org.rapla.plugin.timeslot;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.rapla.components.calendar.RaplaTime;
import org.rapla.components.layout.TableLayout;
import org.rapla.components.util.SerializableDateTimeFormat;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.DefaultPluginOption;
import org.rapla.gui.OptionPanel;
import org.rapla.gui.toolkit.RaplaButton;



public class TimeslotOption extends DefaultPluginOption implements OptionPanel {
   
	JPanel list = new JPanel();
	List<Timeslot> timeslots;
	
    class TimeslotRow
    {
    	RaplaTime raplatime = new RaplaTime();
    	JTextField textfield = new JTextField();
    	RaplaButton delete = new RaplaButton(RaplaButton.SMALL);

    	public TimeslotRow(Timeslot slot, final JPanel container) 
    	{
    		try {
				addCopyPaste( textfield);
			} catch (RaplaException e) {
			}
    		textfield.setText( slot.getName());
    		int minuteOfDay = slot.getMinuteOfDay();
    		int hour = minuteOfDay /60;
			int minute = minuteOfDay %60;
			raplatime.setTime(hour, minute);
			delete.setIcon(getIcon("icon.remove"));
			delete.addActionListener(new ActionListener() {
				
				public void actionPerformed(ActionEvent arg0) {
					rows.remove( TimeslotRow.this);
					update();
				}
			});
    	}
    	
    	
    }
    public TimeslotOption(RaplaContext sm) throws RaplaException {
        super(sm);
    }

    List<TimeslotRow> rows = new ArrayList<TimeslotOption.TimeslotRow>();
    
    protected JPanel createPanel() throws RaplaException 
    {
    	JPanel main = super.createPanel();
        
    	JScrollPane jScrollPane = new JScrollPane(list);
        JPanel container = new JPanel();
        container.setLayout( new BorderLayout());
        container.add(jScrollPane,BorderLayout.CENTER);
        JPanel header = new JPanel();
        RaplaButton reset = new RaplaButton(RaplaButton.SMALL);
    	RaplaButton resetButton = reset;
		resetButton.setIcon(getIcon("icon.remove"));
		resetButton.setText(getString("reset"));
		RaplaButton newButton = new RaplaButton(RaplaButton.SMALL);
		newButton.setIcon(getIcon("icon.new"));
		newButton.setText(getString("new"));
		
		header.add( newButton);
		header.add( resetButton );
		newButton.addActionListener( new ActionListener() {
			
			public void actionPerformed(ActionEvent arg0) {
				int minuteOfDay = 0;
				String lastName = "";
				Timeslot slot = new Timeslot(lastName, minuteOfDay);
				rows.add( new TimeslotRow(slot, list));
				update();
			}
		});
		
		resetButton.addActionListener( new ActionListener() {
			
			public void actionPerformed(ActionEvent arg0) {
				timeslots = TimeslotProvider.getDefaultTimeslots(getRaplaLocale());
				initRows();
			}
		});
        container.add(header,BorderLayout.NORTH);
        
        main.add( container, BorderLayout.CENTER);
		return main;
    }


	protected void initRows()  {
		rows.clear();
    	for ( Timeslot slot:timeslots)
    	{
    		TimeslotRow row =  new TimeslotRow( slot, list);
    		rows.add( row);
    	}
    	TimeslotRow firstRow = rows.get(0);
    	firstRow.delete.setEnabled( false);
    	firstRow.raplatime.setEnabled( false);
	}


	protected void update() {
		list.removeAll();
    	TableLayout tableLayout = new TableLayout();
    	list.setLayout(tableLayout);
    	tableLayout.insertColumn(0,TableLayout.PREFERRED);
    	tableLayout.insertColumn(1,10);
    	tableLayout.insertColumn(2,TableLayout.PREFERRED);
    	tableLayout.insertColumn(3,10);
    	tableLayout.insertColumn(4,TableLayout.FILL);

    	list.setLayout( tableLayout);
    	tableLayout.insertRow(0, TableLayout.PREFERRED);
    	list.add(new JLabel("time"),"2,0");
    	list.add(new JLabel("name"),"4,0");
    	int i = 0;
    	for ( TimeslotRow row:rows)
    	{
    		tableLayout.insertRow(++i, TableLayout.MINIMUM);
    	 	list.add(row.delete,"0,"+i);
    		list.add(row.raplatime,"2,"+i);
        	list.add(row.textfield,"4,"+i);
    	}
    	list.revalidate();
    	list.repaint();
	}

        
    protected void addChildren( DefaultConfiguration newConfig) 
    {
    	if (!activate.isSelected())
    	{
    		return;
    	}
      	List<Timeslot> timeslots = new ArrayList<Timeslot>();
      	Calendar cal = getRaplaLocale().createCalendar();
      	for ( TimeslotRow row: rows)
    	{
    		
    		String name = row.textfield.getText();
    		Date time = row.raplatime.getTime();
    		if ( time != null )
    		{
    			cal.setTime( time);
    			int minuteOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
    			timeslots.add( new Timeslot( name, minuteOfDay));
    		}
    	}
      	Collections.sort( timeslots);
      	
      	for ( Timeslot slot: timeslots)
    	{
    		DefaultConfiguration conf = new DefaultConfiguration("timeslot");
    		conf.setAttribute("name", slot.getName());
    		int minuteOfDay = slot.getMinuteOfDay();
    		
    		cal.set(Calendar.HOUR_OF_DAY, minuteOfDay / 60);
    		cal.set(Calendar.MINUTE, minuteOfDay % 60);
    		cal.set(Calendar.SECOND, 0);
    		cal.set(Calendar.MILLISECOND, 0);
    		SerializableDateTimeFormat format = new SerializableDateTimeFormat(cal);
			String time = format.formatTime(cal.getTime());
			conf.setAttribute("time", time);
    		newConfig.addChild( conf);
    	}
      	
      	if ( getContext().has(TimeslotProvider.class.getName()))
      	{
	      	try {
				getService(TimeslotProvider.class).update( newConfig);
			} catch (ParseException e) {
				getLogger().error(e.getMessage());
			}
      	}
    }

    protected void readConfig( Configuration config)   
    {
    	try {
			timeslots = TimeslotProvider.parseConfig(config);
		} catch (ParseException e) {
		}
    	if ( timeslots == null)
    	{
    		timeslots = TimeslotProvider.getDefaultTimeslots(getRaplaLocale());
    	}
    	initRows();
    	update();
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
        return TimeslotPlugin.class.getName();
    }
    
    public String getName(Locale locale) {
        return "Timeslot Plugin";
    }

}
