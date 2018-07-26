package com.greenvulcano.dcsp.ddt.beans;

public class LuogoDestinazione {
	private String denominazione;
	private Recapito recapito;
	public String getDenominazione() {
		return denominazione;
	}
	public void setDenominazione(String denominazione) {
		this.denominazione = denominazione;
	}
	public String getRecapitoAsString() {
		return recapito.getRecapitoAsString();
	}
	public Recapito getRecapito() {
		return recapito;
	}
	public void setRecapito(Recapito indirizzo) {
		this.recapito = indirizzo;
	}
	
}
