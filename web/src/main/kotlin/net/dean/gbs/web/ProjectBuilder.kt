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
import net.dean.gbs.web.db.ProjectDao
import java.util.concurrent.Executors
import org.slf4j.LoggerFactory
import net.dean.gbs.api.io.ProjectRenderer
import net.dean.gbs.api.io.ZipHelper
import net.dean.gbs.api.io.delete

/**
 * This class provides a way to create zip files from project models
 */
public class ProjectBuilder(private val dao: ProjectDao, private val storageFolder: Path) {
    {
        if (!Files.exists(storageFolder)) {
            Files.createDirectories(storageFolder)
        }
    }

    private val queue = Executors.newCachedThreadPool()
    private val log = LoggerFactory.getLogger(javaClass)

    public fun enqueue(model: ProjectModel) {
        fun update(status: BuildStatus) {
            model.setStatus(status)
            dao.update(model)
        }

        // We don't need to update the status to ENQUEUED because that was already done by ProjectResource.create()
        queue.submit(object: Runnable {
            override fun run() {
                update(BuildStatus.BUILDING)
                val projectPath = getUnzippedPath(model)
                try {
                    val renderer = ProjectRenderer(projectPath)
                    Files.createDirectories(projectPath)
                    val project = toProject(model)
                    renderer.render(project)
                    val zipPath = getZipPath(model)
                    Files.createDirectories(zipPath.getParent())
                    ZipHelper.createZip(projectPath, zipPath)
                    update(BuildStatus.READY)
                } catch (ex: Exception) {
                    update(BuildStatus.ERRORED)
                    log.error("Build errored", ex)
                } finally {
                    delete(projectPath)
                }
            }
        })
    }

    public fun downloadAvailable(project: ProjectModel): Boolean =
        Files.isRegularFile(getZipPath(project))

    public fun download(project: ProjectModel): StreamingOutput {
        if (!downloadAvailable(project))
            throw IllegalStateException("Download not ready yet")

        val file = getZipPath(project)

        return object: StreamingOutput {
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
        val proj = Project(model.getName()!!, model.getGroup()!!, model.getVersion()!!)
        if (model.getTestingFramework() != null) proj.build.testing = TestingFramework.valueOf(model.getTestingFramework()!!.toUpperCase())
        if (model.getLoggingFramework() != null) proj.build.logging = LoggingFramework.valueOf(model.getLoggingFramework()!!.toUpperCase())
        if (model.getLicense() != null) proj.license = License.valueOf(model.getLicense()!!.toUpperCase())
        for (lang in model.getLanguages()) {
            proj.add(lang)
        }
        return proj
    }
}

