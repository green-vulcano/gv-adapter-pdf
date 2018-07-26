package com.greenvulcano.dcsp.ddt;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Vector;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.commons.io.FileUtils;

import com.greenvulcano.dcsp.ddt.beans.Articolo;
import com.greenvulcano.dcsp.ddt.beans.DDT;
import com.greenvulcano.dcsp.ddt.beans.Destinatario;
import com.greenvulcano.dcsp.ddt.beans.Indirizzo;
import com.greenvulcano.dcsp.ddt.beans.LuogoDestinazione;
import com.greenvulcano.dcsp.ddt.beans.Mittente;
import com.greenvulcano.dcsp.ddt.beans.Recapito;
import com.greenvulcano.dcsp.ddt.exceptions.DdtException;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Font.FontFamily;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

public class DdtGenerator {
	
	public static final float TAB_PADDING = 2.5F;
	
	private DDT documentoDiTrasporto;
	private Integer pagg;
	private Integer totaleArticoli;
	private Integer minPrimaPagina; 
	private Integer maxPrimaPagina; 
	private Integer maxPagina; 

	private Properties properties;
	
	public DDT getDdt() {
		return documentoDiTrasporto;
	}

	public void setDdt(DDT ddt) {
		this.documentoDiTrasporto = ddt;
	}

	public DdtGenerator () {
		
	}
	
	public DdtGenerator(String json) throws DdtException {
		parseDdt(json);
		
		InputStream propFile = this.getClass().getClassLoader().getResourceAsStream("ddt.properties");
		try {
			this.properties = new Properties();
			this.properties.load(propFile);
		} catch (IOException e) {
			throw new DdtException("cannot read properties file");
		}
		
		totaleArticoli = 0;
		if(documentoDiTrasporto != null) {
			totaleArticoli = (documentoDiTrasporto.getArticoli() == null) ? 0 : documentoDiTrasporto.getArticoli().size();
		}
		
		minPrimaPagina = Integer.parseInt(properties.getProperty("pagesize.min.primapagina"));
		maxPrimaPagina = Integer.parseInt(properties.getProperty("pagesize.max.primapagina"));
		maxPagina = Integer.parseInt(properties.getProperty("pagesize.max.pagina"));

		if(totaleArticoli <= minPrimaPagina) {
			pagg = 1;
		} else if(totaleArticoli <= maxPrimaPagina) {
			pagg = 2;
		} else {
			pagg = 1 + ((totaleArticoli-maxPrimaPagina)/maxPagina) + 1; // 25 articoli per pagina + ultima pagina
		}
	}

	private void parseDdt(String json) throws DdtException {
		JsonReader reader = Json.createReader(new StringReader(json));
		JsonObject jsonObject = reader.readObject();
		if(jsonObject != null) {
			this.documentoDiTrasporto = loadDdtFromJson(jsonObject);
		}
	}
	
