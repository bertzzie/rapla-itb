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
package org.rapla.gui.internal.edit;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.ItemSelectable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.rapla.components.layout.TableLayout;
import org.rapla.components.util.Assert;
import org.rapla.entities.Category;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.dynamictype.AttributeType;
import org.rapla.entities.dynamictype.ClassificationFilter;
import org.rapla.entities.dynamictype.ClassificationFilterRule;
import org.rapla.entities.dynamictype.ConstraintIds;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.dynamictype.DynamicTypeAnnotations;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.ClassifiableFilter;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.internal.common.CalendarSelectionModel;
import org.rapla.gui.internal.common.NamedListCellRenderer;
import org.rapla.gui.toolkit.RaplaButton;
import org.rapla.gui.toolkit.RaplaWidget;


public class ClassifiableFilterEdit extends RaplaGUIComponent
    implements
        ActionListener
        ,RaplaWidget
{
    JPanel content = new JPanel();
    JScrollPane scrollPane;
    JCheckBox[] checkBoxes;
    ClassificationEdit filterEdit[];
    DynamicType[] types;
    boolean isResourceOnly;

    ArrayList<ChangeListener> listenerList = new ArrayList<ChangeListener>();
    RaplaButton resetButton;
    
    public ClassifiableFilterEdit(RaplaContext context, boolean isResourceOnly) throws RaplaException {
        super( context);
        content.setBackground(UIManager.getColor("List.background"));
        scrollPane = new JScrollPane(content
                                     ,JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
                                     ,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
                                     );
        scrollPane.setPreferredSize(new Dimension(590,270));
        this.isResourceOnly = isResourceOnly;
        content.setBorder( BorderFactory.createEmptyBorder(5,5,5,5));

    }

    public JComponent getClassificationTitle(String classificationType) {
        JLabel title = new JLabel( classificationType );
        title.setFont( title.getFont().deriveFont( Font.BOLD ));
        title.setText( getString( classificationType) + ":" );
        return title;
    }
    
    public void addChangeListener(ChangeListener listener) {
        listenerList.add(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        listenerList.remove(listener);
    }

    public ChangeListener[] getChangeListeners() {
        return (ChangeListener[])listenerList.toArray(new ChangeListener[]{});
    }

    protected void fireFilterChanged() {
          
        if (listenerList.size() == 0)
            return;
        ChangeEvent evt = new ChangeEvent(this);
        ChangeListener[] listeners = getChangeListeners();
        for (int i = 0;i<listeners.length; i++) {
            listeners[i].stateChanged(evt);
        }
        updateResetButton();
    }

    private void updateResetButton() {
        final CalendarSelectionModel calendarSelectionModel = getService(CalendarSelectionModel.class);
        boolean defaultFilter = isResourceOnly ? calendarSelectionModel.isDefaultResourceTypes() : calendarSelectionModel.isDefaultEventTypes();
        if ( resetButton != null)
        {
            resetButton.setEnabled( !defaultFilter);
        }
    }

    public void setTypes(DynamicType[] types) throws RaplaException {
        this.types = types;
        content.removeAll();
        TableLayout tableLayout = new TableLayout();
        content.setLayout(tableLayout);
        tableLayout.insertColumn(0,TableLayout.PREFERRED);
        tableLayout.insertColumn(1,10);
        tableLayout.insertColumn(2,TableLayout.FILL);
        tableLayout.insertRow(0, TableLayout.PREFERRED);
        if (checkBoxes != null) {
            for (int i=0;i<checkBoxes.length;i++) {
                checkBoxes[i].removeActionListener(this);
            }
        }
        checkBoxes = new JCheckBox[types.length];
        filterEdit = new ClassificationEdit[types.length];

        String lastClassificationType= null;
        int row = 0;
        for (int i=0;i<types.length;i++) {
            String classificationType = types[i].getAnnotation( DynamicTypeAnnotations.KEY_CLASSIFICATION_TYPE);
            if ( !classificationType.equals( lastClassificationType)) {
                tableLayout.insertRow( row, 2);
                row ++;
                lastClassificationType = classificationType;
                tableLayout.insertRow( row, TableLayout.MINIMUM);
                content.add( getClassificationTitle( classificationType),"0,"+ row +",1," + row) ;
                if ( i== 0 )
                {
                    resetButton = new RaplaButton(getString("reset"), RaplaButton.SMALL);
                    resetButton.setIcon( getIcon("icon.remove"));
                    content.add( resetButton,"2,"+ row +",r,c");
                    resetButton.addActionListener( new ActionListener() {
                        
                        public void actionPerformed(ActionEvent arg0) {
                         try
                         {
                            mapFrom( null);
                            fireFilterChanged();
                         } catch (RaplaException ex) {
                            showException(ex, getComponent());
                         }
                        }
                    });
                }
                row ++;
                tableLayout.insertRow( row, 4);
                row ++;
                tableLayout.insertRow( row, 2);
                content.add( new JPanel() , "0," + row  + ",2,"  + row );
                row ++;

            }
            tableLayout.insertRow( row, 3);
            tableLayout.insertRow( row + 1, TableLayout.MINIMUM);
            tableLayout.insertRow( row + 2, TableLayout.MINIMUM);
            tableLayout.insertRow( row + 3, 3);
            tableLayout.insertRow( row + 4, 2);
            checkBoxes[i] = new JCheckBox(getName(types[i]));
            final JCheckBox checkBox = checkBoxes[i];
            checkBox.setBorder( BorderFactory.createEmptyBorder(0,10,0,0));
            checkBox.setOpaque( false );
            checkBox.addActionListener(this);
            checkBox.setSelected( true );
            content.add( checkBox , "0," + (row + 1) + ",l,t");
            filterEdit[i] = new ClassificationEdit(getContext());
            final ClassificationEdit edit = filterEdit[i];
            content.add( edit.getNewComponent() , "2," + (row + 1));
            content.add( edit.getRulesComponent() , "0," + (row + 2) + ",2,"+ (row + 2));
            content.add( new JPanel() , "0," + (row + 4) + ",2,"  + (row + 4));
            edit.addChangeListener(new ChangeListener() {
                
                public void stateChanged(ChangeEvent e) {
                    fireFilterChanged();
                }
            }
            );
            row += 5;
        }
    }


    private ClassificationFilter findFilter(DynamicType type,ClassificationFilter[] filters) {
        for (int i=0;i<filters.length;i++)
            if (filters[i].getType().equals(type))
                return filters[i];
        return null;
    }

    public void setFilter(CalendarSelectionModel o) throws RaplaException {
        CalendarSelectionModel filter =  o;

        List<DynamicType> list = new ArrayList<DynamicType>();
        if ( !isResourceOnly) {
            list.addAll( Arrays.asList( getQuery().getDynamicTypes( DynamicTypeAnnotations.VALUE_RESERVATION_CLASSIFICATION )));
        } 
        else
        {
            list.addAll( Arrays.asList( getQuery().getDynamicTypes( DynamicTypeAnnotations.VALUE_RESOURCE_CLASSIFICATION )));
            list.addAll( Arrays.asList( getQuery().getDynamicTypes( DynamicTypeAnnotations.VALUE_PERSON_CLASSIFICATION )));
        }
        setTypes( list.toArray( DynamicType.DYNAMICTYPE_ARRAY));

        mapFrom( filter );
    }

    public void mapFrom(ClassifiableFilter classifiableFilter) throws RaplaException {
        final ClassificationFilter[] filters;
        if ( classifiableFilter != null)
        {
            filters = isResourceOnly ? classifiableFilter.getAllocatableFilter() : classifiableFilter.getReservationFilter();
        }
        else
        {
            filters = new ClassificationFilter[] {};
        }

        for (int i=0;i<types.length;i++) {
            final DynamicType dynamicType = types[i];
            ClassificationFilter filter = findFilter(dynamicType, filters);
            final boolean fillDefault;
            if ( classifiableFilter != null)
            {
                fillDefault = isResourceOnly ? classifiableFilter.isDefaultResourceTypes() : classifiableFilter.isDefaultEventTypes();
            }
            else
            {
                fillDefault = true;
            }
            if ( filter == null && fillDefault)
            {
                filter = dynamicType.newClassificationFilter();
            }
            checkBoxes[i].setSelected( filter != null);
            filterEdit[i].mapFrom(filter);
        }
        scrollPane.revalidate();
        scrollPane.repaint();
        updateResetButton();
    }

    
    
    public ClassificationFilter[] getFilters() throws RaplaException {
        ArrayList<ClassificationFilter> list = new ArrayList<ClassificationFilter>();
        for (int i=0; i< filterEdit.length; i++) {
            ClassificationFilter filter = filterEdit[i].getFilter();
            if (filter != null) {
                list.add(filter);
            }
        }
        return list.toArray(new ClassificationFilter[] {});
    }

    public void actionPerformed(ActionEvent evt) {
        try {
            for (int i=0;i<checkBoxes.length;i++) {
                if (checkBoxes[i] == evt.getSource()) {
                    if (checkBoxes[i].isSelected())
                        filterEdit[i].mapFrom(types[i].newClassificationFilter());
                    else
                        filterEdit[i].mapFrom(null);
                    // activate the i. filter
                }
            }
        } catch (RaplaException ex) {
            showException(ex, getComponent());
        }
        content.revalidate();
        content.repaint();
        fireFilterChanged();
    }

    public JComponent getComponent() {
        return scrollPane;
    }

}


class ClassificationEdit extends RaplaGUIComponent implements ActionListener {
    JPanel ruleListPanel = new JPanel();
    JPanel newPanel = new JPanel();
    List<RuleComponent> ruleList = new ArrayList<RuleComponent>();
    JComboBox attributeSelector;
    JButton newLabel = new JButton();
    DynamicType type;
    
    ArrayList<ChangeListener> listenerList = new ArrayList<ChangeListener>();
   
    ClassificationEdit(RaplaContext sm) throws RaplaException {
        super(sm );
        ruleListPanel.setOpaque( false );
        ruleListPanel.setLayout(new BoxLayout(ruleListPanel,BoxLayout.Y_AXIS));
        newPanel.setOpaque( false );
        newPanel.setLayout(new TableLayout(new double[][] {{TableLayout.PREFERRED},{TableLayout.PREFERRED}}));
    }
    public void addChangeListener(ChangeListener listener) {
        listenerList.add(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        listenerList.remove(listener);
    }

    public ChangeListener[] getChangeListeners() {
        return listenerList.toArray(new ChangeListener[]{});
    }

    protected void fireFilterChanged() {
        if (listenerList.size() == 0)
            return;
        ChangeEvent evt = new ChangeEvent(this);
        ChangeListener[] listeners = getChangeListeners();
        for (int i = 0;i<listeners.length; i++) {
            listeners[i].stateChanged(evt);
        }
    }

    public JComponent getRulesComponent() {
        return ruleListPanel;
    }

    public JComponent getNewComponent() {
        return newPanel;
    }

    public void mapFrom(ClassificationFilter filter) throws RaplaException {
        getRulesComponent().removeAll();
        ruleList.clear();
        getNewComponent().removeAll();
        if ( filter == null) {
            type = null;
            return;
        }
        this.type = filter.getType();
        Attribute[] attributes = type.getAttributes();
        if (attributes.length == 0 )
            return;

        if (attributeSelector != null)
            attributeSelector.removeActionListener(this);
        attributeSelector = new JComboBox(attributes);

        attributeSelector.setRenderer(new NamedListCellRenderer(getI18n().getLocale()) {
            private static final long serialVersionUID = 1L;

                
                public Component getListCellRendererComponent(JList list,
                                                              Object value,
                                                              int index,
                                                              boolean isSelected,
                                                              boolean cellHasFocus) {
                    if (value == null) {
                        setText(getString("new_rule"));
                        return this;
                    } else {
                        return super.getListCellRendererComponent(list, value,index,isSelected,cellHasFocus);
                    }
                }
            });

        attributeSelector.addActionListener(this);
        newPanel.add(newLabel,"0,0,f,c");
        newPanel.add(attributeSelector,"0,0,f,c");
        newLabel.setText(getString("new_rule"));
        newLabel.setVisible(false);
        attributeSelector.setSelectedItem(null);
        Iterator<? extends ClassificationFilterRule> it = filter.ruleIterator();
        while (it.hasNext()) {
            addRuleComponent().setRule( it.next());
        }
        update();
    }

    public void update() {
        ruleListPanel.removeAll();
        int i=0;
        for (Iterator<RuleComponent> it = ruleList.iterator();it.hasNext();) {
            RuleComponent rule =  it.next();
            ruleListPanel.add( rule);
            rule.setAndVisible( i > 0);
            i++;
        }

        ruleListPanel.revalidate();
        ruleListPanel.repaint();
    }


    public void actionPerformed(ActionEvent evt) {
        if (evt.getSource() == attributeSelector) {
            Attribute att = (Attribute)attributeSelector.getSelectedItem();
            try {
                if (att != null) {
                    RuleComponent ruleComponent = getComponent(att);
                    if (ruleComponent == null) {
                        addRuleComponent().newRule( att);
                    } else {
                        ruleComponent.addOr();
                    }
                    update();
                    // invokeLater prevents a deadlock in jdk <=1.3
                    javax.swing.SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                attributeSelector.setSelectedIndex(-1);
                            }
                        });
                    fireFilterChanged();
                }
            } catch (RaplaException ex) {
                showException(ex, getNewComponent());
            }
        }
    }

    public ClassificationFilter getFilter() throws RaplaException {
        if ( type == null )
             return null;
        ClassificationFilter filter = type.newClassificationFilter();
        int i=0;
        for (Iterator<RuleComponent> it = ruleList.iterator();it.hasNext();) {
            RuleComponent ruleComponent =  it.next();
            Attribute attribute = ruleComponent.getAttribute();
            List<RuleRow> ruleRows = ruleComponent.getRuleRows();
            int size =  ruleRows.size();
            Object[][] conditions = new Object[size][2];
            for (int j=0;j<size;j++) {
                RuleRow ruleRow = ruleRows.get(j);
                conditions[j][0] = ruleRow.getOperatorValue();
                conditions[j][1] = ruleRow.getValue();
            }
            filter.setRule(i++ , attribute, conditions);
        }
        return filter;
    }

    private RuleComponent addRuleComponent() {
        RuleComponent ruleComponent = new RuleComponent();
        ruleList.add( ruleComponent );
        return ruleComponent;
    }

    private RuleComponent getComponent(Attribute attribute) {
        for (Iterator<RuleComponent> it = ruleList.iterator();it.hasNext();) {
            RuleComponent c2 = it.next();
            if (attribute.equals(c2.getAttribute())) {
                return c2;
            }
        }
        return null;
    }

    private void deleteRule(Component ruleComponent) {
        ruleList.remove( ruleComponent );
        update();
    }

    private String getAttName(String key) {
        return getName( type.getAttribute(key));
    }

    class RuleComponent extends JPanel {
        private static final long serialVersionUID = 1L;

        Attribute attribute;
        private final Listener listener = new Listener();
        List<RuleRow> ruleRows = new ArrayList<RuleRow>();
        List<RaplaButton> deleteButtons = new ArrayList<RaplaButton>();
        boolean isAndVisible;
        JLabel and;

        RuleComponent() {
            Border outer = BorderFactory.createCompoundBorder(
                                                               BorderFactory.createEmptyBorder(5,20,0,3)
                                                               ,BorderFactory.createEtchedBorder()
                                                               );
            this.setBorder(BorderFactory.createCompoundBorder(
                                                              outer
                                                              ,BorderFactory.createEmptyBorder(2,3,2,3)
                                                              ));
            this.setOpaque( false );
        }

        public Attribute getAttribute() {
            return attribute;
        }

        public List<RuleRow> getRuleRows() {
            return ruleRows;
        }

        public void newRule(Attribute attribute) throws RaplaException {
            ruleRows.clear();
            deleteButtons.clear();
            this.attribute = attribute;
            addRow(null, null);
            rebuild();
        }

        public void setRule(ClassificationFilterRule rule) throws RaplaException {
            ruleRows.clear();
            deleteButtons.clear();
            attribute = rule.getAttribute();
            Assert.notNull(attribute);
            Object[] ruleValues = rule.getValues();
            String[] operators = rule.getOperators();
            for (int i=0;i<ruleValues.length;i++) {
                addRow(operators[i], ruleValues[i]);
            }
            rebuild();
        }

        public void setAndVisible( boolean andVisible) {
            this.isAndVisible = andVisible;
            if ( and!= null)
            {
                if ( andVisible) {
                    and.setText( getString("and"));
                } else {
                    and.setText("");
                }
            }
        }

        private void rebuild() {
            this.removeAll();
            TableLayout layout = new TableLayout();
            layout.insertColumn(0,TableLayout.PREFERRED);
            layout.insertColumn(1,10);
            layout.insertColumn(2,TableLayout.PREFERRED);
            layout.insertColumn(3,5);
            layout.insertColumn(4,TableLayout.PREFERRED);
            layout.insertColumn(5,5);
            layout.insertColumn(6,TableLayout.FILL);
            this.setLayout(layout);

            int row =0;
            layout.insertRow(row,TableLayout.PREFERRED);
            and = new JLabel();
        //  and.setAlignmentX( and.LEFT_ALIGNMENT);
            this.add("0,"+row +",6,"+ row + ",l,c", and);
            if ( isAndVisible) {
                and.setText( getString("and"));
            } else {
                and.setText("");
            }
            row ++;

            int size =  ruleRows.size();
            for (int i=0;i<size;i++) {
                RuleRow ruleRow = (RuleRow) ruleRows.get(i);
                RaplaButton deleteButton = (RaplaButton) deleteButtons.get(i);
                layout.insertRow(row,TableLayout.PREFERRED);
                this.add("0," + row + ",l,c", deleteButton);
                if (i == 0)
                    this.add("2," + row + ",l,c", ruleRow.ruleLabel);
                else
                    this.add("2," + row + ",r,c", new JLabel(getString("or")));
                this.add("4," + row + ",l,c", ruleRow.operatorComponent);
                this.add("6," + row + ",f,c", ruleRow.field.getComponent());
                row ++;
                if (i<size -1) {
                     layout.insertRow(row , 2);
                     row++;
                }
            }
            revalidate();
            repaint();
        }

        public void addOr() throws RaplaException{
            addRow(null,null);
            rebuild();
        }

        private void addRow(String operator,Object ruleValue) throws RaplaException {
            RaplaButton deleteButton = new RaplaButton(RaplaButton.SMALL);
            deleteButton.setToolTipText(getString("delete"));
            deleteButton.setIcon(getIcon("icon.delete"));
            deleteButton.addActionListener(listener);
            deleteButtons.add(deleteButton);
            ruleRows.add(new RuleRow(attribute,operator,ruleValue));
        }

        class Listener implements ActionListener {
            public void actionPerformed(ActionEvent evt) {
                int index = deleteButtons.indexOf(evt.getSource());
                if (ruleRows.size() <= 1) {
                    deleteRule(RuleComponent.this);
                } else {
                    ruleRows.remove(index);
                    deleteButtons.remove(index);
                    rebuild();
                }
                fireFilterChanged();
            }
        }

    }

    class RuleRow {
        Object ruleValue;
        JLabel ruleLabel;
        JComponent operatorComponent;
        AbstractEditField field;
        Attribute attribute;

        RuleRow(Attribute attribute,String operator,Object ruleValue) throws RaplaException {
            this.attribute = attribute;
            this.ruleValue = ruleValue;
            ruleLabel = new JLabel();
            ruleLabel.setText(attribute.getName().getName(getI18n().getLang()));
            createField( attribute );
            field.setValue(ruleValue);
            field.addChangeListener( new ChangeListener() {
                
                public void stateChanged(ChangeEvent e) {
                    fireFilterChanged();
                }
            });
            setOperatorValue(operator);
            
            if ( operatorComponent instanceof ItemSelectable)
            {
                ((ItemSelectable)operatorComponent).addItemListener(new ItemListener() {
    
                    public void itemStateChanged(ItemEvent arg0) {
                        fireFilterChanged();
                    }
                });
            }
        }


        public String getOperatorValue() {
            AttributeType type = attribute.getType();
            if (type.equals(AttributeType.CATEGORY) || type.equals(AttributeType.BOOLEAN) )
                return "is";
            if (type.equals(AttributeType.STRING)) {
            	int index = ((JComboBox)operatorComponent).getSelectedIndex();
            	if (index == 0)
                return "contains";
            	if (index == 1)
            		return "starts";
            }
            if (type.equals(AttributeType.DATE) || type.equals(AttributeType.INT)) {
                int index = ((JComboBox)operatorComponent).getSelectedIndex();
                if (index == 0)
                    return "<";
                if (index == 1)
                    return "=";
                if (index == 2)
                    return ">";
                if (index == 3)
                    return "<>";
                if (index == 4)
                    return "<=";
                if (index == 5)
                    return ">=";
                
            }
            Assert.notNull(field,"Unknown AttributeType" + type);
            return null;
        }

        private void setOperatorValue(String operator) {
            AttributeType type = attribute.getType();
            if ((type.equals(AttributeType.DATE) || type.equals(AttributeType.INT)))
            {
                if (operator == null)
                    operator = "<";
                JComboBox box = (JComboBox)operatorComponent;
                if (operator.equals("<"))
                    box.setSelectedIndex(0);
                if (operator.equals("=") || operator.equals("is"))
                    box.setSelectedIndex(1);
                if (operator.equals(">"))
                    box.setSelectedIndex(2);
                if (operator.equals("<>"))
                    box.setSelectedIndex(3);
                if (operator.equals("<="))
                    box.setSelectedIndex(4);
                if (operator.equals(">="))
                    box.setSelectedIndex(5);
                
            }
        }

        private EditField createField(Attribute attribute) throws RaplaException {
            operatorComponent = null;
            AttributeType type = attribute.getType();
            String key = attribute.getKey();
            if (type.equals(AttributeType.CATEGORY))
            {
                operatorComponent = new JLabel("");
                Category rootCategory = (Category)attribute.getConstraint(ConstraintIds.KEY_ROOT_CATEGORY);
                if (rootCategory.getDepth() > 2) {
                    Category defaultCategory = (Category) attribute.defaultValue();
                    field = new CategorySelectField(getContext(),key,rootCategory,defaultCategory);
                } else {
                    field = new CategoryListField(getContext(),key,rootCategory);
                }
            }
            else if (type.equals(AttributeType.STRING))
                {
                field = new TextField(getContext(),key);
                DefaultComboBoxModel model = new DefaultComboBoxModel(new String[] {
                		 getString("filter.contains")
                        ,getString("filter.starts")
                    });
                operatorComponent = new JComboBox(model);
            }
            else if (type.equals(AttributeType.INT))
            {
                field = new LongField(getContext(),key);
                DefaultComboBoxModel model = new DefaultComboBoxModel(new String[] {
                    getString("filter.is_smaller_than")
                    ,getString("filter.equals")
                    ,getString("filter.is_greater_than")
                    ,getString("filter.not_equals")
                    ,getString("filter.smaller_or_equals")
                    ,getString("filter.greater_or_equals")
                });
                operatorComponent = new JComboBox(model);
                
            }
            else if (type.equals(AttributeType.DATE))
            {
                field = new DateField(getContext(),key);
                DefaultComboBoxModel model = new DefaultComboBoxModel(new String[] {
                    getString("filter.earlier_than")
                    ,getString("filter.equals")
                    ,getString("filter.later_than")
                    ,getString("filter.not_equals")
                }); 
                operatorComponent = new JComboBox(model);            }
            else if (type.equals(AttributeType.BOOLEAN))
            {
                operatorComponent = new JLabel("");
                field = new BooleanField(getContext(),key);
                ruleValue = new Boolean(false);
            }
           
            Assert.notNull(field,"Unknown AttributeType");
            field.setDelegate(new MyMappingDelegate(field));
            return field;
        }


        public Object getValue() throws RaplaException {
            ruleValue = field.getValue();
            return ruleValue;
        }

        class MyMappingDelegate implements MappingDelegate {
            AbstractEditField editField;
            MyMappingDelegate(AbstractEditField field) {
                this.editField = field;
            }
            public String getName() {
                return getAttName(editField.getFieldName());
            }
            public void mapFrom(Object o) throws RaplaException {
            }
            public void mapTo(Object o) throws RaplaException {
            }
        };
    }
}

