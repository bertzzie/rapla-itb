/*--------------------------------------------------------------------------*
 | Copyright (C) 2006 ?, Christopher Kohlhaas                               |
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
package org.rapla.server;

import java.util.Map;


public class RemoteMethod {
    String methodName;
    String[] parameterNames;
    public RemoteMethod(String methodName)
    {
        this(methodName,new String[]{} );
    }
    public RemoteMethod(String methodName, String[] parameterNames)
    {
        this.methodName = methodName;
        this.parameterNames =  parameterNames;
    }

    public String method()
    {
        return methodName;
    }
   
    public String arg(int index)
    {
        return parameterNames[index];
    }

    public int length()
    {
        return parameterNames.length;
    }
    
    public String[] getArgumentNames()
    {
        return parameterNames;
    }
    
    public String value( Map<String,String> arguments, int i )
    {
        String argumentName = parameterNames[i];
        return arguments.get( argumentName);
    }
    public boolean is( String methodName)
    {
      
        return this.methodName.equals( methodName);
    }
    
    public boolean equals(Object obj) {
        if ( obj instanceof String)
        {
            return is( (String)obj); 
        }
        return super.equals (obj);
   }
    
}
