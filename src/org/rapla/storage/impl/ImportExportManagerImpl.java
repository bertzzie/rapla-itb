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
package org.rapla.storage.impl;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.Logger;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaContextException;
import org.rapla.framework.RaplaException;
import org.rapla.storage.CachableStorageOperator;
import org.rapla.storage.ImportExportManager;
import org.rapla.storage.LocalCache;
/**  Imports the content of on store into another.
 Export does an import with source and destination exchanged.
<p>Configuration:</p>
<pre>
  <importexport id="importexport" activation="request">
    <source>file</source>
    <dest>sql</dest>
  </importexport>
</pre>
*/
public class ImportExportManagerImpl implements ImportExportManager {

    RaplaContext serviceManager;
    String sourceString;
    String destString;
    Logger logger;
    
    public ImportExportManagerImpl(RaplaContext serviceManager,Configuration configuration) throws RaplaException
    {
        this.logger = (Logger) serviceManager.lookup( Logger.class.getName());
        this.serviceManager = serviceManager;
        try {
            sourceString = configuration.getChild("source").getValue();
            destString = configuration.getChild("dest").getValue();
        } catch (ConfigurationException e) {
            throw new RaplaException( e);
        }
    }
    
    protected Logger getLogger() {
        return logger;
    }

    /* Import the source into dest.   */
    public void doImport() throws RaplaException {
        getLogger().info("Import started");
        doConvert(sourceString,destString);
        getLogger().info("Import completed");
    }

    /* Export the dest into source.   */
    public void doExport() throws RaplaException {
        getLogger().info("Export started");
        doConvert(destString,sourceString);
        getLogger().info("Export completed");
    }

    private void doConvert(String source,String dest) throws RaplaException {
        CachableStorageOperator sourceOperator;
        CachableStorageOperator destOperator;
        getLogger().info("Converting from " + source + " to " + dest);
        try {
            sourceOperator = ( CachableStorageOperator)
                serviceManager.lookup(CachableStorageOperator.ROLE + "/" + source);
            destOperator = ( CachableStorageOperator)
                serviceManager.lookup(CachableStorageOperator.ROLE + "/" + dest);
        } catch (RaplaContextException ex) {
            throw new RaplaException(ex);
        }
        sourceOperator.connect();
        LocalCache cache = sourceOperator.getCache();
        destOperator.saveData(cache);
        sourceOperator.disconnect();
        destOperator.disconnect();
    }
}
