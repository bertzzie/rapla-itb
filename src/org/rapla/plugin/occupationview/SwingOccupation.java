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

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DateFormatSymbols;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.rapla.components.calendar.DateChangeEvent;
import org.rapla.components.calendar.DateChangeListener;
import org.rapla.components.tablesorter.TableSorter;
import org.rapla.components.util.DateTools;
import org.rapla.entities.Category;
import org.rapla.entities.CategoryAnnotations;
import org.rapla.entities.User;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.Repeating;
import org.rapla.entities.domain.RepeatingType;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.dynamictype.AttributeType;
import org.rapla.entities.dynamictype.Classifiable;
import org.rapla.entities.dynamictype.Classification;
import org.rapla.entities.dynamictype.ClassificationFilter;
import org.rapla.entities.dynamictype.ConstraintIds;
import org.rapla.facade.QueryModule;
import org.rapla.facade.UpdateModule;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.CalendarModel;
import org.rapla.gui.CalendarOptions;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.SwingCalendarView;
import org.rapla.gui.internal.action.AppointmentAction;
import org.rapla.gui.internal.common.CalendarSelectionModel;
import org.rapla.gui.toolkit.DialogUI;

public class SwingOccupation extends RaplaGUIComponent implements SwingCalendarView
{

	OccupationTableModel occupationTableModel;
    JTable table;
    final CalendarSelectionModel model;
    TimeShiftPanel timeShift;
    JComponent container;

    boolean checkRestrictions=false;
    Appointment[] appointments;
    Map<Appointment, Set<Allocatable>> appointmentMap = new HashMap();
    User user;
    Reservation mutableReservation;

    private boolean conflictingAppointments[]; // stores the temp conflicting appointments
    private int conflictCount; // temp value for conflicts
    private int permissionConflictCount; // temp value for conflicts that are the result of denied permissions
    int calendarShift = Calendar.MONTH;
    Calendar calendarDS = getRaplaLocale().createCalendar();
    Calendar calendarDE = getRaplaLocale().createCalendar();
    JPopupMenu popupMenu = new JPopupMenu();
    List<Allocatable> allocatableList=null;
   
    boolean coloredEvents = false;
    RepeatingType repeatingType;
    int repeatingDuration;
    CalendarOptions options;
    AllocatableCellRenderer alcRenderer = new AllocatableCellRenderer();
    Locale locale = getLocale();
    Date nullDate = new Date(-2208988800000L); // Initialize with 1900/01/01
    int archiveAge = 0;
	private DecimalFormat formatDaysInOut = new DecimalFormat("#");
	Date today = getQuery().today();
	
	 TableSorter  sorter;
		
    public SwingOccupation( RaplaContext context, final CalendarModel model, final boolean editable ) throws RaplaException
    {
        super( context ); 
        setChildBundleName( OccupationPlugin.RESOURCE_FILE);
        table = new JTable()
        {
            private static final long serialVersionUID = 1L;
            
            public String getToolTipText(MouseEvent evt) {
                if (!editable)
                    return null;
                
                int r = rowAtPoint( evt.getPoint() );
                int c = columnAtPoint( evt.getPoint() );
                if(c<OccupationTableModel.CALENDAR_EVENTS) // no menu on fixed columns
                	return null;
                Object value = occupationTableModel.getValueAt(r, c);
                if(value instanceof OccupationCell) {
                	OccupationCell occCell = (OccupationCell) value;
                	final Reservation reservation = occCell.getReservation();
                    if(reservation != null) {
                        Appointment[] app = reservation.getAppointments();
                        return getInfoFactory().getToolTip( app[0] );
		        	}
                	if(occCell.getTypeId() == -1 ) {
					        AllocationCell alcCell = (AllocationCell) occupationTableModel.getValueAt(r, OccupationTableModel.CALENDAR_RESOURCE);
	            	    	Allocatable alloc = alcCell.allocatable;
							Classification classification = alloc.getClassification();
							return classification.getType().getName(locale) + ":" + alloc.getName(locale)+ " " + getString("not_selectable");
					}
                }
                return null;
            }
            
            public Point getToolTipLocation(MouseEvent evt) {

            	return new Point(0, rowAtPoint( evt.getPoint() ) * getRowHeight());

              }
        };
        
        
        archiveAge = getClientFacade().getPreferences(null).getEntryAsInteger(UpdateModule.ARCHIVE_AGE, 31);
        		
		TableCellRenderer	renderer = new OccupationTableCellRenderer();
			    
		table.setDefaultRenderer( Object.class, renderer );

        if ( editable )
        {
            container = new JScrollPane( table);
            container.setPreferredSize( new Dimension(600,800));
        }
        else
        {
            container = table;
        	Dimension size = table.getPreferredSize();
            container.setBounds( 0,0,(int)600, (int)size.getHeight());
        }
        this.model = (CalendarSelectionModel) model;
       
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setColumnSelectionAllowed(false);
        table.setRowSelectionAllowed(false);
        table.setCellSelectionEnabled(true);
        table.getTableHeader().setReorderingAllowed(false); // no column reordering 
        table.setColumnModel(new GroupableTableColumnModel());
        table.setTableHeader(new GroupableTableHeader((GroupableTableColumnModel)table.getColumnModel()));
       
        if ( editable ) {
            PopupTableHandler popupHandler = new PopupTableHandler();
            container.addMouseListener( popupHandler);;
            table.addMouseListener( popupHandler );
        }

        //model.setStartDate(null); // null = today()
        timeShift = new TimeShiftPanel( context, model);
        timeShift.setIncrementSize(calendarShift); // increment 1 Month 
        this.user = getUser();
        timeShift.addDateChangeListener( new DateChangeListener() {
            public void dateChanged( DateChangeEvent evt )
            {
                try {
                		update(  );
                } catch (RaplaException ex ){
                    showException( ex, getComponent());
                }
            }
        });
        
        // get default user preferences from user profile
        options = getCalendarOptions();
    	repeatingType = options.getRepeatingType();
    	repeatingDuration = options.isInfiniteRepeating() ? -1 : (1 * getCalendarOptions().getnTimes());  // -1:infinite; >0:=n-times
        //update();
    }


    public void update() throws RaplaException
    {
    	try {
    			container.getTopLevelAncestor().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    			updatetable();
    		} finally {
    			container.getTopLevelAncestor().setCursor(Cursor.getDefaultCursor());
    		}
    }
    
