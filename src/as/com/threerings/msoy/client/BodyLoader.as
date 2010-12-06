//
// $Id: $

package com.threerings.msoy.client {

import flash.events.EventDispatcher;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientAdapter;
import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.client.ClientObserver;
import com.threerings.presents.client.SessionObserver;
import com.threerings.presents.util.SafeSubscriber;
import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.ObjectAccessError;

import com.threerings.util.Log;

import com.threerings.msoy.client.MsoyClient;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.data.MemberClientObject;
import com.threerings.msoy.data.MemberObject;

/**
 * This class inserts itself into the client object resolution chain. The idea is that when we
 * login, the server sends us a tiny client object. It then enters us into a login queue,
 * eventually getting around to resolving our full client object. At that point, it updates the
 * tiny one. We detect this, subscribe to the large one, and when all is ready, then we finally
 * dispatch the DID_LOGON.
 */
public class BodyLoader extends EventDispatcher
    implements AttributeChangeListener
{
    private static const log :Log = Log.getLog(BodyLoader);

    public function BodyLoader (client :MsoyClient)
    {
        _client = client;

        var adapter :ClientAdapter = new ClientAdapter(
            forward, didLogon, forward, didLogoff, forward, forward, forward, forward);
        _client.addRealClientObserver(adapter);
    }

    public function init (ctx :MsoyContext) :void
    {
        _ctx = ctx;
    }

    public function getBody () :MemberObject
    {
        return _body;
    }

    /**
     * Registers the supplied observer with this client. While registered the observer will receive
     * notifications of state changes within the client. The function will refuse to register an
     * already registered observer.
     *
     * @see ClientObserver
     * @see SessionObserver
     */
    public function addClientObserver (observer :SessionObserver) :void
    {
        addEventListener(ClientEvent.CLIENT_WILL_LOGON, observer.clientWillLogon);
        addEventListener(ClientEvent.CLIENT_DID_LOGON, observer.clientDidLogon);
        addEventListener(ClientEvent.CLIENT_OBJECT_CHANGED, observer.clientObjectDidChange);
        addEventListener(ClientEvent.CLIENT_DID_LOGOFF, observer.clientDidLogoff);
        if (observer is ClientObserver) {
            var cliObs :ClientObserver = (observer as ClientObserver);
            addEventListener(ClientEvent.CLIENT_FAILED_TO_LOGON, cliObs.clientFailedToLogon);
            addEventListener(ClientEvent.CLIENT_CONNECTION_FAILED, cliObs.clientConnectionFailed);
            addEventListener(ClientEvent.CLIENT_WILL_LOGOFF, cliObs.clientWillLogoff);
            addEventListener(ClientEvent.CLIENT_DID_CLEAR, cliObs.clientDidClear);
        }
    }

    /**
     * Unregisters the supplied observer. Upon return of this function, the observer will no longer
     * receive notifications of state changes within the client.
     */
    public function removeClientObserver (observer :SessionObserver) :void
    {
        removeEventListener(ClientEvent.CLIENT_WILL_LOGON, observer.clientWillLogon);
        removeEventListener(ClientEvent.CLIENT_DID_LOGON, observer.clientDidLogon);
        removeEventListener(ClientEvent.CLIENT_OBJECT_CHANGED, observer.clientObjectDidChange);
        removeEventListener(ClientEvent.CLIENT_DID_LOGOFF, observer.clientDidLogoff);
        if (observer is ClientObserver) {
            var cliObs :ClientObserver = (observer as ClientObserver);
            removeEventListener(ClientEvent.CLIENT_FAILED_TO_LOGON, cliObs.clientFailedToLogon);
            removeEventListener(
                ClientEvent.CLIENT_CONNECTION_FAILED, cliObs.clientConnectionFailed);
            removeEventListener(ClientEvent.CLIENT_WILL_LOGOFF, cliObs.clientWillLogoff);
            removeEventListener(ClientEvent.CLIENT_DID_CLEAR, cliObs.clientDidClear);
        }
    }

    public function forward (event :ClientEvent) :void
    {
        // these events we just send on without delay
        this.dispatchEvent(event);
    }

    public function didLogon (event :ClientEvent) :void
    {
        _switching = event.isSwitchingServers();
        _loader = _client.getClientObject() as MemberClientObject;
        if (_loader == null) {
            // we're screwed
            log.warning("Expected MemberClientObject", "got", _client.getClientObject());
            bail();
            return;
        }
        if (_loader.bodyOid != 0) {
            // if the oid is already set, the object didn't need resolving (was forwarded)
            subscribeToBody();
        } else {
            // else wait until it's ready
            _loader.addListener(this);

            // and possibly show a login progress bar
            if (_ctx.getPlaceView() is BlankPlaceView) {
                (_ctx.getPlaceView() as BlankPlaceView).gotClientObject(_loader);
            }
        }
    }

    public function didLogoff (event :ClientEvent) :void
    {
        if (_subscriber != null) {
            _subscriber.unsubscribe(_ctx.getDObjectManager());
            _subscriber = null;
        }
        forward(event);
    }

    public function attributeChanged (event :AttributeChangedEvent) :void
    {
        if (MemberClientObject.BODY_OID == event.getName()) {
            // the oid won't be set until the body is fully resolved
            subscribeToBody();
        }
    }

    protected function subscribeToBody () :void
    {
        _subscriber = new SafeSubscriber(_loader.bodyOid, gotBodyObject, subscribeFailed);
        _subscriber.subscribe(_ctx.getDObjectManager());
    }

    protected function gotBodyObject (obj :MemberObject) :void
    {
        _body = obj;
        _loader.setMemberObject(obj);

        dispatchEvent(new ClientEvent(ClientEvent.CLIENT_DID_LOGON, _client, _switching, null));
    }

    protected function subscribeFailed (oid :int, cause :ObjectAccessError) :void
    {
        log.warning("Party subscription failed", "cause", cause);
        bail();
    }

    protected function bail () :void
    {
        if (_subscriber != null) {
            _subscriber.unsubscribe(_ctx.getDObjectManager());
            _subscriber = null;
        }
        _client.logoff(false);
    }

    protected var _client :MsoyClient;
    protected var _ctx :MsoyContext;
    protected var _loader :MemberClientObject;
    protected var _subscriber :SafeSubscriber;
    protected var _switching :Boolean;
    protected var _body :MemberObject;
}
}

