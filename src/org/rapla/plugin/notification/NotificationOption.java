/*--------------------------------------------------------------------------*
 | Copyright (C) 2006 Christopher Kohlhaas                                  |
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
package org.rapla.plugin.notification;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.apache.avalon.framework.configuration.ConfigurationException;
import org.rapla.components.layout.TableLayout;
import org.rapla.entities.configuration.Preferences;
import org.rapla.entities.configuration.RaplaMap;
import org.rapla.entities.domain.Allocatable;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.OptionPanel;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.TreeFactory;
import org.rapla.gui.toolkit.DialogUI;
import org.rapla.gui.toolkit.RaplaTree;

public class NotificationOption extends RaplaGUIComponent implements OptionPanel,ChangeListener {
    final static String ALLOCATIONLISTENERS_CONFIG = NotificationService.ALLOCATIONLISTENERS_CONFIG;
    final static String NOTIFY_IF_OWNER_CONFIG =  NotificationService.NOTIFY_IF_OWNER_CONFIG;
    boolean state = false;
    JPanel content= new JPanel();
    RaplaTree treeSelection;
    JPanel buttonPanel;
    JButton deleteButton;
    JButton addButton;
    JCheckBox notifyIfOwnerCheckBox;
    NotificationAction deleteAction;
    NotificationAction addAction;
    Preferences preferences;

    public NotificationOption(RaplaContext sm) throws RaplaException {
        super( sm);
        setChildBundleName( NotificationPlugin.RESOURCE_FILE);
    }

    public void create() throws RaplaException {
        treeSelection = new RaplaTree();
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        deleteButton = new JButton();
        addButton = new JButton();
        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);
        double[][] sizes = new double[][] {
            {5,TableLayout.FILL,5}
            ,{TableLayout.PREFERRED,5,TableLayout.PREFERRED,5,TableLayout.PREFERRED,5,TableLayout.FILL}
        };
        TableLayout tableLayout = new TableLayout(sizes);
        content.setLayout(tableLayout);
        content.add(new JLabel(getStringAsHTML("notification.option.description")), "1,0");
        notifyIfOwnerCheckBox = new JCheckBox();
        content.add(notifyIfOwnerCheckBox, "1,2");
        
        notifyIfOwnerCheckBox.setText(getStringAsHTML("notify_if_owner"));
        notifyIfOwnerCheckBox.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				try {
					updateNotifyIfOwner();
				} catch (Exception ex) {
					showException(ex, getComponent());
				}
			}
		});

        content.add(buttonPanel, "1,4");
        content.add(treeSelection,"1,6");

        deleteAction = new NotificationAction().setDelete();
        addAction = new NotificationAction().setAdd();
        deleteButton.setAction(deleteAction);
        addButton.setAction(addAction);
        treeSelection.addChangeListener(this);
    }

    public JComponent getComponent() {
        return content;
    }
    public String getName(Locale locale) {
        return getString("notification_options");
    }

    final private TreeFactory getTreeFactory() {
        return  getService(TreeFactory.class);
    }

    private void delete(List<?> selection)  throws RaplaException,ConfigurationException {
        RaplaMap<Allocatable> allocatableList = preferences.getEntry(ALLOCATIONLISTENERS_CONFIG);
        if ( allocatableList == null)
            return;
        Set<Object> set= new HashSet<Object>();
        for (Iterator<?> it = allocatableList.values().iterator();it.hasNext();) {
            Object allocatable = it.next();
            if ( !selection.contains( allocatable ) ) {
                set.add( allocatable);
            }
        }
        preferences.putEntry( ALLOCATIONLISTENERS_CONFIG, getModification().newRaplaMap( set) );
        update();
    }


    public void show() throws RaplaException {
        if (treeSelection== null)
            create();
        update();
    }

    private void update() throws RaplaException {
        RaplaMap<Allocatable> allocatableList =  preferences.getEntry(ALLOCATIONLISTENERS_CONFIG);

        boolean notify = preferences.getEntryAsBoolean( NOTIFY_IF_OWNER_CONFIG, false);
        notifyIfOwnerCheckBox.setEnabled( false );
        notifyIfOwnerCheckBox.setSelected(notify);
        notifyIfOwnerCheckBox.setEnabled( true );
        
        treeSelection.getTree().setCellRenderer(getTreeFactory().createRenderer());
        Allocatable[] allocatables = Allocatable.ALLOCATABLE_ARRAY;
        if ( allocatableList!= null) {
            Set<Allocatable> set = new HashSet<Allocatable>( allocatableList.values());
            allocatables = set.toArray( Allocatable.ALLOCATABLE_ARRAY);
        }

        treeSelection.exchangeTreeModel(getTreeFactory().createClassifiableModel(allocatables));
    }



    private void updateNotifyIfOwner() throws RaplaException, ConfigurationException {
        preferences.putEntry( NOTIFY_IF_OWNER_CONFIG,  String.valueOf(notifyIfOwnerCheckBox.isSelected()));
    }

    private void add(Allocatable allocatable) throws ConfigurationException, RaplaException {
        RaplaMap<Allocatable> raplaEntityList = preferences.getEntry( ALLOCATIONLISTENERS_CONFIG );
        Set<Object> list;
        if ( raplaEntityList != null ){
            list = new HashSet<Object>(raplaEntityList.values());
        } else {
            list = new HashSet<Object>();
        }
        list.add( allocatable );
        getLogger().info("Subscription to notification service: " + allocatable.getName(getLocale()) +" by " + getUser().getUsername());
        preferences.putEntry( ALLOCATIONLISTENERS_CONFIG ,getModification().newRaplaMap( list ));
        update();
    }


    public void setPreferences(Preferences preferences) {
        this.preferences = preferences;
    }

    public void commit() {
    }


    public void stateChanged(ChangeEvent e) {
        deleteAction.update();
        //addAction.update();
    }

    class NotificationAction extends AbstractAction {
        private static final long serialVersionUID = 1L;
        
        List<Allocatable> selection;
        int ADD = 1;
        int DELETE = 2;
        int type;

        NotificationAction setDelete() {
            putValue(NAME,getString("delete"));
            putValue(SMALL_ICON,getIcon("icon.delete"));
            setEnabled(false);
            type = DELETE;
            return this;
        }

        NotificationAction setAdd() {
            putValue(NAME,getString("add"));
            putValue(SMALL_ICON,getIcon("icon.new"));
            type = ADD;
            return this;
        }

      

        void update() {
            selection = getSelected();
            setEnabled(selection.size() > 0);
        }

		protected List<Allocatable> getSelected() {
			List<Allocatable> allocatables = new ArrayList<Allocatable>();
			for ( Object obj:treeSelection.getSelectedElements())
			{
				allocatables.add((Allocatable) obj);
			}
			return allocatables;
		}

        public void actionPerformed(ActionEvent evt) {
            try {
                if (type == DELETE) {
                    delete(selection);
                } else if (type == ADD) {
                    showAddDialog();
                }
            } catch (Exception ex) {
                showException(ex,getComponent());
            }
        }

        private void showAddDialog() throws RaplaException, ConfigurationException {
            final DialogUI dialog;
            RaplaTree treeSelection = new RaplaTree();
            treeSelection.setMultiSelect(true);
            treeSelection.getTree().setCellRenderer(getTreeFactory().createRenderer());

            treeSelection.exchangeTreeModel(getTreeFactory().createClassifiableModel(getQuery().getAllocatables()));
            treeSelection.setMinimumSize(new java.awt.Dimension(300, 200));
            treeSelection.setPreferredSize(new java.awt.Dimension(400, 260));
            dialog = DialogUI.create(
                    getContext()
                    ,getComponent()
                    ,true
                    ,treeSelection
                    ,new String[] { getString("add"),getString("cancel")});
            dialog.setTitle(getString("subscribe_notification") );
            dialog.getButton(0).setEnabled(false);
            
            final JTree tree = treeSelection.getTree();
            tree.addMouseListener(new MouseAdapter() {
                // End dialog when a leaf is double clicked
                public void mousePressed(MouseEvent e) {
                    TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
                    if (selPath != null && e.getClickCount() == 2) {
                        final Object lastPathComponent = selPath.getLastPathComponent();
                        if (((TreeNode) lastPathComponent).isLeaf() ) {
                            dialog.getButton(0).doClick();
                            return;
                        }
                    }
                    else
                    	if (selPath != null && e.getClickCount() == 1) {
	                        final Object lastPathComponent = selPath.getLastPathComponent();
	                        if (((TreeNode) lastPathComponent).isLeaf() ) {
	                            dialog.getButton(0).setEnabled(true);
	                            return;
	                        }
                    	}
	                tree.removeSelectionPath(selPath);
                }
            });
            
            dialog.start(); 
            if (dialog.getSelectedIndex() == 0) {
                Iterator<Allocatable> it = getSelected().iterator();
                while (it.hasNext()) {
                    add( it.next());
                }
            }
        }
    }
    
}
