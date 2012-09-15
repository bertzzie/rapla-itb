/*--------------------------------------------------------------------------* | Copyright (C) 2006  Christopher Kohlhaas                                 |
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

package org.rapla.gui.internal.splitpanes;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;

import javax.swing.JComponent;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.DefaultTreeModel;

import org.rapla.components.calendar.RaplaArrowButton;
import org.rapla.entities.dynamictype.ClassificationFilter;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.internal.RaplaSelectionPane;
import org.rapla.gui.internal.common.CalendarSelectionModel;
import org.rapla.gui.internal.common.MultiCalendarView;
import org.rapla.gui.internal.edit.ClassifiableFilterEdit;

public abstract class RaplaFiltrableSelectionPane extends RaplaSelectionPane {

    boolean treeListenersEnabled = true;
    protected FilterEdit filterEdit;
   
    
    public RaplaFiltrableSelectionPane(final RaplaContext context, MultiCalendarView view, final CalendarSelectionModel model) throws RaplaException {
        super(context, view, model);
        buttonsPanel.setLayout( new BorderLayout());
        filterEdit = new FilterEdit(context, model);
        buttonsPanel.add(filterEdit.getButton(), BorderLayout.EAST);
    }

    
    protected abstract DefaultTreeModel generateTree() throws RaplaException;

    
    protected HashSet<?> getSelectedObjects() throws RaplaException {
        HashSet<Object> elements = new HashSet<Object>(treeSelection.getSelectedElements());
        getModel().setSelectedObjects(elements);
        return elements;
    }
    
    public RaplaArrowButton getFilterButton() {
        return (RaplaArrowButton) filterEdit.getButton();
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
                    boolean isResourceOnly = true;
                    try {
                        if ( ui != null)
                        {
                            ui.removeChangeListener( listener);
                        }
                        ui = new ClassifiableFilterEdit( context, isResourceOnly);
                        ui.addChangeListener(listener);
                        ui.setFilter( model);
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
                    model.setAllocatableFilter( filters);
                    updateTree();
                    applyFilter();
                } catch (Exception ex) {
                    showException(ex, getComponent());
                }
            }
        };
        public JComponent getButton()
        {
            return filterButton;
        }
        
    }
    
}
