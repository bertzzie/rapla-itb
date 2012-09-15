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
package org.rapla.facade.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EventListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.rapla.components.util.Command;
import org.rapla.components.util.CommandQueue;
import org.rapla.components.util.DateTools;
import org.rapla.components.xmlbundle.I18nBundle;
import org.rapla.entities.Category;
import org.rapla.entities.Entity;
import org.rapla.entities.EntityNotFoundException;
import org.rapla.entities.Ownable;
import org.rapla.entities.RaplaObject;
import org.rapla.entities.RaplaType;
import org.rapla.entities.User;
import org.rapla.entities.UserComparator;
import org.rapla.entities.configuration.CalendarModelConfiguration;
import org.rapla.entities.configuration.Preferences;
import org.rapla.entities.configuration.RaplaMap;
import org.rapla.entities.configuration.internal.CalendarModelConfigurationImpl;
import org.rapla.entities.configuration.internal.RaplaMapImpl;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.Period;
import org.rapla.entities.domain.Permission;
import org.rapla.entities.domain.RepeatingType;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.domain.internal.AllocatableImpl;
import org.rapla.entities.domain.internal.AppointmentImpl;
import org.rapla.entities.domain.internal.PeriodImpl;
import org.rapla.entities.domain.internal.ReservationImpl;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.dynamictype.AttributeType;
import org.rapla.entities.dynamictype.Classifiable;
import org.rapla.entities.dynamictype.Classification;
import org.rapla.entities.dynamictype.ClassificationFilter;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.dynamictype.DynamicTypeAnnotations;
import org.rapla.entities.dynamictype.internal.AttributeImpl;
import org.rapla.entities.dynamictype.internal.DynamicTypeImpl;
import org.rapla.entities.internal.CategoryImpl;
import org.rapla.entities.internal.UserImpl;
import org.rapla.entities.storage.Mementable;
import org.rapla.entities.storage.RefEntity;
import org.rapla.facade.AllocationChangeEvent;
import org.rapla.facade.AllocationChangeListener;
import org.rapla.facade.ClientFacade;
import org.rapla.facade.Conflict;
import org.rapla.facade.ModificationEvent;
import org.rapla.facade.ModificationListener;
import org.rapla.facade.PeriodModel;
import org.rapla.facade.UpdateErrorListener;
import org.rapla.facade.UpdateModule;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaContextException;
import org.rapla.framework.RaplaException;
import org.rapla.framework.RaplaLocale;
import org.rapla.storage.RaplaSecurityException;
import org.rapla.storage.StorageOperator;
import org.rapla.storage.StorageUpdateListener;
import org.rapla.storage.UpdateResult;

/**
 * This is the default implementation of the necessary Client-Facade to the
 * DB-Subsystem.
 * <p>
 * Sample configuration 1:
 * 
 * <pre>
 *    &lt;facade id="facade">
 *       &lt;store>file&lt;/store>
 *    &lt;/facade>
 * </pre>
 * 
 * </p>
 * <p>
 * Sample Configuration 2:
 * 
 * <pre>
 *      &lt;facade id="facade" logger="facade">
 *         &lt;store>remote&lt;/store>
 *         &lt;username>homer&lt;/username>
 *         &lt;password>duffs&lt;/password>
 *      &lt;/facade>
 * </pre>
 * 
 * This facade automatically starts with user homer.
 * </p>
 * <p>
 * The store entry contains the id of a storage-component. Storage-Components
 * are all components that implement the {@link StorageOperator} interface.
 * </p>
 */