    public void updatetable() throws RaplaException
    {  
    	timeShift.update();
    	int timeShiftTimes = timeShift.timeShiftTimes.getNumber().intValue() + 1;
        
        ClassificationFilter[] cfilters  = model.getReservationFilter();
        if(cfilters.length>1) {
        	DialogUI dialog = DialogUI.create(
                        						 getContext()
                        						,container.getTopLevelAncestor()
                        						,true
                        						,getString("warning")
                        						,getString("warning.max-one-eventtype")
                        						,new String[] { getString("continue") }
            								 );
            dialog.setIcon(getIcon("icon.warning"));
            dialog.setDefault(0);
            dialog.start();
            return;
        }
    	mutableReservation = getClientFacade().newReservation();
        Appointment appointment = getClientFacade().newAppointment( model.getSelectedDate(), model.getSelectedDate());
        mutableReservation.addAppointment(appointment);
        appointments = mutableReservation.getAppointments();
        allocatableList = getAllAllocatables();
        Collections.sort(allocatableList, new AllocatableSortByName());
        int rowCount = allocatableList.size();
        Iterator<Allocatable> it = allocatableList.iterator();
        calendarDS = timeShift.getSelectedStartTime(); // start midnight
        calendarDE = timeShift.getSelectedEndTime(); // end midnight
        
          
        // calculate number of columns required to display from calendar
        Calendar calendarTmp = (Calendar) calendarDS.clone();
        int totalDays= -1 * calendarTmp.get(Calendar.DAY_OF_MONTH) + 1;
        for ( int i = 0; i< timeShiftTimes; i++) {
        	totalDays += calendarTmp.getActualMaximum(Calendar.DAY_OF_MONTH);
        	calendarTmp.add(calendarShift, 1);
        }
        int columnCount = totalDays + OccupationTableModel.CALENDAR_EVENTS ; // + for fixed columns
        //Object occupationTable[][] = new Object[rowCount][columnCount]; 
        occupationTableModel = new OccupationTableModel(getI18n(), rowCount, columnCount, calendarDS.getTime());
       
        
// Sorting block

       sorter = new TableSorter(occupationTableModel,table.getTableHeader());
       sorter.setSortable(0, false);

		sorter.setColumnComparator(1, new Comparator<AllocationCell>() {
	        public int compare(AllocationCell o1, AllocationCell o2) {
					String s1 = o1.allocatable.getName(locale);
	   				String s2 = o2.allocatable.getName(locale);
		            return s1.compareTo(s2);
	        }
	    });

		sorter.setSortable(2, false);

		sorter.setColumnComparator(3, new Comparator<Object>() {
	        public int compare(Object o1, Object o2) { 
	        	Integer i1 = (Integer) o1;
	        	Integer i2 = (Integer) o2;
	        	return i1.compareTo(i2);
	        }
	    });

		sorter.setColumnComparator(4, new Comparator<Object>() {
	        public int compare(Object o1, Object o2) { 
	        	Integer i1 = (Integer) o1;
	        	Integer i2 = (Integer) o2;
	        	return i1.compareTo(i2);
	        }
	    });

		//sorter.setSortable(4, false);
		sorter.setSortable(5, false);
		
		//table.setRowSorter(sorter);
		
		table.setModel(  sorter );
        
		table.getColumnModel().getColumn(OccupationTableModel.CALENDAR_CHECK).setCellRenderer(alcRenderer);
        int r = 0;
        char leftBound = ' ';
        char rightBound = ' ';
        while (it.hasNext()) {
        	
        	// get resource data
    		Allocatable alloc = it.next();
    		AllocationCell alcCell = new AllocationCell(alloc);
    		occupationTableModel.setValueAt( alcCell, r, OccupationTableModel.CALENDAR_RESOURCE);
    		
    		// get reservation data
            Calendar calendarTDS = (Calendar) calendarDS.clone();
            Calendar calendarTDE = (Calendar) calendarDE.clone();
            mutableReservation.addAllocatable(alloc);
            String occupationType = null;
            QueryModule qry = getQuery();
        	for ( int c = OccupationTableModel.CALENDAR_EVENTS ; c <= (columnCount - 1); c++) {
        		sorter.setSortable(c, false);
        		if(DateTools.cutDate(today).equals(DateTools.cutDate(calendarTDS.getTime())))
        			occupationTableModel.setTodayColumn(c);
        		appointment.move(calendarTDS.getTime(),calendarTDE.getTime());
                updateBindings(appointments);  
        		occupationType = getOccupationType((Allocatable) alloc);
        		//occupationTableModel.setValueAt( null, r, OccupationTableModel.CALENDAR_IN_DAYS);
        		if(occupationType.equals("C")) { // Conflict
        			// Not Free
        			Reservation [] res = qry.getReservationsForAllocatable(new Allocatable[] { alloc },calendarTDS.getTime(),calendarTDE.getTime(), cfilters);
        			if(res.length==0) {
                        OccupationCell occCell = new OccupationCell('N',0,'N', null);
                		occupationTableModel.setValueAt( occCell, r, c);
        			}
        			else { // Not Free
        				//  A from-to will be split is days like [ [, ] [ , ] [, ] ] [ left boundary = startdate, ] right boundary = enddate
        				//   ] and [ used in the middle.
	                    Reservation reservation = res[0];
	                    Appointment[] apps = reservation.getAppointmentsFor(alloc);
	                    Appointment app = apps[0];
	                    //System.out.println(alloc.getName(locale));
	                    //System.out.println("Start= " + app.getStart() + " TDS= " + calendarTDS.getTime()); 
	                    Date minStartDate = app.getStart();
	                    if(DateTools.isSameDay(minStartDate.getTime(),calendarTDS.getTime().getTime()))
	                    	if(DateTools.isMidnight(minStartDate))
	                    		leftBound = '[';
	                    	else
	                    		leftBound = '<';
	                    else
	                    	if(DateTools.isMidnight(minStartDate))
	                    		leftBound = ']';
	                    	else
	                    		leftBound = '>';
	                    //Repeating rep = app[0].getRepeating();
	                    //if (rep != null) System.out.println("Repating= " + rep);             
	                    //System.out.println("MaxEnd = " + app.getMaxEnd() + " End= " + app.getEnd()); 
	                    Date maxendDate = app.getMaxEnd();
	            		rightBound = '[';
	                    if(maxendDate!=null)
	                    	if(DateTools.isMidnight(maxendDate)) { // endDate 00:00:00 = previous date
	                    		if(DateTools.isSameDay((DateTools.subDay(maxendDate)).getTime(),calendarTDS.getTime().getTime()))
	                    			rightBound = ']';
	                    	}
	                    	else
		                    	if(DateTools.isSameDay(maxendDate.getTime(),calendarTDS.getTime().getTime()))
		                    		rightBound = '>';	                    			           
	                    OccupationCell occCell = new OccupationCell(leftBound,1,rightBound, reservation);
	            		occupationTableModel.setValueAt( occCell, r, c);
	            		if(c == occupationTableModel.getTodayColumn())
	            			setDaysInOut(app, r, today);
	            		else
	            			if(c >= OccupationTableModel.CALENDAR_EVENTS)
	            				setDaysInOut(app, r, calendarTDS.getTime());
	            		
	        			//System.out.println(res[0].toString() + " Length:" + res.length);
	        		}
        		}
        		else
	        		if(occupationType.equals(" ")) { // Free
	                    OccupationCell occCell = new OccupationCell('N',0,'N');
	            		occupationTableModel.setValueAt( occCell, r, c);
	            		
	            		// calculate archive date
                		Date startDate = DateTools.subDays(today, archiveAge);
                		Reservation [] res = getQuery().getReservationsForAllocatable(new Allocatable[] { alloc },startDate,today, null);
                		if(res.length != 0) {
        	                Appointment[] apps = res[res.length - 1].getAppointments();
        	                Appointment app = apps[0];
        	                if ( c == occupationTableModel.getTodayColumn())
        	                	setDaysInOut(app, r, today);
        	            }
        	            
	        		}
	        		else 
	        			if(occupationType.equals("F")) { // Forbidden Resource is not available at all, out of order or in maintenance
	        				OccupationCell occCell = new OccupationCell('N',-1,'N');
	        				occupationTableModel.setValueAt( occCell, r, c);
	        			}
        		/* debug
        		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String DS = sdf.format(calendarTDS.getTime());
                String DE = sdf.format(calendarTDE.getTime());                
        		System.out.println("Row="+r+" Column=" + c + " StartDate=" + DS + " EndDate= " + DE + " Type = " + occupationType);
                */
        		calendarTDS.add(Calendar.DATE, 1); // next startday
                calendarTDE.add(Calendar.DATE, 1); // next endday
        	}
           r++;
        }
        
        occupationTableModel.setFreeSlot(timeShift.freeSlot.getNumber().intValue());
        //setLineNumbers();

        occupationTableModel.firstFit();

    	TableColumnModel cm = table.getColumnModel();
    	
    	// Resource column Header
        TableColumn column = cm.getColumn(OccupationTableModel.CALENDAR_SEQUENCE_NUMBER);
        column.setPreferredWidth(30); 
        column.setMaxWidth(50);
        
        // Check column Header 
        column = cm.getColumn(OccupationTableModel.CALENDAR_RESOURCE); 
        column.setPreferredWidth(200); 
        column.setMaxWidth(300);
        
        // In column Header 
        column = cm.getColumn(OccupationTableModel.CALENDAR_CHECK); 
        column.setPreferredWidth(200); 
        column.setMaxWidth(300);

        // Out column Header 
        column = cm.getColumn(OccupationTableModel.CALENDAR_IN_DAYS); 
        column.setPreferredWidth(40); 
        column.setMaxWidth(50);

        // Sequence line number
        column = cm.getColumn(OccupationTableModel.CALENDAR_OUT_DAYS); 
        column.setPreferredWidth(40);
        column.setMaxWidth(50);
        
        calendarTmp = (Calendar) calendarDS.clone();
        SimpleDateFormat sdfMM = new SimpleDateFormat("MM",locale);
    	sdfMM.setTimeZone(DateTools.getTimeZone());
        SimpleDateFormat sdfdd = new SimpleDateFormat("dd",locale);
    	sdfdd.setTimeZone(DateTools.getTimeZone());
        //SimpleDateFormat sdfEE  = new SimpleDateFormat("EE",locale);
    	//sdfEE.setTimeZone(DateTools.getTimeZone());
    	GroupableTableColumnModel gcm = (GroupableTableColumnModel)table.getColumnModel();
    	String oldMM=null; // old month groupHeader label
    	String newMM=null; // new month groupHeader label
    	ColumnGroup g_MM = null;
    	for ( int i = OccupationTableModel.CALENDAR_EVENTS ; i <= (columnCount - 1); i++) {
    		
    		Date dateTmp = calendarTmp.getTime();
            int day = calendarTmp.get(Calendar.DAY_OF_WEEK);
            
            newMM = sdfMM.format(dateTmp);
            //set columnGroupHeader label == MM (Month) 01, , 12
            if(!newMM.equals(oldMM)) {
            	if(oldMM!=null)
            		gcm.addColumnGroup(g_MM);
            	g_MM = new ColumnGroup(new GroupableTableCellRenderer(),newMM);
            	oldMM = newMM;
            }
            //set columnGroupHeader label == dd (Day) 01, ...,31
            ColumnGroup g_dd = new ColumnGroup(new GroupableTableCellRenderer(), sdfdd.format(dateTmp));
            g_MM.add(g_dd);
            ColumnGroup g_dw = new ColumnGroup(new DayOfWeekHeaderRenderer(), Integer.toString(day));
            g_dd.add(g_dw);
            column = cm.getColumn(i);
            g_dw.add(column);
            
            // set column sizes
            column.setMinWidth(19);
            column.setMaxWidth(26);
            column.setPreferredWidth(26);
            //set columnHeader label == Day of the week  Mo, .... , Su
            int  selectedCount = occupationTableModel.getSelectedRows(i);
            column.setHeaderValue(selectedCount);
           	column.setHeaderRenderer(new countRenderer());
            calendarTmp.add(Calendar.DATE,1);
    	}
        gcm.addColumnGroup(g_MM);
    }
    
