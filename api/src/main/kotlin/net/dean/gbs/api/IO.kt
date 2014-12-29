package net.dean.gbs.api

import java.nio.file.Path
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.charset.StandardCharsets
import java.nio.charset.Charset
import java.nio.file.FileAlreadyExistsException
import java.io.FileNotFoundException
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.io.File
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import java.io.FileInputStream
import org.apache.commons.compress.utils.IOUtils

public class Exporter(public val charset: Charset = StandardCharsets.UTF_8) {
    class object {
        private val renderer = ProjectRenderer()

        public fun relativePath(base: Path, other: String, vararg others: String): Path {
            return Paths.get(base.normalize().toString(), other, *others).normalize()
        }
    }

    /**
     * Renders and writes a Project into the given base path.
     */
    public fun export(project: Project, base: Path) {
        val files = renderer.render(project)

        Files.createDirectories(base)
        for (path in project.directoriesToCreate) {
            Files.createDirectories(relativePath(base, path))
        }

        for ((fileName, contents) in files) {
            Files.write(relativePath(base, fileName), contents, charset)
        }
    }

    /**
     * Creates a zip archive. [root] must be an existing directory, and [out] must not exist.
     */
    public fun zip(root: Path, out: Path) {
        ZipHelper.createZip(root, out)
    }
}

private object ZipHelper {
    public fun createZip(inputDir: Path, outputFile: Path) {
        // Assert that the output file does not already exist
        if (Files.exists(outputFile)) throw FileAlreadyExistsException(outputFile.toString())
        // Assert that the root path is not a file
        if (Files.isRegularFile(inputDir)) throw IllegalArgumentException("Input directory ($inputDir) is a file")
        // Assert that the root path is actually a directory
        if (!Files.isDirectory(inputDir)) throw FileNotFoundException(outputFile.toString())

        val zipOut = ZipArchiveOutputStream(BufferedOutputStream(FileOutputStream(outputFile.toFile())))

        addFileToZip(zipOut, inputDir.toString(), "")
        zipOut.close()
    }

    private fun addFileToZip(zipOut: ZipArchiveOutputStream, path: String, base: String) {
        val f = File(path)
        val entryName = base + f.getName()
        val zipEntry = ZipArchiveEntry(f, entryName)
        // Write to the ZIP manifest
        zipOut.putArchiveEntry(zipEntry)

        // Write the actual file
        if (f.isFile()) {
            val fileInput = FileInputStream(f)
            IOUtils.copy(fileInput, zipOut)
            zipOut.closeArchiveEntry()
            IOUtils.closeQuietly(fileInput)
        } else {
            zipOut.closeArchiveEntry()
            val children = f.listFiles()

            if (children != null)
                for (child in children)
                    addFileToZip(zipOut, child.getAbsolutePath(), entryName + "/")
        }
    }
}

