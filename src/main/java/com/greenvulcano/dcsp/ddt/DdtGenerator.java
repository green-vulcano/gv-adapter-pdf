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
package com.greenvulcano.dcsp.ddt;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.Properties;
import java.util.Vector;

import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import com.greenvulcano.dcsp.ddt.beans.Articolo;
import com.greenvulcano.dcsp.ddt.beans.DDT;
import com.greenvulcano.dcsp.ddt.beans.Destinatario;
import com.greenvulcano.dcsp.ddt.beans.Destinazione;
import com.greenvulcano.dcsp.ddt.beans.Indirizzo;
import com.greenvulcano.dcsp.ddt.beans.Mittente;
import com.greenvulcano.dcsp.ddt.exceptions.DdtException;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Font.FontFamily;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

public class DdtGenerator {
	
	private static final Logger logger = org.slf4j.LoggerFactory.getLogger(DdtGenerator.class);
	
	public static final float TAB_PADDING = 2.5F;
	
	private DDT documentoDiTrasporto;
	private Integer pagg;
	private Integer totaleArticoli;
	private Integer minPrimaPagina; 
	private Integer maxPrimaPagina; 
	private Integer maxPagina;
	
	private String logoPath;

	private Properties properties;
	
	public DDT getDdt() {
		return documentoDiTrasporto;
	}

	public void setDdt(DDT ddt) {
		this.documentoDiTrasporto = ddt;
	}

	public DdtGenerator () {
		
	}
	
	public DdtGenerator(String json, Properties properties, String logoPath) throws DdtException {

		this.properties = properties;
		this.logoPath = logoPath;
		
		parseDdt(json);
		
		totaleArticoli = 0;
		if(documentoDiTrasporto != null) {
			totaleArticoli = (documentoDiTrasporto.getArticoli() == null) ? 0 : documentoDiTrasporto.getArticoli().size();
		}
		
		minPrimaPagina = Integer.parseInt(properties.getProperty("DDT_PAGESIZE_MIN_PRIMAPAGINA"));
		maxPrimaPagina = Integer.parseInt(properties.getProperty("DDT_PAGESIZE_MAX_PRIMAPAGINA"));
		maxPagina = Integer.parseInt(properties.getProperty("DDT_PAGESIZE_MAX_PAGINA"));

		if(totaleArticoli <= minPrimaPagina) {
			pagg = 1;
		} else if(totaleArticoli <= maxPrimaPagina) {
			pagg = 2;
		} else {
			pagg = 1 + ((totaleArticoli-maxPrimaPagina)/maxPagina) + 1; // 25 articoli per pagina + ultima pagina
		}
	}

	private void parseDdt(String json) throws DdtException {
		JSONObject jsonObject = new JSONObject(json);    
		if(jsonObject != null) {
			this.documentoDiTrasporto = loadDdtFromJson(jsonObject);
		}
	}

	private DDT loadDdtFromJson(JSONObject jDdt) throws DdtException {
		DDT ddt = null;
		DateFormat df = null;
		if(jDdt != null) {
			df = new SimpleDateFormat("d/M/yyyy");
			ddt = new DDT();
			
			String tipoDocumento = jDdt.has("numeroDocumento") ? jDdt.getString("tipoDocumento") : "DDT";
			String dataDocumento = jDdt.has("dataDocumento") ? jDdt.getString("dataDocumento") : df.format(new Date());
			String causaleTrasporto = jDdt.has("causaleTrasporto") ? jDdt.getString("causaleTrasporto") : "conto vendita";
			String vettore = jDdt.has("vettore") ? jDdt.getString("vettore") : "";
			String annotazioni = jDdt.has("annotazioni") ? jDdt.getString("annotazioni") : "";
			String colli = jDdt.has("colli") ? jDdt.getString("colli") : "";

			ddt.setTipoDocumento(tipoDocumento);
			ddt.setDataDocumento(dataDocumento);
			ddt.setCausaleTrasporto(causaleTrasporto);
			ddt.setVettore(vettore);
			ddt.setAnnotazioni(annotazioni);
			ddt.setColli(colli);
			
			if(jDdt.has("numeroDocumento")) {
				ddt.setNumeroDocumento(jDdt.getString("numeroDocumento"));
			} else {
				throw new DdtException("Il numero di documento e' obbligatorio");
			}


			if(jDdt.has("mittente")) {
				JSONObject jMittente = jDdt.getJSONObject("mittente");
				ddt.setMittente(loadMittenteFromJson(jMittente));
			} else {
				throw new DdtException("Il mittente e' obbligatorio");
			}
			
			if(jDdt.has("destinatario")) {
				JSONObject jDestinatario = jDdt.getJSONObject("destinatario");
				ddt.setDestinatario(loadDestinatarioFromJson(jDestinatario));
			} else {
				throw new DdtException("Il destinatario e' obbligatorio");
			}
			
			JSONObject jDestinazione = null;
			if(jDdt.has("destinazione")) {
				jDestinazione = jDdt.getJSONObject("destinazione");
			}
			ddt.setDestinazione(loadDestinazioneFromJson(jDestinazione));
			
			JSONObject jArticolo = null;
			Vector<Articolo>articoli = null;
			if(jDdt.has("articoli")) {
				JSONArray jArticoli = jDdt.getJSONArray("articoli");
				if(jArticoli != null && jArticoli.length() > 0) {
					articoli = new Vector<Articolo>(jArticoli.length());
					for(int i=0; i<jArticoli.length(); i++) {
						jArticolo = jArticoli.getJSONObject(i);
						articoli.add(loadArticoloFromJson(jArticolo));
					}
				}
				ddt.setArticoli(articoli);
			} else {
				throw new DdtException("La lista degli articoli e' obbligatoria");
			}
		}
		return ddt;
	}

	private Articolo loadArticoloFromJson(JSONObject jArticolo) throws DdtException {
		Articolo articolo = null;
		if(jArticolo != null) {
			articolo = new Articolo();

			if(jArticolo.has("codice")) {
				articolo.setCodice(jArticolo.getString("codice"));
			} else {
				throw new DdtException("Il codice articolo e' obbligatorio");
			}

			if(jArticolo.has("quantita")) {
				articolo.setQuantita(jArticolo.getString("quantita"));
			} else {
				throw new DdtException("Il campo quantita' e' obbligatorio");
			}
			
			String descrizione = jArticolo.has("descrizione") ? jArticolo.getString("descrizione") : "";
			String um = jArticolo.has("um") ? jArticolo.getString("um") : "";
			articolo.setDescrizione(descrizione);
			articolo.setUm(um);
		}
		return articolo;
	}

	private Mittente loadMittenteFromJson(JSONObject jMittente) throws DdtException {
		Mittente mittente = null;
		if(jMittente != null) {
			mittente = new Mittente();
			
			if(jMittente.has("denominazione")) {
				mittente.setDenominazione(jMittente.getString("denominazione"));
			} else {
				throw new DdtException("Il campo demìnominazione del mittente e' obbligatorio");
			}
			
			if(jMittente.has("partitaIva")) {
				mittente.setPartitaIva(jMittente.getString("partitaIva"));
			} else {
				throw new DdtException("Il campo Partita Iva del mittente e' obbligatorio");
			}
			
			if(jMittente.has("indirizzo")) {
				JSONObject jIndirizzo = jMittente.getJSONObject("indirizzo");
				mittente.setIndirizzo(loadIndirizzoFromJson(jIndirizzo));
			} else {
				throw new DdtException("Il campo indirizzo del mittente e' obbligatorio");
			}
		}
		
		return mittente;
	}

	private Destinatario loadDestinatarioFromJson(JSONObject jDestinatario) throws DdtException {
		Destinatario destinatario = null;
		if(jDestinatario != null) {
			destinatario = new Destinatario();
			
			if(jDestinatario.has("denominazione")) {
				destinatario.setDenominazione(jDestinatario.getString("denominazione"));
			} else {
				throw new DdtException("Il campo demìnominazione del destinatario e' obbligatorio");
			}

			if(jDestinatario.has("indirizzo")) {
				JSONObject jIndirizzo = jDestinatario.getJSONObject("indirizzo");
				destinatario.setIndirizzo(loadIndirizzoFromJson(jIndirizzo));
			} else {
				throw new DdtException("Il campo indirizzo del destinatario e' obbligatorio");
			}

			if(!jDestinatario.has("partitaIva") && !jDestinatario.has("codiceFiscale")) {
				throw new DdtException("Partita Iva e Codice Fiscale non possono essere entrambi non valorizzati");
			}
			
			String partitaIva = jDestinatario.has("partitaIva") ? jDestinatario.getString("partitaIva") : "";
			String codiceFiscale = jDestinatario.has("codiceFiscale") ? jDestinatario.getString("codiceFiscale") : "";
			destinatario.setPartitaIva(partitaIva);
			destinatario.setCodiceFiscale(codiceFiscale);
		}
		
		return destinatario;
	}

	private Destinazione loadDestinazioneFromJson(JSONObject jDestinazione) throws DdtException {
		Destinazione destinazione = null;
		if(jDestinazione != null) {
			destinazione = new Destinazione();
			
			if(jDestinazione.has("denominazione")) {
				destinazione.setDenominazione(jDestinazione.getString("denominazione"));
			} else {
				throw new DdtException("Il campo demìnominazione della destinazione e' obbligatorio");
			}

			if(jDestinazione.has("indirizzo")) {
				JSONObject jIndirizzo = jDestinazione.getJSONObject("indirizzo");
				destinazione.setRecapito(loadIndirizzoFromJson(jIndirizzo));
			} else {
				throw new DdtException("Il campo indirizzo della destinazione e' obbligatorio");
			}
		}
		
		return destinazione;
	}

	private Indirizzo loadIndirizzoFromJson(JSONObject jIndirizzo) throws DdtException {
		Indirizzo indirizzo = null;
		if(jIndirizzo != null) {
			indirizzo = new Indirizzo();
			
			if(jIndirizzo.has("indirizzo")) {
				indirizzo.setIndirizzo(jIndirizzo.getString("indirizzo"));
			} else {
				throw new DdtException("Il codice articolo e' obbligatorio");
			}
			
			if(jIndirizzo.has("cap")) {
				indirizzo.setCap(jIndirizzo.getString("cap"));
			} else {
				throw new DdtException("Il cap e' obbligatorio");
			}
			
			if(jIndirizzo.has("citta")) {
				indirizzo.setCitta(jIndirizzo.getString("citta"));
			} else {
				throw new DdtException("La citta' e' obbligatoria");
			}
			
			if(jIndirizzo.has("provincia")) {
				indirizzo.setProvincia(jIndirizzo.getString("provincia"));
			} else {
				throw new DdtException("La provincia e' obbligatoria");
			}
			
			String numeroCivico = jIndirizzo.has("numero_civico") ? jIndirizzo.getString("numero_civico") : "";
			String telefono = jIndirizzo.has("telefono") ? jIndirizzo.getString("telefono") : "";
			String fax = jIndirizzo.has("fax") ? jIndirizzo.getString("fax") : "";
			String email = jIndirizzo.has("email") ? jIndirizzo.getString("email") : "";
			indirizzo.setNumeroCivico(numeroCivico);
			indirizzo.setTelefono(telefono);
			indirizzo.setFax(fax);
			indirizzo.setEmail(email);
		}
		return indirizzo;
	}

	public static void main(String[] args) {
		String dcspPath = "/home/gv/Documents/Projects/DailyCash/ddt";
		String jsonFile = dcspPath + "/ddt.js";
		String propFile = dcspPath + "/ddt.properties";
		
		DdtGenerator ddtGenerator = null;
	
		try {
			Properties properties = new Properties();
			properties.load(new FileInputStream(propFile));
			String json = FileUtils.readFileToString(new File(jsonFile));
			if(json != null && json.length() > 0) {
				ddtGenerator = new DdtGenerator(json, properties, null);
				ddtGenerator.convertToPDF();
			} else {
				throw new IOException("Json is null or empty");
			}
		} catch (IOException e) {			
			e.printStackTrace();
		} catch (DocumentException e) {
			e.printStackTrace();
		} catch (DdtException e) {
			e.printStackTrace();
		}
	}
	
