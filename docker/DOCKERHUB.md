<p align="center">
<img src="https://www.link.it/wp-content/uploads/2025/01/logo-govpay.svg" alt="GovPay Logo" width="200"/>
</p>

# GovPay - Porta di accesso al sistema pagoPA - AppIO Batch

[![GitHub](https://img.shields.io/badge/GitHub-link--it%2Fgovpay--appio--batch-blue?logo=github)](https://github.com/link-it/govpay-appio-batch)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)

Batch Spring Boot che invierà, tramite App IO, i messaggi e gli avvisi di pagamento delle pendenze gestite da [GovPay](https://github.com/link-it/govpay).

> **Progetto in fase di avvio.** L'immagine contiene lo scheletro dell'applicazione: si collega al database di GovPay ed espone gli endpoint actuator, ma non ha ancora job né integrazione con App IO.

## Versioni disponibili

- `latest` - ultima versione rilasciata
- `linkitaly/govpay-appio-batch-dev` - immagine di sviluppo, aggiornata a ogni push su `main`

Storico delle modifiche nel [ChangeLog](https://github.com/link-it/govpay-appio-batch/blob/main/ChangeLog).

## Avvio

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

I driver JDBC non sono inclusi nell'immagine e vanno montati in `/opt/jdbc-drivers`.

## Documentazione

- [README: configurazione, variabili d'ambiente, esecuzione](https://github.com/link-it/govpay-appio-batch/blob/main/README.md)
- [Immagine Docker: contenuto, costruzione, modalità di esecuzione](https://github.com/link-it/govpay-appio-batch/blob/main/docker/DOCKER.md)
- [Dockerfile](https://github.com/link-it/govpay-appio-batch/blob/main/docker/govpay-appio/Dockerfile.github)

## Licenza

GovPay AppIO Batch è distribuito con licenza [GPL v3](https://www.gnu.org/licenses/gpl-3.0).

---

Sviluppato da [Link.it s.r.l.](https://www.link.it)
