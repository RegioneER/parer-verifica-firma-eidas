<img src="src/docs/spring-boot.png" width="300">
<br/><br/>

# Verifica Firma EIDAS 

Fonte template redazione documento:  https://www.makeareadme.com/.


# Descrizione

Microservizio realizzato per effettuare verifica e validazione di documenti con firma digitale. <br/>
Realizzato attraverso framework [Spring Boot](https://spring.io/projects/spring-boot) (versione 3.x) e [OpenJDK 17](https://openjdk.org/projects/jdk/17/), utilizza la versione <b>5.12.1</b> del progetto [DSS](https://ec.europa.eu/digital-building-blocks/wikis/display/DIGITAL/Digital+Signature+Service+-++DSS).

# Installazione

Di seguito verranno riportati sotto alcuni paragrafi, le modalità possibili con cui è possibile rendere operativo il microservizio. 
## Rilascio su RedHat Openshift

Vedere specifica guida per il rilascio [OKD.md](OKD.md).

### Openshift template

Per la creazione dell'applicazione con risorse necessarie correlate sotto Openshift (https://www.redhat.com/it/technologies/cloud-computing/openshift) viene fornito un apposito template (la solzuzione, modificabile, è basata su Oracle DB) [template](src/main/openshift/verifica-firma-eidas-template.yml).

## Installazione applicazione come servizio/demone

Vedere guida all'installazione [INSTALL.md](INSTALL.md).

# Utilizzo

Basandosi su spring boot, il seguente progetto è dotato di una sorta di "launcher" ossia, una semplice classe Java con main che ne permette l'esecuzione. Inoltre il progetto permette una gestione "profilata" delle configurazioni con il quale eseguire in locale l'applicazione, come previsto dalla dinamiche di spring boot stesso:
- default : è il profilo "standard" di spring boot quello con cui normalmente viene eseguito il processo applicativo; 
- h2: profilo legato al db h2 (è quello di **riferimento**) del progetto, con db in **memoria** (vedi [application-h2.yaml](src/main/resource/application-h2.yaml));
- oracle: profilo legato al db oracle (vedi [application-oracle.yaml](src/main/resource/application-oracle.yaml)); in particolare **username** e **password** dovranno essere fornite allo start dell'applicazione in una delle possibili modalità previste vedi https://www.baeldung.com/spring-boot-command-line-arguments; nel caso specifico, attraverso l'IDE utilizzato con la modalità ```-Ddbusername=user -Ddbpassword=password```

Le configurazioni sono legate ai file yaml che sono gestiti come previsto dai meccanismi di overrinding messi a disposizione, vedi https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config. 

### Esempio override configurazioni

Nel seguente esempio, si riporta una casistica di esecuzione dell'applicazione (attraverso apposito jar) con override delle configurazioni base:

```java
$ java -jar myproject.jar --spring.config.location=\
    optional:classpath:/default.properties,\
    optional:classpath:/override.properties
```
Nota: nell'esempio sopra riportato, si utilizzano file di tipo properties (default previsto da spring boot), mentre in questo caso si è scelto lo standard YAML, non cambiano le dinamiche descritte ma semplicemente l'estenzione (.yaml).

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
## Note aggiornamenti librerie DSS

Versione libreria DSS **> 5.5** : introdotte, a partire da questa versione, una serie di "ri-modulazioni" / "re-factoring" del codice.
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


# Requisiti e librerie utilizzate

Requisiti minimi per installazione: 

- Sistema operativo : consigliato Linux server (in alternativa compatibilità con Windows server)
- Java versione 17 (OpenJDK / Oracle)
- Kubernetes / Docker : se rilasciato attraverso container oppure si esegue una build del progetto attraverso il profilo maven **uber-jar** per ottenere il JAR eseguibile (vedi paragrafi precendeti)

## Librerie utilizzate

|  GroupId | ArtifactId  | Version  | Type   |  Licenses |
|---|---|---|---|---|
|com.fasterxml.jackson.module|jackson-module-jaxb-annotations|2.15.1|jar|The Apache Software License, Version 2.0
|com.h2database|h2|2.1.214|jar|MPL 2.0EPL 1.0
|com.oracle.database.jdbc|ojdbc11|21.7.0.0|jar|Oracle Free Use Terms and Conditions (FUTC)
|eu.europa.ec.joinup.sd-dss|dss-cades|5.12.1|jar|GNU Lesser General Public License
|eu.europa.ec.joinup.sd-dss|dss-crl-parser-stream|5.12.1|jar|GNU Lesser General Public License
|eu.europa.ec.joinup.sd-dss|dss-crl-parser-x509crl|5.12.1|jar|GNU Lesser General Public License
|eu.europa.ec.joinup.sd-dss|dss-document|5.12.1|jar|GNU Lesser General Public License
|eu.europa.ec.joinup.sd-dss|dss-model|5.12.1|jar|GNU Lesser General Public License
|eu.europa.ec.joinup.sd-dss|dss-pades|5.12.1|jar|GNU Lesser General Public License
|eu.europa.ec.joinup.sd-dss|dss-pades-pdfbox|5.12.1|jar|GNU Lesser General Public License
|eu.europa.ec.joinup.sd-dss|dss-server-signing-rest|5.12.1|jar|GNU Lesser General Public License
|eu.europa.ec.joinup.sd-dss|dss-service|5.12.1|jar|GNU Lesser General Public License
|eu.europa.ec.joinup.sd-dss|dss-signature-rest|5.12.1|jar|GNU Lesser General Public License
|eu.europa.ec.joinup.sd-dss|dss-spi|5.12.1|jar|GNU Lesser General Public License
|eu.europa.ec.joinup.sd-dss|dss-token|5.12.1|jar|GNU Lesser General Public License
|eu.europa.ec.joinup.sd-dss|dss-tsl-validation|5.12.1|jar|GNU Lesser General Public License
|eu.europa.ec.joinup.sd-dss|dss-utils-apache-commons|5.12.1|jar|GNU Lesser General Public License
|eu.europa.ec.joinup.sd-dss|dss-validation-dto|5.12.1|jar|GNU Lesser General Public License
|eu.europa.ec.joinup.sd-dss|dss-validation-rest|5.12.1|jar|GNU Lesser General Public License
|eu.europa.ec.joinup.sd-dss|dss-validation-soap|5.12.1|jar|GNU Lesser General Public License
|eu.europa.ec.joinup.sd-dss|dss-xades|5.12.1|jar|GNU Lesser General Public License
|it.eng.parer|verificafirma-eidas-beans|1.7.0|jar|-
|javax.xml.bind|jaxb-api|2.3.1|jar|CDDL 1.1GPL2 w/ CPE
|net.logstash.logback|logstash-logback-encoder|7.2|jar|Apache License, Version 2.0MIT License
|org.apache.taglibs|taglibs-standard-jstlel|1.2.5|jar|The Apache Software License, Version 2.0
|org.apache.tika|tika-core|2.8.0|jar|Apache-2.0
|org.apache.tika|tika-parsers-standard-package|2.8.0|jar|Apache-2.0
|org.apache.xmlgraphics|fop|2.7|jar|The Apache Software License, Version 2.0
|org.springdoc|springdoc-openapi-starter-webmvc-ui|2.1.0|jar|The Apache License, Version 2.0
|org.springframework.boot|spring-boot-starter-actuator|3.0.9|jar|Apache License, Version 2.0
|org.springframework.boot|spring-boot-starter-data-jpa|3.0.9|jar|Apache License, Version 2.0
|org.springframework.boot|spring-boot-starter-security|3.0.9|jar|Apache License, Version 2.0
|org.springframework.boot|spring-boot-starter-thymeleaf|3.0.9|jar|Apache License, Version 2.0
|org.springframework.boot|spring-boot-starter-validation|3.0.9|jar|Apache License, Version 2.0
|org.springframework.boot|spring-boot-starter-web|3.0.9|jar|Apache License, Version 2.0
|org.springframework.boot|spring-boot-starter-webflux|3.0.9|jar|Apache License, Version 2.0
|org.springframework.ws|spring-ws-core|4.0.5|jar|Apache License, Version 2.0
|org.thymeleaf.extras|thymeleaf-extras-springsecurity6|3.1.1.RELEASE|jar|The Apache Software License, Version 2.0
|org.webjars|bootstrap|4.6.1|jar|Apache License, Version 2.0
|org.webjars|font-awesome|6.4.0|jar|CC BY 3.0
|org.webjars|highlightjs|11.5.0|jar|BSD
|org.webjars|jquery|3.6.4|jar|MIT License
|org.webjars|jquery-ui|1.13.2|jar|MIT License
|org.webjars.npm|highlightjs-badgejs|0.0.5|jar|MIT
|org.webjars.npm|popper.js|1.16.0|jar|MIT

# Supporto

Mantainer del progetto è [Engineering Ingegneria Informatica S.p.A.](https://www.eng.it/).

# Contributi

Se interessati a crontribuire alla crescita del progetto potete scrivere all'indirizzo email <a href="mailto:areasviluppoparer@regione.emilia-romagna.it">areasviluppoparer@regione.emilia-romagna.it</a>.

# Credits

Progetto di proprietà di [Regione Emilia-Romagna](https://www.regione.emilia-romagna.it/) sviluppato a cura di [Engineering Ingegneria Informatica S.p.A.](https://www.eng.it/).

# Licenza

Questo progetto è rilasciato sotto licenza GNU Affero General Public License v3.0 or later ([LICENSE.txt](LICENSE.txt)).

# Appendice

## Spring Boot 3.x

Alcuni riferimenti:

- Migrazione Spring boot versione 3 https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-3.0-Migration-Guide