	private PdfPTable creaPagina(int pag) {
		PdfPTable pagina = new PdfPTable(1);
		pagina.setExtendLastRow(true);
		
		if(pag == 0) {
		    PdfPCell sezioneMittenteDestinatario = new PdfPCell();
		    sezioneMittenteDestinatario.setPadding(0);
		    sezioneMittenteDestinatario.setVerticalAlignment(Element.ALIGN_TOP);
		    sezioneMittenteDestinatario.setBorder(Rectangle.NO_BORDER);
		    sezioneMittenteDestinatario.addElement(creaSezioneMittenteDestinatario());
		    pagina.addCell(sezioneMittenteDestinatario);
		    pagina.addCell(creaSeparatore());
		}

	    PdfPCell intestazionePagina = new PdfPCell();
	    intestazionePagina.setPadding(0);
	    intestazionePagina.setVerticalAlignment(Element.ALIGN_TOP);
	    intestazionePagina.setBorder(Rectangle.NO_BORDER);
	    intestazionePagina.addElement(creaIntestazionePagina(pag));
	    pagina.addCell(intestazionePagina);
	    pagina.addCell(creaSeparatore());

	    PdfPCell sezioneArticoli = new PdfPCell();
	    sezioneArticoli.setPadding(0);
	    sezioneArticoli.setVerticalAlignment(Element.ALIGN_TOP);
	    sezioneArticoli.setBorder(Rectangle.NO_BORDER);
	    sezioneArticoli.addElement(creaSezioneArticoli(pag));
	    pagina.addCell(sezioneArticoli);
	    pagina.addCell(creaSeparatore());
	    
	    if(pag == pagg - 1) {
		    PdfPCell sezioneFinale = new PdfPCell();
		    sezioneFinale.setPadding(0);
		    sezioneFinale.setVerticalAlignment(Element.ALIGN_BOTTOM);
		    sezioneFinale.setBorder(Rectangle.NO_BORDER);
		    sezioneFinale.addElement(creaSezioneConclusiva());
		    pagina.addCell(sezioneFinale);

	    }
	    
	    return pagina;
	}
	
	public ByteArrayOutputStream convertToPDF() throws FileNotFoundException, DocumentException {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    Document document = new Document();
	    PdfWriter.getInstance(document, baos);
	    document.open();
	    
	    PdfPTable pagina = null;
	    for(int i=0; i<pagg; i++) {
	    	pagina = creaPagina(i);
	    	document.add(pagina);
	    	if(i != pagg-1) {
	    		document.newPage();
	    	}
	    }
	    document.close();
	    
	    return baos;
	}
	
	private void creaHeader(PdfPTable tabella) {
	    
		Font fontLabel = new Font(FontFamily.HELVETICA, 8, Font.BOLD);

	    String codiceText = properties.getProperty("DDT_LABEL_CODICE", "DC_ID");
	    PdfPCell codice = new PdfPCell();
	    Paragraph codiceParagrafo = new Paragraph(codiceText, fontLabel);
	    codiceParagrafo.setAlignment(Element.ALIGN_CENTER);
	    codice.setMinimumHeight(15);
	    codice.addElement(codiceParagrafo);
	    codice.setUseAscender(true);
	    codice.setVerticalAlignment(Element.ALIGN_MIDDLE);
	    
	    String descrizioneText = properties.getProperty("DDT_LABEL_DESCRIZIONE", "descrizione");
	    PdfPCell descrizione = new PdfPCell();
	    Paragraph descrizioneParagrafo = new Paragraph(descrizioneText, fontLabel);
	    descrizioneParagrafo.setAlignment(Element.ALIGN_CENTER);
	    descrizione.setMinimumHeight(15);
	    descrizione.addElement(descrizioneParagrafo);
	    descrizione.setUseAscender(true);
	    descrizione.setVerticalAlignment(Element.ALIGN_MIDDLE);
	    
	    String umText = properties.getProperty("DDT_LABEL_UM", "um");;
	    PdfPCell um = new PdfPCell();
	    Paragraph umParagrafo = new Paragraph(umText, fontLabel);
	    umParagrafo.setAlignment(Element.ALIGN_CENTER);
	    um.setMinimumHeight(15);
	    um.addElement(umParagrafo);
	    um.setUseAscender(true);
	    um.setVerticalAlignment(Element.ALIGN_MIDDLE);
    
	    String quantitaText = properties.getProperty("DDT_LABEL_QUANTITA", "quantita");
	    PdfPCell quantita = new PdfPCell();
	    Paragraph quantitaParagrafo = new Paragraph(quantitaText, fontLabel);
	    quantitaParagrafo.setAlignment(Element.ALIGN_CENTER);
	    quantita.setMinimumHeight(15);
	    quantita.addElement(quantitaParagrafo);
	    quantita.setUseAscender(true);
	    quantita.setVerticalAlignment(Element.ALIGN_MIDDLE);
	    
	    codice.setBackgroundColor(BaseColor.LIGHT_GRAY);
	    descrizione.setBackgroundColor(BaseColor.LIGHT_GRAY);
	    um.setBackgroundColor(BaseColor.LIGHT_GRAY);
	    quantita.setBackgroundColor(BaseColor.LIGHT_GRAY);
	    
	    tabella.addCell(codice);
	    tabella.addCell(descrizione);
	    tabella.addCell(um);
	    tabella.addCell(quantita);
	}

