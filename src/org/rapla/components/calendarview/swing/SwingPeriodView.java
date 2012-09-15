/*--------------------------------------------------------------------------*
 | Copyright (C) 2006 Gereon Fassbender, Christopher Kohlhaas               |
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
package org.rapla.components.calendarview.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;

import org.rapla.components.calendarview.Block;
import org.rapla.components.calendarview.Builder;
import org.rapla.components.calendarview.swing.SelectionHandler.SelectionStrategy;
import org.rapla.components.layout.TableLayout;

/** @deprecated not supported anymore by the rapla team*/
public class SwingPeriodView extends AbstractSwingCalendar
{

    private PeriodDaySlot[] slots ;
    int weeksNr ;
    long startTime ;
    DraggingHandler draggingHandler = new DraggingHandler(this, true);
    SelectionHandler selectionHandler = new SelectionHandler(this);
    	
    
    TimeScale        timeScale = new TimeScale();
    BoxLayout        boxLayout2= new BoxLayout(jCenter, BoxLayout.X_AXIS);
    private int rowSize = 4;
    private int rowsPerHour = 4;
    private int startHour= 0;
    private int endHour= 24;

    public SwingPeriodView() {
        this(true);
    }

    public SwingPeriodView(boolean showScrollPane) {
        super( showScrollPane );
        jCenter.setLayout(boxLayout2);
        jCenter.setAlignmentY(JComponent.TOP_ALIGNMENT);
        jCenter.setAlignmentX(JComponent.LEFT_ALIGNMENT);
        if ( showScrollPane ) {
            scrollPane.setRowHeaderView(timeScale);
        } else {
            component.add(timeScale,"0,1");
        }
    }

    public void setPeriodEnd(Date end) {
        if ( end == null ) {
            this.setEndDate( null );
            return;
        }
        Calendar calendar = createCalendar();
        calendar.setTime( end );
        //  calendar.set(Calendar.DAY_OF_WEEK, 6);
        calendar.set(Calendar.HOUR_OF_DAY,23);
        calendar.set(Calendar.MINUTE,59);
        calendar.set(Calendar.SECOND,59);
        calendar.set(Calendar.MILLISECOND,999);
        this.setEndDate( calendar.getTime() );
    }

    public void setLocale(Locale locale) {
        super.setLocale( locale );
        if ( timeScale != null )
            timeScale.setLocale( locale );
    }

    public void setBackground(Color color) {
        super.setBackground(color);
        if (timeScale != null)
            timeScale.setBackground(color);
    }

    /** The granularity of the selection rows.
     * <ul>
     * <li>1:  1 rows per hour =   1 Hour</li>
     * <li>2:  2 rows per hour = 1/2 Hour</li>
     * <li>3:  3 rows per hour = 20 Minutes</li>
     * <li>4:  4 rows per hour = 15 Minutes</li>
     * <li>6:  6 rows per hour = 10 Minutes</li>
     * <li>12: 12 rows per hour =  5 Minutes</li>
     * </ul>
     * Default is 4.
     */
    public void setRowsPerHour(int rowsPerHour) {
        this.rowsPerHour = rowsPerHour;
    }

    /** @see #setRowsPerHour */
    public int getRowsPerHour() {
        return rowsPerHour;
    }

    /** The size of each row (in pixel). Default is 15.*/
    public void setRowSize(int rowSize) {
        this.rowSize = rowSize;
    }

    public int getRowSize() {
        return rowSize;
    }

    public void setWorktime(int startHour, int endHour) {
        this.startHour = startHour;
        this.endHour = endHour;
        if (getStartDate() != null)
            calcMinMaxDates( getStartDate() );
    }


