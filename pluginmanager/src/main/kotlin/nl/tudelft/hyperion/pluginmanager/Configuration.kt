package nl.tudelft.hyperion.pluginmanager

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.nio.file.Files
import java.nio.file.Path

/**
 * Configuration for each plugin in the pipeline
 *
 * @id Unique identifier of a pipeline plugin
 * @host Host address to which other plugins should bind to given plugin
 */
data class PipelinePluginConfig(
    val id: String,
    val host: String
)

/**
 * Configuration used by the :PluginManager: to establish locations and order of plugins
 *
 * @host The host address to which the :PluginManager: REP socket should bind
 * @plugins List of :PipelinePlugin:, plugins will be loaded in pipeline in this order
 */
data class Configuration(
    val host: String,
    val plugins: List<PipelinePluginConfig>
) {
    fun verify() {
        if (plugins.size < 2) {
            throw IllegalArgumentException("At least 2 plugins should be defined, got ${plugins.size}")
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
        private fun parse(content: String): Configuration {
            val mapper = ObjectMapper(YAMLFactory())
            mapper.registerModule(KotlinModule())

            return mapper.readValue(content, Configuration::class.java)
        }
    }
}
