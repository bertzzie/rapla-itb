package org.rapla.plugin.tableview.internal;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.rapla.components.calendar.DateChangeEvent;
import org.rapla.components.calendar.DateChangeListener;
import org.rapla.components.tablesorter.TableSorter;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.AppointmentBlock;
import org.rapla.entities.domain.Reservation;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.CalendarModel;
import org.rapla.gui.MenuContext;
import org.rapla.gui.MenuFactory;
import org.rapla.gui.ObjectMenuFactory;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.SwingCalendarView;
import org.rapla.gui.internal.action.AppointmentAction;
import org.rapla.gui.toolkit.MenuInterface;
import org.rapla.gui.toolkit.RaplaMenu;
import org.rapla.gui.toolkit.RaplaMenuItem;
import org.rapla.gui.toolkit.RaplaPopupMenu;
import org.rapla.plugin.RaplaExtensionPoints;
import org.rapla.plugin.abstractcalendar.IntervallChooserPanel;
import org.rapla.plugin.tableview.AppointmentTableColumn;

public class SwingAppointmentTableView extends RaplaGUIComponent implements SwingCalendarView, Printable
{
    AppointmentTableModel appointmentTableModel;

    JTable table;
    CalendarModel model;
    IntervallChooserPanel dateChooser;
    JComponent scrollpane;
    TableSorter sorter;

	ActionListener copyListener = new ActionListener() {
		
		public void actionPerformed(ActionEvent evt) 
		{
	        List<AppointmentBlock> selectedEvents = getSelectedEvents();
            if ( selectedEvents.size() == 1) {
            	AppointmentBlock appointmentBlock = selectedEvents.get( 0);
            	Appointment appointment = appointmentBlock.getAppointment();
				try {
					Date date = new Date(appointmentBlock.getStart());
					Component sourceComponent = table;
					Point p = null;
					getReservationController().copyAppointment(appointment,date,sourceComponent,p);
				} catch (RaplaException e) {
					showException( e, getComponent());
				}
            }
            copy(table, evt);
		}

	};

	private JComponent container;

    public SwingAppointmentTableView( RaplaContext context, CalendarModel model, final boolean editable ) throws RaplaException
    {
        super( context );
       
        table = new JTable() {
            private static final long serialVersionUID = 1L;
            
            public String getToolTipText(MouseEvent e) 
            {
                if (!editable)
                    return null;
                int rowIndex = rowAtPoint( e.getPoint() );
                AppointmentBlock app = appointmentTableModel.getAppointmentAt( sorter.modelIndex( rowIndex ));
                Reservation reservation = app.getAppointment().getReservation();
                return getInfoFactory().getToolTip( reservation );
            }
        };
    	scrollpane = new JScrollPane( table);
    	if ( editable )
        {
            scrollpane.setPreferredSize( new Dimension(600,800));
        	PopupTableHandler popupHandler = new PopupTableHandler();
        	scrollpane.addMouseListener( popupHandler);
        	table.addMouseListener( popupHandler );
        	container = new JPanel();
        	container.setLayout( new BorderLayout());
        	container.add( scrollpane, BorderLayout.CENTER);
        	JPanel extensionPanel = new JPanel();
        	extensionPanel.setLayout( new BoxLayout(extensionPanel, BoxLayout.X_AXIS));
        	container.add( extensionPanel, BorderLayout.SOUTH);
            Map<?,?> map3 = getContainer().lookupServicesFor(RaplaExtensionPoints.RESERVATION_TABLE_SUMMARY);
            Collection< ? extends SummaryExtension> reservationSummaryExtensions = (Collection<? extends SummaryExtension>) map3.values();
    		for ( SummaryExtension summary:reservationSummaryExtensions)
    		{
    			summary.init(table, extensionPanel);
    		}

        }
        else
        {
            Dimension size = table.getPreferredSize();
            scrollpane.setBounds( 0,0,(int)600, (int)size.getHeight());
            container = scrollpane;
        }
        this.model = model;
        
       	Map<?,?> map2 = getContainer().lookupServicesFor(RaplaExtensionPoints.APPOINTMENT_TABLE_COLUMN);
       	
       	Collection< ? extends AppointmentTableColumn> columnPlugins = (Collection<? extends AppointmentTableColumn>) map2.values();
		appointmentTableModel = new AppointmentTableModel( getLocale(),getI18n(), columnPlugins );
        sorter =  new TableSorter( appointmentTableModel, table.getTableHeader());
        table.setModel(  sorter );
        int column = 0;
        for (AppointmentTableColumn col: columnPlugins)
        {
        	col.init(table.getColumnModel().getColumn(column  ));
        	column++;	
        }
        
        table.setColumnSelectionAllowed( true );
        table.setRowSelectionAllowed( true);
        table.getTableHeader().setReorderingAllowed(true);
       
    	table.registerKeyboardAction(copyListener,getString("copy"),COPY_STROKE,JComponent.WHEN_FOCUSED);
        
        dateChooser = new IntervallChooserPanel( context, model);
        dateChooser.addDateChangeListener( new DateChangeListener() {
            public void dateChanged( DateChangeEvent evt )
            {
                try {
                    update(  );
                } catch (RaplaException ex ){
                    showException( ex, getComponent());
                }
            }
        });

       
        update(model);
    }

	protected void update(CalendarModel model) throws RaplaException 
	{
		appointmentTableModel.setAppointments(model.getBlocks());
	}
   
