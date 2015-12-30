package net.dean.gbs.web

import net.dean.gbs.api.io.ProjectRenderer
import net.dean.gbs.api.io.ZipHelper
import net.dean.gbs.api.io.delete
import net.dean.gbs.api.io.mkdirs
import net.dean.gbs.api.models.*
import net.dean.gbs.web.db.DataAccessObject
import net.dean.gbs.web.models.BuildStatus
import net.dean.gbs.web.models.ProjectModel
import org.apache.commons.io.FileUtils
import org.hibernate.HibernateException
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.context.internal.ManagedSessionContext
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileNotFoundException
import javax.ws.rs.core.StreamingOutput
import kotlin.collections.map
import kotlin.text.replace
import kotlin.text.toUpperCase

/**
 * This class provides a way to create zip files from project models. If sessionFactory is null, no session will be
 * bound to any Hibernate session context, causing errors if the DataAccessObject uses Hibernate. This is mostly for
 * testing purposes.
 */
public class ProjectBuilder(private val dao: DataAccessObject<ProjectModel>,
                            private val storageFolder: File,
                            private val sessionFactory: SessionFactory) {
    init {
        if (!storageFolder.exists()) {
            mkdirs(storageFolder)
        }
    }

    private val log = LoggerFactory.getLogger(javaClass)

    public fun enqueue(model: ProjectModel) {
        /**
         * Updates the build status if and only if the current build status is not the same as the one given
         */
        fun update(status: BuildStatus) {
            if (model.status.toUpperCase() != status.name) {
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
                mkdirs(projectPath)
                val project = toProject(model)

                // Create the project files
                renderer.render(project)
                val zipPath = getZipPath(model)

                // Make sure the zip file has an existing parent directory
                mkdirs(zipPath.parentFile)
                // Create the zip file
                ZipHelper.createZip(projectPath, zipPath)
                // Download is ready
                update(BuildStatus.READY)
            } catch (ex: Exception) {
                update(BuildStatus.ERRORED)
                log.error("Build errored", ex)
            } finally {
                // Remove the unzipped project directory
                delete(projectPath.toPath())
            }
        }
    }

    private fun commitTransaction(session: Session) {
        log.info("Commiting transaction ${session.transaction}")
        session.transaction.commit()
    }

    private fun rollbackTransaction(session: Session) {
        log.info("Rolling back transaction ${session.transaction}")
        val trx = session.transaction
        if (trx != null && trx.isActive)
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
            session!!.transaction.begin()
            ManagedSessionContext.bind(session)

            // Mess with the database
            doWork()

            // Commit the transaction
            commitTransaction(session)

            // All operations completed successfully
            onSuccess()
        } catch (ex: HibernateException) {
            rollbackTransaction(session!!)
            onFail(ex)
        } catch (ex: Exception) {
            onFail(ex)
        } finally {
            if (session != null) {
                // Clean up the session
                session.close()
                ManagedSessionContext.unbind(sessionFactory)
            }
        }
    }

    public fun downloadAvailable(project: ProjectModel): Boolean = getZipPath(project).isFile

    public fun stream(project: ProjectModel): Pair<String, StreamingOutput> {
        if (!downloadAvailable(project))
            throw FileNotFoundException(getZipPath(project).toString())

        val file = getZipPath(project)

        return file.name to StreamingOutput { output -> FileUtils.copyFile(file, output) }
    }

    /** Returns the name of the immediate subdirectory of [storageFolder] where files for this project will be placed. */
    private fun getBaseName(project: ProjectModel): String = project.id.toString()
    /** Returns the directory in which the project's file structure will be created */
    private fun getUnzippedPath(project: ProjectModel): File =
            File(File(storageFolder, getBaseName(project)), filterFilename(project.name))
    /** Returns the location of a project's zip file */
    private fun getZipPath(project: ProjectModel): File =
            File(File(storageFolder, getBaseName(project)), filterFilename(project.name) + ".zip")
    /** Replaces all characters except characters A-Z (case insensitive), 0-9, periods, and hyphens */
    private fun filterFilename(name: String): String = name.replace("[^a-zA-Z0-9.-]", "_");

    /** Creates a Project out of the given ProjectModel */
    private fun toProject(model: ProjectModel): Project {
        val proj = Project(model.name!!,
                model.group!!,
                model.version!!,
                model.git.url,
                model.git.isInit,
                model.languages.map { Language.valueOf(it.toUpperCase()) })
        if (model.testingFramework != null) proj.build.testing = TestingFramework.valueOf(model.testingFramework!!.toUpperCase())
        if (model.loggingFramework != null) proj.build.logging = LoggingFramework.valueOf(model.loggingFramework!!.toUpperCase())
        if (model.license != null) proj.license = License.valueOf(model.license!!.toUpperCase())
        return proj
    }
}

