package org.rapla.framework;



public interface RaplaContext
{
	/** Returns a reference to the requested object (e.g. a component instance).
	 *  throws a RaplaContextException if the object can't be returned. This could have
	 *  different reasons: For example it is not found in the context, or there has been 
	 *  a problem during the component creation.   
	 */
    Object lookup( String key ) throws RaplaContextException;
    boolean has( String key );
}
