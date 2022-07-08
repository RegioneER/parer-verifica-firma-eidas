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
