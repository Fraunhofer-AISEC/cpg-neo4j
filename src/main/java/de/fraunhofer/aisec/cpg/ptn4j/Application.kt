package de.fraunhofer.aisec.cpg.ptn4j

import de.fraunhofer.aisec.cpg.TranslationConfiguration
import de.fraunhofer.aisec.cpg.TranslationManager
import de.fraunhofer.aisec.cpg.TranslationResult
import de.fraunhofer.aisec.cpg.graph.Node
import de.fraunhofer.aisec.cpg.helpers.SubgraphWalker
import org.neo4j.ogm.config.Configuration
import org.neo4j.ogm.exception.ConnectionException
import org.neo4j.ogm.session.Session
import org.neo4j.ogm.session.SessionFactory
import java.io.File
import java.net.ConnectException
import java.nio.file.Paths
import java.util.*

object Application {
    private const val TIME_BETWEEN_CONNECTION_TRIES = 2000
    private const val MAX_COUNT_OF_FAILS = 10
    private const val URI = "bolt://localhost"
    private const val AUTO_INDEX = "none"
    private const val VERIFY_CONNECTION = true
    private const val NEO4J_USERNAME = "neo4j"
    private const val NEO4J_PASSWORD = "neo4j"
    private const val START_DOCKER = false

    /**
     * Pushes the whole translationResult to the neo4j db.
     *
     * @param translationResult, not null
     * @throws InterruptedException, if the thread is interrupted while it try´s to connect to the
     * neo4j db.
     * @throws ConnectException, if there is no connection to bolt://localhost:7687 possible
     */
    @JvmStatic
    @Throws(InterruptedException::class, ConnectException::class)
    fun pushToNeo4j(translationResult: TranslationResult) {
        Objects.requireNonNull(translationResult)
        val sessions = connect()
        val session = sessions.getT()
        session.beginTransaction().use { transaction ->
            pushNodes(translationResult, session)
            transaction.commit()
        }
        close(session, sessions.getU())
    }

    /**
     * Connects to the neo4j db.
     *
     * @return a Pair of Optionals of the Session and the SessionFactory, if it is possible to connect
     * to neo4j. If it is not possible, the return value is a Pair of empty Optionals.
     * @throws InterruptedException, if the thread is interrupted while it try´s to connect to the
     * neo4j db.
     * @throws ConnectException, if there is no connection to bolt://localhost:7687 possible
     */
    @Throws(InterruptedException::class, ConnectException::class)
    private fun connect(): Pair<Session, SessionFactory> {
        var fails = 0
        var sessionFactory: SessionFactory? = null
        var session: Session? = null
        while (session == null && fails < MAX_COUNT_OF_FAILS) {
            try {
                val configuration = Configuration.Builder()
                        .uri(URI)
                        .autoIndex(AUTO_INDEX)
                        .credentials(NEO4J_USERNAME, NEO4J_PASSWORD)
                        .verifyConnection(VERIFY_CONNECTION)
                        .build()
                sessionFactory = SessionFactory(configuration, "de.fraunhofer.aisec.cpg.graph")
                session = sessionFactory.openSession()
            } catch (ex: ConnectionException) {
                sessionFactory = null
                fails++
                System.err.println(
                        "Unable to connect to localhost:7687, "
                                + "ensure the database is running and that "
                                + "there is a working network connection to it.")
                Thread.sleep(TIME_BETWEEN_CONNECTION_TRIES.toLong())
            }
        }
        assert(fails <= MAX_COUNT_OF_FAILS)
        if (session == null || sessionFactory == null) {
            throw ConnectException("Unable to connect to localhost:7687")
        }
        return Pair(session, sessionFactory)
    }

    /**
     * PushNodes to the neo4j db.
     *
     * @param translationResult, not null,
     * @param session, not null,
     */
    private fun pushNodes(translationResult: TranslationResult, session: Session) {
        val translationUnitDeclarations = translationResult.translationUnits
        // Only to remove duplicated elements in the translationUnitDeclarations
        // This "Bug" will be solved in future releases of the cpg
        val nodes: Set<Node> = HashSet<Node>(translationUnitDeclarations)
        for (elem in nodes) {
            for (child in SubgraphWalker.flattenAST(elem)) {
                session.save(child, 1)
            }
        }
    }

    /**
     * Clears the session and closes the sessionFactory
     *
     * @param session, not null
     * @param sessionFactory, not null
     */
    private fun close(session: Session, sessionFactory: SessionFactory) {
        session.clear()
        sessionFactory.close()
    }

    /**
     * @param args
     * @throws IllegalArgumentException, if there was no arguments provided, or the path does not
     * point to a file, is a directory or point to a hidden file or the paths does not have the
     * same top level path
     * @throws InterruptedException, if the thread is interrupted while it try´s to connect to the
     * neo4j db.
     * @throws ConnectException, if there is no connection to bolt://localhost:7687 possible
     */
    @Throws(Exception::class, ConnectException::class, IllegalArgumentException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.isNotEmpty()) { "A path is required." }
        val files = arrayOfNulls<File>(args.size)
        var topLevel: File? = null
        for (index in args.indices) {
            val path = Paths.get(args[index]).toAbsolutePath().normalize()
            val file = File(path.toString())
            require(!(!file.exists() || file.isDirectory || file.isHidden)) { "Please use a correct path. It was: $path" }
            if (topLevel == null) {
                topLevel = file.parentFile
            } else {
                require(topLevel.toString() == file.parentFile.toString()) { "All files should have the same top level path." }
            }
            files[index] = file
        }

        if (START_DOCKER) {
            val utils = DockerUtils()
            utils.startDocker()
        }

        val translationConfiguration = TranslationConfiguration.builder()
                .sourceLocations(*files)
                .topLevel(topLevel!!)
                .defaultPasses()
                .debugParser(true)
                .build()

        val translationManager = TranslationManager.builder().config(translationConfiguration).build()

        val translationResult = translationManager.analyze().get()

        pushToNeo4j(translationResult)
    }
}