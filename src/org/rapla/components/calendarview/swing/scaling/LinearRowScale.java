package org.rapla.components.calendarview.swing.scaling;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;



public class LinearRowScale implements IRowScale
{
    private int rowSize = 15;
    private int rowsPerHour = 4;
    final private static int MINUTES_PER_HOUR= 60;
    private TimeZone timeZone = TimeZone.getDefault();

    private int mintime;
    private int maxtime;
    private int workstart;
    private int workend;
    
    public LinearRowScale()
    {
    }
    
    public void setTimeZone( TimeZone timeZone)
    {
        this.timeZone = timeZone;
    }
    
    public int getRowsPerDay()
    {
        return rowsPerHour * 24;
    }

    public void setRowSize( int rowSize )
    {
        this.rowSize = rowSize;
    }

    public int getRowSize()
    {
        return rowSize;
    }

    public int getRowsPerHour()
    {
        return rowsPerHour;
    }

    public void setRowsPerHour( int rowsPerHour )
    {
        this.rowsPerHour = rowsPerHour;
    }
    
    public int calcHour(int index) {
        return index / rowsPerHour;
    }

    public int calcMinute(int index) {
        int minutesPerRow =  60 / rowsPerHour;
        return (index % rowsPerHour) * (minutesPerRow);
    }

    public int getSizeInPixel()
    {
        return rowSize * getMaxRows();
    }
    
    public int getMaxRows()
    {
        int max;
        max = rowsPerHour * (maxtime - mintime) ;
        return max;
    }
    
    private int getMinuteOfDay(Date time) {
       Calendar cal = getCalendar();
       cal.setTime(time);
       return (cal.get(Calendar.HOUR_OF_DAY )) * MINUTES_PER_HOUR + cal.get(Calendar.MINUTE);
   }

   public int getYCoord(Date time)  {
       int diff = getMinuteOfDay(time) - mintime * MINUTES_PER_HOUR ;
       int pixelPerHour= rowSize * rowsPerHour;
       return (diff * pixelPerHour) / MINUTES_PER_HOUR;
   }
   
   public int getStartWorktimePixel()
   {
       int pixelPerHour= rowSize * rowsPerHour;
       int starty = pixelPerHour * workstart;
       return starty;   
   }

   public int getEndWorktimePixel()
   {
       int pixelPerHour= rowSize * rowsPerHour;
       int endy = pixelPerHour * workend;
       return endy;
   }

 
   private Calendar calendar = null;
   private Calendar getCalendar() {
       // Lazy creation of the calendar
       if (calendar == null)
           calendar = Calendar.getInstance(timeZone);
       return calendar;
   }

    public boolean isPaintRowThick( int row )
    {
        return  row % rowsPerHour == 0;
    }

    public void setTimeIntervall( int startHour, int endHour )
    {
        mintime = startHour;
        maxtime = endHour;
    }

    public void setWorktime( int startHour, int endHour )
    {
        workstart = startHour;
        workend = endHour;
    }

    public int getYCoordForRow( int row )
    {
        return row * rowSize;
    }

    public int getSizeInPixelBetween( int startRow, int endRow )
    {
        return (endRow - startRow) * rowSize;
    }

    public int getRowSizeForRow( int row )
    {
        return rowSize;
    }
    
    public int calcRow(int y) {
        int rowsPerDay = getRowsPerDay();
        int row = (y-3) / rowSize;
        return Math.min(Math.max(0, row), rowsPerDay -1);
    }
    
    public int trim(int y )
    {
        return (y  / rowSize) * rowSize;
    }

    public int getDraggingCorrection( int y)
    {
        return rowSize / 2;
    }



}