    public void setDaysInOut(Appointment app, int r, Date referenceDate) {

    	if(referenceDate.before(today))
    		return;
    	else 
    		if(referenceDate.after(today))
    			if((Integer) occupationTableModel.getValueAt(r,OccupationTableModel.CALENDAR_IN_DAYS) != Integer.MAX_VALUE)
    				return;
    	
	    int days = (int) ((app.getStart().getTime() -  today.getTime()) / DateTools.MILLISECONDS_PER_DAY);
		occupationTableModel.setValueAt( days, r, OccupationTableModel.CALENDAR_IN_DAYS);
    		
		Repeating rpt = app.getRepeating();
		Date edate = null;
		if ( rpt == null )
			edate = app.getEnd();
		else 
			if ( rpt.getEnd() != null && !rpt.isFixedNumber() ) 
				edate =  rpt.getEnd();		
			else 
				if (rpt.getEnd() != null)
					edate = rpt.getEnd(); 
		if(edate == null)
			occupationTableModel.setValueAt(null, r, OccupationTableModel.CALENDAR_OUT_DAYS);
		else {
			days = (int) ((edate.getTime() -  today.getTime()) / DateTools.MILLISECONDS_PER_DAY);
			occupationTableModel.setValueAt( days, r, OccupationTableModel.CALENDAR_OUT_DAYS);
		}
    	
		return;
	}
       
    public JComponent getDateSelection()
    {
        return timeShift.getComponent();
    }