	private void aggiungiArticolo(PdfPTable tabella, String codiceText, String descrizioneText, String umText, String quantitaText) {
	    Font font = new Font(FontFamily.COURIER, 8, Font.NORMAL);

	    PdfPCell codice = new PdfPCell();
	    Paragraph codiceParagrafo = new Paragraph(codiceText, font);
	    codiceParagrafo.setAlignment(Element.ALIGN_LEFT);
	    codice.addElement(codiceParagrafo);
//	    codice.setMinimumHeight(15);
//	    codice.setNoWrap(true);
//	    codice.setUseAscender(true);
//	    codice.setVerticalAlignment(Element.ALIGN_BOTTOM);
	    
	    PdfPCell descrizione = new PdfPCell();
	    Paragraph descrizioneParagrafo = new Paragraph(descrizioneText, font);
	    descrizioneParagrafo.setAlignment(Element.ALIGN_LEFT);
	    descrizione.addElement(descrizioneParagrafo);
//	    descrizione.setMinimumHeight(15);
//	    descrizione.setNoWrap(true);
//	    descrizione.setUseAscender(true);
//	    descrizione.setVerticalAlignment(Element.ALIGN_BOTTOM);
	    
	    PdfPCell um = new PdfPCell();
	    Paragraph umParagrafo = new Paragraph(umText, font);
	    umParagrafo.setAlignment(Element.ALIGN_LEFT);
	    um.addElement(umParagrafo);
//	    um.setMinimumHeight(15);
//	    um.setNoWrap(true);
//	    um.setUseAscender(true);
//	    um.setVerticalAlignment(Element.ALIGN_BOTTOM);
    
	    PdfPCell quantita = new PdfPCell();
	    Paragraph quantitaParagrafo = new Paragraph(quantitaText, font);
	    quantitaParagrafo.setAlignment(Element.ALIGN_LEFT);
	    quantita.addElement(quantitaParagrafo);
//	    quantita.setMinimumHeight(15);
//	    quantita.setNoWrap(true);
//	    quantita.setUseAscender(true);
//	    quantita.setVerticalAlignment(Element.ALIGN_BOTTOM);
	    
	    tabella.addCell(codice);
	    tabella.addCell(descrizione);
	    tabella.addCell(um);
	    tabella.addCell(quantita);
	}
	
	private PdfPTable creaSezioneArticoli(int pag) {
		PdfPTable tabellaArticoli = null;
		float[] pointColumnWidths = {20F, 55F, 10F, 15F};
		tabellaArticoli = new PdfPTable(pointColumnWidths);
		tabellaArticoli.setWidthPercentage(100);
		tabellaArticoli.getDefaultCell().setMinimumHeight(15);
		tabellaArticoli.getDefaultCell().setUseAscender(true);
		tabellaArticoli.getDefaultCell().setNoWrap(true);
		tabellaArticoli.getDefaultCell().setVerticalAlignment(Element.ALIGN_BOTTOM);

		int beginIndex = 0;
		int endIndex = 0;
		
		if(pag == 0) {
			beginIndex = 0;
			endIndex = min(totaleArticoli, maxPrimaPagina);
		} else {
			int numArticoliRestanti = (totaleArticoli - maxPrimaPagina - maxPagina*(pag - 1));
			beginIndex=maxPrimaPagina + maxPagina*(pag - 1);
			endIndex = beginIndex + min(numArticoliRestanti, maxPagina);
		}

		if(endIndex != beginIndex) {
			creaHeader(tabellaArticoli);
		}
		
		Articolo articolo = null;
		for(int i=beginIndex; i<endIndex; i++) {
			articolo = this.documentoDiTrasporto.getArticoli().elementAt(i);
//			aggiungiArticolo(tabellaArticoli, "cod1", "maglia rossa modello giuditta", "N", "20");
			aggiungiArticolo(tabellaArticoli, articolo.getCodice(), articolo.getDescrizione(), articolo.getUm(), articolo.getQuantita());
		}
		
		return tabellaArticoli;
	}
		
	private PdfPTable creaIntestazionePagina(int pag) {
		
		PdfPTable table = null;
		float[] pointColumnWidths = {21F, 21F, 21F, 12F, 25F};
		table = new PdfPTable(pointColumnWidths);
	    table.setWidthPercentage(100);
	    
	    PdfPCell tipoDocumento = new PdfPCell();
	    tipoDocumento.setPadding(0);
	    tipoDocumento.setNoWrap(true);
	    tipoDocumento.setVerticalAlignment(Element.ALIGN_TOP);
	    tipoDocumento.addElement(creaCampo(properties.getProperty("DDT_LABEL_TIPODOC", "Tipo Documento"), this.documentoDiTrasporto.getTipoDocumento()));

	    PdfPCell dataDocumento = new PdfPCell();
	    dataDocumento.setPadding(0);
	    tipoDocumento.setNoWrap(true);
	    dataDocumento.setVerticalAlignment(Element.ALIGN_TOP);
	    dataDocumento.addElement(creaCampo(properties.getProperty("DDT_LABEL_DATADOC", "Data Documento"), this.documentoDiTrasporto.getDataDocumento()));

	    PdfPCell numeroDocumento = new PdfPCell();
	    numeroDocumento.setPadding(0);
	    tipoDocumento.setNoWrap(true);
	    numeroDocumento.setVerticalAlignment(Element.ALIGN_TOP);
	    numeroDocumento.addElement(creaCampo(properties.getProperty("DDT_LABEL_NUMDOC", "N.ro Documento"), this.documentoDiTrasporto.getNumeroDocumento()));

	    PdfPCell pagina = new PdfPCell();
	    pagina.setPadding(0);
	    tipoDocumento.setNoWrap(true);
	    pagina.setVerticalAlignment(Element.ALIGN_TOP);
	    pagina.addElement(creaCampo(properties.getProperty("DDT_LABEL_PAGINA", "Pagina"), (pag + 1) + " / " + pagg));

	    PdfPCell causaleTrasporto = new PdfPCell();
	    causaleTrasporto.setPadding(0);
	    tipoDocumento.setNoWrap(true);
	    causaleTrasporto.setVerticalAlignment(Element.ALIGN_TOP);
	    causaleTrasporto.addElement(creaCampo(properties.getProperty("DDT_LABEL_CAUSALE", "Causale"), this.documentoDiTrasporto.getCausaleTrasporto()));
	    
	    table.addCell(tipoDocumento);
	    table.addCell(dataDocumento);
	    table.addCell(numeroDocumento);
	    table.addCell(pagina);
	    table.addCell(causaleTrasporto);

	    return table;
	}
	
