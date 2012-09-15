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
package org.rapla.gui.internal.edit;

import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
/****************************************************************
 * This is the controller-class for the Resource-Edit-Panel     *
 ****************************************************************/
class AllocatableEditUI  extends AbstractEditUI  {
    public AllocatableEditUI(RaplaContext sm) throws RaplaException {
        super(sm);
        EditField[] fields = new EditField[] {
            new ClassificationField(sm)
            ,new PermissionListField(sm,"permissions")
            ,new BooleanField(sm,"holdBackConflicts")
        };

        setFields(fields);
    }

    public void mapToObject() throws RaplaException {
        super.mapToObject();
        if ( getName(o).length() == 0)
            throw new RaplaException(getString("error.no_name"));

    }


}
