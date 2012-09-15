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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JToolBar;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.rapla.facade.ModificationEvent;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.internal.action.ShowConflictsAction;
import org.rapla.gui.internal.action.ShowSelectionAction;
import org.rapla.gui.internal.common.CalendarSelectionModel;
import org.rapla.gui.internal.common.InternMenus;
import org.rapla.gui.internal.common.MultiCalendarView;
import org.rapla.gui.internal.splitpanes.RaplaConflictSelectionPane;
import org.rapla.gui.internal.splitpanes.RaplaFiltrableSelectionPane;
import org.rapla.gui.internal.splitpanes.RaplaResourceSelectionPane;
import org.rapla.gui.toolkit.RaplaButton;
import org.rapla.gui.toolkit.RaplaMenu;
import org.rapla.gui.toolkit.RaplaWidget;

final public class CalendarEditor extends RaplaGUIComponent implements RaplaWidget {
    JSplitPane content = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    final private RaplaFiltrableSelectionPane resourceSelection;
    final private SavedCalendarView savedViews;
    final private RaplaConflictSelectionPane conflictsView;
    final public MultiCalendarView calendarContainer;
    
    final JToolBar minimized;
    final JPanel left;
    public CalendarEditor(RaplaContext context, CalendarSelectionModel model) throws RaplaException {
        super(context);
        final ChangeListener treeListener = new ChangeListener() {
            
            public void stateChanged(ChangeEvent e) {
                try {
					conflictsView.updateTree();
				} catch (RaplaException e1) {
					getLogger().error( e1.getMessage(), e1);
				}
            }
        };
        
        calendarContainer = new MultiCalendarView(getContext(), model);
        resourceSelection = new RaplaResourceSelectionPane(context, calendarContainer, model);
        resourceSelection.getTreeSelection().addChangeListener( treeListener);
        //selection.add(getI18n().getString("resources"), resourcesView.getComponent());
//        if (this.isAdmin()) {
//            adminView = new RaplaSelectionPane(context, calendarContainer, model)
//            {
//              protected MenuContext createMenuContext(Point p, Object obj) {
//                  final MenuContext menuContext = super.createMenuContext(p, obj);
//                  menuContext.put("adminpane", "true");
//                  return menuContext;
//              };  
//            };
//            adminView.getTreeSelection().addChangeListener( treeListener);
//            selection.add(getI18n().getString("admin"), adminView.getComponent());
//            selection.setSelectedComponent(adminView.getComponent());
//            content.setDividerLocation(320);
//        } else {
//        }
        
        
        conflictsView = new RaplaConflictSelectionPane(context, calendarContainer, model);
        left = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridheight = 1;
        c.gridx = 1;
        c.gridy = 1;
        c.weightx = 0;
        c.weighty = 0;
        c.anchor = GridBagConstraints.EAST;
        final JButton max  = new JButton();
        final JButton tree  = new JButton();
        minimized = new JToolBar(JToolBar.VERTICAL);
        minimized.setFloatable( false);
        minimized.add( max);
        minimized.add( tree);
        tree.setEnabled( false );
        
        max.setIcon( UIManager.getDefaults().getIcon("InternalFrame.maximizeIcon"));
        tree.setIcon( getIcon("icon.tree"));
        JButton min = new RaplaButton(RaplaButton.SMALL);
        final RaplaMenu viewMenu = (RaplaMenu)getService( InternMenus.VIEW_MENU_ROLE );
        ActionListener minmaxAction = new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				savedViews.closeFilter();
				int index = viewMenu.getIndexOfEntryWithId("show_selection");
				JMenuItem component = (JMenuItem)viewMenu.getMenuComponent( index);
				((ShowSelectionAction)component.getAction()).switchSelection( component);
			}
		};
		min.addActionListener( minmaxAction);
		max.addActionListener( minmaxAction);
        tree.addActionListener( minmaxAction);
		Icon icon = UIManager.getDefaults().getIcon("InternalFrame.minimizeIcon");
        min.setIcon( icon) ;
        //left.add(min, c);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
         c.gridy = 1;
        JPanel jp = new JPanel();
        jp.setLayout( new BorderLayout());
        
        savedViews = new SavedCalendarView(context, calendarContainer, resourceSelection,model);
        jp.add( savedViews.getComponent(), BorderLayout.CENTER );
        JToolBar mintb =new JToolBar();
        mintb.setFloatable( false);
       // mintb.add( min);
        min.setAlignmentY( JButton.TOP);
        jp.add( min, BorderLayout.EAST);
        left.add(jp, c);
        c.fill = GridBagConstraints.BOTH;
        c.gridy = 2;
        c.weightx = 1;
        c.weighty = 2.5;
        left.add(resourceSelection.getComponent(), c);
        c.weighty = 1.0;
        c.gridy = 3;
        left.add(conflictsView.getComponent(), c);
        
        //content.setLeftComponent(left);
        content.setRightComponent(calendarContainer.getComponent());
        updateViews();
        //content.getLeftComponent().setVisible( false);
    }

    private void showSelection(boolean showSelection) {
		if (  showSelection)
		{
			if ( content.getLeftComponent() != left )
			{
				content.setLeftComponent( left);
				content.setDividerSize( 5);
				content.setDividerLocation(285); 
			}
		}
		else
		{
	    	content.setLeftComponent( minimized);
			content.setDividerSize(0);
		}
	}


    
    public void dataChanged(ModificationEvent evt) throws RaplaException {
        resourceSelection.dataChanged(evt);
        calendarContainer.update();
//        if (adminView != null) {
//        	adminView.updateTree();
//        }
        savedViews.update();
        conflictsView.updateTree();
        updateViews();
    }

    private void updateViews() throws RaplaException {
        boolean showConflicts = getClientFacade().getPreferences().getEntryAsBoolean( ShowConflictsAction.CONFIG_ENTRY, true);
        boolean showSelection = getClientFacade().getPreferences().getEntryAsBoolean( ShowSelectionAction.CONFIG_ENTRY, true);
        conflictsView.getComponent().setVisible( showConflicts);
        showSelection( showSelection );
    }


	public void start() throws RaplaException {
        calendarContainer.getSelectedCalendar().scrollToStart();
    }

    public JComponent getComponent() {
        return content;
    }

}
