//
// $Id$

package com.threerings.msoy.client {

import flash.display.DisplayObject;
import flash.display.Stage;

import flash.external.ExternalInterface;

import flash.geom.Point;

import flash.system.Security;

import mx.core.Application;

import mx.resources.ResourceBundle;

import com.threerings.util.MenuUtil;
import com.threerings.util.Name;
import com.threerings.util.ResultAdapter;
import com.threerings.util.StringUtil;

import com.threerings.presents.client.Client;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.data.ClientObject;

import com.threerings.presents.dobj.DObjectManager;

import com.threerings.presents.net.BootstrapData;

// imported so that they'll be compiled into the .swf
import com.threerings.presents.data.TimeBaseMarshaller;
import com.threerings.crowd.data.BodyMarshaller;
import com.threerings.crowd.data.LocationMarshaller;
import com.threerings.crowd.chat.data.ChatMarshaller;

import com.threerings.whirled.data.SceneMarshaller;
import com.threerings.whirled.spot.data.SpotMarshaller;
import com.threerings.whirled.spot.data.SpotSceneObject;

import com.threerings.parlor.data.ParlorMarshaller;
import com.threerings.toybox.data.ToyBoxMarshaller;

import com.threerings.msoy.data.MemberInfo;
import com.threerings.msoy.data.MemberMarshaller;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyAuthResponseData;
import com.threerings.msoy.data.MsoyBootstrapData;
import com.threerings.msoy.data.MsoyCredentials;

import com.threerings.msoy.item.data.ItemMarshaller;
import com.threerings.msoy.item.web.Document;
import com.threerings.msoy.item.web.Furniture;
import com.threerings.msoy.item.web.Game;
import com.threerings.msoy.item.web.Photo;

import com.threerings.msoy.world.data.PetMarshaller;
import com.threerings.msoy.world.data.RoomConfig;

import com.threerings.msoy.game.data.LobbyMarshaller;
import com.threerings.msoy.game.data.WorldGameMarshaller;
import com.threerings.msoy.game.client.LobbyController;

import com.threerings.msoy.swiftly.data.SwiftlyMarshaller;

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

        if (!configureExternalFunctions()) {
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
     * Configure our external functions that we expose to javascript.
     *
     * @return true if successfully configured.
     */
    protected function configureExternalFunctions () :Boolean
    {
        try {
            if (!ExternalInterface.available) {
                return false;
            }

//             ExternalInterface.addCallback("clientLogon", externalClientLogon);
//             ExternalInterface.addCallback("clientLogoff", externalClientLogoff);

        } catch (err :Error) {
            // nada: ExternalInterface isn't there. Oh well!
            return false;
        }

        return true;
    }

//     /**
//      * Exposed to javascript so that it may notify us to logon.
//      */
//     protected function externalClientLogon (memberId :int, sessionToken :String) :void
//     {
//         if (sessionToken == null) {
//             return;
//         }

//         log.info("Logging on via external request [id=" + memberId +
//                  ", token=" + sessionToken + "].");
//         Prefs.setSessionToken(sessionToken);
//         var co :MemberObject = _ctx.getClientObject();
//         if (co == null || co.getMemberId() != memberId) {
//             _ctx.getMsoyController().handleLogon(createStartupCreds(false, false));
//         }
//     }

//     /**
//      * Exposed to javascript so that it may notify us to logoff.
//      */
//     protected function externalClientLogoff (backAsGuest :Boolean = true) :void
//     {
//         log.info("Logging off via external request [backAsGuest=" + backAsGuest + "].");

//         if (backAsGuest) {
//             // have the controller handle it it will logoff, then back as a guest
//             _ctx.getMsoyController().handleLogon(null);
//         } else {
//             logoff(false);
//         }
//     }

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