    public void update() throws RaplaException
    {
    	update(model);
        dateChooser.update();
    }

    public JComponent getDateSelection()
    {
        return dateChooser.getComponent();
    }

    public void scrollToStart()
    {

    }

    public JComponent getComponent()
    {
    	return container;
    }

    
    List<AppointmentBlock> getSelectedEvents() {
        int[] rows = table.getSelectedRows();
        List<AppointmentBlock> selectedEvents = new ArrayList<AppointmentBlock>();
        for (int i=0;i<rows.length;i++)
        {
            AppointmentBlock reservation =appointmentTableModel.getAppointmentAt( sorter.modelIndex(rows[i]) );
            selectedEvents.add( reservation);
        }
        return selectedEvents;
    }

    class PopupTableHandler extends MouseAdapter 
    {
        void showPopup(MouseEvent me) 
        {
            Point p = new Point(me.getX(), me.getY());
            List<AppointmentBlock> selectedEvents = getSelectedEvents();
            try {
                AppointmentBlock focusedObject = null;
                if ( selectedEvents.size() == 1) 
                {
                    focusedObject = selectedEvents.get( 0);
                }
                
                RaplaPopupMenu menu= new RaplaPopupMenu();
             // add the new reservations wizards

                {
	                MenuContext menuContext = new MenuContext( getContext(), null);
	                RaplaMenu newMenu = new RaplaMenu("new");
	                newMenu.setText(getString("new"));
	                MenuFactory menuFactory = getService(MenuFactory.class);
	                menuFactory.addReservationWizards( newMenu, menuContext, null);
	                menu.add( newMenu );
                }
                
                {
                
                	final JMenuItem copyItem = new JMenuItem();
                	copyItem.addActionListener( copyListener);
                	copyItem.setIcon(  getIcon("icon.copy"));
                	copyItem.setText(getString("copy"));
                	menu.add(copyItem);
                }
                Date start = null;
				if ( focusedObject != null)
                {
					start = new Date(focusedObject.getStart());
	                Component component = table;
	                Appointment appointment = focusedObject.getAppointment();
	                
	                addAppointmentAction( menu, component, p ).setEdit(appointment,start);
	                addAppointmentAction( menu, component, p).setView(appointment);
	                addAppointmentAction( menu, component, p).setDelete(appointment,start);
	                
                }
                
				Iterator<?> it = getContainer().lookupServicesFor( RaplaExtensionPoints.OBJECT_MENU_EXTENSION).values().iterator();
				while (it.hasNext())
				{
					ObjectMenuFactory objectMenuFact = (ObjectMenuFactory) it.next();
					Appointment appointment = focusedObject != null? focusedObject.getAppointment() : null;
					MenuContext menuContext = new MenuContext( getContext(), appointment);
					menuContext.put("selected_date", start);
					menuContext.setSelectedObjects( selectedEvents);
					RaplaMenuItem[] items = objectMenuFact.create( menuContext, appointment );
					for ( int i =0;i<items.length;i++)
					{
					    RaplaMenuItem item =  items[i];
					    menu.add( item);
					}
				}
//                RaplaMenuItem calendar = new RaplaMenuItem("calendar");
//                CalendarModel model =  getService(CalendarModel.class);
//                CalendarAction action = new CalendarAction( getContext(), getComponent(), model);
//                action.changeObjects( selectedEvents);
//                action.setStart( ReservationHelper.findFirst( selectedEvents ));
//                calendar.setAction( action );
//                menu.add( calendar );
                
                // add the edit methods
            /*
                menuFactory.addObjectMenu( menu, menuContext, null);
    */            
                menu.show( table, p.x, p.y);
            } catch (RaplaException ex) {
                showException (ex,getComponent());
            }
        }

        /** Implementation-specific. Should be private.*/
        public void mousePressed(MouseEvent me) {
            if (me.isPopupTrigger())
                showPopup(me);
        }
        /** Implementation-specific. Should be private.*/
        public void mouseReleased(MouseEvent me) {
            if (me.isPopupTrigger())
                showPopup(me);
        }
        /** we want to edit the reservation on double click*/
        public void mouseClicked(MouseEvent me) {
            List<AppointmentBlock> selectedEvents = getSelectedEvents();
            if (me.getClickCount() > 1  && selectedEvents.size() == 1 )
            {
                Appointment appointment = selectedEvents.get( 0).getAppointment();
                try {
                    getReservationController().edit( appointment);
                } catch (RaplaException ex) {
                    showException (ex,getComponent());
                }
            }
        }
    }
    
    public AppointmentAction addAppointmentAction(MenuInterface menu, Component parent, Point p) throws RaplaException {
        AppointmentAction action = new AppointmentAction(getContext(),parent,p);
        menu.add(new JMenuItem(action));
     //   action.setKeepTime( keepTime); 
        return action;
    }

    Printable printable = null;
    /**
     * @see java.awt.print.Printable#print(java.awt.Graphics, java.awt.print.PageFormat, int)
     */
    public int print(Graphics graphics, PageFormat format, int page) throws PrinterException {
    	MessageFormat f1 = new MessageFormat( model.getNonEmptyTitle());
    	MessageFormat f2 = null;//new MessageFormat( "footer");
    	Printable  printable = table.getPrintable( JTable.PrintMode.FIT_WIDTH,f1, f2 );
        return printable.print( graphics, format, page);
    }


 }
