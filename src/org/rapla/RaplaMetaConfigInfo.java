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

import java.util.HashMap;

import org.rapla.client.ClientService;
import org.rapla.client.internal.RaplaClientServiceImpl;
import org.rapla.components.mail.MailInterface;
import org.rapla.components.mail.SmtpClient;
import org.rapla.components.xmlbundle.I18nBundle;
import org.rapla.components.xmlbundle.impl.I18nBundleImpl;
import org.rapla.facade.ClientFacade;
import org.rapla.facade.internal.FacadeImpl;
import org.rapla.framework.internal.ComponentInfo;
import org.rapla.plugin.jndi.JNDIAuthenticationStore;
import org.rapla.server.ServerService;
import org.rapla.server.ShutdownService;
import org.rapla.server.internal.ServerServiceImpl;
import org.rapla.server.internal.ShutdownServiceImpl;
import org.rapla.storage.CachableStorageOperator;
import org.rapla.storage.ImportExportManager;
import org.rapla.storage.StorageOperator;
import org.rapla.storage.dbfile.FileOperator;
import org.rapla.storage.dbrm.RemoteOperator;
import org.rapla.storage.dbsql.DBOperator;
import org.rapla.storage.impl.ImportExportManagerImpl;


class RaplaMetaConfigInfo  extends HashMap<String,ComponentInfo> {
    private static final long serialVersionUID = 1L;
    
    public RaplaMetaConfigInfo() {
        put( "rapla-client", new ComponentInfo(RaplaClientServiceImpl.class.getName(),ClientService.class.getName()));
        put( "resource-bundle",new ComponentInfo(I18nBundleImpl.class.getName(),I18nBundle.class.getName()));
        put( "facade",new ComponentInfo(FacadeImpl.class.getName(),ClientFacade.class.getName()));
        put( "file-storage",new ComponentInfo(FileOperator.class.getName(),new String[] {StorageOperator.class.getName(), CachableStorageOperator.class.getName()}));
        put( "remote-storage",new ComponentInfo(RemoteOperator.class.getName(),new String[] {StorageOperator.class.getName(), CachableStorageOperator.class.getName()}));
        put( "db-storage",new ComponentInfo(DBOperator.class.getName(),new String[] {StorageOperator.class.getName(), CachableStorageOperator.class.getName()}));
        put( "sql-storage",new ComponentInfo(DBOperator.class.getName(),new String[] {StorageOperator.class.getName(), CachableStorageOperator.class.getName()}));
        
        put( "importexport", new ComponentInfo(ImportExportManagerImpl.class.getName(),ImportExportManager.class.getName()));
        put( "shutdown-service", new ComponentInfo(ShutdownServiceImpl.class.getName(),ShutdownService.class.getName()));
        put( "jndi-authentication", new ComponentInfo(JNDIAuthenticationStore.class.getName()));

        put( "rapla-server", new ComponentInfo(ServerServiceImpl.class.getName(),ServerService.class.getName()));
        put("smtp-service", new ComponentInfo( SmtpClient.class.getName(), MailInterface.ROLE ));
    }
}


