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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.rapla.components.calendarview.Block;
import org.rapla.components.calendarview.BlockComparator;

class SmallDaySlot extends AbstractDaySlot implements DaySlot
{
    private static final long serialVersionUID = 1L;

    public static int BLOCK_HEIGHT = 32 +20;
    public static int START_GAP = 10;
    public static Color THICK_LINE_COLOR = Color.black;
    public static Color LINE_COLOR = new Color(0xaa, 0xaa, 0xaa);
    public static Color WORKTIME_BACKGROUND = Color.white;
    
    private List<Block> blocks = new LinkedList<Block>();
    private int slotxsize;
    private String number;
    private Color dateNumberColor;
    private int rowSize = 15;
    
    private boolean selected;

    int slot_space = 3;

   private BlockListener blockListener = new BlockListener();
   
   public SmallDaySlot(String number,int slotxsize,Color dateNumberColor 
                    ) {
        this.dateNumberColor = dateNumberColor;
        this.slotxsize= slotxsize;
        setLayout( null );
        setAlignmentX(0);
        setAlignmentY(TOP_ALIGNMENT);
        Dimension newSize = new Dimension(slotxsize, 10);
        setPreferredSize(newSize);
        this.setBackground(Color.white);
        this.number = number;
    }

    public boolean isBorderVisible() {
        return true;
    }

    public Collection<Block> getBlocks() {
        return blocks;
    }

    public void select(int startRow, int endRow) {
        boolean selected = (startRow >=0 || endRow>=0);
        if (this.selected == selected)
            return;
        this.selected = selected;
        invalidateSelection();
    }

    public void unselectAll() {
        if (!selected )
            return;
        selected = false;
        invalidateSelection();
    }

    private void invalidateSelection() {
        repaint();
        // Update the values after calling repaint, because paint needs the old values.
    }


    /**
       fuegt einen block im gewuenschten slot ein
       (konflikte werden ignoriert).
    */
    public void putBlock(SwingBlock bl)  {
        add( bl.getView() );
        blocks.add( bl );
        // The blockListener can be shared among all blocks,
        // as long es we can only click on one block simultanously
        bl.getView().addMouseListener( blockListener );
        bl.getView().addMouseMotionListener( blockListener );
        blockViewMapper.put( bl.getView(), bl );
        updateSize();
    }

	public void updateSize() {
		int height = Math.max( 25, blocks.size() * BLOCK_HEIGHT + START_GAP );
        Dimension newSize = new Dimension( slotxsize, height );
        setPreferredSize( newSize );
        updateBounds();
	}

    void sort() {
        Collections.sort( blocks, BlockComparator.COMPARATOR);
        updateBounds();
    }

    private void updateBounds() {
        for (int i=0;i< blocks.size();i++) {
            int y = i * BLOCK_HEIGHT + START_GAP;
            SwingBlock bl = (SwingBlock) blocks.get(i);
            bl.getView().setBounds( 1 ,y, slotxsize -1, BLOCK_HEIGHT -1);
        }
    }

    public Dimension getMinimumSize() { return getPreferredSize(); }
    public Dimension getMaximumSize() { return getPreferredSize(); }
    Insets insets = new Insets(0,0,0, 0);

    public Insets getInsets() {
        return insets;
    }

    int getBlockCount() {
        return blocks.size();
    }

    public boolean isEmpty() {
        return getBlockCount() == 0;
    }

    public int calcRow(int y) {
        return 0;
    }

    public int calcSlot(int x) {
        return 0;
    }

    public int getX(Component component) {
        return component.getLocation().x;
    }

    int max;
    public void paint(Graphics g)  {
         Dimension dim = getSize();
         Rectangle rect = g.getClipBounds();
         g.setColor(Color.white);

         if ( selected ) {
             g.setColor(getSelectionColor());
         } else {
             g.setColor(getBackground());
         }

         g.fillRect(rect.x
                 , rect.y
                 , rect.x + rect.width
                 , rect.y + rect.height);

         if (isBorderVisible()) {
             g.setColor(LINE_COLOR);
             g.drawLine(0,rect.y,0,rect.y + rect.height);
             g.drawLine(dim.width - 1,rect.y,dim.width - 1 ,rect.y + rect.height);
             g.drawLine(rect.x,0, rect.x + rect.width,0);
             g.drawLine(rect.x, dim.height -1, rect.x + rect.width, dim.height -1);
         }

         g.setColor( dateNumberColor );
         int numberWidth = g.getFontMetrics().stringWidth( number );
         g.drawString( number , dim.width - numberWidth - 5, Math.max( START_GAP, 10 ) );
         
         super.paintChildren(g);
         if ( paintDraggingGrid ) {
             int height = draggingView.getView().getHeight() - 1;
             int x = 0;
             int y = draggingView.getView().getBounds().y;
             if ( y + height  + 2> getHeight()) {
                 y = getHeight() - height -2;
             }
             paintDraggingGrid(g, x, y, slotxsize -1, height );
         }
    }

    public int getRowSize()
    {
        return rowSize;
    }

    public boolean isSelected()
    {
    	return selected ;
    }

	public void updateSize(int newWidth) 
	{
		this.slotxsize = newWidth;
		updateSize();
	}
  


}