	private PdfPTable creaSezioneFirme() {
		
		PdfPTable table = null;
		float[] pointColumnWidths = {1F, 1F, 1F};
		table = new PdfPTable(pointColumnWidths);
	    table.setWidthPercentage(100);
	    
	    PdfPCell firmamittente = new PdfPCell();
	    firmamittente.setPadding(0);
	    firmamittente.setVerticalAlignment(Element.ALIGN_TOP);
	    firmamittente.addElement(creaCampo(properties.getProperty("DDT_LABEL_FIRMAMITTENTE", "Firma Mittente"), " "));

	    PdfPCell firmadestinatario = new PdfPCell();
	    firmadestinatario.setPadding(0);
	    firmadestinatario.setVerticalAlignment(Element.ALIGN_TOP);
	    firmadestinatario.addElement(creaCampo(properties.getProperty("DDT_LABEL_FIRMADESTINATARIO", "Firma Destinatario"), " "));

	    PdfPCell firmavettore = new PdfPCell();
	    firmavettore.setPadding(0);
	    firmavettore.setVerticalAlignment(Element.ALIGN_TOP);
	    firmavettore.addElement(creaCampo(properties.getProperty("DDT_LABEL_FIRMAVETTORE", "Firma Vettore"), " "));

	    
	    table.addCell(firmamittente);
	    table.addCell(firmavettore);
	    table.addCell(firmadestinatario);

	    return table;
	}
	
	private PdfPTable creaSezioneVettore() {

		float[] pointColumnWidths = {1F};
		String vettoreLabelText = properties.getProperty("DDT_LABEL_VETTORE", "Vettore");
		
		Font fontLabel = new Font(FontFamily.HELVETICA, 10, Font.BOLD);
	    Paragraph paragrafoLabel = new Paragraph(vettoreLabelText, fontLabel);
	    paragrafoLabel.setLeading(0, 1);

		PdfPTable tabella = new PdfPTable(pointColumnWidths);
	    tabella.setWidthPercentage(100);
	    
	    PdfPCell vettoreLabel = new PdfPCell();
	    vettoreLabel.setPadding(TAB_PADDING);
	    vettoreLabel.setFixedHeight(20);
	    vettoreLabel.setVerticalAlignment(Element.ALIGN_TOP);
	    vettoreLabel.addElement(paragrafoLabel);
	    vettoreLabel.setBorder(Rectangle.NO_BORDER);
	    
	    PdfPCell vettoreInfo = new PdfPCell();
	    vettoreInfo.setPadding(TAB_PADDING);
	    vettoreInfo.setMinimumHeight(10);
	    vettoreInfo.setVerticalAlignment(Element.ALIGN_TOP);
	    vettoreInfo.setBorder(Rectangle.NO_BORDER);

	    tabella.addCell(vettoreLabel);
	    tabella.addCell(vettoreInfo);

		PdfPTable tabellaVettore = null;
		tabellaVettore = new PdfPTable(pointColumnWidths);
		tabellaVettore.setWidthPercentage(100);
		tabellaVettore.setExtendLastRow(true);

	    PdfPCell headerVettore = new PdfPCell();
	    headerVettore.setPadding(0);
	    headerVettore.addElement(tabella);
	    headerVettore.setBorder(Rectangle.BOX);
	    tabellaVettore.addCell(headerVettore);

		return tabellaVettore;
	}
	
	private PdfPTable creaSezioneAnnotazioni() {
		
		PdfPTable table = null;
		float[] pointColumnWidths = {2F, 1F};
		table = new PdfPTable(pointColumnWidths);
	    table.setWidthPercentage(100);
	    
	    PdfPCell annotazioni = new PdfPCell();
	    annotazioni.setPadding(0);
	    annotazioni.setVerticalAlignment(Element.ALIGN_TOP);
	    annotazioni.addElement(creaCampo(properties.getProperty("DDT_LABEL_ANNOTAZIONI", "Annotazioni"), " "));

	    PdfPCell colli = new PdfPCell();
	    colli.setPadding(0);
	    colli.setVerticalAlignment(Element.ALIGN_TOP);
	    colli.addElement(creaCampo(properties.getProperty("DDT_LABEL_COLLI", "Colli"), " "));

	    table.addCell(annotazioni);
	    table.addCell(colli);

		return table;
	}
	
	private PdfPTable creaSezioneConclusiva() {
		PdfPTable footer = null;
		float[] pointColumnWidths = {1F};
		footer = new PdfPTable(pointColumnWidths);
		footer.setWidthPercentage(100);
		
    	PdfPCell sezioneVettore = new PdfPCell();
	    sezioneVettore.setPadding(0);
	    sezioneVettore.setVerticalAlignment(Element.ALIGN_TOP);
	    sezioneVettore.setBorder(Rectangle.NO_BORDER);
	    sezioneVettore.addElement(creaSezioneVettore());
	    footer.addCell(sezioneVettore);
	    footer.addCell(creaSeparatore());
	    
	    PdfPCell sezioneAnnotazioni = new PdfPCell();
	    sezioneAnnotazioni.setPadding(0);
	    sezioneAnnotazioni.setVerticalAlignment(Element.ALIGN_TOP);
	    sezioneAnnotazioni.setBorder(Rectangle.NO_BORDER);
	    sezioneAnnotazioni.addElement(creaSezioneAnnotazioni());
	    footer.addCell(sezioneAnnotazioni);
	    footer.addCell(creaSeparatore());
	    
	    PdfPCell sezioneFirme = new PdfPCell();
	    sezioneFirme.setPadding(0);
	    sezioneFirme.setVerticalAlignment(Element.ALIGN_TOP);
	    sezioneFirme.setBorder(Rectangle.NO_BORDER);
	    sezioneFirme.addElement(creaSezioneFirme());
	    footer.addCell(sezioneFirme);

		return footer;
	}

	private Image creaImage() {
		Image img = null;
		try {
			logger.debug("Loading logo image from "+logoPath);
			img = Image.getInstance(logoPath);
			img.scalePercent(10f);
			
		} catch (Exception e) {
			logger.error("Failed to load logo image from"+logoPath, e);
		}

		return img;
	}

