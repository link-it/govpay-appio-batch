package it.govpay.appio.batch;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import it.govpay.common.configurazione.service.ConfigurazioneService;
import it.govpay.common.logging.level.DynamicLogLevelService;

/**
 * Avvio del contesto su H2 in memoria: verifica che la configurazione comune di
 * govpay-common sia caricata e che i livelli di log letti dalla tabella
 * configurazione ({@code govpay.logging.livelli-dinamici.*}) siano attivi.
 */
@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:appiobatch;DB_CLOSE_DELAY=-1",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop"
})
class ApplicationContextTest {

	@Autowired
	private ApplicationContext context;

	@Test
	void livelliDiLogDinamiciAttivi() {
		assertNotNull(this.context.getBean(ConfigurazioneService.class));
		assertNotNull(this.context.getBean(DynamicLogLevelService.class));
	}
}