	private DDT loadDdtFromJson(JsonObject jDdt) throws DdtException {
		DDT ddt = null;
		DateFormat df = null;
		if(jDdt != null) {
			df = new SimpleDateFormat("d/M/yyyy");
			ddt = new DDT();
			ddt.setTipoDocumento(jDdt.getString("tipoDocumento", "DDT"));
			ddt.setDataDocumento(jDdt.getString("dataDocumento", df.format(new Date())));
			if(jDdt.containsKey("numeroDocumento")) {
				ddt.setNumeroDocumento(jDdt.getString("numeroDocumento"));
			} else {
				throw new DdtException("Il numero di documento e' obbligatorio");
			}
			ddt.setCausaleTrasporto(jDdt.getString("causaleTrasporto", "conto vendita"));
			ddt.setVettore(jDdt.getString("vettore", ""));
			ddt.setAnnotazioni(jDdt.getString("annotazioni", ""));

			if(jDdt.containsKey("mittente")) {
				JsonObject jMittente = jDdt.getJsonObject("mittente");
				ddt.setMittente(loadMittenteFromJson(jMittente));
			} else {
				throw new DdtException("Il mittente e' obbligatorio");
			}
			
			if(jDdt.containsKey("destinatario")) {
				JsonObject jDestinatario = jDdt.getJsonObject("destinatario");
				ddt.setDestinatario(loadDestinatarioFromJson(jDestinatario));
			} else {
				throw new DdtException("Il destinatario e' obbligatorio");
			}
			
			JsonObject jLuogoDestinazione = null;
			if(jDdt.containsKey("luogoDestinazione")) {
				jLuogoDestinazione = jDdt.getJsonObject("luogoDestinazione");
			}
			ddt.setLuogoDestinazione(loadLuogoDestinazioneFromJson(jLuogoDestinazione));
			
			JsonObject jArticolo = null;
			Vector<Articolo>articoli = null;
			if(jDdt.containsKey("articoli")) {
				JsonArray jArticoli = jDdt.getJsonArray("articoli");
				if(jArticoli != null && jArticoli.size() > 0) {
					articoli = new Vector<Articolo>(jArticoli.size());
					for(int i=0; i<jArticoli.size(); i++) {
						jArticolo = jArticoli.getJsonObject(i);
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

	private Articolo loadArticoloFromJson(JsonObject jArticolo) throws DdtException {
		Articolo articolo = null;
		if(jArticolo != null) {
			articolo = new Articolo();
			
			if(jArticolo.containsKey("codice")) {
				articolo.setCodice(jArticolo.getString("codice"));
			} else {
				throw new DdtException("Il codice articolo e' obbligatorio");
			}

			articolo.setDescrizione(jArticolo.getString("descrizione", ""));
			articolo.setUm(jArticolo.getString("um", "N"));
			if(jArticolo.containsKey("quantita")) {
				articolo.setQuantita(jArticolo.getString("quantita"));
			} else {
				throw new DdtException("Il campo quantita' e' obbligatorio");
			}
		}
		return articolo;
	}

	private LuogoDestinazione loadLuogoDestinazioneFromJson(JsonObject jLuogoDestinazione) throws DdtException {
		LuogoDestinazione luogoDestinazione = null;
		if(jLuogoDestinazione != null) {
			luogoDestinazione = new LuogoDestinazione();
			luogoDestinazione.setDenominazione(jLuogoDestinazione.getString("denominazione", ""));
			
			if(jLuogoDestinazione.containsKey("recapito")) {
				JsonObject jRecapito = jLuogoDestinazione.getJsonObject("recapito");
				luogoDestinazione.setRecapito(loadRecapitoFromJson(jRecapito));
			} else {
				throw new DdtException("Il campo recapito del luogo di destinazione e' obbligatorio");
			}
		}
		
		return luogoDestinazione;
	}

	private Mittente loadMittenteFromJson(JsonObject jMittente) throws DdtException {
		Mittente mittente = null;
		if(jMittente != null) {
			mittente = new Mittente();
			
			if(jMittente.containsKey("denominazione")) {
				mittente.setDenominazione(jMittente.getString("denominazione"));
			} else {
				throw new DdtException("Il campo demìnominazione del mittente e' obbligatorio");
			}
			
			if(jMittente.containsKey("partitaIva")) {
				mittente.setPartitaIva(jMittente.getString("partitaIva"));
			} else {
				throw new DdtException("Il campo Partita Iva del mittente e' obbligatorio");
			}
			
			if(jMittente.containsKey("indirizzo")) {
				JsonObject jIndirizzo = jMittente.getJsonObject("indirizzo");
				mittente.setIndirizzo(loadIndirizzoFromJson(jIndirizzo));
			} else {
				throw new DdtException("Il campo indirizzo del mittente e' obbligatorio");
			}
		}
		
		return mittente;
	}

	private Destinatario loadDestinatarioFromJson(JsonObject jDestinatario) throws DdtException {
		Destinatario destinatario = null;
		if(jDestinatario != null) {
			destinatario = new Destinatario();
			
			if(jDestinatario.containsKey("denominazione")) {
				destinatario.setDenominazione(jDestinatario.getString("denominazione"));
			} else {
				throw new DdtException("Il campo demìnominazione del destinatario e' obbligatorio");
			}

			if(!jDestinatario.containsKey("partitaIva") && !jDestinatario.containsKey("codiceFiscale")) {
				throw new DdtException("Partita Iva e Codice Fiscale non possono essere entrambi non valorizzati");
			}
			destinatario.setPartitaIva(jDestinatario.getString("partitaIva",""));
			destinatario.setCodiceFiscale(jDestinatario.getString("codiceFiscale",""));
			
			if(jDestinatario.containsKey("indirizzo")) {
				JsonObject jIndirizzo = jDestinatario.getJsonObject("indirizzo");
				destinatario.setIndirizzo(loadIndirizzoFromJson(jIndirizzo));
			} else {
				throw new DdtException("Il campo indirizzo del destinatario e' obbligatorio");
			}
		}
		
		return destinatario;
	}

	private Indirizzo loadIndirizzoFromJson(JsonObject jIndirizzo) throws DdtException {
		Indirizzo indirizzo = null;
		if(jIndirizzo != null) {
			indirizzo = new Indirizzo();
			
			if(jIndirizzo.containsKey("indirizzo")) {
				indirizzo.setIndirizzo(jIndirizzo.getString("indirizzo"));
			} else {
				throw new DdtException("Il codice articolo e' obbligatorio");
			}
			
			indirizzo.setNumeroCivico(jIndirizzo.getString("numeroCivico", ""));

			if(jIndirizzo.containsKey("cap")) {
				indirizzo.setCap(jIndirizzo.getString("cap"));
			} else {
				throw new DdtException("Il cap e' obbligatorio");
			}
			
			if(jIndirizzo.containsKey("citta")) {
				indirizzo.setCitta(jIndirizzo.getString("citta"));
			} else {
				throw new DdtException("La citta' e' obbligatoria");
			}
			
			if(jIndirizzo.containsKey("provincia")) {
				indirizzo.setProvincia(jIndirizzo.getString("provincia"));
			} else {
				throw new DdtException("La provincia e' obbligatoria");
			}
			
			indirizzo.setTelefono(jIndirizzo.getString("telefono", ""));
			indirizzo.setFax(jIndirizzo.getString("fax", ""));
			indirizzo.setEmail(jIndirizzo.getString("email", ""));
		}
		return indirizzo;
	}

	private Recapito loadRecapitoFromJson(JsonObject jRecapito) throws DdtException {
		Recapito recapito = null;
		if(jRecapito != null) {
			recapito = new Recapito();
			
			if(jRecapito.containsKey("indirizzo")) {
				recapito.setIndirizzo(jRecapito.getString("indirizzo"));
			} else {
				throw new DdtException("Il campo indirizzo del recapito e' obbligatorio");
			}

			recapito.setNumeroCivico(jRecapito.getString("numeroCivico", ""));

			if(jRecapito.containsKey("cap")) {
				recapito.setCap(jRecapito.getString("cap"));
			} else {
				throw new DdtException("Il campo cap del recapito e' obbligatorio");
			}

			if(jRecapito.containsKey("citta")) {
				recapito.setCitta(jRecapito.getString("citta"));
			} else {
				throw new DdtException("Il campo citta' del recapito e' obbligatorio");
			}

			if(jRecapito.containsKey("provincia")) {
				recapito.setProvincia(jRecapito.getString("provincia"));
			} else {
				throw new DdtException("Il campo provincia del recapito e' obbligatorio");
			}
		}
		return recapito;
	}

	public static void main(String[] args) {
		String dcspPath = "/home/gv/Documents/Projects/DailyCash/ddt";
		String jsonFile = dcspPath + "/ddt.js";
		
		DdtGenerator ddtGenerator = null;
	
		try {
			String json = FileUtils.readFileToString(new File(jsonFile));
			if(json != null && json.length() > 0) {
				ddtGenerator = new DdtGenerator(json);
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
		
		if(pag == 0) {
		    PdfPCell sezioneMittenteDestinatario = new PdfPCell();
		    sezioneMittenteDestinatario.setPadding(0);
//		    sezioneMittenteDestinatario.setMinimumHeight(50);
		    sezioneMittenteDestinatario.setVerticalAlignment(Element.ALIGN_TOP);
		    sezioneMittenteDestinatario.setBorder(Rectangle.NO_BORDER);
		    sezioneMittenteDestinatario.addElement(creaSezioneMittenteDestinatario());
		    pagina.addCell(sezioneMittenteDestinatario);
		    pagina.addCell(creaSeparatore());
		}

	    PdfPCell intestazionePagina = new PdfPCell();
	    intestazionePagina.setPadding(0);
//	    intestazionePagina.setMinimumHeight(50);
	    intestazionePagina.setVerticalAlignment(Element.ALIGN_TOP);
	    intestazionePagina.setBorder(Rectangle.NO_BORDER);
	    intestazionePagina.addElement(creaIntestazionePagina(pag));
	    pagina.addCell(intestazionePagina);
	    pagina.addCell(creaSeparatore());

	    PdfPCell sezioneArticoli = new PdfPCell();
	    sezioneArticoli.setPadding(0);
//	    sezioneArticoli.setMinimumHeight(50);
	    sezioneArticoli.setVerticalAlignment(Element.ALIGN_TOP);
	    sezioneArticoli.setBorder(Rectangle.NO_BORDER);
	    sezioneArticoli.addElement(creaSezioneArticoli(pag));
	    pagina.addCell(sezioneArticoli);
	    pagina.addCell(creaSeparatore());
	    
	    if(pag == pagg - 1) {
		    PdfPCell sezioneVettore = new PdfPCell();
		    sezioneVettore.setPadding(0);
//		    sezioneVettore.setMinimumHeight(30);
		    sezioneVettore.setVerticalAlignment(Element.ALIGN_TOP);
		    sezioneVettore.setBorder(Rectangle.NO_BORDER);
		    sezioneVettore.addElement(creaSezioneVettore());
		    pagina.addCell(sezioneVettore);
		    pagina.addCell(creaSeparatore());
		    
		    PdfPCell sezioneAnnotazioni = new PdfPCell();
		    sezioneAnnotazioni.setPadding(0);
//		    sezioneAnnotazioni.setMinimumHeight(30);
		    sezioneAnnotazioni.setVerticalAlignment(Element.ALIGN_TOP);
		    sezioneAnnotazioni.setBorder(Rectangle.NO_BORDER);
		    sezioneAnnotazioni.addElement(creaSezioneAnnotazioni());
		    pagina.addCell(sezioneAnnotazioni);
		    pagina.addCell(creaSeparatore());
		    
		    PdfPCell sezioneFirme = new PdfPCell();
		    sezioneFirme.setPadding(0);
//		    sezioneFirme.setMinimumHeight(50);
		    sezioneFirme.setVerticalAlignment(Element.ALIGN_TOP);
		    sezioneFirme.setBorder(Rectangle.NO_BORDER);
		    sezioneFirme.addElement(creaSezioneFirme());
		    pagina.addCell(sezioneFirme);
	    }
	    
	    return pagina;
	}
	
	public void convertToPDF() throws FileNotFoundException, DocumentException {
		String outputFileName = properties.getProperty("output.path") + "/" + properties.getProperty("output.filename");
	    Document document = new Document();
	    PdfWriter.getInstance(document, new FileOutputStream(outputFileName));
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
	}
	
	private void creaHeader(PdfPTable tabella) {
	    
		Font fontLabel = new Font(FontFamily.HELVETICA, 8, Font.BOLD);

	    String codiceText = properties.getProperty("label.codice", "codice");
	    PdfPCell codice = new PdfPCell();
	    Paragraph codiceParagrafo = new Paragraph(codiceText, fontLabel);
	    codiceParagrafo.setAlignment(Element.ALIGN_CENTER);
	    codice.setMinimumHeight(15);
	    codice.addElement(codiceParagrafo);
	    codice.setUseAscender(true);
	    codice.setVerticalAlignment(Element.ALIGN_MIDDLE);
	    
	    String descrizioneText = properties.getProperty("label.descrizione", "descrizione");
	    PdfPCell descrizione = new PdfPCell();
	    Paragraph descrizioneParagrafo = new Paragraph(descrizioneText, fontLabel);
	    descrizioneParagrafo.setAlignment(Element.ALIGN_CENTER);
	    descrizione.setMinimumHeight(15);
	    descrizione.addElement(descrizioneParagrafo);
	    descrizione.setUseAscender(true);
	    descrizione.setVerticalAlignment(Element.ALIGN_MIDDLE);
	    
	    String umText = properties.getProperty("label.um", "um");;
	    PdfPCell um = new PdfPCell();
	    Paragraph umParagrafo = new Paragraph(umText, fontLabel);
	    umParagrafo.setAlignment(Element.ALIGN_CENTER);
	    um.setMinimumHeight(15);
	    um.addElement(umParagrafo);
	    um.setUseAscender(true);
	    um.setVerticalAlignment(Element.ALIGN_MIDDLE);
    
	    String quantitaText = properties.getProperty("label.quantita", "quantita");
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
		float[] pointColumnWidths = {21F, 21F, 23F, 15F, 20F};
		table = new PdfPTable(pointColumnWidths);
	    table.setWidthPercentage(100);
	    
	    PdfPCell tipoDocumento = new PdfPCell();
	    tipoDocumento.setPadding(0);
	    tipoDocumento.setNoWrap(true);
	    tipoDocumento.setVerticalAlignment(Element.ALIGN_TOP);
	    tipoDocumento.addElement(creaCampo(properties.getProperty("label.tipodoc", "Tipo Documento"), this.documentoDiTrasporto.getTipoDocumento()));

	    PdfPCell dataDocumento = new PdfPCell();
	    dataDocumento.setPadding(0);
	    tipoDocumento.setNoWrap(true);
	    dataDocumento.setVerticalAlignment(Element.ALIGN_TOP);
	    dataDocumento.addElement(creaCampo(properties.getProperty("label.datadoc", "Data Documento"), this.documentoDiTrasporto.getDataDocumento()));

	    PdfPCell numeroDocumento = new PdfPCell();
	    numeroDocumento.setPadding(0);
	    tipoDocumento.setNoWrap(true);
	    numeroDocumento.setVerticalAlignment(Element.ALIGN_TOP);
	    numeroDocumento.addElement(creaCampo(properties.getProperty("label.numdoc", "N.ro Documento"), this.documentoDiTrasporto.getNumeroDocumento()));

	    PdfPCell pagina = new PdfPCell();
	    pagina.setPadding(0);
	    tipoDocumento.setNoWrap(true);
	    pagina.setVerticalAlignment(Element.ALIGN_TOP);
	    pagina.addElement(creaCampo(properties.getProperty("label.pagina", "Pagina"), (pag + 1) + " / " + pagg));

	    PdfPCell causaleTrasporto = new PdfPCell();
	    causaleTrasporto.setPadding(0);
	    tipoDocumento.setNoWrap(true);
	    causaleTrasporto.setVerticalAlignment(Element.ALIGN_TOP);
	    causaleTrasporto.addElement(creaCampo(properties.getProperty("label.causale", "Causale"), this.documentoDiTrasporto.getCausaleTrasporto()));
	    
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
	    firmamittente.addElement(creaCampo(properties.getProperty("label.firmamittente", "Firma Mittente"), " "));

	    PdfPCell firmadestinatario = new PdfPCell();
	    firmadestinatario.setPadding(0);
	    firmadestinatario.setVerticalAlignment(Element.ALIGN_TOP);
	    firmadestinatario.addElement(creaCampo(properties.getProperty("label.firmadestinatario", "Firma Destinatario"), " "));

	    PdfPCell firmavettore = new PdfPCell();
	    firmavettore.setPadding(0);
	    firmavettore.setVerticalAlignment(Element.ALIGN_TOP);
	    firmavettore.addElement(creaCampo(properties.getProperty("label.firmavettore", "Firma Vettore"), " "));

	    
	    table.addCell(firmamittente);
	    table.addCell(firmavettore);
	    table.addCell(firmadestinatario);

	    return table;
	}
	
	
	private PdfPTable creaSezioneVettore() {

		float[] pointColumnWidths = {1F};
		String vettoreLabelText = properties.getProperty("label.vettore", "Vettore");
		
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
	
	private PdfPTable creaSezioneFirme_old() {

		float[] pointColumnWidths = {1F};
		Font fontLabel = new Font(FontFamily.HELVETICA, 10, Font.BOLD);

		String firmaMittenteLabelText = properties.getProperty("label.firmamittente", "Firma Mittente");
		String firmaDestinatarioLabelText =  properties.getProperty("label.firmadestinatario", "Firma Destinatario");
		String firmaVettoreLabelText =  properties.getProperty("label.firmavettore", "Firma Vettore");
		
	    Paragraph paragrafoFirmaMittenteLabel = new Paragraph(firmaMittenteLabelText, fontLabel);
	    paragrafoFirmaMittenteLabel.setLeading(0, 1);

	    Paragraph paragrafoFirmaDestinatarioLabel = new Paragraph(firmaDestinatarioLabelText, fontLabel);
	    paragrafoFirmaDestinatarioLabel.setLeading(0, 1);

	    Paragraph paragrafoFirmaVettoreLabel = new Paragraph(firmaVettoreLabelText, fontLabel);
	    paragrafoFirmaVettoreLabel.setLeading(0, 1);

		PdfPTable tabella = new PdfPTable(pointColumnWidths);
	    tabella.setWidthPercentage(100);
	    
	    PdfPCell firmaMittenteLabel = new PdfPCell();
	    firmaMittenteLabel.setPadding(TAB_PADDING);
	    firmaMittenteLabel.setFixedHeight(20);
	    firmaMittenteLabel.setVerticalAlignment(Element.ALIGN_TOP);
	    firmaMittenteLabel.addElement(paragrafoFirmaMittenteLabel);
	    
	    PdfPCell firmaDestinatarioLabel = new PdfPCell();
	    firmaDestinatarioLabel.setPadding(TAB_PADDING);
	    firmaDestinatarioLabel.setFixedHeight(20);
	    firmaDestinatarioLabel.setVerticalAlignment(Element.ALIGN_TOP);
	    firmaDestinatarioLabel.addElement(paragrafoFirmaDestinatarioLabel);
	    
	    PdfPCell firmaVettoreLabel = new PdfPCell();
	    firmaVettoreLabel.setPadding(TAB_PADDING);
	    firmaVettoreLabel.setFixedHeight(20);
	    firmaVettoreLabel.setVerticalAlignment(Element.ALIGN_TOP);
	    firmaVettoreLabel.addElement(paragrafoFirmaVettoreLabel);
	    

	    tabella.addCell(firmaMittenteLabel);
	    tabella.addCell(firmaDestinatarioLabel);
	    tabella.addCell(firmaVettoreLabel);

		return tabella;
	}
	
	private PdfPTable creaSezioneAnnotazioni() {

		float[] pointColumnWidths = {1F};
		String annotazioniLabelText = properties.getProperty("label.annotazioni", "Annotazioni");
		
		Font fontLabel = new Font(FontFamily.HELVETICA, 10, Font.BOLD);
	    Paragraph paragrafoLabel = new Paragraph(annotazioniLabelText, fontLabel);
	    paragrafoLabel.setLeading(0, 1);

		PdfPTable tabella = new PdfPTable(pointColumnWidths);
	    tabella.setWidthPercentage(100);
	    
	    PdfPCell annotazioniLabel = new PdfPCell();
	    annotazioniLabel.setPadding(TAB_PADDING);
	    annotazioniLabel.setFixedHeight(20);
	    annotazioniLabel.setVerticalAlignment(Element.ALIGN_TOP);
	    annotazioniLabel.addElement(paragrafoLabel);
	    annotazioniLabel.setBorder(Rectangle.NO_BORDER);
	    
	    PdfPCell annotazioniInfo = new PdfPCell();
	    annotazioniInfo.setPadding(TAB_PADDING);
	    annotazioniInfo.setMinimumHeight(10);
	    annotazioniInfo.setVerticalAlignment(Element.ALIGN_TOP);
	    annotazioniInfo.setBorder(Rectangle.NO_BORDER);

	    tabella.addCell(annotazioniLabel);
	    tabella.addCell(annotazioniInfo);

		PdfPTable tabellaAnnotazioni = null;
		tabellaAnnotazioni = new PdfPTable(pointColumnWidths);
		tabellaAnnotazioni.setWidthPercentage(100);
		tabellaAnnotazioni.setExtendLastRow(true);

	    PdfPCell headerAnnotazioni = new PdfPCell();
	    headerAnnotazioni.setPadding(0);
	    headerAnnotazioni.addElement(tabella);
	    headerAnnotazioni.setBorder(Rectangle.BOX);
	    tabellaAnnotazioni.addCell(headerAnnotazioni);

		return tabellaAnnotazioni;
	}
	

	private PdfPTable creaSezioneMittenteDestinatario() {
		PdfPTable table = null;
		float[] pointColumnWidths = {1F, 1F};
		table = new PdfPTable(pointColumnWidths);
	    table.setWidthPercentage(100);
	    
	    PdfPCell mittente = new PdfPCell();
	    mittente.setPaddingRight(TAB_PADDING);
	    mittente.setPaddingLeft(0);
//	    mittente.setMinimumHeight(50);
	    mittente.setVerticalAlignment(Element.ALIGN_TOP);
	    mittente.addElement(creaMittente());
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
	
	private PdfPTable creaMittente() {

		float[] pointColumnWidths = {1F};
		String mittenteLabelText = properties.getProperty("label.mittente", "Mittente:");
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

		String destinatarioLabelText = properties.getProperty("label.destinatario", "Destinatario");;
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
	
	private PdfPTable creaLuogoDestinazione() {
		
		String luogoDestinazioneLabelText = properties.getProperty("label.luogodestinazione", "Luogo di destinazione");
		String luogoDestinazioneInfoText = "";
		
		if(documentoDiTrasporto.getLuogoDestinazione() != null) {
			if(documentoDiTrasporto.getLuogoDestinazione().getDenominazione() != null && 
					documentoDiTrasporto.getLuogoDestinazione().getDenominazione().length()>0) {
				luogoDestinazioneInfoText = documentoDiTrasporto.getLuogoDestinazione().getDenominazione() + "\n";
			}
			luogoDestinazioneInfoText += documentoDiTrasporto.getLuogoDestinazione().getRecapitoAsString();
		}
		
		Font fontLabel = new Font(FontFamily.HELVETICA, 10, Font.BOLD);
	    Paragraph paragrafoLabel = new Paragraph(luogoDestinazioneLabelText, fontLabel);
	    paragrafoLabel.setLeading(0, 1);

	    Font fontInfo = new Font(FontFamily.HELVETICA, 8, Font.NORMAL);
	    Paragraph paragrafoInfo = new Paragraph(luogoDestinazioneInfoText, fontInfo);
	    paragrafoInfo.setLeading(0, 1);

		
		PdfPTable luogoDestinazione = null;
		float[] pointColumnWidths = {1F};
		luogoDestinazione = new PdfPTable(pointColumnWidths);
		luogoDestinazione.setWidthPercentage(100);
	    
	    PdfPCell luogoDestinazioneLabel = new PdfPCell();
	    luogoDestinazioneLabel.setPadding(TAB_PADDING);
	    luogoDestinazioneLabel.setFixedHeight(20);
	    luogoDestinazioneLabel.setVerticalAlignment(Element.ALIGN_TOP);
	    luogoDestinazioneLabel.addElement(paragrafoLabel);
	    luogoDestinazioneLabel.setBorder(Rectangle.NO_BORDER);
	    
	    PdfPCell luogoDestinazioneInfo = new PdfPCell();
	    luogoDestinazioneInfo.setPadding(TAB_PADDING);
	    luogoDestinazioneInfo.setMinimumHeight(50);
	    luogoDestinazioneInfo.setVerticalAlignment(Element.ALIGN_TOP);
	    luogoDestinazioneInfo.addElement(paragrafoInfo);
	    luogoDestinazioneInfo.setBorder(Rectangle.NO_BORDER);

	    luogoDestinazione.addCell(luogoDestinazioneLabel);
	    luogoDestinazione.addCell(luogoDestinazioneInfo);
		return luogoDestinazione;
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
	    headerLuogoDestinatario.addElement(creaLuogoDestinazione());
	    headerLuogoDestinatario.setBorder(Rectangle.BOX);
	    destinatario.addCell(headerLuogoDestinatario);

		return destinatario;
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
