//
// $Id$

package com.threerings.msoy.server.persist;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;

/**
 * A persistent object repository that inter-operates with Hibernate.
 */
public abstract class HibernateRepository
{
    /**
     * Database operations should be encapsulated in instances of this class
     * and then provided to the repository for invocation. This allows the
     * repository to manage transaction commits for you as well as for it to
     * automatically retry an operation if the connection failed for some
     * transient reason.
     */
    public interface Operation<V>
    {
        /**
         * Invokes code that performs one or more database operations, all of
         * which will be encapsulated in a single unit of work (which can be
         * retried in the event of a transient failure).
         */
	public V invoke (Session session);
    }

    /**
     * Like {@link Operation<V>} but for operations that return no value.
     */
    public abstract class VoidOperation implements Operation<Object>
    {
        /**
         * Invokes code that performs one or more database operations, all of
         * which will be encapsulated in a single unit of work (which can be
         * retried in the event of a transient failure).
         */
	public abstract void invokeVoid (Session session);

        // from interface Operation<Object>
        public Object invoke (Session session) {
            invokeVoid(session);
            return null;
        }
    }

    /**
     * Instructs the repository to wire all of its persistent objects into the
     * supplied configuration.
     */
    public abstract void configure (AnnotationConfiguration config);

    /**
     * Configures this repository with its session factory.
     */
    public void init (SessionFactory factory)
    {
        _factory = factory;
    }

    /**
     * Executes the supplied operation followed by a commit of the transaction
     * unless a runtime error occurs, in which case a rollback is executed.
     *
     * @return whatever value is returned by the invoked operation.
     */
    protected <V> V execute (Operation<V> op)
    {
        Session session = _factory.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            return op.invoke(session);

        } catch (RuntimeException e) {
            if (tx != null) {
                tx.rollback();
            }
            // TODO: auto-retry on transient exceptions (maybe Hibernate
            // automatically does this?)
            throw e;

        } finally {
            if (tx != null) {
                tx.commit();
            }
            session.close();
        }
    }

    /** Provides access to persistent objects. */
    protected SessionFactory _factory;
}
