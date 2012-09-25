package org.rapla.storage.xml.tests;

import java.util.Date;
import java.util.Locale;

import org.rapla.*;
import org.rapla.entities.Entity;
import org.rapla.entities.RaplaType;
import org.rapla.entities.User;
import org.rapla.entities.domain.*;
import org.rapla.entities.domain.internal.*;
import org.rapla.entities.dynamictype.Classification;
import org.rapla.storage.xml.*;

public class AllocatableTest extends RaplaTestCase {
	class AllocatableMock implements Allocatable {

		@Override
		public boolean isIdentical(Entity<?> id2) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean isPersistant() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public Allocatable cast() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public RaplaType getRaplaType() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getName(Locale locale) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Classification getClassification() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setClassification(Classification classification) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setOwner(User owner) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public User getOwner() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Date getCreateTime() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Date getLastChangeTime() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public User getLastChangedBy() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setHoldBackConflicts(boolean enable) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean isHoldBackConflicts() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void addPermission(Permission permission) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean removePermission(Permission permission) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean canAllocate(User user, Date start, Date end, Date today) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean canCreateConflicts(User user) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean canModify(User user) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public boolean canRead(User user) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public Permission[] getPermissions() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Permission newPermission() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean isPerson() {
			return false;
		}
		
	}
	
	public AllocatableTest(String name) {
		super(name);
	}
}
