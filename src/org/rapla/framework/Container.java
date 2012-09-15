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
package org.rapla.framework;

import java.util.Collection;
import java.util.Map;

import org.apache.avalon.framework.configuration.Configuration;

public interface Container
{
     StartupEnvironment getStartupEnvironment();
     void addContainerProvidedComponentInstance(String role,Object component);
     void addContainerProvidedComponent(String role,String classname);
     void addContainerProvidedComponent(String role,String classname, String hint,Configuration config);
     RaplaContext getContext();
     /** returns a set with all hints to the services*/
     Collection<?> getAllServicesFor(String role);
     /** lookup all services for this role (grouped by their hint)*/
     Map<?,?> lookupServicesFor(String role) throws RaplaContextException;
 }

