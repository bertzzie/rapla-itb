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
package org.rapla.entities.internal;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;

import org.rapla.components.util.Assert;
import org.rapla.components.util.Tools;
import org.rapla.entities.Category;
import org.rapla.entities.EntityNotFoundException;
import org.rapla.entities.IllegalAnnotationException;
import org.rapla.entities.MultiLanguageName;
import org.rapla.entities.RaplaType;
import org.rapla.entities.storage.EntityResolver;
import org.rapla.entities.storage.RefEntity;
import org.rapla.entities.storage.internal.SimpleEntity;

public class CategoryImpl extends SimpleEntity<Category> implements Category,java.io.Serializable
{
    // Don't forget to increase the serialVersionUID when you change the fields
    private static final long serialVersionUID = 1;

    private MultiLanguageName name = new MultiLanguageName();
    private String key;
    transient boolean childArrayUpToDate = false;
    transient Category[] childs;
    private HashMap<String,String> annotations = new HashMap<String,String>();

    public CategoryImpl() {
    }

    public void resolveEntities( EntityResolver resolver) throws EntityNotFoundException {
        super.resolveEntities( resolver);
        childArrayUpToDate = false;
    }

    public Category getParent() {
        return (Category)getReferenceHandler().get("parent");
    }

    public RaplaType getRaplaType() {return TYPE;}

    @SuppressWarnings("unchecked")
	void setParent(Category parent) {
        getReferenceHandler().put("parent",(RefEntity<Category>)parent);
    }

    public void removeParent()
    {
        getReferenceHandler().removeId("parent");
    }

    public Category[] getCategories() {
        if (!childArrayUpToDate || childs == null) {
            ArrayList<Category> categoryList = new ArrayList<Category>();
            Iterator<RefEntity<?>> it = getSubEntities();
            while (it.hasNext())
                categoryList.add((Category)it.next());
            childs = categoryList.toArray(Category.CATEGORY_ARRAY);
            childArrayUpToDate = true;
        }
        return childs;
    }

    public boolean isAncestorOf(Category category) {
        if (category == null)
            return false;
        if (category.getParent() == null)
            return false;
        if (category.getParent().equals(this))
            return true;
        else
            return isAncestorOf(category.getParent());
    }

    public Category getCategory(String key) {
        Iterator<RefEntity<?>> it = getSubEntities();
        while (it.hasNext()) {
            Category cat = (Category) it.next();
            if (cat.getKey().equals(key))
                return cat;
        }
        return null;
    }

    public boolean hasCategory(Category category) {
        return (super.isSubEntity((RefEntity<?>)category));
    }

    public void addCategory(Category category) {
        checkWritable();
        if (super.isSubEntity((RefEntity<?>)category))
            return;
        childArrayUpToDate = false;
        Assert.notNull(  category );
        Assert.isTrue(category.getParent() == null || category.getParent().equals(this)
                      ,"Category is already attached to a parent");
        super.addEntity((RefEntity<?>) category);
        ((CategoryImpl)category).setParent(this);
    }

    public int getDepth() {
        int max = 0;
        Category[] categories = getCategories();
        for (int i=0;i<categories.length;i++) {
            int depth = categories[i].getDepth();
            if (depth > max)
                max = depth;
        }
        return max + 1;
    }

    public void removeCategory(Category category) {
        checkWritable();
        if ( findCategory( category ) == null)
            return;
        childArrayUpToDate = false;
        super.removeEntity((RefEntity<?>) category);
        if (category.getParent().equals(this))
            ((CategoryImpl)category).setParent(null);
    }

    public Category findCategory(Category copy) {
        return (Category) super.findEntity((RefEntity<?>)copy);
    }

    public MultiLanguageName getName() {
        return name;
    }

    public void setReadOnly(boolean enable) {
        super.setReadOnly( enable );
        name.setReadOnly( enable );
    }

