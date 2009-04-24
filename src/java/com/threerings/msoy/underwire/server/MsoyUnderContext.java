//
// $Id$

package com.threerings.msoy.underwire.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.PersistenceContext;
import com.samskivert.servlet.IndiscriminateSiteIdentifier;

import com.threerings.msoy.underwire.server.MsoyGameActionHandler;
import com.threerings.msoy.underwire.server.MsoyGameInfoProvider;
import com.threerings.underwire.server.UnderContext;
import com.threerings.underwire.server.persist.UnderwireRepository;

/**
 * Wires {@link UnderContext} up for Guice injection.
 */
@Singleton
public class MsoyUnderContext extends UnderContext
{
    @Inject public MsoyUnderContext (MsoyGameInfoProvider info, MsoyGameActionHandler action,
                                     MsoyUnderwireRepository urepo)
    {
        super(new IndiscriminateSiteIdentifier(), info, action, urepo);
    }

    @Singleton
    protected static class MsoyUnderwireRepository extends UnderwireRepository
    {
        @Inject public MsoyUnderwireRepository (PersistenceContext perCtx) {
            super(perCtx);
        }
    }
}
