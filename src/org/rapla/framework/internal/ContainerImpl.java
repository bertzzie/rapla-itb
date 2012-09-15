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
package org.rapla.framework.internal;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.ConsoleLogger;
import org.apache.avalon.framework.logger.Logger;
import org.rapla.framework.Container;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaContextException;
import org.rapla.framework.RaplaException;
import org.rapla.framework.StartupEnvironment;
import org.rapla.server.RemoteSession;

/** Base class for the ComponentContainers in Rapla.
 * Containers are the RaplaMainContainer, the Client- and the Server-Service
 */
public class ContainerImpl extends AbstractLogEnabled implements Container, Disposable
{
    protected Container m_parent;
    protected RaplaContext m_context;
    protected Configuration m_config;

    protected List<ComponentHandler> m_componentHandler = new ArrayList<ComponentHandler>();
    protected HashMap<String,RoleEntry> m_roleMap = new HashMap<String,RoleEntry>();
    protected LogManagerAdapter m_loggerManager;

    public ContainerImpl(RaplaContext parentContext, Configuration config) throws RaplaException  {
        m_config = config;
        service( parentContext );
        init( );
    }


    protected void init() throws RaplaException {
        configure( m_config );
        addContainerProvidedComponentInstance( Container.class.getName(), this );
        addContainerProvidedComponentInstance( Logger.class.getName(), getLogger());
    }

    public StartupEnvironment getStartupEnvironment() {
        try
        {
            return (StartupEnvironment)getContext().lookup( StartupEnvironment.class.getName());
        }
        catch ( RaplaContextException e )
        {
            throw new IllegalStateException(" Container not initialized with a startup environment");
        }
    }

    private void service(final RaplaContext parent) throws RaplaContextException {
        if (parent.has( "logger-manager" )) {
            m_loggerManager = (LogManagerAdapter) parent.lookup("logger-manager");
        } else {
            final Logger logger;
            if ( parent.has(Logger.class.getName() ) )
            {
                logger = (Logger) parent.lookup( Logger.class.getName());
            }
            else
            {
                logger = new ConsoleLogger(ConsoleLogger.LEVEL_INFO);
            }

            m_loggerManager = new LogManagerAdapter()
            {
                public Logger getLoggerForCategory(String categoryName)
                {
                    return logger.getChildLogger( categoryName );
                }

                public Logger getDefaultLogger()
                {
                    return logger;
                }
            };
        }
        enableLogging( m_loggerManager.getDefaultLogger());
        if ( parent.has(Container.class.getName() )) {
            m_parent = (Container) parent.lookup( Container.class.getName());
        }
        m_context = new RaplaContext() {

            public Object lookup(String role) throws RaplaContextException {
                ComponentHandler handler = getHandler( role );
                if ( handler != null ) {
                    return handler.get();
                }
                return parent.lookup( role);
            }

            public boolean has(String role) {
                if (getHandler( role ) != null)
                    return true;
                return parent.has( role );
            }

            ComponentHandler getHandler( String role) {
                int hintSeperator = role.indexOf('/');
                String roleName = role;
                String hint = null;
                if ( hintSeperator > 0 ) {
                    roleName = role.substring( 0, hintSeperator   );
                    hint = role.substring( hintSeperator + 1 );
                }
                return ContainerImpl.this.getHandler( roleName, hint );
            }


        };
    }



    protected void configure( final Configuration config )
        throws RaplaException
    {
        Map<String,ComponentInfo> m_componentInfos = getComponentInfos();
        final Configuration[] elements = config.getChildren();
        for ( int i = 0; i < elements.length; i++ )
        {
            final Configuration element = elements[i];
            final String id = element.getAttribute( "id", null );
            if ( null == id )
            {
                // Only components with an id attribute are treated as components.
                getLogger().debug( "Ignoring configuration for component, " + element.getName()
                    + ", because the id attribute is missing." );
            }
            else
            {
                final String className;
                final String[] roles;
                if ( "component".equals( element.getName() ) )
                {
                    try {
                        className = element.getAttribute( "class" );
                        Configuration[] roleConfigs = element.getChildren("roles");
                        roles = new String[ roleConfigs.length ];
                        for ( int j=0;j< roles.length;j++) {
                            roles[j] = roleConfigs[j].getValue();
                        }
                    } catch ( ConfigurationException ex) {
                        throw new RaplaException( ex);
                    }
                }
                else
                {
                    String configName = element.getName();
                    final ComponentInfo roleEntry = m_componentInfos.get( configName );
                    if ( null == roleEntry )
                    {
                        final String message = "No class found matching configuration name " +
                            "[name: " + element.getName() + ", location: " + element.getLocation() + "]";
                        getLogger().error( message );

                        continue;
                    }
                    roles = roleEntry.getRoles();
                    className = roleEntry.getClassname();
                }
                if ( getLogger().isDebugEnabled() )
                {
                    getLogger().debug( "Configuration processed for: " + className );
                }
                Logger logger = m_loggerManager.getLoggerForCategory( id );
                ComponentHandler handler =new ComponentHandler( element, className, logger);
                for ( int j=0;j< roles.length;j++) {
                    String roleName = (roles[j]);
                    addHandler( roleName, id, handler );
                }
            }
        }
    }

