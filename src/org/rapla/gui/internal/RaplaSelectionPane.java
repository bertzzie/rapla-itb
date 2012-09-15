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

package org.rapla.gui.internal;

import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.DefaultTreeModel;

import org.rapla.components.layout.TableLayout;
import org.rapla.entities.RaplaObject;
import org.rapla.entities.RaplaType;
import org.rapla.entities.User;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Period;
import org.rapla.entities.dynamictype.ClassificationFilter;
import org.rapla.facade.ModificationEvent;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.MenuContext;
import org.rapla.gui.MenuFactory;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.TreeFactory;
import org.rapla.gui.internal.action.RaplaObjectAction;
import org.rapla.gui.internal.common.CalendarSelectionModel;
import org.rapla.gui.internal.common.InternMenus;
import org.rapla.gui.internal.common.MultiCalendarView;
import org.rapla.gui.internal.view.TreeFactoryImpl;
import org.rapla.gui.toolkit.PopupEvent;
import org.rapla.gui.toolkit.PopupListener;
import org.rapla.gui.toolkit.RaplaMenu;
import org.rapla.gui.toolkit.RaplaPopupMenu;
import org.rapla.gui.toolkit.RaplaTree;
import org.rapla.gui.toolkit.RaplaWidget;

public class RaplaSelectionPane extends RaplaGUIComponent implements RaplaWidget {
    protected JPanel content = new JPanel();
    public RaplaTree treeSelection = new RaplaTree();
    TableLayout tableLayout;
    protected JPanel buttonsPanel = new JPanel();

    protected final CalendarSelectionModel model;
    MultiCalendarView view;
    Listener listener = new Listener();
  
    
	public RaplaSelectionPane(RaplaContext context, MultiCalendarView view, CalendarSelectionModel model) throws RaplaException {
        super(context);

        this.model = model;
        this.view = view;
        /*double[][] sizes = new double[][] { { TableLayout.FILL }, { TableLayout.PREFERRED, TableLayout.PREFERRED, TableLayout.FILL } };
        tableLayout = new TableLayout(sizes);*/
        content.setLayout(new BorderLayout());

        content.add(treeSelection);
        // content.setPreferredSize(new Dimension(260,400));
        content.setBorder(BorderFactory.createRaisedBevelBorder());
        
        content.add(buttonsPanel, BorderLayout.NORTH);
        
        treeSelection.setToolTipRenderer(getTreeFactory().createTreeToolTipRenderer());
        treeSelection.setMultiSelect(true);

        updateTree();
        updateSelection();
        treeSelection.addChangeListener(listener);
        treeSelection.addPopupListener(listener);
        treeSelection.addDoubleclickListeners(listener);
        javax.swing.ToolTipManager.sharedInstance().registerComponent(treeSelection.getTree());

        updateMenu();

      
        /*JButton test = new JButton("Show Selection");
        test.addActionListener(new ActionListener() {

            
            public void actionPerformed(ActionEvent e) {
                try {
                    applyFilter();
                } catch (Exception ex) {
                    showException(ex, getComponent());
                }
            }
            
        });
        buttonsPanel.add(test);*/
    }
	
	public RaplaTree getTreeSelection() {
        return treeSelection;
    }

    protected CalendarSelectionModel getModel() {
        return model;
    }

    public void dataChanged(ModificationEvent evt) throws RaplaException {
        updateTree();
        updateMenu();
    }

    final protected TreeFactory getTreeFactory() {
        return (TreeFactory) getService(TreeFactory.class);
    }

    boolean treeListenersEnabled = true;

    /*
     * (non-Javadoc)
     * 
     * @see org.rapla.gui.internal.view.ITreeFactory#createClassifiableModel(org.rapla.entities.dynamictype.Classifiable[], org.rapla.entities.dynamictype.DynamicType[])
     */
    protected void updateTree() throws RaplaException {

        treeSelection.getTree().setRootVisible(false);
        treeSelection.getTree().setShowsRootHandles(true);
        treeSelection.getTree().setCellRenderer(((TreeFactoryImpl) getTreeFactory()).createComplexRenderer());

        DefaultTreeModel treeModel = generateTree();
        try {
            treeListenersEnabled = false;
            treeSelection.exchangeTreeModel(treeModel);
            updateSelection();
        } finally {
            treeListenersEnabled = true;
        }

    }

    protected DefaultTreeModel generateTree() throws RaplaException {
        ClassificationFilter[] filter = getModel().getAllocatableFilter();
        DefaultTreeModel treeModel = ((TreeFactoryImpl) getTreeFactory()).createModel(filter, null, null);
        return treeModel;
    }