	private PdfPTable creaSezioneMittenteDestinatario() {
		PdfPTable table = null;
		float[] pointColumnWidths = {99F, 2F, 99F};
		table = new PdfPTable(pointColumnWidths);
	    table.setWidthPercentage(100);
	    table.getDefaultCell().setBorder(Rectangle.NO_BORDER);
	    table.getDefaultCell().setPadding(TAB_PADDING);
	    table.getDefaultCell().setVerticalAlignment(Element.ALIGN_TOP);
	    
	    
	    if (Objects.nonNull(logoPath)) {
		    PdfPCell logo = new PdfPCell();
		    logo.setBorder(Rectangle.NO_BORDER);
		    logo.setVerticalAlignment(Element.ALIGN_MIDDLE);
		    logo.addElement(creaImage());
		    table.addCell(logo);
	    }
	    PdfPCell mittente = new PdfPCell();
	    mittente.addElement(creaMittente());
	    
	    PdfPCell destinatario = new PdfPCell();
	    destinatario.addElement(creaDestinatario());
	    
	    PdfPCell destinazione = new PdfPCell();
	    destinazione.addElement(creaDestinazione());
	    
	   
	    table.addCell(creaSeparatore());
	    table.addCell(destinazione);
	    table.addCell(creaSeparatore());
	    table.addCell(creaSeparatore());
	    table.addCell(creaSeparatore());
	    table.addCell(mittente);
	    table.addCell(creaSeparatore());
	    table.addCell(destinatario);

		return table;
	}
	
	protected PdfPTable creaSezioneMittenteDestinatario_1() {
		PdfPTable table = null;
		float[] pointColumnWidths = {1F, 1F};
		table = new PdfPTable(pointColumnWidths);
	    table.setWidthPercentage(100);
	    
	    PdfPCell mittente = new PdfPCell();
	    mittente.setPaddingRight(TAB_PADDING);
	    mittente.setPaddingLeft(0);
//	    mittente.setMinimumHeight(50);
	    mittente.setVerticalAlignment(Element.ALIGN_TOP);
	    mittente.addElement(creaTabellaMittente());
	    mittente.setBorder(Rectangle.NO_BORDER);
	    table.addCell(mittente);
	    
	    PdfPCell destinatario = new PdfPCell();
	    destinatario.setPaddingLeft(TAB_PADDING);
	    destinatario.setPaddingRight(0);
//	    destinatario.setMinimumHeight(50);
	    destinatario.setVerticalAlignment(Element.ALIGN_TOP);
	    destinatario.addElement(creaTabellaDestinatario());
	    destinatario.setBorder(Rectangle.NO_BORDER);
	    table.addCell(destinatario);
	    
		return table;
	}
	
	protected PdfPTable creaMit() {

		float[] pointColumnWidths = {1F};
		String mittenteLabelText = properties.getProperty("DDT_LABEL_MITTENTE", "Mittente");
		String mittenteDenominazioneText = documentoDiTrasporto.getMittente().getDenominazione() + "\n";
		String mittenteIndirizzoText = documentoDiTrasporto.getMittente().getIndirizzoAsString();
		String mittentePIText = documentoDiTrasporto.getMittente().getPartitaIvaAsString();
				
		Font fontLabel = new Font(FontFamily.HELVETICA, 10, Font.BOLD);
	    Paragraph paragrafoLabel = new Paragraph(mittenteLabelText, fontLabel);
	    paragrafoLabel.setLeading(0, 1);

	    Font fontIndirizzo = new Font(FontFamily.HELVETICA, 8, Font.NORMAL);
	    Paragraph paragrafoIndirizzo = new Paragraph(mittenteIndirizzoText, fontIndirizzo);
	    paragrafoIndirizzo.setLeading(0, 1);

	    Font fontPartitaIva = new Font(FontFamily.HELVETICA, 8, Font.NORMAL);
	    Paragraph paragrafoPartitaIva = new Paragraph(mittentePIText, fontPartitaIva);
	    paragrafoIndirizzo.setLeading(0, 1);

	    Font fontDenominazione = new Font(FontFamily.HELVETICA, 8, Font.BOLD);
	    Paragraph paragrafoDenominazione = new Paragraph(mittenteDenominazioneText, fontDenominazione);
	    paragrafoDenominazione.setLeading(0, 1);

		PdfPTable tabella = new PdfPTable(pointColumnWidths);
	    tabella.setWidthPercentage(100);
	    
	    PdfPCell mittenteLabel = new PdfPCell();
	    mittenteLabel.setPadding(TAB_PADDING);
	    mittenteLabel.setFixedHeight(20);
	    mittenteLabel.setVerticalAlignment(Element.ALIGN_TOP);
	    mittenteLabel.addElement(paragrafoLabel);
	    mittenteLabel.setBorder(Rectangle.NO_BORDER);
	    
	    PdfPCell mittenteInfo = new PdfPCell();
	    mittenteInfo.setPadding(TAB_PADDING);
	    mittenteInfo.setMinimumHeight(50);
	    mittenteInfo.setVerticalAlignment(Element.ALIGN_TOP);
	    mittenteInfo.addElement(paragrafoDenominazione);
	    mittenteInfo.addElement(paragrafoIndirizzo);
	    mittenteInfo.addElement(paragrafoPartitaIva);
	    mittenteInfo.setBorder(Rectangle.NO_BORDER);

	    tabella.addCell(mittenteLabel);
	    tabella.addCell(mittenteInfo);

		PdfPTable tabellaMittente = null;
		tabellaMittente = new PdfPTable(pointColumnWidths);
		tabellaMittente.setWidthPercentage(100);
		tabellaMittente.setExtendLastRow(true);

	    PdfPCell headerMittente = new PdfPCell();
	    headerMittente.setPadding(0);
	    headerMittente.addElement(tabella);
	    headerMittente.setBorder(Rectangle.BOX);
	    tabellaMittente.addCell(headerMittente);

		return tabellaMittente;
	}
	