    protected Map<String,ComponentInfo> getComponentInfos() {
        return Collections.emptyMap();
    }

    synchronized public void addContainerProvidedComponentInstance(String role,Object component) {
        addContainerProvidedComponentInstance( role, component.getClass().getName(),component);
    }

    synchronized public void addContainerProvidedComponentInstance(String roleName,String hint,Object component) {
        addHandler( roleName, hint, new ComponentHandler(component));
    }

    synchronized public void addContainerProvidedComponent(String classname) {
        addContainerProvidedComponent( classname, classname);
    }

    synchronized public void addContainerProvidedComponent(String role,String classname) {
        addContainerProvidedComponent( new String[] {role}, classname, classname, null);
    }

    synchronized public void addContainerProvidedComponent(String role,String classname, Configuration config) {
        addContainerProvidedComponent( new String[] {role}, classname, classname, config);
    }

    synchronized public void addContainerProvidedComponent(String role,String classname, String hint,Configuration config) {
        addContainerProvidedComponent( new String[] {role}, classname, hint, config);
    }

    synchronized public void addWebservice(String role,String classname, String hint,Configuration config) {
        addContainerProvidedComponent( new String[] {role}, classname, hint, config);
    }

    
    synchronized public void addContainerProvidedComponent(String[] roles,String classname,String hint, Configuration config) {
        ComponentHandler handler = new ComponentHandler( config, classname, getLogger() );
        for ( int i=0;i<roles.length;i++) {
            addHandler( roles[i], hint, handler);
        }
    }
    

    public Collection<?> getAllServicesFor(String role) {
        List<Object> list = new ArrayList<Object>( getAllServicesForThisContainer( role));
        if ( m_parent != null) {
            list.addAll( m_parent.getAllServicesFor( role ));
        }
        return list;
    }

    protected Collection<?> getAllServicesForThisContainer(String role) {
        RoleEntry entry = (RoleEntry)m_roleMap.get( role );
        if ( entry == null)
            return Collections.emptyList();
        return entry.getHintSet();
    }

    public Map<Object,Object> lookupServicesFor(String role) throws RaplaContextException {
        Map<Object,Object> map = new LinkedHashMap<Object,Object>();
        for (Iterator<?> it = getAllServicesFor(role).iterator();it.hasNext();) {
            Object hint =  it.next();
            map.put(hint, getContext().lookup( role  + "/" + hint.toString()));
        }
        return map;
    }


    /**
     * @param roleName
     * @param hint
     * @param handler
     */
    private void addHandler(String roleName, Object hint, ComponentHandler handler) {
        m_componentHandler.add(  handler);
        RoleEntry entry = m_roleMap.get( roleName );
        if ( entry == null)
            entry = new RoleEntry();
        entry.put( hint , handler);
        m_roleMap.put( roleName, entry);
    }


    synchronized ComponentHandler getHandler( String role,Object hint) {
        RoleEntry entry = (RoleEntry)m_roleMap.get( role );
        if ( entry == null)
        {
            return null;
        }

        ComponentHandler handler = entry.getHandler( hint );
        if ( handler != null)
        {
            return handler;
        }
        if ( hint == null || hint.equals("*" ) )
            return entry.getFirstHandler();
        // Try the first accessible handler
        return null;
    }





    class RoleEntry {
        Map<Object,ComponentHandler> componentMap = new LinkedHashMap<Object,ComponentHandler>();
        ComponentHandler firstEntry;

        RoleEntry() {

        }

        void put( Object hint, ComponentHandler handler ){
            componentMap.put( hint, handler);
            if (firstEntry == null)
                firstEntry = handler;
        }

