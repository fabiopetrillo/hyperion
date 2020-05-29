package nl.tudelft.hyperion.pipeline.pathextractor

import com.fasterxml.jackson.annotation.JsonProperty
import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration

/**
 * Configuration for field extraction
 * @param field the field containing the package name
 * @param relativePathFromSource the path from the source to the package
 * @param postfix the extension of the class (e.g. .java, .kt, etc.)
 * @Param pipeline configuration for the abstract plugin
 */
data class Configuration(
    val field: String,
    @JsonProperty("relative-source-path")
    val relativePathFromSource: String,
    val postfix: String,
    val pipeline: PipelinePluginConfiguration
)
