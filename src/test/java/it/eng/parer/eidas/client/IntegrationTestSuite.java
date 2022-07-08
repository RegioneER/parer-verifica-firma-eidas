package it.eng.parer.eidas.client;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

import it.eng.parer.eidas.client.rest.RestTemplateActuatorTest;
import it.eng.parer.eidas.client.rest.RestTemplateVerificaFirmaTest;

/*@SelectClasses permette di definire puntualmente l'ordine di esecuzione*/
@SelectClasses({ RestTemplateActuatorTest.class, RestTemplateVerificaFirmaTest.class })
@Suite
public class IntegrationTestSuite {

}
