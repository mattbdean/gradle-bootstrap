package net.dean.gbs.api.io

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File

public object GitHelper {
    public fun initialize(root: File): Repository {
        val gitDir = File(root, ".git")

        Git.init()
                .setDirectory(root)
                .call();

        return FileRepositoryBuilder.create(gitDir)

    }

    public fun setUpstream(repo: Repository, url: String) {
        val config = repo.config
        config.setString("remote", "origin", "url", url)
        config.save()
    }
}
