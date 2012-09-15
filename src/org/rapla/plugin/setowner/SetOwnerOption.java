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
package org.rapla.plugin.setowner;

import java.util.Locale;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.DefaultPluginOption;
import org.rapla.gui.OptionPanel;

public class SetOwnerOption extends DefaultPluginOption implements OptionPanel {
    
    public SetOwnerOption( RaplaContext sm ) throws RaplaException
    {
        super( sm );
    }


        
    protected void addChildren( DefaultConfiguration newConfig) {

    }

    protected void readConfig( Configuration config)   {

    }

    public void show() throws RaplaException  {
        super.show();
    }
  
    public void commit() throws RaplaException {
        super.commit();
    }

    /**
     * @see org.rapla.gui.DefaultPluginOption#getDescriptorClassName()
     */
    public String getDescriptorClassName() {
        return SetOwnerPlugin.class.getName();
    }
    
    public String getName(Locale locale) {
        return "Set Owner";
    }

}
