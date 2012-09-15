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
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.rapla.components.calendarview.Block;


class PeriodDaySlot extends AbstractDaySlot implements DaySlot
{
   private static final long serialVersionUID = 1L;

   private int rowsPerHour = 4;

   public static Color THICK_LINE_COLOR = Color.black;
   public static Color LINE_COLOR = new Color(0xaa, 0xaa, 0xaa);
   public static Color WORKTIME_BACKGROUND = Color.white;
   public static Color NON_WORKTIME_BACKGROUND = new Color(0xcc, 0xcc, 0xcc);

   private Vector<Slot> slots= new Vector<Slot>();
   private int mintime, maxtime;
   private int slotxsize;
   private TimeZone timeZone;

   private int selectionStart = -1;
   private int selectionEnd = -1;
   private int oldSelectionStart = -1;
   private int oldSelectionEnd = -1;

   BoxLayout boxLayout1 = new BoxLayout(this, BoxLayout.X_AXIS);
   int right_gap = 5;
   int left_gap = 3;
   int slot_space = 3;
   int rowSize;
   
   private BlockListener blockListener = new BlockListener();
   /**
      es muss auch noch setTimeIntervall() aufgerufen werden, um die initialisierung
      fertig zu stellen (wie beim konstruktor von Slot).
      slotxsize ist die breite der einzelnen slots.
      "date" legt das Datum fest, fuer welches das Slot anzeigt (Uhrzeit bleibt unberuecksichtigt)
   */
   public PeriodDaySlot(
                    TimeZone timeZone
                    ,int slotxsize
                    ,int rowsPerHour
                    ,int rowSize
                    ) {
       this.timeZone = timeZone;
       this.slotxsize= slotxsize;
       this.rowsPerHour = rowsPerHour;
       this.rowSize = rowSize;
       setLayout(boxLayout1);
       this.add(Box.createHorizontalStrut(left_gap));
       addSlot();
       setBackground(getBackground());
       setAlignmentX(0);
       setAlignmentY(TOP_ALIGNMENT);
       this.setBackground(Color.white);
   }

   private Calendar calendar = null;
   private Calendar getCalendar() {
       // Lazy creation of the calendar
       if (calendar == null)
           calendar = Calendar.getInstance(timeZone);
       return calendar;
   }

   public boolean isBorderVisible() {
       return true;
//     return getBackground() != Color.white;
   }

   // override
   public int calcRow(int y) {
       y -= 3 ; // Excoffier: Why ?
       if ( y < 0 )
           y -= rowSize - 1;
       int row = y / rowSize;
       return row;
   }
   
   public int getSlotCount()
   {
   	return slots.size();
   }

   // override
   public int calcHour(int index) {
       index += 7 * this.rowsPerHour * (maxtime - mintime) ; // Goto positives
       index = index % (this.rowsPerHour * (maxtime - mintime)) ;
       return index / rowsPerHour;
   }

   // override
   public int calcMinute(int index) {
       int minutesPerRow =  60 / rowsPerHour;
       index += rowsPerHour * 24 * 7 ; // So it is positive
       return (index % rowsPerHour) * (minutesPerRow);
   }

   // override
   private int addSlot() {
       Slot slot= new Slot();
       slot.setTimeIntervall(mintime,maxtime);
       slots.addElement(slot);
       this.add(slot);
       this.add(Box.createHorizontalStrut(slot_space));
       return slots.size()-1;
   }

   // XXX Thierry Excoffier, this draggingY correction should be
   // implemented in AbstractDaySlot?
   // It is badly implemented here.
   public int dragY() {
    int h = this.rowsPerHour * ((maxtime - mintime) * rowSize) ;
    return (draggingY + 7*h) % h;
   }

   public int calcSlot(int x) {
       int slot = ((x - left_gap) / (slotxsize + slot_space));
       //System.out.println ( x + "  slot " + slot);
       if (slot<0)
           slot = 0;
       if (slot >= slots.size())
           slot = slots.size() -1;
       return slot;
   }

   public Collection<Block> getBlocks() {
       ArrayList<Block> list = new ArrayList<Block>();
       for (int i=0;i<slots.size();i++)
           list.addAll(slots.get(i).getBlocks());
       return list;
   }