    public void calcMinMaxDates(Date date) {
        Calendar calendar = createCalendar();
        calendar.setTime( date );
        // calendar.set(Calendar.DAY_OF_WEEK, 0);
        calendar.set(Calendar.HOUR_OF_DAY,0);
        calendar.set(Calendar.MINUTE,0);
        calendar.set(Calendar.SECOND,0);
        calendar.set(Calendar.MILLISECOND,0);
        this.setStartDate( calendar.getTime() );

        if ( this.getEndDate() == null
             || (this.getEndDate().getTime() - this.getStartDate().getTime())<7L*7*24*3600*1000
             ) {
            calendar.add(Calendar.DATE, 7*7); // 7 weeks (no scroll bar)
            this.setEndDate( calendar.getTime() );
        }
    }


    public Collection<Block> getBlocks() {
        ArrayList<Block> list = new ArrayList<Block>();
        for (int i=0;i<slots.length;i++) {
            list.addAll(slots[i].getBlocks());
        }
        return Collections.unmodifiableCollection( list );
    }

    public void setEditable(boolean b) {
        super.setEditable( b);
        if ( slots == null )
            return;
        // Hide the rest
        for (int i= 0;i<slots.length;i++) {
            PeriodDaySlot slot = slots[i];
            if (slot == null) continue;
            slot.setEditable(b);
        }
    }

    public void rebuild() {

        int start = startHour;
        int end = endHour;

        selectionHandler.setSelectionStrategy(SelectionStrategy.PERIOD);

        // calculate the blocks
        Iterator<Builder> it= builders.iterator();
        Date startDate = getStartDate();
        Date endDate = getEndDate();
        while (it.hasNext()) {
            Builder b= (Builder)it.next();
            b.prepareBuild(startDate,endDate );
        }
        weeksNr = Math.max(1,(int)((endDate.getTime() - startDate.getTime())/(1000*3600*24*7)));

        slots = new PeriodDaySlot[7*weeksNr + 7]; // XXX Thierry Excoffier: Why +7

        int pixelPerHour = rowSize * rowsPerHour;

        // create fields
        for (int i=0; i<slots.length; i++) {
            createField(i);
        slots[i].setTimeIntervall(start,end);
        }

        timeScale.setTimeIntervall(start,end,pixelPerHour);
        timeScale.setBackground(component.getBackground());
        timeScale.setSmallSize(true);


        // clear everything
        jHeader.removeAll();
        jCenter.removeAll();
        // build Blocks
        it= builders.iterator();
        while (it.hasNext()) {
            Builder b= (Builder)it.next();
            if (b.isEnabled()) { b.build(this); }
        }
        TableLayout tableLayout= new TableLayout();
        jCenter.setLayout(tableLayout);
        // add headers
        for(int i=0; i<weeksNr; i++) {
            tableLayout.insertColumn(i, slotSize );
            jHeader.add( createSlotHeader( i*7 ) );
        }

        for (int i=0;i<7;i++) {
            tableLayout.insertRow(i, TableLayout.PREFERRED );
        }
        // add Fields
        int workDayNr=0;
        Calendar calendar = createCalendar();
        calendar.setTime( startDate );
        String[] days = new String[7];
        for (int i=0; i<7; i++, calendar.add(Calendar.DATE, 1)) {
            int day_of_week = calendar.get(Calendar.DAY_OF_WEEK);
            if ( excludeDays.contains( new Integer(day_of_week)) ) {
                continue;
            }
            days[workDayNr] =formatDayOfWeek(calendar.getTime()).toUpperCase();
            workDayNr++;
            for (int w=0; w<weeksNr; w++) {
                jCenter.add( slots[i+w*7] , w + "," + i);
           }
        }
        timeScale.setRepeat(workDayNr, days);

        jHeader.validate();
        jCenter.validate();
        if ( isEditable())
        {
        	updateSize(component.getSize().width);
        }
        component.revalidate();
        component.repaint();
    }


    private void createField(int pos)  {
        PeriodDaySlot c= new PeriodDaySlot(timeZone,slotSize-5,rowsPerHour,rowSize) ;
        c.setEditable(isEditable());
        c.setDraggingHandler(draggingHandler);
        c.addMouseListener(selectionHandler);
        c.addMouseMotionListener(selectionHandler);
        slots[pos]= c;
    };