	private PdfPTable creaDestinatario() {
		
		float[] pointColumnWidths = {1F};

		String destinatarioLabelText = properties.getProperty("DDT_LABEL_DESTINATARIO", "Destinatario");;
		String destinatarioDenominazioneText = documentoDiTrasporto.getDestinatario().getDenominazione();
		String destinatarioIndirizzoText = documentoDiTrasporto.getDestinatario().getIndirizzoAsString();
		String destinatarioPICFText = (documentoDiTrasporto.getDestinatario().getPartitaIva() != null &&
										documentoDiTrasporto.getDestinatario().getPartitaIva().length()>0)
				? documentoDiTrasporto.getDestinatario().getPartitaIvaAsString()
				: documentoDiTrasporto.getDestinatario().getCodiceFiscaleAsString();
		
		Font fontLabel = new Font(FontFamily.HELVETICA, 10, Font.BOLD);
	    Paragraph paragrafoLabel = new Paragraph(destinatarioLabelText, fontLabel);
	    paragrafoLabel.setLeading(0, 1);

	    Font fontDenominazione = new Font(FontFamily.HELVETICA, 8, Font.BOLD);
	    Paragraph paragrafoDenominazione = new Paragraph(destinatarioDenominazioneText, fontDenominazione);
	    paragrafoDenominazione.setLeading(0, 1);

	    Font fontIndirizzo = new Font(FontFamily.HELVETICA, 8, Font.NORMAL);
	    Paragraph paragrafoIndirizzo = new Paragraph(destinatarioIndirizzoText, fontIndirizzo);
	    paragrafoIndirizzo.setLeading(0, 1);

	    Font fontPartitaIva = new Font(FontFamily.HELVETICA, 8, Font.NORMAL);
	    Paragraph paragrafoPartitaIva = new Paragraph(destinatarioPICFText, fontPartitaIva);
	    paragrafoIndirizzo.setLeading(0, 1);

	    PdfPTable destinatario = null;
		destinatario = new PdfPTable(pointColumnWidths);
	    destinatario.setWidthPercentage(100);
	    
	    PdfPCell destinatarioLabel = new PdfPCell();
	    destinatarioLabel.setPadding(TAB_PADDING);
	    destinatarioLabel.setFixedHeight(20);
	    destinatarioLabel.setVerticalAlignment(Element.ALIGN_TOP);
	    destinatarioLabel.addElement(paragrafoLabel);
	    destinatarioLabel.setBorder(Rectangle.NO_BORDER);
	    
	    PdfPCell destinatarioInfo = new PdfPCell();
	    destinatarioInfo.setPadding(TAB_PADDING);
	    destinatarioInfo.setMinimumHeight(50);
	    destinatarioInfo.setVerticalAlignment(Element.ALIGN_TOP);
	    destinatarioInfo.addElement(paragrafoDenominazione);
	    destinatarioInfo.addElement(paragrafoIndirizzo);
	    destinatarioInfo.addElement(paragrafoPartitaIva);
	    destinatarioInfo.setBorder(Rectangle.NO_BORDER);

	    destinatario.addCell(destinatarioLabel);
	    destinatario.addCell(destinatarioInfo);
		return destinatario;
	}	
	
	private PdfPTable creaMittente() {
		
		float[] pointColumnWidths = {1F};

		String mittenteLabelText = properties.getProperty("DDT_LABEL_MITTENTE", "Mittente");;
		String mittenteDenominazioneText = documentoDiTrasporto.getMittente().getDenominazione();
		String mittenteIndirizzoText = documentoDiTrasporto.getMittente().getIndirizzoAsString();
		String mittentePICFText = (documentoDiTrasporto.getMittente().getPartitaIva() != null &&
										documentoDiTrasporto.getMittente().getPartitaIva().length()>0)
				? documentoDiTrasporto.getMittente().getPartitaIvaAsString()
				: documentoDiTrasporto.getMittente().getCodiceFiscaleAsString();
		
		Font fontLabel = new Font(FontFamily.HELVETICA, 10, Font.BOLD);
	    Paragraph paragrafoLabel = new Paragraph(mittenteLabelText, fontLabel);
	    paragrafoLabel.setLeading(0, 1);

	    Font fontDenominazione = new Font(FontFamily.HELVETICA, 8, Font.BOLD);
	    Paragraph paragrafoDenominazione = new Paragraph(mittenteDenominazioneText, fontDenominazione);
	    paragrafoDenominazione.setLeading(0, 1);

	    Font fontIndirizzo = new Font(FontFamily.HELVETICA, 8, Font.NORMAL);
	    Paragraph paragrafoIndirizzo = new Paragraph(mittenteIndirizzoText, fontIndirizzo);
	    paragrafoIndirizzo.setLeading(0, 1);

	    Font fontPartitaIva = new Font(FontFamily.HELVETICA, 8, Font.NORMAL);
	    Paragraph paragrafoPartitaIva = new Paragraph(mittentePICFText, fontPartitaIva);
	    paragrafoIndirizzo.setLeading(0, 1);

	    PdfPTable mittente = null;
		mittente = new PdfPTable(pointColumnWidths);
	    mittente.setWidthPercentage(100);
	    
	    PdfPCell mittenteLabel = new PdfPCell();
	    mittenteLabel.setPadding(TAB_PADDING);
	    mittenteLabel.setFixedHeight(20);
	    mittenteLabel.setVerticalAlignment(Element.ALIGN_TOP);
	    mittenteLabel.addElement(paragrafoLabel);
	    mittenteLabel.setBorder(Rectangle.NO_BORDER);
	    
	    PdfPCell mittenteInfo = new PdfPCell();
	    mittenteInfo.setPadding(TAB_PADDING);
	    mittenteInfo.setMinimumHeight(50);
	    mittenteInfo.setVerticalAlignment(Element.ALIGN_TOP);
	    mittenteInfo.addElement(paragrafoDenominazione);
	    mittenteInfo.addElement(paragrafoIndirizzo);
	    mittenteInfo.addElement(paragrafoPartitaIva);
	    mittenteInfo.setBorder(Rectangle.NO_BORDER);

	    mittente.addCell(mittenteLabel);
	    mittente.addCell(mittenteInfo);
		return mittente;
	}	
	
