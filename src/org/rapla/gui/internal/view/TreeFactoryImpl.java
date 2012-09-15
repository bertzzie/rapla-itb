/*--------------------------------------------------------------------------*
 | Copyright (C) 2006 Christopher Kohlhaas, Bettina Lademann                |
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
package org.rapla.gui.internal.view;

import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreeNode;

import org.rapla.components.util.Assert;
import org.rapla.components.util.InverseComparator;
import org.rapla.entities.Category;
import org.rapla.entities.Named;
import org.rapla.entities.NamedComparator;
import org.rapla.entities.RaplaType;
import org.rapla.entities.User;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Period;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.domain.ReservationStartComparator;
import org.rapla.entities.dynamictype.Classifiable;
import org.rapla.entities.dynamictype.Classification;
import org.rapla.entities.dynamictype.ClassificationFilter;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.dynamictype.DynamicTypeAnnotations;
import org.rapla.facade.Conflict;
import org.rapla.facade.QueryModule;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.TreeFactory;
import org.rapla.gui.internal.common.CalendarSelectionModel;
import org.rapla.gui.toolkit.RecursiveNode;
import org.rapla.gui.toolkit.TreeToolTipRenderer;

public class TreeFactoryImpl extends RaplaGUIComponent implements TreeFactory {
    public TreeFactoryImpl(RaplaContext sm) throws RaplaException {
        super(sm);
        this.defaultIcon = getIcon("icon.tree.default");
    }

    Icon defaultIcon;

    public TreeModel createClassifiableModel(Classifiable[] classifiables) throws RaplaException {
        return createClassifiableModel(classifiables, null);
    }

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public DefaultTreeModel createClassifiableModel(Classifiable[] classifiables, final DynamicType[] dynamicTypes) throws RaplaException {
        boolean addOnlyTypesWithVisibleChildren = false;
        List<DynamicType> typeList = new ArrayList<DynamicType>();
        if (dynamicTypes == null || dynamicTypes.length == 0) {
            final QueryModule query = getQuery();
            typeList.addAll(Arrays.asList(query.getDynamicTypes(DynamicTypeAnnotations.VALUE_RESOURCE_CLASSIFICATION)));
            typeList.addAll(Arrays.asList(query.getDynamicTypes(DynamicTypeAnnotations.VALUE_PERSON_CLASSIFICATION)));
            typeList.addAll(Arrays.asList(query.getDynamicTypes(DynamicTypeAnnotations.VALUE_RESERVATION_CLASSIFICATION)));
            addOnlyTypesWithVisibleChildren = true;
        }
        else
        {
            for (DynamicType type: dynamicTypes) {
                typeList.add(type);
            }   
        }
        Map<DynamicType,TreeNode> nodeMap = new HashMap<DynamicType,TreeNode>();
        for (DynamicType type: typeList) {
            TreeNode node = new NamedNode(type);
            nodeMap.put(type, node);
        }
        
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("ROOT");
        Comparator<Classifiable> comp = null;
        {
	        if (classifiables.length > 0 && classifiables[0] instanceof Reservation) {
	            comp = new InverseComparator(new ReservationStartComparator(getLocale()));
	        } else {
	            comp = new NamedComparator(getLocale());
	        }
        }
        Set<Classifiable> sortedClassifiable = new TreeSet<Classifiable>(comp);
        sortedClassifiable.addAll(Arrays.asList(classifiables));
        for (Iterator<Classifiable> it = sortedClassifiable.iterator(); it.hasNext();) {
            Classifiable classifiable =  it.next();
            Classification classification = classifiable.getClassification();
            NamedNode childNode = new NamedNode((Named) classifiable);
            DynamicType type = classification.getType();
            Assert.notNull(type);
            DefaultMutableTreeNode typeNode = (DefaultMutableTreeNode) nodeMap.get(type);
            Assert.notNull(typeNode);
            typeNode.add(childNode);

        }
        int count = 0;
        for (DynamicType type: typeList) {
            DefaultMutableTreeNode typeNode = (DefaultMutableTreeNode) nodeMap.get(type);
            // Only add typeNodes with children
            if (!typeNode.isLeaf() || !addOnlyTypesWithVisibleChildren)
                root.insert(typeNode, count++);
        }
        return new DefaultTreeModel(root);
    }

    public TreeCellRenderer createComplexRenderer() {
        return new ComplexTreeCellRenderer();
    }

    private boolean isInFilter(ClassificationFilter[] filter, Classifiable classifiable) {
        if (filter == null)
            return true;
        for (int i = 0; i < filter.length; i++) {
            if (filter[i].matches(classifiable.getClassification())) {
                return true;
            }
        }
        return false;
    }

    private boolean isInFilter(ClassificationFilter[] filter, DynamicType type) {
        if (filter == null)
            return true;
        for (int i = 0; i < filter.length; i++) {
            if (filter[i].getType().equals(type)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasRulesFor(ClassificationFilter[] filter, DynamicType type) {
        if (filter == null)
            return false;
        for (int i = 0; i < filter.length; i++) {
            if (filter[i].getType().equals(type) && filter[i].ruleSize() > 0) {
                return true;
            }
        }
        return false;
    }

    private Collection<Conflict> getConflicts(Collection<Allocatable> allocatables, User user) throws RaplaException {

        List<Conflict> result = new ArrayList<Conflict>();
        Conflict[] conflicts = getQuery().getConflicts(allocatables,getQuery().today());
        for (int i = 0; i < conflicts.length; i++) {
            Conflict conflict = conflicts[i];
            if (!allocatables.contains( conflict.getAllocatable())) {
                continue;
            }
//            if (!isInFilter(reservationFilter, conflict.getReservation1()) && !isInFilter(reservationFilter, conflict.getReservation2())) {
//                continue;
//            }
            if (user != null && !user.equals(conflict.getReservation1().getOwner()) && !user.equals(conflict.getReservation1().getOwner())) {
                continue;
            }
            result.add(conflict);
        }
        return result;
    }

    /**
     * Returns the Resources root
     * 
     * @param filter
     * @param selectedUser
     * @return
     * @throws RaplaException
     */
    public TypeNode createResourcesModel(ClassificationFilter[] filter) throws RaplaException {
        TypeNode treeNode = new TypeNode(Allocatable.TYPE, CalendarSelectionModel.ALLOCATABLES_ROOT, getString("resources"));
        Map<DynamicType,MutableTreeNode> nodeMap = new HashMap<DynamicType, MutableTreeNode>();

        boolean resourcesFiltered = false;

        DynamicType[] types = getQuery().getDynamicTypes(DynamicTypeAnnotations.VALUE_RESOURCE_CLASSIFICATION);
        for (int i = 0; i < types.length; i++) {
            DynamicType type = types[i];
            if (hasRulesFor(filter, type)) {
                resourcesFiltered = true;
            }
            if (!isInFilter(filter, type)) {
                resourcesFiltered = true;
                continue;
            }

            NamedNode node = new NamedNode(type);
            treeNode.add(node);
            nodeMap.put(type, node);
        }

        // creates typ folders
        types = getQuery().getDynamicTypes(DynamicTypeAnnotations.VALUE_PERSON_CLASSIFICATION);
        for (int i = 0; i < types.length; i++) {
            DynamicType type = types[i];
            if (hasRulesFor(filter, type)) {
                resourcesFiltered = true;
            }
            if (!isInFilter(filter, type)) {
                resourcesFiltered = true;
                continue;
            }

            NamedNode node = new NamedNode(type);
            treeNode.add(node);
            nodeMap.put(type, node);
        }

        treeNode.setFiltered(resourcesFiltered);

        // adds elements to typ folders
        Allocatable[] allocatables = getQuery().getAllocatables();
        for (Allocatable classifiable: sorted(allocatables)) {
            if (!isInFilter(filter, classifiable)) {
                continue;
            }
            Classification classification = classifiable.getClassification();
            NamedNode childNode = new NamedNode((Named) classifiable);
            DynamicType type = classification.getType();
            Assert.notNull(type);
            DefaultMutableTreeNode typeNode = (DefaultMutableTreeNode) nodeMap.get(type);
            Assert.notNull(typeNode);
            typeNode.add(childNode);
        }
        
        for (Map.Entry<DynamicType, MutableTreeNode> entry: nodeMap.entrySet())
        {
        	MutableTreeNode value = entry.getValue();
        	if  (value.getChildCount() == 0 && (!isAdmin() || !isRegisterer()))
        	{
        		treeNode.remove( value);
        	}
        }
        return treeNode;
    }

    private <T extends Named> Collection<T> sorted(T[] allocatables) {
        TreeSet<T> sortedList = new TreeSet<T>(new NamedComparator<T>(getLocale()));
        sortedList.addAll(Arrays.asList(allocatables));
        return sortedList;
    }

    public TypeNode createReservationsModel() throws RaplaException {
        TypeNode treeNode = new TypeNode(Reservation.TYPE, getString("reservation_type"));

        // creates typ folders
        DynamicType[] types = getQuery().getDynamicTypes(DynamicTypeAnnotations.VALUE_RESERVATION_CLASSIFICATION);
        for (int i = 0; i < types.length; i++) {
            DynamicType type = types[i];
            
            
            NamedNode node = new NamedNode(type);
            treeNode.add(node);
        }
        treeNode.setFiltered(false);
        return treeNode;
    }

    public DefaultTreeModel createModel(ClassificationFilter[] filter, User selectedUser, RaplaType classificationType) throws RaplaException {
        // Resources and Persons
        // Add the resource types
        // Add the resources
        // Add the person types
        // Add the persons
        // Eventtypes
        // Add the event types
        // Conflicts (0)
        // Add the conflicts
        // Users
        // Add the users
        // Categories (the root category)
        // Add the periods

        DefaultMutableTreeNode root = new DefaultMutableTreeNode("ROOT");

        if (classificationType != null) {
            if (classificationType.equals(Allocatable.TYPE)) {
                TypeNode resourceRoot = createResourcesModel(filter);
                root.add(resourceRoot);
                
            } 
        } 
        
        else 
         {
            TypeNode resourceRoot = createResourcesModel(filter);
            root.add(resourceRoot);
            if (isAdmin()) {

                DefaultMutableTreeNode userRoot = new TypeNode(User.TYPE, getString("users"));
                User[] userList = getQuery().getUsers();
                for (final User user: userList) {
                    DefaultMutableTreeNode node = new DefaultMutableTreeNode();
                    node.setUserObject( user);
                    userRoot.add(node);
                }
                root.add(userRoot);

        
                TypeNode reservationsRoot = createReservationsModel();
                root.add(reservationsRoot);
                
                CategoryNode categoryRoot = new CategoryNode(getI18n().getLocale(), null, getQuery().getSuperCategory());
                root.add(categoryRoot);
    
                // set category root name
                getQuery().getSuperCategory().getName().setReadOnly(false);
                getQuery().getSuperCategory().getName().setName(getI18n().getLang(), getString("categories"));
                getQuery().getSuperCategory().getName().setReadOnly(true);
    
               
                
    
                DefaultMutableTreeNode periodRoot = new TypeNode(Period.TYPE, getString("periods"));
                Period[] periodList = getQuery().getPeriods();
                for (final Period period: sorted(periodList)) {
                    NamedNode node = new NamedNode(period);
                    periodRoot.add(node);
                }
                root.add(periodRoot);
            }
        }
        return new DefaultTreeModel(root);
    }
    
    public DefaultTreeModel createConflictModel(Collection<Allocatable> allocatables, User selectedUser) throws RaplaException {

        Collection<Conflict> conflicts = getConflicts(allocatables, selectedUser);
        String conflict_number = allocatables.size() >0 ? new Integer(conflicts.size()).toString() : getString("nothing_selected") ;
		String conflictText = getI18n().format("conflictUC", conflict_number);
		DefaultMutableTreeNode conflictRoot = new TypeNode(Conflict.TYPE, conflictText);
        for (Iterator<Conflict> it = conflicts.iterator(); it.hasNext();) {
            conflictRoot.add(new NamedNode( it.next()));
        }
        return new DefaultTreeModel(conflictRoot);
    }

    class TypeNode extends DefaultMutableTreeNode {
        private static final long serialVersionUID = 1L;

        boolean filtered;
        RaplaType type;
        String title;

        TypeNode(RaplaType type, Object userObject, String title) {
            this.type = type;
            this.title = title;
            setUserObject(userObject);
        }

        TypeNode(RaplaType type, Object userObject) {
            this(type, userObject, null);
        }

        public RaplaType getType() {
            return type;
        }

        public boolean isFiltered() {
            return filtered;
        }

        public void setFiltered(boolean filtered) {
            this.filtered = filtered;
        }

        public Object getTitle() {
            if (title != null) {
                return title;
            } else {
                return userObject.toString();
            }
        }

    }

    public DefaultMutableTreeNode newNamedNode(Named element) {
        return new NamedNode(element);
    }

    public TreeModel createModel(Category category) throws RaplaException {
        return new DefaultTreeModel(new CategoryNode(getI18n().getLocale(), null, category));
    }

    public TreeModel createModelFlat(Named[] element) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("");
        for (int i = 0; i < element.length; i++) {
            root.add(new NamedNode(element[i]));
        }
        return new DefaultTreeModel(root);
    }

    public TreeToolTipRenderer createTreeToolTipRenderer() {
        return new RaplaTreeToolTipRenderer();
    }

    public TreeCellRenderer createRenderer() {
        return new ComplexTreeCellRenderer();
    }

    static public class CategoryNode extends RecursiveNode implements MutableTreeNode {
        Locale locale;

        public CategoryNode(Locale locale, TreeNode parent, Category category) {
            super(parent, category);
            this.locale = locale;
        }

        protected Category getCategory() {
            return (Category) getUserObject();
        }

        
        protected Object[] getChildObjects() {
            return getCategory().getCategories();
        }

        
        protected RecursiveNode createChildNode(Object userObject) {
            return new CategoryNode(locale, this, (Category) userObject);
        }

        
        public String toString() {
            return getCategory().getName(locale);
        }

        public void insert(MutableTreeNode child, int index) {
        }

        public void remove(int index) {
        }

        public void remove(MutableTreeNode node) {
        }

        public void setUserObject(Object object) {
        }

        public void removeFromParent() {
            parent = null;
        }

        public void setParent(MutableTreeNode newParent) {
            parent = newParent;

        }
    }

    public class NamedNode extends DefaultMutableTreeNode {
        private static final long serialVersionUID = 1L;

        NamedNode(Named obj) {
            super(obj);
        }

        
        public String toString() {
            Named obj = (Named) getUserObject();
            if (obj != null) {
                return obj.getName(getI18n().getLocale());
            } else {
                return super.toString();
            }
        }
    };

    // TODO this class is a bit of Hack
    class ComplexTreeCellRenderer extends DefaultTreeCellRenderer {
        private static final long serialVersionUID = 1L;

        Icon personIcon;
        Icon folderClosedIcon;
        Icon folderOpenIcon;
        Font normalFont;
        Font bigFont;
        Border nonIconBorder = BorderFactory.createEmptyBorder(1, 0, 1, 0);
        Border conflictBorder = BorderFactory.createEmptyBorder(2, 0, 2, 0);

        public ComplexTreeCellRenderer() {
            personIcon = TreeFactoryImpl.this.getIcon("icon.tree.persons");
            // folderClosedIcon = UIManager.getIcon("Tree.closedIcon");
            // folderOpenIcon = UIManager.getIcon("Tree.openIcon");
            folderClosedIcon = getI18n().getIcon("icon.folder");
            folderOpenIcon = getI18n().getIcon("icon.folder");
            normalFont = UIManager.getFont("Tree.font");
            bigFont = normalFont.deriveFont(Font.BOLD, (float) (normalFont.getSize() * 1.2));
            setLeafIcon(defaultIcon);

        }

        public void setLeaf(Object object) {
            Icon icon = null;
            if (object instanceof Allocatable) {
                if (((Allocatable) object).isPerson()) {
                    icon = personIcon;
                } else {
                    icon = defaultIcon;
                }

            } else if (object instanceof DynamicType) {
                DynamicType type = (DynamicType) object;
                String classificationType = type.getAnnotation(DynamicTypeAnnotations.KEY_CLASSIFICATION_TYPE);
                if (DynamicTypeAnnotations.VALUE_RESERVATION_CLASSIFICATION.equals(classificationType)) {
                    setBorder(conflictBorder);
                } else {
                    icon = folderClosedIcon;
                }
            }
            if (icon == null) {
                setBorder(nonIconBorder);
            }
            setLeafIcon(icon);
        }

        
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            setBorder(null);
            setFont(normalFont);
            setClosedIcon(folderClosedIcon);
            setOpenIcon(folderOpenIcon);
            if (value != null && value instanceof TypeNode) {
                TypeNode typeNode = (TypeNode) value;
                Icon bigFolderIcon;
                if (typeNode.getType().equals(User.TYPE)) {
                    bigFolderIcon = getI18n().getIcon("icon.big_folder_users");
                } else if (typeNode.getType().equals(Period.TYPE)) {
                    bigFolderIcon = getI18n().getIcon("icon.big_folder_periods");
                } else if (typeNode.getType().equals(Reservation.TYPE)) {
                    if (typeNode.isFiltered()) {
                        bigFolderIcon = getI18n().getIcon("icon.big_folder_events_filtered");
                    } else {
                        bigFolderIcon = getI18n().getIcon("icon.big_folder_events");
                    }
                } else if (typeNode.getType().equals(Conflict.TYPE)) {
                    bigFolderIcon = getI18n().getIcon("icon.big_folder_conflicts");
                } else {
                    if (typeNode.isFiltered()) {
                        bigFolderIcon = getI18n().getIcon("icon.big_folder_resources_filtered");
                    } else {
                        bigFolderIcon = getI18n().getIcon("icon.big_folder_resources");
                    }
                }
                setClosedIcon(bigFolderIcon);
                setOpenIcon(bigFolderIcon);
                setLeafIcon(bigFolderIcon);
                setFont(bigFont);
                value = typeNode.getTitle();
            } else if (value instanceof CategoryNode) {
                Category category = ((CategoryNode) value).getCategory();
                if (category.getParent() == null) {
                    setClosedIcon(getI18n().getIcon("icon.big_folder_categories"));
                    setOpenIcon(getI18n().getIcon("icon.big_folder_categories"));
                    setFont(bigFont);
                } else {
                    boolean hasChildren = category.getCategories().length > 0;
                    if (!hasChildren) {
                        setClosedIcon(null);
                        setOpenIcon(null);
                        setLeafIcon(null);
                        setBorder(nonIconBorder);
                    }
                }
            } else {
                Object nodeInfo = getTheUserObject(value);
                if (nodeInfo instanceof Conflict) {
                    Conflict conflict = (Conflict) nodeInfo;
                    String text = getInfoFactory().getToolTip(conflict);
                    value = text;
                    setBorder(conflictBorder);
                    setLeafIcon(null);
                } else {
                    setClosedIcon(folderClosedIcon);
                    setOpenIcon(folderOpenIcon);
                    if (leaf) {
                        setLeaf(nodeInfo);
                    }
                }
            }
            Component result = super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            return result;
        }

    }

    private static Object getTheUserObject(Object node) {
        if (node instanceof DefaultMutableTreeNode)
            return ((DefaultMutableTreeNode) node).getUserObject();
        if (node instanceof RecursiveNode)
            return ((RecursiveNode) node).getUserObject();
        return node;
    }

    class RaplaTreeToolTipRenderer implements TreeToolTipRenderer {
        public String getToolTipText(JTree tree, int row) {
            Object node = tree.getPathForRow(row).getLastPathComponent();
            Object value = getTheUserObject(node);
            if (value instanceof Conflict) {
                return null;
            }
            return getInfoFactory().getToolTip(value);
        }
    }

}
