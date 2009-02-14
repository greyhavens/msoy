package com.threerings.msoy.client {

import flash.external.ExternalInterface;

import com.threerings.util.ClassUtil;
import com.threerings.util.Log;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientEvent;

import com.threerings.presents.net.AuthResponseData

import com.threerings.msoy.data.MsoyAuthResponseData;
import com.threerings.msoy.data.MsoyCredentials;

import com.threerings.msoy.data.all.MemberName;

/** Takes care of monitoring a client's logon and conditionally updating the guest session tokens
 * in flash and browser cookies. */
public class GuestSessionCapture
{
    /** Set up monitoring on the given client, with an optional function (taking no arguments) to
     * be invoked once the client is logged on successfully, regardless of guest status. This
     * method is mainly to avoid the odd syntax "new X()" with no assignment. */
    public static function capture (client :Client, didLogon :Function = null) :void
    {
        new GuestSessionCapture(client, didLogon);
    }

    /** @private */
    public function GuestSessionCapture (client :Client, didLogon :Function = null)
    {
        _client = client;
        _didLogon = didLogon;

        _client.addEventListener(ClientEvent.CLIENT_WILL_LOGON, this.willLogon);
        _client.addEventListener(ClientEvent.CLIENT_DID_LOGON, this.didLogon);
        _client.addEventListener(ClientEvent.CLIENT_FAILED_TO_LOGON, this.logonFailed);
    }

    /** Removes the listeners. */
    protected function cancel () :void
    {
        _client.removeEventListener(ClientEvent.CLIENT_WILL_LOGON, this.willLogon);
        _client.removeEventListener(ClientEvent.CLIENT_DID_LOGON, this.didLogon);
        _client.removeEventListener(ClientEvent.CLIENT_FAILED_TO_LOGON, this.logonFailed);
    }

    /** Callback when client is about to log on. */
    protected function willLogon (evt :ClientEvent) :void
    {
        // we just need to detect whether or not the logon was anonymous (b/c WorldClient
        // de-anonymizes the creds in its gotBootstrap)
        var creds :MsoyCredentials = MsoyCredentials(_client.getCredentials());
        if (creds == null) {
            log.warning("Null creds during logon", "client", _client);
            cancel();
            return;
        }

        _anonymous = creds.getUsername() == null && creds.sessionToken == null;
    }

    /** Callback when client has successfully logged on. */
    protected function didLogon (evt :ClientEvent) :void
    {
        // try block so we guarantee call to cancel
        try {
            tryDidLogon(evt);

        } finally {
            cancel();

            // invoke our application's callback
            if (_didLogon != null) {
                _didLogon();
            }
        }
    }

    /** Checks response data etc. and conditionally sets cookies. */
    protected function tryDidLogon (evt :ClientEvent) :void
    {
        if (_client.getClientObject().username == null) {
            log.warning("Null username after logon?", "client", _client);
            return;
        }

        var username :String = _client.getClientObject().username.toString();

        // the rest is only for permaguests
        if (!MemberName.isPermaguest(username)) {
            return;
        }

        // set or reset our flash permaguest token
        log.info("You are a permaguest", "client", _client, "name", username);
        Prefs.setPermaguestUsername(username);

        // if this is a new permagust account, transfer the credentials to gwt
        if (_anonymous && ExternalInterface.available) {
            var authdata :AuthResponseData = _client.getAuthResponseData();
            if (authdata == null) {
                log.warning("Null authdata after login");

            } else if (MsoyAuthResponseData(authdata) == null) {
                log.warning("Non-msoy authdata", "class", ClassUtil.getClassName(authdata),
                    "data", authdata);

            } else {
                // the server has created an account for us, yippee! let gwt know
                var serverToken :String = MsoyAuthResponseData(authdata).sessionToken;
                log.info("Setting permaguest token to GWT", "token", serverToken);
                ExternalInterface.call("setPermaguestInfo", username, serverToken);
            }
        }
        
    }

    /** Callback when logon fails for some reason. */
    protected function logonFailed (evt :ClientEvent) :void
    {
        cancel();
    }

    /** Grabs a log for this class. */
    protected function get log () :Log
    {
        return Log.getLog(this);
    }

    /** Client we are capturing. */
    protected var _client :Client;

    /** Function to invoke after logon finishes successfully. */
    protected var _didLogon :Function;

    /** Whether the logon was initiated with no credentials. */
    protected var _anonymous :Boolean;
}

}
