package nl.tudelft.hyperion.aggregator.api

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.joda.time.DateTime

/**
 * Represents an incoming log that has all required metadata attached,
 * either directly from the start or otherwise through the pipeline.
 *
 * This is parsed from a JSON object that contains at least the following
 * properties. All other properties are discarded:
 *
 * {
 *   “project”: “some unique identifier for project, such as the repo name or package”,
 *   “version”: “some way to represent the version, such as a git tag or hash”,
 *   “severity”: “some severity, no fixed format”,
 *   “location”: {
 *     “file”: “fully specified path relative to repo root”,
 *     “line”: 10
 *   },
 *   “timestamp”: “ISO 8601 timestamp format”
 * }
 */
data class LogEntry(
    /**
     * Some unique identifier that represents the project, such as the repo or package name.
     */
    val project: String,

    /**
     * Some way to represent the version the project is running on, such as a git commit hash.
     */
    val version: String,

    /**
     * Some way to represent the severity of the log line. Usually a standard severity, but
     * this is not required.
     * TODO: Maybe make this optional?
     */
    val severity: String,

    /**
     * The location where the log was triggered.
     */
    val location: LogLocation,

    /**
     * The time at which this log statement happened. Note that this is nullable.
     * When null, the [Configuration.verifyTimestamp] property determines whether
     * or not this message is rejected.
     */
    val timestamp: DateTime? = null
) {
    companion object {
        private val mapper = ObjectMapper(JsonFactory())

        init {
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false) // ignore weird properties
            mapper.registerModule(KotlinModule())
            mapper.registerModule(JodaModule())
        }

        /**
         * Parses a LogEntry from the specified JSON string. Throws an exception if
         * the JSON is malformed or missing specific properties.
         */
        fun parse(json: String): LogEntry {
            return mapper.readValue(json, LogEntry::class.java)
        }
    }
}

/**
 * Represents the location metadata in a {@link LogEntry}.
 * See LogEntry for more details.
 */
data class LogLocation(
    /**
     * The file in which the log was triggered.
     */
    val file: String,

    /**
     * The line in which the log was triggered.
     */
    val line: Int
)
