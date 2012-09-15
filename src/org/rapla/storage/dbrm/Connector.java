package org.rapla.storage.dbrm;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.rapla.framework.RaplaException;

public interface Connector
{

    String getInfo();

    void start() throws Exception;

    InputStream call( String methodName, Map<String,String> args) throws IOException, RaplaException;

    void stop();
    
}
