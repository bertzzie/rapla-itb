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

import java.awt.Component;
import java.util.Calendar;
import java.util.Locale;

import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;

import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.rapla.components.calendar.RaplaNumber;
import org.rapla.components.calendar.RaplaTime;
import org.rapla.components.calendarview.WeekdayMapper;
import org.rapla.components.layout.TableLayout;
import org.rapla.entities.configuration.Preferences;
import org.rapla.entities.configuration.RaplaConfiguration;
import org.rapla.entities.domain.RepeatingType;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.CalendarOptions;
import org.rapla.gui.CalendarOptionsImpl;
import org.rapla.gui.OptionPanel;
import org.rapla.gui.RaplaGUIComponent;

public class CalendarOption extends RaplaGUIComponent implements OptionPanel 
{
    JPanel panel = new JPanel();
    JCheckBox showExceptionsField = new JCheckBox();
    JComboBox colorBlocks = new JComboBox( new String[] {
    		 CalendarOptionsImpl.COLOR_NONE	
    		,CalendarOptionsImpl.COLOR_RESOURCES
    		, CalendarOptionsImpl.COLOR_EVENTS
    		, CalendarOptionsImpl.COLOR_EVENTS_AND_RESOURCES
    }
    													);
    RaplaNumber rowsPerHourField = new RaplaNumber(new Double(1),new Double(1),new Double(12), false);
    Preferences preferences;
    CalendarOptions options;
    RaplaTime worktimeStart;
    RaplaTime worktimeEnd;
    JPanel excludeDaysPanel =  new JPanel();
    JCheckBox[] box = new JCheckBox[7];
    WeekdayMapper mapper;
    RaplaNumber nTimesField = new RaplaNumber(new Double(1),new Double(1),new Double(365), false);



    JComboBox repeatingType = new JComboBox( new RepeatingType[] { 
		        RepeatingType.DAILY,
		        RepeatingType.WEEKLY,
		        RepeatingType.MONTHLY,
		        RepeatingType.YEARLY
		  });
    
    JComboBox firstDayOfWeek;
    RaplaNumber daysInWeekview;


    JComboBox eventTypeSelector;

    public CalendarOption(RaplaContext sm) throws RaplaException {
        super( sm);
        daysInWeekview = new RaplaNumber(7, 3, 14, false);
        mapper = new WeekdayMapper(getLocale());
        worktimeStart = createRaplaTime();
        worktimeStart.setRowsPerHour( 1 );
        worktimeEnd = createRaplaTime();
        worktimeEnd.setRowsPerHour( 1 );
        double pre = TableLayout.PREFERRED;
        double fill = TableLayout.FILL;
        // rows = 8 columns = 4
        panel.setLayout( new TableLayout(new double[][] {{pre, 5, pre, 5 , pre, 5, pre}, {pre,5,pre,5,pre,5,pre,5,pre,5,pre,5,pre,5,pre,5,pre,5,pre,5,pre,5,pre,5,pre,5,fill}}));

        showExceptionsField.setText("");
        panel.add( new JLabel(getString("rows_per_hour")),"0,0"  );
        panel.add( rowsPerHourField,"2,0");
        panel.add( new JLabel(getString("start_time")),"0,2"  );
        panel.add( worktimeStart, "2,2");
        panel.add( new JLabel(getString("end_time")),"0,4"  );
        panel.add( worktimeEnd,"2,4");
        panel.add( new JLabel(getString("color")),"0,6"  );
        panel.add( colorBlocks,"2,6");

        ListRenderer listRenderer = new ListRenderer();
        
        colorBlocks.setRenderer( listRenderer );
        showExceptionsField.setText("");        
        panel.add( new JLabel(getString("display_exceptions")),"0,8");
        panel.add( showExceptionsField,"2,8");
        
        firstDayOfWeek = new JComboBox(mapper.getNames());
        panel.add( new JLabel(getString("day1week")),"0,10");
        panel.add( firstDayOfWeek,"2,10");

        panel.add( new JLabel(getString("daysInWeekview")),"0,12");
        panel.add( daysInWeekview,"2,12");
// BJO 00000079           
// BJO 00000012 
 //       panel.add( new JLabel(getString("repeating")),"0,16"  );
// BJO 00000012 
// BJO 00000052        
//        panel.add( repeatingType,"2,16");
//        repeatingType.setRenderer( listRenderer );   
// BJO 00000052
// BJO 00000012 
  //      panel.add( repeatingDuration,"4,16");
  //      panel.add( nTimesField,"6,16");
       
//        repeatingDuration.setRenderer( listRenderer );
//        ActionListener repeatingListener = new ActionListener() {
//            public void actionPerformed(ActionEvent evt) {
//                if(repeatingDuration.getSelectedIndex()==0)
//                    nTimesField.setEnabled(false);
//                else
//                    nTimesField.setEnabled(true);
//            }     
//        };
//        repeatingDuration.addActionListener(repeatingListener);     

// BJO 00000063 
    //    panel.add( new JLabel(getString("reservation_type")),"0,18"  );
      //  DynamicType[] types = getQuery().getDynamicTypes( DynamicTypeAnnotations.VALUE_RESERVATION_CLASSIFICATION);
      //  eventTypeSelector =  new JComboBox( types );
      //  panel.add( eventTypeSelector,"2,18");
      //  eventTypeSelector.setRenderer(new NamedListCellRenderer(getI18n().getLocale()));
        //eventTypeSelector.addActionListener( this );
// BJO 00000063 

// BJO 00000012
        panel.add( new JLabel(getString("exclude_days")),"0,22,l,t");
        panel.add( excludeDaysPanel,"2,22");
        excludeDaysPanel.setLayout( new BoxLayout( excludeDaysPanel,BoxLayout.Y_AXIS));
        for ( int i=0;i<box.length;i++) {
            int weekday = mapper.dayForIndex( i);
            box[i] = new JCheckBox(mapper.getName(weekday));
            excludeDaysPanel.add( box[i]);
            
        }
    }