    public void scrollToStart()
    {
    	try {
			update();
		} catch (RaplaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    public JComponent getComponent()
    {
    	return container;
    }

    private List<Allocatable> getAllAllocatables() throws RaplaException {
	    Allocatable[] allocatables = model.getSelectedAllocatables();
	    return getAllocatableList( allocatables);
	 }
   
    private List<Allocatable> getAllocatableList(Allocatable[] allocatables) {
        List<Allocatable> result = Arrays.asList( allocatables );
        return result;
    }
    
    private void updateBindings(Appointment[] appointments) {
            //      System.out.println("getting allocated resources");
        try {

            	for (int i=0;i<appointments.length;i++) {
            		Appointment appointment = appointments[i];
            		Set<Allocatable> allocatables = new HashSet(Arrays.asList(getQuery().getAllocatableBindings(appointment)));
            		appointmentMap.put(appointment,allocatables);
            	}
        	} 
        		catch (RaplaException ex) {
        			showException(ex,table);
        		}
    }

    private String getOccupationType(Allocatable allocatable) {
        calcConflictingAppointments( allocatable );
        if ( conflictCount == 0 ) {
            return " ";
        } else 
        	if ( conflictCount == appointments.length ) {
        		if ( conflictCount == permissionConflictCount ) {
               		if (!checkRestrictions) {
               			return "F"; //forbiddenIcon;
               		}
               	} else {
               		return "C"; //conflictIcon;
               	}
        	} else 
        		if ( !checkRestrictions ) {
        			return "X";
        		}
        for ( int i = 0 ; i < appointments.length; i++ ) {
            Appointment appointment = appointments[i];
            if ( mutableReservation.hasAllocated( allocatable, appointment ) && !getQuery().hasPermissionToAllocate( appointment,allocatable )) 
                return "F"; //forbiddenIcon;           
        }
        
        if ( permissionConflictCount - conflictCount == 0 ) {
            return " ";
        }
        
        Appointment[] restriction = mutableReservation.getRestriction(allocatable);
        if ( restriction.length == 0 ) {
            return "C"; //conflictIcon;
        } else {
            boolean conflict = false;
            for (int i = 0 ;i < restriction.length;i++) {
                Set allocatables = (Set)appointmentMap.get( restriction[i]);
                if (allocatables.contains(allocatable)) {
                    conflict = true;
                    break;
                }
            }
            if ( conflict )
                return "C"; // conflictIcon;
            else
                return "X"; // "X" not allways available
        }
    }

    // calculates the number of conflicting appointments for this allocatable
    private void calcConflictingAppointments(Allocatable allocatable) {
    	if (conflictingAppointments == null || conflictingAppointments.length!=appointments.length)
    		conflictingAppointments = new boolean[appointments.length];
    	conflictCount = 0;
    	permissionConflictCount = 0;
    	for (int i=0;i<appointments.length;i++) {
    		Set allocatables = (Set) appointmentMap.get(appointments[i]);
    		if (allocatables != null && allocatables.contains(allocatable))
    		{
    			conflictingAppointments[i] = true;
    			conflictCount ++;
    		} 
    		else
    			if (!isAllowed( allocatable, appointments[i] ) ) {
    				conflictingAppointments[i] = true;
    				conflictCount ++;
    				permissionConflictCount ++;
    			} 
    			else
    			{
    				conflictingAppointments[i] = false;
    			}
    	}
    }

    // returns if the user is allowed to allocate the passed allocatable
    private boolean isAllowed( Allocatable allocatable, Appointment appointment ) {
    	Date start = appointment.getStart();
    	Date end = appointment.getMaxEnd();
    	return allocatable.canAllocate(user, start, end, today );
    }
    
    public class OccupationTableCellRenderer extends DefaultTableCellRenderer 
    {
		private static final long serialVersionUID = 1L;

		public Component getTableCellRendererComponent (JTable table, Object value, boolean isSelected, boolean hasFocus, int r, int c) 
        {
            Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, r, c);
			int row = convertRowIndexToModel(table, r);		
            super.setHorizontalAlignment(SwingConstants.LEFT);

            LinesBorder cellBorder = new LinesBorder(Color.BLACK); 
            cell.setBackground( Color.WHITE );
           
	        if( value instanceof OccupationCell ) {
	        	OccupationCell occCell = (OccupationCell) value;
	        	final Reservation reservation = occCell.getReservation();
	        	if( reservation != null ) {
		            cellBorder.setThickness(2, NORTH);
	    	        

	        		if(occCell.leftBound=='[')
	        			cellBorder.setThickness(2, WEST);
	        		else 
	        			if(occCell.leftBound=='<')
	        				cellBorder.setThickness(10, WEST);
	        			else
	        				cellBorder.setThickness(0, WEST);
	    	        
	        		if(occCell.rightBound==']')
	        			cellBorder.setThickness(2, EAST);
	        		else 
	        			if(occCell.rightBound=='>')
	        				cellBorder.setThickness(10, EAST);
	        			else
	            			cellBorder.setThickness(0, EAST);
	        		
	    	        cellBorder.setThickness(2, SOUTH);

	        		Color color = getColorForClassifiable( reservation );
	    	       	if(color==null)
	           			cell.setBackground( Color.WHITE );
	           		else
	           			cell.setBackground( color);
	       			setBorder(cellBorder);
	        	}
	           	else
		           	if( occCell.getTypeId() == 0) { // Free 
		           		cell.setBackground( Color.LIGHT_GRAY);
		            }
		           	else 
		           		if( occCell.getTypeId() == -1) { // Forbidden
		           			cell.setBackground( Color.BLACK );	                    
		           		}
		           		else  
		           			if( occCell.getTypeId() == -2) { // FirstFit 		    	    	
		           				cellBorder.setThickness(1, NORTH);
		           				if(occCell.leftBound=='[')
		           					cellBorder.setThickness(2, WEST);
		           				else
		           					cellBorder.setThickness(0, WEST);	        				
		           				cellBorder.setThickness(1, SOUTH);
		           				if(occCell.rightBound==']')
		           					cellBorder.setThickness(2, EAST);
		           				else
		           					cellBorder.setThickness(0, EAST);	
		           				setBorder(cellBorder);
		           				cell.setBackground( Color.GREEN );	                    
		           			}
	        	setText("");
                if( c == table.getSelectedColumn() &&  r == table.getSelectedRow()) { // identify selected cell
                	// Selector context Popup
       				cellBorder.setThickness(0, NORTH);
       				cellBorder.setThickness(5, WEST);
       				cellBorder.setThickness(0, SOUTH);
       				cellBorder.setThickness(5, EAST);	
                	cell.setBackground( Color.GRAY );
       				setBorder(cellBorder);
	        	}
	        }
	        
	        if( value instanceof AllocationCell )
	        {
	          	Font font = cell.getFont();
       			cell.setFont(font.deriveFont(Font.BOLD));
       			AllocationCell allcCell = (AllocationCell) value;
       			// handle the first column: Resources
       			Allocatable allc = allcCell.allocatable;
   				cell.setBackground( Color.WHITE);
            	setText(allc.getName(locale));
       			cell.setBackground( Color.WHITE );
	        	Color color = getColorForClassifiable( allc );
	    	    if(color!=null)
	           		cell.setBackground( color);
	       	}
			
	        if( value instanceof Integer )
	        {
	      		if( c == OccupationTableModel.CALENDAR_IN_DAYS) {
		      		int days = (Integer) value;
	      			if(days == Integer.MAX_VALUE) {
	      					setText("");
	      			}
		      		else {
		      			cell.setBackground( Color.WHITE);
			        	if(days >= 0) {
			        		formatDaysInOut.setPositivePrefix("+"); 
			        		setText(formatDaysInOut.format(days));
			        	}
			        	else {
			      			formatDaysInOut.setPositivePrefix("-"); 
			      			setText(formatDaysInOut.format(days));
			          	}
	      			}
		      	}
	      		else {
		      		if( c == OccupationTableModel.CALENDAR_OUT_DAYS ) {
		      			cell.setBackground( Color.WHITE);
			      		int days = (Integer) value;
		      			if(days == Integer.MAX_VALUE) {
	          				Object daysIn = occupationTableModel.getValueAt(row,OccupationTableModel.CALENDAR_IN_DAYS);				
      						if((Integer) daysIn == Integer.MAX_VALUE)
      							setText("");
      						else
      							setText("?");
          				}
			      		else {
				        	if(days >= 0)
				        		formatDaysInOut.setPositivePrefix("+"); 
				        	else 
				      			formatDaysInOut.setPositivePrefix("-"); 
				        	setText(formatDaysInOut.format(days));
		      			}
			      	}
	      		}
	        }
	        
      		if(c == OccupationTableModel.CALENDAR_SEQUENCE_NUMBER) {
   				Font font = cell.getFont();
   				cell.setFont(font.deriveFont(Font.BOLD));
   				AllocationCell allcCell = (AllocationCell) occupationTableModel.getValueAt(row,OccupationTableModel.CALENDAR_RESOURCE);
    			cell.setBackground( Color.WHITE);
    			Allocatable alloc = allcCell.allocatable;
        		AttributeType type = EndOfLifeArchiver.getEndOfLifeType(alloc);
        		if(type != null) {
	        		Classification classification = alloc.getClassification();
	       	    	Object endlife = classification.getValue("_endoflife");
	       	    	if(endlife==null) {
    	        			Object daysOut  = occupationTableModel.getValueAt(row, OccupationTableModel.CALENDAR_OUT_DAYS);
    	        			if( (Integer) daysOut == Integer.MAX_VALUE) // old enough to be archived
           	    				cell.setBackground( Color.GREEN);
           	    			else
    	       	    			cell.setBackground( Color.ORANGE);
           	    		}
       	    		}
   	    		setText(Integer.toString(r+1));
   	    		return cell;
   			}

	        if( value instanceof String ) {
	        	if( c == OccupationTableModel.CALENDAR_IN_DAYS || c == OccupationTableModel.CALENDAR_OUT_DAYS) {
	        		cell.setBackground( Color.WHITE);
	        		setText((String) value);
       			}
       		}
	        
	        if(value==null) {
   				cell.setBackground( Color.WHITE);
   				setText((value == null) ? "" : "Unknown"); 
   			}

    		if(c == occupationTableModel.getTodayColumn() && c > OccupationTableModel.CALENDAR_EVENTS) {
    			cell.setBackground( Color.decode("#bb5823"));
   			}
   	        
    		if(c >= OccupationTableModel.CALENDAR_EVENTS) {
   				if (hasFocus)
   					cell.setForeground( Color.GRAY );
            }
            
	        //setText((value == null) ? "" : value.toString()); 
            return cell;
        }
    }
    
    private int convertRowIndexToModel(JTable table, int r) {
        return sorter.modelIndex(r);
    }

     Color getColorForClassifiable( Classifiable classifiable ) {
        Classification c = classifiable.getClassification();
        Attribute colorAttribute = c.getAttribute("color");
        String color = null;
        if ( colorAttribute != null) {
            Object hexValue = c.getValue( colorAttribute );
            if ( hexValue != null) {
                if ( hexValue instanceof Category) {
                    hexValue = ((Category) hexValue).getAnnotation( CategoryAnnotations.KEY_NAME_COLOR );
                }
                if ( hexValue != null) {
                    color = hexValue.toString();
                }
            }
        }
        if ( color != null)
        {
            try
            {
                return Color.decode(color);
            }
            catch (NumberFormatException ex)
            {
                getLogger().warn( "Can't parse Color " + color  + " " +ex.getMessage());
            }
        }
        return null;
    }

    class GroupableTableCellRenderer extends DefaultTableCellRenderer {

		private static final long serialVersionUID = 1L;

        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int r, int c) {
            JTableHeader header = table.getTableHeader();
            if (header != null) {
                setBackground(Color.WHITE);
                Font font = getFont();
                setFont(font.deriveFont(Font.BOLD));
            }

            setHorizontalAlignment(SwingConstants.CENTER);
            setText(value != null ? value.toString() : " ");
            setBorder(UIManager.getBorder("TableHeader.cellBorder"));
            return this;
        }
    }
    
