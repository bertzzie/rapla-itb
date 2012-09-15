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
package org.rapla.facade;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.swing.ImageIcon;

import org.apache.avalon.framework.logger.Logger;
import org.rapla.components.xmlbundle.CompoundI18n;
import org.rapla.components.xmlbundle.I18nBundle;
import org.rapla.entities.Category;
import org.rapla.entities.Named;
import org.rapla.entities.Ownable;
import org.rapla.entities.RaplaObject;
import org.rapla.entities.User;
import org.rapla.entities.configuration.Preferences;
import org.rapla.entities.configuration.RaplaConfiguration;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.AppointmentFormater;
import org.rapla.entities.domain.Permission;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.dynamictype.AttributeType;
import org.rapla.entities.dynamictype.Classifiable;
import org.rapla.entities.dynamictype.Classification;
import org.rapla.entities.dynamictype.ConstraintIds;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.framework.Container;
import org.rapla.framework.PluginDescriptor;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaContextException;
import org.rapla.framework.RaplaException;
import org.rapla.framework.RaplaLocale;
import org.rapla.gui.CalendarOptions;
import org.rapla.gui.CalendarOptionsImpl;
import org.rapla.server.RemoteServiceCaller;

/**
    Base class for most components. Eases
    access to frequently used services, e.g. {@link I18nBundle}.
 */
public class RaplaComponent
{
	final static String PERMISSION_MODIFY = "permission_modify";
		
    final private ClientServiceManager serviceManager;
    private String childBundleName;
    private Logger logger;

    public RaplaComponent(RaplaContext context) throws RaplaException {
        try {
            logger = (Logger)context.lookup(Logger.class.getName() );
            this.serviceManager = new ClientServiceManager();
            this.serviceManager.parent = context;
        } catch ( RaplaException ex) {
            throw ex;
        } catch ( Exception ex) {
            throw new RaplaException(ex);
        }
    }

    public String getChildBundleName() {
        return childBundleName;
    }

    public void setChildBundleName(String childBundleName) {
        this.childBundleName =  childBundleName;
    }

    protected Container getContainer() throws RaplaException {
        return ((Container)getContext().lookup(Container.class.getName()));
    }

    /** returns if the session user is admin */
    public boolean isAdmin() {
        try {
            return getUser().isAdmin();
        } catch (RaplaException ex) {
        }
        return false;
    }

    /** returns if the session user is a registerer */
    public boolean isRegisterer() {
        if (isAdmin())
        {
            return true;
        }
        try {
            Category registererGroup = getQuery().getUserGroupsCategory().getCategory(Permission.GROUP_REGISTERER_KEY);
            return getUser().belongsTo(registererGroup);
        } catch (RaplaException ex) {
        }
        return false;
    }

    public boolean isModifyPreferencesAllowed() {
        if (isAdmin())
        {
            return true;
        }
        try {
            Category modifyPreferences = getQuery().getUserGroupsCategory().getCategory(Permission.GROUP_MODIFY_PREFERENCES_KEY);
            if ( modifyPreferences == null ) {
                return true;
            }
            return getUser().belongsTo(modifyPreferences);
        } catch (RaplaException ex) {
        }
        return false;
    }

    /** returns if the user has allocation rights for one or more resource */
    public boolean canUserAllocateSomething(User user) throws RaplaException {
        Allocatable[] allocatables =getQuery().getAllocatables();
        if ( user.isAdmin() )
            return true;
        for ( int i=0;i<allocatables.length;i++) {
            Permission[] permissions = allocatables[i].getPermissions();
            for ( int j=0;j<permissions.length;j++) {
                Permission p = permissions[j];
                if (!p.affectsUser( user ))
                {
                    continue;
                }
                if ( p.getAccessLevel() > Permission.READ)
                {
                    return true;
                }
            }
        }
        return false;
    }

    /** returns if the current user is allowed to modify the object. */
    public boolean canModify(Object object) {
        try {
            User user = getUser();
            return canModify(object, user);
        } catch (RaplaException ex) {
            return false;
        }
    }

