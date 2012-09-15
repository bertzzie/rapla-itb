package org.rapla.storage.dbrm;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.rapla.components.util.Tools;
import org.rapla.components.xmlbundle.I18nBundle;
import org.rapla.facade.RaplaComponent;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.framework.internal.ConfigTools;

public class HTTPConnector extends RaplaComponent implements Connector
{
    String sessionId;
    URL server;
    
    public HTTPConnector(RaplaContext context, Configuration config) throws RaplaException{
        super(context);
        try
        {
            String configEntry = config.getChild("server").getValue();
            String serverURL = ConfigTools.resolveContext(configEntry, context );
            server = new URL(serverURL);
        }
        catch (MalformedURLException e)
        {
            throw new RaplaException("Malformed url. Could not parse " + server);
        }
        catch (ConfigurationException e)
        {
            throw new RaplaException(e);
        }
    }
    
    public InputStream call(String methodName, Map<String,String> args) throws IOException, RaplaException
    {
        URL methodURL = new URL(server,"rapla/rpc/" + methodName );
        //URL server = new URL("http://127.0.0.1:8080/vorsorge-optimierer/start");
        
        //System.err.println("Calling " + methodURL.toExternalForm() );
        
        HttpURLConnection conn = (HttpURLConnection)methodURL.openConnection();
        conn.setRequestMethod("POST");
        conn.setUseCaches( false );
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=utf-8");
        conn.setRequestProperty("Cookie","JSESSIONID=" + sessionId);
        setSessionForRequest( conn );
        conn.setDoOutput(true);
        
        try
        {
            conn.connect();
        }
        catch (ConnectException ex)
        {   
            throw new RaplaException(getConnectError(ex));
        }
         
        Writer wr = new OutputStreamWriter(conn.getOutputStream(),"UTF-8");
        addParams( wr, args);
        wr.flush();
        
        try
        {
            updateSession ( conn );
            return new BufferedInputStream(conn.getInputStream());
        } 
        catch (ConnectException ex)
        {   
            throw new RaplaException(getConnectError(ex));
        }
        catch (IOException ex)
        {
            String entry = conn.getHeaderField("X-Error-Stacktrace");
            if ( entry != null)
            {
                Throwable e = getServerException( server);
                if ( e instanceof  RaplaException)
                {
                    throw (RaplaException) e;
                }
                throw new RaplaException( e);
            }
            else
            {
                throw ex;
            }
        }
   }

	protected String getConnectError(ConnectException ex2) {
		try
		{
			return ((I18nBundle)getContext().lookup(I18nBundle.ROLE)).format("error.connect", server);
		}
		catch (Exception ex)
		{
			return "Connection error with server " + server + ": " + ex2.getMessage();
		}
	}

    private void setSessionForRequest( HttpURLConnection connection )
    {
        if ( sessionId != null)
        {
            connection.addRequestProperty("Cookie","JSESSIONID=" + sessionId);
        }
    }
    
    private Throwable getServerException( URL server ) throws IOException, RaplaException
    {
        URL methodURL = new URL(server,"rapla/rpc/getException");
        HttpURLConnection connection = (HttpURLConnection)methodURL.openConnection();
        setSessionForRequest( connection );
        
        //ByteArrayOutputStream output = new ByteArrayOutputStream();

        ObjectInputStream in = new ObjectInputStream( connection.getInputStream());
        Throwable e;
        try
        {
            e = (Throwable)in.readObject();
        }
        catch (Exception e1)
        {
            throw new RaplaException( e1);
        }
        return e;
    }

    private void addParams(Writer writer, Map<String,String> args ) throws IOException
    {
        boolean appendAdd = false;
        for (Iterator<String> it = args.keySet().iterator();it.hasNext();)
        {
            if ( appendAdd)
            {
                writer.write( "&");
            }
            String key = it.next();
            String value= args.get( key);
            {
                String pair = key;
                writer.write( pair);
                if ( value != null)
                {
                	writer.write("="+ URLEncoder.encode(value,"utf-8"));
                }
                appendAdd = true;
            }
//            else
//            {
//                appendAdd = false;
//            }
           
        }
    }

    private void updateSession( URLConnection connection ) throws IOException
    {
       
        Map<String,String> cookies = new HashMap<String,String>();
        String entry = connection.getHeaderField("Set-Cookie");
        if ( entry != null)
        {
            String[] splitted = Tools.split(entry,';');
            if ( splitted.length > 0)
            {
                String[] first = Tools.split(splitted[0],'=');
                cookies.put(first[0], first[1]);
            }
        }
        String sessionId = cookies.get("JSESSIONID");
        if ( sessionId != null)
        {
            this.sessionId = sessionId;
        }
    }
    
    public boolean hasSession()
    {
        
        return sessionId != null;
    }

    public String getInfo()
    {
        return sessionId;
    }

    public void start() throws Exception
    {
        // TODO Auto-generated method stub
        
    }

  
    public void stop()
    {
        // TODO Auto-generated method stub
        
    }

    public long getUpdateIntervall()
    {
        return 30000;
    }
    
}
