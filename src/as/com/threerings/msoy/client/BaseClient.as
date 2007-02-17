//
// $Id$

package com.threerings.msoy.client {

import flash.display.DisplayObject;
import flash.display.Stage;

import flash.external.ExternalInterface;

import mx.core.Application;
import mx.resources.ResourceBundle;

import com.threerings.util.ResultAdapter;
import com.threerings.util.StringUtil;

import com.threerings.presents.client.Client;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.data.ClientObject;

import com.threerings.presents.dobj.DObjectManager;

import com.threerings.presents.net.BootstrapData;

import com.threerings.presents.data.TimeBaseMarshaller;
import com.threerings.crowd.data.BodyMarshaller;
import com.threerings.crowd.data.LocationMarshaller;
import com.threerings.crowd.chat.data.ChatMarshaller;

import com.threerings.msoy.data.MemberInfo;
import com.threerings.msoy.data.MemberMarshaller;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyAuthResponseData;
import com.threerings.msoy.data.MsoyBootstrapData;
import com.threerings.msoy.data.MsoyCredentials;

/**
 * A client shared by both our virtual world and header incarnations.
 */
public /*abstract*/ class BaseClient extends Client
{
    public static const log :Log = Log.getLog(BaseClient);

    public function BaseClient (stage :Stage)
    {
        super(createStartupCreds(), stage);

        _ctx = createContext();
        LoggingTargets.configureLogging(_ctx);

        // wire up our JavaScript bridge functions
        try {
            if (ExternalInterface.available) {
                configureExternalFunctions();
            }
        } catch (err :Error) {
            // nada: ExternalInterface isn't there. Oh well!
            log.info("Unable to configure external functions.");
        }

        // configure our server and port info and logon
        setServer(DeploymentConfig.serverHost, DeploymentConfig.serverPorts);
    }

    public function fuckingCompiler () :void
    {
        var i :int = TimeBaseMarshaller.GET_TIME_OID;
        i = LocationMarshaller.LEAVE_PLACE;
        i = BodyMarshaller.SET_IDLE;
        i = ChatMarshaller.AWAY;

        var c :Class;
        c = MsoyBootstrapData;
        c = MemberObject;
        c = MemberInfo;
        c = MsoyAuthResponseData;
        c = MemberMarshaller;

        // these cause bundles to be compiled in.
        [ResourceBundle("global")]
        var rb :ResourceBundle; // this needs to be here for the above lines
    }

    // from Client
    override public function gotBootstrap (data :BootstrapData, omgr :DObjectManager) :void
    {
        super.gotBootstrap(data, omgr);

        // save any machineIdent or sessionToken from the server.
        var rdata :MsoyAuthResponseData = (getAuthResponseData() as MsoyAuthResponseData);
        if (rdata.ident != null) {
            Prefs.setMachineIdent(rdata.ident);
        }
        if (rdata.sessionToken != null) {
            Prefs.setSessionToken(rdata.sessionToken);
        }

        if (rdata.sessionToken != null) {
            try {
                if (ExternalInterface.available) {
                    ExternalInterface.call("flashDidLogon", "Foo", 1, rdata.sessionToken);
                }
            } catch (err :Error) {
                log.warning("Unable to inform javascript about login: " + err);
            }
        }
    }

    // from Client
    override public function gotClientObject (clobj :ClientObject) :void
    {
        super.gotClientObject(clobj);

        // set up our logging targets
        LoggingTargets.configureLogging(_ctx);

        // possibly ensure our local storage capacity
        var user :MemberObject = (clobj as MemberObject);
        if (!user.isGuest()) {
            Prefs.config.ensureCapacity(
                102400, new ResultAdapter(null, function (cause :Error) :void {
                log.warning("User denied request to increase local storage capacity.");
            }));
        }
    }

    /**
     * Configure any external functions that we wish to expose to JavaScript.
     */
    protected function configureExternalFunctions () :void
    {
    }

    /**
     * Creates the context we'll use with this client.
     */
    protected function createContext () :BaseContext
    {
        return new BaseContext(this);
    }

    /**
     * Create the credentials that will be used to log us on
     */
    protected static function createStartupCreds (
        allowGuest :Boolean = true, checkCookie :Boolean = true) :MsoyCredentials
    {
        var creds :MsoyCredentials = new MsoyCredentials(null, null);
        creds.ident = Prefs.getMachineIdent();
        var params :Object = Application.application.loaderInfo.parameters;
        if (!allowGuest || (null == params["guest"])) {
            if (checkCookie) {
                creds.sessionToken = getSessionTokenFromCookie();
            }
            if (creds.sessionToken == null) {
                creds.sessionToken = Prefs.getSessionToken();
            }
        }

        return creds;
    }

    /**
     * Attempt to read our session token from the cookies set on the host document.
     */
    protected static function getSessionTokenFromCookie () :String
    {
        if (ExternalInterface.available) {
            try {
                var cookies :String = ExternalInterface.call("eval", "document.cookie");
                if (cookies != null) {
                    var credPrefix :String = "creds=";
                    for each (var cook :String in cookies.split(";")) {
                        cook = StringUtil.trim(cook);
                        if (StringUtil.startsWith(cook, credPrefix)) {
                            cook = cook.substring(credPrefix.length);
                            var peridx :int = cook.indexOf(".");
                            if (peridx != -1) {
                                return cook.substring(peridx + 1);
                            }
                        }
                    }
                }

            } catch (err :Error) {
                log.warning("Error reading session token from cookie: " + err);
            }
        }

        return null;
    }

    protected var _ctx :BaseContext;
}
}
