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
package org.rapla.server;

import org.rapla.framework.RaplaException;
/** You will need this remote-service to shutdown the server from
   another process. This is for-example necessary if the server is running in
   the background.

   To prevent a shutdown from unprivileged users on the server-host specify
   a shutdown-password.
   Hint: This is only useful if the configuration can't be
   read by unprivileged users ;-).
*/

public interface ShutdownService 
{
    String ROLE = ShutdownService.class.getName();
    void shutdown(String shutdownPassword, boolean restart) throws RaplaException;
    void addShutdownListener(ShutdownListener listener);
    void removeShutdownListener(ShutdownListener listener);
}
