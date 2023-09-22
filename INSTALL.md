# Installazione

Di seguito si riportano i passaggi operativi al fine di installare il microservizio come processo / demone.
Oltre che rilasciabile su architettura Openshift, l'applicazione può essere gestita come processo / demone all'interno di un apposito server.

Esempio di installazione [eidas.zip](src/standalone/eidas.zip).

**Nota:** all'interno del file mancante il jar eseguibile (vedi sotto con dettagli su struttura directory e generazione del jar).

### Unix con systemd

Si riportano i macro titoli con le attività da svolgere per installazione e avvio dell'applicazione come demone

##### Creare directory e relativa sotto-struttura in cui verranno depositati i file applicativi 

Si riporta un esempio in cui la "radice" è **/opt/eidas**

```
|____bin
| |____env.conf
| |____eidas
|____db
|____verifica-firma-eidas-web.jar
|____config
| |____logback-spring.xml
| |____application.yaml
|____template
| |____eidas.service
|____logs
```

* verifica-firma-eidas-web.jar : jar eseguibile 
* bin : contiene il bash script per effettuare lo start dell'applicativo
* template : file service da editare con i dati mancanti (user = utente creato in precedenza e directory radice dell'installazione, e.g. /opt/eidas)
* config : file di configurazione dell'applicativo (application.yaml) e la configurazione di logback (logback-spring.xml) per i log su file con rolling giornaliero
* logs : log applicativi
* db : l'applicazione utilizza H2 configurato su file (vedi application.yaml), questa directory ospita il DB H2 configurato

Nota : è possibile generare il jar attraverso una maven build con apposito profilo : 

```sh
mvn clean install -DskipTests -Pfatjar
```
Il jar da depositore sotto la direcotory precedentemente creata, lo si può trovare sotto verifica-firma-eidas-web/target/.

##### Creare, se già non esiste, un apposito utente/gruppo con diritti di scrittura/lettura/esecuzione su directory/sottodirectory e relativi file, precedentemente creata:

Esempio

```sh
useradd verificafirma 
chown -R verificafirma /opt/eidas
```
In questo modo solo l'utente creato potra accedere alla directory applicativa.
Inoltre è necessario impostare lo script bash per l'esecuzione.

Esempio

```sh
chmod  u+x /opt/eidas/bin/eidas
```
Di seguito il contenuto della direcotry bin.

**env.conf**
```sh 
# launcher environmet 

APP_BINARY="verifica-firma-eidas-web.jar"
APP_JAVA_OPTS="-Xmx2048m"
APP_LOGGING="-Dlogging.config=config/logback-spring.xml -Dlogging.path=logs"
APP_CONFIG="-Dspring.config.location=file:config/application.yaml,classpath:application.yaml"

```

**eidas**
```sh 
#!/bin/bash

# env 
source bin/env.conf
# laucher 
/usr/bin/java $APP_JAVA_OPTS -jar $APP_BINARY $APP_LOGGING $APP_CONFIG
```

#### Creare file sotto /etc/systemd/system

Al fine di controllare il processo applicativo mediante systemd, dovrà essere creato l'apposito file service.
Nella directory template è presente il file .service di esempio:

```sh
# /etc/systemd/system/eidas.service

[Unit]
Description=Verifica firma EIDAS
After=syslog.target network.target
	 
[Service]
Type=simple
User=<utente con cui viene eseguito il processo>
Restart=on-failure
RestartSec=3s

WorkingDirectory=<directory completa con installazione eidas>

ExecStart=<directory completa con installazione eidas>/bin/eidas 
SuccessExitStatus=143 

[Install] 
WantedBy=multi-user.target
```
Nello specifico, sono da indicare l'utente applicativo e la working directory, ossia, il path completo della directory in cui sono stati riversati i file applicativi.
Si riporta un esempio completo: 
```sh
# /etc/systemd/system/eidas.service

[Unit]
Description=Verifica firma EIDAS
After=syslog.target network.target
	 
[Service]
Type=simple
User=verificafirma
Restart=on-failure
RestartSec=3s

WorkingDirectory=/opt/eidas

ExecStart=/opt/eidas/bin/eidas 
SuccessExitStatus=143 

[Install] 
WantedBy=multi-user.target
```
Al fine di rendere effettiva l'installazione del nuovo servizio è necessario eseguire il seguente comando: 
```sh
systemctl daemon-reload
```
L'applicazione è quasi pronta per essere eseguita.

Ultimo passagio riguarda le configurazioni applicative sulla base dell'architettura/necessità.
Si riporta la configurazione consigliata (base): 

**application.yaml**
```yaml
server:
  port: 8090 ## si può indicare una qualunque porta
  
# LOGGING
logging: 
  file:
    name: logs/eidas.log
    max-size: 50MB
    max-history: 10
  pattern:
    console: ""
    file: "%d %-5p [%c] [%t] [%X{uuid}] %m %n"
  level:
    root: INFO
    eu.europa.esig.dss: INFO
    org.springframework: INFO
    org.apache: INFO
    it.eng.parer.eidas: INFO
    org.hibernate: INFO
    com.sun.xml.bind: INFO
    javax.xml.bind: INFO
    com.zaxxer.hikari.HikariConfig: OFF
    com.fasterxml.jackson: DEBUG
    springfox.documentation: OFF
     
spring:
  profiles:
    active: dameon
  security: 
    user:
     name: test
     password: test
     roles: ADMIN
 # H2
  datasource:
    url: jdbc:h2:file:./db/eidasdb;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE 
    driver-class-name: org.h2.Driver
    username: sa
    password: password
    hikari:
      #idle-timeout: 10000
      maximum-pool-size: 20
      #minimum-idle: 5
      pool-name: ParerEidasHikariPool
    #data: file:/<path>/data.sql (caricamento dati di default allo startup)
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    show_sql: false
    hibernate:
      max_fetch_depth: 3
      ddl-auto: update
      hbm2ddl:
        auto: update 
  h2:
    console:
      enabled: true    
      path: /admin/h2-console
  main:
    allow-bean-definition-overriding: true
     
```
**logback-spring.xml**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    <property name="LOG_FILE" value="${LOG_FILE:-${LOG_PATH:-${LOG_TEMP:-${java.io.tmpdir:-/tmp}}/}eidas.log}"/>

    <appender name="ROLLING-FILE"
              class="ch.qos.logback.core.rolling.RollingFileAppender">
        <encoder>
            <pattern>${FILE_LOG_PATTERN}</pattern>
        </encoder>
        <file>${LOG_FILE}</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <!-- daily rollover -->
            <fileNamePattern>${LOG_FILE}.%d{yyyy-MM-dd}.log</fileNamePattern>
        </rollingPolicy>
    </appender>

    <root level="INFO">
        <appender-ref ref="ROLLING-FILE"/>
    </root>

</configuration>
```


Questo era l'ultimo passaggio per rendere operativo l'applicativo.
Per un primo test è sufficiente eseguire il comando:

```sh
systemctl start eidas
```
Per verificarne lo stato: 
```sh
systemctl status eidas

● eidas.service - Verifica firma EIDAS
   Loaded: loaded (/etc/systemd/system/eidas.service; disabled; vendor preset: enabled)
   Active: active (running) since Fri 2020-07-10 12:07:51 CEST; 4s ago
 Main PID: 14206 (java)
    Tasks: 26 (limit: 4915)
   CGroup: /system.slice/eidas.service
           └─14206 /usr/bin/java -jar verifica-firma-eidas-web.jar -Dlogging.config=/opt/eidas/config/logback-spring.xml -Dlogging.path=/opt/eidas/logs -Xmx2048m -Dspring.config.location=file:///opt/eidas/config/application.yaml,classpa

lug 10 12:07:51 kubuhp systemd[1]: Started Verifica firma EIDAS.
lug 10 12:07:53 kubuhp java[14206]: LOGBACK: No context given for c.q.l.core.rolling.SizeAndTimeBasedRollingPolicy@364604394
lug 10 12:07:53 kubuhp java[14206]: ================================================================
lug 10 12:07:53 kubuhp java[14206]: @EIDAS v.1.1.2-SNAPSHOT
lug 10 12:07:53 kubuhp java[14206]: ================================================================
```
Per lo stop del processo: 
```sh
systemctl stop eidas
```
Si consiglia inoltre di impostare il processo al boot del server: 
```sh
systemctl enable eidas
```
Il microservizio di verifica firma EIDAS è quindi operativo e contattabile all'indirizzo : http://localhost:8090, host e porta secondo l'esempio completo di configurazione sopra riportata.

# Appendice

Riferimento / Guida : https://www.baeldung.com/spring-boot-app-as-a-service
