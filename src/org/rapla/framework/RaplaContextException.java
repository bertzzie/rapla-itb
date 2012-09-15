package org.rapla.framework;


public class RaplaContextException
    extends RaplaException

{
    private static final long serialVersionUID = 1L;
    
    public RaplaContextException( final String key, final String message ) {
        super( key + " " + message);
    }
      
    public RaplaContextException( final String key, final String message, final Throwable throwable )
    {
        super( message, throwable );
    }


}
