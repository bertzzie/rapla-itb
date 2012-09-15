package org.rapla.plugin.jndi.internal;

import org.rapla.framework.RaplaException;

public interface JNDITest {
    String ROLE = JNDITest.class.getName();
    
    public void test(String config,String username,String password) throws RaplaException;
}
