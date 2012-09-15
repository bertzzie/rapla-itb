package org.rapla.plugin.urlencryption;

import java.util.Date;

import org.rapla.facade.RaplaComponent;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;

/**
 * Remote Stub for UrlEncryptionService.
 * This class has to be initialized on the client side.
 * 
 * @author Jonas Kohlbrenner
 * 
 */
public class UrlEncryptionRemote extends RaplaComponent implements UrlEncryption
{
	public UrlEncryptionRemote(RaplaContext context) throws RaplaException
	{
		super(context);
	}
	
	public String encrypt(String plain) throws RaplaException
	{	
		return getWebservice(UrlEncryption.class).encrypt(plain);
	}
	
	public Date getKeyGenerationTime()
	{
		return getWebservice(UrlEncryption.class).getKeyGenerationTime();
	}
}
