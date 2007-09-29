//
// $Id$

package com.threerings.msoy.game.client {

import flash.utils.ByteArray;

import com.threerings.util.Name;
import com.threerings.util.ObjectMarshaller;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.OidList;
import com.threerings.presents.dobj.SetAdapter;

import com.threerings.crowd.client.LocationObserver;
import com.threerings.crowd.client.OccupantObserver;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.whirled.spot.data.SceneLocation;
import com.threerings.whirled.spot.data.SpotSceneObject;

import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.client.ControlBackend;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;

import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.RoomObject;

import com.threerings.msoy.game.data.GameState;
import com.threerings.msoy.game.data.AVRGameObject;

public class AVRGameControlBackend extends ControlBackend
    implements LocationObserver, OccupantObserver
{
    public function AVRGameControlBackend (
        ctx :WorldContext, avrGameObj :AVRGameObject, ctrl :AVRGameController)
    {
        _mctx = ctx;
        _avrGameObj = avrGameObj;

        _avrGameObj.addListener(_memlist);
        
        _mctx.getLocationDirector().addLocationObserver(this);
        _mctx.getOccupantDirector().addOccupantObserver(this);
        
        // will be null if not a room
        _roomObj = (_mctx.getLocationDirector().getPlaceObject() as RoomObject);
        if (_roomObj != null) {
            _roomObj.addListener(_movelist);
        }
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
    
    // from ControlBackend
     override public function shutdown () :void
     {
         super.shutdown();
        
         _avrGameObj.removeListener(_memlist);
        
         _mctx.getLocationDirector().removeLocationObserver(this);
         _mctx.getOccupantDirector().removeOccupantObserver(this);
        
         if (_roomObj != null) {
             _roomObj.removeListener(_movelist);
             _roomObj = null;
         }
     }

//     // from GameControlBackend
//     override protected function populateProperties (o :Object) :void
//     {
//         super.populateProperties(o);
        
//         o["lookupMemory_v1"] = lookupMemory_v1;
//         o["updateMemory_v1"] = updateMemory_v1;
//         o["getOccupantLocation_v1"] = getOccupantLocation_v1;
//     }
    
//     protected function lookupMemory_v1 (key :String) :Object
//     {
//         validateConnected();
//         var mkey :MemoryEntry = new MemoryEntry(_gameIdent, key),
//             entry :MemoryEntry = _avrGameObj.memories.get(mkey) as MemoryEntry;
//         return (entry == null) ? null : ObjectMarshaller.decode(entry.value);
//     }

//     protected function updateMemory_v1 (key :String, value: Object) :Boolean
//     {
//         validateConnected();
//         var data :ByteArray = ObjectMarshaller.validateAndEncode(value);
// //        var wgsvc :AVRGameService =
// //            (_ctx.getClient().requireService(AVRGameService) as AVRGameService);
// //        wgsvc.updateMemory(_ctx.getClient(), new MemoryEntry(_gameIdent, key, data));
//         return true;
//     }
    
//     override protected function getOccupants_v1 () :Array
//     {
//         validateConnected();
//         return (_roomObj == null ? null : getOccupantIds(_roomObj));
//     }

//     override protected function getPlayers_v1 () :Array
//     {
//         validateConnected();
//         return getOccupantIds(_avrGameObj);
//     }
    
//     // helper function for getOccupants_v1 and getPlayers_v1
//     protected function getOccupantIds (plobj :PlaceObject) :Array
//     {
//         var occs :Array = new Array();
//         var occlist :OidList = plobj.occupants;
//         for (var ii :int = 0, nn :int = occlist.size(); ii < nn; ii++) {
//             occs.push(occlist.get(ii));
//         }
//         return occs;
//     }
    
//     protected function getOccupantLocation_v1 (occupantId :int) :Array
//     {
//         validateConnected();
//         if (_roomObj != null) {
//             var sloc :SceneLocation = (_roomObj.occupantLocs.get(occupantId) as SceneLocation);
//             if (sloc != null) {
//                 var loc :MsoyLocation = (sloc.loc as MsoyLocation);
//                 return [loc.x, loc.y, loc.z];
//             }
//         }
//         return null;
//     }

//     /* override */ protected function getOccupantName_v1 (occupantId :int) :String
//     {
//         validateConnected();
//         if (_roomObj != null) {
//             var info :OccupantInfo = (_roomObj.occupantInfo.get(occupantId) as OccupantInfo);
//             if (info != null) {
//                 return info.username.toString();
//             }
//         }
//         return null;
//     }
    
     protected function callMemoryChanged (entry :GameState) :void
     {
         callUserCode("memoryChanged_v1", entry.key, ObjectMarshaller.decode(entry.value));
     }
    
    protected var _mctx :WorldContext;
    protected var _avrGameObj :AVRGameObject;
    protected var _gameIdent :ItemIdent;
    protected var _roomObj :RoomObject;
    
    protected var _movelist :SetAdapter = new SetAdapter(null,
        function (event :EntryUpdatedEvent) :void {
            if (event.getName() == SpotSceneObject.OCCUPANT_LOCS) {
                callUserCode("occupantMoved_v1", int(event.getEntry().getKey()));
            }
        });
    
    protected var _memlist :SetAdapter = new SetAdapter(
        function (event :EntryAddedEvent) :void {
            if (event.getName() == AVRGameObject.MEMORIES) {
                callMemoryChanged(event.getEntry() as GameState);
            }
        },
        function (event :EntryUpdatedEvent) :void {
            if (event.getName() == AVRGameObject.MEMORIES) {
                callMemoryChanged(event.getEntry() as GameState);
            }
        });
}
}
