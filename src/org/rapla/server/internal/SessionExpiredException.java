package org.rapla.server.internal;

import org.rapla.framework.RaplaException;


public class SessionExpiredException extends RaplaException
{
    private static final long serialVersionUID = 1L;

    public SessionExpiredException( String text )
    {
        super( text );
    }

}
