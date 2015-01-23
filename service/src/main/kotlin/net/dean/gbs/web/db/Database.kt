package net.dean.gbs.web.db

import java.util.UUID
import net.dean.gbs.web.models.Model
import io.dropwizard.hibernate.AbstractDAO
import net.dean.gbs.web.models.ProjectModel
import org.hibernate.SessionFactory
import java.io.Serializable

/**
 * Provides a standard interface for retrieving models from the database
 *
 * M: Model type
 */
public trait DataAccessObject<M : Model> {
    public fun get(id: UUID): M?
    public fun getAll(): List<M>
    public fun insert(model: M)
    public fun update(model: M)
}

public open class BaseDao<M : Model>(sessionFactory: SessionFactory) : DataAccessObject<M>, AbstractDAO<M>(sessionFactory) {
    override fun get(id: UUID): M? = get(id : Serializable)
    override fun getAll(): List<M> {
        return list(criteria())
    }

    override fun insert(model: M) {
        currentSession().save(model)
    }

    override fun update(model: M) {
        currentSession().update(model)
    }
}

public class ProjectDao(sessionFactory: SessionFactory) :
        BaseDao<ProjectModel>(sessionFactory)

