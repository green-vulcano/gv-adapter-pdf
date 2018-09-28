/*******************************************************************************
 * Copyright (c) 2009, 2016 GreenVulcano ESB Open Source Project.
 * All rights reserved.
 *
 * This file is part of GreenVulcano ESB.
 *
 * GreenVulcano ESB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * GreenVulcano ESB is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *  
 * You should have received a copy of the GNU Lesser General Public License
 * along with GreenVulcano ESB. If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package it.greenvulcano.esb.virtual.pdf;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Optional;

import org.slf4j.Logger;
import org.w3c.dom.Node;
import org.xhtmlrenderer.pdf.ITextRenderer;

import it.greenvulcano.configuration.XMLConfig;
import it.greenvulcano.gvesb.buffer.GVBuffer;
import it.greenvulcano.gvesb.virtual.CallException;
import it.greenvulcano.gvesb.virtual.CallOperation;
import it.greenvulcano.gvesb.virtual.ConnectionException;
import it.greenvulcano.gvesb.virtual.InitializationException;
import it.greenvulcano.gvesb.virtual.InvalidDataException;
import it.greenvulcano.gvesb.virtual.OperationKey;
import it.greenvulcano.util.metadata.PropertiesHandler;

/**
 * 
 * @version 4.0 07/august/2016
 * @author GreenVulcano Developer Team
 */
public class HTML2PDFCallOperation implements CallOperation {

	private static final Logger logger = org.slf4j.LoggerFactory.getLogger(HTML2PDFCallOperation.class);
	private OperationKey key = null;

	protected String name;
	protected String srcPath;
	protected String trgPath;

	@Override
	public void init(Node node) throws InitializationException {
		logger.debug("Init start");
		try {
			name = XMLConfig.get(node, "@name");
			srcPath = XMLConfig.get(node, "@source");
			trgPath = XMLConfig.get(node, "@target");

		} catch (Exception exc) {
			throw new InitializationException("GV_INIT_SERVICE_ERROR",
					new String[][] { { "message", exc.getMessage() } }, exc);
		}

	}

	@Override
	public GVBuffer perform(GVBuffer gvBuffer) throws ConnectionException, CallException, InvalidDataException {

		try {
			
			ITextRenderer renderer = new ITextRenderer();
						
      	  	if (srcPath==null) {
      	  		
      	  		logger.debug("Reading input XHTML from GVBuffer ...");
      	  		
	      	  	byte[] htmlData = new byte[] {};
				if (gvBuffer.getObject() != null) {
	
					if (gvBuffer.getObject() instanceof byte[]) {
						htmlData = (byte[]) gvBuffer.getObject();
					
					} else if (gvBuffer.getObject() instanceof String) {
	
						String charset = Optional.ofNullable(gvBuffer.getProperty("OBJECT_ENCODING")).orElse("UTF-8");
						htmlData = gvBuffer.getObject().toString().getBytes(charset);
	
					} else {
	
						throw new IllegalArgumentException("Invalid input data: " + gvBuffer.getObject());
					}
				}
				
				renderer.setDocument(htmlData);
			} else {
				
				String realSrcPath = PropertiesHandler.expand(srcPath, gvBuffer);				
				logger.debug("Reading input XHTML from file {}", realSrcPath);
				
				if (realSrcPath.startsWith("http")) {
					renderer.setDocument(realSrcPath);
				} else {
					renderer.setDocument(new File(realSrcPath));
				}
				
			}
	        
	      
      	    renderer.layout();      	    
      	    
      	    if (trgPath==null) {
      	    	
      	    	logger.debug("Writing output PDF into GVBuffer...");
      	    	
      	    	ByteArrayOutputStream output = new ByteArrayOutputStream();
      	    	renderer.createPDF(output);
      	          	    	
      	    	gvBuffer.setObject(output.toByteArray());
      	    	
      	    	output.close();
      	    } else {
      	    	
      	    	String realTrgPath = PropertiesHandler.expand(trgPath, gvBuffer);
      	    	
      	    	logger.debug("Writing output PDF into file {}", realTrgPath);
      	    	
      	    	FileOutputStream fos = new FileOutputStream(realTrgPath);
    		    renderer.createPDF(fos);
    		    fos.close();
      	    }
      	    		    
	        
		} catch (Exception exc) {
			throw new CallException("GV_CALL_SERVICE_ERROR",
					new String[][] { { "service", gvBuffer.getService() }, { "system", gvBuffer.getSystem() },
							{ "tid", gvBuffer.getId().toString() }, { "message", exc.getMessage() } },
					exc);
		}
		return gvBuffer;
	}

	@Override
	public void cleanUp() {
		// do nothing
	}

	@Override
	public void destroy() {
		// do nothing
	}

	@Override
	public String getServiceAlias(GVBuffer gvBuffer) {
		return gvBuffer.getService();
	}

	@Override
	public void setKey(OperationKey key) {
		this.key = key;
	}

	@Override
	public OperationKey getKey() {
		return key;
	}
}