   public void select(int start,int end) {
       if (start == selectionStart && end == selectionEnd)
           return;
       selectionStart = start;
       selectionEnd = end;
       invalidateSelection();
   }

   public void unselectAll() {
       if (selectionStart == -1 || selectionEnd == -1)
           return;
       selectionStart = -1;
       selectionEnd = -1;
       invalidateSelection();
   }

   /**
      min und max sind uhrzeiten in vollen stunden.
      siehe auch beschreibung von Slot.setTimeIntervall().
   */
   public void setTimeIntervall(int min, int max) {
       mintime= min;
       maxtime= max;
       Iterator<Slot> it= slots.iterator();
       while (it.hasNext()) {
           Slot slot = (Slot)it.next();
           slot.setTimeIntervall(mintime,maxtime);
       }
   }


   /**
      fuegt einen block im gewuenschten slot ein
      (konflikte werden ignoriert).
   */
   public void putBlock(SwingBlock bl, int slotnr)  {
       while (slotnr >= slots.size()) {
           addSlot();
       }
       ((Slot)slots.elementAt(slotnr)).putBlock(bl);
       // The blockListener can be shared among all blocks,
       // as long es we can only click on one block simultanously
       bl.getView().addMouseListener(blockListener);
       bl.getView().addMouseMotionListener(blockListener);
       blockViewMapper.put(bl.getView(),bl);
   }

   public Dimension getMinimumSize() { return getPreferredSize(); }
   public Dimension getMaximumSize() { return getPreferredSize(); }
   Insets insets = new Insets(0,0,0, right_gap);
   Insets slotInsets = new Insets(0,0,0,0);

   public Insets getInsets() {
       return insets;
   }

   SwingBlock getBlockFor(Object component) {
       return (SwingBlock) blockViewMapper.get(component);
   }

   int getBlockCount() {
       int count = 0;
       Iterator<Slot> it = slots.iterator();
       while (it.hasNext())
           count += ((Slot)it.next()).getBlockCount();
       return count;
   }

   boolean isEmpty() {
       return getBlockCount() == 0;
   }

   public boolean isSelected() {
       return selectionStart >= 0;
   }
   protected void invalidateDragging(Point oldPoint) {
       int width = (int) getSize().width;
       int start = Math.min(calcRow(dragY()),calcRow(oldPoint.y ));
       int end = Math.max(calcRow(dragY()),calcRow(oldPoint.y )) + 1;
       repaint(0
               , start * rowSize -10
               , width
               , (end - start) * rowSize + draggingHeight + 20
               );
   }

   private void invalidateSelection() {
       int width = (int) getSize().width;
       int start = Math.min(selectionStart,oldSelectionStart);
       int end = Math.max(selectionEnd,oldSelectionEnd) + 1;
       repaint(0,start * rowSize, width, (end  - start) * rowSize);
       // Update the values after calling repaint, because paint needs the old values.
       oldSelectionStart = selectionStart;
       oldSelectionEnd = selectionEnd;
   }

   int max;
   public void paint(Graphics g)  {
       Dimension dim = getSize();
       Rectangle rect = g.getClipBounds();

        g.setColor(Color.white);
        g.fillRect(rect.x
               ,rect.y
               ,rect.width
               ,rect.height);

       if (isBorderVisible()) {
           g.setColor(LINE_COLOR);
           g.drawLine(0,rect.y,0,rect.y + rect.height);
           g.drawLine(dim.width - 1,rect.y,dim.width - 1 ,rect.y + rect.height);
       }

       max = rowsPerHour * (maxtime - mintime);

        // System.out.println (g + " start=" + selectionStart + " end=" + selectionEnd);


       for (int i=0; i <= max ; i++) {
           int y = rowSize * i;
           if ((y + rowSize) >= rect.y && y < (rect.y + rect.height)) {
               if (i>= selectionStart && i<=selectionEnd) {
                   g.setColor(getSelectionColor());
                   g.fillRect(Math.max (rect.x,1)
                              , y
                              , Math.min (rect.width,dim.width - Math.max (rect.x,1) - 1)
                              , rowSize);
               }
               boolean bFullHour = (i % rowsPerHour == 0);
               g.setColor((bFullHour) ? THICK_LINE_COLOR : LINE_COLOR);
               if (isEditable() || (bFullHour && (i<max || isBorderVisible())))
                   g.drawLine(rect.x,y ,rect.x + rect.width , y);
           }
       }

       super.paintChildren(g);
       if ( paintDraggingGrid ) {
           int x = draggingSlot * (slotxsize + slot_space) + left_gap;
           paintDraggingGrid(g, x , dragY(), slotxsize -1,  draggingHeight);
       }
   }