    static public boolean canModify(Object object, User user) {
        if (object == null || !(object instanceof RaplaObject))
        {
            return false;
        }
        if ( user == null)
        {
            return false;
        }
        if (user.isAdmin())
            return true;
        if (object instanceof Ownable) {
            Ownable ownable = (Ownable) object;
            if  (ownable.getOwner() == null || user.equals(ownable.getOwner()))
            {
                return true;
            }
        }
        if (object instanceof Allocatable) {
            Allocatable allocatable = (Allocatable) object;
            if (allocatable.canModify( user ))
            {
                return true;
            }
        }
        if (checkClassifiablePermissions(object, user))
        {
            return true;
        }
        return false;
    }
/** We check if an attribute with the permission_modify exists and look if the permission is set either globaly (if boolean type is used) or for a specific user group (if category type is used)*/
    public static boolean checkClassifiablePermissions(Object object, User user) {
        if (object instanceof Classifiable ) {
            final Classifiable classifiable = (Classifiable) object;
            
            Classification classification = classifiable.getClassification();
            if ( classification != null)
            {
                final DynamicType type = classification.getType();
               final Attribute attribute = type.getAttribute(PERMISSION_MODIFY);
                if ( attribute != null)
                {
                    final AttributeType type2 = attribute.getType();
                    if (type2 == AttributeType.BOOLEAN)
                    {
                        final Object value = classification.getValue( attribute);
                        return Boolean.TRUE.equals(value);
                    }
                    if ( type2 == AttributeType.CATEGORY)
                    {
                        Category cat = (Category)classification.getValue( attribute);
                        if ( cat == null)
                        {
                            Category rootCat = (Category)attribute.getConstraint(ConstraintIds.KEY_ROOT_CATEGORY);
                            if ( rootCat.getCategories().length == 0)
                            {
                                cat = rootCat;
                            }
                        }
                        if (user.belongsTo( cat))
                        {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public CalendarOptions getCalendarOptions() {
    	User user;
    	try
    	{
    		user = getUser();
    	} 
    	catch (RaplaException ex) {
    		// Use system settings if an error occurs
    		user = null;
        }
    	return getCalendarOptions( user);
    }
    
    protected CalendarOptions getCalendarOptions(User user) {
        RaplaConfiguration conf = null;
        try {
            if ( user != null)
            {
                conf = (RaplaConfiguration)getQuery().getPreferences( user ).getEntry(CalendarOptionsImpl.CALENDAR_OPTIONS);
            }
            if ( conf == null)
            {
                conf = (RaplaConfiguration)getQuery().getPreferences( null ).getEntry(CalendarOptionsImpl.CALENDAR_OPTIONS);
            }
            if ( conf != null)
            {
                return new CalendarOptionsImpl( conf.getConfig());
            }
        } catch (RaplaException ex) {

        }
        return (CalendarOptions)getService( CalendarOptions.ROLE);
    }

    protected User getUser() throws RaplaException {
    	return getUserModule().getUser();
    }

    protected Logger getLogger() {
        return logger;
    }

    /** lookup the service in the serviceManager under the specified key:
        serviceManager.lookup(role).
        @throws IllegalStateException if GUIComponent wasn't serviced. No service method called
        @throws UnsupportedOperationException if service not available.
     */
    @SuppressWarnings("unchecked")
	protected <T> T getService(Class<T> role) {
        return (T) getService( role.getName());
    }
    
    protected Object getService(String role) {
        try {
            return  getContext().lookup(role.toString());
        } catch (RaplaContextException ex) {
            getLogger().error("Cause " , ex);
            throw new UnsupportedOperationException("Service not supported in this context: " + role);
        }
    }
    
    protected RaplaContext getContext() {
        return serviceManager;
    }

    /** lookup RaplaLocale from the context */
    protected RaplaLocale getRaplaLocale() {
        if (serviceManager.raplaLocale == null)
            serviceManager.raplaLocale = getService(RaplaLocale.class);
        return serviceManager.raplaLocale;
    }


    protected Locale getLocale() {
        return getRaplaLocale().getLocale();
    }

    protected I18nBundle childBundle;
    /** lookup I18nBundle from the serviceManager */
    protected I18nBundle getI18n() {
        String childBundleName = getChildBundleName();
        if ( childBundleName != null) {
            if ( childBundle == null) {
                I18nBundle pluginI18n = (I18nBundle)getService(I18nBundle.class.getName() + "/" + childBundleName );
                childBundle = new CompoundI18n(pluginI18n,getI18nDefault());
            }
            return childBundle;
        }
        return getI18nDefault();
    }

    private I18nBundle getI18nDefault() {
        if (serviceManager.i18n == null)
            serviceManager.i18n = (I18nBundle)getService(I18nBundle.class.getName() + "/org.rapla.RaplaResources");
        return serviceManager.i18n;
    }

    /** lookup AppointmentFormater from the serviceManager */
    protected AppointmentFormater getAppointmentFormater() {
        if (serviceManager.appointmentFormater == null)
            serviceManager.appointmentFormater = getService(AppointmentFormater.class);
        return serviceManager.appointmentFormater;
    }

    /** lookup PeriodModel from the serviceManager */
    protected PeriodModel getPeriodModel() {
    	try {
    		return getQuery().getPeriodModel();
    	} catch (RaplaException ex) {
    		throw new UnsupportedOperationException("Service not supported in this context: " );
    	}
    }

    /** lookup QueryModule from the serviceManager */
    protected QueryModule getQuery() {
        return getClientFacade();
    }

    protected ClientFacade getClientFacade() {
        if (serviceManager.facade == null)
            serviceManager.facade =  getService( ClientFacade.class );
        return serviceManager.facade;
    }

    /** lookup ModificationModule from the serviceManager */
    protected ModificationModule getModification() {
        return getClientFacade();
    }

    /** lookup UpdateModule from the serviceManager */
    protected UpdateModule getUpdateModule() {
        return getClientFacade();
    }

    /** lookup UserModule from the serviceManager */
    protected UserModule getUserModule() {
        return getClientFacade();
    }

    /** returns a translation for the object name into the selected language. If
     a translation into the selected language is not possible an english translation will be tried next.
     If theres no translation for the default language, the first available translation will be used. */
    public String getName(Object object) {
        if (object == null)
            return "";
        if (object instanceof Named) {
            String name = ((Named) object).getName(getI18n().getLocale());
            return (name != null) ? name : "";
        }
        return object.toString();
    }

    /** calls getI18n().getString(key) */
    public String getString(String key) {
        return getI18n().getString(key);
    }


    /** calls "&lt;html>" + getI18n().getString(key) + "&lt;/html>"*/
    public String getStringAsHTML(String key) {
        return "<html>" + getI18n().getString(key) + "</html>";
    }

    /** calls getI18n().getIcon(key) */
    public ImageIcon getIcon(String key) {
        return getI18n().getIcon(key);
    }

    private static class ClientServiceManager implements RaplaContext {
        I18nBundle i18n;
        ClientFacade facade;
        RaplaLocale raplaLocale;
        RaplaContext parent;
        AppointmentFormater appointmentFormater;
        public Object lookup(String role) throws RaplaContextException {
            return parent.lookup(role);
        }
        
        public boolean has(String role) {
            return parent.has(role);
        }
    }

    public Preferences newEditablePreferences() throws RaplaException {
        return (Preferences) getModification().edit(getQuery().getPreferences());
    }

    public PluginDescriptor findDescriptor( String pluginClassName ) throws RaplaException
    {
        @SuppressWarnings("unchecked")
		List<PluginDescriptor> pluginList = (List<PluginDescriptor>) getService( PluginDescriptor.PLUGIN_LIST);
        for (Iterator<PluginDescriptor> it = pluginList.iterator();it.hasNext();) {
            PluginDescriptor descriptor = it.next();
            if (descriptor.getClass().getName().equals( pluginClassName )) {
                return descriptor;
            }
        }
        return null;
    }
    
    public <T> T getWebservice(Class<T> a)
    {
    	RemoteServiceCaller remote = getService( RemoteServiceCaller.class);
    	return remote.getRemoteMethod(a);
    }
    
    
    public static boolean isSigned() {
        try
        {
            final ClassLoader classLoader = RaplaComponent.class.getClassLoader();
            {
                final Enumeration<URL> resources = classLoader.getResources("META-INF/RAPLA.SF");
                if (resources.hasMoreElements() )
                     return true;
            }
            {
                final Enumeration<URL> resources = classLoader.getResources("META-INF/RAPLA.DSA");
                if (resources.hasMoreElements() )
                    return true;
            }
        }
        catch ( IOException ex)
        {
            
        }
        return false;
    }

}
