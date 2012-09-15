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
package org.rapla.gui.internal.edit.reservation;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.rapla.components.layout.TableLayout;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.dynamictype.AttributeAnnotations;
import org.rapla.entities.dynamictype.Classifiable;
import org.rapla.entities.dynamictype.Classification;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.dynamictype.DynamicTypeAnnotations;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.internal.common.NamedListCellRenderer;
import org.rapla.gui.internal.edit.ClassificationEditUI;
import org.rapla.gui.internal.edit.EditField;
import org.rapla.gui.toolkit.EmptyLineBorder;
import org.rapla.gui.toolkit.RaplaButton;
import org.rapla.gui.toolkit.RaplaWidget;
/**
   Gui for editing the {@link Classification} of a reservation. Same as
   {@link org.rapla.gui.internal.edit.ClassificationEditUI}. It will only layout the
   field with a {@link java.awt.FlowLayout}.
 */
public class ReservationInfoEdit extends RaplaGUIComponent
    implements
        RaplaWidget
        ,ActionListener
{
    JPanel content = new JPanel();
    MyClassificationEditUI editUI;

    DynamicType oldDynamicType;
    Classification classification;
    Classification oldClassification;
    Classifiable classifiable;

    ArrayList<ChangeListener> listenerList = new ArrayList<ChangeListener>();
    ArrayList<DetailListener> detailListenerList = new ArrayList<DetailListener>();
    JComboBox typeSelector = new JComboBox();
    RaplaButton tabSelector = new RaplaButton();
    boolean isMainViewSelected = true;

    public ReservationInfoEdit(RaplaContext sm) throws RaplaException {
        super( sm);
        editUI = new MyClassificationEditUI(sm);
    }

    public JComponent getComponent() {
        return content;
    }


    public void requestFocus() {
        editUI.requestFocus();
    }

    private boolean hasSecondTab(Classification classification) {
        Attribute[] atts = classification.getAttributes();
        for ( int i=0; i < atts.length; i++ ) {
            String view = atts[i].getAnnotation(AttributeAnnotations.KEY_EDIT_VIEW,AttributeAnnotations.VALUE_MAIN_VIEW);
            if ( view.equals(AttributeAnnotations.VALUE_ADDITIONAL_VIEW)) {
                return true;
            }
        }
        return false;
    }

    public void setReservation(Classifiable classifiable) throws RaplaException {
        content.removeAll();
        this.classifiable = classifiable;
        classification = classifiable.getClassification();
        oldClassification = classification;

        DynamicType[] types = getQuery().getDynamicTypes( DynamicTypeAnnotations.VALUE_RESERVATION_CLASSIFICATION);
        DynamicType dynamicType = classification.getType();
        oldDynamicType = dynamicType;

        typeSelector =  new JComboBox( types );
        typeSelector.setSelectedItem(dynamicType);
        typeSelector.setRenderer(new NamedListCellRenderer(getI18n().getLocale()));
        typeSelector.addActionListener( this );


        content.setLayout( new BorderLayout());
        JPanel header = new JPanel();
        header.setLayout( null );
        header.add( typeSelector );
        Border border = new EmptyLineBorder();
        header.setBorder(  BorderFactory.createTitledBorder( border, getString("reservation_type") +":"));
        Dimension dim = typeSelector.getPreferredSize();
        typeSelector.setBounds(135,0, dim.width,dim.height);
        tabSelector.setText(getString("additional-view"));
        tabSelector.addActionListener( this );
        Dimension dim2 = tabSelector.getPreferredSize();
        tabSelector.setBounds(145 + dim.width ,0,dim2.width,dim2.height);
        header.add( tabSelector );
        header.setPreferredSize( new Dimension(600, Math.max(dim2.height, dim.height)));
        content.add( header,BorderLayout.NORTH);
        content.add( editUI.getComponent(),BorderLayout.CENTER);
        
        tabSelector.setVisible( hasSecondTab( classification ) || !isMainViewSelected);
        editUI.setObject( classification );
        
        editUI.getComponent().validate();
        updateHeight();
        content.validate();
        
    }

    /** registers new ChangeListener for this component.
     *  An ChangeEvent will be fired to every registered ChangeListener
     *  when the info changes.
     * @see javax.swing.event.ChangeListener
     * @see javax.swing.event.ChangeEvent
    */
    public void addChangeListener(ChangeListener listener) {
        listenerList.add(listener);
    }

    /** removes a listener from this component.*/
    public void removeChangeListener(ChangeListener listener) {
        listenerList.remove(listener);
    }

    public ChangeListener[] getChangeListeners() {
        return (ChangeListener[])listenerList.toArray(new ChangeListener[]{});
    }

    public void addDetailListener(DetailListener listener) {
        detailListenerList.add(listener);
    }

    /** removes a listener from this component.*/
    public void removeDetailListener(DetailListener listener) {
        detailListenerList.remove(listener);
    }

    public DetailListener[] getDetailListeners() {
        return detailListenerList.toArray(new DetailListener[]{});
    }

    protected void fireDetailChanged() {
        DetailListener[] listeners = getDetailListeners();
        for (int i = 0;i<listeners.length; i++) {
            listeners[i].detailChanged();
        }
    }

    public interface DetailListener {
    	void detailChanged();
    }

    protected void fireInfoChanged() {
        if (listenerList.size() == 0)
            return;
        ChangeEvent evt = new ChangeEvent(this);
        ChangeListener[] listeners = getChangeListeners();
        for (int i = 0;i<listeners.length; i++) {
            listeners[i].stateChanged(evt);
        }
    }

    // The DynamicType has changed
    public void actionPerformed(ActionEvent event) {
        try {
            Object source = event.getSource();
            if (source == typeSelector ) {
                DynamicType dynamicType = (DynamicType) typeSelector.getSelectedItem();
                if (dynamicType.equals(oldDynamicType))
                    classification = oldClassification;
                else
                    classification = dynamicType.newClassification( classification );
                classifiable.setClassification(classification);
                editUI.setObject(classification);
                tabSelector.setVisible( hasSecondTab( classification ) || !isMainViewSelected );
                content.validate();
                updateHeight();
                content.repaint();
                fireInfoChanged();
            }
            if (source == tabSelector ) {
                isMainViewSelected = !isMainViewSelected;
                fireDetailChanged();
                editUI.layout();
                tabSelector.setText( isMainViewSelected ?
                        getString("additional-view")
                        :getString("appointments")
                        );
                tabSelector.setIcon( isMainViewSelected ?
                        null
                        : getIcon("icon.list")
                        );
            }
        } catch (RaplaException ex) {
            showException(ex, content);
        }
    }

    private void updateHeight()
    {
        int newHeight = editUI.getHeight();
        editUI.getComponent().setPreferredSize(new Dimension(600,newHeight));
    }

    class MyClassificationEditUI extends ClassificationEditUI {
        int height  = 0;
        public MyClassificationEditUI(RaplaContext sm) throws RaplaException {
            super(sm);
        }

        public int getHeight()
        {
            return height;
        }
        protected void layout() {
            editPanel.removeAll();
            editPanel.setLayout( null );
            if ( !isMainViewSelected ) {
                super.layout();
                return;
            }
            /*
            FlowLayout layout = new FlowLayout();
            layout.setAlignment(FlowLayout.LEFT);
            layout.setHgap(10);
            layout.setVgap(2);
            editPanel.setLayout(layout);
            for (int i=0;i<fields.length;i++) {
                String tabview = getAttribute( i ).getAnnotation(AttributeAnnotations.KEY_EDIT_VIEW, AttributeAnnotations.VALUE_MAIN_VIEW);
                JPanel fieldPanel = new JPanel();
                fieldPanel.setLayout( new BorderLayout());
                fieldPanel.add(new JLabel(fields[i].getName() + ": "),BorderLayout.WEST);
                fieldPanel.add(fields[i].getComponent(),BorderLayout.CENTER);
                if ( tabview.equals("main-view") || !isMainViewSelected ) {
                    editPanel.add(fieldPanel);
                }
            }
            */
            TableLayout layout = new TableLayout();
            
            layout.insertColumn(0, 5);
            layout.insertColumn(1,TableLayout.PREFERRED);
            layout.insertColumn(2,TableLayout.PREFERRED);
            layout.insertColumn(3, 10);
            layout.insertColumn(4,TableLayout.PREFERRED);
            layout.insertColumn(5,TableLayout.PREFERRED);
            layout.insertColumn(6,TableLayout.FULL);
            
            int col= 1;
            int row = 0;
            layout.insertRow( row, 8);   
            row ++;
            layout.insertRow( row, TableLayout.PREFERRED);   
            editPanel.setLayout(layout);
            height = 10;
            int maxCompHeightInRow = 0;
            for (int i=0;i<fields.length;i++) {
                String tabview = getAttribute( i ).getAnnotation(AttributeAnnotations.KEY_EDIT_VIEW, AttributeAnnotations.VALUE_MAIN_VIEW);
                if ( !tabview.equals("main-view") ) {
                    continue;
                }
                editPanel.add(new JLabel(fields[i].getName() + ": "),col + "," + row +",l,c");
                col ++;
                editPanel.add(fields[i].getComponent(), col + "," + row +",f,c");
                int compHeight = (int)fields[i].getComponent().getPreferredSize().getHeight();
                compHeight =  Math.max(25, compHeight);
                // first row
                
                maxCompHeightInRow = Math.max(maxCompHeightInRow ,compHeight);
                
                col ++;
                col ++;
                if ( col >= layout.getNumColumn())
                {
                    col = 1;
                    if ( i < fields.length -1)
                    {
                        row ++;
                        layout.insertRow( row, 5);
                        height +=5;
                        row ++;
                        layout.insertRow( row, TableLayout.PREFERRED);
                        height += maxCompHeightInRow;
                        maxCompHeightInRow = 0;    
                    }
                }
            }
            height += maxCompHeightInRow;
            
        }

        public void requestFocus() {
            if (fields.length>0)
                fields[0].getComponent().requestFocus();
        }

        public void stateChanged(ChangeEvent evt) {
            try {
                Object o = this.classification;
                ((EditField)evt.getSource()).mapTo( o );
                fireInfoChanged();
            } catch (RaplaException ex) {
                showException(ex, this.getComponent());
            }
        }
    }

    public boolean isMainView() {
        return isMainViewSelected;
    }


}

