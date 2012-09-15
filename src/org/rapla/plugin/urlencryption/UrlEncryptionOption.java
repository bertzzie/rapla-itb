package org.rapla.plugin.urlencryption;

import java.awt.BorderLayout;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.apache.avalon.framework.configuration.Configuration;
import org.rapla.components.layout.TableLayout;
import org.rapla.framework.RaplaContext;
import org.rapla.framework.RaplaException;
import org.rapla.framework.StartupEnvironment;
import org.rapla.gui.DefaultPluginOption;

/**
 * This class provides the administrators option panel for the URL encryption plugin.
 * 
 * @author Jonas Kohlbrenner
 * 
 */
public class UrlEncryptionOption 
extends DefaultPluginOption 
{
	
	private final JLabel lEncryptionKey = new JLabel();
	
	public UrlEncryptionOption(RaplaContext sm) throws RaplaException
	{
		super(sm);
	}
	
	protected JPanel createPanel() throws RaplaException
	{
		JPanel panel = super.createPanel();
        JPanel content = new JPanel();
        double[][] sizes = new double[][]
        {
        	{
        		5,
        		TableLayout.PREFERRED,
        		5,
        		TableLayout.FILL,
        		5
        	},
        	{
        		TableLayout.PREFERRED,
        		5,
        		TableLayout.PREFERRED,
        		5,
        		TableLayout.PREFERRED,
        		5,
        		TableLayout.PREFERRED,
        		5,
        		TableLayout.PREFERRED,
        		5,
        		TableLayout.PREFERRED
        	}
        };
        
        TableLayout tableLayout = new TableLayout(sizes);
        content.setLayout(tableLayout);
        content.add(new JLabel("Encryption Key: "), "1,4");
        content.add(this.lEncryptionKey, "3,4");
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }
	
    protected void readConfig(Configuration config)
    {
    	 StartupEnvironment env = getService( StartupEnvironment.class );
    	 // Encryption key only available in server mode
    	 if (env.getStartupMode() == StartupEnvironment.CONSOLE)
    	 {
    		 this.lEncryptionKey.setText("No encryption key available");
    		 return;
    	 }
    	try
    	{
    		Date generationTime = getWebservice(UrlEncryption.class).getKeyGenerationTime();
			this.lEncryptionKey.setText("Generated on " + new SimpleDateFormat("yyyy/MM/dd").format(generationTime));
		}
    	catch(Exception e)
    	{
            getLogger().error(e.getMessage());
    		this.lEncryptionKey.setText("No encryption key available");
		}
    }
    
	@Override
	public String getDescriptorClassName()
	{
		return UrlEncryptionPlugin.class.getName();
	}

    public String getName(Locale locale)
    {
        return "URL Encryption";
    }
}
