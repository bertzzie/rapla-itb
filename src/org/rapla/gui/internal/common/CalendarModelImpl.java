/*--------------------------------------------------------------------------*
 | Copyright (C) 2006  Christopher Kohlhaas                                 |
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

package org.rapla.gui.internal.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.rapla.components.util.Assert;
import org.rapla.components.util.TimeInterval;
import org.rapla.components.xmlbundle.I18nBundle;
import org.rapla.entities.EntityNotFoundException;
import org.rapla.entities.Named;
import org.rapla.entities.RaplaObject;
import org.rapla.entities.RaplaType;
import org.rapla.entities.User;
import org.rapla.entities.configuration.CalendarModelConfiguration;
import org.rapla.entities.configuration.Preferences;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.AppointmentBlock;
import org.rapla.entities.domain.AppointmentBlockStartComparator;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.dynamictype.Classification;
import org.rapla.entities.dynamictype.ClassificationFilter;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.dynamictype.DynamicTypeAnnotations;
import org.rapla.facade.ClientFacade;
import org.rapla.facade.Conflict;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.framework.RaplaLocale;
import org.rapla.plugin.abstractcalendar.AbstractHTMLCalendarPage;
import org.rapla.plugin.autoexport.AutoExportPlugin;
import org.rapla.plugin.export2ical.Export2iCalPlugin;
import org.rapla.plugin.weekview.WeekViewFactory;

public class CalendarModelImpl implements CalendarSelectionModel
{
    Date startDate;
    Date endDate;
    Date selectedDate;
    List<RaplaObject> selectedObjects = new ArrayList<RaplaObject>();
    String title;
    ClientFacade m_facade;
    String selectedView;
    I18nBundle i18n;
    RaplaContext context;
    RaplaLocale raplaLocale;
    User user;
    Map<String,String> optionMap = new HashMap<String,String>();

    boolean defaultEventTypes = true;
    boolean defaultResourceTypes = true;
    Collection<TimeInterval> timeIntervals = Collections.emptyList();
    Collection<Allocatable> markedAllocatables = Collections.emptyList();
    
    Map<DynamicType,ClassificationFilter> reservationFilter = new LinkedHashMap<DynamicType, ClassificationFilter>();
    Map<DynamicType,ClassificationFilter> allocatableFilter = new LinkedHashMap<DynamicType, ClassificationFilter>();
    
    public CalendarModelImpl(RaplaContext sm, User user) throws RaplaException {
        this.context = sm;
        this.raplaLocale = (RaplaLocale) sm.lookup(RaplaLocale.ROLE);
        i18n = (I18nBundle)sm.lookup(I18nBundle.ROLE + "/org.rapla.RaplaResources");
        m_facade = (ClientFacade) sm.lookup(ClientFacade.ROLE);
        if ( user == null && m_facade.isSessionActive()) {
            user = m_facade.getUser();
        }
        setSelectedDate( m_facade.today());
        DynamicType[] types = m_facade.getDynamicTypes( DynamicTypeAnnotations.VALUE_RESOURCE_CLASSIFICATION);
        if ( types.length == 0 ) {
            types = m_facade.getDynamicTypes( DynamicTypeAnnotations.VALUE_PERSON_CLASSIFICATION);
        }
        setSelectedObjects( Collections.singletonList( types[0]) );
        setViewId( WeekViewFactory.WEEK_VIEW);
        this.user = user;
        if ( user != null && !user.isAdmin()) {
            selectUser( user );
        }
        optionMap.put( AbstractHTMLCalendarPage.SAVE_SELECTED_DATE, "false");
        resetExports();
    }
    
    public void resetExports() 
    {
        setTitle(null);
        setOption( AbstractHTMLCalendarPage.SHOW_NAVIGATION_ENTRY, "true");
        setOption(AutoExportPlugin.HTML_EXPORT, "false");
        setOption(Export2iCalPlugin.ICAL_EXPORT, "false");                       
    }


    private boolean setConfiguration(CalendarModelConfiguration config, final Map<String,String> alternativOptions) throws RaplaException {
        selectedObjects = new ArrayList<RaplaObject>();
        allocatableFilter.clear();
        reservationFilter.clear();
        if ( config == null)
        {
            defaultEventTypes = true;
            defaultResourceTypes = true;
            return true;
        }
        else
        {
            defaultEventTypes = config.isDefaultEventTypes();
            defaultResourceTypes = config.isDefaultResourceTypes();
        }
        boolean couldResolveAllEntities = true;
       
        // get filter
        title = config.getTitle();
        selectedView = config.getView();
        //selectedObjects
        optionMap = new HashMap<String,String>();
        if ( config.getOptionMap() != null)
        {
            optionMap.putAll( config.getOptionMap());
        }
        if (alternativOptions != null )
        {
            optionMap.putAll( alternativOptions);
        } 
        ClassificationFilter[] filter = config.getFilter();
        final String saveDate = (String)optionMap.get( AbstractHTMLCalendarPage.SAVE_SELECTED_DATE);
        if ( config.getSelectedDate() != null && (saveDate == null || saveDate.equals("true"))) {
            setSelectedDate( config.getSelectedDate() );
        }
        else
        {
            setSelectedDate( m_facade.today());
        }
        if ( config.getStartDate() != null) {
            setStartDate( config.getStartDate() );
        }
        if ( config.getEndDate() != null) {
            setEndDate( config.getEndDate() );
        }
        selectedObjects.addAll( config.getSelected());
        for ( ClassificationFilter f:filter)
        {
            final DynamicType type = f.getType();
            final String annotation = type.getAnnotation(DynamicTypeAnnotations.KEY_CLASSIFICATION_TYPE);
            boolean eventType = annotation != null &&annotation.equals( DynamicTypeAnnotations.VALUE_RESERVATION_CLASSIFICATION);
            Map<DynamicType,ClassificationFilter> map = eventType ? reservationFilter : allocatableFilter;
            map.put(type, f);
        }
        
        return couldResolveAllEntities;
    }

    public User getUser() {
        return user;
    }

    public CalendarModelConfiguration createConfiguration() throws RaplaException {
        ClassificationFilter[] allocatableFilter = getAllocatableFilter();
        ClassificationFilter[] eventFilter = getReservationFilter();
        return createConfiguration(allocatableFilter, eventFilter);
    }
    
    public CalendarModelConfiguration createConfiguration(ClassificationFilter[] allocatableFilter, ClassificationFilter[] eventFilter) throws RaplaException {
        String viewName = selectedView;
        Set<RaplaObject> selected = new HashSet<RaplaObject>( );
        
        for (Iterator<RaplaObject> it = selectedObjects.iterator();it.hasNext();) {
            RaplaObject object = it.next();
			if ( !(object instanceof Conflict)) 
            {
				//  throw new RaplaException("Storing the conflict view is not possible with Rapla.");
				selected.add( object );
            }
        }

        final Date selectedDate = getSelectedDate();
        final Date startDate = getStartDate();
        final Date endDate = getEndDate();
        return m_facade.newRaplaCalendarModel( m_facade.newRaplaMap(selected), allocatableFilter,eventFilter, title, startDate, endDate, selectedDate, viewName, m_facade.newRaplaMap(optionMap));
    }

    public void setReservationFilter(ClassificationFilter[] array) {
        reservationFilter.clear();
        try {
            defaultEventTypes = createConfiguration(null,array).isDefaultEventTypes();
        } catch (RaplaException e) {
            // DO Not set the types
        }
        for (ClassificationFilter entry: array)
        {   
            final DynamicType type = entry.getType();
            reservationFilter.put( type, entry);
        }
    }

    public void setAllocatableFilter(ClassificationFilter[] array) {
        allocatableFilter.clear();
        try {
            defaultResourceTypes = createConfiguration(array,null).isDefaultResourceTypes();
        } catch (RaplaException e) {
            // DO Not set the types
        }
        for (ClassificationFilter entry: array)
        {   
            final DynamicType type = entry.getType();
            allocatableFilter.put( type, entry);
        }
    }
   
    /* (non-Javadoc)
	 * @see org.rapla.calendarview.CalendarModel#getSelectedDate()
	 */
    public Date getSelectedDate() {
        return selectedDate;
    }

    /* (non-Javadoc)
	 * @see org.rapla.calendarview.CalendarModel#setSelectedDate(java.util.Date)
	 */
    public void setSelectedDate(Date date) {
        if ( date == null)
            throw new IllegalStateException("Date can't be null");
        this.selectedDate = date;
    }

    /* (non-Javadoc)
	 * @see org.rapla.calendarview.CalendarModel#getStartDate()
	 */
    public Date getStartDate() {
        return startDate;
    }

    /* (non-Javadoc)
	 * @see org.rapla.calendarview.CalendarModel#setStartDate(java.util.Date)
	 */
    public void setStartDate(Date date) {
        this.startDate = date;
    }

    /* (non-Javadoc)
	 * @see org.rapla.calendarview.CalendarModel#getEndDate()
	 */
    public Date getEndDate() {
        return endDate;
    }

    /* (non-Javadoc)
	 * @see org.rapla.calendarview.CalendarModel#setEndDate(java.util.Date)
	 */
    public void setEndDate(Date date) {
        this.endDate = date;
    }

    /* (non-Javadoc)
	 * @see org.rapla.calendarview.CalendarModel#getTitle()
	 */
    public String getTitle() {
        return title;
    }

    /* (non-Javadoc)
	 * @see org.rapla.calendarview.CalendarModel#setTitle(java.lang.String)
	 */
    public void setTitle(String title) {
        this.title = title;
    }

    /* (non-Javadoc)
	 * @see org.rapla.calendarview.CalendarModel#setView(java.lang.String)
	 */
    public void setViewId(String view) {
        this.selectedView = view;
    }

    /* (non-Javadoc)
	 * @see org.rapla.calendarview.CalendarModel#getView()
	 */
    public String getViewId() {
        return this.selectedView;
    }

    /* (non-Javadoc)
	 * @see org.rapla.calendarview.CalendarModel#getNonEmptyTitle()
	 */
    public String getNonEmptyTitle() {
        if (getTitle() != null && getTitle().trim().length()>0)
            return getTitle();


        String types = "";
        /*
        String dateString = getRaplaLocale().formatDate(getSelectedDate());
        if  ( isListingAllocatables()) {
            try {
                Collection list = getSelectedObjectsAndChildren();
                if (list.size() == 1) {
                    Object obj = list.iterator().next();
                    if (!( obj instanceof DynamicType))
                    {
                        types = getI18n().format("allocation_view",getName( obj ),dateString);
                    }
                }

            } catch (RaplaException ex) {
            }
            if ( types == null )
                types = getI18n().format("allocation_view",  getI18n().getString("resources_persons"));
        } else if ( isListingReservations()) {
             types =  getI18n().getString("reservations");
        } else {
            types = "unknown";
        }
        */

        return types;
    }

    public String getName(Object object) {
        if (object == null)
            return "";
        if (object instanceof Named) {
            String name = ((Named) object).getName(getI18n().getLocale());
            return (name != null) ? name : "";
        }
        return object.toString();
    }

    private Collection<Allocatable> getFilteredAllocatables() throws RaplaException {
        List<Allocatable> list = new ArrayList<Allocatable>();
        Allocatable[] all = m_facade.getAllocatables();
        for (int i=0;i<all.length;i++) {
            if ( isInFilter( all[i]) ) {
                list.add( all[i]);
            }
        }
        return list;
    }

    private boolean isInFilter( Allocatable classifiable) {
        final Classification classification = classifiable.getClassification();
        final DynamicType type = classification.getType();
        final ClassificationFilter classificationFilter = allocatableFilter.get( type);
        if ( classificationFilter != null)
        {
            final boolean matches = classificationFilter.matches(classification);
            return matches;
        }
        else
        {
            return defaultResourceTypes;
        }
    }
    
    public Collection<RaplaObject> getSelectedObjectsAndChildren() throws RaplaException
    {
        Assert.notNull(selectedObjects);

        ArrayList<DynamicType> dynamicTypes = new ArrayList<DynamicType>();
        for (Iterator<RaplaObject> it = selectedObjects.iterator();it.hasNext();)
        {
            Object obj = it.next();
            if (obj instanceof DynamicType) {
                dynamicTypes.add ((DynamicType)obj);
            }
        }

        HashSet<RaplaObject> result = new HashSet<RaplaObject>();
        result.addAll( selectedObjects );
        
        boolean allAllocatablesSelected = selectedObjects.contains( ALLOCATABLES_ROOT);
        
        Collection<Allocatable> filteredList = getFilteredAllocatables();
        for (Iterator<Allocatable> it = filteredList.iterator();it.hasNext();)
        {
        	Allocatable oneSelectedItem =  it.next();
            if ( selectedObjects.contains(oneSelectedItem)) {
                continue;
            }
            Classification classification = oneSelectedItem.getClassification();
            if ( classification == null)
            {
            	continue;
            }
             if ( allAllocatablesSelected || dynamicTypes.contains(classification.getType()))
             {
                result.add( oneSelectedItem );
                continue;
            }
        }

        return result;
    }


    /* (non-Javadoc)
	 * @see org.rapla.calendarview.CalendarModel#setSelectedObjects(java.util.List)
	 */
    public void setSelectedObjects(Collection<? extends Object> selectedObjects) {
        this.selectedObjects = retainRaplaObjects(selectedObjects);
    }

    private List<RaplaObject> retainRaplaObjects(Collection<? extends Object> list ){
        List<RaplaObject> result = new ArrayList<RaplaObject>();
        for ( Iterator<? extends Object> it = list.iterator();it.hasNext();) {
            Object obj = it.next();
            if ( obj instanceof RaplaObject) {
                result.add( (RaplaObject)obj );
            }
        }
        return result;
    }



    public Collection<RaplaObject> getSelectedObjects()
    {
        return selectedObjects;
    }

    /* (non-Javadoc)
	 * @see org.rapla.calendarview.CalendarModel#getReservationFilter()
	 */
    public ClassificationFilter[] getReservationFilter() throws RaplaException 
    {
        Collection<ClassificationFilter> filter ;
        if ( isDefaultEventTypes())
        {
            filter = new ArrayList<ClassificationFilter>();
            for (DynamicType type :m_facade.getDynamicTypes( DynamicTypeAnnotations.VALUE_RESERVATION_CLASSIFICATION))
            {
                filter.add( type.newClassificationFilter());
            }
        }
        else
        {
            filter = reservationFilter.values();
        }
        return filter.toArray(ClassificationFilter.CLASSIFICATIONFILTER_ARRAY);
    }

    /* (non-Javadoc)
     * @see org.rapla.calendarview.CalendarModel#getAllocatableFilter()
     */
    public ClassificationFilter[] getAllocatableFilter() throws RaplaException {
        Collection<ClassificationFilter> filter ;
        if ( isDefaultResourceTypes())
        {
            filter = new ArrayList<ClassificationFilter>();
            for (DynamicType type :m_facade.getDynamicTypes( DynamicTypeAnnotations.VALUE_RESOURCE_CLASSIFICATION))
            {
                filter.add( type.newClassificationFilter());
            }
            for (DynamicType type :m_facade.getDynamicTypes( DynamicTypeAnnotations.VALUE_PERSON_CLASSIFICATION))
            {
                filter.add( type.newClassificationFilter());
            }
            
        }
        else
        {
            filter = allocatableFilter.values();
        }
        return filter.toArray(ClassificationFilter.CLASSIFICATIONFILTER_ARRAY);
    }

    public CalendarSelectionModel clone()  {
        CalendarModelImpl clone;
        try
        {
            clone = (CalendarModelImpl )new CalendarModelImpl(context, user);
            CalendarModelConfiguration config = createConfiguration();
            Map<String, String> alternativOptions = null;
			clone.setConfiguration( config, alternativOptions);
        }
        catch ( RaplaException e )
        {
            throw new IllegalStateException( e.getMessage() );
        }
        return clone;
    }

    /* (non-Javadoc)
	 * @see org.rapla.calendarview.CalendarModel#getReservations(java.util.Date, java.util.Date)
	 */
    public Reservation[] getReservations() throws RaplaException {
        return getReservations( getStartDate(), getEndDate() );
    }

    public Reservation[] getReservations(Date startDate, Date endDate) throws RaplaException 
    {
        return (Reservation[]) getReservationsAsList( startDate, endDate ).toArray( Reservation.RESERVATION_ARRAY);
    }

    private boolean hasConflict( Reservation reservation,Conflict[] conflict ) {
        for (int j=0;j<conflict.length;j++) {
            Conflict allocatable = conflict[j];
            if (reservation.equals( allocatable.getReservation1() ) || reservation.equals( allocatable.getReservation2())) {
               return true;
            }
        }
        return false;
    }

    private void restrict(Collection<Reservation> reservations,User[] users) throws RaplaException {
        HashSet<User> usersSet = new HashSet<User>(Arrays.asList(users));
        for ( Iterator<Reservation> it = reservations.iterator();it.hasNext();) {
            Reservation event = it.next();
            if ( !usersSet.contains( event.getOwner() )) {
                it.remove();
            }
        }
    }

    private void restrict(Collection<Reservation> reservations,Conflict[] conflicts) throws RaplaException {
        for ( Iterator<Reservation> it = reservations.iterator();it.hasNext();) {
            Reservation event = it.next();
            if ( !hasConflict( event, conflicts )) {
                it.remove();
            }
        }
    }

    private Collection<Reservation> getRestrictedReservations( Allocatable[] allocatables,Date start, Date end) throws RaplaException 
    {
        ClassificationFilter[] reservationFilter2 = getReservationFilter();
        if ( isDefaultEventTypes())
        {
            reservationFilter2 = null;
        }
        
        Reservation[] reservations =m_facade.getReservationsForAllocatable(allocatables, start, end,reservationFilter2 );
        return Arrays.asList( reservations );
    }

    private List<Reservation> getReservationsAsList(Date start, Date end) throws RaplaException 
    {
    	Allocatable[] allocatables = getSelectedAllocatables();
    	if ( allocatables.length == 0)
    	{
    		allocatables = null;
    	}
    	List<Reservation> reservations = new ArrayList<Reservation>(getRestrictedReservations( allocatables, start, end));
//        Set reservationTypes = getSelectedTypes( DynamicTypeAnnotations.VALUE_RESERVATION_CLASSIFICATION);
//        if ( reservationTypes.size() > 0 ) {
//            restrictReservationTypes( reservations, reservationTypes);
//        }
 
        User[] users = getSelectedUsers();
        if ( users.length> 0 || isOnlyCurrentUserSelected()) {
            restrict( reservations, users );
        }
        Conflict[] conflicts = getSelectedConflicts();
        if ( conflicts.length > 0) {
            restrict( reservations, conflicts );
        }
        return reservations;
    }

    /* (non-Javadoc)
	 * @see org.rapla.calendarview.CalendarModel#getAllocatables()
	 */
    public Allocatable[] getSelectedAllocatables() throws RaplaException {
        Collection<Allocatable> result = new HashSet<Allocatable>();
        Iterator<RaplaObject> it = getSelectedObjectsAndChildren().iterator();
        while (it.hasNext()) {
            RaplaObject object  =  it.next();
            if ( object.getRaplaType().equals( Allocatable.TYPE )) {
                result.add( (Allocatable)object  );
            }
            if ( object.getRaplaType().equals( Conflict.TYPE )) {
                result.add( (Allocatable)((Conflict)object).getAllocatable() );
            }
        }
         return result.toArray(Allocatable.ALLOCATABLE_ARRAY);
   }

    public User[] getSelectedUsers()  {
        User currentUser = getUser();
        if ( currentUser != null && !m_facade.canReadReservationsFromOthers( currentUser))
        {
            return new User[] {currentUser};
        }
        return (User[]) getSelected(User.TYPE).toArray(User.USER_ARRAY);
   }

    public Conflict[] getSelectedConflicts()  throws RaplaException {
        return (Conflict[]) getSelected(Conflict.TYPE).toArray(Conflict.CONFLICT_ARRAY);
   }

    public Set<DynamicType> getSelectedTypes(String classificationType) throws RaplaException {
        Set<DynamicType> result = new HashSet<DynamicType>();
        Iterator<RaplaObject> it = getSelectedObjectsAndChildren().iterator();
        while (it.hasNext()) {
            RaplaObject object  =  it.next();
            if ( object.getRaplaType().equals( DynamicType.TYPE )) {
                if (classificationType == null || (( DynamicType) object).getAnnotation( DynamicTypeAnnotations.KEY_CLASSIFICATION_TYPE).equals( classificationType))
                {
                    result.add((DynamicType) object  );
                }
            }
        }
         return result;
   }
    
    public Set<RaplaObject> getSelected(RaplaType type)  {
        Set<RaplaObject> result = new HashSet<RaplaObject>();
        Iterator<RaplaObject> it = getSelectedObjects().iterator();
        while (it.hasNext()) {
            RaplaObject object  =  it.next();
            if ( object.getRaplaType().equals( type )) {
                result.add( object  );
            }
        }
         return result;
   }

   

    

    protected I18nBundle getI18n() {
        return i18n;
    }

    protected RaplaLocale getRaplaLocale() {
        return raplaLocale;
    }

    public boolean isOnlyCurrentUserSelected() {
        User[] users = getSelectedUsers();
        User currentUser = getUser();

    
            
        return ( users.length == 1 && users[0].equals( currentUser));
    }

    public void selectUser(User user) {
        for (Iterator<RaplaObject> it = selectedObjects.iterator();it.hasNext();) {
            RaplaObject obj = it.next();
            if (obj.getRaplaType().equals( User.TYPE) ) {
                it.remove();
            }
        }
        if ( user != null)
        {
            selectedObjects.add( user );
        }
    }

    public String getOption( String name )
    {
        return optionMap.get(  name );
    }


    public void setOption( String name, String string )
    {
        optionMap.put( name, string);
    }

    public boolean isDefaultEventTypes() 
    {
        return defaultEventTypes;
    }

    public boolean isDefaultResourceTypes() 
    {
        return defaultResourceTypes;
    }

    public void save(final String filename) throws RaplaException,
            EntityNotFoundException {
        final CalendarModelConfiguration conf = createConfiguration();
        
        Preferences clone = m_facade.edit(m_facade.getPreferences(user));

        if ( isDefaultFilename(filename))
        {
            clone.putEntry( CalendarModelConfiguration.CONFIG_ENTRY, conf);
        }
        else
        {
            Map<String,Object> exportMap= clone.getEntry(AutoExportPlugin.PLUGIN_ENTRY);
            Map<String,Object> newMap;
            if ( exportMap == null)
                newMap = new TreeMap<String,Object>();
            else
                newMap = new TreeMap<String,Object>( exportMap);
            newMap.put(filename, conf);
            clone.putEntry( AutoExportPlugin.PLUGIN_ENTRY, m_facade.newRaplaMap( newMap ));
        }
        m_facade.store(clone);
    }
    
    // #FIXME this is very ugly. Duplication of language resource names. But the system has to be replaced anyway in the future, because it doesnt allow for multiple language outputs on the server.
    private boolean isDefaultFilename(final String filename) 
	{
		List<String> translations = new ArrayList<String>();
		translations.add( getI18n().getString("default") );
		translations.add( "default" );
		translations.add( "Default" );
		translations.add( "Standard" );
		translations.add( "Standaard");
		// special for polnish
		if (filename.startsWith( "Domy") && filename.endsWith("lne"))
		{
			return true;
		}
		if (filename.startsWith( "Est") && filename.endsWith("ndar"))
		{
			return true;
		}
		return translations.contains(filename);
	}

    public void load(final String filename)
    throws RaplaException, EntityNotFoundException, CalendarNotFoundExeption {
        final CalendarModelConfiguration modelConfig;
        final Preferences preferences = m_facade.getPreferences(user);
        final boolean isDefault = filename == null || isDefaultFilename(filename);
        if ( isDefault )
        {
            modelConfig = preferences.getEntry(CalendarModelConfiguration.CONFIG_ENTRY);
        }
        else if ( filename != null && !isDefault)
        {
            Map<String,CalendarModelConfiguration> exportMap= preferences.getEntry(AutoExportPlugin.PLUGIN_ENTRY);
            if ( exportMap != null)
            {
            	modelConfig = exportMap.get(filename);
            }
            else
            {
            	modelConfig = null;
            }            
        }
        else
        {
        	modelConfig = null;
        }
        if ( modelConfig == null && filename != null && !isDefault)
        {
            throw new CalendarNotFoundExeption("Calendar with name " +  filename + " not found.");
        }
        else
        {
            Map<String,String> alternativeOptions = new HashMap<String,String>();
            if (modelConfig != null && modelConfig.getOptionMap() != null)
            {
                // All old default calendars have no selected date
                if (isDefault && (modelConfig.getOptionMap().get( AbstractHTMLCalendarPage.SAVE_SELECTED_DATE) == null))
                {
                    alternativeOptions.put(AbstractHTMLCalendarPage.SAVE_SELECTED_DATE , "false");
                }
                // All old calendars are exported
                if ( !isDefault && modelConfig.getOptionMap().get(AutoExportPlugin.HTML_EXPORT) == null)
                {
                    alternativeOptions.put(AutoExportPlugin.HTML_EXPORT,"true");
                }
            }
            setConfiguration(modelConfig, alternativeOptions);
        }
    }

	public List<AppointmentBlock> getBlocks() throws RaplaException 
	{
		List<AppointmentBlock> appointments = new ArrayList<AppointmentBlock>();
        for ( Reservation event:getReservations())
        {
        	for (Appointment  app: event.getAppointments())
        	{
        		app.createBlocks(getStartDate(), getEndDate(), appointments);
        	}
        }
        Collections.sort(appointments, new AppointmentBlockStartComparator());
        return appointments;
	}

	public DynamicType guessNewEventType() throws RaplaException  {
		
		Set<DynamicType> selectedTypes = getSelectedTypes(DynamicTypeAnnotations.VALUE_RESERVATION_CLASSIFICATION);
		DynamicType guessedType;
        if (selectedTypes.size()>0)
        {
        	guessedType = selectedTypes.iterator().next();
        }
        else
        {
        	guessedType = m_facade.getDynamicTypes(DynamicTypeAnnotations.VALUE_RESERVATION_CLASSIFICATION)[0];
        }
		ClassificationFilter[] reservationFilter = getReservationFilter();
        DynamicType firstType = null;
        boolean found = false;
        // assure that the guessed type is in the filter selection list
        for (ClassificationFilter filter : reservationFilter)
        {
        	DynamicType type = filter.getType();
			if ( firstType == null)
			{
				firstType = type;
			}
        	if ( type.equals( guessedType))
        	{
        		found = true;
        		break;
        	}
        }
        if  (!found  && firstType != null)
        {
        	guessedType = firstType;
        }
        return guessedType;
	}

	public Collection<TimeInterval> getMarkedIntervals() 
	{
		return timeIntervals;
	}
	
	public void setMarkedIntervals(Collection<TimeInterval> timeIntervals)
	{
		if ( timeIntervals != null)
		{
			this.timeIntervals = Collections.unmodifiableCollection(timeIntervals);
		}
		else
		{
			this.timeIntervals = Collections.emptyList();
		}
	}

	
	public void markInterval(Date start, Date end) {
		TimeInterval timeInterval = new TimeInterval( start, end);
		setMarkedIntervals( Collections.singletonList( timeInterval));
	}

	public Collection<Allocatable> getMarkedAllocatables() {
		return markedAllocatables;
	}

	public void setMarkedAllocatables(Collection<Allocatable> allocatables) {
		this.markedAllocatables = allocatables;
	}
}


