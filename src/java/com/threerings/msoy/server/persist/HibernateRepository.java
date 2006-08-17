//
// $Id$

package com.threerings.msoy.server.persist;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;

/**
 * A persistent object repository that inter-operates with Hibernate.
 */
public abstract class HibernateRepository
{
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

    /** Provides access to persistent objects. */
    protected SessionFactory _factory;
}
