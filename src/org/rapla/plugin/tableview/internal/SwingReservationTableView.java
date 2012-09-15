package org.rapla.plugin.tableview.internal;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
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
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.domain.ReservationHelper;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.CalendarModel;
import org.rapla.gui.MenuContext;
import org.rapla.gui.MenuFactory;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.SwingCalendarView;
import org.rapla.gui.internal.common.CalendarAction;
import org.rapla.gui.internal.common.RaplaClipboard;
import org.rapla.gui.toolkit.RaplaMenu;
import org.rapla.gui.toolkit.RaplaMenuItem;
import org.rapla.gui.toolkit.RaplaPopupMenu;
import org.rapla.plugin.RaplaExtensionPoints;
import org.rapla.plugin.abstractcalendar.IntervallChooserPanel;
import org.rapla.plugin.tableview.RaplaTableColumn;
import org.rapla.plugin.tableview.ReservationTableColumn;

public class SwingReservationTableView extends RaplaGUIComponent implements SwingCalendarView, Printable
{
    ReservationTableModel reservationTableModel;

    JTable table;
    CalendarModel model;
    IntervallChooserPanel dateChooser;
    JScrollPane scrollpane;
    JComponent container;
    TableSorter sorter;

	ActionListener copyListener = new ActionListener() {
		
		public void actionPerformed(ActionEvent evt) 
		{
	        List<Reservation> selectedEvents = getSelectedEvents();
            if ( selectedEvents.size() == 1) {
            	Reservation focusedObject = selectedEvents.get( 0);
            	Reservation copyReservation;
				try {
					copyReservation = getModification().clone(focusedObject);
					getClipboard().setReservation(copyReservation);
				} catch (RaplaException e) {
					showException( e, getComponent());
				}
            }
            copy(table, evt);            
		}

		private RaplaClipboard getClipboard() {
	        return getService(RaplaClipboard.class);
	    }

	};

    public SwingReservationTableView( RaplaContext context, CalendarModel model, final boolean editable ) throws RaplaException
    {
        super( context );
       
        table = new JTable() {
            private static final long serialVersionUID = 1L;
            
            public String getToolTipText(MouseEvent e) 
            {
                if (!editable)
                    return null;
                int rowIndex = rowAtPoint( e.getPoint() );
                Reservation reservation = reservationTableModel.getReservationAt( sorter.modelIndex( rowIndex ));
                return getInfoFactory().getToolTip( reservation );
            }
        };
        
        scrollpane = new JScrollPane( table);
        if ( editable )
        {
        	container = new JPanel();
        	container.setLayout( new BorderLayout());
        	container.add( scrollpane, BorderLayout.CENTER);
        	JPanel extensionPanel = new JPanel();
        	extensionPanel.setLayout( new BoxLayout(extensionPanel, BoxLayout.X_AXIS));
        	container.add( extensionPanel, BorderLayout.SOUTH);
            scrollpane.setPreferredSize( new Dimension(600,800));
        	PopupTableHandler popupHandler = new PopupTableHandler();
        	scrollpane.addMouseListener( popupHandler);
        	table.addMouseListener( popupHandler );
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
        
        //Map<?,?> map = getContainer().lookupServicesFor(RaplaExtensionPoints.APPOINTMENT_STATUS);
        //Collection<AppointmentStatusFactory> appointmentStatusFactories = (Collection<AppointmentStatusFactory>) map.values();
       	
       	Map<?,?> map2 = getContainer().lookupServicesFor(RaplaExtensionPoints.RESERVATION_TABLE_COLUMN);
       	
       	Collection< ? extends ReservationTableColumn> reservationColumnPlugins = (Collection<? extends ReservationTableColumn>) map2.values();
       	reservationTableModel = new ReservationTableModel( getLocale(),getI18n(), reservationColumnPlugins );
        sorter =  new TableSorter( reservationTableModel, table.getTableHeader());
        table.setModel(  sorter );
        int column = 0;
        for (RaplaTableColumn<?> col: reservationColumnPlugins)
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

        reservationTableModel.setReservations( model.getReservations() );
        
       

    }
    
   
    public void update() throws RaplaException
    {
        reservationTableModel.setReservations( model.getReservations() );
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

    List<Reservation> getSelectedEvents() {
        int[] rows = table.getSelectedRows();
        List<Reservation> selectedEvents = new ArrayList<Reservation>();
        for (int i=0;i<rows.length;i++)
        {
            Reservation reservation =reservationTableModel.getReservationAt( sorter.modelIndex(rows[i]) );
            selectedEvents.add( reservation);
        }
        return selectedEvents;
    }

    class PopupTableHandler extends MouseAdapter {

        void showPopup(MouseEvent me) {
            Point p = new Point(me.getX(), me.getY());
            List<Reservation> selectedEvents = getSelectedEvents();
            try {
                Reservation focusedObject = null;
                if ( selectedEvents.size() == 1) {
                    focusedObject = selectedEvents.get( 0);
                }
                MenuContext menuContext = new MenuContext( getContext(), focusedObject,getComponent(),p);
                menuContext.setSelectedObjects( selectedEvents);
                RaplaPopupMenu menu= new RaplaPopupMenu();

                // add the new reservations wizards
                RaplaMenu newMenu = new RaplaMenu("new");
                newMenu.setText(getString("new"));
                MenuFactory menuFactory = getService(MenuFactory.class);
                menuFactory.addReservationWizards( newMenu, menuContext, null);
                menu.add( newMenu );
                
                RaplaMenuItem calendar = new RaplaMenuItem("calendar");
                CalendarModel model =  getService(CalendarModel.class);
                CalendarAction action = new CalendarAction( getContext(), getComponent(), model);
                action.changeObjects( selectedEvents);
                action.setStart( ReservationHelper.findFirst( selectedEvents ));
                calendar.setAction( action );
                menu.add( calendar );
                
                // add the edit methods
                final JMenuItem copyItem = new JMenuItem();
    			copyItem.addActionListener( copyListener);
                copyItem.setText(getString("copy"));
        		copyItem.setIcon(  getIcon("icon.copy"));
                menu.add(copyItem);
                menuFactory.addObjectMenu( menu, menuContext, null);
                
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
            List<Reservation> selectedEvents = getSelectedEvents();
            if (me.getClickCount() > 1  && selectedEvents.size() == 1 )
            {
                Reservation reservation = selectedEvents.get( 0);
                try {
                    getReservationController().edit( reservation );
                } catch (RaplaException ex) {
                    showException (ex,getComponent());
                }
            }
        }
    }

    Printable printable = null;
    /**
     * @see java.awt.print.Printable#print(java.awt.Graphics, java.awt.print.PageFormat, int)
     */
    public int print(Graphics graphics, PageFormat format, int page) throws PrinterException {
    	MessageFormat f1 = new MessageFormat( model.getNonEmptyTitle());
    	Printable  printable = table.getPrintable( JTable.PrintMode.FIT_WIDTH,f1, null );
        return printable.print( graphics, format, page);
    }


 }
