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
package org.rapla.plugin.periodwizard;

import java.awt.Component;

import javax.swing.Icon;

import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.CalendarModel;
import org.rapla.gui.RaplaGUIComponent;
import org.rapla.gui.ReservationWizard;

/** The period-wizard-plugin eases the creation of reservations that repeat weekly
    in a given period. This is a very common usecase at universities and schools.
 */
public class PeriodReservationWizard extends RaplaGUIComponent implements ReservationWizard  {

    public PeriodReservationWizard(RaplaContext sm) throws RaplaException {
        super( sm);
        setChildBundleName( PeriodWizardPlugin.RESOURCE_FILE);
    }


    public String toString() {
        return getString("reservation.create_with_default_wizard");
    }

	public void start(Component owner, CalendarModel model
			) throws RaplaException {
	    WizardSequence sequence = new WizardSequence(getContext());
        DynamicType type = model.guessNewEventType();
		sequence.start(owner,model,type);
    	
	}

	public Icon getIcon() {
		// TODO Auto-generated method stub
		return null;
	}
}


