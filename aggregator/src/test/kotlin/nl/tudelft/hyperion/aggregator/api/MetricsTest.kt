package nl.tudelft.hyperion.aggregator.api

import nl.tudelft.hyperion.aggregator.Configuration
import nl.tudelft.hyperion.aggregator.database.AggregationEntries
import nl.tudelft.hyperion.aggregator.utils.TestWithoutLogging
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.transactions.transactionManager
import org.joda.time.DateTimeUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.charset.Charset
import java.sql.Connection

class MetricsTest : TestWithoutLogging() {
    lateinit var transaction: Transaction

    @BeforeEach
    fun `Setup SQLite database with predefined data`() {
        val db = Database.connect("jdbc:sqlite::memory:")
        TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

        transaction = db.transactionManager.newTransaction()

        transaction {
            SchemaUtils.create(AggregationEntries)

            // Load statements and execute.
            val statements = String(
                javaClass.classLoader.getResourceAsStream("aggregates.sql")!!
                    .readAllBytes(), Charset.defaultCharset()
            )

            for (stmt in statements.split("\n")) {
                exec(stmt)
            }
        }

        // Hardcode timestamp
        DateTimeUtils.setCurrentMillisFixed(1588844616804)
    }

    @AfterEach
    fun `Reset timestamp and database`() {
        DateTimeUtils.setCurrentMillisSystem()

        transaction {
            // Drop table after finishing
            SchemaUtils.drop(AggregationEntries)
        }

        transaction.commit()
    }

    @Test
    fun `computeMetrics should clamp to the minimum granularity`() {
        val config = Configuration("a", 1, 20, 1000)

        // 1 second should yield no result. 20 seconds should yield results.
        // ie, if we clamp we should have results
        val results = computeMetrics(
            config,
            "TestProject",
            "com.sap.enterprises.server.impl.TransportationService",
            listOf(1)
        )

        Assertions.assertFalse(results[0].versions.isEmpty())
    }

    @Test
    fun `computeMetrics should aggregate across the specified timeframe`() {
        val config = Configuration("a", 1, 20, 1000)

        // 1 second should yield no result. 20 seconds should yield results.
        // ie, if we clamp we should have results
        val results = computeMetrics(
            config,
            "TestProject",
            "com.sap.enterprises.server.impl.TransportationService",
            listOf(20)
        )

        Assertions.assertEquals(
            listOf(
                MetricsResult(
                    interval = 20, versions = mapOf(
                        "v1.0.0" to listOf(
                            Metric(line = 11, severity = "INFO", count = 5),
                            Metric(line = 20, severity = "WARN", count = 1),
                            Metric(line = 37, severity = "INFO", count = 1)
                        )
                    )
                )
            ),
            results
        )
    }

    @Test
    fun `computeMetrics should allow multiple timeframes`() {
        val config = Configuration("a", 1, 20, 1000)

        // 1 second should yield no result. 20 seconds should yield results.
        // ie, if we clamp we should have results
        val results = computeMetrics(
            config,
            "TestProject",
            "com.sap.enterprises.server.impl.TransportationService",
            listOf(20, 120)
        )

        Assertions.assertEquals(
            listOf(
                MetricsResult(
                    interval = 20, versions = mapOf(
                        "v1.0.0" to listOf(
                            Metric(line = 11, severity = "INFO", count = 5),
                            Metric(line = 20, severity = "WARN", count = 1),
                            Metric(line = 37, severity = "INFO", count = 1)
                        )
                    )
                ),
                MetricsResult(
                    interval = 120, versions = mapOf(
                        "v1.0.0" to listOf(
                            Metric(line = 11, severity = "INFO", count = 36),
                            Metric(line = 20, severity = "WARN", count = 9),
                            Metric(line = 23, severity = "ERROR", count = 4),
                            Metric(line = 34, severity = "ERROR", count = 7),
                            Metric(line = 37, severity = "INFO", count = 21)
                        )
                    )
                )
            ),
            results
        )
    }

