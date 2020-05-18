package nl.tudelft.hyperion.pluginmanager

import com.fasterxml.jackson.databind.ObjectMapper
import org.yaml.snakeyaml.introspector.MissingProperty
import org.zeromq.SocketType
import org.zeromq.ZMQ

class PluginManager(config: Configuration) {

    private val host = config.host
    private val plugins = config.plugins

    private val logger = mu.KotlinLogging.logger {}

    init {
        logger.info { "Initialized PluginManager" }
    }

    fun launchListener() {
        val context = ZMQ.context(1)
        val responder = context.socket(SocketType.REP)

        responder.bind(host)
        logger.info { "Connected ZMQ reply to $host" }
        logger.info { "Launching REQ/REP loop" }
        while (!Thread.currentThread().isInterrupted) {
            //  Wait for next request from client
            val request = responder.recvStr(0)
            logger.debug { "Received: [$request]" }

            handleRegister(request, responder)
        }

        // cleanup
        responder.close()
        context.term()
    }

    @Suppress("TooGenericExceptionCaught")
    fun handleRegister(request: String, res: ZMQ.Socket) {
        val reqMap: MutableMap<*, *> = try {
            verifyRequest(request)
        } catch (ex: Exception) {
            logger.error { ex }
            res.send("Invalid Request")
            return
        }

        logger.info { "Received register request: [$reqMap]" }
        val plugin = reqMap["id"].toString()
        when (reqMap["type"].toString()) {
            "push" -> res.send(registerPush(plugin))
            "pull" -> res.send(registerPull(plugin))
        }
    }

    private fun verifyRequest(request: String): MutableMap<*, *> {
        val mapper = ObjectMapper()
        val map = mapper.readValue(
            request,
            MutableMap::class.java
        )

        if (!map.containsKey("id")) {
            throw NoSuchFieldException("Request should contain id field but did not")
        } else if (!map.containsKey("type")) {
            throw NoSuchFieldException("Request should include type field but did not")
        } else if (map["type"] != "push" && map["type"] != "pull") {
            throw IllegalArgumentException("Incorrect request type ${map["type"]} received")
        }

        // check whether the plugin that tries to register is in the config
        getPlugin(map["id"].toString())
        return map
    }

    private fun registerPush(pluginName: String): String {
        return """{"isBind":"true","host":"${getPlugin(pluginName).host}"}"""
    }

    private fun registerPull(pluginName: String): String {
        return """{"isBind":"false","host":"${previousPlugin(pluginName).host}"}"""
    }

    private fun getPlugin(pluginName: String): PipelinePluginConfig {
        return plugins.find { it.id == pluginName } ?: throw IllegalArgumentException(
            "Plugin $pluginName does not exist in current pipeline"
        )
    }

    private fun previousPlugin(pluginName: String): PipelinePluginConfig {
        for ((index, plugin) in plugins.withIndex()) {
            if (plugin.id == pluginName) {
                return plugins[index - 1]
            }
        }
        throw IllegalArgumentException("Plugin $pluginName does not exist in current pipeline")
    }
}
