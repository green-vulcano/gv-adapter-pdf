package com.greenvulcano.dcsp.ddt.beans;

public class Mittente {
	private String denominazione;
	private String partitaIva;
	private Indirizzo indirizzo;
	public String getDenominazione() {
		return denominazione;
	}
	public void setDenominazione(String denominazione) {
		this.denominazione = denominazione;
	}
	public String getPartitaIva() {
		return partitaIva;
	}
	public String getPartitaIvaAsString() {
		return "P. Iva: " + partitaIva;
	}
	public void setPartitaIva(String partitaIva) {
		this.partitaIva = partitaIva;
	}
	public Indirizzo getIndirizzo() {
		return indirizzo;
	}
	public String getIndirizzoAsString() {
		return indirizzo.getIndirizzoAsString();
	}
	
	public void setIndirizzo(Indirizzo indirizzo) {
		this.indirizzo = indirizzo;
	}

}