    public JComponent getComponent() {
        return panel;
    }
    public String getName(Locale locale) {
        return getString("calendar");
    }

    public void setPreferences( Preferences preferences) {
        this.preferences = preferences;
    }

    public void show() throws RaplaException {
    	// get the options 
        RaplaConfiguration config = (RaplaConfiguration)preferences.getEntry( CalendarOptionsImpl.CALENDAR_OPTIONS);
        if ( config != null) {
            options = new CalendarOptionsImpl( config.getConfig());
        } else {
            options = getCalendarOptions();
        }

        if ( options.isEventColoring() && options.isResourceColoring())
        {
        	colorBlocks.setSelectedItem(  CalendarOptionsImpl.COLOR_EVENTS_AND_RESOURCES);
        }
        else if ( options.isEventColoring() )
        {
        	colorBlocks.setSelectedItem(  CalendarOptionsImpl.COLOR_EVENTS);
        }
        else if (  options.isResourceColoring())
        {
        	colorBlocks.setSelectedItem(  CalendarOptionsImpl.COLOR_RESOURCES);
        }
        else
        {
          	colorBlocks.setSelectedItem(  CalendarOptionsImpl.COLOR_NONE);
        } 
        
        showExceptionsField.setSelected( options.isExceptionsVisible());
        
        rowsPerHourField.setNumber( new Long(options.getRowsPerHour()));
        
        Calendar calendar = getRaplaLocale().createCalendar();
        calendar.set( Calendar.HOUR_OF_DAY, options.getWorktimeStart());
        calendar.set( Calendar.MINUTE, 0);
        worktimeStart.setTime( calendar.getTime() );
        calendar.set( Calendar.HOUR_OF_DAY, options.getWorktimeEnd());
        worktimeEnd.setTime( calendar.getTime() );
        
        for ( int i=0;i<box.length;i++) {
            int weekday = mapper.dayForIndex( i);
            box[i].setSelected( options.getExcludeDays().contains( new Integer( weekday)));
        }
        int firstDayOfWeek2 = options.getFirstDayOfWeek();
        firstDayOfWeek.setSelectedIndex( mapper.indexForDay( firstDayOfWeek2));
        
        daysInWeekview.setNumber( options.getDaysInWeekview());

// BJO 00000012
        //repeating.setSelectedItem( options.isInfiniteRepeating() ? CalendarOptionsImpl.NTIMES : CalendarOptionsImpl.REPEATING_NTIMES );
        //repeatingDuration.setSelectedItem( options.getRepeatingDuration());
        //nTimesField.setNumber( new Long(options.getnTimes()));
        //nTimesField.setEnabled(options.isNtimesRepeating());
// BJO 00000012
// BJO 00000037        
// BJO 00000037
// BJO 00000052
        //repeatingType.setSelectedItem( options.getRepeatingType());
// BJO 00000052
// BJO 00000063 
//        String eventType = options.getEventType();
//        DynamicType dt;
//		if(eventType==null){
//        	dt = getQuery().getDynamicTypes( DynamicTypeAnnotations.VALUE_RESERVATION_CLASSIFICATION)[0];
//        	eventTypeSelector.setSelectedItem(dt);
//		}
//		else {
//			dt = getQuery().getDynamicType(eventType);
//			eventTypeSelector.setSelectedItem(dt);
//		}
// BJO 00000063 

    }

