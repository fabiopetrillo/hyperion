package nl.tudelft.hyperion.pluginmanager.hyperionplugin

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import nl.tudelft.hyperion.pluginmanager.RedisConfig

import java.nio.file.Files
import java.nio.file.Path

private val logger = mu.KotlinLogging.logger {}

data class PluginConfiguration(
        val redis: RedisConfig,
        var registrationChannelPostfix: String?,
        val name: String
) {
    init {
        // set default settings if field is missing
        if (registrationChannelPostfix == null) {
            registrationChannelPostfix = "-config"
        }
    }

    fun verify() {
        if (name.isEmpty()) {
            throw IllegalArgumentException("Plugin name should not be empty")
        }

        if (redis.port !in 0..65535) {
            throw IllegalArgumentException("Redis port must be between 0 and 65535 but was ${redis.port}")
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
        fun load(path: Path): PluginConfiguration {
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
        private fun parse(content: String): PluginConfiguration {
            val mapper = ObjectMapper(YAMLFactory())
            mapper.registerModule(KotlinModule())

            return mapper.readValue(content, PluginConfiguration::class.java)
        }
    }
}