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

import java.util.Vector;

public class DDT {

	private String tipoDocumento;
	private String dataDocumento;
	private String numeroDocumento;
	private String causaleTrasporto;
	private String vettore;
	private String annotazioni;

	private Mittente mittente;
	private Destinatario destinatario;
	private LuogoDestinazione luogoDestinazione;
	private Vector<Articolo>articoli;
	
	public String getTipoDocumento() {
		return tipoDocumento;
	}
	public void setTipoDocumento(String tipoDocumento) {
		this.tipoDocumento = tipoDocumento;
	}
	public String getDataDocumento() {
		return dataDocumento;
	}
	public void setDataDocumento(String dataDocumento) {
		this.dataDocumento = dataDocumento;
	}
	public String getNumeroDocumento() {
		return numeroDocumento;
	}
	public void setNumeroDocumento(String numeroDocumento) {
		this.numeroDocumento = numeroDocumento;
	}
	public String getCausaleTrasporto() {
		return causaleTrasporto;
	}
	public void setCausaleTrasporto(String causaleTrasporto) {
		this.causaleTrasporto = causaleTrasporto;
	}
	public String getVettore() {
		return vettore;
	}
	public void setVettore(String vettore) {
		this.vettore = vettore;
	}
	public String getAnnotazioni() {
		return annotazioni;
	}
	public void setAnnotazioni(String annotazioni) {
		this.annotazioni = annotazioni;
	}
	public Mittente getMittente() {
		return mittente;
	}
	public void setMittente(Mittente mittente) {
		this.mittente = mittente;
	}
	public Destinatario getDestinatario() {
		return destinatario;
	}
	public void setDestinatario(Destinatario destinatario) {
		this.destinatario = destinatario;
	}
	public LuogoDestinazione getLuogoDestinazione() {
		return luogoDestinazione;
	}
	public void setLuogoDestinazione(LuogoDestinazione luogoDestinazione) {
		this.luogoDestinazione = luogoDestinazione;
	}
	public Vector<Articolo> getArticoli() {
		return articoli;
	}
	public void setArticoli(Vector<Articolo> articoli) {
		this.articoli = articoli;
	}
}
