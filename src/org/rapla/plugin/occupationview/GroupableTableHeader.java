package org.rapla.plugin.occupationview;

import java.util.Iterator;

import javax.swing.table.JTableHeader;


/**
 * This is the object which manages the header of the JTable and
 * also provides functionality for groupable headers.
 */
public class GroupableTableHeader extends JTableHeader {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
     * Constructs a GroupableTableHeader which is initialized with cm as the
     * column model. If cm is null this method will initialize the table header
     * with a default TableColumnModel.
     * @param model the column model for the table
     */    
    public GroupableTableHeader(GroupableTableColumnModel model) {
        super(model);
        setUI(new GroupableTableHeaderUI());
        setReorderingAllowed(false);
    }
    
    /**
     * Sets the margins correctly for all groups within
     * the header.
     */    
    public void setColumnMargin() {
        int columnMargin = getColumnModel().getColumnMargin();
        Iterator iter = ((GroupableTableColumnModel)columnModel).columnGroupIterator();
        while (iter.hasNext()) {
            ColumnGroup cGroup = (ColumnGroup)iter.next();
            cGroup.setColumnMargin(columnMargin);
        }
    }
}