	private PdfPTable creaDestinazione() {
		
		float[] pointColumnWidths = {1F};

		String destinazioneLabelText = properties.getProperty("DDT_LABEL_DESTINAZIONE", "Luogo di destinazione");
		String destinazioneDenominazioneText = documentoDiTrasporto.getDestinazione().getDenominazione();
		String destinazioneIndirizzoText = documentoDiTrasporto.getDestinazione().getRecapitoAsString();
		
		Font fontLabel = new Font(FontFamily.HELVETICA, 10, Font.BOLD);
	    Paragraph paragrafoLabel = new Paragraph(destinazioneLabelText, fontLabel);
	    paragrafoLabel.setLeading(0, 1);

	    Font fontDenominazione = new Font(FontFamily.HELVETICA, 8, Font.BOLD);
	    Paragraph paragrafoDenominazione = new Paragraph(destinazioneDenominazioneText, fontDenominazione);
	    paragrafoDenominazione.setLeading(0, 1);

	    Font fontIndirizzo = new Font(FontFamily.HELVETICA, 8, Font.NORMAL);
	    Paragraph paragrafoIndirizzo = new Paragraph(destinazioneIndirizzoText, fontIndirizzo);
	    paragrafoIndirizzo.setLeading(0, 1);

	    PdfPTable destinazione = null;
		destinazione = new PdfPTable(pointColumnWidths);
	    destinazione.setWidthPercentage(100);
	    
	    PdfPCell destinazioneLabel = new PdfPCell();
	    destinazioneLabel.setPadding(TAB_PADDING);
	    destinazioneLabel.setFixedHeight(20);
	    destinazioneLabel.setVerticalAlignment(Element.ALIGN_TOP);
	    destinazioneLabel.addElement(paragrafoLabel);
	    destinazioneLabel.setBorder(Rectangle.NO_BORDER);
	    
	    PdfPCell destinazioneInfo = new PdfPCell();
	    destinazioneInfo.setPadding(TAB_PADDING);
	    destinazioneInfo.setMinimumHeight(50);
	    destinazioneInfo.setVerticalAlignment(Element.ALIGN_TOP);
	    destinazioneInfo.addElement(paragrafoDenominazione);
	    destinazioneInfo.addElement(paragrafoIndirizzo);
	    destinazioneInfo.setBorder(Rectangle.NO_BORDER);

	    destinazione.addCell(destinazioneLabel);
	    destinazione.addCell(destinazioneInfo);
		return destinazione;
	}	

	private PdfPTable creaTabellaDestinatario() {
		PdfPTable destinatario = null;
		float[] pointColumnWidths = {1F};
		destinatario = new PdfPTable(pointColumnWidths);
	    destinatario.setWidthPercentage(100);
	    
	    PdfPCell headerDestinatario = new PdfPCell();
	    headerDestinatario.setPaddingBottom(0);
	    headerDestinatario.addElement(creaDestinatario());
	    headerDestinatario.setBorder(Rectangle.BOX);
	    destinatario.addCell(headerDestinatario);

	    destinatario.addCell(creaSeparatore());

	    PdfPCell headerLuogoDestinatario = new PdfPCell();
	    headerLuogoDestinatario.setPaddingTop(0);
	    headerLuogoDestinatario.addElement(creaDestinazione());
	    headerLuogoDestinatario.setBorder(Rectangle.BOX);
	    destinatario.addCell(headerLuogoDestinatario);

		return destinatario;
	}
	
	private PdfPTable creaTabellaMittente() {
		PdfPTable mittente = null;
		float[] pointColumnWidths = {1F};
		mittente = new PdfPTable(pointColumnWidths);
		mittente.setWidthPercentage(100);
		
		URL url = FrameworkUtil.getBundle(this.getClass()).getResource("DC_logo_righsided.png");
		System.out.println("url: " + url);
		Image img = null;
		try {
			img = Image.getInstance(url);
			
		} catch (Exception e) {
			e.printStackTrace();
		}

	    PdfPCell headerLogo = new PdfPCell();
	    headerLogo.setPaddingTop(0);
	    headerLogo.addElement(img);
	    headerLogo.setBorder(Rectangle.NO_BORDER);
	    mittente.addCell(headerLogo);

	    mittente.addCell(creaSeparatore());

	    PdfPCell headerMittente = new PdfPCell();
	    headerMittente.setPaddingBottom(0);
	    headerMittente.addElement(creaMittente());
	    headerMittente.setBorder(Rectangle.BOX);
	    mittente.addCell(headerMittente);

		return mittente;
	}
	
	private PdfPTable creaCampo(String nome, String valore) {
		PdfPTable campo = null;
		float[] pointColumnWidths = {1F};
		campo = new PdfPTable(pointColumnWidths);
	    campo.setWidthPercentage(100);
	    
	    PdfPCell nomeCampo = new PdfPCell();
	    nomeCampo.setPadding(TAB_PADDING);
	    nomeCampo.setFixedHeight(20);
	    nomeCampo.setVerticalAlignment(Element.ALIGN_TOP);
	    nomeCampo.addElement(creaNomeCampo(nome));
	    nomeCampo.setBorder(Rectangle.NO_BORDER);
	    
	    PdfPCell valoreCampo = new PdfPCell();
	    valoreCampo.setPadding(TAB_PADDING);
	    valoreCampo.setVerticalAlignment(Element.ALIGN_TOP);
	    valoreCampo.addElement(creaValoreCampo(valore));
	    valoreCampo.setBorder(Rectangle.NO_BORDER);

	    campo.addCell(nomeCampo);
	    campo.addCell(valoreCampo);
		return campo;
	}	
	
	private Paragraph creaNomeCampo(String name) {
	    Font font = new Font(FontFamily.HELVETICA, 8, Font.BOLD);
	    Paragraph paragrafo = new Paragraph(name, font);
	    paragrafo.setLeading(0, 1);

	    return paragrafo;
	}
	
	private Paragraph creaValoreCampo(String text) {
	    Font font = new Font(FontFamily.COURIER, 8, Font.NORMAL);
	    Paragraph paragrafo = new Paragraph(text, font);
	    paragrafo.setLeading(0, 1);

	    return paragrafo;
	}
	
	private PdfPCell creaSeparatore() {
	    PdfPCell separatore= new PdfPCell();
	    separatore.setFixedHeight(5);
	    separatore.setBorder(Rectangle.NO_BORDER);
	    
	    return separatore;
	}
	
	private int min(int a, int b) {
		return (a<b) ? a : b;
	}
}

