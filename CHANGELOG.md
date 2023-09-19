
## 1.11.1 (13-09-2023)

### Bugfix: 2
- [#30189](https://parermine.regione.emilia-romagna.it/issues/30189) Eliminazione OracleDB constraint e introduzione configurazione per eliminazione di oggetti "non più validi"
- [#30078](https://parermine.regione.emilia-romagna.it/issues/30078) Correzione processo di verifica documenti "multi busta" con DSS-EIDAS versione 5.12

## 1.11.0 (16-08-2023)

### Novità: 3
- [#29863](https://parermine.regione.emilia-romagna.it/issues/29863) Aggiornamento librerie obsolete 2023
- [#29853](https://parermine.regione.emilia-romagna.it/issues/29853) Aggiornamento alle versione 5.12 della libreria DSS
- [#29852](https://parermine.regione.emilia-romagna.it/issues/29852) Modifica gestione TLS (rimosso il default alla versione v1.2) con inserimento certificato trusted "Certigna Services CA" 

## 1.10.0 (28-06-2023)

### Novità: 2
- [#29717](https://parermine.regione.emilia-romagna.it/issues/29717) Gestione casi in cui il certificato del responder OCSP ha data di inizio validità maggiore del riferimento temporale a cui si chiede di verificare la firma
- [#29693](https://parermine.regione.emilia-romagna.it/issues/29693) Introduzione degli attribute su reponse dei servizi (ETag + last-modified) e aggiornamento spring boot

## 1.9.0 (19-05-2023)

### Novità: 1
- [#27885](https://parermine.regione.emilia-romagna.it/issues/27885) Gestione risposta Verifica Eidas se impostato a true UtilizzoDataFirmaPerRifTemp

## 1.8.0 (28-02-2023)

### Novità: 1
- [#28008](https://parermine.regione.emilia-romagna.it/issues/28008) Modifica parametri di input per object storage

## 1.7.0 (10-01-2023)

### Novità: 1
- [#28141](https://parermine.regione.emilia-romagna.it/issues/28141)  Aggiornamento libreria dss 5.11

## 1.6.0 (23-11-2022)

### Novità: 1
- [#27525](https://parermine.regione.emilia-romagna.it/issues/27525) aggiornamento libreria dss 5.10

## 1.5.0 (18-10-2022)

### Novità: 1
- [#27361](https://parermine.regione.emilia-romagna.it/issues/27361) Analisi librerie obsolete 2022

## 1.4.1 (03-08-2022)

### Bugfix: 1
- [#27524](https://parermine.regione.emilia-romagna.it/issues/27524) Correzione verifica firma documenti PDF PADES

## 1.4.0 (31-05-2022)

### Novità: 1
- [#27181](https://parermine.regione.emilia-romagna.it/issues/27181) Aggiornamento libreria DSS 5.9

## 1.3.4

### Bugfix: 1
- [#27182](https://parermine.regione.emilia-romagna.it/issues/27182) Correzione gestione log applicativi

### Novità: 2
- [#27183](https://parermine.regione.emilia-romagna.it/issues/27183) Migrazione su Oracle del db h2 interno all'applicazione 
- [#27013](https://parermine.regione.emilia-romagna.it/issues/27013) Introduzione supporto decodifica base64 

## 1.3.3

### Novità: 1
- [#27098](https://parermine.regione.emilia-romagna.it/issues/27098) Aggiornamento Springboot per vulnerabilità Spring4Shell

## 1.3.2 (08-02-2022)

### Novità: 1
- [#26665](https://parermine.regione.emilia-romagna.it/issues/26665) Aggiornamento librerie obsolete primo quadrimestre 2021

## 1.3.1 (09-12-2021)

### Novità: 1
- [#26363](https://parermine.regione.emilia-romagna.it/issues/26363)  Gestione system logging attraverso logback

## 1.3.0

### Novità: 1
- [#24518](https://parermine.regione.emilia-romagna.it/issues/24518) Gestione degli ocsp in fase di verifica firme

## 1.2.2

### Bugfix: 1
- [#25763](https://parermine.regione.emilia-romagna.it/issues/25763) Correzione recupero mimetype

## 1.2.1

### Novità: 1
- [#25402](https://parermine.regione.emilia-romagna.it/issues/25402) Aggiornamento jdk versione 11
## 1.2.0

### Novità: 1
- [#25244](https://parermine.regione.emilia-romagna.it/issues/25244) Aggiornamento immagine Ubi RedHat JDK 11

## 1.1.8 (20-05-2021)

### Novità: 1
- [#25133](https://parermine.regione.emilia-romagna.it/issues/25133)  Aggiornamento immagine Ubi RedHat


## 1.0.5 (2019-11-12)
### Added
 * [94f59b8](../../commit/94f59b8) - __(Stefano Sinatti)__ /v1/report-firma output XML / EidasParerException as JSON
 * [b7a31f5](../../commit/b7a31f5) - __(Stefano Sinatti)__ REST API: Fixed Jaxb serializazion problem (Response JSON Object)
 * [8879489](../../commit/8879489) - __(Lorenzo Snidero)__ Disabilitato il controllo OCSP

Disabilitato il controllo delle revoche tramite OCSP. In questo recupera le informazioni dalla lista di distribuzione delle CRL (ove specificata).
Quando verrà effettuata l'implementazione anche su SACER dell'OCSP andrà ri-abilitato.

 
 * [9e98d69](../../commit/9e98d69) - __(Stefano Sinatti)__ fix exception management
 * [d0aee61](../../commit/d0aee61) - __(Lorenzo Snidero)__ FIX: rilanciata l'eccezione gestita

Rilanciata l'eccezione gestita nel blocco try-catch-finally.
Wrappando l'eccezione in un'eccezione _terminale_ gestita è possibile, lato sacerws, gestire in maniera più efficiente il retry mandandolo in corto-circuito.
Senza rilanciare l'eccezione su sacerws andava in null pointer exception il popolamento di EsitoValidazioneEidas (i report erano nulli).

 * [812317d](../../commit/812317d) - __(Stefano Sinatti)__ fix gestione exception with final / log timing
 * [b121f31](../../commit/b121f31) - __(Stefano Sinatti)__ [ci skip] fix default config
 * [a2bce05](../../commit/a2bce05) - __(Lorenzo Snidero)__ FIX: Inizializzazione lista su eccezione default

Nel metodo toString dell'EidasParerException viene ciclata la lista dei dettagli. Se non viene inizializzata viene lanciata un'eccezione.
Inizializzata la lista con un ArrayList vuoto.

 * [32d9402](../../commit/32d9402) - __(Stefano Sinatti)__ [ci skip] fix deployments config
 * [d64f6c9](../../commit/d64f6c9) - __(Stefano Sinatti)__ fix UI + dss.properties su yaml
 * [502c054](../../commit/502c054) - __(Stefano Sinatti)__ fix UI
 * [42e96b2](../../commit/42e96b2) - __(Stefano Sinatti)__ re-factor coding
 * [d4d8f95](../../commit/d4d8f95) - __(Stefano Sinatti)__ introdotta validazione input / fix UI
 * [6cd9460](../../commit/6cd9460) - __(Stefano Sinatti)__ [ci skip] fix formatted java file
 * [59f8219](../../commit/59f8219) - __(Stefano Sinatti)__ input verified
 * [d180590](../../commit/d180590) - __(Stefano Sinatti)__ [ci skip] fix / cleaning code / tika 1.22
 * [6408e60](../../commit/6408e60) - __(Stefano Sinatti)__ fix / cleaning code
 * [1060e41](../../commit/1060e41) - __(Stefano Sinatti)__ [ci skip] DSS + H2
 * [047d2fb](../../commit/047d2fb) - __(Stefano Sinatti)__ logging with UUID / fix code
 * [a0b69a4](../../commit/a0b69a4) - __(Stefano Sinatti)__ fix : verifica se report contiene almeno una firma
 * [8a7e452](../../commit/8a7e452) - __(Stefano Sinatti)__ fix gestione crl number
 * [bde8412](../../commit/bde8412) - __(Stefano Sinatti)__ fix gestione input idComponente/SottoComponente
 * [425d629](../../commit/425d629) - __(Stefano Sinatti)__ [ci skip] Update .gitlab-ci.yml
 * [cad16d6](../../commit/cad16d6) - __(Stefano Sinatti)__ fix page
 * [7263d7f](../../commit/7263d7f) - __(Stefano Sinatti)__ added search on prop
 * [7da3b34](../../commit/7da3b34) - __(Stefano Sinatti)__ thymeleaf fragments applied
 * [2d34546](../../commit/2d34546) - __(Stefano Sinatti)__ fix code and okd deployments
 * [53b75df](../../commit/53b75df) - __(Stefano Sinatti)__ fix html
 * [1e1c5fe](../../commit/1e1c5fe) - __(Stefano Sinatti)__ Spring security supported
 * [309244d](../../commit/309244d) - __(Lorenzo Snidero)__ FEAT: Aggiunte date di validazione

Aggiunta compilazione di data/ora di inizio e fine verifica.

 * [c32c4a3](../../commit/c32c4a3) - __(Lorenzo Snidero)__ FIX: aggiunta propOrder relativa al selfLink

Aggiunto nell'oggetto di risposta eidas il selfLink. Senza questa aggiunta il marshal (su sacerws) fallisce

* [f4a995d](../../commit/f4a995d) - __(Stefano Sinatti)__ fix exception handler
 * [a4d2b9b](../../commit/a4d2b9b) - __(Stefano Sinatti)__ SelfLink added & code re-factor
 * [1d29c1c](../../commit/1d29c1c) - __(Stefano Sinatti)__ aggiunto su model EsitoValidazioneEidas
 * [2609909](../../commit/2609909) - __(Lorenzo Snidero)__ Aggiunta ENV_VAR per abilitare Jolokia. Aggiunta la variabile d'ambiente AB_JOLOKIA_OFF a true per esporre jolokia fornito da spring boot.
 * [5466c96](../../commit/5466c96) - __(Stefano Sinatti)__ actuator exposed changed
 * [2609909](../../commit/2609909) - __(Lorenzo Snidero)__ Aggiunta ENV_VAR per abilitare Jolokia

Aggiunta la variabile d'ambiente AB_JOLOKIA_OFF a true per esporre jolokia fornito da spring boot.

 * [9d1687f](../../commit/9d1687f) - __(Lorenzo Snidero)__ Recupero del crl number

Aggiunta l'estrazione dell'estensione CRL number (OID 2.5.29.20) dalla CRL.
L'informazione è stata aggiunta alla mappa delle estensioni.

 * [8156fc9](../../commit/8156fc9) - __(Lorenzo Snidero)__ Compilazione estensioni su marca e firma

Aggiunta delle estensioni relative alla marca e alla firma sul report.
Le estensioni sono relative al base64 della firma o della marca apposta.

 * [2bcc311](../../commit/2bcc311) - __(Lorenzo Snidero)__ Estrazione del base64 relativa alla firma/marca

Estrazione dei byte relativi alla firma. Tali byte vengono codificati in base64 e restituiti a sacer per essere salvati su ARO_MARCA_COMP e ARO_FIRMA_COMP.
Prima versione (non definitiva).
