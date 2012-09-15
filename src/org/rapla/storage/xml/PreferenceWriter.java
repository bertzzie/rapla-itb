/*--------------------------------------------------------------------------*
  | Copyright (C) 2006 Christopher Kohlhaas                                  |
  |                                                                          |
  | This program is free software; you can redistribute it and/or modify     |
  | it under the terms of the GNU General Public License as published by the |
  | Free Software Foundation. A copy of the license has been included with   |
  | these distribution in the COPYING file, if not go to www.fsf.org .       |
  |                                                                          |
  | As a special exception, you are granted the permissions to link this     |
  | program with every library, which license fulfills the Open Source       |
  | Definition as published by the Open Source Initiative (OSI).             |
  *--------------------------------------------------------------------------*/

package org.rapla.storage.xml;

import java.io.IOException;
import java.util.Iterator;

import org.rapla.entities.RaplaObject;
import org.rapla.entities.RaplaType;
import org.rapla.entities.User;
import org.rapla.entities.configuration.Preferences;
import org.rapla.entities.configuration.internal.PreferencesImpl;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;

    
public class PreferenceWriter extends RaplaXMLWriter {
    public static final String WRITERMAP = PreferenceWriter.class.getPackage().getName() + ".writerMap";

    public PreferenceWriter(RaplaContext sm) throws RaplaException {
        super(sm);
    }
    
    protected void printPreferences(Preferences preferences) throws IOException,RaplaException {
        if ( !preferences.isEmpty()) {
            openTag("rapla:preferences");
            if (isIdOnly())
            {
                att("id",getId(preferences));
                User owner = preferences.getOwner();
                if ( owner != null )
                {
                    att("ownerid", getId( owner));
                }
            }
            printVersion( preferences);
            
            closeTag();
            Iterator<String> it = ((PreferencesImpl)preferences).getPreferenceEntries();
            while (it.hasNext()) {
                String role =  it.next();
                Object entry = preferences.getEntry(role);
                if ( entry instanceof String) {
                	openTag("rapla:entry");
                    att("key", role );
                    att("value", (String)entry);
                    closeElementTag();
                } if ( entry instanceof RaplaObject) {
                	openTag("rapla:entry");
                    att("key", role );
                    closeTag();
                    RaplaType raplaType = ((RaplaObject)entry).getRaplaType();
                	RaplaXMLWriter writer = getWriterFor( raplaType); 
                	writer.writeObject( (RaplaObject)entry );
                	closeElement("rapla:entry");
                }
            }
            closeElement("rapla:preferences");
        }
    }
    
    public void writeObject(RaplaObject object) throws IOException, RaplaException {
        printPreferences( (Preferences) object);
    }

 
 }
 


