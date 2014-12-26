package net.dean.gbs.api

public trait ModularComponent {
    public fun configureOnto(project: Project)
}

public trait ModularGradleComponent : ModularComponent {
    public fun configureOnto(build: GradleBuild)

    override fun configureOnto(project: Project) {
        configureOnto(project.build)
    }
}

public trait Framework : ModularGradleComponent {
    public val deps: Array<Dependency>

    public override fun configureOnto(build: GradleBuild) {
        // Only add the dependency if it hasn't been specified
        for (dep in deps) {
            build.projectContext.add(dep)
        }
    }

    public fun deconfigureFrom(build: GradleBuild) {
        for (dep in deps) {
            build.projectContext.remove(dep)
        }
    }
}

