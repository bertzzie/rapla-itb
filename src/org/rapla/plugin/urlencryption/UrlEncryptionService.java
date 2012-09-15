package org.rapla.plugin.urlencryption;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.commons.codec.binary.Base64;
import org.rapla.entities.User;
import org.rapla.entities.configuration.Preferences;
import org.rapla.entities.configuration.RaplaConfiguration;
import org.rapla.facade.ClientFacade;
import org.rapla.facade.RaplaComponent;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.gui.internal.common.CalendarModelImpl;
import org.rapla.gui.internal.common.CalendarNotFoundExeption;

/**
 * This class provides functionality to encrypt URL parameters to secure the resource export.
 * The class runs on the server and implements the Interface UrlEncryption which provides
 * encryption service to all clients and some minor utilities.
 *
 * @author Jonas Kohlbrenner
 */
public class UrlEncryptionService
        extends RaplaComponent
        implements UrlEncryption {
    public static String KEY_ATTRIBUTE_NAME = "urlEncKey";
    public static String GENERATION_TIME_ATTRIBUTE_NAME = "genTime";

    private byte[] encryptionKey;
    private SimpleDateFormat genTimeFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm");
    private Date generationTime;

    private Cipher encryptionCipher;
    private Cipher decryptionCipher;

    private Base64 base64;

    /**
     * Initializes the Url encryption plugin.
     * Checks whether an encryption key exists or not, reads an existing one from the configuration file
     * or generates a new one. The decryption and encryption ciphers are also initialized here.
     *
     * @param context
     * @param config
     * @throws RaplaException
     */
    public UrlEncryptionService(RaplaContext context, Configuration config) throws RaplaException, InvalidKeyException {
        super(context);

        byte[] linebreake = {};
        this.base64 = new Base64(64, linebreake, true);

        // Try to red the encryption key from the plugin configuration file.
        try {
            try {
                this.generationTime = this.genTimeFormat.parse(config.getAttribute(UrlEncryptionService.GENERATION_TIME_ATTRIBUTE_NAME));
            } catch (Exception e) {
                getLogger().error("Parsing Key Generation Time failed.");
                this.generationTime = new Date(0);
            }

            this.encryptionKey = this.base64.decode(config.getAttribute(UrlEncryptionService.KEY_ATTRIBUTE_NAME));
            if (this.encryptionKey == null || this.encryptionKey.equals(""))
                throw new InvalidKeyException("Empty key string found!");

            this.initializeCiphers(this.encryptionKey);
        } catch (ConfigurationException ce) {
            // No encryption key found in the plugin configuration!
            this.initializePlugin();
        }
    }

    /**
     * Generates a private encryption key and persists it to the configuration file.
     */
    private void initializePlugin() {
        try {
            this.generateKey();
            this.saveConfiguration();

            this.initializeCiphers(this.encryptionKey);
        } catch (NoSuchAlgorithmException nsae) {
            // AES Algorithm not existing
            getLogger().error("AES Algorithm not existing. Inizialization failed!");
        } catch (InvalidKeyException ike) {
            // Something went wrong while generating the key
            //System.err.println("The encryption key couldn't be initialized. Inizialization failed!");
        }
    }

    /**
     * Initializes the encryption an decryption Cipher so they can be used.
     *
     * @param encryptionKey
     */
    private void initializeCiphers(byte[] encryptionKey) throws InvalidKeyException {
        try {
            Key specKey = new SecretKeySpec(encryptionKey, "AES");

            this.encryptionCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            this.encryptionCipher.init(Cipher.ENCRYPT_MODE, specKey);

            this.decryptionCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            this.decryptionCipher.init(Cipher.DECRYPT_MODE, specKey);
        } catch (NoSuchAlgorithmException e) {
            // AES Algorithm does not exist here
            getLogger().error("AES Algorithm does not exist here");
        } catch (NoSuchPaddingException e) {
            // AES/ECB/PKCS5 Padding missing
        	getLogger().error("AES/ECB/PKCS5 Padding missing");
        }
    }

    /**
     * Generates the encryption key.
     *
     * @throws NoSuchAlgorithmException
     */
    protected void generateKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128);
        SecretKey sKey = keyGen.generateKey();
        this.encryptionKey = sKey.getEncoded();

        this.generationTime = new Date();
    }

    /**
     * Persists the encryption key to the configuration file of the plugin.
     */
    private void saveConfiguration() {
        // Client facade has to be used for configuration manipulation.
        // If the Service is only established on the server, the key doesn't get transfered!
        ClientFacade facade = this.getClientFacade();
        try {
            Preferences preferences = (Preferences) facade.edit(facade.getPreferences(null));
            RaplaConfiguration config = (RaplaConfiguration) preferences.getEntry("org.rapla.plugin");

            final Configuration oldClass = config.find("class", UrlEncryptionPlugin.PLUGIN_CLASS);

            DefaultConfiguration newChild = new DefaultConfiguration(oldClass);
            newChild.setAttribute(UrlEncryptionService.KEY_ATTRIBUTE_NAME, this.base64.encodeToString(this.encryptionKey));
            newChild.setAttribute(UrlEncryptionService.GENERATION_TIME_ATTRIBUTE_NAME, this.genTimeFormat.format(this.generationTime));


            RaplaConfiguration newConfig = config.replace(oldClass, newChild);
            preferences.putEntry("org.rapla.plugin", newConfig);

            facade.store(preferences);
        } catch (Exception e) {
        	getLogger().error("Error writing URL Encryption Startup Config!\n" + e.getMessage());
        }
    }

    /**
     * Looks whether an HttpServletRequest contains encrypted parameters.
     * If so, the parameters will be decrypted and passed on together with the original request.
     *
     * @param request Original request
     * @return HttpServletRequest Request with decrypted URL parameters
     */
    public HttpServletRequest handleEncryptedSource(HttpServletRequest request) {
        String parameters = request.getParameter(UrlEncryption.ENCRYPTED_PARAMETER_NAME);

        // If no encrypted parameters are provided return the original request.
        if (parameters == null)
            return request;
        try {
            parameters = this.decrypt(parameters);
        } catch (Exception e) {
            // The parameters could't been decrypted.
            return request;
        }

        Map<String, String[]> parameterMap = new TreeMap<String, String[]>();
        StringTokenizer valuePairs = new StringTokenizer(parameters, "&");

        // parse the key - value pairs from the encrypted parameter
        while (valuePairs.hasMoreTokens()) {
            String[] pair = valuePairs.nextToken().split("=");
            try {
                parameterMap.put(pair[0], new String[]{pair[1]});
            } catch (ArrayIndexOutOfBoundsException e) {
                // In case, the given parameter doesn't have a value pass an empty array as value.
                parameterMap.put(pair[0], new String[]{});
            }
        }

        return new EncryptedHttpServletRequest(request, parameterMap);
    }

    /**
     * Checks if the requested page has been called illegally.
     * This is the case, when the page was called using plain parameters
     * instead of encrypted ones.
     *
     * @param request Page request Object
     * @return boolean true if page was called illegally
     */
    public boolean isCalledIllegally(HttpServletRequest request) throws RaplaException {
        String username = request.getParameter("user");
        String filename = request.getParameter("file");

        if (filename == null) {
            // no calendar information, so no model and no a
            return false;
        }

        final User user = getQuery().getUser(username);
        final CalendarModelImpl model = new CalendarModelImpl(getContext(), user);
        try
        {
        	model.load(filename);
        } 
        catch (CalendarNotFoundExeption ex)
        {
        	return false;            	
        }


        // check if URL encryption is activated in general
        boolean encryptionActivated = getContext().has(UrlEncryption.class.getName());

        // check if encryption is enabled for the requested source
        boolean encryptionEnabled = false;
        Object encryptionOption = model.getOption(UrlEncryptionPlugin.URL_ENCRYPTION);
        if (encryptionOption != null)
            encryptionEnabled = encryptionOption.equals("true");

        // check if the page was called via encrypted parameters
        boolean calledViaEncryptedParameter = false;
        if (request.getParameter(UrlEncryption.ENCRYPTED_PARAMETER_NAME) != null && request instanceof EncryptedHttpServletRequest) {
            // check if there are additional parameters which are already passed with the encrypted one
            EncryptedHttpServletRequest requestCopy = (EncryptedHttpServletRequest) request;
            if (requestCopy.getOriginalParameter("file") == null && requestCopy.getOriginalParameter("user") == null && requestCopy.getOriginalParameter("page") == null)
                calledViaEncryptedParameter = true;
        }

        if (encryptionActivated && encryptionEnabled && !calledViaEncryptedParameter)
            return true;
        else
            return false;
    }

    /**
     * Encrypts any text using the generated encryption key.
     *
     * @param plain Plain text
     * @return String The encrypted result or null in case of an exception
     */
    public synchronized String encrypt(String plain) {
        try {
            return this.base64.encodeToString(this.encryptionCipher.doFinal(plain.getBytes()));
        } catch (IllegalBlockSizeException e) {
            // Something went wrong while converting the plain String into byte array
        } catch (BadPaddingException e) {
            // Something went wrong while initializing the used cipher
        }
        return "";
    }

    /**
     * Decrypts the provided string using the encryption key defined at the plugins initialization.
     *
     * @param encrypted Encrypted string
     * @return String Plain string
     * @throws Exception If the String could't be decrypted.
     */
    protected synchronized String decrypt(String encrypted) throws Exception {
        try {
            return new String(this.decryptionCipher.doFinal(this.base64.decode(encrypted.getBytes())));
        } catch (Exception e) {
            throw new Exception("The provided URL is not valide.");
        }
    }

    /**
     * @return Date Date, the encryption key was generated.
     */
    public Date getKeyGenerationTime() {
        return this.generationTime;
    }


    public boolean isEnabled() {
        Preferences preferences = null;
        try {
            preferences = (Preferences) getClientFacade().edit(getClientFacade().getPreferences(null));
            RaplaConfiguration config = (RaplaConfiguration) preferences.getEntry("org.rapla.plugin");
            return config.getConfig().getAttributeAsBoolean("enabled", UrlEncryptionPlugin.ENABLE_BY_DEFAULT);
        } catch (RaplaException e) {

        }
        return false;
    }
}
