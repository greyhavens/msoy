package com.threerings.msoy.client {

import flash.display.Stage;

import mx.core.Application;

import com.threerings.util.Name;
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
import com.threerings.msoy.data.SimpleChatConfig;
import com.threerings.msoy.world.data.MsoyFurniSceneModel;

public class MsoyClient extends Client
{
    public function MsoyClient (app :Application)
    {
        super(new UsernamePasswordCreds(new Name("guest"), "guest"), app.stage);

        _ctx = new MsoyContext(this, app);

        setServer("tasman.sea.earth.threerings.net", DEFAULT_SERVER_PORT);
        logon();
    }

    // documetnation inherited
    public override function gotClientObject (clobj :ClientObject) :void
    {
        super.gotClientObject(clobj);

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

        var c :Class = SimpleChatConfig;
        c = RoomConfig;
        c = SceneMarshaller;
        c = SpotMarshaller;
        c = MsoyBootstrapData;
        c = MsoyUserObject;
        c = MsoyOccupantInfo;
        c = SpotSceneObject;
        c = MsoyFurniSceneModel;
    }

    protected var _ctx :MsoyContext;
}
}
