package net.dean.gbs.api.io

import java.nio.file.Path
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.lib.Repository

public object GitHelper {
    public fun initialize(root: Path): Repository {
        val dir = root.toFile()
        val gitDir = relativePath(root, ".git").toFile()

        Git.init()
                .setDirectory(dir)
                .call();

        return FileRepositoryBuilder.create(gitDir)

    }

    public fun setUpstream(repo: Repository, url: String) {
        val config = repo.getConfig()
        config.setString("remote", "origin", "url", url)
        config.save()
    }
}
