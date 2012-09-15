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

import java.util.Collection;
import java.util.Date;

import org.rapla.entities.RaplaObject;
import org.rapla.entities.RaplaType;
import org.rapla.entities.dynamictype.ClassificationFilter;
import org.rapla.entities.storage.Mementable;

/**
 *
 * @author ckohlhaas
 * @version 1.00.00
 * @since 2.03.00
 */
public interface CalendarModelConfiguration extends RaplaObject, Mementable<CalendarModelConfiguration> {
    public static final RaplaType TYPE = new RaplaType(CalendarModelConfiguration.class, "calendar");
    public static final String CONFIG_ENTRY = "org.rapla.DefaultSelection";
    public Date getStartDate();
    public Date getEndDate();
    public Date getSelectedDate();
    public String getTitle();
    public String getView();
    public Collection<RaplaObject> getSelected();
    public RaplaMap<RaplaObject> getSelectedMap();
    public RaplaMap<String> getOptionMap();
    //public Configuration get
    public ClassificationFilter[] getFilter();
    
    public boolean isDefaultEventTypes();
    public boolean isDefaultResourceTypes(); 

}
