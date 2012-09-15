/*--------------------------------------------------------------------------*
 | Copyright (C) 2006 Christopher Kohlhaas                                  |
 |                                                                          |
 | This program is free software; you can redistribute it and/or modify     |
 | it under the terms of the GNU General Public License as published by the |
 | Free Software Foundation. A copy of the license has been included with   |
 | these distribution in the COPYING file, if not go to www.fsf.org         |
 |                                                                          |
 | As a special exception, you are granted the permissions to link this     |
 | program with every library, which license fulfills the Open Source       |
 | Definition as published by the Open Source Initiative (OSI).             |
 *--------------------------------------------------------------------------*/

package org.rapla.components.iolayer;

import java.awt.print.*;
import javax.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;
import java.io.*;

/** This class will only work with JDK 1.4 and above, it 
    uses javax.print.PrintService for exporting to postscript format
 */

public class PrintExport  {
    static DocFlavor flavor = DocFlavor.SERVICE_FORMATTED.PRINTABLE;

    private static StreamPrintServiceFactory getPSExportServiceFactory() {
        StreamPrintServiceFactory []factories =
            StreamPrintServiceFactory.lookupStreamPrintServiceFactories(flavor,"application/postscript");
	
    	if (factories.length == 0) {
    	    return null;
    	} 
    	/*
    	  for (int i=0;i<factories.length;i++) {
    	  System.out.println("Stream Factory " + factories[i]);
    	  System.out.println("  Output " + factories[i].getOutputFormat());
    	  DocFlavor[] docFlavors = factories[i].getSupportedDocFlavors();
    	  for (int j=0;j<docFlavors.length;j++) {
    	  System.out.println("  Flavor " + docFlavors[j].getMimeType());
    	  }
    	  }*/
    	return factories[0];
    }
    
    public static boolean supportsPostscriptExport() {
        return getPSExportServiceFactory() != null;
    }

    public void save(Printable print,PageFormat format,OutputStream out) throws IOException {
    	StreamPrintService sps = null;
    	try {
    	    StreamPrintServiceFactory spsf = getPSExportServiceFactory();
    	    if (spsf == null)
            {
    	        throw new UnsupportedOperationException("No suitable factories for postscript-export.");
            }
    	    sps = spsf.getPrintService(out);
    	    Doc  doc = new SimpleDoc(print, flavor, null);
    	    PrintRequestAttributeSet aset = new HashPrintRequestAttributeSet();
    	    if (format.getOrientation() == PageFormat.LANDSCAPE)
            {
    	        aset.add(OrientationRequested.LANDSCAPE);
            }
    	    if (format.getOrientation() == PageFormat.PORTRAIT)
            {
    	        aset.add(OrientationRequested.PORTRAIT);
            }
    	    if (format.getOrientation() == PageFormat.REVERSE_LANDSCAPE)
            {
    	        aset.add(OrientationRequested.REVERSE_LANDSCAPE);
            }
    
    	    aset.add(MediaName.ISO_A4_WHITE);
    	    Paper paper = format.getPaper();
    	    if (sps.getSupportedAttributeValues(MediaPrintableArea.class,null,null) != null)
    	    {
    	        MediaPrintableArea printableArea = new MediaPrintableArea
    		    (
    		     (float)(paper.getImageableX()/72)
    		     ,(float)(paper.getImageableY()/72)
    		     ,(float)(paper.getImageableWidth()/72)
    		     ,(float)(paper.getImageableHeight()/72)
    		     ,Size2DSyntax.INCH
    		     );
    	        aset.add(printableArea);
    	        //  System.out.println("new Area: " + printableArea);
    	    }
    	    sps.createPrintJob().print(doc,aset);
    	} catch (PrintException ex) { 
    	    throw new IOException(ex.getMessage());
    	} finally {
    	    if (sps != null)
    		sps.dispose();
    	}
        }
	
}


