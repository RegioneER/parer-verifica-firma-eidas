# Guida per il rilascio su Openshift

Si riportano i macro-passaggi utili al rilascio del microservizio su architettura Openshift.

## Creazione della config map con le configurazioni (yaml) dell'applicazione: 

```
 oc create configmap verifica-firma-eidas-config --from-file=application.yaml
 ```
 
Si riporta un esempio di application.yaml (sviluppo): 
```
server:
  port: 8443
  ssl:
    key-store-type: JKS
    key-store: file:/etc/verifica-firma-eidas/keystore/keystore.jks
    key-store-password: ${KEYSTORE_PWD}
    key-alias: parerapps
    require-ssl: true

spring:
  profiles:
    active: svil
  security: 
    user:
     name: ${ADMIN_USER}
     password: ${ADMIN_PWD}
     roles: ADMIN
  datasource:
    url: jdbc:h2:file:/deployments/eidasdb
    driver-class-name: org.h2.Driver
    username: ${H2_USER}
    password: ${H2_PWD}
    platform: h2
    #data: file:/<path>/data.sql
  jpa:
    database-platform: org.hibernate.dialect.H2Dialect
    show_sql: true
    hibernate:
      max_fetch_depth: 3
      ddl-auto: update
      hbm2ddl:
        auto: update
  h2:
    console:
      enabled: true
      path: /admin/h2-console
      settings:
        web-allow-others: true
  main:
    allow-bean-definition-overriding: true

# LOGGING
logging: 
  pattern:
    console: "%d %-5p [%c] [%t] [%X{uuid}] %m %n"
  level:
    root: INFO
    eu.europa.esig.dss: INFO
    org.springframework: INFO
    org.apache: INFO
    it.eng.parer.eidas: INFO
    org.hibernate: INFO
    com.zaxxer: INFO
    com.sun.xml.bind: INFO
    javax.xml.bind: INFO

# BEAN
it:
  eng:
    parer:
      core:
        viewhelper:
          propstoskip: .*H2_USER.*|.*H2_PWD.*|.*ADMIN_USER.*|.*ADMIN_PWD.*|.*password.*|.*email.*
      eidas:
        core:
          validation:
            writefile: false
```

## Creazione secrets per la gestione delle user / password: 

```
oc create -f <secrets_file>.yaml
```
Si riporta un esempio di secrets "opaco" con chiave / valore necessari all'applicazione: 

```
apiVersion: v1
data:
  admin-password: <value_pwd>
  admin-user: <value_user>
  database-password: <value_pwd>
  database-user: <value_iser>
  keystore-pwd: <value_pwd>
kind: Secret
metadata:
  name: <name> (e.g. verifica-firma-eidas-secrets-svil)
  namespace: <namespace> (e.g. parer)
type: Opaque

```

## Creazione service / route

```
oc create -f <svc_file>.yaml
```

Esempio di service yaml.

```
apiVersion: v1
kind: Service
metadata:
  name: <name> (e.g. verifica-firma-eidas-web-svil)
  namespace: <namespace> (e.g. parer)
spec:
  ports:
    - name: https
      port: 443
      protocol: TCP
      targetPort: 8443
  selector:
    ambiente: <env> (e.g. sviluppo)
    app: verifica-firma-eidas-web
```


Da notare che, il tipo di protocollo utilizzato è HTTPS (subito sotto l'esempio di creazione del secret contenente il keystore utilizzato dall'appliazione - vedi file di configurazione sopra). 
Altra nota impotante, il TLS Termination è stato impostato come re-encrypt, ciò avvolora la necessità di configurare su Openshift un keystore con il quale, 
si effettua una cifratura sul canale utilizzato per ricevere e inviare chiamate.

Riferimenti : 
* https://docs.openshift.com/container-platform/3.7/architecture/networking/routes.html#route-types
* https://docs.openshift.com/container-platform/3.7/architecture/networking/routes.html#re-encryption-termination

Esempio di rotta yaml.

```
apiVersion: route.openshift.io/v1
kind: Route
metadata:
  name: <name> (e.g. verifica-firma-eidas-web-svil)
  namespace: <namespace> (e.g. parer)
spec:
  host: <hostname> (e.g. verifica-firma-eidas-svil.parer-apps.ente.regione.emr.it)
  port:
    targetPort: https
  tls:
    destinationCACertificate: <ascii armor> (e.g."-----BEGIN CERTIFICATE-----\n-----END CERTIFICATE-----\r\n")
    termination: reencrypt
  to:
    kind: Service
    name: <name> (e.g. verifica-firma-eidas-web-svil)
```
## Creazione secrets con keystore

Viene creato un secret al fine di mascherare il contenuto del file keystore.jsk utilizzato come mountpoint sul deployment config che ospiterà l'applicazione verifica-firma-eidas-web da rilacare.

Di seguito un esempio.

```
apiVersion: v1
data:
  keystore.jks: >-
    <base64 keystore>
kind: Secret
metadata:
  name: <name> (e.g. parerapps-keystore)
  namespace: <namespace> (e.g. parer)
type: Opaque
```

Una volta creato il secret verrà configurato come mountpoint, in modo tale che l'applicazione abbia accesso al file keystore.jks.

Esempio di mountpoint:

```
Mount: parerapps-keystore-ywvze → /etc/verifica-firma-eidas/keystore read-only 
```
Chiaramente tale mountpoint sarà configurabile ed editabile direttamente alla creazione dell'applicazione e relativo deployment config, i requisiti sono: 
* creazione del secret che continee di fatto, il file jks
* configurazione delle rotte 
* configurazione del path/file nell'application yaml 

## Deploy dell'applicazione su Openshift

Il passaggio finale è, naturalmente, quello del primo deploy/creazione dell'applicazione su Openshift.


Vedi : <https://www.baeldung.com/spring-boot-deploy-openshift>

Per effettuare il <b>deploy</b> eseguire il seguente comando: 

```
mvn clean install fabric8:deploy -P openshift
```

In alternativa si può direttamente creare l'applicazione via Dashboard ma l'utilizzo del plugin fabric8 ha come vantaggio quello di creare una prima "installazione" su cui poi sarà possibile effettuare 
gli edit opportuni.

Nello specifico, esistono due mountpoint:

* Mount: verifica-firma-eidas-config → /etc/verifica-firma-eidas/config read-only 
* Mount: parerapps-keystore-ywvze → /etc/verifica-firma-eidas/keystore read-only

Il primo contenente la configmap con le configurazioni e il secondo con il keystore.

Sono inoltre configurati i due probe per liveness e readiness:

* Readiness Probe: GET /actuator/health on port 8443 (HTTPS) 60s delay, 1s timeout
* Liveness Probe: GET /actuator/health on port 8443 (HTTPS) 180s delay, 1s timeout
 
# Conclusioni

Sulla base delle rotte impostate, l'applicazione sarà raggiungibile su indirizzo del tipo https://verifica-firma-eidas-svil.parer-apps.ente.regione.emr.it/.