        Set<?> getHintSet() {
            return componentMap.keySet();
        }

        ComponentHandler getHandler(Object hint) {
            return  componentMap.get( hint );
        }

        ComponentHandler getFirstHandler() {
            return firstEntry;
        }
        public String toString()
        {
        	return componentMap.toString();
        }

    }

    public RaplaContext getContext() {
        return m_context;
    }

    /**
     * @see org.apache.avalon.framework.activity.Disposable#dispose()
     */
    public void dispose() {
        removeAllComponents();
    }

    protected void removeAllComponents() {
        Iterator<ComponentHandler> it = m_componentHandler.iterator();
        while ( it.hasNext() ) {
            it.next().dispose();
        }
        m_componentHandler.clear();
        m_roleMap.clear();

    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
	static Constructor findDependantConstructor(Class componentClass) {
        Constructor[] construct= componentClass.getConstructors();
        for (int i=0;i< construct.length;i++) {
            Class[] types = construct[i].getParameterTypes();
            for (int j=0; j< types.length; j++ ) {
                Class type = types[j];
                if ( type.isAssignableFrom( RaplaContext.class) || type.isAssignableFrom( Configuration.class) ||
                        type.isAssignableFrom(Logger.class)) {

                    return construct[i];
                }

            }
        }
        return null;
    }


    protected ThreadLocal<RemoteSession> request = new ThreadLocal<RemoteSession>();
    
    /** instanciates a class and passes the config, logger and the parent context to the object if needed by the constructor.
     * This concept is taken form pico container.*/
    @SuppressWarnings({ "rawtypes", "unchecked" })
	protected Object instanciate( String componentClassName, Configuration config, Logger logger ) throws RaplaContextException
    {
    	RaplaContext context = m_context;
		Class componentClass;
        try {
            componentClass = Class.forName( componentClassName );
        } catch (ClassNotFoundException e1) {
            throw new RaplaContextException(componentClassName,e1.getMessage() , e1);
        }
		Constructor c = findDependantConstructor( componentClass );
        Object[] params = null;
        if ( c != null) {
            Class[] types = c.getParameterTypes();
            params = new Object[ types.length ];
            Class unknownParameter = null;
            for (int i=0; i< types.length; i++ ) {
                Class type = types[i];
                if ( type.isAssignableFrom( RaplaContext.class)) {
                    params[i] = context;
                } else  if ( type.isAssignableFrom( Configuration.class)) {
                    params[i] = config;
                } else if ( type.isAssignableFrom( Logger.class)) {
                    params[i] = logger;
                } else {
                    String guessedRole = type.getClass().getName();
                    if ( context.has( guessedRole )) {
                        params[i] = context.lookup( guessedRole );
                    } else {
                        unknownParameter = type;
                        break;
                    }
                }
            }
            if ( unknownParameter != null) {
                throw new RaplaContextException(componentClassName, "Can't statisfy constructor dependency " + unknownParameter.getName() + " for class " + componentClassName);
            }
        }
        try {
            Object component;
            if ( c!= null) {
                component = c.newInstance( params);
            } else {
                component = componentClass.newInstance();
            }
            ContainerUtil.start(component);
            return component;
        } catch (Exception ex) {
            throw new RaplaContextException(componentClassName, ex.getMessage(), ex);
        }
    }

    protected class ComponentHandler implements Disposable {
        protected Configuration config;
        protected Logger logger;
        protected Object component;
        protected String componentClassName;
        boolean dispose = true;
        protected ComponentHandler( Object component) {
            this.component = component;
            this.dispose = false;
        }

        protected ComponentHandler( Configuration config, String componentClass, Logger logger) {
            this.config = config;
            this.componentClassName = componentClass;
            this.logger = logger;
        }


        Object get() throws RaplaContextException {
            if ( component == null)
                component = instanciate( componentClassName, config, logger );

            return component;
        }



        public void dispose() {
            if ( !dispose)
                return;
            try {
                ContainerUtil.stop(  component );
            } catch ( Exception ex) {
                getLogger().error("Error stopping component ", ex );
            }
            ContainerUtil.dispose(  component );
        }

        
        public String toString()
        {
        	if ( component != null)
        	{
        		return component.toString();
        	}
        	if ( componentClassName != null)
        	{
        		return componentClassName.toString();
        	}
        	else
        	{
        		return super.toString();
        	}
        }
    }

 }

