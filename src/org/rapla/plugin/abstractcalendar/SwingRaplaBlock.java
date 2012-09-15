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

package org.rapla.plugin.abstractcalendar;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.TexturePaint;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.event.MouseInputListener;

import org.rapla.components.calendarview.Block;
import org.rapla.components.calendarview.swing.SwingBlock;
import org.rapla.components.util.SmallIntMap;
import org.rapla.entities.Named;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Repeating;
import org.rapla.framework.RaplaContextException;
import org.rapla.gui.InfoFactory;
import org.rapla.gui.toolkit.RaplaColorList;

public class SwingRaplaBlock extends AbstractRaplaBlock implements SwingBlock
{
    private static BufferedImage exceptionImage;
    RaplaBlockView m_view = new RaplaBlockView();

    private BufferedImage getExceptionImage()
    {
        if ( exceptionImage != null )
            return exceptionImage;

        Image image = getContext().getBuildContext().getExceptionBackgroundIcon().getImage();
        MediaTracker m = new MediaTracker( m_view );
        m.addImage( image, 0 );
        try
        {
            m.waitForID( 0 );
        }
        catch ( InterruptedException ex )
        {
        }

        exceptionImage = new BufferedImage( image.getWidth( null ), image.getHeight( null ),
                                            BufferedImage.TYPE_INT_ARGB );
        Graphics g = exceptionImage.getGraphics();
        g.drawImage( image, 0, 0, null );
        return exceptionImage;
    }

    public Component getView()
    {
        return m_view;
    }

    public boolean isException()
    {
        Repeating repeating = getAppointment().getRepeating();
        return repeating != null && repeating.isException( getStart().getTime() );
    }

    static Color TRANS = new Color( 100, 100, 100, 100 );

    public void paintDragging( Graphics g, int width, int height )
    {
        g.setColor( TRANS );
        m_view.paint( g, width, height );
        g.setColor( LINECOLOR_ACTIVE );
        g.drawRoundRect( 0, 0, width, height, 5, 5 );
    }

    static Font FONT_TITLE = new Font( "SansSerif", Font.BOLD, 12 );
    static Font FONT_SMALL_TITLE = new Font( "SansSerif", Font.BOLD, 10 );
    static Font FONT_INVISIBLE = new Font( "SansSerif", Font.PLAIN, 10 );
    static Font FONT_RESOURCE = new Font( "SansSerif", Font.PLAIN, 12 );
    static Font FONT_PERSON = new Font( "SansSerif", Font.ITALIC, 12 );
    static String FOREGROUND_COLOR = RaplaColorList.getHexForColor( Color.black );

    static SmallIntMap alphaMap = new SmallIntMap();

    private static Color LINECOLOR_INACTIVE = Color.darkGray;
    private static Color LINECOLOR_ACTIVE = new Color( 255, 90, 10 );
    private static Color LINECOLOR_SAME_RESERVATION = new Color( 180, 20, 120 );

    // The Linecolor is not static because it can be changed depending on the mouse move
    private Color linecolor = LINECOLOR_INACTIVE;

    class RaplaBlockView extends JComponent implements MouseInputListener
    {
        private static final long serialVersionUID = 1L;

        RaplaBlockView()
        {
            javax.swing.ToolTipManager.sharedInstance().registerComponent( this );
            addMouseListener( this );
        }

        public String getName( Named named )
        {
            return SwingRaplaBlock.this.getName( named );
        }

        public String getToolTipText( MouseEvent evt )
        {
            String text = "";
            if ( getContext().isAnonymous() )
            {
                text = getI18n().getString( "not_visible.help" );
            }
            else if ( !getContext().isEventSelected() && !getBuildContext().isConflictSelected() )
            {
                text = getI18n().getString( "not_selected.help" );
            }
            else
            {
                try
                {
                    InfoFactory infoFactory = (InfoFactory) getBuildContext().getServiceManager()
                                                                             .lookup( InfoFactory.ROLE );
                    text = infoFactory.getToolTip( getAppointment(), false );
                }
                catch ( RaplaContextException ex )
                {
                }
            }
            return "<html>" + text + "</html>";
        }

        private Color adjustColor( String org, int alpha )
        {
            @SuppressWarnings("unchecked")
			Map<String,Color> colorMap = (Map<String,Color>) alphaMap.get( alpha );
            if ( colorMap == null )
            {
                colorMap = new HashMap<String,Color>();
                alphaMap.put( alpha, colorMap );
            }
            Color color = colorMap.get( org );
            if ( color == null )
            {
                Color or;
                try
                {
                    or = RaplaColorList.getColorForHex( org );
                }
                catch ( NumberFormatException nf )
                {
                    or = RaplaColorList.getColorForHex( "#FFFFFF" );
                }
                color = new Color( or.getRed(), or.getGreen(), or.getBlue(), alpha );
                colorMap.put( org, color );
            }

            return color;
        }