    public String getName(Locale locale) {
        return name.getName(locale.getLanguage());
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        checkWritable();
        this.key = key;
    }

    public String getPath(Category rootCategory,Locale locale) {
        StringBuffer buf = new StringBuffer();
        if (rootCategory != null && this.equals(rootCategory))
            return "";
        if (this.getParent() != null) {
            String path = this.getParent().getPath(rootCategory,locale);
            buf.append(path);
            if (path.length()>0)
                buf.append('/');
        }
        buf.append(this.getName(locale));
        return buf.toString();
    }

    public String toString() {
        MultiLanguageName name = getName();
        if (name != null) {
            return name.toString() + " ID='" + getId() + "'";
        }  else {
            return getKey()  + " " + getId();
        }
    }


    public String getPathForCategory(Category searchCategory) throws EntityNotFoundException {
        return getPathForCategory(searchCategory, true);
    }
    
    public String getPathForCategory(Category searchCategory, boolean fail) throws EntityNotFoundException {
        StringBuffer buf = new StringBuffer();
        Category category = searchCategory;
        Category parent = category.getParent();
        if (category == this)
            return "";
        if (parent == null)
            throw new EntityNotFoundException("Category has no parents!");
        while (true) {
            buf.insert(0,"']");
            buf.insert(0,category.getKey());
            buf.insert(0,"category[key='");
            parent = category.getParent();
            category = parent;
            if (parent == null)
            {
                if ( fail)
                {
                    throw new EntityNotFoundException("Category not found!" + searchCategory);
                }
                return null;
            }
            if (!parent.equals(this))
                buf.insert(0,'/');
            else
                break;
        }
        return buf.toString();
    }

    public Category getCategoryFromPath(String path) throws ParseException,EntityNotFoundException {
        int start = 0;
        int end = 0;
        int pos = 0;
        Category category = this;
        while (category != null) {
            start = path.indexOf("'",pos) + 1;
            if (start==0)
                break;
            end = path.indexOf("'",start);
            if (end < 0)
                throw new ParseException("Invalid xpath expression: " + path,start);
            String key = path.substring(start,end);
            category = category.getCategory(key);
            pos = end + 1;
        }
        if (category == null)
            throw new EntityNotFoundException("could not resolve category xpath expression: " + path);
        return category;
    }

    public Category findCategory(Object copy) {
        return (Category) super.findEntity((RefEntity<?>)copy);
    }


    public String getAnnotation(String key) {
        return annotations.get(key);
    }

    public String getAnnotation(String key, String defaultValue) {
        String annotation = getAnnotation( key );
        return annotation != null ? annotation : defaultValue;
    }

    public void setAnnotation(String key,String annotation) throws IllegalAnnotationException {
        checkWritable();
        if (annotation == null) {
            annotations.remove(key);
            return;
        }
        annotations.put(key,annotation);
    }

    public String[] getAnnotationKeys() {
        return annotations.keySet().toArray(Tools.EMPTY_STRING_ARRAY);
    }

    @SuppressWarnings("unchecked")
	static private void copy(CategoryImpl source,CategoryImpl dest) {
        dest.name = (MultiLanguageName) source.name.clone();
        dest.annotations = (HashMap<String,String>) source.annotations.clone();
        dest.key = source.key;
        Iterator<RefEntity<?>> it = dest.getSubEntities();
        while ( it.hasNext()) {
            ((CategoryImpl)it.next()).setParent(dest);
        }
        dest.childArrayUpToDate = false;
    }

    public void copy(Category obj) {
        CategoryImpl category = (CategoryImpl)obj;
		super.copy((SimpleEntity<Category>)category);
        copy(category,this);
    }

    public Category deepClone() {
        CategoryImpl clone = new CategoryImpl();
        super.deepClone(clone);
        copy(this,clone);
        return clone;
    }

    public Category clone() {
        CategoryImpl clone = new CategoryImpl();
        super.clone(clone);
        copy(this,clone);
        return clone;
    }



}


