package com.threerings.msoy.bureau.client {

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.dobj.DObjectManager;

/**
 * Abstracts everything about a world server client that a bureau agent may need to know.
 * Implementations are managed by {@link WindowDirector}.
 */
public interface Window
{
    /** Retrieve a service provided by this world server. */
    function requireService (sclass :Class) :InvocationService;

    /** Retrieve the client for use in making service calls on services returned by
     * <code>requireService</code>. */
    function getClient () :Client;

    /** Retrieve the distributed object manager of this world server's objects. */
    function getDObjectManager () :DObjectManager;
}

}
