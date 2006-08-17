//
// $Id$

package com.threerings.msoy.person.server.persist;

import org.hibernate.cfg.AnnotationConfiguration;

import com.threerings.msoy.server.persist.HibernateRepository;

/**
 * Manages the persistent information associated with a member's person page.
 */
public class PersonPageRepository extends HibernateRepository
{
    @Override // from HibernateRepository
    public void configure (AnnotationConfiguration config)
    {
        config.addAnnotatedClass(PersonPageRecord.class);
        config.addAnnotatedClass(BlurbRecord.class);
    }
}
