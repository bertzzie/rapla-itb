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
package org.rapla.storage.xml;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.rapla.entities.Category;
import org.rapla.entities.RaplaObject;
import org.rapla.entities.RaplaType;
import org.rapla.entities.User;
import org.rapla.entities.configuration.Preferences;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.Period;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;

/** Stores the data from the local cache in XML-format to a print-writer.*/
public class RaplaMainWriter extends RaplaXMLWriter
{
    /**
     * @param sm
     * @throws RaplaException
     */
    public RaplaMainWriter(RaplaContext sm) throws RaplaException {
        super(sm);
    }
    
    public void setWriter( BufferedWriter writer ) {
        super.setWriter( writer );
        for ( Iterator<RaplaXMLWriter> it = writerMap.values().iterator();it.hasNext();) {
            ((RaplaXMLWriter)it.next()).setWriter( writer );
        }
    }
    
    public void setEncoding( String encoding ) {
        super.setEncoding( encoding );
        for ( Iterator<RaplaXMLWriter> it = writerMap.values().iterator();it.hasNext();) {
            (it.next()).setEncoding( encoding );
        }
    }
    
    protected void printContent() throws IOException,RaplaException {
        printHeader( 0 );

        ((CategoryWriter)getWriterFor(Category.TYPE)).printCategories();
        println();
        ((DynamicTypeWriter)getWriterFor(DynamicType.TYPE)).printDynamicTypes();
        println();
        ((PreferenceWriter)getWriterFor(Preferences.TYPE)).printPreferences( cache.getPreferences( null ));
        println();
        ((UserWriter)getWriterFor(User.TYPE)).printUsers();
        println();
        ((AllocatableWriter)getWriterFor(Allocatable.TYPE)).printAllocatables();
        println();
        ((PeriodWriter)getWriterFor(Period.TYPE)).printPeriods();
        println();
        ((ReservationWriter)getWriterFor(Reservation.TYPE)).printReservations();
        println();
        
        closeElement("rapla:data");
    }

    private void printHeader(long repositoryVersion) throws IOException
    {
        println("<?xml version=\"1.0\" encoding=\"" + encoding + "\"?><!--*- coding: " + encoding + " -*-->");
        openTag("rapla:data");
        for (int i=0;i<NAMESPACE_ARRAY.length;i++) {
            String prefix = NAMESPACE_ARRAY[i][1];
            String uri = NAMESPACE_ARRAY[i][0];
            if ( prefix == null) {
                att("xmlns", uri);
            } else {
                att("xmlns:" + prefix, uri);
            }
            println();
        }
        att("version", OUTPUT_FILE_VERSION);
        if ( repositoryVersion > 0)
        {
            att("repositoryVersion", String.valueOf(repositoryVersion));
        }
        closeTag();
    }

    public void write(OutputStream out ) throws IOException,RaplaException {
        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(out,encoding));
        setWriter(w);
        printContent();
        w.flush();
    }

    public void printList( List<? extends RaplaObject> resources,List<? extends RaplaObject> remove, long repositoryVersion ) throws RaplaException, IOException
    {
       printHeader( repositoryVersion);
       printListPrivate( resources);
       openElement("rapla:remove");
       for ( Iterator<? extends RaplaObject> it = remove.iterator();it.hasNext();)
       {
           RaplaObject object =  it.next();
           openTag("rapla:" + object.getRaplaType().getLocalName());
           att("idref", getId( object));
           closeElementTag();
       }
       closeElement("rapla:remove");
       closeElement("rapla:data");
    }
    
    
    protected void printListPrivate( List<? extends RaplaObject> resources ) throws RaplaException, IOException
    {
       SortedSet<RaplaObject> set = new TreeSet<RaplaObject>(new RaplaEntityComparator());
       HashSet<RaplaObject> hashSet = new HashSet<RaplaObject>(resources);
       set.addAll(resources );
       for ( Iterator<RaplaObject> it = set.iterator();it.hasNext();)
       {
           RaplaObject object =  it.next();
           
           RaplaType type = object.getRaplaType();
           RaplaXMLWriter writer;
           if ( type == Attribute.TYPE || type == Appointment.TYPE)
           {
               continue;
           }
           try
           {
              writer = getWriterFor(type);
           }
           catch (RaplaException e)
           {
               System.err.println( e.getMessage());
               continue;
           }
           if ( type == Preferences.TYPE)
           {
               Preferences preferences= (Preferences)object;
               if ( preferences.getOwner() != null && hashSet.contains( preferences.getOwner()))
               {
                   continue;
               }
               writer.writeObject(object);
           }
           if ( type == Category.TYPE)
           {
               Category category = (Category)object;
               if (category.getParent() != null && hashSet.contains(category.getParent()))
               {
                   continue;
               }
               ((CategoryWriter) writer).printCategory( category, true);
           }
           else
           {
               writer.writeObject(object);
           }
            
       }
        
    }
    

    




}



