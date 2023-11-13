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

/**
 * DTO per mappare l'esito del servizio di verifica firma.
 *
 * @author Snidero_L
 */
public class VerificaFirmaResultPaginatorBean implements Serializable {

    private static final long serialVersionUID = 8127555240225439382L;

    private int nrResult = 1;
    private int curPage = 1;

    /**
     * @return the nrResult
     */
    public int getNrResult() {
        return nrResult;
    }

    public void incNrResult() {
        nrResult++;
    }

    /**
     * @return the curPage
     */
    public int getCurPage() {
        return curPage;
    }

    /**
     * @param curPage
     *            the curPage to set
     */
    public void setCurPage(int curPage) {
        this.curPage = curPage;
    }

}
