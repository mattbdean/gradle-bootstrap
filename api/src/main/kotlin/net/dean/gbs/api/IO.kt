package net.dean.gbs.api

import java.nio.file.Path
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.charset.StandardCharsets
import java.nio.charset.Charset

public class Exporter(public val charset: Charset = StandardCharsets.UTF_8) {

    public fun export(project: Project, base: Path, files: Map<String, List<String>>) {
        Files.createDirectories(base)
        for (path in project.directoriesToCreate) {
            Files.createDirectories(relativePath(base, path))
        }

        for ((fileName, contents) in files) {
            Files.write(relativePath(base, fileName), contents, charset)
        }
    }

    private fun relativePath(base: Path, other: String, vararg others: String): Path {
        return Paths.get(base.normalize().toString(), other, *others)
    }
}

