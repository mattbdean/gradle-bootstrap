package net.dean.gbs.api.models

public interface Component<T> {
    public fun configureOnto(it: T)
}

public interface ProjectComponent : Component<Project> {
    public override fun configureOnto(it: Project)
}

public interface ModularGradleComponent : ProjectComponent {
    public fun configureOnto(build: GradleBuild)

    override fun configureOnto(it: Project) {
        configureOnto(it.build)
    }
}

public interface Framework : ModularGradleComponent {
    public val deps: Array<Dependency>

    public override fun configureOnto(build: GradleBuild) {
        // Only add the dependency if it hasn't been specified
        for (dep in deps) {
            build.projectContext.add(dep)
        }
    }

    public fun deconfigureFrom(build: GradleBuild) {
        for (dep in deps)
            build.projectContext.remove(dep)
    }
}

