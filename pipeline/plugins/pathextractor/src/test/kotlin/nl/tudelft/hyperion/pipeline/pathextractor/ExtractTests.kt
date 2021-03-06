package nl.tudelft.hyperion.pipeline.pathextractor

import com.fasterxml.jackson.databind.ObjectMapper
import nl.tudelft.hyperion.pipeline.PipelinePluginConfiguration
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class ExtractTests {
    private val mapper = ObjectMapper()

    @Test
    fun testRenameLogLine() {
        val config = Configuration(
            "log4j_file",
            "src/main/java",
            ".java",
            PipelinePluginConfiguration("pathExtractor", "1.2.3.4:4567")
        )

        val input = """{ "log4j_file" :  "com.sap.enterprises.server.impl.TransportationService" }"""
        val expected = """{"log4j_file":"src/main/java/com/sap/enterprises/server/impl/TransportationService.java"}"""

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(extractPath(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun testParentNull() {
        val config = Configuration(
            "nonExisting",
            "src/main/java",
            ".java",
            PipelinePluginConfiguration("pathExtractor", "1.2.3.4:4567")
        )

        val input = """{ "log4j_file" :  "com.sap.enterprises.server.impl.TransportationService" }"""

        val treeExpected = mapper.readTree(input)
        val treeActual = mapper.readTree(extractPath(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun testFieldNotString() {
        val config = Configuration(
            "log4j_file",
            "src/main/java",
            ".java",
            PipelinePluginConfiguration("pathExtractor", "1.2.3.4:4567")
        )

        val input = """{"log4j_file":true}"""
        val actual = extractPath(input, config)

        Assertions.assertEquals(input, actual)
    }

    @Test
    fun testInputNotJSONObject() {
        val config = Configuration(
            "log4j_file",
            "src/main/java",
            ".java",
            PipelinePluginConfiguration("pathExtractor", "1.2.3.4:4567")
        )

        val input = """true"""
        val actual = extractPath(input, config)

        Assertions.assertEquals(input, actual)
    }

    @Test
    fun testKotlinNamingSupport() {
        val config = Configuration(
            "log4j_file",
            "src/main/kotlin",
            ".kt",
            PipelinePluginConfiguration("pathExtractor", "1.2.3.4:4567")
        )

        val input = """{ "log4j_file" :  "com.sap.enterprises.server.impl.TransportationServiceKt" }"""
        val expected = """{"log4j_file":"src/main/kotlin/com/sap/enterprises/server/impl/TransportationService.kt"}"""

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(extractPath(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun testNestedField() {
        val config = Configuration(
            "location.file",
            "src/main/kotlin",
            ".java",
            PipelinePluginConfiguration("pathExtractor", "1.2.3.4:4567")
        )

        val input = """{ "location" :  { "file" : "com.sap.enterprises.server.impl.TransportationService" } }"""
        val expected = """{"location": { "file" : 
            |"src/main/kotlin/com/sap/enterprises/server/impl/TransportationService.java"} }""".trimMargin()

        val treeExpected = mapper.readTree(expected)
        val treeActual = mapper.readTree(extractPath(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }

    @Test
    fun `Test path non existent`() {
        val config = Configuration(
            "location.nonExistent",
            "src/main/kotlin",
            ".java",
            PipelinePluginConfiguration("pathExtractor", "1.2.3.4:4567")
        )

        val input = """{ "location" :  { "file" : "com.sap.enterprises.server.impl.TransportationService" } }"""

        val treeExpected = mapper.readTree(input)
        val treeActual = mapper.readTree(extractPath(input, config))

        Assertions.assertEquals(treeExpected, treeActual)
    }
}
