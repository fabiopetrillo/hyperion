@file:JvmName("VisWindow")

package nl.tudelft.hyperion.plugin.visualization

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.ComboBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import nl.tudelft.hyperion.plugin.connection.APIRequestor
import nl.tudelft.hyperion.plugin.graphs.HistogramParameters
import nl.tudelft.hyperion.plugin.graphs.InteractiveHistogram
import nl.tudelft.hyperion.plugin.metric.APIBinMetricsResponse
import nl.tudelft.hyperion.plugin.metric.BaseAPIMetric
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.joda.time.format.DateTimeFormatter
import java.awt.Color
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel

class VisWindow {
    lateinit var root: JPanel
    lateinit var main: JPanel
    lateinit var granularityComboBox: JComboBox<String>
    lateinit var onlyFileCheckBox: JCheckBox
    lateinit var statusLabel: JLabel
    lateinit var refreshButton: JButton

    companion object {
        const val HISTOGRAM_X_MARGIN = 50
        const val HISTOGRAM_BAR_SPACING = 5

        // the start is from down up, so start > end
        // the y coordinates go from top to bottom
        const val HISTOGRAM_Y_START = 200
        const val HISTOGRAM_Y_END = 100

        // TODO: make color scheme configurable
        //  or make the unique severities in the aggregator unique
        val HISTOGRAM_DEFAULT_COLOR = Color.GRAY

        val HISTOGRAM_COLOR_SCHEME = mapOf(
            "err" to Color.RED,
            "error" to Color.RED,
            "warn" to Color.ORANGE,
            "warning" to Color.ORANGE,
            "info" to Color.GREEN,
            "debug" to Color.BLUE
        )

        private val logger = Logger.getInstance(VisWindow::class.java)

        private val DATETIME_FORMATTER: DateTimeFormatter = DateTimeFormat.forPattern("kk:mm:ss");

        fun parseAPIBinResponse(
            version: String,
            response: APIBinMetricsResponse<out BaseAPIMetric>
        ): HistogramParameters {
            // TODO: also add parsing for the severity label
            val bins = mutableListOf<Array<Int>>()
            val timestamps = mutableListOf<String>()
            val colors = mutableListOf<Array<Color>>()

            response.results.forEach {
                // Add formatted timestamp values per box
                val endTime = it.startTime + response.interval
                timestamps.add(DateTime(endTime * 1000L).toString(DATETIME_FORMATTER))

                // Check if the given version exists
                if (version !in it.versions) {
                    // TODO: add better missing version handling
                    logger.warn("Version=$version missing from API response, setting count to 0")

                    bins.add(arrayOf())
                    colors.add(arrayOf())

                    return@forEach
                }

                val bin = it.versions[version] ?: error("version=$version removed at runtime")

                // Add the counts per box from the metrics
                bins.add(bin.map(BaseAPIMetric::count).toTypedArray())

                // Add the color per box from the metrics
                colors.add(
                    bin.map { metric ->
                        HISTOGRAM_COLOR_SCHEME.getOrDefault(metric.severity.toLowerCase(), HISTOGRAM_DEFAULT_COLOR)
                    }.toTypedArray()
                )
            }

            return Triple(bins.toTypedArray(), timestamps.toTypedArray(), colors.toTypedArray())
        }
    }

    val content
        get() = root

    fun createUIComponents() {
        // Currently placeholder
        granularityComboBox = ComboBox(arrayOf("30M", "1H", "3H", "6H", "24H", "7D"))
        onlyFileCheckBox = JCheckBox()
        statusLabel = JLabel()
        refreshButton = JButton()

        main = createHistogramComponent()

        runBlocking {
            launch(Dispatchers.IO) {
                val params = queryBinAPI("v1.0.0", 2400, 12)
                val hist = (main as InteractiveHistogram)
                // hist.setAllParameters(params)
            }
        }
    }

    private suspend fun queryBinAPI(
        version: String,
        relativeTime: Int,
        steps: Int
    ): HistogramParameters {
        val project = ProjectManager.getInstance().openProjects[0]
        val data = APIRequestor.getBinnedMetrics(project, relativeTime, steps)

        return parseAPIBinResponse(version, data)
    }

    private fun createHistogramComponent(): InteractiveHistogram {

        return InteractiveHistogram(
            arrayOf(
                arrayOf(10),
                arrayOf(40, 10, 30, 5),
                arrayOf(20, 20, 10, 5),
                arrayOf(20, 15, 40, 5),
                arrayOf(20, 15, 30, 5),
                arrayOf(20, 15, 50, 5),
                arrayOf(20, 15, 50, 5),
                arrayOf(20, 15, 60, 5)
            ),
            50,
            200, 100,
            10,
            arrayOf(
                arrayOf(Color.RED),
                arrayOf(Color.RED, Color.ORANGE, Color.GREEN, Color.BLUE),
                arrayOf(Color.RED, Color.ORANGE, Color.GREEN, Color.BLUE),
                arrayOf(Color.RED, Color.ORANGE, Color.GREEN, Color.BLUE),
                arrayOf(Color.RED, Color.ORANGE, Color.GREEN, Color.BLUE),
                arrayOf(Color.RED, Color.ORANGE, Color.GREEN, Color.BLUE),
                arrayOf(Color.RED, Color.ORANGE, Color.GREEN, Color.BLUE),
                arrayOf(Color.RED, Color.ORANGE, Color.GREEN, Color.BLUE)
            ),
            arrayOf("ERROR", "WARN", "INFO", "DEBUG"),
            arrayOf("10:00:00", "10:00:05", "10:00:10", "10:00:15", "10:00:20", "10:00:25", "10:00:30", "10:00:35")
        )
    }
}
