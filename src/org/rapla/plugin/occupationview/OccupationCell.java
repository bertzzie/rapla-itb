/*--------------------------------------------------------------------------*
 | Copyright (C) 2011 Bob Jordaens                                          |
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

package org.rapla.plugin.occupationview;

import org.rapla.entities.domain.Reservation;

public class OccupationCell {
	
		char leftBound;
		char rightBound;
		//  0 : Free Cell
		// -1 : Forbidden Cell Resource is not available at all, out of order or in maintenance
		// -2 : FirstFit Cell
		private int typeId;
		Reservation reservation;
		public Reservation getReservation() {
            return reservation;
        }
        public OccupationCell(char leftBound, int typeId, char rightBound) 
		{
		    this(leftBound, typeId, rightBound, null);
		}
		public OccupationCell(char leftBound, int typeId, char rightBound, Reservation reservation) {
			this.leftBound = leftBound;
			this.typeId = typeId;
			this.rightBound = rightBound;
			this.reservation = reservation;
		}
		    

        public int getTypeId() {
            return typeId;
        }
}