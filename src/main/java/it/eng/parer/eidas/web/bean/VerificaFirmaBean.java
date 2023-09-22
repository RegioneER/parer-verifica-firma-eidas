/*
 * Engineering Ingegneria Informatica S.p.A.
 *
 * Copyright (C) 2023 Regione Emilia-Romagna
 * <p/>
 * This program is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or (at your option) any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package it.eng.parer.eidas.web.bean;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;

/**
 * DTO per la pagina di verifica firma manuale.
 *
 * @author Snidero_L
 */
public class VerificaFirmaBean implements Serializable {

    private static final long serialVersionUID = -4277775905954308303L;

    private boolean abilitaControlloRevoca = true;
    private boolean abilitaControlloCatenaTrusted = true;
    private boolean abilitaControlloCa = true;
    private boolean abilitaControlloCrittografico = true;
    private boolean includiRaw = false;
    // il tag html5 restiutisce la data in formato iso
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dataRiferimento;
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime oraRiferimento;
    @NotNull(message = "Il file da verificare non può essere vuoto")
    private transient MultipartFile fileDaVerificare;
    private transient MultipartFile fileDssPolicy;
    private transient List<MultipartFile> fileOriginali;

    public boolean isAbilitaControlloRevoca() {
        return abilitaControlloRevoca;
    }

    public void setAbilitaControlloRevoca(boolean abilitaControlloRevoca) {
        this.abilitaControlloRevoca = abilitaControlloRevoca;
    }

    public boolean isAbilitaControlloCatenaTrusted() {
        return abilitaControlloCatenaTrusted;
    }

    public void setAbilitaControlloCatenaTrusted(boolean abilitaControlloCatenaTrusted) {
        this.abilitaControlloCatenaTrusted = abilitaControlloCatenaTrusted;
    }

    public boolean isAbilitaControlloCa() {
        return abilitaControlloCa;
    }

    public void setAbilitaControlloCa(boolean abilitaControlloCa) {
        this.abilitaControlloCa = abilitaControlloCa;
    }

    public boolean isAbilitaControlloCrittografico() {
        return abilitaControlloCrittografico;
    }

    public void setAbilitaControlloCrittografico(boolean abilitaControlloCrittografico) {
        this.abilitaControlloCrittografico = abilitaControlloCrittografico;
    }

    /**
     * @return the includiRaw
     */
    public boolean isIncludiRaw() {
        return includiRaw;
    }

    /**
     * @param includiRaw
     *            the includiRaw to set
     */
    public void setIncludiRaw(boolean includiRaw) {
        this.includiRaw = includiRaw;
    }

    public LocalDate getDataRiferimento() {
        return dataRiferimento;
    }

    public void setDataRiferimento(LocalDate dataRiferimento) {
        this.dataRiferimento = dataRiferimento;
    }

    public LocalTime getOraRiferimento() {
        return oraRiferimento;
    }

    public void setOraRiferimento(LocalTime oraRiferimento) {
        this.oraRiferimento = oraRiferimento;
    }

    public MultipartFile getFileDaVerificare() {
        return fileDaVerificare;
    }

    public void setFileDaVerificare(MultipartFile fileDaVerificare) {
        this.fileDaVerificare = fileDaVerificare;
    }

    public MultipartFile getFileDssPolicy() {
        return fileDssPolicy;
    }

    public void setFileDssPolicy(MultipartFile fileDssPolicy) {
        this.fileDssPolicy = fileDssPolicy;
    }

    public List<MultipartFile> getFileOriginali() {
        return fileOriginali;
    }

    public void setFileOriginali(List<MultipartFile> fileOriginali) {
        this.fileOriginali = fileOriginali;
    }

}
