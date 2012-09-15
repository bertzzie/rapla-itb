
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

package org.rapla.gui.internal.common;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.rapla.components.calendar.RaplaArrowButton;
import org.rapla.components.layout.TableLayout;
import org.rapla.components.util.Tools;
import org.rapla.entities.dynamictype.ClassificationFilter;
import org.rapla.entities.dynamictype.DynamicTypeAnnotations;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.SwingCalendarView;
import org.rapla.gui.ViewFactory;
import org.rapla.gui.internal.edit.ClassifiableFilterEdit;
import org.rapla.gui.toolkit.IdentifiableMenuEntry;
import org.rapla.gui.toolkit.RaplaMenu;
import org.rapla.gui.toolkit.RaplaMenuItem;
import org.rapla.gui.toolkit.RaplaWidget;
import org.rapla.plugin.RaplaExtensionPoints;


public class MultiCalendarView extends RaplaGUIComponent
    implements
    RaplaWidget
{
    
    private final JPanel page = new JPanel();
    private final JPanel header = new JPanel();
    Map<String,RaplaMenuItem> viewMenuItems = new HashMap<String,RaplaMenuItem>();
    JComboBox viewChooser;

    // Default view, when no plugin defined
    private SwingCalendarView currentView = new SwingCalendarView() {
        JLabel noViewDefined = new JLabel("No view defined");

        public JComponent getDateSelection()
        {
            return null;
        }

        public void scrollToStart()
        {
        }

        public JComponent getComponent()
        {
            return noViewDefined;
        }

        public void update( ) throws RaplaException
        {
        }

    };

    private final CalendarSelectionModel model;
    final Map<String,ViewFactory> map;
    /** renderer for weekdays in month-view */
    boolean editable = true;
    boolean listenersEnabled = true;
    FilterEdit filter;
    
    public MultiCalendarView(RaplaContext context,CalendarSelectionModel model) throws RaplaException {
    	this( context, model, true);
    }
    
	public MultiCalendarView(RaplaContext context,CalendarSelectionModel model, boolean editable) throws RaplaException {
        super( context);
        this.editable = editable;
        map = (Map<String,ViewFactory>)getContainer().lookupServicesFor(RaplaExtensionPoints.CALENDAR_VIEW_EXTENSION);
        this.model = model;
        String[] ids = getIds();
        {
	         ViewFactory factory = findFactory( model.getViewId());
             if ( factory == null)
             {
                 if ( ids.length != 0 ) {
	                 String firstId = ids[0];
	                 model.setViewId( firstId );
	                 factory = findFactory( firstId );
	             }
	         }
         }
        RaplaMenu view = (RaplaMenu) getContext().lookup( InternMenus.VIEW_MENU_ROLE);
        if ( !view.hasId( "views") ) 
        {
            addMenu( model, ids, view );
        }

        addTypeChooser( ids );
        header.setLayout(new BorderLayout());
        header.add( viewChooser, BorderLayout.CENTER);
        filter =new FilterEdit(context,model);
        final JPanel filterContainer = new JPanel();
        filterContainer.setLayout( new BorderLayout());
        filterContainer.add(filter.getButton(), BorderLayout.WEST);
        header.add( filterContainer, BorderLayout.SOUTH);
        page.setLayout(new TableLayout( new double[][]{
                {TableLayout.PREFERRED, TableLayout.FILL}
                ,{TableLayout.PREFERRED, TableLayout.FILL}}));
        page.add( header, "0,0,f,f");
        page.setBackground( Color.white );
        update();
    }
    private void addTypeChooser( String[] ids )
    {
        viewChooser = new JComboBox( ids);
         viewChooser.setVisible( viewChooser.getModel().getSize() > 0);
         viewChooser.setSelectedItem( getModel().getViewId() );
         viewChooser.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
            	if ( !listenersEnabled )
            		return;
                String viewId = (String) ((JComboBox)evt.getSource()).getSelectedItem();
                try {
                    selectView( viewId );
                } catch (RaplaException ex) {
                    showException(ex, page);
                }
            }
        }
        );
        viewChooser.setRenderer( new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;
            
            public Component getListCellRendererComponent(JList arg0, Object selectedItem, int index, boolean arg3, boolean arg4) {
                super.getListCellRendererComponent( arg0, selectedItem, index, arg3, arg4);
                if ( selectedItem == null) {
                    setIcon( null );
                } else {
                    ViewFactory factory = findFactory( (String)selectedItem);
                    setText( factory.getName() );
                    setIcon( factory.getIcon());
                }
                return this;
            }
        });
    }
    
    public RaplaArrowButton getFilterButton() 
    {
    	return filter.getButton();
    }
    public class FilterEdit extends RaplaGUIComponent
    {
        protected RaplaArrowButton filterButton;
        Popup popup;
        ClassifiableFilterEdit ui;
            
        public FilterEdit(final RaplaContext context,final CalendarSelectionModel model) throws RaplaException
        {
            super(context);
            filterButton = new RaplaArrowButton('v');
            filterButton.setText(getString("filter"));
            filterButton.setSize(80,18);
            filterButton.addActionListener( new ActionListener()
            {
                public void actionPerformed(ActionEvent e) {
                    
                    if ( popup != null)
                    {
                        popup.hide();
                        popup= null;
                        filterButton.setChar('v');
                        return;
                    }
                    boolean isResourceOnly = false;
                    try {
                        if ( ui != null)
                        {
                            ui.removeChangeListener( listener);
                        }
                        ui = new ClassifiableFilterEdit( context, isResourceOnly);
                        ui.addChangeListener(listener);
                        ui.setTypes( getQuery().getDynamicTypes( DynamicTypeAnnotations.VALUE_RESERVATION_CLASSIFICATION ));
                        ui.mapFrom(model);
                        final Point locationOnScreen = filterButton.getLocationOnScreen();
                        final int y = locationOnScreen.y + 18;
                        final int x = locationOnScreen.x;
                        popup = PopupFactory.getSharedInstance().getPopup( filterButton, ui.getComponent(), x, y);
                        popup.show();
                        filterButton.setChar('^');
                    } catch (Exception ex) {
                        showException(ex, getComponent());
                    }
                }
                
            });
            
        }
        
        private ChangeListener listener = new ChangeListener() {
            
            public void stateChanged(ChangeEvent e) {
                try {
                    final ClassificationFilter[] filters = ui.getFilters();
                    model.setReservationFilter( filters );
                    update();
                } catch (Exception ex) {
                    showException(ex, getComponent());
                }
            }
        };
        public RaplaArrowButton getButton()
        {
            return filterButton;
        }
        
    }
    private void addMenu( CalendarSelectionModel model, String[] ids, RaplaMenu view )
    {
        RaplaMenu viewMenu = new RaplaMenu("views");
        viewMenu.setText(getString("show_as"));
        view.insertBeforeId( viewMenu, "show_tips");
        ButtonGroup group = new ButtonGroup();
        for (int i=0;i<ids.length;i++)
        {
            String id = ids[i];
            RaplaMenuItem viewItem = new RaplaMenuItem( id);
            if ( id.equals( model.getViewId()))
            {
                viewItem.setIcon( getIcon("icon.radio"));
             }  
            else
             {  
                 viewItem.setIcon( getIcon("icon.empty"));
             }
             
        	 group.add( viewItem );
             
             ViewFactory factory = findFactory( id );
             viewItem.setText( factory.getName() );
             viewMenu.add( viewItem );
             viewItem.setSelected( id.equals( getModel().getViewId()));
             viewMenuItems.put( id, viewItem );
             viewItem.addActionListener( new ActionListener() {
   
        		public void actionPerformed(ActionEvent evt) {
                	if ( !listenersEnabled )
                		return;
                    String viewId = ((IdentifiableMenuEntry)evt.getSource()).getId();
                    try {
                        selectView( viewId );
                    } catch (RaplaException ex) {
                        showException(ex, page);
                    }
        		}
   
             });
         }
    }

    private ViewFactory findFactory(String id) {
        for (Iterator<ViewFactory> it = map.values().iterator();it.hasNext();) {
            ViewFactory factory =  it.next();
            if ( factory.getViewId().equals( id ) ) {
                return factory;
            }
        }
        return null;
    }

    private void selectView(String viewId) throws RaplaException {
    	listenersEnabled = false;
        try {
        	getModel().setViewId( viewId );
        	update();
        	getSelectedCalendar().scrollToStart();
        	if ( viewMenuItems.size() > 0) {
            	for ( Iterator<RaplaMenuItem> it = viewMenuItems.values().iterator();it.hasNext();) 
                {
                    RaplaMenuItem item =  it.next();
                    if ( item.isSelected() ) 
                    {
                        item.setIcon( getIcon("icon.empty"));
                    }
                        
                }
                RaplaMenuItem item = viewMenuItems.get( viewId );
                item.setSelected( true );
                item.setIcon( getIcon("icon.radio"));
            }
        	viewChooser.setSelectedItem( viewId );
        } finally {
        	listenersEnabled = true;
        }
    }

    private String[] getIds() {
        List<ViewFactory> factoryList = new ArrayList<ViewFactory>(map.values());
        Collections.sort( factoryList, new Comparator<ViewFactory>() {
            public int compare( ViewFactory arg0, ViewFactory arg1 )
            {
                ViewFactory f1 = arg0;
                ViewFactory f2 = arg1;
                return f1.getMenuSortKey().compareTo( f2.getMenuSortKey() );
            }
        });
        List<String> list = new ArrayList<String>();
        for (Iterator<ViewFactory> it = factoryList.iterator();it.hasNext();) {
            ViewFactory factory =  it.next();
            list.add(factory.getViewId());
        }
        return list.toArray( Tools.EMPTY_STRING_ARRAY);
    }

    private CalendarSelectionModel getModel() {
        return model;
    }

    private final HashMap<String,SwingCalendarView> cache = new HashMap<String,SwingCalendarView>();

    public void update() throws RaplaException {
        try
        {
            listenersEnabled = false;
            if ( currentView!= null ) {
                currentView.getComponent().setVisible( false );
                if ( currentView.getDateSelection() != null)
                    currentView.getDateSelection().setVisible( false );
            }
    
            final String viewId = model.getViewId();
            ViewFactory factory = findFactory( viewId );
            viewChooser.setSelectedItem( viewId );
            if ( factory == null) {
                return;
            }
    
            SwingCalendarView cal = cache.get( factory.getViewId() );
            if ( cal == null ) {
                cal = factory.createSwingView( getContext(), getModel(), editable);
                cache.put( factory.getViewId(),  cal);
                if ( cal.getDateSelection() != null)
                    page.add( cal.getDateSelection(), "1,0,f,f" );
                page.add( cal.getComponent(), "0,1,1,1,f,f" );
                cal.getComponent().setBorder( BorderFactory.createEtchedBorder());
                page.invalidate();
            } else {
                cal.getComponent().setVisible( true );
                if ( cal.getDateSelection() != null)
                {
                    cal.getDateSelection().setVisible( true );
                }
                cal.update( );
            }
    
            currentView = cal;
        }
        finally
        {
            listenersEnabled = true;
        }
    }

    public SwingCalendarView getSelectedCalendar() {
        return currentView;
    }

    public JComponent getComponent() {
        return page;
    }


}
