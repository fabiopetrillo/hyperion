package nl.tudelft.hyperion.datasource.plugins.elasticsearch

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import nl.tudelft.hyperion.pluginmanager.RedisConfig
import java.lang.IllegalArgumentException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Configuration for Elasticsearch communication.
 *
 * @property hostname hostname of the Elasticsearch server
 * @property index which index to retrieve documents from
 * @property port port of the Elasticsearch server
 * @property scheme scheme to use for retrieval, is either `http` or `https`
 * @property authentication whether authentication is enabled
 * @property timestampField which field to use for querying based on time
 * @property responseHitCount amount of hits to expect
 * @property username username to pass if authentication is set to true
 * @property password password to pass if authentication is set to true
 */
data class ElasticsearchConfig(
        val hostname: String,
        val index: String,
        var port: Int?,
        var scheme: String?,
        val authentication: Boolean,
        @JsonProperty("timestamp_field")
        val timestampField: String,
        @JsonProperty("response_hit_count")
        var responseHitCount: Int,
        val username: String?,
        val password: String?
) {
    init {
        // set default values if field is missing
        // jackson does not support default value setting as of 2.7.1
        if (port == null) {
            port = 9200
        }

        if (scheme == null) {
            scheme = "http"
        }
    }

    /**
     * Verifies that the configuration is correct.
     *
     * @throws IllegalArgumentException if any of the fields are invalid
     */
    fun verify() {
        if (port !in 0..65535) {
            throw IllegalArgumentException("port must be between 0 and 65535 but was port=$port")
        }

        val schemeLower = scheme!!.toLowerCase()
        if (schemeLower != "http" && schemeLower != "https") {
            throw IllegalArgumentException("scheme must be 'http' or 'https' but not scheme=$schemeLower")
        }

        if (responseHitCount < 1) {
            throw IllegalArgumentException("response_hit_count must be a positive non-zero integer")
        }

        if (authentication && (username == null || password == null)) {
            throw IllegalArgumentException("username and password must be provided to use authentication")
        }
    }
}

/**
 * Represents a configuration setup for the Elasticsearch plugin.
 * Contains the necessary arguments to communicate and pull data from
 * Elasticsearch.
 *
 * @property pollInterval time between sending queries in milliseconds
 * @property redis Redis configuration
 * @property es Elasticsearch configuration
 * @property registrationChannelPostfix postfix to add after the name for the redis hash
 * @property name name of the plugin
 */
data class Configuration(
        @JsonProperty("poll_interval")
        var pollInterval: Long,
        val redis: RedisConfig,
        @JsonProperty("elasticsearch")
        val es: ElasticsearchConfig,
        @JsonProperty("registration_channel_postfix")
        var registrationChannelPostfix: String?,
        val name: String
) {

    /**
     * Verifies that the configuration is correct.
     *
     * @throws IllegalArgumentException if any of the fields are invalid
     */
    fun verify() {
        es.verify()

        if (pollInterval < 1) {
            throw IllegalArgumentException("poll_interval must be a positive non-zero integer")
        }
    }

    companion object {
        /**
         * Parses the configuration file located at the specified path into
         * a configuration of the content. Will throw if the config is not
         * formatted properly.
         *
         * @param path the path to the configuration file
         * @return the parsed configuration
         */
        fun load(path: Path): Configuration {
            val content = Files.readString(path)
            return parse(content)
        }

        /**
         * Parses a configuration object from the specified YAML string.
         * Will throw if the config is not formatted properly.
         *
         * @param content the configuration as a YAML string
         * @return the parsed configuration
         */
        fun parse(content: String): Configuration {
            val mapper = ObjectMapper(YAMLFactory())
            mapper.registerModule(KotlinModule())

            return mapper.readValue(content, Configuration::class.java)
                    .also(Configuration::verify)
        }
    }
}
