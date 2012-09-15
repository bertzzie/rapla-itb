package org.rapla.plugin.urlencryption;

import java.util.Date;

import org.rapla.framework.RaplaException;

/**
 * This Interface is used to provide the encryption functionality to the RAPLA Clients.
 * 
 * @author Jonas Kohlbrenner
 * 
 */
public interface UrlEncryption
{
	String ROLE = UrlEncryption.class.getName();
	
	/**
	 *  Parameter in the URL which contains the encrypted parameters
	 */
	String ENCRYPTED_PARAMETER_NAME = "key";


    /**
	 * Encrypts a given string on the RAPLA server.
	 * 
	 * @param plain Plain parameter string
	 * @return String Encrypted parameter string
	 * @throws RaplaException In case the encryption fails
	 */
    public String encrypt(String plain) throws RaplaException;
    
    /**
     * Returns the generation date of the encryption key
     * @return Date Date, the encryption key was generated.
     */
    public Date getKeyGenerationTime();
}
