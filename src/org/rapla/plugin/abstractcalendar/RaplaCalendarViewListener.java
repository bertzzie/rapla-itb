/**
 *
 */
package org.rapla.plugin.abstractcalendar;

import java.awt.Component;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JMenuItem;

import org.rapla.components.calendarview.Block;
import org.rapla.components.calendarview.swing.ViewListener;
import org.rapla.entities.NamedComparator;
import org.rapla.entities.User;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.CalendarModel;
import org.rapla.gui.MenuContext;
import org.rapla.gui.ObjectMenuFactory;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.ReservationEdit;
import org.rapla.gui.ReservationWizard;
import org.rapla.gui.internal.action.AppointmentAction;
import org.rapla.gui.internal.common.CalendarSelectionModel;
import org.rapla.gui.internal.common.RaplaClipboard;
import org.rapla.gui.toolkit.MenuInterface;
import org.rapla.gui.toolkit.RaplaMenu;
import org.rapla.gui.toolkit.RaplaMenuItem;
import org.rapla.gui.toolkit.RaplaPopupMenu;
import org.rapla.plugin.RaplaExtensionPoints;

public class RaplaCalendarViewListener extends RaplaGUIComponent
implements
ViewListener
{
	private boolean keepTime = false;

	protected JComponent calendarContainerComponent;
    CalendarModel model;
    public RaplaCalendarViewListener(RaplaContext context, CalendarModel model, JComponent calendarContainerComponent)  throws RaplaException {
        super( context);
        this.model = model;
        this.calendarContainerComponent = calendarContainerComponent;
    }

    protected CalendarModel getModel() 
    {
        return model;
    }

    public void selectionChanged(Date start,Date end) 
    {
		// #FIXME this cast need to be replaced without adding the setter methods to the readOnly interface CalendarModel
    	CalendarSelectionModel castedModel = (CalendarSelectionModel)model;
		castedModel.markInterval( start, end);
		Collection<Allocatable> markedAllocatables = getMarkedAllocatables();
		castedModel.setMarkedAllocatables( markedAllocatables);
    }

	public void selectionPopup(Component component,Point p,Date start,Date end, int slotNr)
	{
        showPopupMenu(component,p,start,end, slotNr);
    }

    public void blockPopup(Block block,Point p) 
    {
        SwingRaplaBlock b = (SwingRaplaBlock) block;
        if ( !b.getContext().isEventSelected() ) 
        {
            return;
        }
        showPopupMenu(b.getView(),p,b.getAppointment(),b.getStart(), b.isException());
    }

    public void blockEdit(Block block,Point p) {
    	// double click on block in view.
        SwingRaplaBlock b = (SwingRaplaBlock) block;
        if ( !b.getContext().isEventSelected() ) {
            return;
        }
        try {
            if (!canModify(b.getReservation()))
                return;
            final Appointment appointment = b.getAppointment();
            appointment.getReservation().setSelectedSlotDate( b.getStart());
            getReservationController().edit(appointment);
        } catch (RaplaException ex) {
            showException(ex,b.getView());
        }
    }

    public void moved(Block block,Point p,Date newStart, int slotNr) {
        SwingRaplaBlock b = (SwingRaplaBlock) block;
        try {
            newStart = calcStartDate( newStart, b.getAppointment());
            getReservationController().moveAppointment(b.getAppointment()
                                                       ,b.getStart()
                                                       ,newStart
                                                       ,calendarContainerComponent
                                                       ,p, keepTime);
        } catch (RaplaException ex) {
            showException(ex,b.getView());
        }
    }

    public boolean isKeepTime() 
    {
		return keepTime;
	}

	public void setKeepTime(boolean keepTime) 
	{
		this.keepTime = keepTime;
	}

    public void resized(Block block,Point p,Date newStart, Date newEnd, int slotNr) {
        SwingRaplaBlock b = (SwingRaplaBlock) block;
        try {
           getReservationController().resizeAppointment(b.getAppointment()
                    ,b.getStart()
                    ,newStart
                    ,newEnd
                    ,calendarContainerComponent
                    ,p, keepTime);
        } catch (RaplaException ex) {
            showException(ex,b.getView());
        }
    }

    /** this method should be overriden by subclasses who want to change the selected allocatable that are passed to the edit. @see SwingCompactWeekCalendar */
    protected void showPopupMenu(Component component,Point p,Date start,Date end, int slotNr)
    {
        Collection<Allocatable> markedAllocatables = getMarkedAllocatables();
		showPopupMenu( component, p, start,end, slotNr, markedAllocatables);
    }    
    
    public List<Allocatable> getSortedAllocatables() 
    {
		try {
			Allocatable[] selectedAllocatables;
			selectedAllocatables = model.getSelectedAllocatables();
			List<Allocatable> sortedAllocatables = new ArrayList<Allocatable>( Arrays.asList( selectedAllocatables));
			Collections.sort(sortedAllocatables, new NamedComparator<Allocatable>( getLocale() ));
			return sortedAllocatables;
		} catch (RaplaException e) {
			getLogger().error(e.getMessage(), e);
			return Collections.emptyList();
		}
    }
    
    protected Collection<Allocatable> getMarkedAllocatables()
    {
    	List<Allocatable>  selectedAllocatables = getSortedAllocatables();
    	if ( selectedAllocatables.size()== 1 ) {
           return Collections.singletonList(selectedAllocatables.get(0));
    	}
       return Collections.emptyList();
	}

    protected void showPopupMenu(Component component,Point p,Date start,Date end, int slotNr, Collection<Allocatable> selectedAllocatables)
    {
        showContextPopup(component, p, start, end, selectedAllocatables);
    }

	protected void showContextPopup(Component component, Point p, Date start,
			Date end, Collection<Allocatable> selectedAllocatables) {
		try {
            User user = getUser();
            Date today = getQuery().today();
            boolean canAllocate = false;
            for ( Allocatable alloc: selectedAllocatables) {
                if (alloc.canAllocate( user, start, end, today))
                    canAllocate = true;
            }
            RaplaPopupMenu menu= new RaplaPopupMenu();
            if ( canAllocate || (selectedAllocatables.size() == 0 && canUserAllocateSomething( getUser()))  ) {
                RaplaMenu newItem = new RaplaMenu("new");
                newItem.setText(getString("new"));
                menu.add(newItem);
            	Iterator<ReservationWizard> it = ((Collection<ReservationWizard>)getContainer().lookupServicesFor( RaplaExtensionPoints.RESERVATION_WIZARD_EXTENSION).values()).iterator();
                while (it.hasNext())
                {
                    ReservationWizard wizard =  it.next();
                    addAppointmentAction(newItem,component,p).setNew(wizard, selectedAllocatables, start,end ); 
                }
               
                ReservationEdit[] editWindows = getReservationController().getEditWindows();
                if ( editWindows.length >0 )
                {
	                RaplaMenu addItem = new RaplaMenu("add_to");
	                addItem.setText(getString("add_to"));
	                menu.add(addItem);
	            	
	                for ( ReservationEdit reservationEdit: editWindows)
	                {
	                    addAppointmentAction(addItem,component,p).setAddTo( reservationEdit, start,end, selectedAllocatables);
	                }
                }
            } 
            else 
            {
               JMenuItem cantAllocate = new JMenuItem(getString("permission.denied"));
               cantAllocate.setEnabled( false);
               menu.add( cantAllocate);
            }

            RaplaClipboard clipboard = getService(RaplaClipboard.class);
			Appointment appointment =  clipboard.getAppointment();
            if ( appointment != null ) {
                Date pasteStart = calcStartDate( start, appointment );
                if (!clipboard.isWholeReservation())
               	{
            	   addAppointmentAction(menu,component,p).setPaste( pasteStart );
                }
                addAppointmentAction(menu,component,p).setPasteAsNew( pasteStart );
            }

            menu.show(component,p.x,p.y);
        } catch (RaplaException ex) {
            showException(ex, calendarContainerComponent);
        }
	}


    protected void showPopupMenu(Component component,Point p,Appointment appointment,Date start, boolean isException)
    {
        try {
            Date copyStart = calcStartDate( start, appointment );
            RaplaPopupMenu menu= new RaplaPopupMenu();

            addAppointmentAction( menu, component, p).setCopy(appointment, copyStart);
            addAppointmentAction( menu, component, p ).setEdit(appointment,start);
            if ( !isException) {
                addAppointmentAction( menu, component, p).setDelete(appointment,start);
            }
            addAppointmentAction( menu, component, p).setView(appointment);

            Iterator<?> it = getContainer().lookupServicesFor( RaplaExtensionPoints.OBJECT_MENU_EXTENSION).values().iterator();
            while (it.hasNext())
            {
                ObjectMenuFactory objectMenuFact = (ObjectMenuFactory) it.next();
                MenuContext menuContext = new MenuContext( getContext(), appointment);
                menuContext.put("selected_date", start);
                
                RaplaMenuItem[] items = objectMenuFact.create( menuContext, appointment );
                for ( int i =0;i<items.length;i++)
                {
                    RaplaMenuItem item =  items[i];
                    menu.add( item);
                }
            }

            menu.show(component,p.x,p.y);
        } catch (RaplaException ex) {
            showException(ex, calendarContainerComponent);
        }
    }

    public AppointmentAction addAppointmentAction(MenuInterface menu, Component parent, Point p) throws RaplaException {
        AppointmentAction action = new AppointmentAction(getContext(),parent,p);
        menu.add(new JMenuItem(action));
        action.setKeepTime( keepTime); 
        return action;
    }
    
   

     /**
      * If the view is a week-view this method will just return the passed date.
      * returns the passed date as start-date. Override this method if you just want another date as start date of an appointment.
      * For Examples of override behaviour see month-view or compact-view
      */
    protected Date calcStartDate(Date date, Appointment appointment) {
        return date;
    }

}