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

import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JCheckBox;
import javax.swing.tree.DefaultTreeModel;

import org.rapla.entities.Category;
import org.rapla.entities.User;
import org.rapla.entities.domain.Permission;
import org.rapla.entities.dynamictype.ClassificationFilter;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.internal.action.OnlyMyAction;
import org.rapla.gui.internal.common.CalendarSelectionModel;
import org.rapla.gui.internal.common.InternMenus;
import org.rapla.gui.internal.common.MultiCalendarView;
import org.rapla.gui.internal.view.TreeFactoryImpl;
import org.rapla.gui.toolkit.RaplaMenu;
import org.rapla.gui.toolkit.RaplaMenuItem;
/** FIXME This class is not correctly named the code should be moved elsewher*/
public final class RaplaResourceSelectionPane extends RaplaFiltrableSelectionPane {

    OnlyMyAction myEventsAction;
    RaplaMenuItem ownReservations;
    JCheckBox onlyOwnReservations;

    public RaplaResourceSelectionPane(RaplaContext context, MultiCalendarView view, CalendarSelectionModel model) throws RaplaException {
        super(context, view, model);

        onlyOwnReservations = new JCheckBox();
        onlyOwnReservations.setFont(onlyOwnReservations.getFont().deriveFont(Font.PLAIN, (float) 10.0));
        myEventsAction = new OnlyMyAction(context, model);
        onlyOwnReservations.setAction(myEventsAction);
        onlyOwnReservations.setIcon(getIcon("icon.unchecked"));
        onlyOwnReservations.setSelectedIcon(getIcon("icon.checked"));

        //buttonsPanel.add(onlyOwnReservations);

        myEventsAction.addPropertyChangeListener(new PropertyChangeListener() {
            
            public void propertyChange(PropertyChangeEvent evt) {
                try {
                    if ( evt.getPropertyName().equals("model")) {
                        updateActions();
                        updateTree();
                        applyFilter();
                    }
                } catch (Exception ex) {
                    showException(ex, getComponent());
                }
            }
        });

        ownReservations = new RaplaMenuItem("only_own_reservations");
        ownReservations.setText(getString("only_own_reservations"));
        ownReservations.setAction(myEventsAction);
        ownReservations.setIcon(getIcon("icon.unchecked"));
     //   filterAction.setResourceOnly(false);
        
        RaplaMenu viewMenu = (RaplaMenu)getContext().lookup( InternMenus.VIEW_MENU_ROLE);
//        RaplaMenuItem filterMenu = new RaplaMenuItem("filter");
//        filterMenu.setAction( filterAction );
//        viewMenu.insertBeforeId( filterMenu,"view_save" );

        ownReservations = new RaplaMenuItem("only_own_reservations");
        ownReservations.setText( getString("only_own_reservations"));
        ownReservations.setAction( myEventsAction );
        ownReservations.setIcon( getIcon("icon.unchecked"));
        viewMenu.insertBeforeId( ownReservations, "show_tips" );

    }

    private boolean canSeeEventsFromOthers() {
        try {
            Category category = getQuery().getUserGroupsCategory().getCategory(Permission.GROUP_CAN_READ_EVENTS_FROM_OTHERS);
            if (category == null) {
                return true;
            }
            User user = getUser();
            return user.isAdmin() || user.belongsTo(category);
        } catch (RaplaException ex) {
            return false;
        }
    }

    
//    protected HashSet getSelectedObjects() throws RaplaException {
//        HashSet elements = new HashSet(treeSelection.getSelectedElements());
//        if (ownReservations.isSelected()) {
//            elements.add(getUser());
//        }
//        getModel().setSelectedObjects(elements);
//        return elements;
//    }
    
    
    protected DefaultTreeModel generateTree() throws RaplaException {
        ClassificationFilter[] filter = getModel().getAllocatableFilter();
        boolean onlyOwn = model.isOnlyCurrentUserSelected();
        User conflictUser = onlyOwn ? getUser() : null;
        final TreeFactoryImpl treeFactoryImpl = (TreeFactoryImpl) getTreeFactory();
        DefaultTreeModel treeModel = treeFactoryImpl.createModel(filter, conflictUser, null);
        return treeModel;
    }
    
    

    
    private void updateActions() throws RaplaException 
    {
        if (!canSeeEventsFromOthers()) {
            myEventsAction.setEnabled(false);
        }
        if (myEventsAction != null) {
            boolean oldState = myEventsAction.isEnabled();
            myEventsAction.setEnabled(false);

            boolean isSelected = model.isOnlyCurrentUserSelected();
            ownReservations.setIcon(isSelected ? getIcon("icon.checked") : getIcon("icon.unchecked"));
            onlyOwnReservations.setSelected(isSelected);
            ownReservations.setSelected(isSelected);
            myEventsAction.setEnabled(oldState);
        }
        
        if ( !canSeeEventsFromOthers())
        {
            myEventsAction.setEnabled( false );
        }

      
    }
}
