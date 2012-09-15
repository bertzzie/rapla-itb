/*--------------------------------------------------------------------------*
 | Copyright (C) 2006  Christopher Kohlhaas                                 |
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

package org.rapla.components.calendarview.html;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import org.rapla.components.calendarview.Block;
import org.rapla.components.calendarview.Builder;
import org.rapla.components.calendarview.swing.SwingWeekView;

public class HTMLWeekView extends AbstractHTMLView {
    private int endHour;
    private int minRow;
    private int maxRow;
    private int startHour;
    private boolean showNonEmptyExcluded;
    int m_rowsPerHour = 2;
    HTMLDaySlot[] multSlots ;
    ArrayList<Block> blocks = new ArrayList<Block>();
    ArrayList<Integer> blockStart = new ArrayList<Integer>();
    ArrayList<Integer> blockSize = new ArrayList<Integer>();

    /** The granularity of the selection rows.
     * <ul>
     * <li>1:  1 rows per hour =   1 Hour</li>
     * <li>2:  2 rows per hour = 1/2 Hour</li>
     * <li>3:  3 rows per hour = 20 Minutes</li>
     * <li>4:  4 rows per hour = 15 Minutes</li>
     * <li>6:  6 rows per hour = 10 Minutes</li>
     * <li>12: 12 rows per hour =  5 Minutes</li>
     * </ul>
     * Default is 2.
     */
    public void setRowsPerHour(int rows) {
        m_rowsPerHour = rows;
    }

    public int getRowsPerHour() {
        return m_rowsPerHour;
    }

    public void setWorktime(int startHour, int endHour) {
        this.startHour = startHour;
        this.endHour = endHour;
    }

    public void setShowNonEmptyExcludedDays( boolean showNonEmptyExcluded) {
        this.showNonEmptyExcluded = showNonEmptyExcluded;
    }

    public void setToDate(Date weekDate) {
        calcMinMaxDates( weekDate );
    }

    public Collection<Block> getBlocks() {
        return blocks;
    }

    /** must be called after the slots are filled*/
    protected boolean isEmpty( int column) 
    {
        return multSlots[column].isEmpty();
    }
    
	protected int getColumnCount() 
	{
		return getDaysInView();
	}

    public void rebuild() {
        int columns = getColumnCount();
        blocks.clear();
        multSlots = new HTMLDaySlot[columns];
        
        String[] headerNames = new String[columns];
        
        for (int i=0;i<columns;i++) {
        	String headerName = createColumnHeader(i);
			headerNames[i] = headerName;
         }

        // calculate the blocks
        int start = startHour;
        int end = endHour;
        Iterator<Builder> it= builders.iterator();
        while (it.hasNext()) {
           Builder b= it.next();
           b.prepareBuild(getStartDate(),getEndDate());
           start = Math.min(b.getMin(),start);
           end = Math.max(b.getMax(),end);
           if (start<0)
             throw new IllegalStateException("builder.getMin() is smaller than 0");
           if (end>24)
             throw new IllegalStateException("builder.getMax() is greater than 24");
        }
        minRow = start * m_rowsPerHour;
        maxRow = end *  m_rowsPerHour;
        for (int i=0;i<multSlots.length;i++) {
            multSlots[i] = new HTMLDaySlot(minRow,2);
        }

        it= builders.iterator();
        while (it.hasNext()) {
           Builder b= (Builder)it.next();
           if (b.isEnabled()) { b.build(this); }
        }

        StringBuffer result = new StringBuffer();
        result.append("<table class=\"week_table\">\n");
        result.append("<tr>\n");
        result.append("<td class=\"week_number\">"+getWeekNumberRow()+"</td>");
        for (int i=0;i<multSlots.length;i++) {
            if ( isExcluded ( i ) )
                continue;
            result.append("<td class=\"week_header\" colspan=\""+ (Math.max(1,multSlots[i].size()) * 2 + 1) + "\">");
            result.append("<nobr>");
            result.append(headerNames[i]);
            result.append("</nobr>");
            result.append("</td>");
        }
        result.append("\n</tr>");
        result.append("<tr></tr>");
        boolean useAM_PM = org.rapla.components.calendarview.swing.TimeScale.isAmPmFormat( locale );
        //System.out.println("USING AM_PM" + useAM_PM);
        for (int row =  minRow;row<maxRow;row++) {
            String timeString = formatTime((row * 60) / m_rowsPerHour, useAM_PM);
            //System.out.println("Start row " + row / m_rowsPerHour  + ":" + row % m_rowsPerHour +" " + timeString );

            result.append("<tr>\n");
            if ( row % m_rowsPerHour == 0) {
                result.append("<td class=\"week_times\" rowspan=\""+  m_rowsPerHour +"\"><nobr>");
                result.append(timeString);
                result.append("</nobr>");
                result.append(" &#160;</td>\n");
            }
            
            
            
            for (int day=0;day<columns;day++) {
				if (isExcluded(day))
					continue;
				if (multSlots[day].size() == 0)
				{
					// Rapla 1.4: Make line for full hours darker than others
					if ((row - minRow) % m_rowsPerHour == 0)
					{
						result.append("<td class=\"week_smallseparatorcell_black\">&nbsp;</td>");
						result.append("<td class=\"week_emptycell_black\">&nbsp;</td>\n");
					}
					else
					{
						result.append("<td class=\"week_smallseparatorcell\">&nbsp;</td>");
						result.append("<td class=\"week_emptycell\">&nbsp;</td>\n");
					}
				}
				for (int slotnr = 0; slotnr < multSlots[day].size(); slotnr++)
				{
					// Rapla 1.4: Make line for full hours darker than others
					if ((row - minRow) % m_rowsPerHour == 0)
					{
						result.append("<td class=\"week_smallseparatorcell_black\">&nbsp;</td>");
					}
					else
					{
						result.append("<td class=\"week_smallseparatorcell\">&nbsp;</td>");
					}
					
					Slot slot = multSlots[day].getSlotAt(slotnr);
					if (slot.isSkip(row))
					{
						// Do nothing
					}
					else if (slot.isEmpty(row))
					{
						// Rapla 1.4: Make line for full hours darker than others
						if ((row - minRow) % m_rowsPerHour == 0 || (!slot.isEmpty(row-1) && (row-minRow) > 0))
						{
							result.append("<td class=\"week_emptycell_black\">&nbsp;</td>\n");
						}
						else
						{
							result.append("<td class=\"week_emptycell\">&nbsp;</td>\n");
						}
					}
					else
					{
						Block block = slot.getBlock(row);
						int rowspan = slot.getBlockSize(row) - Math.max(minRow - slot.getBlockStart(row), 0);
						result.append("<td valign=\"top\" class=\"week_block\"");
						result.append(" rowspan=\"" + rowspan + "\"");
						if (block instanceof HTMLBlock)
							result.append(" bgcolor=\"" + ((HTMLBlock) block).getBackgroundColor() + "\"");
						result.append(">");
						result.append(block.toString());
						result.append("</td>\n");
					}
				}
				
				// Rapla 1.4: Make line for full hours darker than others
				if ((row - minRow) % m_rowsPerHour == 0)
				{
					result.append("<td class=\"week_separatorcell_black\">&nbsp;</td>");
				}
				else
				{
					result.append("<td class=\"week_separatorcell\">&nbsp;</td>");
				}
			}
			
			result.append("\n</tr>\n");
        }
        result.append("</table>\n");
        m_html = result.toString();
    }

	protected String createColumnHeader(int i) {
		blockCalendar.setTime(getStartDate());
		blockCalendar.add(Calendar.DATE, i);
		String headerName = SwingWeekView.formatDayOfWeekDateMonth
		    (blockCalendar.getTime()
		     ,locale
		     ,timeZone
		     );
		return headerName;
	}

    protected String getWeekNumberRow() {
        return "";  //To change body of created methods use File | Settings | File Templates.
    }
    
    public void addBlock(Block block,int column,int slot) {
        checkBlock ( block );
        HTMLDaySlot multiSlot =multSlots[column];
        blockCalendar.setTime( block.getStart());
        int row = (int) (
            blockCalendar.get(Calendar.HOUR_OF_DAY)* m_rowsPerHour
            + Math.round((blockCalendar.get(Calendar.MINUTE) * m_rowsPerHour)/60.0)
            );
        blockCalendar.setTime(block.getEnd());
        row  = Math.max( minRow, row );
        int endRow = (int) (
            blockCalendar.get(Calendar.HOUR_OF_DAY)* m_rowsPerHour
            + Math.round((blockCalendar.get(Calendar.MINUTE) * m_rowsPerHour)/60.0)
            );
        endRow  = Math.min( maxRow, endRow );
        int rowCount = endRow -row;
        blocks.add(block);
        blockStart.add(new Integer(row));
        blockSize.add(new Integer( rowCount));
        multiSlot.putBlock( blocks.size() - 1, slot, row, rowCount);
    }

    private String formatTime(int minuteOfDay,boolean useAM_PM) {
        blockCalendar.set(Calendar.MINUTE, minuteOfDay%60);
        int hour = minuteOfDay/60;
        blockCalendar.set(Calendar.HOUR_OF_DAY, hour);
        SimpleDateFormat format = new SimpleDateFormat(useAM_PM ? "h:mm" : "H:mm", locale);
        format.setTimeZone(blockCalendar.getTimeZone());
        if (useAM_PM && hour == 12 && minuteOfDay%60 == 0) {
            return format.format(blockCalendar.getTime()) + " PM";
        } else {
            return format.format(blockCalendar.getTime());
        }
    }
   
    protected class HTMLDaySlot extends ArrayList<Slot> {
        private static final long serialVersionUID = 1L;

        int minSlotRow = 0;
        private boolean empty = true;

        public HTMLDaySlot(int minRow,int size) {
            super(size);
            this.minSlotRow = minRow;
        }

        public void putBlock(int blockNr,int slotnr, int row,int size) {
            while (slotnr >= size()) {
                addSlot();
            }
            getSlotAt(slotnr).putBlock( blockNr, row, size);
            empty = false;
        }

        public int addSlot() {
            Slot slot = new Slot(minSlotRow);
            add(slot);
            return size();
        }
        public Slot getSlotAt(int index) {
            return (Slot) get(index);
        }

        public boolean isEmpty() {
            return empty;
        }
    }

    protected class Slot {
        int EMPTY = -2;
        int SKIP = -1;
        int[] rows = new int[24 * m_rowsPerHour];
        int minSlotRow = 0;

        public Slot(int minRow) {
            this.minSlotRow = minRow;
            for (int i = 0;i < rows.length;i++) {
                rows[i] = EMPTY;
            }
        }

        public void putBlock(int blockNr, int row, int size) {
            int start = Math.max(minSlotRow , row );
            rows[start] = blockNr;
            for (int i = start + 1;i < row + size;i++) {
                rows[i] = SKIP;
            }
        }

        public boolean isSkip(int row) {
            return rows[row]==SKIP;
        }

        public boolean isEmpty(int row) {
            return rows[row]==EMPTY;
        }

        public Block getBlock(int row) {
            if (rows[row] == SKIP || rows[row] == EMPTY )
                return null;
            else
                return (Block) blocks.get(rows[row]);
        }

        public int getBlockStart(int row) {
           return ((Integer)blockStart.get(rows[row])).intValue();
        }
        public int getBlockSize(int row) {
           return ((Integer)blockSize.get(rows[row])).intValue();
        }

    }
}
