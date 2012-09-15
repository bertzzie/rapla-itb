package org.rapla.plugin.appointmentmarker;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.apache.avalon.framework.configuration.Configuration;
import org.rapla.entities.RaplaObject;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.AppointmentBlock;
import org.rapla.entities.domain.Reservation;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaContextException;
import org.rapla.framework.RaplaException;
import org.rapla.gui.MenuContext;
import org.rapla.gui.ObjectMenuFactory;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.toolkit.DialogUI;
import org.rapla.gui.toolkit.RaplaMenuItem;

public class AppointmentMarkerMenuFactory extends RaplaGUIComponent implements ObjectMenuFactory
{

    final AppointmentMarker marker;
    String markerLabelText;
    public AppointmentMarkerMenuFactory( RaplaContext context, Configuration config) throws RaplaException
    {
        super( context );
        this.markerLabelText = config.getChild("marker-label").getValue("mark");
        marker= (AppointmentMarker) context.lookup( AppointmentMarker.ROLE);
    }

    public RaplaMenuItem[] create( final MenuContext menuContext, final RaplaObject focusedObject )
    {
    	if ( focusedObject == null || !focusedObject.getRaplaType().equals(Appointment.TYPE))
        {
            return RaplaMenuItem.EMPTY_ARRAY;
        }
        if (!menuContext.has("selected_date"))
        {
            return RaplaMenuItem.EMPTY_ARRAY;
        }
        Appointment appointment = (Appointment) focusedObject;
        final Reservation event = appointment.getReservation();
        if ( !marker.canBeMarked( event))
        {
            return RaplaMenuItem.EMPTY_ARRAY;
        }
        // Currently only admins can mark dates
        if ( !isAdmin())
        {
            return RaplaMenuItem.EMPTY_ARRAY;
        }
        
        Date selected;
        try
        {
            selected = (Date)menuContext.lookup("selected_date");
        }
        catch (RaplaContextException e1)
        {
            throw new IllegalStateException("selected_date has somehow vanished. This exception should not happen.");
        }
        
        // create the menu entry
        final RaplaMenuItem markerItem = new RaplaMenuItem("MARK_APPOINTMENT");
        final Date currentSelectedDate = selected;
        final boolean isMarked = marker.isMarked( event, selected);
        if ( isMarked)
        {
            markerItem.setIcon( getIcon("icon.checked"));
        } 
        else 
        {
            markerItem.setIcon( getIcon("icon.unchecked"));
        }
        markerItem.setSelectedIcon( getIcon("icon.checked"));
        markerItem.setSelected( isMarked );
        markerItem.setText( markerLabelText );
        // Last the action for the marked menu 
        markerItem.addActionListener( new ActionListener()
        {
            public void actionPerformed( ActionEvent e )
            {
                try 
                {
                    Reservation editableEvent = (Reservation)getClientFacade().edit( event);
                    marker.setMarked( editableEvent, currentSelectedDate, !isMarked);
                    getClientFacade().store( editableEvent ); 
                }
                catch (RaplaException ex )
                {
                    showException( ex, menuContext.getComponent());
                }
            }
         });
        
        final RaplaMenuItem viewMarks = new RaplaMenuItem("MARK_APPOINTMENT");
        viewMarks.setText("view marks");
        viewMarks.setIcon( getIcon("icon.help"));
        viewMarks.addActionListener( new ActionListener()
            {
                public void actionPerformed( ActionEvent e )
                {
                    try 
                    {
                        JPanel content = new JPanel();
                        content.setLayout( new BorderLayout());
                        List<String> dateList = createList( event);
                        Set<Date> markedDates = marker.getMarkedDates( event );
                        if ( dateList != null)
                        {
                            content.add(new JScrollPane(new JList( dateList.toArray() )), BorderLayout.CENTER);
                            JLabel total = new JLabel("Marked " + markedDates.size() + " from " + dateList.size() );
                            content.add(total, BorderLayout.SOUTH);
                        }
                        else
                        {
                            content.add(new JLabel("Cant view never ending events."), BorderLayout.CENTER);
                        }
                        
                        DialogUI dialogUI = DialogUI.create( getContext(), menuContext.getComponent(), false,content,new String[] {"OK"});
                        dialogUI.setSize( 300, 300);
                        dialogUI.setTitle("Overview of marked dates");
                        if (menuContext.getPoint() != null)
                        {    
                            dialogUI.setLocation( menuContext.getPoint() );
                        }
                        dialogUI.startNoPack();
                    }
                    catch (RaplaException ex )
                    {
                        showException( ex, menuContext.getComponent());
                    }
                }
             });
        
        return new RaplaMenuItem[] {markerItem, viewMarks};
    }
    
    /** returns null if event is lasting forever*/
    public List<String> createList(Reservation event)
    {
        List<String> list = new ArrayList<String>();
        List<AppointmentBlock> array = marker.getAllBlocks( event );
        Set<Date> markedDates = marker.getMarkedDates( event );
        if ( array == null)
        {
            return null;
        }
        for ( AppointmentBlock block: array)
        {
             Date start = new Date(block.getStart());
             String dateString = getRaplaLocale().formatDate( start );
             if ( AppointmentMarker.isMarked( start, markedDates))
             {
                 dateString = " * " + dateString ;
             }
             else
             {
                 dateString = "    " + dateString ;
             }
             list.add( dateString);
        }
        return list;
        
    }
   
}