        public void paint( Graphics g )
        {
            Dimension dim = getSize();
            paint( g, dim.width, dim.height );
        }

        public void paint( Graphics g, int width, int height )
        {
            int alpha = g.getColor().getAlpha();

            if ( !getContext().isEventSelected() )
            {
                alpha = 80;
                paintBackground( g, width, height, alpha );
            }
            else
            {
                paintBackground( g, width, height, alpha );
            }

            //boolean isException = getAppointment().getRepeating().isException(getStart().getTime());
            Color fg = adjustColor( FOREGROUND_COLOR, alpha ); //(isException() ? Color.white : Color.black);
            g.setColor( fg );

            if ( getAppointment().getRepeating() != null && getBuildContext().isRepeatingVisible() )
            {
                if ( !getContext().isAnonymous() && getContext().isEventSelected() && !isException() )
                {
                    getBuildContext().getRepeatingIcon().paintIcon( this, g, width - 17, 0 );
                }
                /*
                 if ( getBuildContext().isTimeVisible() )
                 g.clipRect(0,0, width -17, height);
                 */
            }
            // y will store the y-position of the carret
            int y = -2;
            // Draw the Reservationname
            boolean small = (height < 30);
           
            String timeString = getTimeString(small);
            StringBuffer buf = new StringBuffer();
            if ( timeString != null )
            {
                if ( !small)
                {
                    g.setFont( FONT_SMALL_TITLE );
                    List<Allocatable> resources = getContext().getAllocatables();
                    StringBuffer buffer  = new StringBuffer() ;
                    for (Allocatable resource: resources)
                    {
                    	if ( getContext().isVisible( resource) && !resource.isPerson())
                    	{
                    		if ( buffer.length() > 0)
                    		{
                    			buffer.append(", ");
                    		}
                    		buffer.append( getName(resource));
                    	}
                    }
                    if (  !getBuildContext().isResourceVisible() && buffer.length() > 0)
                    {
                    	timeString = timeString + " " + buffer.toString();
                    }
                    y = drawString( g, timeString, y, 2, false ) - 1;
                }
                else
                {
                    buf.append( timeString );
                    buf.append( " " );
                }
            }
            if ( !small)
            {
                g.setFont( FONT_TITLE );
            }
            else
            {
                g.setFont( FONT_SMALL_TITLE );
            }

            if ( getContext().isAnonymous() )
            {
                y += 4;
                g.setFont( FONT_INVISIBLE );
                String label = getI18n().getString( "not_visible" );
                buf.append(label);
                y = drawString( g, buf.toString(), y, 5, true ) + 2;
                return;
            }

            String label = getName( getReservation() );
            buf.append(label);
            y = drawString( g, buf.toString(), y, 2, true ) + 2;

            // If the y reaches the lowerBound "..." will be displayed
            double lowerBound = height - 11;

            if ( getBuildContext().isPersonVisible() )
            {
                g.setFont( FONT_PERSON );
                g.setColor( fg );
                List<Allocatable> persons = getContext().getAllocatables();
                for ( Allocatable person:persons)
                {
                  String text = getName( person);
                  if ( !getContext().isVisible( person) || !person.isPerson())
                	  continue;
                    if ( y > lowerBound )
                    {
                        text = "...";
                        y -= 7;
                    }
                    y = drawString( g, text, y, 7, true );
                }
            }
            else
            {
            	   g.setFont( FONT_PERSON );
                   g.setColor( fg );
                   buf = new StringBuffer();
                   List<Allocatable> persons = getContext().getAllocatables();
                   for ( Allocatable person:persons)
                   {
                	   if ( !getContext().isVisible( person) || !person.isPerson())
                     	  continue;
                     
                	   if ( buf.length() > 0)
                       {
                    	   buf.append(", ");
                       }
                       buf.append( getName( person ));
                   }
                   String text = buf.toString();
                   y = drawString( g, text, y, 7, true );
               
            }

            if ( getBuildContext().isResourceVisible() )
            {
            	List<Allocatable> resources = getContext().getAllocatables();
                g.setFont( FONT_RESOURCE );
                g.setColor( fg );
                for ( Allocatable resource:resources)
                {
                	 if ( !getContext().isVisible( resource) || resource.isPerson())
                    	  continue;
                	String text = getName( resource );
                    if ( y > lowerBound )
                    {
                        text = "...";
                        y -= 7;
                    }
                    y = drawString( g, text, y, 7, true );
                }
            }
        }

        private void setExceptionPaint( Graphics g )
        {
            Paint p = new TexturePaint( getExceptionImage(), new Rectangle( 14, 14 ) );
            ( (Graphics2D) g ).setPaint( p );
        }