public class FacadeImpl extends AbstractLogEnabled implements ClientFacade,
		StorageUpdateListener {
	protected CommandQueue notifyQueue;
	private Configuration operatorConfig;
	private User workingUser = null;
	private User originalUser = null;
	private StorageOperator operator;
	private Vector<ModificationListener> modificatonListenerList = new Vector<ModificationListener>();
	private Vector<AllocationChangeListener> allocationListenerList = new Vector<AllocationChangeListener>();
	private Vector<UpdateErrorListener> errorListenerList = new Vector<UpdateErrorListener>();
	private I18nBundle i18n;
	private PeriodModelImpl periodModel;
	private ConflictFinder conflictFinder;
	private Vector<ModificationListener> directListenerList = new Vector<ModificationListener>();

	String username;
	String password;
	Locale locale;
	Timer refreshTimer;

	public FacadeImpl(RaplaContext context, Configuration config, Logger logger)
			throws RaplaException {
		enableLogging(logger);
		i18n = (I18nBundle) context.lookup(I18nBundle.ROLE
				+ "/org.rapla.RaplaResources");
		locale = ((RaplaLocale) context.lookup(RaplaLocale.ROLE)).getLocale();
		operatorConfig = config.getChild("store");
		getLogger().debug(
				"Facade configured with operator '"
						+ operatorConfig.getValue("*") + "'");
		username = config.getChild("username").getValue(null);
		password = config.getChild("password").getValue("");
		try {
			operator = (StorageOperator) context.lookup(StorageOperator.ROLE
					+ "/" + operatorConfig.getValue("*"));
			operator.addStorageUpdateListener(this);

		} catch (RaplaContextException ex) {
			throw new RaplaContextException(StorageOperator.ROLE, "Store "
					+ operatorConfig.getValue("*") + " "
					+ operatorConfig.getLocation() // BJO
					+ " is not found (or could not be initialized)", ex);
		}
		notifyQueue = org.rapla.components.util.CommandQueue
				.createCommandQueue();
		if (username != null)
			if (!login(username, password.toCharArray()))
				throw new RaplaException(i18n.getString("error.login"));
		initRefresh();
		conflictFinder = new ConflictFinder(operator);
	}

	public StorageOperator getOperator() {
		return operator;
	}

	// Implementation of StorageUpdateListener.
	/**
	 * This method is called by the storage-operator, when stored objects have
	 * changed.
	 * 
	 * <strong>Caution:</strong> You must not lock the storage operator during
	 * processing of this call, because it could have been locked by the store
	 * method, causing deadlocks
	 */
	public void objectsUpdated(UpdateResult evt) {
		if (getLogger().isDebugEnabled())
			getLogger().debug("Objects updated");

		notifyQueue.enqueue(new UserCheckCommand());
		
		// Conflicts are updated before the queue
		AllocationChangeEvent[] events = createAllocationChangeEvents(evt);
		conflictFinder.updateConflicts( events);
		
		fireUpdateEvent(evt);
	}

	public void switchTo(User user) {
		if (user == null) {
			workingUser = originalUser;
			originalUser = null;
		} else {
			originalUser = workingUser;
			workingUser = user;
		}
		// fireUpdateEvent(new ModificationEvent());
	}

	public boolean canSwitchBack() {
		return originalUser != null;
	}

	public void updateError(RaplaException ex) {
		getLogger().fatalError(ex.getMessage(), ex);
		fireUpdateError(ex);
	}

	public void storageDisconnected() {
		fireStorageDisconnected();
	}

	/******************************
	 * Update-module *
	 ******************************/
	public boolean isClientForServer() {
		return operator.supportsActiveMonitoring();
	}

	public void refresh() throws RaplaException {
		synchronized (operator.getLock()) {
			if (operator.supportsActiveMonitoring()) {
				operator.refresh();
			}
		}
	}

	public void addModificationListener(ModificationListener listener) {
		modificatonListenerList.add(listener);
	}

	public void addDirectModificationListener(ModificationListener listener) 
	{
		directListenerList.add(listener);
	}
	
	public void removeDirectModificationListener(ModificationListener listener) 
	{
		directListenerList.remove(listener);
	}

	
	public void removeModificationListener(ModificationListener listener) {
		modificatonListenerList.remove(listener);
	}

	public ModificationListener[] getModificationListeners() {
		return (ModificationListener[]) modificatonListenerList
				.toArray(new ModificationListener[] {});
	}

	private Collection<ModificationListener> getModificationListeners(
			boolean invokeLater) {
		if (modificatonListenerList.size() == 0)
			return Collections.emptyList();
		synchronized (this) {
			Collection<ModificationListener> list = new ArrayList<ModificationListener>(3);
			if (periodModel != null) {
				list.add(periodModel);
			}
			Iterator<ModificationListener> it = modificatonListenerList
					.iterator();
			while (it.hasNext()) {
				ModificationListener listener =  it
						.next();
				if (listener.isInvokedOnAWTEventQueue() == invokeLater)
					list.add(listener);
			}
			return list;
		}
	}

	public void addAllocationChangedListener(AllocationChangeListener listener) {
		allocationListenerList.add(listener);
	}

	public void removeAllocationChangedListener(
			AllocationChangeListener listener) {
		allocationListenerList.remove(listener);
	}

	private Collection<AllocationChangeListener> getAllocationChangeListeners(
			boolean invokeLater) {
		if (allocationListenerList.size() == 0)
			return Collections.emptyList();
		synchronized (this) {
			Collection<AllocationChangeListener> list = new ArrayList<AllocationChangeListener>(
					3);
			Iterator<AllocationChangeListener> it = allocationListenerList
					.iterator();
			while (it.hasNext()) {
				AllocationChangeListener listener = (AllocationChangeListener) it
						.next();
				if (listener.isInvokedOnAWTEventQueue() == invokeLater)
					list.add(listener);
			}
			return list;
		}
	}
	
	public AllocationChangeEvent[] createAllocationChangeEvents(UpdateResult evt) {
		Logger logger = getLogger().getChildLogger("trigger.allocation");
		AllocationChangeFinder trigger = new AllocationChangeFinder(logger,
				evt);
		return (AllocationChangeEvent[]) trigger
				.getTriggerEvents().toArray(new AllocationChangeEvent[0]);
	}

	public void addUpdateErrorListener(UpdateErrorListener listener) {
		errorListenerList.add(listener);
	}

	public void removeUpdateErrorListener(UpdateErrorListener listener) {
		errorListenerList.remove(listener);
	}

	public UpdateErrorListener[] getUpdateErrorListeners() {
		return (UpdateErrorListener[]) errorListenerList
				.toArray(new UpdateErrorListener[] {});
	}

	protected void fireUpdateError(RaplaException ex) {
		UpdateErrorListener[] listeners = getUpdateErrorListeners();
		for (int i = 0; i < listeners.length; i++) {
			listeners[i].updateError(ex);
		}
	}

	protected void fireStorageDisconnected() {
		UpdateErrorListener[] listeners = getUpdateErrorListeners();
		for (int i = 0; i < listeners.length; i++) {
			listeners[i].disconnected();
		}
	}

	final class UserCheckCommand implements Command {
		public void execute() {
			try {
				if (workingUser == null)
					return;

				synchronized (operator.getLock()) {
					Iterator<User> it = operator.getObjects(User.class)
							.iterator();
					while (it.hasNext()) {
						User user = it.next();
						if (user.equals(workingUser)) {
							workingUser = user;
							return;
						}
					}
				}
				if (workingUser == null)
					return;
				throw new EntityNotFoundException("User (" + workingUser
						+ ") not found");
			} catch (RaplaException ex) {
				fireUpdateError(ex);
			}
		}
	}

	private final void initRefresh() 
	{
		TimerTask refreshTask = new TimerTask() {
			public void run() {
				synchronized (operator.getLock()) {
					try {
						if (operator.isConnected()) {
							refresh();
						}
					} catch (RaplaException e) {
						getLogger().error("Error refreshing.", e);
					}
				}
			}
		};
		int intervalLength = 30000;
		if (operator.isConnected()) {
			try {
				intervalLength = operator.getPreferences(null)
						.getEntryAsInteger(UpdateModule.REFRESH_INTERVAL_ENTRY,
								intervalLength);
			} catch (RaplaException e) {
				getLogger().error("Error refreshing.", e);
			}
		}
		if ( refreshTimer != null)
		{
			refreshTimer.cancel();
		}
		refreshTimer = new Timer(true); // Start timer as daemon-thread
		refreshTimer.schedule(refreshTask, 0, intervalLength);
	}

	final class UpdateCommand implements Runnable, Command {
		Collection<EventListener> listeners;
		boolean bError = false;
		ModificationEvent modificationEvent;
		AllocationChangeEvent[] allocationChangeEvents;

		public UpdateCommand(Collection<ModificationListener> modificationListeners,
				Collection<AllocationChangeListener> allocationChangeListeners, UpdateResult evt) {

			this.listeners = new ArrayList<EventListener>(modificationListeners);
			this.listeners.addAll(allocationChangeListeners);
			if (evt != null) {
				Set<RaplaObject> storeEvents = new HashSet<RaplaObject>();
				storeEvents.addAll(evt.getChangeObjects());
				storeEvents.addAll(evt.getAddObjects());
				Set<RaplaObject> removeEvents = evt.getRemoveObjects();
				this.modificationEvent = new ModificationEventImpl(storeEvents,
						removeEvents);
			} else {
				// refresh
				this.modificationEvent = new ModificationEventImpl();
			}

			allocationChangeEvents = createAllocationChangeEvents(evt);
		}

		

		public boolean hasFailed() {
			return bError;
		}

		public void execute() {
			run();
		}

		public void run() {
			long time = System.currentTimeMillis();
			Iterator<EventListener> it = listeners.iterator();

			while (it.hasNext()) {
				if (!operator.isConnected())
					return;
				try {
					Object listener = it.next();
					if (getLogger().isDebugEnabled())
						getLogger().debug("Notifying " + listener);
					if (listener instanceof ModificationListener) {
						((ModificationListener) listener)
								.dataChanged(modificationEvent);
					}
					if (listener instanceof AllocationChangeListener
							&& allocationChangeEvents.length > 0) {
						((AllocationChangeListener) listener)
								.changed(allocationChangeEvents);
					}

				} catch (RaplaException ex) {
					getLogger().error("update-exception", ex);
					bError = true;
				} catch (Exception ex) {
					getLogger().error("update-exception", ex);
					bError = true;
				}
			}
			if (getLogger().isDebugEnabled() && !bError)
				getLogger().debug(
						"GUI update took "
								+ (System.currentTimeMillis() - time) + " ms.");
			if (hasFailed())
				getLogger()
						.error("There was an error while refreshing the displayed data. It could be different from the data stored. Restarting Rapla is recommended.");
		}
	};

	/**
	 * fires update event asynchronous.
	 */
	protected void fireUpdateEvent(UpdateResult evt) {
		if (periodModel != null) {
			try {
				periodModel.update();
			} catch (RaplaException e) {
				getLogger().error("Can't update Period Model", e);
			}
		}
		
		{
			Collection<ModificationListener> modificationListeners = directListenerList;
			Collection<AllocationChangeListener> allocationChangeListeners = Collections.emptyList();
			if (modificationListeners.size() > 0
					|| allocationChangeListeners.size() > 0) {
				new UpdateCommand(modificationListeners,
						allocationChangeListeners, evt).execute();
			}
		}

		{
			Collection<ModificationListener> modificationListeners = getModificationListeners(false);
			Collection<AllocationChangeListener> allocationChangeListeners = getAllocationChangeListeners(false);
			if (modificationListeners.size() > 0
					|| allocationChangeListeners.size() > 0) {
				notifyQueue.enqueue(new UpdateCommand(modificationListeners,
						allocationChangeListeners, evt));
			}
		}
		{
			Collection<ModificationListener> modificationListeners = getModificationListeners(true);
			Collection<AllocationChangeListener> allocationChangeListeners = getAllocationChangeListeners(true);
			if (modificationListeners.size() > 0
					|| allocationChangeListeners.size() > 0) {
				javax.swing.SwingUtilities.invokeLater(new UpdateCommand(
						modificationListeners, allocationChangeListeners, evt));
			}
			if (getLogger().isDebugEnabled()) {
				getLogger().debug("Update event fired");
			}
		}
	}

	/******************************
	 * Query-module *
	 ******************************/
	private Collection<Allocatable> getVisibleAllocatables(
			ClassificationFilter[] filters) throws RaplaException {
		Collection<Allocatable> allocatables = new ArrayList<Allocatable>();
		synchronized (operator.getLock()) {
			Collection<Allocatable> objects = operator
					.getObjects(Allocatable.class);
			allocatables.addAll(objects);
		}
		Iterator<Allocatable> it = allocatables.iterator();
		while (it.hasNext()) {
			Allocatable allocatable = it.next();
			if (workingUser == null || workingUser.isAdmin())
				continue;
			if (!allocatable.canRead(workingUser))
				it.remove();
		}

		removeFilteredClassifications(allocatables, filters);
		return allocatables;
	}

	private Collection<Reservation> getVisibleReservations(User user,
			Date start, Date end, ClassificationFilter[] reservationFilters)
			throws RaplaException {
		Collection<Reservation> reservations = new ArrayList<Reservation>();
		synchronized (operator.getLock()) {
			reservations.addAll(operator.getReservations(user, start, end));
		}
		removeFilteredClassifications(reservations, reservationFilters);

		// Category can_see = getUserGroupsCategory().getCategory(
		// Permission.GROUP_CAN_READ_EVENTS_FROM_OTHERS);
		Iterator<Reservation> it = reservations.iterator();
		boolean canSeeOthers = canReadReservationsFromOthers(user);
		while (it.hasNext()) {
			Reservation r = it.next();
			if (workingUser == null || r.getOwner().equals(workingUser)
					|| canSeeOthers)
				continue;
			Allocatable[] allocatables = r.getAllocatables();
			boolean oneVisibleAllocatable = false;
			for (int i = 0; i < allocatables.length; i++) {
				if (allocatables[i].canRead(workingUser)) {
					oneVisibleAllocatable = true;
					break;
				}
			}
			if (!oneVisibleAllocatable)
				it.remove();
		}
		return reservations;
	}

	private void removeFilteredClassifications(
			Collection<? extends Classifiable> list,
			ClassificationFilter[] filters) {
		if (filters == null)
			return;

		Iterator<? extends Classifiable> it = list.iterator();
		while (it.hasNext()) {
			Classification classification = it.next().getClassification();
			boolean found = false;
			for (int i = 0; i < filters.length; i++) {
				if (filters[i].matches(classification)) {
					found = true;
					break;
				}
			}
			if (!found)
				it.remove();
		}
	}

	public Allocatable[] getAllocatables() throws RaplaException {
		return getAllocatables(null);
	}

	public Allocatable[] getAllocatables(ClassificationFilter[] filters)
			throws RaplaException {
		return (Allocatable[]) getVisibleAllocatables(filters).toArray(
				Allocatable.ALLOCATABLE_ARRAY);
	}

	public Reservation[] getReservations(User user, Date start, Date end,
			ClassificationFilter[] filters) throws RaplaException {
		return (Reservation[]) getVisibleReservations(user, start, end, filters)
				.toArray(Reservation.RESERVATION_ARRAY);
	}

	public boolean canExchangeAllocatables(Reservation reservation) {
		try {
			Allocatable[] all = getAllocatables(null);
			User user = getUser();
			for (int i = 0; i < all.length; i++) {
				if (all[i].canModify(user)) {
					return true;
				}
			}
		} catch (RaplaException ex) {
		}
		return false;
	}

	public Preferences getPreferences() throws RaplaException {
		return getPreferences(getUser());
	}

	public Preferences getPreferences(User user) throws RaplaException {
		synchronized (operator.getLock()) {
			return (Preferences) operator.getPreferences(user);
		}
	}

	public Category getSuperCategory() {
		synchronized (operator.getLock()) {
			return (Category) operator.getSuperCategory();
		}
	}

	public Category getUserGroupsCategory() throws RaplaException {
		Category userGroups = getSuperCategory().getCategory(
				Permission.GROUP_CATEGORY_KEY);
		if (userGroups == null) {
			throw new RaplaException("No category '"
					+ Permission.GROUP_CATEGORY_KEY + "' available");
		}
		return userGroups;
	}

	public Reservation[] getReservations(Allocatable[] allocatables,
			Date start, Date end) throws RaplaException {
		return getReservationsForAllocatable(allocatables, start, end, null);
	}

	public Reservation[] getReservationsForAllocatable(
			Allocatable[] allocatables, Date start, Date end,
			ClassificationFilter[] reservationFilters) throws RaplaException {
		Collection<Reservation> reservations = getVisibleReservations(null,
				start, end, reservationFilters);
		Collection<Allocatable> allocatableSet = null;
		if (allocatables != null) {
			allocatableSet = new HashSet<Allocatable>(Arrays.asList(allocatables));
			Iterator<Reservation> it = reservations.iterator();
			while (it.hasNext()) {
				Reservation reservation = it.next();
				Allocatable[] reservedAllocatables = reservation
						.getAllocatables();
				boolean bFound = false;
				for (int i = 0; i < reservedAllocatables.length; i++) {
					if (allocatableSet.contains(reservedAllocatables[i]))
						bFound = true;
				}
				if (!bFound)
					it.remove();
			}
		}
		return (Reservation[]) reservations
				.toArray(Reservation.RESERVATION_ARRAY);
	}

	public Period[] getPeriods() throws RaplaException {
		synchronized (operator.getLock()) {
			Period[] result = operator.getObjects(Period.class).toArray(
					Period.PERIOD_ARRAY);
			return result;
		}
	}

	public PeriodModel getPeriodModel() throws RaplaException {
		if (periodModel == null) {
			periodModel = new PeriodModelImpl(this);
		}
		return periodModel;
	}

	public DynamicType[] getDynamicTypes(String classificationType)
			throws RaplaException {
		if (classificationType == null) {
			synchronized (operator.getLock()) {
				return operator.getObjects(DynamicType.class).toArray(
						DynamicType.DYNAMICTYPE_ARRAY);
			}
		}
		ArrayList<DynamicType> result = new ArrayList<DynamicType>();
		synchronized (operator.getLock()) {
			Collection<DynamicType> collection = operator
					.getObjects(DynamicType.class);
			for (Iterator<DynamicType> it = collection.iterator(); it.hasNext();) {
				DynamicType type = it.next();
				if (classificationType
						.equals(type
								.getAnnotation(DynamicTypeAnnotations.KEY_CLASSIFICATION_TYPE))) {
					result.add(type);
				}
			}
		}
		return result.toArray(DynamicType.DYNAMICTYPE_ARRAY);
	}

	public DynamicType getDynamicType(String elementKey) throws RaplaException {
		synchronized (operator.getLock()) {
			Collection<DynamicType> collection = operator
					.getObjects(DynamicType.class);
			for (Iterator<DynamicType> it = collection.iterator(); it.hasNext();) {
				DynamicType type = it.next();
				if (type.getElementKey().equals(elementKey))
					return type;
			}
		}
		throw new EntityNotFoundException("No dynamictype with elementKey "
				+ elementKey);
	}

	public User[] getUsers() throws RaplaException {
		synchronized (operator.getLock()) {
			Set<User> users = new TreeSet<User>(new UserComparator(locale));
			users.addAll(operator.getObjects(User.class));
			User[] result = users.toArray(User.USER_ARRAY);
			return result;
		}
	}

	public User getUser(String username) throws RaplaException {
		synchronized (operator.getLock()) {
			User user = operator.getUser(username);
			if (user == null)
				throw new EntityNotFoundException("No User with username "
						+ username);
			return user;
		}
	}

	public Conflict[] getConflicts(Reservation reservation)
			throws RaplaException {
		return conflictFinder.getConflicts(reservation);
	}

	public Conflict[] getConflicts(Collection<Allocatable> allocatables,
			Date startDate) throws RaplaException {
		Collection<Conflict> conflicts = conflictFinder.getConflicts(
				allocatables, (User) null);
		return conflicts.toArray(new Conflict[] {});
	}
	
	public static boolean hasPermissionToAllocate( User user, Appointment appointment,Allocatable allocatable, Reservation original, Date today) {
        if ( user.isAdmin()) {
            return true;
        }
        Date start = appointment.getStart();
        Date end = appointment.getMaxEnd();
        Permission[] permissions = allocatable.getPermissions();
        for ( int i = 0; i < permissions.length; i++) {
            Permission p = permissions[i];
            int accessLevel = p.getAccessLevel();
            if ( (!p.affectsUser( user )) ||  accessLevel< Permission.READ) {
                continue;
            }
            
            if ( accessLevel ==  Permission.ADMIN)
            {
                // user has the right to allocate
                return true;
            }
           
            if ( accessLevel >= Permission.ALLOCATE && p.covers( start, end, today ) ) 
            {
                return true;
            }
            if ( original == null )
            {
                continue;
            }

            // We must check if the changes of the existing appointment
            // are in a permisable timeframe (That should be allowed)

            // 1. check if appointment is old,
            // 2. check if allocatable was already assigned to the appointment
            Appointment originalAppointment = original.findAppointment( appointment );
            if ( originalAppointment == null
                 || !original.hasAllocated( allocatable, originalAppointment)
                 ) {
                continue;
            }

            // 3. check if the appointment has changed during
            // that time
            if ( appointment.matches( originalAppointment ) ) 
            {
                return true;
            }
            if ( accessLevel >= Permission.ALLOCATE )
            {
                Date maxTime =  p.getMaxAllowed( today );
                if (maxTime == null)
                    maxTime = p.getMinAllowed( today);
    
                Date minChange =
                    appointment.getFirstDifference( originalAppointment, maxTime );
                Date maxChange =
                    appointment.getLastDifference( originalAppointment, maxTime );
                //System.out.println ( "minChange: " + minChange + ", maxChange: " + maxChange );
    
                if ( p.covers( minChange, maxChange, today ) ) {
                    return true;
                }
            }
        }
        return false;
    }

	public boolean hasPermissionToAllocate(Appointment appointment,
			Allocatable allocatable) {
		if (workingUser == null) {
			return true;
		}
		try {
			Reservation reservation = appointment.getReservation();
			Reservation originalReservation;
			if (!reservation.isPersistant()) {
				try {
					originalReservation = (Reservation) getPersistant(reservation);
				} catch (EntityNotFoundException ex) {
					// is New
					originalReservation = null;
					return allocatable.canAllocate(workingUser,
							appointment.getStart(), appointment.getMaxEnd(),
							today());
				}
			} else {
				originalReservation = null;
			}
			return hasPermissionToAllocate(workingUser,
					appointment, allocatable, originalReservation, today());
		} catch (RaplaException ex) {
			getLogger().error("Can't get permissions!", ex);
			return false;
		}
	}

	public boolean canReadReservationsFromOthers(User user) {
		if (user == null) {
			return workingUser == null || workingUser.isAdmin();
		}
		if (user.isAdmin()) {
			return true;
		}
		try {
			Category can_see = getUserGroupsCategory().getCategory(
					Permission.GROUP_CAN_READ_EVENTS_FROM_OTHERS);
			return user.belongsTo(can_see);
		} catch (Exception ex) {
			getLogger().error("Can't get permissions!", ex);
		}
		return false;
	}

	@Deprecated
	public Allocatable[] getAllocatableBindings(Appointment forAppointment)
			throws RaplaException {
		return (Allocatable[]) getAllocatableBindings(
				Arrays.asList(getAllocatables()), forAppointment).toArray(
				Allocatable.ALLOCATABLE_ARRAY);
	}

	public Collection<Allocatable> getAllocatableBindings(
			List<Allocatable> allocatables, Appointment forAppointment)
			throws RaplaException {
		return conflictFinder.getAllocatableBindings(allocatables,
				forAppointment);
	}

	/******************************
	 * Login - Module *
	 ******************************/
	public User getUser() throws RaplaException {
		if (this.workingUser == null) {
			throw new RaplaException("no user loged in");
		}
		return this.workingUser;
	}

	public boolean login(String username, char[] password)
			throws RaplaException {
		synchronized (operator.getLock()) {
			try {
				if (!operator.isConnected()) {
					operator.connect(username, password);
				}

			} catch (RaplaSecurityException ex) {
				return false;
			} finally {
				// Clear password
				for (int i = 0; i < password.length; i++)
					password[i] = 0;
			}
			initRefresh();
			User user = operator.getUser(username);
			if (user != null) {
				this.workingUser = user;
				getLogger().info("Login " + user.getUsername());
				return true;
			} else {
				return false;
			}
		}
	
	}

	public boolean canChangePassword() {
		synchronized (operator.getLock()) {
			return operator.canChangePassword();
		}
	}

	public boolean isSessionActive() {
		return (this.workingUser != null);
	}

	public void logout() throws RaplaException {
		if (refreshTimer != null)
		{
			refreshTimer.cancel();
		}

		if (this.workingUser == null || this.originalUser != null)
			return;
		getLogger().info("Logout " + workingUser.getUsername());
		this.workingUser = null;
		// we need to remove the storage update listener, because the disconnect
		// would trigger a restart otherwist
		operator.removeStorageUpdateListener(this);
		operator.disconnect();
		// now we can add it again
		operator.addStorageUpdateListener(this);
		Runtime.getRuntime().gc();
	}

	@SuppressWarnings("unchecked")
	public void changePassword(User user, char[] oldPassword, char[] newPassword)
			throws RaplaException {
		synchronized (operator.getLock()) {
			operator.changePassword((RefEntity<User>) user, oldPassword,
					newPassword);
		}
	}

	/******************************
	 * Modification-module *
	 ******************************/
	public <T> RaplaMap<T> newRaplaMap(Map<String, T> map) {
		return new RaplaMapImpl<T>(map);
	}

	public <T> RaplaMap<T> newRaplaMap(Collection<T> col) {
		return new RaplaMapImpl<T>(col);
	}

	public CalendarModelConfiguration newRaplaCalendarModel(RaplaMap<? extends RaplaObject> selected,
			ClassificationFilter[] allocatableFilter,
			ClassificationFilter[] eventFilter, String title, Date startDate,
			Date endDate, Date selectedDate, String view, RaplaMap<String> optionMap) {
		boolean defaultResourceTypes;
		boolean defaultEventTypes;

		int eventTypes = 0;
		int resourceTypes = 0;
		defaultResourceTypes = true;
		defaultEventTypes = true;
		List<ClassificationFilter> filter = new ArrayList<ClassificationFilter>();
		if (allocatableFilter != null) {
			for (ClassificationFilter entry : allocatableFilter) {
				ClassificationFilter clone = entry.clone();
				filter.add(clone);
				resourceTypes++;
				if (entry.ruleSize() > 0) {
					defaultResourceTypes = false;
				}
			}
		}
		if (eventFilter != null) {
			for (ClassificationFilter entry : eventFilter) {
				ClassificationFilter clone = entry.clone();
				filter.add(clone);
				eventTypes++;
				if (entry.ruleSize() > 0) {
					defaultEventTypes = false;
				}
			}
		}

		try {
			DynamicType[] allEventTypes;
			allEventTypes = getDynamicTypes(DynamicTypeAnnotations.VALUE_RESERVATION_CLASSIFICATION);
			if (allEventTypes.length > eventTypes && eventFilter != null) {
				defaultEventTypes = false;
			}
			final DynamicType[] allTypes = getDynamicTypes(null);
			final int allResourceTypes = allTypes.length - allEventTypes.length;
			if (allResourceTypes > resourceTypes && allocatableFilter != null) {
				defaultResourceTypes = false;
			}
		} catch (RaplaException e) {
			getLogger().warn("Could not set default filters", e);
		}

		final ClassificationFilter[] filterArray = filter
				.toArray(ClassificationFilter.CLASSIFICATIONFILTER_ARRAY);
		return new CalendarModelConfigurationImpl(selected, filterArray,
				defaultResourceTypes, defaultEventTypes, title, startDate,
				endDate, selectedDate, view, optionMap);
	}

	public Reservation newReservation() throws RaplaException {
		Date today = today();
		ReservationImpl reservation = new ReservationImpl(today, today);
		Classification classification = getDynamicTypes(DynamicTypeAnnotations.VALUE_RESERVATION_CLASSIFICATION)[0]
				.newClassification();
		reservation.setClassification(classification);
		setNew(reservation);
		return reservation;
	}

	public Appointment newAppointment(Date startDate, Date endDate)
			throws RaplaException {
		AppointmentImpl appointment = new AppointmentImpl(startDate, endDate);
		setNew(appointment);
		return appointment;
	}

	public Appointment newAppointment(Date startDate, Date endDate,
			RepeatingType repeatingType, int repeatingDuration)
			throws RaplaException {
		AppointmentImpl appointment = new AppointmentImpl(startDate, endDate,
				repeatingType, repeatingDuration);
		setNew(appointment);
		return appointment;
	}

	public Allocatable newResource() throws RaplaException {
		return newAllocatable(false);
	}

	public Allocatable newPerson() throws RaplaException {
		return newAllocatable(true);
	}

	private Allocatable newAllocatable(boolean isPerson) throws RaplaException {
		Date today = today();
		AllocatableImpl allocatable = new AllocatableImpl(today, today);
		String classificationType = isPerson ? DynamicTypeAnnotations.VALUE_PERSON_CLASSIFICATION
				: DynamicTypeAnnotations.VALUE_RESOURCE_CLASSIFICATION;
		DynamicType[] dynamicTypes = getDynamicTypes(classificationType);
		DynamicType dynamicType = dynamicTypes[0];
		Classification classification = dynamicType.newClassification();
		User user = getUser();
		allocatable.addPermission(allocatable.newPermission());
		if (!user.isAdmin()) {
			Permission permission = allocatable.newPermission();
			permission.setUser(user);
			permission.setAccessLevel(Permission.ADMIN);
			allocatable.addPermission(permission);
		}
		allocatable.setClassification(classification);
		setNew(allocatable);
		return allocatable;
	}

	public Period newPeriod() throws RaplaException {
		PeriodImpl period = new PeriodImpl();
		Date today = today();
		period.setStart(DateTools.cutDate(today));
		period.setEnd(DateTools.fillDate(today));
		setNew(period);
		return period;
	}

	public Date today() {
		return operator.today();
	}

	public Category newCategory() throws RaplaException {
		CategoryImpl category = new CategoryImpl();
		setNew(category);
		return category;
	}

	private Attribute createStringAttribute(String key, String name)
			throws RaplaException {
		Attribute attribute = newAttribute(AttributeType.STRING);
		attribute.setKey(key);
		attribute.getName().setName(i18n.getLang(), i18n.getString(name));
		return attribute;
	}

	public DynamicType newDynamicType(String classificationType)
			throws RaplaException {
		DynamicTypeImpl dynamicType = new DynamicTypeImpl();
		dynamicType.setAnnotation("classification-type", classificationType);
		dynamicType.setElementKey(createDynamicTypeKey(classificationType));
		setNew(dynamicType);
		if (classificationType
				.equals(DynamicTypeAnnotations.VALUE_RESOURCE_CLASSIFICATION)) {
			dynamicType.addAttribute(createStringAttribute("name", "name"));
			dynamicType.setAnnotation(DynamicTypeAnnotations.KEY_NAME_FORMAT,
					"{name}");
			dynamicType.setAnnotation(DynamicTypeAnnotations.KEY_COLORS,
					"automatic");
		} else if (classificationType
				.equals(DynamicTypeAnnotations.VALUE_RESERVATION_CLASSIFICATION)) {
			dynamicType.addAttribute(createStringAttribute("name",
					"reservation.name"));
			dynamicType.setAnnotation(DynamicTypeAnnotations.KEY_NAME_FORMAT,
					"{name}");
			dynamicType.setAnnotation(DynamicTypeAnnotations.KEY_COLORS, null);
		} else if (classificationType
				.equals(DynamicTypeAnnotations.VALUE_PERSON_CLASSIFICATION)) {
			dynamicType
					.addAttribute(createStringAttribute("surname", "surname"));
			dynamicType.addAttribute(createStringAttribute("forename",
					"forename"));
			dynamicType.addAttribute(createStringAttribute("email", "email"));
			dynamicType.setAnnotation(DynamicTypeAnnotations.KEY_NAME_FORMAT,
					"{surname} {forename}");
			dynamicType.setAnnotation(DynamicTypeAnnotations.KEY_COLORS, null);
		}
		return dynamicType;
	}

	public Attribute newAttribute(AttributeType attributeType)
			throws RaplaException {
		AttributeImpl attribute = new AttributeImpl(attributeType);
		setNew(attribute);
		return attribute;
	}

	public User newUser() throws RaplaException {
		UserImpl user = new UserImpl();
		setNew(user);
		Category modifyPreferences = getUserGroupsCategory().getCategory(
				Permission.GROUP_MODIFY_PREFERENCES_KEY);
		if (modifyPreferences != null) {
			user.addGroup(modifyPreferences);
		}
		Category readOthers = getUserGroupsCategory().getCategory(
				Permission.GROUP_CAN_READ_EVENTS_FROM_OTHERS);
		if (readOthers != null) {
			user.addGroup(readOthers);
		}
		return user;
	}

	private String createDynamicTypeKey(String classificationType)
			throws RaplaException {
		DynamicType[] dts = getDynamicTypes(classificationType);
		int max = 1;
		for (int i = 0; i < dts.length; i++) {
			String key = dts[i].getElementKey();
			int len = classificationType.length();
			if (key.indexOf(classificationType) >= 0 && key.length() > len
					&& Character.isDigit(key.charAt(len))) {
				try {
					int value = Integer.valueOf(key.substring(len)).intValue();
					if (value >= max)
						max = value + 1;
				} catch (NumberFormatException ex) {
				}
			}
		}
		return classificationType + (max);
	}

	private void setNew(RefEntity<?> entity) throws RaplaException {
		setNew(entity, null);
	}

	/** recursivly set new ids */
	private <T extends RefEntity<?>> void setNew(T entity, User user)
			throws RaplaException {

		if (entity.getSubEntities().hasNext()) {
			throw new RaplaException(
					"The current Rapla Version doesnt support cloning entities with sub-entities. (Except reservations)");
		}

		RaplaType raplaType = entity.getRaplaType();
		synchronized (operator.getLock()) {
			entity.setId(operator.createIdentifier(raplaType));
		}

		entity.setVersion(0);
		if (getLogger() != null && getLogger().isDebugEnabled()) {
			getLogger().debug("new " + entity.getId());
		}

		if (entity instanceof Ownable) {
			if (user == null)
				user = getUser();
			((Ownable) entity).setOwner(user);
		}
	}

	public void checkReservation(Reservation reservation) throws RaplaException {
		if (reservation.getAppointments().length == 0) {
			throw new RaplaException(i18n.getString("error.no_appointment"));
		}

		if (reservation.getName(i18n.getLocale()).trim().length() == 0) {
			throw new RaplaException(
					i18n.getString("error.no_reservation_name"));
		}
	}

	public <T extends Entity<T>> T edit(Entity<T> obj) throws RaplaException {
		if (obj == null)
			throw new NullPointerException("Can't edit null objects");
		synchronized (operator.getLock()) {
			RefEntity<T> editObject = operator.editObject((RefEntity<T>) obj,
					workingUser);
			return editObject.cast();
		}
	}

	@SuppressWarnings("unchecked")
	private <T extends Entity<T>> T _clone(Entity<T> obj) throws RaplaException {
		// We assume that all type preconditions are met, e.g. obj implements
		// Refentity and Memementable
		Entity<T> deepClone = ((Mementable<T>) obj).deepClone();
		T clone = deepClone.cast();

		RaplaType raplaType = clone.getRaplaType();
		if (raplaType == Appointment.TYPE) {
			// Hack for 1.6 compiler compatibility
			Object temp = clone;
			((AppointmentImpl) temp).removeParent();
		}
		if (raplaType == Category.TYPE) {
			// Hack for 1.6 compiler compatibility
			Object temp = clone;
			((CategoryImpl) temp).removeParent();
		}

		setNew((RefEntity<T>) clone, this.workingUser);
		return clone;
	}

	@SuppressWarnings("unchecked")
	public <T extends Entity<T>> T clone(Entity<T> obj) throws RaplaException {
		if (obj == null)
			throw new NullPointerException("Can't clone null objects");

		if (obj.getRaplaType() == Appointment.TYPE) {
			T _clone = _clone(obj);
			// Hack for 1.6 compiler compatibility
			Object temp = _clone;
			((AppointmentImpl) temp).setParent(null);
			return _clone;
		} else if (obj.getRaplaType() == Reservation.TYPE) {
			// Hack for 1.6 compiler compatibility
			Object temp = obj;
			Entity<Reservation> clonedReservation = cloneReservation((Entity<Reservation>) temp);
			// Hack for 1.6 compiler compatibility
			T result = (T) ((Object) clonedReservation);
			return result;
		}

		try {
			T _clone = _clone(obj);
			return _clone.cast();
		} catch (ClassCastException ex) {
			throw new RaplaException("This entity can't be cloned ", ex);
		} finally {
		}

	}

	@SuppressWarnings({ "unchecked" })
	private Reservation cloneReservation(Entity<Reservation> obj)
			throws RaplaException {
		Reservation clone = (Reservation) ((Mementable<Reservation>) obj)
				.deepClone();
		HashMap<Allocatable[], Appointment[]> restrictions = new HashMap<Allocatable[], Appointment[]>();
		Allocatable[] allocatables = clone.getAllocatables();

		for (int i = 0; i < allocatables.length; i++) {
			restrictions.put(allocatables,
					clone.getRestriction(allocatables[i]));
		}

		Appointment[] clonedAppointments = clone.getAppointments();
		for (int i = 0; i < clonedAppointments.length; i++) {
			setNew((RefEntity<Appointment>) clonedAppointments[i],
					this.workingUser);
			clone.removeAppointment(clonedAppointments[i]);
		}

		setNew((RefEntity<Reservation>) clone, this.workingUser);
		for (int i = 0; i < clonedAppointments.length; i++) {
			clone.addAppointment(clonedAppointments[i]);
		}

		for (int i = 0; i < allocatables.length; i++) {
			Appointment[] appointments = restrictions.get(allocatables[i]);
			if (appointments != null) {
				clone.setRestriction(allocatables[i], appointments);
			}
		}

		return clone;
	}

	public <T> T getPersistant(Entity<T> obj) throws RaplaException {
		synchronized (operator.getLock()) {
			T persistant = operator.getPersistant((RefEntity<T>) obj);
			return persistant;
		}
	}

	public void store(Entity<?> obj) throws RaplaException {
		if (obj == null)
			throw new NullPointerException("Can't store null objects");
		storeObjects(new Entity[] { obj });
	}

	public void remove(Entity<?> obj) throws RaplaException {
		if (obj == null)
			throw new NullPointerException("Can't remove null objects");
		removeObjects(new Entity[] { obj });
	}

	public void storeObjects(Entity<?>[] obj) throws RaplaException {
		storeAndRemove(obj, Entity.ENTITY_ARRAY);
	}

	public void removeObjects(Entity<?>[] obj) throws RaplaException {
		storeAndRemove(Entity.ENTITY_ARRAY, obj);
	}

	@SuppressWarnings("unchecked")
	public void storeAndRemove(Entity<?>[] storeObjects,
			Entity<?>[] removedObjects) throws RaplaException {
		if (storeObjects.length == 0 && removedObjects.length == 0)
			return;
		long time = System.currentTimeMillis();
		for (int i = 0; i < storeObjects.length; i++) {
			if (storeObjects[i] == null) {
				throw new RaplaException("Stored Objects cant be null");
			}
			if (storeObjects[i].getRaplaType().equals(Reservation.TYPE)) {
				checkReservation((Reservation) storeObjects[i]);
			}
		}

		for (int i = 0; i < removedObjects.length; i++) {
			if (removedObjects[i] == null) {
				throw new RaplaException("Removed Objects cant be null");
			}
		}

		synchronized (operator.getLock()) {
			ArrayList<RefEntity<?>> storeList = new ArrayList<RefEntity<?>>();
			ArrayList<RefEntity<?>> removeList = new ArrayList<RefEntity<?>>();
			for (Entity<?> toStore : storeObjects) {
				storeList.add((RefEntity<?>) toStore);
			}
			for (Entity<?> toRemove : removedObjects) {
				removeList.add((RefEntity<?>) toRemove);
			}
			operator.storeAndRemove(storeList, removeList,
					(RefEntity<User>) workingUser);
		}
		if (getLogger().isDebugEnabled())
			getLogger().debug(
					"Storing took " + (System.currentTimeMillis() - time)
							+ " ms.");
	}

	

}
