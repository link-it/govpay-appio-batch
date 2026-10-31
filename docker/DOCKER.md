# GovPay AppIO Batch - Immagine Docker

Immagine Docker del batch GovPay AppIO Batch. Il progetto è in fase di avvio: l'immagine contiene lo scheletro dell'applicazione, senza job (vedi il [README](../README.md#sommario)).

Le variabili d'ambiente dell'immagine, con default e descrizione, sono elencate nel [README](../README.md#esecuzione-con-docker), insieme a un esempio di `docker run`.

## Contenuto dell'immagine

| Percorso | Contenuto |
|----------|-----------|
| `/opt/govpay-appio/govpay-appio-batch.jar` | Fat jar del batch (`PropertiesLauncher`) |
| `/opt/jdbc-drivers` | Directory dei driver JDBC, da montare come volume (`GOVPAY_DS_JDBC_LIBS`) |
| `/opt/sql` | Contenuto di `sql.zip`: schema Spring Batch per ogni database (`sql/spring-batch`), script di svecchiamento dei metadati (`sql/cleanup`) e file `VERSION` |
| `/opt/hsqldb-2.7.4/hsqldb/lib` | SqlTool di HSQLDB, usato da `init_appio_db.sh` per creare lo schema |
| `/usr/local/bin/entrypoint.sh` | Entrypoint: costruisce l'URL JDBC dalle variabili `GOVPAY_DB_*`, inizializza il database se richiesto e avvia il jar |
| `/usr/local/bin/init_appio_db.sh` | Creazione delle tabelle Spring Batch, se `GOVPAY_APPIO_POP_DB_SKIP=FALSE` |
| `/var/log/govpay-appio/govpay-appio.log` | File di log dell'applicazione |

Immagine base `eclipse-temurin:25-jre-alpine`, utente `govpay`, fuso orario `Europe/Rome`.

## Costruzione

Lo script `build_image.sh` costruisce l'immagine in due modi:

- **da un jar locale** (`Dockerfile.daFile`), dopo `mvn clean install -Pjar,dist` nella radice del progetto, che produce anche `target/sql.zip`:

  ```bash
  cd docker
  ./build_image.sh -l ../target/govpay-appio-batch.jar -t linkitaly/govpay-appio-batch:locale
  ```

- **da una release GitHub** (`Dockerfile.github`): scarica jar e `sql.zip` della release indicata con `-v` (default: l'ultima) e ne verifica lo sha256, letto dalle API delle release di GitHub o passato con `-s` e `-q`.

Opzioni: `-t <repository>:<tag>` nome dell'immagine, `-e <path>` directory dell'applicazione, `-f <path>` directory dei log, `-h` aiuto.

La pipeline costruisce e pubblica `linkitaly/govpay-appio-batch-dev` a ogni push su `main` e `linkitaly/govpay-appio-batch` sui tag, dopo la scansione descritta in [SCANSIONE.md](SCANSIONE.md).

## Modalità di esecuzione

| `GOVPAY_APPIO_BATCH_USA_CRON` | Comportamento |
|-------------------------------|---------------|
| non impostata o diversa dai valori sotto | Il processo resta attivo ed espone gli endpoint actuator sulla porta `SERVER_PORT` (default `10001`). L'intervallo del futuro job è `GOVPAY_APPIO_BATCH_INTERVALLO_CRON` minuti (default `120`) |
| `true`, `si`, `yes`, `1` | Esecuzione singola con il profilo `cron`, senza server HTTP, da pianificare dall'esterno (cron, CronJob Kubernetes). Oggi, senza job, il container parte e termina subito |

## Database

L'entrypoint costruisce l'URL JDBC da `GOVPAY_DB_TYPE` (`postgresql`, `mysql`, `mariadb`, `oracle`), `GOVPAY_DB_SERVER` e `GOVPAY_DB_NAME`; MySQL e MariaDB usano entrambi il driver MySQL. I driver vanno montati in `/opt/jdbc-drivers`.

Con `GOVPAY_APPIO_POP_DB_SKIP=FALSE`, prima di avviare il batch `init_appio_db.sh`:

1. attende che la porta del database risponda (`GOVPAY_APPIO_LIVE_DB_CHECK_*`);
2. verifica se la tabella `GOVPAY_APPIO_DB_CHECK_TABLE` esiste (`GOVPAY_APPIO_READY_DB_CHECK_*`);
3. se non esiste, esegue `/opt/sql/spring-batch/schema-<database>.sql`.

Se uno dei passi fallisce il container termina con errore.

## Log

```bash
docker logs -f govpay-appio-batch
docker exec -it govpay-appio-batch tail -f /var/log/govpay-appio/govpay-appio.log
```
