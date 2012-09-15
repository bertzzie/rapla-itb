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
package org.rapla.gui.toolkit;

import javax.swing.tree.TreeNode;
import javax.swing.JTree;
import javax.swing.tree.TreePath;
import java.util.Enumeration;

/** Node object that is used to show recursive structures like the categories */
public abstract class RecursiveNode implements TreeNode {
    protected Object userObject;
    RecursiveNode[] childNodes;
    protected TreeNode parent;

    public RecursiveNode(TreeNode parent,Object userObject) {
        this.parent = parent;
        this.userObject = userObject;
    }

    abstract protected Object[] getChildObjects();
    abstract protected RecursiveNode createChildNode(Object userObject);


    public int getIndexOfUserObject(Object object) {
        Object[] childNodes = getChildNodes();
        for (int i=0;i<childNodes.length;i++) {
            if (((RecursiveNode)childNodes[i]).getUserObject().equals(object))
                return i;
        }
        return -1;
    }

    public Object getUserObject() {
        return userObject;
    }

    public TreeNode getParent() {
        return parent;
    }

    public int countParents() { // Count the parents
        TreeNode tmp = this;
        int parentCount = 0;
        while (tmp.getParent() != null) {
            tmp = tmp.getParent();
            parentCount ++;
        }
        return parentCount;
    }

    public boolean isLeaf() {
        return  getChildCount() == 0;
    }
    public int getChildCount() {
        return getChildNodes().length;
    }

    public boolean getAllowsChildren() {
        return true;
    }

    public Enumeration<?> children() {
        return new Enumeration<Object>() {
                int i = 0;
                public  boolean hasMoreElements() {
                    return (i + 1<childNodes.length);
                }
                public Object nextElement() {
                    if (i + 1<childNodes.length)
                        return childNodes[i ++ ];
                    else
                        return null;
                }
            };
    }

    /** Selects a row in a tree wich TreeNodePath corresponds to the
     * given path of userObjects.*/
    static public void selectUserObjects(JTree tree,Object[] userObjects) {
        int size = userObjects.length;
        RecursiveNode[] nodes = new RecursiveNode[size + 1];
        nodes[0] = (RecursiveNode)tree.getModel().getRoot();
        for (int i=1;i<=size ;i++) {
            Object obj = userObjects[size-i];
            int index = nodes[i-1].getIndexOfUserObject(obj);
            if (index<0) {
                tree.setSelectionRow(-1);
                return;
            }
            nodes[i] = (RecursiveNode)nodes[i-1].getChildAt(index);
        }
        /*
        System.out.print("TreePath: " );
        for (int i=0;i<=size ;i++)
            System.out.print("[" + i + "]" + nodes[i].getUserObject());
            System.out.println();*/
        TreePath treePath = new TreePath(nodes);
        if (nodes.length > 1) {
            RecursiveNode[] nodes2 = new RecursiveNode[nodes.length-1];
            System.arraycopy(nodes,0,nodes2,0,nodes.length-1);
            tree.expandPath(new TreePath(nodes2));
        }
        tree.setSelectionPath(treePath);
        tree.scrollPathToVisible(treePath);
    }

    /* returns the path from the root to the node*/
    public TreeNode[] getPath() {
        int parentCount = countParents();
        TreeNode[] path = new TreeNode[parentCount + 1];
        for (int i=parentCount ;i>0;i--) {
            if (i == parentCount)
                path[i-1] = getParent();
            else
                path[i-1] = path[i].getParent();
        }
        path[parentCount] = this;
        return path;
    }

    /* returns the path from the root to the node*/
    public TreePath getTreePath() {
        Object[] nodes = getPath();
        return new TreePath( nodes );
    }

    public int getIndex(TreeNode treeNode) {
        Object[] childNodes = getChildNodes();
        for (int i=0;i<childNodes.length;i++) {
            if (childNodes[i].equals(treeNode))
                return i;
        }
        return -1;
    }

    public TreeNode getChildAt(int index) {
        return getChildNodes()[index];
    }

    public RecursiveNode findNodeFor( Object obj ) {
        Object userObject = getUserObject();
        if ( userObject != null && userObject.equals( obj ) )
            return this;
        RecursiveNode[] childs = getRecursiveNodes();
        for ( int i = 0; i< childs.length; i++ ) {
            RecursiveNode result = childs[i].findNodeFor ( obj );
            if ( result != null ) {
                return result;
            }
        }
        return null;

    }

    public TreeNode[] getChildNodes() {
        return getRecursiveNodes();
    }

    private RecursiveNode[] getRecursiveNodes() {
        Object[] newChildren = getChildObjects();
        // Check if the childrens have changed
        if (childNodes != null && childNodes.length == newChildren.length) {
            boolean bChanged = false;
            for (int i=0;i<childNodes.length;i++) {
                if (childNodes[i].getUserObject() != newChildren[i]) {
                    bChanged = true;
                    break;
                }
            }
            // No changes, we can return the chached ones;
            if (!bChanged)
                return childNodes;
        }

        childNodes = new RecursiveNode[newChildren.length];
        for(int i = 0; i < newChildren.length; i++) {
            childNodes[i] = createChildNode(newChildren[i]);
        }
        return childNodes;
    }

    public boolean equals(Object obj) {
        if (obj instanceof RecursiveNode && getUserObject() != null) {
            return getUserObject().equals(((RecursiveNode)obj).getUserObject());
        }
        return false;
    }
}






