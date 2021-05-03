package de.fraunhofer.aisec.cpg_vis_neo4j

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.declarations.FunctionDeclaration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File
import java.net.ConnectException
import java.nio.file.Paths
import java.util.concurrent.ExecutionException

class ApplicationTest {

    @Test
    @Throws(InterruptedException::class)
    fun testPush() {
        val application = Application()

        application.pushToNeo4j(translationResult!!)

        val sessionAndSessionFactoryPair = application.connect()

        val session = sessionAndSessionFactoryPair.first
        session.beginTransaction().use { transaction ->

            val functions = session.loadAll(FunctionDeclaration::class.java)
            assertNotNull(functions)

            assertEquals(2, functions.size)

            transaction.commit()
        }

        session.clear()
        sessionAndSessionFactoryPair.second.close()
    }

    companion object {
        private var translationResult: TranslationResult? = null

        @BeforeAll
        @JvmStatic
        @Throws(ExecutionException::class, InterruptedException::class)
        private fun init() {
            val topLevel = Paths.get("src").resolve("test").resolve("resources").toAbsolutePath()
            val path = topLevel.resolve("Implementor1.java").toAbsolutePath()
            val file = File(path.toString())
            assert(file.exists() && !file.isDirectory && !file.isHidden)
            val translationConfiguration = TranslationConfiguration.builder()
                .sourceLocations(file)
                .topLevel(topLevel.toFile())
                .defaultPasses()
                .defaultLanguages()
                .debugParser(true)
                .build()
            val translationManager = TranslationManager.builder().config(translationConfiguration).build()
            translationResult = translationManager.analyze().get()
        }
    }
}