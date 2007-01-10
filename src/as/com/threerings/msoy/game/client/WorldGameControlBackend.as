//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.util.Name;

import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.OidList;
import com.threerings.presents.dobj.SetAdapter;

import com.threerings.crowd.client.LocationObserver;
import com.threerings.crowd.client.OccupantObserver;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.whirled.spot.data.SpotSceneObject;

import com.threerings.ezgame.client.GameControlBackend;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.RoomObject;

import com.threerings.msoy.game.data.WorldGameObject;

public class WorldGameControlBackend extends GameControlBackend
    implements LocationObserver, OccupantObserver
{
    public function WorldGameControlBackend (
        ctx :MsoyContext, worldGameObj :WorldGameObject)
    {
        super(ctx, worldGameObj);
        _mctx = ctx;
        _worldGameObj = worldGameObj;
        
        _mctx.getLocationDirector().addLocationObserver(this);
        _mctx.getOccupantDirector().addOccupantObserver(this);
        
        // will be null if not a room
        _roomObj = (_mctx.getLocationDirector().getPlaceObject() as RoomObject);
    }
    
    // from LocationObserver
    public function locationMayChange (placeId :int) :Boolean
    {
        // TODO: Let user code veto move?
        return true;
    }

    // from LocationObserver
    public function locationDidChange (place :PlaceObject) :void
    {
        if (_roomObj != null) {
            _roomObj.removeListener(_movelist);
            callUserCode("leftRoom_v1");
        }
        _roomObj = (place as RoomObject);
        if (_roomObj != null) {
            _roomObj.addListener(_movelist);
            callUserCode("enteredRoom_v1");
        }
    }

    // from LocationObserver
    public function locationChangeFailed (placeId :int, reason :String) :void
    {
        // no-op
    }
    
    // from OccupantObserver
    public function occupantEntered (info :OccupantInfo) :void
    {
        if (_roomObj != null) {
            callUserCode("occupantEntered_v1", info.getBodyOid());
        }
    }
    
    // from OccupantObserver
    public function occupantLeft (info :OccupantInfo) :void
    {
        if (_roomObj != null) {
            callUserCode("occupantLeft_v1", info.getBodyOid());
        }
    }

    // from OccupantObserver
    public function occupantUpdated (oldinfo :OccupantInfo, newinfo :OccupantInfo) :void
    {
        // naught for now
    }
    
    // from GameControlBackend
    override public function shutdown () :void
    {
        super.shutdown();
        
        _mctx.getLocationDirector().removeLocationObserver(this);
        _mctx.getOccupantDirector().removeOccupantObserver(this);
        
        if (_roomObj != null) {
            _roomObj.removeListener(_movelist);
            _roomObj = null;
        }
    }
    
    // from GameControlBackend
    override protected function populateProperties (o :Object) :void
    {
        super.populateProperties(o);
        
        o["getPlayerOccupantIds_v1"] = getPlayerOccupantIds_v1;
        o["getMyOccupantId_v1"] = getMyOccupantId_v1;
        o["getRoomOccupantIds_v1"] = getRoomOccupantIds_v1;
        o["getOccupantName_v1"] = getOccupantName_v1;
        o["getOccupantLocation_v1"] = getOccupantLocation_v1;
    }
    
    protected function getPlayerOccupantIds_v1 () :Array
    {
        return getOccupantIds(_worldGameObj);
    }
    
    protected function getMyOccupantId_v1 () :int
    {
        return _mctx.getClientObject().getOid();
    }
    
    protected function getRoomOccupantIds_v1 () :Array
    {
        return (_roomObj == null ? null : getOccupantIds(_roomObj));
    }
    
    protected function getOccupantIds (plobj :PlaceObject) :Array
    {
        var occs :Array = new Array();
        var occlist :OidList = plobj.occupants;
        for (var ii :int = 0, nn :int = occlist.size(); ii < nn; ii++) {
            occs.push(occlist.getAt(ii));
        }
        return occs;
    }
    
    protected function getOccupantLocation_v1 (occupantId :int) :Array
    {
        if (_roomObj != null) {
            var loc :MsoyLocation = (_roomObj.occupantLocs.get(occupantId) as MsoyLocation);
            if (loc != null) {
                return [loc.x, loc.y, loc.z];
            }
        }
        return null;
    }

    protected function getOccupantName_v1 (occupantId :int) :String
    {
        if (_roomObj != null) {
            var info :OccupantInfo = (_roomObj.occupantInfo.get(occupantId) as OccupantInfo);
            if (info != null) {
                return info.username.toString();
            }
        }
        return null;
    }
    
    protected var _mctx :MsoyContext;
    protected var _worldGameObj :WorldGameObject;
    protected var _roomObj :RoomObject;
    
    protected var _movelist :SetAdapter = new SetAdapter(null,
        function (event :EntryUpdatedEvent) :void {
        if (event.getName() == SpotSceneObject.OCCUPANT_LOCS) {
            callUserCode("occupantMoved_v1", int(event.getEntry().getKey()));
        }
    });
}
}
