/*--------------------------------------------------------------------------*
 | Copyright (C) 2006 Gereon Fassbender, Christopher Kohlhaas               |
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
/** rapla-specific implementation of builder.
* Splits the appointments into day-blocks.
*/

package org.rapla.plugin.abstractcalendar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.apache.avalon.framework.logger.Logger;
import org.rapla.components.calendarview.Block;
import org.rapla.components.calendarview.BuildStrategy;
import org.rapla.components.calendarview.Builder;
import org.rapla.components.calendarview.CalendarView;
import org.rapla.components.util.Assert;
import org.rapla.components.util.DateTools;
import org.rapla.components.xmlbundle.I18nBundle;
import org.rapla.entities.Category;
import org.rapla.entities.CategoryAnnotations;
import org.rapla.entities.NamedComparator;
import org.rapla.entities.User;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.AppointmentBlock;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.dynamictype.Classifiable;
import org.rapla.entities.dynamictype.Classification;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.dynamictype.DynamicTypeAnnotations;
import org.rapla.facade.Conflict;
import org.rapla.facade.RaplaComponent;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.framework.RaplaLocale;
import org.rapla.gui.CalendarModel;
import org.rapla.gui.toolkit.RaplaColorList;

public abstract class RaplaBuilder extends RaplaComponent
    implements
        Builder
        ,Cloneable
{

    private Collection<Reservation> selectedReservations;
    private List<Allocatable> selectedAllocatables = new ArrayList<Allocatable>();

    private boolean enabled= true;
    private boolean bExceptionsExcluded = false;
    private boolean bResourceVisible = true;
    private boolean bPersonVisible = true;
    private boolean bRepeatingVisible = true;
    // TODO Christopher Check if this fits most users
    private boolean bTimeVisible = true; //Shows time <from - till> in top of all HTML- and Swing-View Blocks
    private boolean splitByAllocatables = false;
    protected boolean isEmptyBlockVisible = false;
    private HashMap<Allocatable,String> colors = new HashMap<Allocatable,String>(); //This currently only works with HashMap
    private User editingUser;
    private boolean isResourceColoring;
    private boolean isEventColoring;
    /** default buildStrategy is {@link GroupAllocatablesStrategy}.*/
    BuildStrategy buildStrategy;

    HashSet<Reservation> allReservationsForAllocatables = new HashSet<Reservation>();
    int max =0;
    int min =0;

    List<AppointmentBlock> preparedBlocks = null;
    private boolean conflictsSelected = false;

    public RaplaBuilder(RaplaContext sm) throws RaplaException {
        super(sm);
        buildStrategy = new GroupAllocatablesStrategy( getRaplaLocale().getLocale() );
    }

    public void setFromModel(CalendarModel model, Date startDate, Date endDate) throws RaplaException {
        Collection<Reservation> reservations = new HashSet<Reservation>(Arrays.asList( model.getReservations(  startDate, endDate )));
        Allocatable[] all = model.getSelectedAllocatables();
        Collection<Allocatable> allocatables = Arrays.asList( all);
        if ( model.getStartDate() != null && allocatables.size()> 0) {
            allReservationsForAllocatables.addAll( Arrays.asList(getQuery().getReservations( all, startDate, endDate)));
        }
        conflictsSelected = false;
        
        for (Object selectedObject: model.getSelectedObjects())
        {
            if (selectedObject instanceof Conflict)
            {
                conflictsSelected = true;
            }
        }
        isEmptyBlockVisible = allocatables.size() == 0;
        isResourceColoring =getCalendarOptions( model.getUser()).isResourceColoring();
        isEventColoring =getCalendarOptions( model.getUser()).isEventColoring();
        
        selectReservations( reservations );


        /* Uncomment this to color allocatables in the reservation view
        if ( allocatables.size() == 0) {
            allocatables = new ArrayList();
            for (int i=0;i< reservations.size();i++) {
                Reservation r = (Reservation) reservations.get( i );
                Allocatable[] a = r.getAllocatables();
                for (int j=0;j<a.length;j++) {
                    if ( !allocatables.contains( a[j] )) {
                        allocatables.add( a[j]);
                    }
                }
            }
        }*/
        selectAllocatables( allocatables );
    }


    public void setSmallBlocks( boolean isSmallView) {
        setTimeVisible(  isSmallView );
        setPersonVisible( !isSmallView );
        setResourceVisible( !isSmallView );
    }

    public void setSplitByAllocatables( boolean enable) {
        splitByAllocatables = enable;
    }

    public void setExceptionsExcluded( boolean exclude) {
        this.bExceptionsExcluded = exclude;
    }

    boolean isExceptionsExcluded() {
        return bExceptionsExcluded;
    }

    public void setEditingUser(User editingUser) {
        this.editingUser = editingUser;
    }

    public User getEditingUser() {
        return this.editingUser;
    }

    public boolean isEnabled()  {
        return enabled;
    }

    public void setEnabled(boolean enable) {
        this.enabled = enable;
    }

    public void setBuildStrategy(BuildStrategy strategy) {
        this.buildStrategy = strategy;
    }

    public void setTimeVisible(boolean bTimeVisible) {
        this.bTimeVisible = bTimeVisible;
    }

    public boolean isTimeVisible() {
        return bTimeVisible;
    }

    public void setResourceVisible(boolean bVisible) {
        this.bResourceVisible = bVisible;
    }

    public void setPersonVisible(boolean bVisible) {
        this.bPersonVisible = bVisible;
    }

    public void setRepeatingVisible(boolean bVisible) {
        this.bRepeatingVisible = bVisible;
    }

    public List<Allocatable> getAllocatables() {
        return selectedAllocatables;
    }

    /**
        Map enthaelt die fuer die resourcen-darstellung benutzten farben
        mit resource-id (vom typ Integer) als schluessel.
        erst nach rebuild() verfuegbar.
    */
    Map<Allocatable,String> getColorMap() {
        return colors;
    }

    private void createColorMap()
    {
        colors.clear();
        List<Allocatable> arrayList = new ArrayList<Allocatable>(selectedAllocatables);
        Comparator<Allocatable> comp =new Comparator<Allocatable>() {
                public int compare(Allocatable o1, Allocatable o2) {
                    if (o1.hashCode()>o2.hashCode())
                        return -1;
                    if (o1.hashCode()<o2.hashCode())
                        return 1;
                    return 0;
                }
            };
        Collections.sort(arrayList,comp);
        Iterator<Allocatable> it = arrayList.iterator();
        int i=0;
        while (it.hasNext()) {
            Allocatable allocatable =  it.next();
            DynamicType type = allocatable.getClassification().getType();
            String annotation = type.getAnnotation(DynamicTypeAnnotations.KEY_COLORS);
        	String color =null;
        	if (annotation  == null)
        	{
        		annotation =  type.getAttribute("color") != null ? DynamicTypeAnnotations.COLORS_COLOR_ATTRIBUTE: DynamicTypeAnnotations.COLORS_AUTOMATED;
        	}
        	color = getColorForClassifiable( allocatable );
            if ( color == null && annotation.equals(DynamicTypeAnnotations.COLORS_AUTOMATED))
            {
            	color = RaplaColorList.getHexForColor( RaplaColorList.getResourceColor(i++));
            }
            else if (  annotation.equals(DynamicTypeAnnotations.COLORS_DISABLED))
            {
            	color = null;
            }
            
            if ( color != null)
            {
            	colors.put(allocatable, color);
            }
        }
    }

    static String getColorForClassifiable( Classifiable classifiable ) {
        Classification c = classifiable.getClassification();
        Attribute colorAttribute = c.getAttribute("color");
        String annotation = c.getType().getAnnotation(DynamicTypeAnnotations.KEY_COLORS);
        if ( annotation != null && annotation.equals( DynamicTypeAnnotations.COLORS_DISABLED))
        {
        	return null;
        }
        String color = null;
        if ( colorAttribute != null) {
            Object hexValue = c.getValue( colorAttribute );
            if ( hexValue != null) {
                if ( hexValue instanceof Category) {
                    hexValue = ((Category) hexValue).getAnnotation( CategoryAnnotations.KEY_NAME_COLOR );
                }
                if ( hexValue != null) {
                    color = hexValue.toString();
                }
            }
        }
        return color;
    }

    /**
       diese reservierungen werden vom WeekView angezeigt.
    */
    public void selectReservations(Collection<Reservation> reservations)
    {
        this.selectedReservations= reservations;
    }

    /**
       nur diese resourcen werden angezeigt.
    */
    public void selectAllocatables(Collection<Allocatable> allocatables)
    {
        selectedAllocatables.clear();
        if (allocatables != null ) {
            selectedAllocatables.addAll(new HashSet<Allocatable>(allocatables));
            Collections.sort( selectedAllocatables, new NamedComparator<Allocatable>( getRaplaLocale().getLocale() ));
        }
        createColorMap();
    }
    /** @param emptyBlockVisible indicates that blocks that don't contain one of the selected
    * allocatables should also be visible.
    * */
    static public List<AppointmentBlock> getAffectedAppointments(Collection<Reservation> selectedReservations, Collection<Allocatable> selectedAllocatables,
                                           Date start, Date end, boolean emptyBlockVisible, boolean excludeExceptions) {
        if ( selectedAllocatables == null )
            selectedAllocatables = Collections.emptyList();
        List<AppointmentBlock> preparedBlocks = new ArrayList<AppointmentBlock>();
        // The appointments that are explictly connected to
        // a selected allocatable
        ArrayList<Appointment> connectedAppointments = new ArrayList<Appointment>();
        Iterator<Reservation> it = selectedReservations.iterator();
        while (it.hasNext()){
            Reservation r= (Reservation) it.next();
            //System.out.println("Selected : " + r.getName(Locale.getDefault()));
            Appointment[] appointments= r.getAppointments();
            Allocatable[] allocatables = r.getAllocatables();

            // this flag is set true if one of the allocatables of a
            // reservation matches a selected allocatable.
            boolean allocatableMatched = false;

            // a reservation is wildcardConnected, if at least one of its
            // allocatables is connected to all appointments and
            // this allocatable is selected.
            boolean wildcardConnected = false;

            connectedAppointments.clear();
            for (int i=0; i<allocatables.length; i++)   {
                if (selectedAllocatables.contains(allocatables[i])) {
                    Appointment[] restriction = r.getRestriction(allocatables[i]);
                    if (restriction.length == 0) {
                        // this allocatable is connected to all appointments.
                        allocatableMatched = true;
                        wildcardConnected = true;
                        break;
                    } else {
                        allocatableMatched = true;
                        for (int j=0; j<appointments.length; j++)   {
                            for (int k = 0; k < restriction.length; k++ ) {
                                if ( appointments[j].equals( restriction[k] ) ) {
                                    // This appointment is explictly connected to
                                    // the selected allocatable
                                    connectedAppointments.add(appointments[j]);
                                }
                            }
                        }
                    }
                }
            }

            // block should not be included if it doesn't contain one
            // of the allocatables
            if (!allocatableMatched && !emptyBlockVisible )
                continue;

            // An  appointment should not be visible, if no
            // selected allocatable is connected to this appointment.
            for (int i=0; i<appointments.length; i++)
            {
                if (    emptyBlockVisible
                        || wildcardConnected
                        || connectedAppointments.contains(appointments[i]))
                {
                    // Add appointment to the blocks
                    appointments[i].createBlocks(start, end, preparedBlocks, excludeExceptions);

                    // Here the other exceptions should be added
                }
            }
        }
        return preparedBlocks;
    }


    static public List<AppointmentBlock> splitBlocks(Collection<AppointmentBlock> preparedBlocks, Date startDate, Date endDate) {
        List<AppointmentBlock> result = new ArrayList<AppointmentBlock>();
        for (AppointmentBlock block:preparedBlocks) {
            long blockStart = block.getStart();
            long blockEnd = block.getEnd();
            Appointment appointment = block.getAppointment();
            boolean isException = block.isException();
            if (DateTools.isSameDay(blockStart, blockEnd)) {
                result.add( new AppointmentBlock(blockStart, blockEnd, appointment, isException));
            } else {
                long firstBlockDate = Math.max(blockStart, startDate.getTime());
                long lastBlockDate = Math.min(blockEnd, endDate.getTime());
                long currentBlockDate = firstBlockDate;
                while ( currentBlockDate >= blockStart && DateTools.cutDate( currentBlockDate ) < lastBlockDate) {
                    long start;
                    long end;
                    if (DateTools.isSameDay(blockStart, currentBlockDate)) {
                        start= blockStart;
                    } else {
                        start = DateTools.cutDate(currentBlockDate);
                    }
                    if (DateTools.isSameDay(blockEnd, currentBlockDate) && !DateTools.isMidnight(blockEnd)) {
                        end = blockEnd;
                    }else {
                        end = DateTools.fillDate( currentBlockDate ) -1;
                    }
                    //System.out.println("Adding Block " + new Date(start) + " - " + new Date(end));
                    result.add ( new AppointmentBlock(start, end, appointment,isException));
                    currentBlockDate+= DateTools.MILLISECONDS_PER_DAY;
                }
            }
        }
        return result;
    }


    /** selects all blocks that should be visible and calculates the max start- and end-time  */
    public void prepareBuild(Date start,Date end) {
    	boolean excludeExceptions = isExceptionsExcluded();
        HashSet<Reservation> allReservations = new HashSet<Reservation>( selectedReservations);
        allReservations.addAll( allReservationsForAllocatables);
        preparedBlocks = getAffectedAppointments(allReservations, selectedAllocatables, start, end, isEmptyBlockVisible, excludeExceptions);
        preparedBlocks = splitBlocks(preparedBlocks, start, end);

        // calculate new start and end times
        max =0;
        min =24;
        for (AppointmentBlock block:preparedBlocks)
        {
            int starthour = DateTools.getHourOfDay(block.getStart());
            int startminute = DateTools.getMinuteOfHour(block.getStart());
            int endhour = DateTools.getHourOfDay(block.getEnd());
            int endminute = DateTools.getMinuteOfHour(block.getEnd());
            if ((starthour != 0 || startminute != 0)  && starthour<min)
                min = starthour;
            if (endhour<min && (endhour >0 || endminute>0))
                min = Math.max(0,endhour-1);
            if ((endhour != 0 || endminute != 0) && (endhour != 23 && endminute!=59) && endhour>max)
                max = Math.min(24,endhour + 1);
            if (starthour>=max)
                max = Math.min(24,starthour +1);
        }
    }

    public int getMin() {
        Assert.notNull(preparedBlocks, "call prepareBuild first");
        return min;
    }

    public int getMax() {
        Assert.notNull(preparedBlocks, "call prepareBuild first");
        return max;
    }

    abstract Block createBlock(RaplaBlockContext blockContext, Date start, Date end);

    public void build(CalendarView wv) {
    	ArrayList<Block> blocks = new ArrayList<Block>();
    	BuildContext buildContext = new BuildContext(this, blocks);
        Assert.notNull(preparedBlocks, "call prepareBuild first");
        for (AppointmentBlock block:preparedBlocks)
        {
            Date start = new Date( block.getStart() );
            Date end = new Date( block.getEnd() );
            Appointment appointment = block.getAppointment();
            RaplaBlockContext[] blockContext = getBlocksForAppointment( appointment, buildContext );
            for ( int j=0;j< blockContext.length; j++) {
                blocks.add( createBlock(blockContext[j], start, end));
            }
        }

        buildStrategy.build(wv, blocks);
    }

    private RaplaBlockContext[] getBlocksForAppointment(Appointment appointment, BuildContext buildContext) {
        boolean isEventSelected = selectedReservations.contains( appointment.getReservation());
        RaplaBlockContext firstContext = new RaplaBlockContext( appointment, this, buildContext, null, isEventSelected );
        List<Allocatable> selectedAllocatables = firstContext.getSelectedAllocatables();
        if ( !splitByAllocatables || selectedAllocatables.size() < 2) {
            return new RaplaBlockContext[] { firstContext };
        }
        RaplaBlockContext[] context = new RaplaBlockContext[ selectedAllocatables.size() ];
        for ( int i= 0;i<context.length;i ++) {
            context[i] = new RaplaBlockContext( appointment, this, buildContext, (Allocatable) selectedAllocatables.get( i ), isEventSelected);
        }
        return context;
    }

    private boolean isMovable(Reservation reservation) {
        return selectedReservations.contains( reservation ) && canModify(reservation, editingUser);
    }

    public boolean isConflictsSelected() {
        return conflictsSelected;
    }

    /** This context contains the shared information for all RaplaBlocks.*/
    static class BuildContext {
        boolean bResourceVisible = true;
        boolean bPersonVisible = true;
        boolean bRepeatingVisible = true;
        boolean bTimeVisible = false;
        boolean conflictsSelected = false;
        Map<Allocatable,String> colors;
        I18nBundle i18n;
        RaplaLocale raplaLocale;
        RaplaContext serviceManager;
        Logger logger;
        User user;
        List<Block> blocks;
        boolean canReadOthers;
		private boolean isResourceColoring;
		private boolean isEventColoring;
		
        @SuppressWarnings("unchecked")
		public BuildContext(RaplaBuilder builder, List<Block> blocks)
        {
        	this.blocks = blocks;
            this.raplaLocale = builder.getRaplaLocale();
            this.bResourceVisible = builder.bResourceVisible;
            this.bPersonVisible= builder.bPersonVisible;
            this.bTimeVisible= builder.isTimeVisible();
            this.bRepeatingVisible= builder.bRepeatingVisible;
            this.colors = (Map<Allocatable,String>) builder.colors.clone();
            this.i18n =builder.getI18n();
            this.serviceManager = builder.getContext();
            this.logger = builder.getLogger();
            this.user = builder.editingUser;
            this.canReadOthers = builder.getClientFacade().canReadReservationsFromOthers(user);
            this.conflictsSelected = builder.isConflictsSelected();
            this.isResourceColoring = builder.isResourceColoring;
            this.isEventColoring = builder.isEventColoring;
        }

        public RaplaLocale getRaplaLocale() {
            return raplaLocale;
        }

        public RaplaContext getServiceManager() {
            return serviceManager;
        }

        public Icon getRepeatingIcon() {
            return i18n.getIcon("icon.repeating");
        }

        public Icon getSingleIcon() {
            return i18n.getIcon("icon.single");
        }
        public List<Block> getBlocks()
        {
        	return blocks;
        }

        public ImageIcon getExceptionBackgroundIcon() {
            return i18n.getIcon("icon.exceptionBackground");
        }
        
        public I18nBundle getI18n() {
            return i18n;
        }
        public boolean isTimeVisible() {
            return bTimeVisible;
        }

        public boolean isPersonVisible() {
            return bPersonVisible;
        }

        public boolean isRepeatingVisible() {
            return bRepeatingVisible;
        }

        public boolean isResourceVisible() {
            return bResourceVisible;
        }

        public String lookupColorString(Allocatable allocatable) {
            if (allocatable == null)
                return RaplaColorList.DEFAULT_COLOR_AS_STRING;
            return (String) colors.get(allocatable);
        }

        public boolean isConflictSelected() 
        {
            return conflictsSelected;
        }

		public boolean isResourceColoringEnabled() {
			return isResourceColoring;
		}
		
		public boolean isEventColoringEnabled() {
			return isEventColoring;
		}

		
    }

    /** This context contains the shared information for one particular RaplaBlock.*/
    static class RaplaBlockContext {
        ArrayList<Allocatable> selectedMatchingAllocatables = new ArrayList<Allocatable>(3);
        ArrayList<Allocatable> matchingAllocatables = new ArrayList<Allocatable>(3);
        Appointment appointment;
        boolean movable;
        BuildContext buildContext;
        boolean isEventSelected;

        public RaplaBlockContext(Appointment appointment,RaplaBuilder builder,BuildContext buildContext, Allocatable selectedAllocatable, boolean isEventSelected) {
            this.buildContext = buildContext;
            this.appointment = appointment;
            this.isEventSelected = isEventSelected;
            Reservation reservation = appointment.getReservation();
            if(isEventSelected)
            	this.movable = builder.isMovable( reservation );
            // Prefer resources when grouping
            addAllocatables(builder, reservation.getResources(), selectedAllocatable);
            addAllocatables(builder, reservation.getPersons(), selectedAllocatable);
        }

        private void addAllocatables(RaplaBuilder builder, Allocatable[] allocatables,Allocatable selectedAllocatable) {
            Reservation reservation = appointment.getReservation();
            for (int i=0; i<allocatables.length; i++)   {
                if ( !reservation.hasAllocated( allocatables[i], appointment ) ) {
                    continue;
                }
                matchingAllocatables.add(allocatables[i]);
                if ( builder.selectedAllocatables.contains(allocatables[i])) {
                    if ( selectedAllocatable == null ||  selectedAllocatable.equals( allocatables[i]) ) {
                        selectedMatchingAllocatables.add(allocatables[i]);
                    }

                }
            }
        }

        public boolean isMovable() {
            return movable;// && !isAnonymous;
        }

        public Appointment getAppointment() {
            return appointment;
        }

        public List<Allocatable> getSelectedAllocatables() {
            return selectedMatchingAllocatables;
        }

        public List<Allocatable> getAllocatables() {
            return matchingAllocatables;
        }

        public boolean isVisible(Allocatable allocatable) {
            User user = buildContext.user;
            if ( user != null && !allocatable.canRead( user) ) {
                return false;
            }
            return matchingAllocatables.contains(allocatable);
        }

        public BuildContext getBuildContext() {
            return buildContext;
        }

        /**
         * @return null if no allocatables found
         */
        public Allocatable getGroupAllocatable() {
            // Look if block belongs to a group according to the selected allocatables
            List<Allocatable> allocatables = getSelectedAllocatables();
            // Look if block belongs to a group according to its reserved allocatables
            if ( allocatables.size() == 0)
                allocatables = getAllocatables();
            if ( allocatables.size() == 0)
                return null;
            return allocatables.get( 0 );
        }

        public boolean isEventSelected() {
            return isEventSelected;
        }

        public boolean isAnonymous() {
           User user = buildContext.user;
           Reservation event = appointment.getReservation();
           if ( user == null ||  user.equals( event.getOwner()) || user.isAdmin())
           {
               return false;
           }
           if ( buildContext.canReadOthers )
           {
               return false;
           }
           return true;
        }

    }

}


