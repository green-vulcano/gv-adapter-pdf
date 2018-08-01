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
package com.greenvulcano.dcsp.ddt.beans;

public class Indirizzo {

	private String indirizzo;
	private String numeroCivico;
	private String citta;
	private String provincia;
	private String cap;
	private String telefono;
	private String fax;
	private String email;
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
	
	public String getTelefono() {
		return telefono;
	}
	public void setTelefono(String telefono) {
		this.telefono = telefono;
	}
	public String getFax() {
		return fax;
	}
	public void setFax(String fax) {
		this.fax = fax;
	}
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getIndirizzoAsString () {
		StringBuffer ret = new StringBuffer();
		
		ret.append(indirizzo);
		if(numeroCivico != null && numeroCivico.length() > 0) {
			ret.append(" " + numeroCivico);
		}
		
		if(cap != null && cap.length() > 0) {
			ret.append("\n");
			ret.append(cap);
		}
		
		if(citta != null && citta.length() > 0) {
			ret.append(" ");
			ret.append(citta);
		}

		if(provincia != null && provincia.length() > 0) {
			ret.append(" ");
			ret.append("(" + provincia + ")");
		}
		
		if(telefono != null && telefono.length() > 0) {
			ret.append("\n");
			ret.append("Tel.: ");
			ret.append(telefono);
		}
		
		if(fax != null && fax.length() > 0) {
			ret.append("\n");
			ret.append("Fax: ");
			ret.append(fax);
		}
		
		if(email != null && email.length() > 0) {
			ret.append("\n");
			ret.append("Email: ");
			ret.append(email);
		}
		
		return ret.toString();
	}
}
