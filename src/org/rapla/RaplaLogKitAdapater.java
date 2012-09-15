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
package org.rapla;


import org.apache.avalon.excalibur.logger.LogKitLoggerManager;
import org.apache.avalon.excalibur.logger.LoggerManager;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.context.DefaultContext;
import org.apache.avalon.framework.logger.Logger;
import org.rapla.components.util.IOUtil;
import org.rapla.framework.RaplaException;
import org.rapla.framework.StartupEnvironment;
import org.rapla.framework.internal.LogManagerAdapter;


class RaplaLogKitAdapater implements LogManagerAdapter {

    LoggerManager m_loggerManager;

    public RaplaLogKitAdapater(StartupEnvironment env, Configuration loggerConfig) throws RaplaException {
        try {
        	final String lmLoggerName = loggerConfig.getAttribute( "logger","system.logkit" );
        	m_loggerManager= new LogKitLoggerManager(  "rapla", lmLoggerName );
        	DefaultContext context = new DefaultContext();
        	context.put( "context-root",IOUtil.getFileFrom(env.getContextRootURL()));
            ContainerUtil.enableLogging(  m_loggerManager, env.getBootstrapLogger());
            ContainerUtil.contextualize(  m_loggerManager, context);
            ContainerUtil.configure( m_loggerManager, loggerConfig);
        } catch (Exception ex) {
            throw new RaplaException(ex);
        }

    }
    public Logger getLoggerForCategory(String categoryName) {
        return m_loggerManager.getLoggerForCategory( categoryName );
    }

    public Logger getDefaultLogger() {
        return m_loggerManager.getDefaultLogger();
    }


}
