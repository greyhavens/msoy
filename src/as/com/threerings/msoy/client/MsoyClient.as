package com.threerings.msoy.client {

import flash.display.Stage;

import flash.external.ExternalInterface;

import flash.system.Security;

import flash.ui.ContextMenu;

import mx.core.Application;

import com.threerings.util.Name;
import com.threerings.util.ResultAdapter;

import com.threerings.presents.client.Client;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.net.UsernamePasswordCreds;

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

import com.threerings.msoy.data.MsoyBootstrapData;
import com.threerings.msoy.data.MsoyOccupantInfo;
import com.threerings.msoy.data.MsoyUserObject;

import com.threerings.msoy.world.data.RoomConfig;

public class MsoyClient extends Client
{
    private static const log :Log = Log.getLog(MsoyClient);

    public function MsoyClient (app :Application)
    {
        var guestId :int = int(Math.random() * int.MAX_VALUE);
        var guestName :Name = new Name("guest" + guestId);
        super(new UsernamePasswordCreds(guestName, "guest"), app.stage);

        // set up a context menu that blocks funnybiz on the stage
        var menu :ContextMenu = new ContextMenu();
        menu.hideBuiltInItems();
        app.contextMenu = menu;

//        Security.allowDomain("*"); // TODO

        _ctx = new MsoyContext(this, app);

        // register our logoff function as being available from javascript
        if (ExternalInterface.available) {
            ExternalInterface.addCallback("msoyLogoff", function () :void {
                logoff(false);
            });

        } else {
            trace("Unable to communicate with javascript!");
        }

        //setServer("tasman.sea.earth.threerings.net", DEFAULT_SERVER_PORTS);
        setServer("tasman.sea.earth.threerings.net", [ 4010 ]);
        //setServer("ice.puzzlepirates.com", [ 4010 ]);
        logon();
    }

    // documetnation inherited
    override public function gotClientObject (clobj :ClientObject) :void
    {
        super.gotClientObject(clobj);

        // set up our logging targets
        LoggingTargets.configureLogging(_ctx);

        // possibly ensure our local storage capacity
        var user :MsoyUserObject = (clobj as MsoyUserObject);
        if (!user.isGuest()) {
            Prefs.config.ensureCapacity(102400, new ResultAdapter(null,
                function (cause :Error) :void {
                    log.warning("User denied request to increase " +
                        "local storage capacity.");
                }));
        }

        // TODO: for now, we start with scene 1
        _ctx.getSceneDirector().moveTo(1);
        //_ctx.getLocationDirector().moveTo(
        //    (getBootstrapData() as MsoyBootstrapData).chatOid);
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
        c = MsoyUserObject;
        c = MsoyOccupantInfo;
        c = SpotSceneObject;
    }

    protected var _ctx :MsoyContext;
}
}
