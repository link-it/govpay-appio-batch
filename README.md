<p align="center">
<img src="https://www.link.it/wp-content/uploads/2025/01/logo-govpay.svg" alt="GovPay Logo" width="200"/>
</p>

# GovPay - Porta di accesso al sistema pagoPA - AppIO Batch

[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=link-it_govpay-appio-batch&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=link-it_govpay-appio-batch)
[![Docker Hub](https://img.shields.io/docker/v/linkitaly/govpay-appio-batch?label=Docker%20Hub&logo=docker)](https://hub.docker.com/r/linkitaly/govpay-appio-batch)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://raw.githubusercontent.com/link-it/govpay-appio-batch/main/LICENSE)

Batch Spring Boot per l'invio dei messaggi e degli avvisi di pagamento di GovPay tramite App IO.

> Il progetto e' in fase di avvio: al momento contiene lo scheletro
> dell'applicazione e le pipeline di validazione e rilascio, allineate agli
> altri componenti GovPay (es. govpay-notify-batch).

## Compilazione ed Esecuzione

### Compilazione
```bash
# Con Java 21 impostato come JAVA_HOME
mvn clean install

# Senza OWASP Dependency-Check (build piu' veloce)
mvn clean install -Dowasp.phase=none

# Con l'archivio degli script SQL (target/sql.zip) usato dall'immagine Docker
mvn clean install -Pjar,dist
```

### Driver JDBC

I driver JDBC **non sono inclusi** nel fat jar e devono essere forniti esternamente a runtime,
in una directory indicata con la proprieta' `loader.path`:

```bash
java -Dloader.path=./jdbc-drivers -jar target/govpay-appio-batch.jar
```

### Esecuzione con Docker

Con Docker la variabile d'ambiente `LOADER_PATH` viene impostata automaticamente
dall'entrypoint alla directory `/opt/jdbc-drivers`:

```bash
docker run -v ./jdbc-drivers:/opt/jdbc-drivers linkitaly/govpay-appio-batch
```

Configurazione completa in [docker/DOCKER.md](docker/DOCKER.md).

## Pipeline

Validazione (build, test, coverage, OWASP, SonarCloud, licenze, SBOM, scansione
dell'immagine) e rilascio su tag sono descritti in [.github/PIPELINE.md](.github/PIPELINE.md).
La scansione dell'immagine Docker e' descritta in [docker/SCANSIONE.md](docker/SCANSIONE.md).

## Documentazione

- **[ChangeLog](ChangeLog)** - Storia completa delle modifiche e release

## License

Questo progetto e' distribuito sotto licenza GPL v3. Vedere il file [LICENSE](LICENSE) per i dettagli.

## Contatti

- **Progetto**: [GovPay AppIO Batch](https://github.com/link-it/govpay-appio-batch)
- **Organizzazione**: [Link.it](https://www.link.it)
