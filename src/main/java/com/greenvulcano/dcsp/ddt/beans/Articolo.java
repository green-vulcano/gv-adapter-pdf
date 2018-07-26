package com.greenvulcano.dcsp.ddt.beans;

public class Articolo {
	private String codice;
	private String descrizione;
	private String um;
	private String quantita;
	public String getCodice() {
		return codice;
	}
	public void setCodice(String codice) {
		this.codice = codice;
	}
	public String getDescrizione() {
		return descrizione;
	}
	public void setDescrizione(String descrizione) {
		this.descrizione = descrizione;
	}
	public String getUm() {
		return um;
	}
	public void setUm(String um) {
		this.um = um;
	}
	public String getQuantita() {
		return quantita;
	}
	public void setQuantita(String quantita) {
		this.quantita = quantita;
	}
	public Articolo() {
		super();
	}
	public Articolo(String codice, String descrizione, String um, String quantita) {
		super();
		this.codice = codice;
		this.descrizione = descrizione;
		this.um = um;
		this.quantita = quantita;
	}
}
