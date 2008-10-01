//
// $Id$

package com.threerings.msoy.bureau.server;

import com.threerings.presents.dobj.DEvent;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.InvocationResponseEvent;
import com.threerings.presents.server.PresentsClient;
import com.threerings.msoy.game.data.PlayerObject;

import static com.threerings.msoy.Log.log;

/**
 * Extends the standard client session for msoy bureaus. It prevents invocation responses that are
 * intended for flash clients from being sent to thane clients. While the responses themselves only
 * contain plain data, the transient fields and methods incur a very large import footprint
 * including many ui and mx classes. These do not compile under thane.
 */
public class MsoyBureauClient extends PresentsClient
{
    // from PresentsClient
    @Override protected ClientProxy createProxySubscriber ()
    {
        return new SubProxy();
    }

    /**
     * Extends the subscription proxy so that we can detect and block invocation responses.
     */
    protected class SubProxy extends ClientProxy
    {
        // from ClientProxy
        @Override public void eventReceived (DEvent event)
        {
            if (_blockInvocationResponses && event instanceof InvocationResponseEvent) {
                // TODO: remove log message once this is proven to work
                log.info(
                    "Preventing bureau invocation response event", "bureau", getUsername(),
                    "event", event);
                return;
            }
            super.eventReceived(event);
        }

        // from ClientProxy
        @Override public void objectAvailable (DObject dobj)
        {
            super.objectAvailable(dobj);
            
            // Block responses to flash clients only
            _blockInvocationResponses = (dobj instanceof PlayerObject);
        }
        
        protected boolean _blockInvocationResponses;
    }
}