        private void paintBackground( Graphics g, int width, int height, int alpha )
        {
            String[] colors = getColorsAsHex();
            double colWidth = (double) ( width - 2 ) / colors.length;
            int x = 0;
            for ( int i = 0; i < colors.length; i++ )
            {
                g.setColor( adjustColor( colors[i], alpha ) );
                g.fillRect( (int) Math.ceil( x ) + 1, 1, (int) Math.ceil( colWidth ), height - 2 );
                if ( isException() )
                {
                    setExceptionPaint( g );
                    g.fillRect( (int) Math.ceil( x ) + 1, 1, (int) Math.ceil( colWidth ), height - 2 );
                }
                x += colWidth;
            }

            //g.setColor( adjustColor( "#000000", alpha ) );
            g.setColor( linecolor );
            g.drawRoundRect( 0, 0, width - 1, height - 1, 5, 5 );
        }

        private int findBreakingSpace( char[] c, int offset, int len, int maxWidth, FontMetrics fm )
        {
            int index = -1;
            for ( int i = offset; i < offset + len; i++ )
            {
                if ( c[i] == ' ' && fm.charsWidth( c, offset, i - offset ) < maxWidth )
                    index = i;
            }
            return index;
        }

        /*
        private int findBreaking( char[] c, int offset, int len, int maxWidth, FontMetrics fm )
        {
            int index = 0;
            for ( int i = offset; i < offset + len; i++ )
            {
                if ( fm.charsWidth( c, offset, i - offset ) < maxWidth )
                    index = i;
            }
            return index - 1;
        }
*/

        // @return the new y-coordiante below the text
        private int drawString( Graphics g, String text, int y, int indent, boolean breakLines )
        {
            FontMetrics fm = g.getFontMetrics();
            //g.setFont(new Font("SimSun",Font.PLAIN, 12));
            char[] c = text.toCharArray();
            int cWidth = getSize().width - indent;
            int height = fm.getHeight();

            int len = c.length;
            int offset = 0;
            int x = indent;
            int maxWidth = ( y >= 14 || getAppointment().getRepeating() == null || !getBuildContext()
                                                                                                     .isRepeatingVisible() )
                    ? cWidth
                    : cWidth - 12;
            if ( !breakLines )
            {
                maxWidth = maxWidth - 5;
            }
            else
            {
                while ( offset < c.length && fm.charsWidth( c, offset, len ) > maxWidth )
                {
                    int breakingSpace = findBreakingSpace( c, offset, len, maxWidth, fm );
                    //int x = bCenter ? (getSize().width - width)/2 : indent ;
                    
                    if ( breakingSpace >= offset && breakLines )
                    {
                        y = y + height;
                    	g.drawChars(c,offset,breakingSpace-offset,x,y);
                        //              System.out.println("Drawing " + new String(c,offset,breakingSpace-offset));
                        len -= breakingSpace - offset + 1;
                        offset = breakingSpace + 1;
                    }
                    else
                    {
                       break;
                    }
                    //      System.out.println("New len " + len + " new offset " + offset);
                    maxWidth = cWidth;
                }
            }
            y = y + height;
            
            g.drawChars( c, offset, len, x, y );
            //      System.out.println("Drawing rest " + new String(c,offset,len));
            return y;
        }

        public void mouseClicked( MouseEvent arg0 )
        {

        }

        public void mousePressed( MouseEvent arg0 )
        {}

        public void mouseReleased( MouseEvent evt )
        {
            Point mp = evt.getPoint();
            boolean inside = mp.x >= 0 && mp.y >= 0 && mp.x <= getWidth() && mp.y <= getHeight();
            changeLineBorder( inside && getContext().isEventSelected() );
        }

        public void mouseEntered( MouseEvent evt )
        {
            if ( ( ( evt.getModifiers() & MouseEvent.BUTTON1_MASK ) > 0 ) || !getContext().isEventSelected() )
            {
                return;
            }
            changeLineBorder( true );
        }

        public void mouseExited( MouseEvent evt )
        {
            if ( ( evt.getModifiers() & MouseEvent.BUTTON1_MASK ) > 0 )
            {
                return;
            }
            changeLineBorder( false );
        }

        public void mouseDragged( MouseEvent arg0 )
        {}

        public void mouseMoved( MouseEvent arg0 )
        {
        // TODO Auto-generated method stub

        }

        private void changeLineBorder( boolean active )
        {
            List<Block> blocks = getBuildContext().getBlocks();
            for ( Iterator<Block> it = blocks.iterator(); it.hasNext(); )
            {
                SwingRaplaBlock block = (SwingRaplaBlock) it.next();

                if ( block.getAppointment().equals( getAppointment() ) )
                {
                    block.linecolor = active ? LINECOLOR_ACTIVE : LINECOLOR_INACTIVE;
                    block.m_view.repaint();
                }
                else if ( block.getReservation().equals( getReservation() ) )
                {
                    block.linecolor = active ? LINECOLOR_SAME_RESERVATION : LINECOLOR_INACTIVE;
                    block.m_view.repaint();
                }
            }
        }

    }
    
    public String toString()
    {
    	return getName() + " " + getStart() + " - " + getEnd();
    }

}
