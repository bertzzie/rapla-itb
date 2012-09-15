package org.rapla;

import java.util.ArrayList;
import java.util.List;

import org.apache.avalon.excalibur.logger.LogTargetFactory;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.log.LogEvent;
import org.apache.log.LogTarget;
import org.apache.log.Priority;
import org.apache.log.output.AbstractTarget;

public class RaplaTestLogManager implements LogTargetFactory {
	List<String> messages = new ArrayList<String>();
	static ThreadLocal<RaplaTestLogManager> localManager = new ThreadLocal<RaplaTestLogManager>();
	
    public RaplaTestLogManager() {
		localManager.set( this);
		clearMessages();
    }

    public void clearMessages() {
    	messages.clear();
	}
    
    static public List<String> getErrorMessages()
    {
    	return localManager.get().messages;
    }


	public LogTarget createTarget(Configuration arg0)
			throws ConfigurationException {
		return new AbstractTarget() 
		{
		@Override
			protected synchronized boolean isOpen() {
				return true;
			}	
			@Override
			protected void doProcessEvent(LogEvent arg0) throws Exception {
				Priority priority = arg0.getPriority();
				if (priority == Priority.ERROR || priority == Priority.FATAL_ERROR){
					messages.add(arg0.getMessage());
				}
			}
		};
	}



}
