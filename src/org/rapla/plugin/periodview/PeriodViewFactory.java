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
package org.rapla.plugin.periodview;

import javax.swing.Icon;

import org.rapla.facade.RaplaComponent;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.CalendarModel;
import org.rapla.gui.SwingCalendarView;
import org.rapla.gui.ViewFactory;
import org.rapla.gui.images.Images;
import org.rapla.servletpages.RaplaPageGenerator;

/** @deprecated */
public class PeriodViewFactory extends RaplaComponent implements ViewFactory
{
    public PeriodViewFactory( RaplaContext context ) throws RaplaException
    {
        super( context );
    }

    public final static String PERIOD_VIEW =  "period";

    public SwingCalendarView createSwingView(RaplaContext context, CalendarModel model, boolean editable) throws RaplaException
    {
        return new SwingPeriodCalendar( context, model, editable);
    }

    public RaplaPageGenerator createHTMLView(RaplaContext context, CalendarModel model) throws RaplaException
    {
        return new HTMLPeriodViewPage( context, model);
    }

    public String getViewId()
    {
        return PERIOD_VIEW;
    }

    public String getName()
    {
        return getString(PERIOD_VIEW);
    }

    Icon icon;
    public Icon getIcon()
    {
        if ( icon == null) {
            icon = Images.getIcon("/org/rapla/plugin/periodview/images/period_view.png");
        }
        return icon;
    }

    public String getMenuSortKey() {
        return "E";
    }

}

