<img src="src/docs/spring-boot.png" width="300">
<br/><br/>

# Verifica Firma EIDAS 

Progetto relativo al ws per la validazione delle firme EIDAS. <br/><br/>
Basato sulla versione <b>5.11.1</b> del progetto DSS (vedi <https://github.com/esig/dss>).


## Esecuzione applicazione 

Basandosi su spring boot, il seguente progetto è dotato di una sorta di "launcher" ossia, una semplice classe Java con main che ne permette l'esecuzione. Inoltre il progetto permette una gestione "profilata" delle configurazioni con il quale eseguire in locale l'applicazione, come previsto dalla dinamiche di spring boot stesso:
- default : è il profilo "standard" di spring boot quello con cui normalmente viene eseguito il processo applicativo; 
- h2: profilo legato al db h2 (è quello di **riferimento**) del progetto, con db in **memoria** (vedi [application-h2.yaml](src/main/resource/application-h2.yaml));
- oracle: profilo legato al db oracle (vedi [application-oracle.yaml](src/main/resource/application-oracle.yaml)); in particolare **username** e **password** dovranno essere fornite allo start dell'applicazione in una delle possibili modalità previste vedi https://www.baeldung.com/spring-boot-command-line-arguments; nel caso specifico, attraverso l'IDE utilizzato con la modalità ```-Ddbusername=user -Ddbpassword=password```
- 
Le configurazioni sono legate ai file yaml che sono gestiti come previsto dai meccanismi di overrinding messi a disposizione, vedi https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config. 


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

## Docker build

Per effettuare una build del progetto via Docker è stato predisposto lo standard [Dockerfile](Dockerfile) e una directory [docker_build](docker_build) con all'interno i file da integrare all'immagine base <strong>registry.access.redhat.com/ubi8/openjdk-11</strong>.
La directory [docker_build](docker_build) è strutturata come segue: 
```bash
|____README.md
|____certs
| |____README.md

```
al fine di integrare certificati non presenti di default nell'immagine principale è stata introdotta la sotto-directory [docker_build/certs](docker_build/certs) in cui dovranno essere inseriti gli appositi certificati che verranno "trustati" in fase di build dell'immagine.
La compilazione dell'immagine può essere eseguita con il comando: 
```bash
docker build -t <registry> -f ./Dockerfile --build-arg EXTRA_CA_CERTS_DIR=docker_build/certs .
```
### Openshift template

Per la creazione dell'applicazione con risorse necessarie correlate sotto Openshift (https://www.redhat.com/it/technologies/cloud-computing/openshift) viene fornito un apposito template (la solzuzione, modificabile, è basata su Oracle DB) [template](src/main/openshift/verifica-firma-eidas-template.yml).

### Note importanti

DSS >5.5 : introdotte, a partire da questa versione, una serie di "ri-modulazioni" / "re-factoring" del codice.
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

## Rilascio su Openshift

Vedere guida per il rilascio [OKD.md](OKD.md).

## Installazione applicazione come servizio/demone

Vedere guida all'installazione [INSTALL.md](INSTALL.md).

# Appendice

## Spring Boot 3.x

Alcuni riferimenti:

- Migrazione Spring boot versione 3 https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide
