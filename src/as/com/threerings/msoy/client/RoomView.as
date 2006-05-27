package com.threerings.msoy.client {

import mx.containers.VBox;

import com.threerings.util.HashMap;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.whirled.spot.data.SpotSceneObject;
import com.threerings.whirled.spot.data.SceneLocation;

import com.threerings.msoy.data.MsoyOccupantInfo;
import com.threerings.msoy.world.data.MsoyLocation;

import com.threerings.msoy.ui.Avatar;
import com.threerings.msoy.ui.ScreenMedia;

public class RoomView extends VBox
    implements PlaceView, SetListener
{
    public function RoomView (ctx :MsoyContext)
    {
        width = 800;
        height = 600;
    }

    protected function addBody (bodyOid :int) :void
    {
        var occInfo :MsoyOccupantInfo =
            (_sceneObj.occupantInfo.get(bodyOid) as MsoyOccupantInfo);
        var sloc :SceneLocation =
            (_sceneObj.occupantLocs.get(bodyOid) as SceneLocation);
        var loc :MsoyLocation = (sloc.loc as MsoyLocation);

        var avatar :Avatar = new Avatar(occInfo.avatar);
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

    // documentation inherited from interface PlaceView
    public function willEnterPlace (plobj :PlaceObject) :void
    {
        _sceneObj = (plobj as SpotSceneObject);

        _sceneObj.addListener(this);

        for (var ii :int = _sceneObj.occupants.size() - 1; ii >= 0; ii--) {
            addBody(_sceneObj.occupants.getAt(ii));
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

    protected var _sceneObj :SpotSceneObject;

    /** A map of bodyOid -> Avatar. */
    protected var _avatars :HashMap = new HashMap();
}
}
