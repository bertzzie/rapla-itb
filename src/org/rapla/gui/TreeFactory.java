package org.rapla.gui;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;

import org.rapla.entities.Category;
import org.rapla.entities.Named;
import org.rapla.entities.dynamictype.Classifiable;
import org.rapla.framework.RaplaException;
import org.rapla.gui.toolkit.TreeToolTipRenderer;

public interface TreeFactory {
	
	TreeModel createClassifiableModel(Classifiable[] classifiables) throws RaplaException;

    DefaultMutableTreeNode newNamedNode(Named element);

	TreeModel createModel(Category category)	throws RaplaException;

	TreeModel createModelFlat(Named[] element);

	TreeToolTipRenderer createTreeToolTipRenderer();

	TreeCellRenderer createRenderer();

}