    /** override this method, if you want to create your own header. */
    protected JComponent createSlotHeader(int day) {
        Calendar calendar = createCalendar();
        calendar.setTime( getStartDate() );
        calendar.add(Calendar.DATE,day);
        for (int i=0; i<7; i++) {
            if ( !excludeDays.contains(new Integer(calendar.get(Calendar.DAY_OF_WEEK)) )) {
                break;
            }
            calendar.add(Calendar.DATE, 1);
        }
        JLabel jLabel = new JLabel();
        jLabel.setBorder(isEditable() ? SLOTHEADER_BORDER : null);
        jLabel.setText(formatDayOfWeekDateMonth(calendar.getTime(),locale,getTimeZone()));

        jLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        jLabel.setHorizontalAlignment(JLabel.CENTER);
        jLabel.setOpaque(false);
        jLabel.setForeground(Color.black);
        Dimension dim = new Dimension(this.slotSize,20);
        jLabel.setPreferredSize( dim);
        jLabel.setMinimumSize( dim );
        jLabel.setMaximumSize( dim );
        return jLabel;
    }

    public void addBlock(Block bl, int column,int slot) {
        checkBlock( bl );

    int day = (int)((bl.getStart().getTime() - getStartDate().getTime())/(1000*3600*24));

        slots[day].putBlock((SwingBlock)bl, 0);
    }

    public int getSlotNr( DaySlot slot) {
        for (int i=0;i<slots.length;i++)
            if (slots[i] == slot)
                return i;
        throw new IllegalStateException("Slot not found in List");
    }

    public boolean isSelected(int nr)
	 {
    	PeriodDaySlot slot = getSlot(nr);
		 if ( slot == null)
		 {
			 return false;
		 }
		 return slot.isSelected();
	}


    int getRowsPerDay() {
        return rowsPerHour * (endHour - startHour);
    }

    PeriodDaySlot getSlot(int nr) {
        if ( nr >=0 && nr< slots.length) {
            return slots[nr];
        } else {
            return null;
        }
    }

    int getDayCount() {
        return weeksNr * 7;
    }


    int calcSlotNr(int x, int y) {
        for (int i=0;i<slots.length;i++) {
            if (slots[i] == null)
                continue;
            Point p = slots[i].getLocation();
            if ((p.x <= x)
                && (x <= p.x + slots[i].getWidth())
                && (p.y <= y)
                && (y <= p.y + slots[i].getHeight())
            ) {
                return i;
            }
        }
        return -1;
    }

    PeriodDaySlot calcSlot(int x,int y) {
        int nr = calcSlotNr(x, y);
        if (nr == -1) {
            return null;
        } else {
            return slots[nr];
        }
    }

    Date createDate(DaySlot slot, int row, boolean startOfRow) {
        if (!startOfRow) {
           row++;
        }
        Calendar calendar = createCalendar();
        calendar.setTime( getStartDate() );
        calendar.add( Calendar.DATE , getSlotNr( slot ) );
        int hour = startHour + ((PeriodDaySlot)slot).calcHour(row);
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE,((PeriodDaySlot)slot).calcMinute(row));
        calendar.set( Calendar.SECOND, 0 );
        calendar.set( Calendar.MILLISECOND, 0 );
        return calendar.getTime();
    }

	@Override
	protected boolean isEmpty(int column) {
		return false;
	}

	@Override
	public void updateSize(int width) {
		int slotCount = 0;
		int columnCount = 0;
		for (int i=0; i<slots.length; i++) {
			PeriodDaySlot largeDaySlot = slots[i];
            if ( isExcluded(i) )  {
                continue;
            }
            
            if ( largeDaySlot != null)
            {
            	slotCount += largeDaySlot.getSlotCount();
            	columnCount++;
            }
		}
		int newWidth = (width - timeScale.getWidth() - 30- columnCount* 24) / (Math.max(1,slotCount));
		for (PeriodDaySlot slot: slots)
    	{
    		if ( slot != null)
    		{
    			slot.updateSize(newWidth);
    		}
    	}
		setSlotSize(newWidth);
		
	}
	
	
}
