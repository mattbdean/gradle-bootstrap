package net.dean.gbs.api.io

import java.nio.file.Path
import net.dean.gbs.api.models.Project
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder

public object GitHelper {
    public fun initialize(root: Path, upstream: String) {
        val dir = root.toFile()
        val gitDir = relativePath(root, ".git").toFile()

        Git.init()
                .setDirectory(dir)
                .call();

        val repo = FileRepositoryBuilder.create(gitDir)
        val config = repo.getConfig()
        config.setString("remote", "origin", "url", upstream)
        config.save()
    }
}
