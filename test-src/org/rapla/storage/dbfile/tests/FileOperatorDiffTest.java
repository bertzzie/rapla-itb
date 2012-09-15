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
package org.rapla.storage.dbfile.tests;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.rapla.RaplaTestCase;
import org.rapla.entities.User;
import org.rapla.entities.storage.RefEntity;
import org.rapla.framework.RaplaException;
import org.rapla.storage.CachableStorageOperator;


public class FileOperatorDiffTest extends RaplaTestCase {
    CachableStorageOperator operator;

    public FileOperatorDiffTest(String name) {
        super(name);
    }

    public boolean differ(String file1, String file2) throws IOException {
        FileInputStream in1 = null;
        FileInputStream in2 = null;
        boolean bDiffer = false;
        try {
            in1 = new FileInputStream(file1);
            in2 = new FileInputStream(file2);
            while (true) {
                int b1 = in1.read();
                int b2 = in2.read();
                if (b1 != b2) {
                    bDiffer = true;
                    break;
                }
                if (b1 == -1) {
                    break;
                }
            }
            return bDiffer;
        }
        finally {
            if (in1 != null)
              in1.close();
            if (in2 != null)
              in2.close();
        }
    }

    public static Test suite() {
        return new TestSuite(FileOperatorDiffTest.class);
    }

    public void setUp() throws Exception {
        super.setUp();
        operator = (CachableStorageOperator)
            getContext().lookup(CachableStorageOperator.ROLE +"/file");
    }

    public void testSave() throws RaplaException,IOException  {
        String testFile = "test-src/testdefault.xml";
        assertTrue(differ(TEST_FOLDER_NAME + "/test.xml",testFile) == false);
        operator.connect();
        RefEntity<?> obj = (RefEntity<?>)operator.getObjects( User.class).iterator().next();
        RefEntity<?> clone = operator.editObject(obj, null);
        
        Collection<RefEntity<?>> storeList = new ArrayList<RefEntity<?>>(1);
        storeList.add( clone);
		Collection<RefEntity<?>> removeList = Collections.emptyList();
		operator.storeAndRemove(storeList, removeList, null);
        assertTrue("stored version differs from orginal " + testFile
                   , differ(TEST_FOLDER_NAME + "/test.xml",testFile) == false );
    }

}