    public void commit() {
    	// Save the options
        DefaultConfiguration calendarOptions = new DefaultConfiguration("calendar-options");
        DefaultConfiguration worktime = new DefaultConfiguration(CalendarOptionsImpl.WORKTIME);
        DefaultConfiguration excludeDays = new DefaultConfiguration(CalendarOptionsImpl.EXCLUDE_DAYS);
        DefaultConfiguration rowsPerHour = new DefaultConfiguration(CalendarOptionsImpl.ROWS_PER_HOUR);
        DefaultConfiguration exceptionsVisible = new DefaultConfiguration(CalendarOptionsImpl.EXCEPTIONS_VISIBLE);
        
        DefaultConfiguration daysInWeekview = new DefaultConfiguration(CalendarOptionsImpl.DAYS_IN_WEEKVIEW);
        DefaultConfiguration firstDayOfWeek = new DefaultConfiguration(CalendarOptionsImpl.FIRST_DAY_OF_WEEK);
        daysInWeekview.setValue( this.daysInWeekview.getNumber().intValue());
        int selectedIndex = this.firstDayOfWeek.getSelectedIndex();
		int weekday = mapper.dayForIndex(selectedIndex);
        firstDayOfWeek.setValue( weekday);
// BJO 00000012   
//        DefaultConfiguration repeating = new DefaultConfiguration(CalendarOptionsImpl.REPEATING); 
//        RepeatingEnding repeatingValue = (RepeatingEnding) this.repeatingDuration.getSelectedItem();
//        if ( repeatingValue != null )
//        	repeating.setValue(  repeatingValue.toString() );
//        else
//        	repeating.setValue(  RepeatingEnding.FOREVEVER.toString() );
//        calendarOptions.addChild( repeating );
        
 //       DefaultConfiguration nTimes = new DefaultConfiguration(CalendarOptionsImpl.NTIMES);
 //       nTimes.setValue( nTimesField.getNumber().intValue());
        // CKO Do not save until moved to new class
        //        calendarOptions.addChild( nTimes);
// BJO 00000012
// BJO 00000052   
//        DefaultConfiguration repeatingType = new DefaultConfiguration(CalendarOptionsImpl.REPEATINGTYPE); 
//        RepeatingType repeatingTypeValue =  (RepeatingType)this.repeatingType.getSelectedItem();
//        if ( repeatingTypeValue != null )
//        	repeatingType.setValue(  repeatingTypeValue.toString());
//        else
//        	repeatingType.setValue( RepeatingType.DAILY.toString() );
        
     // CKO Do not save until moved to new class
     //   calendarOptions.addChild( repeatingType);
// BJO 00000052

        
        DefaultConfiguration colorBlocks = new DefaultConfiguration(CalendarOptionsImpl.COLOR_BLOCKS);
        String colorValue = (String) this.colorBlocks.getSelectedItem();
        if ( colorValue != null )
        {
            colorBlocks.setValue(  colorValue );
        }
        calendarOptions.addChild( colorBlocks );
        
//// BJO 00000063        
//        DefaultConfiguration eventType = new DefaultConfiguration(CalendarOptionsImpl.EVENTTYPE);
//        DynamicType dynamicType = (DynamicType) eventTypeSelector.getSelectedItem();
//        if ( dynamicType != null )
//        {
//        	eventType.setValue( dynamicType.getElementKey() );
//        }
        // CKO Do not save until moved to new class
        
        //calendarOptions.addChild( eventType );
// BJO 00000063
        
        Calendar calendar = getRaplaLocale().createCalendar();
        calendar.setTime( worktimeStart.getTime());
        int worktimeStart = calendar.get(Calendar.HOUR_OF_DAY);
        calendar.setTime( worktimeEnd.getTime());
        int worktimeEnd = calendar.get(Calendar.HOUR_OF_DAY);
        worktime.setValue(  worktimeStart + "-" + worktimeEnd );
        calendarOptions.addChild( worktime);

        exceptionsVisible.setValue( showExceptionsField.isSelected() );
        calendarOptions.addChild( exceptionsVisible);

        rowsPerHour.setValue( rowsPerHourField.getNumber().intValue());
        StringBuffer days = new StringBuffer();
        for ( int i=0;i<box.length;i++) {
            if (box[i].isSelected()) {
                if ( days.length() > 0)
                    days.append(",");
                days.append( mapper.dayForIndex( i ));
            }
        }
        calendarOptions.addChild( rowsPerHour);
        excludeDays.setValue( days.toString());
        calendarOptions.addChild( excludeDays);
        calendarOptions.addChild(daysInWeekview);
        calendarOptions.addChild(firstDayOfWeek);
        preferences.putEntry( CalendarOptionsImpl.CALENDAR_OPTIONS,new RaplaConfiguration( calendarOptions));
	}

   

	private class ListRenderer extends DefaultListCellRenderer  {
		private static final long serialVersionUID = 1L;
		
		public Component getListCellRendererComponent(JList list,Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if ( value != null) {
                setText(getString(  value.toString()));
            }
            return this;
        }
	}	
}