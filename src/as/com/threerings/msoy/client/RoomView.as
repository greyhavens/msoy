package com.threerings.msoy.client {

import mx.containers.Canvas;

import mx.core.ScrollPolicy;

import com.threerings.util.HashMap;
import com.threerings.util.Iterator;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.whirled.spot.data.Portal;
import com.threerings.whirled.spot.data.SpotSceneObject;
import com.threerings.whirled.spot.data.SceneLocation;

import com.threerings.msoy.data.MsoyOccupantInfo;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyScene;

import com.threerings.msoy.ui.Avatar;
import com.threerings.msoy.ui.PortalMedia;
import com.threerings.msoy.ui.ScreenMedia;

public class RoomView extends Canvas
    implements PlaceView, SetListener
{
    public function RoomView (ctx :MsoyContext)
    {
        _ctx = ctx;

        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;

        width = 800;
        height = 600;
    }

    /**
     * Return the current location of the avatar that represents our body.
     */
    public function getMyCurrentLocation () :MsoyLocation
    {
        var oid :int = _ctx.getClient().getClientOid();
        var avatar :Avatar = (_avatars.get(oid) as Avatar);
        // TODO
        return new MsoyLocation(avatar.x, avatar.y, 0, 0);
    }

    protected function addBody (bodyOid :int) :void
    {
        var occInfo :MsoyOccupantInfo =
            (_sceneObj.occupantInfo.get(bodyOid) as MsoyOccupantInfo);
        var sloc :SceneLocation =
            (_sceneObj.occupantLocs.get(bodyOid) as SceneLocation);
        var loc :MsoyLocation = (sloc.loc as MsoyLocation);

        var avatar :Avatar = new Avatar(occInfo.avatar, loc);
        _avatars.put(bodyOid, avatar);
        avatar.x = loc.x;
        avatar.y = loc.y;
        addChild(avatar);
    }

    protected function removeBody (bodyOid :int) :void
    {
        var avatar :Avatar = (_avatars.remove(bodyOid) as Avatar);
        if (avatar != null) {
            removeChild(avatar);
        }
    }

    protected function moveBody (bodyOid :int) :void
    {
        var avatar :Avatar = (_avatars.get(bodyOid) as Avatar);
        var sloc :SceneLocation =
            (_sceneObj.occupantLocs.get(bodyOid) as SceneLocation);
        var loc :MsoyLocation = (sloc.loc as MsoyLocation);

        avatar.moveTo(loc);
    }

    protected function addPortal (portal :Portal) :void
    {
        var pm :PortalMedia = new PortalMedia(portal);
        var loc :MsoyLocation = (portal.loc as MsoyLocation);
        pm.x = loc.x;
        pm.y = loc.y;
        addChild(pm);
    }

    // documentation inherited from interface PlaceView
    public function willEnterPlace (plobj :PlaceObject) :void
    {
        // listen to updates
        _sceneObj = (plobj as SpotSceneObject);
        _sceneObj.addListener(this);

        // add all currently present occupants
        for (var ii :int = _sceneObj.occupants.size() - 1; ii >= 0; ii--) {
            addBody(_sceneObj.occupants.getAt(ii));
        }

        // set up any portals
        var scene :MsoyScene =
            (_ctx.getSceneDirector().getScene() as MsoyScene);
        var itr :Iterator = scene.getPortals();
        while (itr.hasNext()) {
            var portal :Portal = (itr.next() as Portal);
            addPortal(portal);
        }
    }

    // documentation inherited from interface PlaceView
    public function didLeavePlace (plobj :PlaceObject) :void
    {
        _sceneObj.removeListener(this);
        _sceneObj = null;

        // TODO: clean up avatars, remove them, etc.
    }

    // documentation inherited from interface SetListener
    public function entryAdded (event :EntryAddedEvent) :void
    {
        var name :String = event.getName();

        if (PlaceObject.OCCUPANT_INFO == name) {
            addBody((event.getEntry() as MsoyOccupantInfo).getBodyOid());
        }
    }

    // documentation inherited from interface SetListener
    public function entryUpdated (event :EntryUpdatedEvent) :void
    {
        var name :String = event.getName();

        if (SpotSceneObject.OCCUPANT_LOCS == name) {
            moveBody((event.getEntry() as SceneLocation).bodyOid);
        }
    }

    // documentation inherited from interface SetListener
    public function entryRemoved (event :EntryRemovedEvent) :void
    {
        var name :String = event.getName();

        if (PlaceObject.OCCUPANT_INFO == name) {
            removeBody((event.getOldEntry() as MsoyOccupantInfo).getBodyOid());
        }
    }

    protected var _ctx :MsoyContext;

    protected var _sceneObj :SpotSceneObject;

    /** A map of bodyOid -> Avatar. */
    protected var _avatars :HashMap = new HashMap();
}
}
