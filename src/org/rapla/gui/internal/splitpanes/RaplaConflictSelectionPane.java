/*--------------------------------------------------------------------------* | Copyright (C) 2008  Christopher Kohlhaas                                 |
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.rapla.components.util.DateTools;
import org.rapla.entities.User;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.AppointmentBlock;
import org.rapla.facade.Conflict;
import org.rapla.facade.ModificationEvent;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.TreeFactory;
import org.rapla.gui.internal.common.CalendarSelectionModel;
import org.rapla.gui.internal.common.MultiCalendarView;
import org.rapla.gui.internal.view.TreeFactoryImpl;
import org.rapla.gui.toolkit.RaplaTree;
import org.rapla.gui.toolkit.RaplaWidget;


public class RaplaConflictSelectionPane extends RaplaGUIComponent implements RaplaWidget {
	public RaplaTree treeSelection = new RaplaTree();
	 protected final CalendarSelectionModel model;
	 MultiCalendarView view;
	 protected JPanel content = new JPanel();
	
    public RaplaConflictSelectionPane(RaplaContext context,final MultiCalendarView view, final CalendarSelectionModel model) throws RaplaException {
        super(context);
        this.model = model;
        this.view = view;
        updateTree();
        final JTree navTree = treeSelection.getTree();
        content.setLayout(new BorderLayout());

        content.add(treeSelection);
        // content.setPreferredSize(new Dimension(260,400));
        content.setBorder(BorderFactory.createRaisedBevelBorder());
        navTree.addTreeSelectionListener(new TreeSelectionListener() {

            boolean treeSelectionListenerEnabled = true;

            public void valueChanged(TreeSelectionEvent e) 
            {
            	 Object lastSelected = treeSelection.getInfoElement();
                 boolean lastClickedElementIsConflict = lastSelected instanceof Conflict;
                 if (lastClickedElementIsConflict) {
                     Date date = getFirstConflictDate((Conflict)lastSelected);
                     model.setSelectedDate(date);
                 }
                
                 if (treeSelectionListenerEnabled) {
                	TreePath treePath = e.getOldLeadSelectionPath();
                	treeSelectionListenerEnabled = false;
                	try {
                		// prevent from leaving the last visited node
                		navTree.setSelectionPath(treePath);
                	} finally {
                		treeSelectionListenerEnabled = true;
                	}
                }
                 try {
 					view.getSelectedCalendar().update();
 				} catch (RaplaException e1) {
 					getLogger().error("Can't switch to conflict dates.", e1);
 				}
            }
        });
        
    }

    public RaplaTree getTreeSelection() {
        return treeSelection;
    }

    protected CalendarSelectionModel getModel() {
        return model;
    }

    public void dataChanged(ModificationEvent evt) throws RaplaException {
        updateTree();
    }
    
    public JComponent getComponent() {
        return content;
    }
    
    final protected TreeFactory getTreeFactory() {
        return (TreeFactory) getService(TreeFactory.class);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.rapla.gui.internal.view.ITreeFactory#createClassifiableModel(org.rapla.entities.dynamictype.Classifiable[], org.rapla.entities.dynamictype.DynamicType[])
     */
    public void updateTree() throws RaplaException {

        JTree tree = treeSelection.getTree();
		tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(((TreeFactoryImpl) getTreeFactory()).createComplexRenderer());
        //if (tree)
        DefaultTreeModel treeModel = generateTree();
        try {
        	
            treeSelection.exchangeTreeModel(treeModel);
            tree.expandRow(0);
        } finally {
        }

    }
    
    protected DefaultTreeModel generateTree() throws RaplaException {
        boolean onlyOwn = model.isOnlyCurrentUserSelected();
        User conflictUser = onlyOwn ? getUser() : null;
        Allocatable[] selectedAllocatables = getModel().getSelectedAllocatables();
		HashSet<Allocatable> allocatables = new HashSet<Allocatable>( Arrays.asList(selectedAllocatables));
        DefaultTreeModel treeModel = ((TreeFactoryImpl) getTreeFactory()).createConflictModel(allocatables, conflictUser);
        
        
        
        return treeModel;
    }
    
    public Date getFirstConflictDate(Conflict conflict) {
        Appointment a1  =conflict.getAppointment1();
        Appointment a2  =conflict.getAppointment2();
        Date minEnd =  a1.getMaxEnd();
        if ( a1.getMaxEnd() != null && a2.getMaxEnd() != null && a2.getMaxEnd().before( a1.getMaxEnd())) {
            minEnd = a2.getMaxEnd();
        }
        Date maxStart = a1.getStart();
        if ( a2.getStart().after( a1.getStart())) {
            maxStart = a2.getStart();
        }
        // Jetzt berechnen wir fuer 2 Jahre
        if ( minEnd == null)
            minEnd = new Date(maxStart.getTime() + DateTools.MILLISECONDS_PER_WEEK * 100);

        List<AppointmentBlock> listA = new ArrayList<AppointmentBlock>();
        a1.createBlocks(maxStart, minEnd, listA );
        List<AppointmentBlock> listB = new ArrayList<AppointmentBlock>();
        a2.createBlocks( maxStart, minEnd, listB );
        for ( int i=0, j=0;i<listA.size() && j<listB.size();) {
            long s1 = listA.get( i).getStart();
            long s2 = listB.get( j).getStart();
            long e1 = listA.get( i).getEnd();
            long e2 = listB.get( j).getEnd();
            if ( s1< e2 && s2 < e1) {
                return new Date( Math.max( s1, s2));
            }
            if ( s1> s2)
               j++;
            else
               i++;
        }
        return null;
    }
    
    
   
}
