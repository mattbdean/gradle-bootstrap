package net.dean.gbs.api.io

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream
import org.apache.commons.compress.utils.IOUtils
import java.io.*
import java.nio.file.FileAlreadyExistsException

/**
 * A singleton class to help with the creation of zip archives. Adapted from
 * http://developer-tips.hubpages.com/hub/Zipping-and-Unzipping-Nested-Directories-in-Java-using-Apache-Commons-Compress
 */
public object ZipHelper {
    /**
     * Creates a zip file. The input directory must be an existing directory and the output file must not exist.
     */
    public fun createZip(inputDir: File, outputFile: File) {
        // Assert that the output file does not already exist
        if (outputFile.exists()) throw FileAlreadyExistsException(outputFile.absolutePath)
        // Assert that the root path is not a file
        if (inputDir.isFile) throw IllegalArgumentException("Input directory (${inputDir.absolutePath}) is a file")
        // Assert that the root path is actually a directory
        if (!inputDir.isDirectory) throw FileNotFoundException(outputFile.toString())

        val zipOut = ZipArchiveOutputStream(BufferedOutputStream(FileOutputStream(outputFile.absolutePath)))

        addFileToZip(zipOut, inputDir.toString(), "")
        zipOut.close()
    }

    private fun addFileToZip(zipOut: ZipArchiveOutputStream, path: String, base: String) {
        val f = File(path)
        val entryName = base + f.name
        val zipEntry = ZipArchiveEntry(f, entryName)
        // Write to the ZIP manifest
        zipOut.putArchiveEntry(zipEntry)

        // Write the actual file
        if (f.isFile) {
            val fileInput = FileInputStream(f)
            IOUtils.copy(fileInput, zipOut)
            zipOut.closeArchiveEntry()
            IOUtils.closeQuietly(fileInput)
        } else {
            zipOut.closeArchiveEntry()
            val children = f.listFiles()

            if (children != null)
                for (child in children)
                    addFileToZip(zipOut, child.absolutePath, entryName + "/")
        }
    }
}

