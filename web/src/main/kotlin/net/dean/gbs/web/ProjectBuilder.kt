package net.dean.gbs.web

import net.dean.gbs.web.models.ProjectModel
import java.nio.file.Path
import java.nio.file.Files
import net.dean.gbs.api.io.relativePath
import net.dean.gbs.web.models.BuildStatus
import net.dean.gbs.api.models.Project
import net.dean.gbs.api.models.TestingFramework
import net.dean.gbs.api.models.LoggingFramework
import net.dean.gbs.api.models.License
import javax.ws.rs.core.StreamingOutput
import java.io.OutputStream
import org.slf4j.LoggerFactory
import net.dean.gbs.api.io.ProjectRenderer
import net.dean.gbs.api.io.ZipHelper
import net.dean.gbs.api.io.delete
import org.hibernate.SessionFactory
import org.hibernate.context.internal.ManagedSessionContext
import org.hibernate.Session
import net.dean.gbs.web.db.DataAccessObject
import net.dean.gbs.api.models.Language
import java.io.FileNotFoundException

/**
 * This class provides a way to create zip files from project models. If sessionFactory is null, no session will be
 * bound to any Hibernate session context, causing errors if the DataAccessObject uses Hibernate. This is mostly for
 * testing purposes.
 */
public class ProjectBuilder(private val dao: DataAccessObject<ProjectModel>,
                            private val storageFolder: Path,
                            private val sessionFactory: SessionFactory) {
    {
        if (!Files.exists(storageFolder)) {
            Files.createDirectories(storageFolder)
        }
    }

    private val log = LoggerFactory.getLogger(javaClass)

    public fun enqueue(model: ProjectModel) {
        /**
         * Updates the build status if and only if the current build status is not the same as the one given
         */
        fun update(status: BuildStatus) {
            if (model.getStatus().toUpperCase() != status.name()) {
                model.setStatus(status)
                dao.update(model)
            }
        }

        // This initial session is done first because we want to have the model in the database so when the other
        // (asynchronous) session is created, we can roll it back to this point if necessary

        withSession {
            dao.insert(model)
            update(BuildStatus.BUILDING)
            val projectPath = getUnzippedPath(model)
            try {
                val renderer = ProjectRenderer(projectPath)
                // Ensure we have somewhere to work
                Files.createDirectories(projectPath)
                val project = toProject(model)

                // Create the project files
                renderer.render(project)
                val zipPath = getZipPath(model)

                // Make sure the zip file has an existing parent directory
                Files.createDirectories(zipPath.getParent())
                // Create the zip file
                ZipHelper.createZip(projectPath, zipPath)
                // Download is ready
                update(BuildStatus.READY)
            } catch (ex: Exception) {
                update(BuildStatus.ERRORED)
                log.error("Build errored", ex)
            } finally {
                // Remove the unzipped project directory
                delete(projectPath)
            }
        }
    }

    private fun commitTransaction(session: Session) {
        log.info("Commiting transaction ${session.getTransaction()}")
        session.getTransaction().commit()
    }

    private fun rollbackTransaction(session: Session) {
        log.info("Rolling back transaction ${session.getTransaction()}")
        val trx = session.getTransaction()
        if (trx != null && trx.isActive())
            trx.rollback()
    }

    /**
     * Executes doWork() within the context of a bound Hibernate session. An an exception is thrown, then the transaction
     * is rolled back. If everything completes successfully, then the transaction is committed.
     */
    private fun withSession(onFail: (Exception) -> Unit = { log.error("Build failed", it) },
                            onSuccess: () -> Unit = {},
                            doWork: () -> Unit) {
        var session: Session? = null
        try {
            // Open a session
            session = sessionFactory.openSession()
            session!!.getTransaction().begin()
            ManagedSessionContext.bind(session)

            // Mess with the database
            doWork()

            // Commit the transaction
            commitTransaction(session!!)

            // All operations completed successfully
            onSuccess()
        } catch (ex: Exception) {
            rollbackTransaction(session!!)
            onFail(ex)
        } finally {
            if (session != null) {
                // Clean up the session
                session!!.close()
                ManagedSessionContext.unbind(sessionFactory)
            }
        }
    }

    public fun downloadAvailable(project: ProjectModel): Boolean = Files.isRegularFile(getZipPath(project))

    public fun stream(project: ProjectModel): Pair<String, StreamingOutput> {
        if (!downloadAvailable(project))
            throw FileNotFoundException(getZipPath(project).toString())

        val file = getZipPath(project)

        return file.getFileName().toString() to object: StreamingOutput {
            override fun write(output: OutputStream) {
                Files.copy(file, output)
            }
        }
    }

    /** Returns the name of the immediate subdirectory of [storageFolder] where files for this project will be placed. */
    private fun getBaseName(project: ProjectModel): String = project.getId().toString()
    /** Returns the directory in which the project's file structure will be created */
    private fun getUnzippedPath(project: ProjectModel): Path =
            relativePath(storageFolder, getBaseName(project), filterFilename(project.getName()))
    /** Returns the location of a project's zip file */
    private fun getZipPath(project: ProjectModel): Path =
            relativePath(storageFolder, getBaseName(project), filterFilename(project.getName()) + ".zip")
    /** Replaces all characters except characters A-Z (case insensitive), 0-9, periods, and hyphens */
    private fun filterFilename(name: String): String = name.replaceAll("[^a-zA-Z0-9.-]", "_");

    /** Creates a Project out of the given ProjectModel */
    private fun toProject(model: ProjectModel): Project {
        val proj = Project(model.getName()!!, model.getGroup()!!, model.getVersion()!!, model.getLanguages().map { Language.valueOf(it.toUpperCase()) })
        if (model.getTestingFramework() != null) proj.build.testing = TestingFramework.valueOf(model.getTestingFramework()!!.toUpperCase())
        if (model.getLoggingFramework() != null) proj.build.logging = LoggingFramework.valueOf(model.getLoggingFramework()!!.toUpperCase())
        if (model.getLicense() != null) proj.license = License.valueOf(model.getLicense()!!.toUpperCase())
        return proj
    }
}

