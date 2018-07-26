package com.greenvulcano.dcsp.ddt.beans;

public class Recapito {

	private String indirizzo;
	private String numeroCivico;
	private String citta;
	private String provincia;
	private String cap;
	public String getIndirizzo() {
		return indirizzo;
	}
	public void setIndirizzo(String indirizzo) {
		this.indirizzo = indirizzo;
	}
	public String getNumeroCivico() {
		return numeroCivico;
	}
	public void setNumeroCivico(String numeroCivico) {
		this.numeroCivico = numeroCivico;
	}
	public String getCitta() {
		return citta;
	}
	public void setCitta(String citta) {
		this.citta = citta;
	}
	public String getProvincia() {
		return provincia;
	}
	public void setProvincia(String provincia) {
		this.provincia = provincia;
	}
	public String getCap() {
		return cap;
	}
	public void setCap(String cap) {
		this.cap = cap;
	}
	
	public String getRecapitoAsString () {
		StringBuffer ret = new StringBuffer();
		ret.append(indirizzo);
		if(numeroCivico != null && numeroCivico.length() > 0) {
			ret.append(" " + numeroCivico);
		}
		ret.append("\n");
		
		if(cap != null && cap.length() > 0) {
			ret.append(cap);
		}
		
		if(citta != null && citta.length() > 0) {
			ret.append(" ");
			ret.append(citta);
		}

		if(provincia != null && provincia.length() > 0) {
			ret.append(" ");
			ret.append("("  + provincia + ")");
		}
		
		return ret.toString();
	}
}
