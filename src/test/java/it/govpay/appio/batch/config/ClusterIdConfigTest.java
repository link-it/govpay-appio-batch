package it.govpay.appio.batch.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import io.micrometer.core.instrument.MeterRegistry;
import it.govpay.appio.batch.Application;
import it.govpay.common.batch.config.ClusterIdPredefinito;
import it.govpay.common.batch.metrics.BatchMetricsAutoConfiguration;
import it.govpay.common.batch.metrics.BatchMetricsNodo;

/**
 * Verifica che il tag {@code nodo} delle metriche valga il cluster-id del nodo
 * (govpay-common#35, #36): senza {@code govpay.batch.cluster-id} e' l'identificativo
 * dell'host, con la property e' il valore configurato. Oggi il batch non legge il
 * cluster-id: il tag e' l'unico consumatore.
 */
class ClusterIdConfigTest {

	private static final String DB_URL = "spring.datasource.url=jdbc:h2:mem:appiobatch-clusterid;DB_CLOSE_DELAY=-1";
	private static final String DB_USER = "spring.datasource.username=sa";
	private static final String DB_PASSWORD = "spring.datasource.password=";
	private static final String DDL = "spring.jpa.hibernate.ddl-auto=create-drop";

	private static String tagNodo(MeterRegistry meterRegistry) {
		return meterRegistry.counter("govpay.test.cluster.id").getId().getTag(BatchMetricsAutoConfiguration.TAG_NODO);
	}

	@Nested
	@SpringBootTest(classes = Application.class, properties = { DB_URL, DB_USER, DB_PASSWORD, DDL })
	class SenzaProperty {

		@Autowired
		private BatchMetricsNodo batchMetricsNodo;

		@Autowired
		private MeterRegistry meterRegistry;

		@Test
		void usaIlClusterIdPredefinitoDelNodo() {
			String atteso = ClusterIdPredefinito.valore();

			assertThat(batchMetricsNodo.valore()).isEqualTo(atteso);
			assertThat(tagNodo(meterRegistry)).isEqualTo(atteso);
		}
	}

	@Nested
	@SpringBootTest(classes = Application.class,
			properties = { DB_URL, DB_USER, DB_PASSWORD, DDL, "govpay.batch.cluster-id=nodo-1" })
	class ConProperty {

		@Autowired
		private BatchMetricsNodo batchMetricsNodo;

		@Autowired
		private MeterRegistry meterRegistry;

		@Test
		void usaIlClusterIdConfigurato() {
			assertThat(batchMetricsNodo.valore()).isEqualTo("nodo-1");
			assertThat(batchMetricsNodo.fonte()).isEqualTo(BatchMetricsNodo.PROPERTY_CLUSTER_ID);
			assertThat(tagNodo(meterRegistry)).isEqualTo("nodo-1");
		}
	}
}
