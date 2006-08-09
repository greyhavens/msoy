package com.threerings.msoy.client {

import flash.display.DisplayObject;
import flash.display.Stage;

import flash.external.ExternalInterface;

import flash.events.ContextMenuEvent;

import flash.system.Security;

import flash.ui.ContextMenu;

import mx.core.Application;

import mx.resources.ResourceBundle;

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

import com.threerings.msoy.data.MemberMarshaller;
import com.threerings.msoy.data.MsoyAuthResponseData;
import com.threerings.msoy.data.MsoyBootstrapData;
import com.threerings.msoy.data.MsoyCredentials;
import com.threerings.msoy.data.MemberInfo;
import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.world.data.RoomConfig;

public class MsoyClient extends Client
{
    private static const log :Log = Log.getLog(MsoyClient);

    public function MsoyClient (app :Application)
    {
        var creds :MsoyCredentials = new MsoyCredentials(null, null);
        creds.sessionToken = Prefs.getSessionToken();
        creds.ident = Prefs.getMachineIdent();
        super(creds, app);

        // set up a context menu that blocks funnybiz on the stage
        var menu :ContextMenu = new ContextMenu();
        menu.hideBuiltInItems();
        app.contextMenu = menu;
        menu.addEventListener(
            ContextMenuEvent.MENU_SELECT, contextMenuWillPopUp);

//        Security.allowDomain("*"); // TODO

        _ctx = new MsoyContext(this, app);
        LoggingTargets.configureLogging(_ctx);

        // register our logoff function as being available from javascript
        try {
            if (ExternalInterface.available) {
                ExternalInterface.addCallback("logoff",
                    function (backAsGuest :Boolean) :void {
                        // TODO
                        logoff(false);
                    });

                ExternalInterface.addCallback("setCredentials",
                    function (username :String, sessionToken :String) :void {
                        // TODO
                    });

            } else {
                trace("Unable to communicate with javascript!");
            }
        } catch (err :Error) {
            // nada: ExternalInterface isn't there. Oh well!
        }

        // configure our server and port info and logon
        setServer(DeploymentConfig.serverHost, DeploymentConfig.serverPorts);
        logon();
    }

    override public function gotBootstrap (
            data :BootstrapData, omgr :DObjectManager) :void
    {
        super.gotBootstrap(data, omgr);

        // save any machineIdent or sessionToken from the server.
        var rdata :MsoyAuthResponseData =
            (getAuthResponseData() as MsoyAuthResponseData);
        if (rdata.ident != null) {
            Prefs.setMachineIdent(rdata.ident);
        }
        if (rdata.sessionToken != null) {
            Prefs.setSessionToken(rdata.sessionToken);
        }
    }

    // documetnation inherited
    override public function gotClientObject (clobj :ClientObject) :void
    {
        super.gotClientObject(clobj);

        // set up our logging targets
        LoggingTargets.configureLogging(_ctx);

        // possibly ensure our local storage capacity
        var user :MemberObject = (clobj as MemberObject);
        if (!user.isGuest()) {
            Prefs.config.ensureCapacity(102400, new ResultAdapter(null,
                function (cause :Error) :void {
                    log.warning("User denied request to increase " +
                        "local storage capacity.");
                }));
        }
    }

    /**
     * Called to process ContextMenuEvent.MENU_SELECT.
     */
    protected function contextMenuWillPopUp (event :ContextMenuEvent) :void
    {
        var disp :DisplayObject = event.mouseTarget;
        var menu :ContextMenu = (event.target as ContextMenu);
        var custom :Array = menu.customItems;
        custom.length = 0;

        do {
            if (disp is ContextMenuProvider) {
                (disp as ContextMenuProvider).populateContextMenu(custom);
            }

            disp = disp.parent;
        } while (disp != null);

        // then, the menu will pop up
    }

    public function fuckingCompiler () :void
    {
        var i :int = TimeBaseMarshaller.GET_TIME_OID;
        i = LocationMarshaller.LEAVE_PLACE;
        i = BodyMarshaller.SET_IDLE;
        i = ChatMarshaller.AWAY;

        var c :Class;
        c = RoomConfig;
        c = SceneMarshaller;
        c = SpotMarshaller;
        c = MsoyBootstrapData;
        c = MemberObject;
        c = MemberInfo;
        c = SpotSceneObject;
        c = MsoyAuthResponseData;
        c = MemberMarshaller;
        c = ParlorMarshaller;

        // these cause bundles to be compiled in.
        [ResourceBundle("global")]
        [ResourceBundle("general")]
        [ResourceBundle("game")]
        var rb :ResourceBundle; // this needs to be here for the above lines
    }

    protected var _ctx :MsoyContext;
}
}