    protected void updateSelection() throws RaplaException {
        Collection<Object> selectedObjects = new ArrayList<Object>(getModel().getSelectedObjects());
        if (model.isOnlyCurrentUserSelected()) {
            selectedObjects.remove(getUser());
        }
        treeSelection.select(selectedObjects);
    }

    public JComponent getComponent() {
        return content;
    }

    
    protected MenuContext createMenuContext(Point p, Object obj) {
        MenuContext menuContext = new MenuContext(getContext(), obj, getComponent(), p);
        return menuContext;
    }
    
    protected void showTreePopup(PopupEvent evt) {
        try {

            Point p = evt.getPoint();
            Object obj = evt.getSelectedObject();
            List<?> list = treeSelection.getSelectedElements();

            MenuContext menuContext = createMenuContext(p, obj);
            menuContext.setSelectedObjects(list);
            

            RaplaPopupMenu menu = new RaplaPopupMenu();

            RaplaMenu newMenu = new RaplaMenu("new");
            newMenu.setText(getString("new"));
            ((MenuFactoryImpl) getMenuFactory()).addNew(newMenu, menuContext, null);

            getMenuFactory().addObjectMenu(menu, menuContext, "EDIT_BEGIN");
            menu.insertAfterId(newMenu, "EDIT_BEGIN");

            JComponent component = (JComponent) evt.getSource();

            menu.show(component, p.x, p.y);
        } catch (RaplaException ex) {
            showException(ex, getComponent());
        }
    }
    
    class Listener implements PopupListener, ChangeListener, ActionListener {

        public void showPopup(PopupEvent evt) {
            showTreePopup(evt);
        }
     
        public void actionPerformed(ActionEvent evt) {
            try {     	
                if (!canUserAllocateSomething( getUser()) )
                    return;
                if (!isRegisterer() && !isAdmin())
                	return;
                Object focusedObject = evt.getSource();
                if ( focusedObject == null || !(focusedObject instanceof RaplaObject))
                	return;
                // System.out.println(focusedObject.toString());
                RaplaType type = ((RaplaObject) focusedObject).getRaplaType();
                if (    type.equals(User.TYPE) 
                     || type.equals(Allocatable.TYPE)
                     || type.equals(Period.TYPE) 
                   )
                {
                	
                    RaplaObjectAction editAction = new RaplaObjectAction( getContext(), getComponent(),null);
                    if (editAction.canModify( focusedObject))
                    {
                        editAction.setEdit((RaplaObject)focusedObject);
                        editAction.actionPerformed(null);
                    }
                }
            } catch (RaplaException ex) {
                showException (ex,getComponent());
            }
        }

        public void stateChanged(ChangeEvent evt) {
            if (!treeListenersEnabled) {
                return;
            }
            try {
                updateChange();
            } catch (Exception ex) {
                showException(ex, getComponent());
            }
        }


    }

    public void updateChange() throws RaplaException {
        getSelectedObjects();
        updateMenu();
        applyFilter();
    }
    
    public void applyFilter() throws RaplaException {
        view.getSelectedCalendar().update();
    }

    protected HashSet<?> getSelectedObjects() throws RaplaException {
        HashSet<Object> elements = new HashSet<Object>(treeSelection.getSelectedElements());

        getModel().setSelectedObjects(elements);
        return elements;
    }
    
    protected void updateMenu() throws RaplaException {
        RaplaMenu editMenu = (RaplaMenu) getService(InternMenus.EDIT_MENU_ROLE);
        RaplaMenu newMenu = (RaplaMenu) getService(InternMenus.NEW_MENU_ROLE);

        editMenu.removeAllBetween("EDIT_BEGIN", "EDIT_END");
        newMenu.removeAll();

        List<?> list = treeSelection.getSelectedElements();
        Object focusedObject = null;
        if (list.size() == 1) {
            focusedObject = treeSelection.getSelectedElement();
        }

        MenuContext menuContext = createMenuContext( null,  focusedObject);
        menuContext.setSelectedObjects(list);

        getMenuFactory().addObjectMenu(editMenu, menuContext, "EDIT_BEGIN");
        ((MenuFactoryImpl) getMenuFactory()).addNew(newMenu, menuContext, null);

        newMenu.setEnabled(newMenu.getMenuComponentCount() > 0 && canUserAllocateSomething(getUser()));
        editMenu.setEnabled(canUserAllocateSomething(getUser()));
    }

    public MenuFactory getMenuFactory() {
        return (MenuFactory) getService(MenuFactory.ROLE);
    }

}