   /** grafische komponente, in die implementierungen von Block (genauer deren zugehoerige BlockViews) eingefuegt werden koennen.
    * entsprechend start- und end-zeit des blocks wird der view
    * im slot dargestellt. die views werden dabei vertikal angeordnet.
    */
   class Slot extends JPanel
   {
       private static final long serialVersionUID = 1L;

       final private static int MINUTES_PER_HOUR= 60;
       private ArrayList<Block> blocks= new ArrayList<Block>();
       private int miny;
       private int maxy;

       public Slot()  {
           setLayout(null);
           setBackground(Color.white);
           setOpaque(false);
       }

       /** legt fest, fuer welches zeitintervall das slot gelten soll.
        (Beispiel 8:00 - 13:00)
        min und max sind uhrzeiten in vollen stunden.
        zur initialisierung der komponente muss diese methode mindestens einmal
        aufgerufen werden.
        */
       public void setTimeIntervall(int min, int max)  {
           this.miny = min;
           this.maxy = max;
           int pixelPerHour= rowSize * rowsPerHour;
           int ysize= ( max - min )* pixelPerHour + 1;
           setSize(slotxsize, ysize);
           setPreferredSize(new Dimension(slotxsize, ysize ) );
       }

       /**
        start des zeitintervalls (nur uhrzeit von Date ist relevant).
        */
       public int getMin()  {
           return miny * MINUTES_PER_HOUR ;
       }

       public int getMax()  {
           return maxy * MINUTES_PER_HOUR;
       }

       private int getMinuteOfDay(Date time) {
           Calendar cal = getCalendar();
           cal.setTime(time);
           return (cal.get(Calendar.HOUR_OF_DAY )) * MINUTES_PER_HOUR + cal.get(Calendar.MINUTE);
       }

       private int getYCoord(Date time)  {
           int diff = getMinuteOfDay(time) - getMin();
           int pixelPerHour= rowSize * rowsPerHour;
           return (diff * pixelPerHour) / MINUTES_PER_HOUR;
       }

       /** fuegt b in den Slot ein.  */
       public void putBlock(SwingBlock b)  {
           //update bounds
           int y1= getYCoord(b.getStart());
           int y2= getYCoord(b.getEnd());
           int x = 0;

           // search conflicts
           Iterator<Block> it= blocks.iterator();
           while (it.hasNext()) {
                SwingBlock block = (SwingBlock)it.next();
                if ( block.getView().getY() <= y1
                     && block.getView().getY() + block.getView().getHeight() >= y2)
                {
                    x += 4;
                    y1 -= 4;
                    y2 -= 4;
                }
           }

           blocks.add(b);
           if ( y1 <  0)
               y1 = 0;
           if ( y2 > getMaximumSize().height)
               y2 = getMaximumSize().height;
           b.getView().setBounds(x, y1, slotxsize - right_gap - left_gap, y2 - y1 + 1);
           add(b.getView());
       }

       public int getBlockCount() {
           return blocks.size();
       }

       public Dimension getMinimumSize() { return getPreferredSize(); }
       public Dimension getMaximumSize() { return getPreferredSize(); }

       public Insets getInsets() {
           return slotInsets;
       }

       public List<Block> getBlocks() {
           return blocks;
       }
   }



   void updateSize(int slotsize)
   {
	   	this.slotxsize = slotsize;
//	   	for ( Slot slot:slots)
//	   	{
//	   		slot.setTimeIntervall();
//	   		for (Block block:slot.getBlocks())
//	       	{
//	       		slot.updateBounds((SwingBlock)block);
//	       	}
//	   	}
	   	//updateHeaderSize();
   }
}