    @Test
    fun `computeMetrics should not error on missing files or projects`() {
        val config = Configuration("a", 1, 20, 1000)

        // Check invalid project
        var results = computeMetrics(
            config,
            "InvalidProject",
            "com.sap.enterprises.server.impl.TransportationService",
            listOf(1)
        )

        Assertions.assertTrue(results[0].versions.isEmpty())

        // Check invalid file.
        results = computeMetrics(
            config,
            "TestProject",
            "com.this.file.does.not.exist",
            listOf(1)
        )

        Assertions.assertTrue(results[0].versions.isEmpty())
    }

    @Test
    fun `computePeriodicMetrics should correctly bin log metrics for a single file`() {
        val config = Configuration("a", 1, 1, 1000)

        val relTime = 12
        val steps = 3

        val results = computePeriodicMetrics(
            config,
            "TestProject",
            "com.sap.enterprises.server.impl.TransportationService",
            relTime,
            steps
        )

        // The relative time of 12s is split into intervals of 4s
        // the current time is 1588844616 and the most recent log
        // for this file starts at 1588844615
        Assertions.assertEquals(relTime / steps, results.first)
        Assertions.assertEquals(
            listOf(
                BinnedMetricsResult(
                    startTime = 1588844604,
                    versions = mapOf(
                        "v1.0.0" to listOf(
                            Metric(11, "INFO", 3),
                            Metric(20, "WARN", 1),
                            Metric(37, "INFO", 1)
                        )
                    )
                ),
                BinnedMetricsResult(1588844608, mapOf()),
                BinnedMetricsResult(
                    startTime = 1588844612,
                    versions = mapOf(
                        "v1.0.0" to listOf(
                            Metric(11, "INFO", 2)
                        )
                    )
                )
            ),
            results.second
        )
    }

    @Test
    fun `computePeriodicMetrics should correctly bin log metrics of all files`() {
        val config = Configuration("a", 1, 1, 1000)

        val relTime = 10
        val steps = 2

        val results = computePeriodicMetrics(
            config,
            "TestProject",
            null,
            relTime,
            steps
        )

        // The relative time of 10s is split into intervals of 5s
        // the current time is 1588844616 and the most recent logs
        // for this project start at 1588844615
        Assertions.assertEquals(relTime / steps, results.first)
        Assertions.assertEquals(
            listOf(
                BinnedMetricsResult(1588844606, mapOf()),
                BinnedMetricsResult(
                    startTime = 1588844611,
                    versions = mapOf(
                        "v1.0.0" to listOf(
                            FileMetric(11, "INFO", 2, "com.sap.enterprises.server.impl.TransportationService"),
                            FileMetric(13, "INFO", 1, "com.sap.enterprises.server.impl.math.IntegerFactory"),
                            FileMetric(24, "INFO", 1, "com.sap.enterprises.server.impl.math.ArithmeticAbstracter")
                        )
                    )
                )
            ),
            results.second
        )
    }

    @Test
    fun `computePeriodicMetrics should properly clamp time`() {
        val config = Configuration("a", 1, 5, 1000)

        val relTime = 10
        // The intervals would be expected to be 2s
        // due to granularity, this value should be clamped to 5s
        val steps = 5

        val results = computePeriodicMetrics(
            config,
            "TestProject",
            "com.sap.enterprises.server.impl.TransportationService",
            relTime,
            steps
        )

        Assertions.assertEquals(5, results.first)
        Assertions.assertEquals(
            listOf(
                BinnedMetricsResult(1588844606, mapOf()),
                BinnedMetricsResult(
                    startTime = 1588844611,
                    versions = mapOf(
                        "v1.0.0" to listOf(
                            Metric(11, "INFO", 2)
                        )
                    )
                )
            ),
            results.second
        )
    }
}
