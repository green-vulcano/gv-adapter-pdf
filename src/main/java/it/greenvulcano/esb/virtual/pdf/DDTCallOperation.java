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
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.w3c.dom.Node;

import com.greenvulcano.dcsp.ddt.DdtGenerator;

import it.greenvulcano.configuration.XMLConfig;
import it.greenvulcano.gvesb.buffer.GVBuffer;
import it.greenvulcano.gvesb.virtual.CallException;
import it.greenvulcano.gvesb.virtual.CallOperation;
import it.greenvulcano.gvesb.virtual.ConnectionException;
import it.greenvulcano.gvesb.virtual.InitializationException;
import it.greenvulcano.gvesb.virtual.InvalidDataException;
import it.greenvulcano.gvesb.virtual.OperationKey;

/**
 * 
 * @version 4.0 07/august/2016
 * @author GreenVulcano Developer Team
 */
public class DDTCallOperation implements CallOperation {

	private static final Logger logger = org.slf4j.LoggerFactory.getLogger(DDTCallOperation.class);
	private OperationKey key = null;

	protected String name;

	@Override
	public void init(Node node) throws InitializationException {
		logger.debug("Init start");
		try {
			name = XMLConfig.get(node, "@name");

		} catch (Exception exc) {
			throw new InitializationException("GV_INIT_SERVICE_ERROR",
					new String[][] { { "message", exc.getMessage() } }, exc);
		}

	}

	@Override
	public GVBuffer perform(GVBuffer gvBuffer) throws ConnectionException, CallException, InvalidDataException {

		try {
			if (gvBuffer.getObject() != null) {
				Object data = gvBuffer.getObject();

				Properties properties = new Properties();
				if(gvBuffer.getProperty("DDT_LABEL_MITTENTE") != null) {
					properties.put("DDT_LABEL_MITTENTE", gvBuffer.getProperty("DDT_LABEL_MITTENTE"));
				}
				
				if(gvBuffer.getProperty("DDT_LABEL_DESTINATARIO") != null) {
					properties.put("DDT_LABEL_DESTINATARIO", gvBuffer.getProperty("DDT_LABEL_DESTINATARIO"));
				}
				
				if(gvBuffer.getProperty("DDT_LABEL_LUOGODESTINAZIONE") != null) {
					properties.put("DDT_LABEL_LUOGODESTINAZIONE", gvBuffer.getProperty("DDT_LABEL_LUOGODESTINAZIONE"));
				}
				
				if(gvBuffer.getProperty("DDT_LABEL_TIPODOC") != null) {
					properties.put("DDT_LABEL_TIPODOC", gvBuffer.getProperty("DDT_LABEL_TIPODOC"));
				}
				
				if(gvBuffer.getProperty("DDT_LABEL_DATADOC") != null) {
					properties.put("DDT_LABEL_DATADOC", gvBuffer.getProperty("DDT_LABEL_DATADOC"));
				}
				
				if(gvBuffer.getProperty("DDT_LABEL_NUMDOC") != null) {
					properties.put("DDT_LABEL_NUMDOC", gvBuffer.getProperty("DDT_LABEL_NUMDOC"));
				}
				
				if(gvBuffer.getProperty("DDT_LABEL_PAGINA") != null) {
					properties.put("DDT_LABEL_PAGINA", gvBuffer.getProperty("DDT_LABEL_PAGINA"));
				}
				
				if(gvBuffer.getProperty("DDT_LABEL_CAUSALE") != null) {
					properties.put("DDT_LABEL_CAUSALE", gvBuffer.getProperty("DDT_LABEL_CAUSALE"));
				}
				
				if(gvBuffer.getProperty("DDT_LABEL_CODICE") != null) {
					properties.put("DDT_LABEL_CODICE", gvBuffer.getProperty("DDT_LABEL_CODICE"));
				}
				
				if(gvBuffer.getProperty("DDT_LABEL_DESCRIZIONE") != null) {
					properties.put("DDT_LABEL_DESCRIZIONE", gvBuffer.getProperty("DDT_LABEL_DESCRIZIONE"));
				}
				
				if(gvBuffer.getProperty("DDT_LABEL_UM") != null) {
					properties.put("DDT_LABEL_UM", gvBuffer.getProperty("DDT_LABEL_UM"));
				}
				
				if(gvBuffer.getProperty("DDT_LABEL_QUANTITA") != null) {
					properties.put("DDT_LABEL_QUANTITA", gvBuffer.getProperty("DDT_LABEL_QUANTITA"));
				}
				
				if(gvBuffer.getProperty("DDT_LABEL_VETTORE") != null) {
					properties.put("DDT_LABEL_VETTORE", gvBuffer.getProperty("DDT_LABEL_VETTORE"));
				}
				
				if(gvBuffer.getProperty("DDT_LABEL_ANNOTAZIONI") != null) {
					properties.put("DDT_LABEL_ANNOTAZIONI", gvBuffer.getProperty("DDT_LABEL_ANNOTAZIONI"));
				}
				
				if(gvBuffer.getProperty("DDT_LABEL_FIRMAMITTENTE") != null) {
					properties.put("DDT_LABEL_FIRMAMITTENTE", gvBuffer.getProperty("DDT_LABEL_FIRMAMITTENTE"));
				}
				
				if(gvBuffer.getProperty("DDT_LABEL_FIRMADESTINATARIO") != null) {
					properties.put("DDT_LABEL_FIRMADESTINATARIO", gvBuffer.getProperty("DDT_LABEL_FIRMADESTINATARIO"));
				}
				
				if(gvBuffer.getProperty("DDT_LABEL_FIRMAVETTORE") != null) {
					properties.put("DDT_LABEL_FIRMAVETTORE", gvBuffer.getProperty("DDT_LABEL_FIRMAVETTORE"));
				}
				
				if(gvBuffer.getProperty("DDT_PAGESIZE_MIN_PRIMAPAGINA") == null) {
					throw new InvalidDataException("DDT_PAGESIZE_MIN_PRIMAPAGINA property is mandatory");
				} else {
					properties.put("DDT_PAGESIZE_MIN_PRIMAPAGINA", gvBuffer.getProperty("DDT_PAGESIZE_MIN_PRIMAPAGINA"));
				}
				
				if(gvBuffer.getProperty("DDT_PAGESIZE_MAX_PRIMAPAGINA") == null) {
					throw new InvalidDataException("DDT_PAGESIZE_MAX_PRIMAPAGINA property is mandatory");
				} else {
					properties.put("DDT_PAGESIZE_MAX_PRIMAPAGINA", gvBuffer.getProperty("DDT_PAGESIZE_MAX_PRIMAPAGINA"));
				}

				if(gvBuffer.getProperty("DDT_PAGESIZE_MAX_PAGINA") == null) {
					throw new InvalidDataException("DDT_PAGESIZE_MAX_PAGINA property is mandatory");
				} else {
					properties.put("DDT_PAGESIZE_MAX_PAGINA", gvBuffer.getProperty("DDT_PAGESIZE_MAX_PAGINA"));
				}
				
				String json = (String) data;
				
				DdtGenerator ddtGenerator = new DdtGenerator(json, properties);
				ByteArrayOutputStream baos = ddtGenerator.convertToPDF();
				String outputFilename = null;
				if((outputFilename = gvBuffer.getProperty("OUTPUT_FILENAME")) != null) {
					FileUtils.writeByteArrayToFile(new File(outputFilename), baos.toByteArray());
				}

				gvBuffer.setObject(baos.toByteArray());
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
