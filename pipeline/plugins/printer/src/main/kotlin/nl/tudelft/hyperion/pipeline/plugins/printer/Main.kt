@file:JvmName("Main")

package nl.tudelft.hyperion.pipeline.plugins.printer

import nl.tudelft.hyperion.pipeline.runPipelinePlugin

/**
 * Use main to launch the printer plugin with the location of the config.yml specified as the first parameter.
 * Will execute [runPipelinePlugin] with the path supplied and the [PrinterPlugin].
 */
fun main(vararg args: String) = runPipelinePlugin(
    args[0],
    ::PrinterPlugin
)
