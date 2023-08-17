<img src="https://spring.io/images/spring-logo-9146a4d3298760c2e7e49595184e1975.svg" width="300">

# Verifica Firma EIDAS 

Progetto relativo al ws per la validazione delle firme EIDAS. <br/><br/>
Basato sulla versione <b>5.9</b> del progetto DSS (vedi <https://github.com/esig/dss>).

## Configurazione settings.xml 

Al fine di compilare il progetto in locale, è necessaria la presenza del software [Maven](https://maven.apache.org/) e la seguente configurazione su settings.xml: 

```xml
<server>
	<id>github</id>
	<username>USERNAME</username>
	<password>ACCESS_TOKEN</password>
</server>
```
per ulteriori dettagi conlsutare la documentazione ufficiale su [GitHub](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry#authenticating-with-a-personal-access-token).

## Esecuzione applicazione 

Basandosi su spring boot, il seguente progetto è dotato di una sorta di "launcher" ossia, una semplice classe Java con main che ne permette l'esecuzione. Inoltre il progetto permette una gestione "profilata" delle configurazioni con il quale eseguire in locale l'applicazione, come previsto dalla dinamiche di spring boot stesso:
- default : è il profilo "standard" di spring boot quello con cui normalmente viene eseguito il processo applicativo; 
- h2: profilo legato al db h2 (è quello di **riferimento**) del progetto, con db in **memoria** (vedi [application-h2.yaml](src/main/resource/application-h2.yaml));
- oracle: profilo legato al db oracle (vedi [application-oracle.yaml](src/main/resource/application-oracle.yaml)); in particolare **username** e **password** dovranno essere fornite allo start dell'applicazione in una delle possibili modalità previste vedi https://www.baeldung.com/spring-boot-command-line-arguments; nel caso specifico, attraverso l'IDE utilizzato con la modalità ```-Ddbusername=user -Ddbpassword=password```
- 
Le configurazioni sono legate ai file yaml che sono gestiti come previsto dai meccanismi di overrinding messi a disposizione (vedi https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config). 


### Override di configurazioni

Come da documentazione:

```java
$ java -jar myproject.jar --spring.config.location=\
    optional:classpath:/default.properties,\
    optional:classpath:/override.properties
```

solo per sottolineare che questo meccanismo necessità di un ordine ben preciso, in sostanza, all'estrema destra la configurazione "overrida" quelle interne.

## Console amministratori

Presente pagina per amministratori /admin.

### Ambiente di sviluppo / localhost

- User: admin
- Password: admin
### Openshift

Su Openshift sono stati configurati tre diversi tipo di deploy, questo al fine di distinguere l'applicazione su tre diversi "ambienti" : Sviluppo / Test e PreProduzione. Le credenziali vengono generate attraverso la craezione della nuova applicazione su singolo namespace, per mezzo di un [template](https://gitlab.ente.regione.emr.it/parer/okd/verificafirma-eidas-config/-/blob/a86dba60c63eda26b927a8cd4fc7c337e4af3319/verifica-firma-eidas-template.yml) standard (verifica su secrets).

### Note importanti

Attualmente il progetto è stato "riadattato" alla versione 5.5 delle librerie DSS in cui ci sono stati una serie di "ri-modulazioni" / "re-factoring" del codice.
In particolare: 
 - Validatori : adesso utilizzano il pattern Factory (da capire se i validatori "custom" sono ancora utili - vedi armored ASCII / TSD / TSR)
 - Modello DTO restituito su risposta : attualmente l'Objcet mapper (jackson) non riesce a rimappare il report EIDAS (vedi WSReportsDTO) in quanto è stata introdotta un'assocazione tra oggetti non presente nella 5.4 ossia 
   vedi : https://github.com/esig/dss/blob/5.4.3/dss-diagnostic-jaxb/src/main/java/eu/europa/esig/dss/jaxb/diagnostic/XmlTrustedService.java in questo oggetto nella nuova versione è presente il seguente codice:
    
    ```
    @XmlIDREF
    @XmlSchemaType(name = "IDREF")
    protected XmlCertificate serviceDigitalIdentifier 
    ```
   
   che genera un errore del tipo <b>"Could not write JSON: Infinite recursion (StackOverflowError)"</b>. 
   Da capire COME intervenire in quanto non è chiaramente possibile modificare il modello dati di EIDAS (probabilmente sui loro servizi REST la soluzione c'è) 
   <br/><br/><b>AGGIORNAMENTO</b> 
   <br/> Il problema è stato risolto con due diversi interventi: 
    - Mapper jackson : introdocendo un introspector Jaxb 
    - Output servizio rest : da Json ad Xml per evitare il problema sopra citat (trattandosi di un Jaxb è la sua naturale rappresentazione) 


Utilizza spring boot 2.X ed espone i seguenti endpoint : 

1.  /v1/report-verifica (accesso json)
2.  /v2/report-verifica (accetta multipart/form-data)
3.  /v1/errors
4.  /v1/errors/{code}

Il servizio REST accetta un input JSON e restituisce: 

1. OK : report in formato XML 
2. KO :  dettaglio errore in formato JSON
#####NOTA: distro Linux necessita di installazione del seguente pacchetto 
Comando : 

```
sudo apt-get install libtcnative-1
```

## Rilascio su Openshift

Vedere guida per il rilascio [OKD.md](OKD.md).

## Installazione applicazione come servizio/demone

Vedere guida all'installazione [INSTALL.md](INSTALL.md).

# Appendice

## Passaggio a JDK 11 

L'applicazione è basata sulla versione 11 di Java, nata con la versione 8. In fase di migrazione, sono stati effettuati i seguenti passaggi: 

1. exclude delle dipendenze xml-apis che riferiscono a moduli noti di java e che quindi non devono essere "importate" da moduli di terze parti
## Spring Boot

Alcuni riferimenti:

- Configurazioni server / altro  https://docs.spring.io/spring-boot/docs/2.3.0.RELEASE/reference/htmlsingle/#common-application-properties
