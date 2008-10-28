//
// $Id$

package com.threerings.msoy.bureau.server;

import com.threerings.presents.client.Client;
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
    /**
     * Notifies this client that an agent has been added. Increases message allowance.
     */
    public void agentAdded ()
    {
        _agentCount++;
        setThrottle();
    }
    
    /**
     * Notifies this client that an agent has been removed. Decreases message allowance.
     */
    public void agentRemoved ()
    {
        _agentCount--;
        setThrottle();
    }

    @Override // from PresentsClient
    protected ClientProxy createProxySubscriber ()
    {
        return new SubProxy();
    }
    
    protected void setThrottle ()
    {
        setIncomingMessageThrottle(
            Client.DEFAULT_MSGS_PER_SECOND * Math.max(_agentCount, 1));
    }
    
    /**
     * Extends the subscription proxy so that we can detect and block invocation responses.
     */
    protected class SubProxy extends ClientProxy
    {
        
        @Override // from ClientProxy 
        public void eventReceived (DEvent event)
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

        @Override // from ClientProxy
        public void objectAvailable (DObject dobj)
        {
            super.objectAvailable(dobj);
            
            // Block responses to flash clients only
            _blockInvocationResponses = (dobj instanceof PlayerObject);
        }
        
        protected boolean _blockInvocationResponses;
    }
    
    protected int _agentCount;
}
