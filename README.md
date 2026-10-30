<p align="center">
<img src="https://www.link.it/wp-content/uploads/2025/01/logo-govpay.svg" alt="GovPay Logo" width="200"/>
</p>

# GovPay - Porta di accesso al sistema pagoPA - AppIO Batch

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=link-it_govpay-appio-batch&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=link-it_govpay-appio-batch)
[![Docker Hub](https://img.shields.io/docker/v/linkitaly/govpay-appio-batch?label=Docker%20Hub&logo=docker)](https://hub.docker.com/r/linkitaly/govpay-appio-batch)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://raw.githubusercontent.com/link-it/govpay-appio-batch/main/LICENSE)
[![Spring Boot](https://img.shields.io/badge/dynamic/xml?url=https%3A%2F%2Fraw.githubusercontent.com%2Flink-it%2Fgovpay-bom%2Fmain%2Fpom.xml&query=%2F%2F*%5Blocal-name()%3D'spring-boot.version'%5D&label=Spring%20Boot&color=brightgreen)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)

Batch Spring Boot che invierà, tramite App IO, i messaggi e gli avvisi di pagamento delle pendenze gestite da GovPay.

## Sommario

> **Progetto in fase di avvio.** Su `main` c'è lo scheletro dell'applicazione: nessun job Spring Batch, nessuna integrazione con App IO e nessuna API REST del batch. Il porting della spedizione dei promemoria via App IO dal core GovPay è tracciato in [#3](https://github.com/link-it/govpay-appio-batch/issues/3). Questo README descrive cosa fa oggi il codice; le sezioni vanno aggiornate quando arrivano i job.

Oggi il batch:

- si avvia come applicazione Spring Boot con il datasource del database di GovPay e il `JobRepository` JDBC di Spring Batch configurato da `govpay-common` (le tabelle dei metadati non vengono lette finché non c'è un job);
- espone gli endpoint actuator (`health`, `info`, `metrics`, `prometheus`);
- applica i livelli di log impostati dalla console nella tabella `configurazione` (riga `log_level`), senza riavvio;
- aggiunge transaction id e correlation id alle righe di log e agli header delle risposte HTTP (tracciatura di `govpay-common`).

Sono già pronti, allineati agli altri batch GovPay (es. [govpay-notify-batch](https://github.com/link-it/govpay-notify-batch)): la pipeline di validazione e rilascio, l'immagine Docker con l'inizializzazione dello schema Spring Batch e il pacchetto `sql.zip`.

Storico delle modifiche nel [ChangeLog](ChangeLog).

## Integrazione con GovPay

| Componente | Quando serve | Obbligatorio | Ruolo |
|------------|--------------|--------------|-------|
| Database di GovPay | runtime | sì | Datasource dell'applicazione, necessario all'avvio e controllato dall'health. Oggi il batch legge solo la riga `log_level` della tabella `configurazione` (e `connettori` se `govpay.client.cache.enabled=true`); non scrive tabelle applicative |
| Tabelle Spring Batch | runtime | no, finché non ci sono job | Metadati delle esecuzioni (`batch_job_*`, `batch_step_*`) su cui è configurato il `JobRepository`. Lo schema è nel pacchetto `sql.zip` (vedi [Compilazione](#compilazione)) e l'immagine Docker può crearlo all'avvio |
| [govpay-bom](https://github.com/link-it/govpay-bom) | build | sì | Parent Maven: versioni delle dipendenze e dei plugin, analisi OWASP, profilo `dist` |
| [govpay-common](https://github.com/link-it/govpay-common) | build | sì | `JobRepository` JDBC per i metadati Spring Batch, tracciatura dei log, livelli di log da database (configurazione comune caricata con lo scan di `it.govpay.common.client`); script di svecchiamento dei metadati inclusi in `sql.zip` |
| App IO | runtime | - | Destinatario previsto dei messaggi: integrazione non ancora implementata |
| [govpay-gde-api](https://github.com/link-it/govpay-gde-api) | runtime | - | Giornale degli Eventi: integrazione non ancora implementata. Negli altri batch si configura con il connettore `govpay_gde_api` di GovPay, non con variabili d'ambiente |

## Compilazione

Requisiti: JDK 21 e Maven 3.8.1 o successivo.

```bash
mvn clean install
```

Il risultato è il fat jar `target/govpay-appio-batch.jar`. La build esegue i test e l'analisi OWASP Dependency-Check delle dipendenze (esecuzione `check-owasp` di `govpay-bom`); per escludere l'analisi OWASP, per esempio in sviluppo:

```bash
mvn clean install -Dowasp.phase=none
```

Per produrre anche `target/sql.zip`, con lo schema Spring Batch per ogni database e gli script di svecchiamento dei metadati, usato dall'immagine Docker e allegato alle release:

```bash
mvn clean install -Pjar,dist
```

Il profilo `jar` va indicato insieme a `dist`: è quello che lascia i driver JDBC fuori dal jar.

Per il deploy su un application server (es. Tomcat 11) si produce il war `target/govpay-appio-batch.war` con il profilo `war`:

```bash
mvn clean install -P war
```

Il war non contiene driver JDBC né il Tomcat embedded (in `WEB-INF/lib-provided`): usa il datasource dell'application server, vedi [Deploy del war](#deploy-del-war).

## Configurazione

Le proprietà si impostano in `application.properties`, da riga di comando (`-D<proprietà>=<valore>`) o con le variabili d'ambiente equivalenti di Spring Boot (es. `govpay.batch.cluster-id` → `GOVPAY_BATCH_CLUSTERID`, `scheduler.appioJob.fixedDelayString` → `SCHEDULER_APPIOJOB_FIXEDDELAYSTRING`). Con l'immagine Docker si usano anche le variabili descritte in [Esecuzione con Docker](#esecuzione-con-docker).

### Esecuzione e cluster

| Proprietà | Default | Descrizione |
|-----------|---------|-------------|
| `spring.batch.job.enabled` | `false` | Avvio automatico dei job da parte di Spring Boot: resta `false`, come negli altri batch GovPay |
| `govpay.batch.job-repository.jdbc.enabled` | `true` | `JobRepository` JDBC sui metadati Spring Batch (da `govpay-common`) |
| `govpay.batch.enabled` | `true` | Predisposta per il job, oggi non letta |
| `govpay.batch.cluster-id` | `GovPay-AppIO-Batch` | Identificativo del nodo, predisposto per il coordinamento tra nodi; oggi non letto |
| `govpay.batch.stale-threshold-minutes` | `120` | Minuti dopo cui un'esecuzione in corso sarà considerata bloccata; oggi non letta |
| `scheduler.appioJob.fixedDelayString` | `7200000` | Intervallo tra un'esecuzione e la successiva del job, in ms (2 ore); oggi non letta, non c'è un job da schedulare |
| `scheduler.initialDelayString` | `1` | Ritardo della prima esecuzione dopo l'avvio, in ms; oggi non letta |
| `server.port` | `8080` (`10001` nel container) | Porta HTTP degli endpoint actuator |
| `spring.application.name` | `govpay-appio-batch` | Nome dell'applicazione nei log |
| `spring.main.banner-mode` | `off` | Banner di Spring Boot all'avvio |
| `spring.jackson.default-property-inclusion` | `non_null` | Esclude i campi nulli dal JSON |
| `spring.jackson.time-zone` | `Europe/Rome` | Fuso orario delle date nel JSON |

### Database

| Proprietà | Default | Descrizione |
|-----------|---------|-------------|
| `spring.datasource.url` | - | URL JDBC del database di GovPay (obbligatoria, salvo con `spring.datasource.jndi-name`) |
| `spring.datasource.jndi-name` | - | Nome JNDI del datasource dell'application server, per il war (es. `java:comp/env/jdbc/govpay`); sostituisce URL, utente e password |
| `spring.datasource.username` | - | Utente del database |
| `spring.datasource.password` | - | Password del database |
| `spring.datasource.driver-class-name` | dedotto dall'URL | Classe del driver JDBC |
| `spring.jpa.database-platform` | dedotto dal driver | Dialetto Hibernate |
| `spring.jpa.properties.hibernate.dialect` | dedotto dal driver | Dialetto Hibernate |
| `spring.datasource.hikari.minimum-idle` | `10` (`2` nel container) | Connessioni minime inattive del pool |
| `spring.datasource.hikari.maximum-pool-size` | `10` | Connessioni massime del pool |
| `spring.datasource.hikari.connection-timeout` | `30000` (`20000` nel container) | Attesa massima di una connessione, in ms |
| `spring.datasource.hikari.idle-timeout` | `600000` (`10000` nel container) | Tempo dopo cui una connessione inattiva viene chiusa, in ms |
| `spring.datasource.hikari.max-lifetime` | `1800000` (`600000` nel container) | Durata massima di una connessione, in ms |

I driver JDBC non sono inclusi nel jar: vedi [Esecuzione](#esecuzione).

### Logging

| Proprietà | Default | Descrizione |
|-----------|---------|-------------|
| `logging.file.name` | - (solo console) | File di log; nel container `/var/log/govpay-appio/govpay-appio.log` |
| `logging.level.it.govpay` | `INFO` | Livello di log dei package GovPay |
| `logging.pattern.level` | `%5p [%X{transactionId:-},%X{correlationId:-}]` | Aggiunge a ogni riga transaction id e correlation id |
| `govpay.logging.tracciatura.enabled` | `true` | Filtro di `govpay-common` che legge o genera il correlation id delle richieste HTTP e restituisce il transaction id negli header di risposta |
| `govpay.logging.tracciatura.correlation-inbound-headers` | `X-Correlation-ID,X-Request-ID` | Header letti in ingresso per il correlation id, in ordine di precedenza |
| `govpay.logging.tracciatura.correlation-response-headers` | `X-Correlation-ID,X-Request-ID` | Header di risposta con il correlation id |
| `govpay.logging.tracciatura.transaction-response-headers` | `X-Transaction-ID,X-Govpay-IdTransazione` | Header di risposta con il transaction id |
| `govpay.logging.tracciatura.lunghezza-massima-id` | `128` | Lunghezza massima di un identificativo ricevuto; se non valido ne viene generato uno nuovo |
| `govpay.logging.tracciatura.pattern-id-valido` | `[A-Za-z0-9._:\-]+` | Espressione regolare che un identificativo ricevuto deve rispettare |
| `govpay.logging.livelli-dinamici.enabled` | `true` | Livelli di log letti dalla riga `log_level` della tabella `configurazione` (impostati dalla console) |
| `govpay.logging.livelli-dinamici.intervallo-refresh` | `60s` (variabile `LOG_LEVEL_REFRESH_INTERVAL`) | Intervallo di rilettura dei livelli dal database; `0` li legge solo all'avvio |
| `govpay.logging.livelli-dinamici.ritardo-iniziale` | `60s` | Ritardo della prima rilettura dopo l'avvio |
| `govpay.logging.livelli-dinamici.logger-gestiti[0]` | `it.govpay` | Prefissi dei logger modificabili da database; i livelli per altri logger sono ignorati con un avviso |

### Connettori e client HTTP

Componenti di `govpay-common` caricati con la configurazione comune; li useranno i job per chiamare servizi esterni.

| Proprietà | Default | Descrizione |
|-----------|---------|-------------|
| `govpay.client.cache.enabled` | `false` | Carica i connettori (tabella `connettori`) all'avvio e li tiene in cache |
| `govpay.client.async.core-pool-size` | `10` | Thread minimi per le chiamate asincrone (es. eventi al GDE) |
| `govpay.client.async.max-pool-size` | `50` | Thread massimi per le chiamate asincrone |
| `govpay.client.async.queue-capacity` | `100` | Richieste in coda in attesa di invio |
| `govpay.client.async.thread-name-prefix` | `async-http-` | Prefisso dei thread di invio |

### Monitoraggio

| Proprietà | Default | Descrizione |
|-----------|---------|-------------|
| `management.server.port` | porta del servizio | Porta dedicata agli endpoint actuator |
| `management.endpoints.web.base-path` | `/actuator` | Base path degli endpoint actuator |
| `management.endpoints.web.exposure.include` | `health,info,metrics,prometheus` (variabile `MANAGEMENT_ENDPOINTS`) | Endpoint actuator esposti |
| `management.endpoint.health.show-details` | `when_authorized` | Dettagli dell'health: il servizio non ha autenticazione, quindi restituisce solo lo stato |
| `management.health.db.enabled` | `true` | Include la connessione al database nell'health |
| `management.metrics.tags.application` | `govpay-appio-batch` | Tag che identifica il servizio nelle metriche |
| `govpay.metrics.enabled` | `false` | Metriche dei tempi delle chiamate HTTP di `govpay-common` |

## Esecuzione

### Driver JDBC

I driver JDBC **non sono inclusi** nel fat jar e vanno forniti a runtime. Creare una directory (es. `jdbc-drivers/`) e copiarvi il driver del database utilizzato (versioni gestite da `govpay-bom`):

| Database   | Driver |
|------------|--------|
| PostgreSQL | [postgresql-42.7.14.jar](https://repo1.maven.org/maven2/org/postgresql/postgresql/42.7.14/postgresql-42.7.14.jar) |
| MySQL      | [mysql-connector-j-26.10.0.jar](https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/26.10.0/mysql-connector-j-26.10.0.jar) |
| Oracle     | [ojdbc11-23.26.3.0.0.jar](https://repo1.maven.org/maven2/com/oracle/database/jdbc/ojdbc11/23.26.3.0.0/ojdbc11-23.26.3.0.0.jar) |
| SQL Server | [mssql-jdbc-13.6.1.jre11.jar](https://repo1.maven.org/maven2/com/microsoft/sqlserver/mssql-jdbc/13.6.1.jre11/mssql-jdbc-13.6.1.jre11.jar) |
| H2         | [h2-2.5.252.jar](https://repo1.maven.org/maven2/com/h2database/h2/2.5.252/h2-2.5.252.jar) |

### Schema Spring Batch

Le tabelle dei metadati Spring Batch si creano nel database di GovPay con lo script `sql/spring-batch/schema-<database>.sql` di `sql.zip`; l'immagine Docker lo esegue da sola se `GOVPAY_APPIO_POP_DB_SKIP=FALSE`. Gli script in `sql/cleanup/` cancellano i metadati delle esecuzioni vecchie. Finché il batch non ha job, le tabelle non vengono usate.

### Jar standalone

Il jar usa `PropertiesLauncher`: la proprietà `loader.path` indica la directory dei driver JDBC. Configurazione minima per PostgreSQL:

```bash
java -Dloader.path=./jdbc-drivers \
     -Dspring.datasource.url=jdbc:postgresql://localhost:5432/govpay \
     -Dspring.datasource.username=govpay \
     -Dspring.datasource.password=<password> \
     -jar target/govpay-appio-batch.jar
```

Il processo resta attivo ed espone gli endpoint actuator. Non esiste ancora un profilo `cron` con un'esecuzione singola: avviato con `--spring.main.web-application-type=none`, come fa l'immagine Docker in modalità cron, il processo parte e termina subito con exit code `0`.

### Deploy del war

Il war si copia nella directory `webapps` dell'application server e risponde sotto il context path `/govpay-appio-batch`. Il datasource si definisce nell'application server e si indica con `spring.datasource.jndi-name`; il driver JDBC va nelle librerie del server (per Tomcat `lib/`). Esempio per Tomcat, in `conf/context.xml`:

```xml
<Resource name="jdbc/govpay" auth="Container" type="javax.sql.DataSource"
          driverClassName="org.postgresql.Driver"
          url="jdbc:postgresql://localhost:5432/govpay"
          username="govpay" password="<password>" maxTotal="10"/>
```

e tra le opzioni della JVM (es. `bin/setenv.sh`):

```bash
CATALINA_OPTS="-Dspring.datasource.jndi-name=java:comp/env/jdbc/govpay"
```

Gli endpoint actuator rispondono allora su `http://<host>:<porta>/govpay-appio-batch/actuator/...`. La pipeline non pubblica il war: si ottiene solo con la build locale.

## Esecuzione con Docker

L'immagine è prevista su Docker Hub come [linkitaly/govpay-appio-batch](https://hub.docker.com/r/linkitaly/govpay-appio-batch) (release) e `linkitaly/govpay-appio-batch-dev` (sviluppo, a ogni push su `main`). Il servizio risponde sulla porta `10001`; i driver JDBC si montano in `/opt/jdbc-drivers`.

```bash
docker run -d --name govpay-appio-batch -p 10001:10001 \
  -v ./jdbc-drivers:/opt/jdbc-drivers \
  -e GOVPAY_DB_TYPE=postgresql \
  -e GOVPAY_DB_SERVER=postgres:5432 \
  -e GOVPAY_DB_NAME=govpay \
  -e GOVPAY_DB_USER=govpay \
  -e GOVPAY_DB_PASSWORD=<password> \
  linkitaly/govpay-appio-batch
```

Variabili d'ambiente dell'immagine, oltre alle proprietà Spring Boot in forma di variabile:

| Variabile | Obbligatoria | Default | Descrizione |
|-----------|--------------|---------|-------------|
| `GOVPAY_DB_TYPE` | sì | - | Tipo di database: `postgresql`, `mysql`, `mariadb`, `oracle` |
| `GOVPAY_DB_SERVER` | sì | - | Server del database, `host` o `host:porta` (porta di default del database se omessa) |
| `GOVPAY_DB_NAME` | sì | - | Nome del database (per Oracle, service name o SID) |
| `GOVPAY_DB_USER` | sì | - | Utente del database |
| `GOVPAY_DB_PASSWORD` | sì | - | Password del database (se manca l'avvio prosegue con un avviso) |
| `GOVPAY_DS_CONN_PARAM` | no | - | Parametri aggiunti all'URL JDBC (es. `zeroDateTimeBehavior=convertToNull` per MySQL) |
| `GOVPAY_DS_JDBC_LIBS` | no | `/opt/jdbc-drivers` | Directory dei driver JDBC |
| `GOVPAY_ORACLE_JDBC_URL_TYPE` | no | `servicename` | Formato dell'URL Oracle: `servicename` o `sid` |
| `ORACLE_TNS_ADMIN` | no | - | Directory dei file TNS di Oracle |
| `GOVPAY_APPIO_MIN_POOL` | no | `2` | Connessioni minime inattive del pool |
| `GOVPAY_APPIO_MAX_POOL` | no | `10` | Connessioni massime del pool |
| `GOVPAY_APPIO_BATCH_USA_CRON` | no | - | `true`, `si`, `yes` o `1`: esecuzione singola con il profilo `cron`, senza server HTTP. Oggi, senza job, il container parte e termina subito. Altrimenti il processo resta attivo |
| `GOVPAY_APPIO_BATCH_INTERVALLO_CRON` | no | `120` | Minuti tra un'esecuzione e l'altra con lo scheduler interno, convertiti in `SCHEDULER_APPIOJOB_FIXEDDELAYSTRING`; un valore non numerico o zero blocca l'avvio. Oggi non c'è un job da schedulare |
| `SCHEDULER_APPIOJOB_FIXEDDELAYSTRING` | no | - | Intervallo in ms (`scheduler.appioJob.fixedDelayString`); se impostata ha la precedenza su `GOVPAY_APPIO_BATCH_INTERVALLO_CRON`, che viene ignorata con un avviso |
| `SERVER_PORT` | no | `10001` | Porta HTTP senza la modalità cron |
| `MANAGEMENT_SERVER_PORT` | no | porta del servizio | Porta dedicata agli endpoint actuator |
| `MANAGEMENT_ENDPOINTS` | no | `health,info,metrics,prometheus` | Endpoint actuator esposti |
| `LOG_LEVEL_REFRESH_INTERVAL` | no | `60s` | Intervallo di rilettura dei livelli di log dal database |
| `GOVPAY_APPIO_POP_DB_SKIP` | no | `TRUE` | `FALSE` crea all'avvio le tabelle Spring Batch, se la tabella `GOVPAY_APPIO_DB_CHECK_TABLE` non esiste |
| `GOVPAY_APPIO_DB_CHECK_TABLE` | no | `batch_job_execution_context` | Tabella che indica che lo schema Spring Batch è già presente |
| `GOVPAY_APPIO_LIVE_DB_CHECK_SKIP` | no | `FALSE` | Salta l'attesa della porta del database prima dell'inizializzazione |
| `GOVPAY_APPIO_LIVE_DB_CHECK_MAX_RETRY` | no | `30` | Tentativi di connessione alla porta del database |
| `GOVPAY_APPIO_LIVE_DB_CHECK_SLEEP_TIME` | no | `2` | Secondi tra un tentativo e l'altro |
| `GOVPAY_APPIO_LIVE_DB_CHECK_CONNECT_TIMEOUT` | no | `5` | Timeout di ogni tentativo di connessione, in secondi |
| `GOVPAY_APPIO_READY_DB_CHECK_SKIP` | no | `FALSE` | Salta la verifica della tabella e crea comunque lo schema |
| `GOVPAY_APPIO_READY_DB_CHECK_MAX_RETRY` | no | `5` | Tentativi della verifica della tabella |
| `GOVPAY_APPIO_READY_DB_CHECK_SLEEP_TIME` | no | `2` | Secondi tra un tentativo e l'altro |
| `GOVPAY_APPIO_JVM_MAX_RAM_PERCENTAGE` | no | `80` | `-XX:MaxRAMPercentage` della JVM |
| `GOVPAY_APPIO_JVM_INITIAL_RAM_PERCENTAGE` | no | - | `-XX:InitialRAMPercentage` della JVM |
| `GOVPAY_APPIO_JVM_MIN_RAM_PERCENTAGE` | no | - | `-XX:MinRAMPercentage` della JVM |
| `GOVPAY_APPIO_JVM_MAX_METASPACE_SIZE` | no | - | `-XX:MaxMetaspaceSize` della JVM |
| `GOVPAY_APPIO_JVM_MAX_DIRECT_MEMORY_SIZE` | no | - | `-XX:MaxDirectMemorySize` della JVM |
| `JAVA_OPTS` | no | - | Opzioni aggiuntive della JVM |

L'entrypoint imposta `GOVPAY_BATCH_CLUSTER_ID` all'indirizzo IP del container, o al suo hostname se l'IP non si ricava. `GOVPAY_APPIO_GDE_URL`, presente nelle versioni precedenti della documentazione, non ha effetto: se impostata, l'entrypoint scrive un avviso. Le altre variabili che compaiono negli script dell'immagine (es. `INTERVALLO_MINUTI`, `HSQLDB_SHA256`) sono interne e non vanno impostate. I messaggi dell'entrypoint e degli script di inizializzazione sono nel log del container (`docker logs`).

Costruzione dell'immagine in locale, scansione delle vulnerabilità e pacchetti inclusi sono descritti in [docker/DOCKER.md](docker/DOCKER.md) e [docker/SCANSIONE.md](docker/SCANSIONE.md).

## Monitoraggio

Gli endpoint actuator rispondono di default sulla porta del servizio (`8080` con il jar, `10001` nel container):

```bash
curl http://localhost:10001/actuator/health       # stato, compresa la connessione al database
curl http://localhost:10001/actuator/prometheus   # metriche in formato Prometheus
```

Per spostarli su una porta dedicata impostare `management.server.port` (nel container `MANAGEMENT_SERVER_PORT`). L'endpoint `loggers`, che permette di cambiare i livelli di log a caldo, non è esposto di default perché il servizio non ha autenticazione: abilitarlo solo insieme a una porta dedicata e non raggiungibile dall'esterno, per esempio `MANAGEMENT_SERVER_PORT=9090` e `MANAGEMENT_ENDPOINTS=health,info,metrics,prometheus,loggers`. In alternativa i livelli si cambiano dalla console, tramite la tabella `configurazione`.

## Pipeline

Validazione (build, test, coverage, OWASP, SonarCloud, licenze, SBOM, scansione dell'immagine) e rilascio su tag sono descritti in [.github/PIPELINE.md](.github/PIPELINE.md).

## Licenza

GovPay AppIO Batch è distribuito con licenza [GNU General Public License v3.0](LICENSE).
