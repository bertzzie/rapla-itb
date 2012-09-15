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
package org.rapla.plugin.jndi.internal;

import java.util.Map;
import java.util.TreeMap;

import javax.naming.CommunicationException;

import org.apache.avalon.framework.logger.Logger;
import org.rapla.facade.RaplaComponent;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.plugin.jndi.JNDIAuthenticationStore;

public class RaplaJNDITestOnLocalhost extends RaplaComponent implements JNDITest
{
        public RaplaJNDITestOnLocalhost( RaplaContext context) throws RaplaException {
            super( context );
        }

        public void test(String config,String username,String password ) throws RaplaException 
        {
            String[] test = config.split("RAPLANEXT");
            Map<String,String> map = new TreeMap<String,String>();
            for (int i =0;i<test.length;i++)
            {
                String next = test[i];
                int delimeter = next.indexOf("=");
                String key = next.substring(0, delimeter);
                String value = next.substring( delimeter+1);
                map.put(key, value);
            }
            JNDIAuthenticationStore testStore;
            Logger logger = getLogger();
            testStore = JNDIAuthenticationStore.createJNDIAuthenticationStore(map,
                    logger);
            logger.info("Test of JNDI Plugin started");
            boolean authenticate;
            try {
                testStore.start();
                authenticate = testStore.authenticate(username, password);
                testStore.stop();
            } catch (CommunicationException e) {
                  throw new RaplaException("Can't connect to server " + e.getMessage() ,e);
            } catch (Exception e) {
            	throw new RaplaException(e);
            }
            if (!authenticate)
            {
                throw new RaplaException("Can establish connection but can't authenticate test user " + username);
            }
            logger.info("Test of JNDI Plugin successfull");
        }

        
}

