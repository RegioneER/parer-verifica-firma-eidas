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
import java.util.ArrayList;
import java.util.List;

/**
 * DTO per mappare l'esito del servizio di verifica firma.
 *
 * @author Snidero_L
 */
public class VerificaFirmaResultBean implements Serializable {

    private static final long serialVersionUID = -8962897347751304790L;

    private String simpleReport;
    private String detailedReport;
    private String diagnosticData;

    private String simpleReportXml;
    private String detailedReportXml;
    private String diagnosticDataXml;

    private boolean withErrors = false;

    private int livello;
    private int busta;

    private VerificaFirmaResultBean parent;
    private List<VerificaFirmaResultBean> children;

    public String getSimpleReport() {
        return simpleReport;
    }

    public void setSimpleReport(String simpleReport) {
        this.simpleReport = simpleReport;
    }

    public String getDetailedReport() {
        return detailedReport;
    }

    public void setDetailedReport(String detailedReport) {
        this.detailedReport = detailedReport;
    }

    public String getDiagnosticData() {
        return diagnosticData;
    }

    public void setDiagnosticData(String diagnosticData) {
        this.diagnosticData = diagnosticData;
    }

    public int getLivello() {
        return livello;
    }

    public void setLivello(int livello) {
        this.livello = livello;
    }

    public int getBusta() {
        return busta;
    }

    public void setBusta(int busta) {
        this.busta = busta;
    }

    public String getSimpleReportXml() {
        return simpleReportXml;
    }

    public void setSimpleReportXml(String simpleReportXml) {
        this.simpleReportXml = simpleReportXml;
    }

    public String getDetailedReportXml() {
        return detailedReportXml;
    }

    public void setDetailedReportXml(String detailedReportXml) {
        this.detailedReportXml = detailedReportXml;
    }

    public String getDiagnosticDataXml() {
        return diagnosticDataXml;
    }

    public void setDiagnosticDataXml(String diagnosticDataXml) {
        this.diagnosticDataXml = diagnosticDataXml;
    }

    /**
     * @return the parent
     */
    public VerificaFirmaResultBean getParent() {
        return parent;
    }

    /**
     * @param parent
     *            the parent to set
     */
    public void setParent(VerificaFirmaResultBean parent) {
        this.parent = parent;
    }

    public List<VerificaFirmaResultBean> getChildren() {
        if (this.children == null) {
            return new ArrayList<>();
        }
        return children;
    }

    public void setChildren(List<VerificaFirmaResultBean> children) {
        this.children = children;
    }

    public boolean isWithErrors() {
        return withErrors;
    }

    public void setWithErrors(boolean withErrors) {
        this.withErrors = withErrors;
    }

    public boolean add(VerificaFirmaResultBean e) {
        if (children == null) {
            children = new ArrayList<>();
        }
        return children.add(e);
    }

    // Helper per la navigazione
    public VerificaFirmaResultBean ricerca(final int livello, final int busta) {
        if (livello == this.livello && busta == this.busta) {
            return this;
        }

        VerificaFirmaResultBean result = null;
        if (children != null) {
            for (VerificaFirmaResultBean child : children) {
                result = child.ricerca(livello, busta);
                if (result != null) {
                    return result;
                }
            }
        }
        return result;
    }

}