	private final String dayNames[] = new DateFormatSymbols().getShortWeekdays();
    class DayOfWeekHeaderRenderer extends DefaultTableCellRenderer {

		private static final long serialVersionUID = 1L;
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int r, int c) {
        	if(value instanceof String)
        	{
        		int dd = Integer.parseInt(((String) value));
        		setText(dayNames[dd]);
        		if( dd == Calendar.SUNDAY || dd == Calendar.SATURDAY)
        			setBackground(Color.LIGHT_GRAY);
        		else
                    setBackground(Color.WHITE);
                Font font = getFont();
                setFont(font.deriveFont(Font.BOLD));
        	}
        	else {
                setText(value != null ? value.toString() : " ");
        	}
            
            //if(c == occupationTableModel.getTodayColumn())
            //    setBackground(Color.decode("#bb5823"));
            
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorder(UIManager.getBorder("TableHeader.cellBorder"));
            return this;
        }
    }
    
    class countRenderer extends DefaultTableCellRenderer {

		private static final long serialVersionUID = 1L;
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int r, int c) {

            setText(value != null ? value.toString() : " ");
            LinesBorder cellBorder = new LinesBorder(Color.GRAY);
            cellBorder.setThickness(2, NORTH);
            cellBorder.setThickness(1, WEST);
            cellBorder.setThickness(1, EAST);
            cellBorder.setThickness(0, SOUTH);
            setBorder(cellBorder);
            setBackground( Color.WHITE);
            setHorizontalAlignment(SwingConstants.CENTER);
            return this;
        }
    }

    public class PopupTableHandler extends MouseAdapter {

	    void showPopup(MouseEvent me) throws RaplaException{
	    	// Conflict   " " = Resource is available
	    	// Conflict   "X" = Resource is not allways available
	    	// Conflict   "C" = Resource is used at requested timestamp
	    	// Forbidden  "F" = Resource is not available at the specified timestamp
	    	// First Fit proposal "=" = Resource is available for the request period
	        Point p = new Point(me.getX(), me.getY());
	        int r = table.getSelectedRow();
	        int c = table.getSelectedColumn();
	        if( r  < 0 || c < 0)
	        	return;
	        r = convertRowIndexToModel(table,r);

	        Object obj = occupationTableModel.getValueAt(r, c);
	        if (obj instanceof OccupationCell ) {
	        	OccupationCell occCell = (OccupationCell) obj;
		        if(occCell.getTypeId() > 0) 
		        	editPopup(occCell, r, c, p);
		        else 
		        	newPopup(occCell, r, c, p);
		        }
	        
	        if (obj instanceof AllocationCell ) {
	        		AllocationCell alcCell = (AllocationCell) obj;
	        		if(alcCell.allocatable !=null ) {
	        			Object daysOut  = occupationTableModel.getValueAt(r, OccupationTableModel.CALENDAR_OUT_DAYS);
	        			Object daysIn  = occupationTableModel.getValueAt(r, OccupationTableModel.CALENDAR_IN_DAYS);
        				if((Integer) daysOut == Integer.MAX_VALUE && (Integer) daysIn == Integer.MAX_VALUE) 
    						archivePopup(alcCell, r, c, p, 0);	
	        			else
		        			if( ((Integer) daysOut < 0)) 
		        				archivePopup(alcCell, r, c, p,(Integer) daysOut);
	        		}	
	        }
	        return;
	    }
	    
		public void newPopup(OccupationCell occCell, int r, int c, Point p) throws RaplaException {    
	        Calendar calendarStart = null;
	        Calendar calendarEnd = null;
	        
	        if( occCell.getTypeId() == -2) { 
	        	int cs = occupationTableModel.findStartSlot(r, c, -2) - OccupationTableModel.CALENDAR_EVENTS; // corrected start
				calendarStart = (Calendar) calendarDS.clone();
				calendarStart.add(Calendar.DATE, cs);
				calendarEnd = (Calendar) calendarStart.clone();
	        	calendarEnd.add(Calendar.DATE, occupationTableModel.getFreeSlot());
			} else 
				if( occCell.getTypeId() == 0) { 
					calendarStart = (Calendar) calendarDS.clone();
	        		calendarStart.add(Calendar.DATE, c - OccupationTableModel.CALENDAR_EVENTS);
					calendarEnd = (Calendar) calendarStart.clone();
					calendarEnd.add(Calendar.DATE, 1);
				} else 
					if( occCell.getTypeId() == -1) {
						return;
					}
	                
	    	mutableReservation = getClientFacade().newReservation();
	    	Appointment appointment = null;
	        appointment = getClientFacade().newAppointment( calendarStart.getTime(), calendarEnd.getTime(), repeatingType, repeatingDuration );
	        appointment.setWholeDays(true);
	        mutableReservation.addAppointment(appointment);
	        AllocationCell alcCell = (AllocationCell) occupationTableModel.getValueAt(r, OccupationTableModel.CALENDAR_RESOURCE);
	    	Allocatable alloc = alcCell.allocatable;
	        mutableReservation.addAllocatable(alloc);   
	        JPopupMenu popup = new JPopupMenu();
	        newAdapter menuAction = new newAdapter(mutableReservation,0);
	        JMenuItem newItem = new JMenuItem(getString("new"),getIcon( "icon.new"));
	        newItem.setActionCommand("new");
	        newItem.addActionListener(menuAction);
	        popup.add(newItem);
	        popup.show( table, p.x, p.y); 
	    }

		SimpleDateFormat sdfdatetime = new SimpleDateFormat("yyyy-MM-dd");
		public void archivePopup(AllocationCell alcCell, int r, int c, Point p, int days) throws RaplaException {    
	    	Allocatable alloc = alcCell.allocatable; 
	    	
    		AttributeType type = EndOfLifeArchiver.getEndOfLifeType(alloc);
    		if(type == null)
				return;

	        JPopupMenu popup = new JPopupMenu();
	        newAdapter menuAction = new newAdapter(alloc, days);
	        JMenuItem archiveItem;
	        if(canModify(alloc)) {
	        	if (type.equals(AttributeType.BOOLEAN))
	        		archiveItem = new JMenuItem(getI18n().getString("archive_yn"),getIcon( "icon.archive"));
	        	else {
		        if(days < 0) {
			        	archiveItem = new JMenuItem(getI18n().format("forcearchive_lt", sdfdatetime.format(DateTools.addDays(today, (int) (archiveAge + days))), archiveAge),getIcon( "icon.archive"));
		        	 archiveItem.setBackground(Color.ORANGE);
		        }	 
		        else {
		        		archiveItem = new JMenuItem(getI18n().format("archiveda_gt", sdfdatetime.format(today), archiveAge),getIcon( "icon.archive"));
		        	 archiveItem.setBackground(Color.GREEN);
		        }
			   }
			    archiveItem.setEnabled(true);
		        archiveItem.setActionCommand("archive");
		        }
	        else {
	        	 archiveItem = new JMenuItem(getString("permission.denied"),getIcon("icon.no_perm"));
	 	         archiveItem.setEnabled(true);
	        }
	        archiveItem.addActionListener(menuAction);
	        popup.add(archiveItem);
	        popup.show( table, p.x, p.y); 
	    }
				
	    public void editPopup(OccupationCell occCell, int r, int c, Point p) throws RaplaException {  

       		Reservation reservation = occCell.getReservation();
    	
	        JPopupMenu popup = new JPopupMenu();
	        newAdapter menuAction = new newAdapter(reservation, 0);
	        JMenuItem editItem = new JMenuItem(getString("edit"),getIcon( "icon.edit"));
	        editItem.setActionCommand("edit");
	        editItem.addActionListener(menuAction);
	        editItem.setEnabled(canModify(reservation) || getQuery().canExchangeAllocatables(reservation));
	        popup.add(editItem);
	              
	        JMenuItem deleteItem = new JMenuItem(getString("delete"),getIcon( "icon.delete"));
	        deleteItem.setActionCommand("delete");
	        deleteItem.addActionListener(menuAction);
	        deleteItem.setEnabled(canModify(reservation));
	        popup.add(deleteItem);
	        
	        JMenuItem viewItem = new JMenuItem(getString("info"),getIcon( "icon.help"));
	        viewItem.setActionCommand("info");
	        viewItem.addActionListener(menuAction);
	        User owner = reservation.getOwner();
	        try 
	        {
	            User user = getUser();
	            boolean canView = getQuery().canReadReservationsFromOthers( user) || user.equals( owner);
	            viewItem.setEnabled( canView);
	        } 
	        catch (RaplaException ex)
	        {
	            getLogger().error( "Can't get user",ex);
	        }
	        popup.add(viewItem);
	        popup.show( table, p.x, p.y); 
	    }
	    
	    /** Implementation-specific. Should be private.*/
	    public void mousePressed(MouseEvent me) {
	        if (me.isPopupTrigger())
				try {
					showPopup(me);
				} catch (RaplaException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    }
	    
	    /** Implementation-specific. Should be private.*/
	    public void mouseReleased(MouseEvent me) {
	        if (me.isPopupTrigger())
				try {
					showPopup(me);
				} catch (RaplaException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    }
	    
	    /** double click*/
	    public void mouseClicked(MouseEvent me) {
	        if (me.getClickCount() > 1 )
	        {
	            //Point p = new Point(me.getX(), me.getY());
		        int r = table.getSelectedRow();
		        int c = table.getSelectedColumn();
		        if( r  < 0 || c < 0)
		        	return;
		        r = convertRowIndexToModel(table, r);
	            if(c<OccupationTableModel.CALENDAR_EVENTS) // no menu on fixed columns
	            	return;
	            Object occupation = occupationTableModel.getValueAt(r, c);
	            if(occupation instanceof OccupationCell) {
	            	OccupationCell occCell = (OccupationCell) occupation;
	            	Reservation reservation = occCell.getReservation();
		            if( reservation != null) {
			    		try {
			    			if(canModify(reservation) || getQuery().canExchangeAllocatables(reservation))
			        			reservation.setSelectedSlotDate(occupationTableModel.getColumnDate(c));            		
			    				getReservationController().edit(reservation);
			        		} catch (RaplaException e) {
			        			// TODO Auto-generated catch block
			        			e.printStackTrace();
			        		}
			    		}
	        		}
	    		}
	    	}
	    }
    
    public class newAdapter implements ActionListener {
    	private Object obj;
    	private int days;

    	newAdapter(Object obj, int days) {
    		this.obj = obj;
    		this.days = days;
    	}

	public void actionPerformed(ActionEvent evt) {
        try {
        	int c = table.getSelectedColumn();
        	//int r = table.getSelectedRow();
        	if(evt.getActionCommand().equals("new")) {
        		((Reservation) obj).setSelectedSlotDate(occupationTableModel.getColumnDate(c));
    			getReservationController().edit((Reservation)obj);
        	}
        	else 
        		if(evt.getActionCommand().equals("edit")) {
        			((Reservation) obj).setSelectedSlotDate(occupationTableModel.getColumnDate(c));            		
        			getReservationController().edit((Reservation) obj);
        		}
        		else {
            		if(evt.getActionCommand().equals("delete")) {
            			AppointmentAction deleteAction = new AppointmentAction( getContext(), getComponent(),null);
            			Appointment[] apps = ((Reservation) obj).getAppointments();
            			// get selected date
            	        //int r = table.getSelectedRow();
            			deleteAction.setDelete(apps[0],occupationTableModel.getColumnDate(c));
                		deleteAction.actionPerformed(null);
            			update();
            		}
            		else
	        			if(evt.getActionCommand().equals("info")) {
	        				AppointmentAction viewAction = new AppointmentAction( getContext(), getComponent(),null);
	        				Appointment[] apps = ((Reservation) obj).getAppointments();
	            			viewAction.setView(apps[0]);
	            			viewAction.actionPerformed(null);
	        			}
	                	else 
	                		if(evt.getActionCommand().equals("archive")) {
	                			Allocatable alloc = (Allocatable) obj;
	                    		AttributeType type = EndOfLifeArchiver.getEndOfLifeType(alloc);
	                    		if(type == null)
	                    			return;
	                    		
	                			AppointmentAction archiveAction = new AppointmentAction( getContext(), getComponent(),null);
	                			
	                			if(days < 0) {
	                				Date endOfLife = DateTools.addDays(today, (int) (archiveAge + days));
	                				if(sendOKMsg("confirm", endOfLife) == 0); // OK
	                					endOfLifeAllocatable (alloc, endOfLife);
	                			}	
	                			else
                					endOfLifeAllocatable (alloc, today);
	                				
	                			archiveAction.actionPerformed(null);
	                		}
        		}
		} catch (RaplaException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	  }
	}

	public  void endOfLifeAllocatable (Allocatable alloc, Date endDate) throws RaplaException {
		Classification classification = alloc.getClassification();
		AttributeType type = EndOfLifeArchiver.getEndOfLifeType(alloc);
		if(type == null)
			return;
		try {
			
			Object endlife = classification.getValue("_endoflife");
			if(type.equals(AttributeType.DATE)) {
				if(endlife == null || endDate.after((Date) endlife)) {	
	    		Allocatable editAllocatable = (Allocatable)getClientFacade().edit( alloc);
	    		editAllocatable.getClassification().setValue("_endoflife", endDate);
	            getClientFacade().store( editAllocatable );
				}
			}
			else if(type.equals(AttributeType.BOOLEAN)) {
		    	Allocatable editAllocatable = (Allocatable)getClientFacade().edit( alloc);
		    	editAllocatable.getClassification().setValue("_endoflife", true);
		        getClientFacade().store( editAllocatable );
			}
		}
        catch (NoSuchElementException e)
        {
        	sendOKMsg("noendoflife",null);
        }
        return;
	}
	
	public int sendOKMsg(String msg, Date endOfLife) throws RaplaException {
		String message=null;
		if(endOfLife== null)
			message = getString(msg);
		else
			message = getI18n().format(msg,endOfLife);
		DialogUI info = DialogUI.create(getContext(),getComponent(),true,getString("ok"),message);
        info.start();
        return info.getSelectedIndex(); // 0 = OK -1 = OS UI Windows Close
    }
	
	public class AllocatableSortByName implements Comparator<Allocatable> {
		public int compare(Allocatable o1, Allocatable o2) {
			return o1.getName(locale).compareTo(o2.getName(locale));
		}
	}
	
	public class LinesBorder extends AbstractBorder implements SwingConstants { 
		
		private static final long serialVersionUID = 1L;
		protected int northThickness;
		protected int southThickness;
		protected int eastThickness;
		protected int westThickness;  
		protected Color northColor;
		protected Color southColor;
		protected Color eastColor;
		protected Color westColor;
		  
		public LinesBorder(Color color) {
			this(color, 1);
		}

		public LinesBorder(Color color, int thickness)  {
		    setColor(color);
		    setThickness(thickness);
	    }

		public LinesBorder(Color color, Insets insets)  {
		    setColor(color);
		    setThickness(insets);
		}

		public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
			Color oldColor = g.getColor();
		    
		    g.setColor(northColor);
		    for (int i = 0; i < northThickness; i++)  {
		      g.drawLine(x, y+i, x+width-1, y+i);
		    }
		    g.setColor(southColor);
		    for (int i = 0; i < southThickness; i++)  {
		      g.drawLine(x, y+height-i-1, x+width-1, y+height-i-1);
		    }
		    g.setColor(eastColor);
		    for (int i = 0; i < westThickness; i++)  {
		      g.drawLine(x+i, y, x+i, y+height-1);
		    }
		    g.setColor(westColor);
		    for (int i = 0; i < eastThickness; i++)  {
		      g.drawLine(x+width-i-1, y, x+width-i-1, y+height-1);
		    }
		    g.setColor(oldColor);
		  }

		public Insets getBorderInsets(Component c)       {
		    return new Insets(northThickness, westThickness, southThickness, eastThickness);
		}

		public Insets getBorderInsets(Component c, Insets insets) {
		    return new Insets(northThickness, westThickness, southThickness, eastThickness);    
		}

		public boolean isBorderOpaque() {
			return true;
		}
		    
		public void setColor(Color c) {
		    northColor = c;
		    southColor = c;
		    eastColor  = c;
		    westColor  = c;
		}
		  
		public void setColor(Color c, int direction) {
			switch (direction) {
				case NORTH: northColor = c; break;
				case SOUTH: southColor = c; break;
				case EAST:  eastColor  = c; break;
				case WEST:  westColor  = c; break;
				default: 
			}	
		}
		    
		public void setThickness(int n) {
		    northThickness = n;
		    southThickness = n;
		    eastThickness  = n;
		    westThickness  = n;
		}
		    
		public void setThickness(Insets insets) {
		    northThickness = insets.top;
		    southThickness = insets.bottom;
		    eastThickness  = insets.right;
		    westThickness  = insets.left;
		}
		  
		public void setThickness(int n, int direction) {
		    switch (direction) {
		     	case NORTH: northThickness = n; break;
		     	case SOUTH: southThickness = n; break;
		     	case EAST:  eastThickness  = n; break;
		     	case WEST:  westThickness  = n; break;
		     	default: 
		    }
		}

		public void append(LinesBorder b, boolean isReplace) {
			if (isReplace) {
				northThickness = b.northThickness;
				southThickness = b.southThickness;
				eastThickness  = b.eastThickness;
				westThickness  = b.westThickness;
		    } else {
		    	northThickness = Math.max(northThickness ,b.northThickness);
		    	southThickness = Math.max(southThickness ,b.southThickness);
		    	eastThickness  = Math.max(eastThickness  ,b.eastThickness);
		    	westThickness  = Math.max(westThickness  ,b.westThickness);
		    }
		  }

		  public void append(Insets insets, boolean isReplace) {
			  if (isReplace) {
				  northThickness = insets.top;
				  southThickness = insets.bottom;
				  eastThickness  = insets.right;
				  westThickness  = insets.left;
		    } else {
		    	northThickness = Math.max(northThickness ,insets.top);
		    	southThickness = Math.max(southThickness ,insets.bottom);
		    	eastThickness  = Math.max(eastThickness  ,insets.right);
		    	westThickness  = Math.max(westThickness  ,insets.left);
		    }
		  }
		}
	
	
	class AllocatableCellRenderer extends JComponent implements TableCellRenderer {
		  
		private static final long serialVersionUID = 1L;

		public AllocatableCellRenderer() {
		    super();
		  }
		  
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int r, int c) {
			int row =convertRowIndexToModel(table,r);
			OccupationCell occCell = null;
			if(occupationTableModel.getTodayColumn()!= 0)
				occCell = (OccupationCell) occupationTableModel.getValueAt(row,occupationTableModel.getTodayColumn());
			else
				occCell = (OccupationCell) occupationTableModel.getValueAt(row,OccupationTableModel.CALENDAR_EVENTS);
				
			if(occCell!=null) {
				final Reservation reservation = occCell.getReservation();
                if( reservation != null  ) {
					AllocatableColors controlColors = new AllocatableColors(reservation); 
			   		return controlColors;
			    }
			}
			return this;
		}
	}
	
	public class AllocatableColors extends JComponent {
   		Font textFont = new Font("SanSerif", Font.BOLD, 12);
   	    FontMetrics fontMetrics = getFontMetrics(textFont);
   		
		private static final long serialVersionUID = 1L;
		
		Reservation reservation;
		
		AllocatableColors(Reservation reservation) {
			this.reservation = reservation;
		}
		
		public void paint(Graphics g) {
			
				Classification classification = null;
        		
        		// Resource check points
		        List arrayList =  Arrays.asList(reservation.getAllocatables());
		        
		        Comparator comp = new Comparator() {
	                public int compare(Object o1, Object o2) {
	                    if ((((Allocatable) o1).getClassification().getType().getName(locale)).compareTo(((Allocatable) o2).getClassification().getType().getName(locale)) > 0)
	                        return 1;
	                    if ((((Allocatable) o1).getClassification().getType().getName(locale)).compareTo(((Allocatable) o2).getClassification().getType().getName(locale)) < 0)
	                        return -1;
	                    return 0;
	                }
	            };
	            
		        Collections.sort(arrayList,comp);
		        Iterator it = arrayList.iterator();
		        int i=0;
		        Color color = null;
		        while (it.hasNext()) {
		            Allocatable allocatable = (Allocatable) it.next();
	    			classification = allocatable.getClassification();
	    			g.drawRect (1 + i*22, 0, 19, 14);
		            color = getColorForClassifiable( allocatable );
		            if ( color == null )
		                g.setColor(Color.WHITE); //RaplaColorList.getHexForColor( RaplaColorList.getResourceColor(i));
		            else
		    			g.setColor(color);
		    			g.fillRect (2 + i*22, 1, 18, 13);
		    			g.setFont(textFont);
		    			g.setColor(Color.BLACK);
	           		
		            g.drawString(classification.getType().getName(locale).substring(0,1), 5 + i*22, 12); // First character from name
	           		i++;
		        }
		        
		        int width = i * 22 + 2; // start of text comments
		        
		        it = arrayList.iterator();

		        while (it.hasNext()) {
		            Allocatable allocatable = (Allocatable) it.next();
	    			classification = allocatable.getClassification();
	    			width = getControlData(classification, g, width);
		        }
		        
                // Event check points
        		classification = reservation.getClassification();
        		width = getControlData(classification, g, width);	        

			
		}
		
		private int getControlData(Classification classification, Graphics g, int inWidth) {
			Attribute[] attributes = classification.getAttributes();
			String color = null;
			String txt = null;

			int outWidth = inWidth;
			for (int k=0; k<attributes.length; k++)
			{      
				String attributeKey = attributes[k].getKey();
				if(attributes[k].getKey().contains("check_")) {
					if (attributes[k].getType() == AttributeType.CATEGORY) {
						Category cat = (Category) classification.getValue(attributeKey);  
						Category rootCategory = (Category) attributes[k].getConstraint(ConstraintIds.KEY_ROOT_CATEGORY);
						if ( cat != null ) {
				            txt = " " + cat.getPath(rootCategory, locale);
							color = cat.getAnnotation(CategoryAnnotations.KEY_NAME_COLOR, null) ;
					        if ( color == null )
					           	g.setColor(Color.BLACK);
					        else
						      	g.setColor(Color.decode(color));
					        g.setFont(textFont);
						    g.drawString(txt, outWidth , 12);
						    outWidth += fontMetrics.stringWidth(txt);
				        }
					}
				}
			}
			return outWidth;
		}
	}
	
	class RowHeaderRenderer extends JLabel implements ListCellRenderer {
		private static final long serialVersionUID = 1L;
		RowHeaderRenderer(JTable table) {
			JTableHeader header = table.getTableHeader();
			setOpaque(true);
			setBorder(UIManager.getBorder("TableHeader.cellBorder"));
			setHorizontalAlignment(CENTER);
			setForeground(header.getForeground());
			setBackground(header.getBackground());
			setFont(header.getFont());
		}
	
		public Component getListCellRendererComponent( JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			setText((value == null) ? "" : value.toString());
			return this;
		}
	}
}
