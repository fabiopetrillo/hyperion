package nl.tudelft.hyperion.plugin.graphs

import com.jetbrains.rd.util.debug
import com.jetbrains.rd.util.getLogger
import nl.tudelft.hyperion.plugin.metric.APIBinMetricsResponse
import nl.tudelft.hyperion.plugin.metric.BaseAPIMetric
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormatter
import java.awt.Color

/**
 * Short alias for 2D arrays.
 */
typealias Array2D<T> = Array<Array<T>>

/**
 * Represents some grouped metric that is displayed in the histogram as a
 * single box.
 *
 * @property count the number of triggered log lines in this grouped metric.
 * @property color the color to give the grouped metric.
 * @property label what the grouped metric should be referenced as.
 *  Currently, this is used for displaying the severity.
 */
data class BinComponent(
    val count: Int,
    val color: Color,
    val label: String
)

/**
 * Represents all data necessary for a histogram, is composed of the counts for
 * each box, the list of timestamps, the label and color of each box.
 */
data class HistogramData(
    var bins: Array2D<BinComponent>,
    var timestamps: Array<String>
) {
    val logCounts
        get() = bins.map { it.map(BinComponent::count) }

    val colors
        get() = bins.map { it.map(BinComponent::color) }

    val labels
        get() = bins.map { it.map(BinComponent::label) }

    // <editor-fold desc="Hide generated">
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HistogramData

        if (!bins.contentDeepEquals(other.bins)) return false
        if (!timestamps.contentEquals(other.timestamps)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bins.contentDeepHashCode()
        result = 31 * result + timestamps.contentHashCode()
        return result
    }
    // </editor-fold>
}

/**
 * Parses the given [APIBinMetricsResponse] into a format suitable for
 * visualization.
 *
 * @param version the version to use.
 * @param timeFormatter the formatter used for getting string representations
 *  of the timestamps.
 * @param colorScheme the color scheme used where the key is the severity and
 *  the value is a [Color].
 * @param defaultColor the color to use if the severity is not in
 *  [colorScheme].
 * @param response the API response to parse
 * @return the extracted [HistogramData] object.
 */
fun parseAPIBinResponse(
    version: String,
    timeFormatter: DateTimeFormatter,
    colorScheme: Map<String, Color>,
    defaultColor: Color,
    response: APIBinMetricsResponse<out BaseAPIMetric>
): HistogramData {
    val bins = mutableListOf<Array<BinComponent>>()
    val timestamps = mutableListOf<String>()

    response.results.forEach {
        // Add formatted timestamp values per box
        val endTime = it.startTime + response.interval
        timestamps.add(DateTime(endTime * 1000L).toString(timeFormatter))

        // Check if the given version exists
        if (version !in it.versions) {
            // TODO: add better missing version handling
            getLogger<HistogramData>().debug {
                "Version=$version missing from API response, setting count to 0"
            }

            bins.add(arrayOf())
            return@forEach
        }

        val bin = it.versions[version] ?: error("version=$version removed at runtime")

        // Add the count, color and severity per box from the metrics
        bins.add(groupAndParseBin(bin, colorScheme, defaultColor))
    }

    return HistogramData(
        bins.toTypedArray(),
        timestamps.toTypedArray()
    )
}

/**
 * Groups metrics by ordered severity and sums the counts.
 *
 * @param bin the metric bin to modify.
 * @param colorScheme the color scheme used where the key is the severity and
 *  the value is a [Color].
 * @param defaultColor the color to use if the severity is not in
 *  [colorScheme].
 * @return the transformed array of grouped bin components.
 */
private fun groupAndParseBin(
    bin: List<BaseAPIMetric>,
    colorScheme: Map<String, Color>,
    defaultColor: Color
): Array<BinComponent> = run {
    val order = colorScheme.keys.toList()

    bin
        .groupBy { it.severity }
        .toSortedMap(compareBy {
            order.indexOf(it.toLowerCase())
        })
        .map {
            BinComponent(
                it.value.map(BaseAPIMetric::count).sum(),
                colorScheme.getOrDefault(it.key.toLowerCase(), defaultColor),
                it.key
            )
        }.toTypedArray()
}
