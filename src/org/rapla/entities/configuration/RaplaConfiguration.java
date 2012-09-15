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
package org.rapla.entities.configuration;

import java.io.Serializable;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.rapla.entities.RaplaObject;
import org.rapla.entities.RaplaType;

/**
 * 
 * @author ckohlhaas
 * @version 1.00.00
 * @since 2.03.00
 */
public class RaplaConfiguration implements RaplaObject, Serializable {
   // Don't forget to increase the serialVersionUID when you change the fields
   private static final long serialVersionUID = 1;

   public static final RaplaType TYPE = new RaplaType(RaplaConfiguration.class, "config");
   private Configuration conf;

   
   /** Creates a RaplaConfinguration with one element of the specified name 
    * @param name the element name
    * @param content The content of the element. Can be null.
    */
   public RaplaConfiguration( String name, String content) {
	   this.conf = new DefaultConfiguration(name);
	   if ( content != null)
	   {
		   ((DefaultConfiguration) this.conf).setValue( content );
	   }
   }
   
   public RaplaConfiguration( Configuration conf) {
       this.conf = conf;
   }
   
   public Configuration getConfig() {
       return conf;
   }
   

    public RaplaType getRaplaType() {
        return TYPE;
    }
    
    public Configuration find(  String localName) {
        Configuration[] childList= getConfig().getChildren();
        for ( int i=0;i<childList.length;i++) {
            if (childList[i].getName().equals( localName)) {
                return childList[i];
            }
        }
        return null;
    }

    public Configuration find(  String attributeName, String attributeValue) {
        Configuration[] childList= getConfig().getChildren();
        for ( int i=0;i<childList.length;i++) {
            String attribute = childList[i].getAttribute( attributeName,null);
            if (attributeValue.equals( attribute)) {
                return childList[i];
            }
        }
        return null;
    }

    public String getNamespace() {
        try {
            return getConfig().getNamespace();
        } catch (ConfigurationException ex){ 
            return null;
        }
    }
    
    public RaplaConfiguration replace(  Configuration newChild) throws ConfigurationException {
        return replace( find( newChild.getName()), newChild );
    }
        
    public RaplaConfiguration replace(  Configuration oldChild, Configuration newChild) {
        String localName = getConfig().getName();
        String nameSpace = getNamespace();
        DefaultConfiguration newConfig = new DefaultConfiguration(localName,nameSpace);
        boolean present = false;
        Configuration[] childList= getConfig().getChildren();
        for ( int i=0;i<childList.length;i++) {
            if (childList[i] != oldChild) {
                newConfig.addChild( childList[i]);
            }  else {
                present = true;
                newConfig.addChild( newChild );
            }
        }
        if (!present) {
            newConfig.addChild( newChild );
        }
        return new RaplaConfiguration( newConfig );
    }

    public RaplaConfiguration add(  Configuration newChild)  {
        String localName = getConfig().getName();
        String nameSpace = getNamespace();
        DefaultConfiguration newConfig = new DefaultConfiguration(localName,nameSpace);
        boolean present = false;
        Configuration[] childList= getConfig().getChildren();
        for ( int i=0;i<childList.length;i++) {
            if (childList[i] == newChild) {
                present = true;
            }
        }
        if (!present) {
            newConfig.addChild( newChild );
        }
        return new RaplaConfiguration( newConfig );
    }

    /**
     * @param configuration
     * @throws ConfigurationException
     */
    public RaplaConfiguration remove(Configuration configuration) {
        String localName = getConfig().getName();
        String nameSpace = getNamespace();
        DefaultConfiguration newConfig = new DefaultConfiguration(localName,nameSpace);
        Configuration[] childList= getConfig().getChildren();
        for ( int i=0;i<childList.length;i++) {
            if (childList[i] != configuration) {
                newConfig.addChild( childList[i]);
            }
        }
        return new RaplaConfiguration( newConfig );
    }
    
    public boolean equals( Object obj) {
    	if ( obj == null || !(obj instanceof RaplaConfiguration))
    	{
    		return false;
    	}
    	if ( obj == this)
    	{
    		return true;
    	}
    	RaplaConfiguration other = (RaplaConfiguration) obj;
    	return this.conf != null && other.conf != null && this.conf.equals( other.conf);  
    }

    
}
