//
// $Id$

package com.threerings.msoy.avrg.client {

import flash.errors.IOError;
import flash.events.Event;
import flash.events.IOErrorEvent;
import flash.net.URLLoader;
import flash.net.URLLoaderDataFormat;
import flash.net.URLRequest;
import flash.utils.ByteArray;
import flash.utils.Dictionary;

import com.threerings.io.TypedArray;

import com.threerings.util.Integer;
import com.threerings.util.Iterator;
import com.threerings.util.Log;
import com.threerings.util.ObjectMarshaller;

import com.threerings.presents.client.ConfirmAdapter;
import com.threerings.presents.client.InvocationAdapter;
import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_InvocationListener;

import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

import com.whirled.game.client.PropertySpaceHelper;
import com.whirled.game.client.WhirledGameMessageService;

import com.whirled.game.data.GameData;
import com.whirled.game.data.LevelData;
import com.whirled.game.data.PropertySpaceObject;

import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.item.data.all.ItemIdent;

import com.threerings.msoy.avrg.data.AVRGameObject;
import com.threerings.msoy.avrg.data.PlayerLocation;

import com.threerings.msoy.room.data.ActorInfo;
import com.threerings.msoy.room.data.MsoyLocation;
import com.threerings.msoy.room.data.RoomObject;

/**
 * Various functions useful to both the thane and the flash backends.
 */
public class BackendUtils
{
    public static const log :Log = Log.getLog(BackendUtils);

    /**
     * Throws an error if the name is not a valid property name.
     */
    public static function validateName (name :String) :void
    {
        if (name == null) {
            throw new ArgumentError("Property, message, and collection names must not be null.");
        }
    }

    /**
     * Verify that the value is legal to be streamed to other clients.
     */
    public static function validateValue (value :Object) :void
    {
        ObjectMarshaller.validateValue(value);
    }

    /**
     * Verify that the property name / value are valid.
     */
    public static function validatePropertyChange (
        propName :String, value :Object, array :Boolean, index :Object) :void
    {
        validateName(propName);

        if (array) {
            if (index == null || int(index) < 0) {
                throw new ArgumentError("Bogus array index specified.");
            }
            // TODO: fixy
//            if (!(_gameData[propName] is Array)) {
//                throw new ArgumentError("Property " + propName + " is not an Array.");
//            }
        }

        // validate the value too
        validateValue(value);
    }

    /**
     * Performs a standard property set.
     */
    public static function encodeAndSet (
        obj :PropertySpaceObject, name :String, value :Object, key :Object,
        isArray :Boolean, immediate :Boolean) :void
    {
        validatePropertyChange(name, value, isArray, key);

        var encoded :Object = PropertySpaceHelper.encodeProperty(value, (key == null));
        var ikey :Integer = (key == null) ? null : new Integer(int(key));

        obj.getPropService().setProperty(
            name, encoded, ikey, isArray, false, null, loggingConfirmListener("setProperty"));

        if (immediate) {
            // we re-decode so that it looks like it came off the net
            // TODO: fix broken behaviour
            try {
                PropertySpaceHelper.applyPropertySet(
                    obj, name, PropertySpaceHelper.decodeProperty(encoded),
                    key, isArray);

            } catch (re :RangeError) {
                log.warning("Error setting property (immediate)", "name", name, re);
            }
        }
    }

    /**
     * Get all the players in a game, optionally filtered by room and an additional
     * filtering function (this will be called with just the member id and should return
     * true to include the member, false to exclude).
     */
    public static function getPlayerIds (
        gameObj :AVRGameObject, room :RoomObject, roomId :int, extraFilter :Function=null) :Array
    {
        // TODO: if searching a room, this could have a high miss rate, consider passing a roomObj
        var result :Array = [];
        var iterator :Iterator = gameObj.occupantInfo.iterator();
        while (iterator.hasNext()) {
            var name :MemberName = OccupantInfo(iterator.next()).username as MemberName;
            if (name == null) {
                continue;
            }
            var memberId :int = name.getMemberId();
            var plLoc :PlayerLocation = gameObj.playerLocs.get(memberId) as PlayerLocation;
            if (plLoc == null || (roomId != 0 && plLoc.sceneId != roomId) ||
                (extraFilter != null && !extraFilter(memberId))) {
                continue;
            }
            if (room != null && room.getMemberInfo(memberId) == null) {
                continue;
            }
            result.push(memberId);
        }
        return result;
    }

    /**
     * Get the ids of all the occupants of the specified place.
     */
    public static function getOccupantIds (placeObj :PlaceObject) :Array
    {
        var result :Array = [];
        if (placeObj != null) {
            for each (var occ :OccupantInfo in placeObj.occupantInfo.toArray()) {
                var name :MemberName = occ.username as MemberName;
                if (name != null) {
                    result.push(name.getMemberId());
                }
            }
        }
        return result;
    }

    /**
     * Get the display name of the specified occupant.
     */
    public static function getOccupantName (placeObj :PlaceObject, playerId :int) :String
    {
        if (placeObj != null) {
            for each (var occ :OccupantInfo in placeObj.occupantInfo.toArray()) {
                var name :MemberName = occ.username as MemberName;
                if ((name != null) && (playerId == name.getMemberId())) {
                    return name.toString();
                }
            }
        }
        return null;
    }

    public static function getLevelPacks (gameData :TypedArray, filter :Function) :Array
    {
        var packs :Array = [];
        for each (var data :GameData in gameData) {
            if (data.getType() != GameData.LEVEL_DATA || (filter != null && !filter(data))) {
                continue;
            }
            packs.unshift({ ident: data.ident,
                            name: data.name,
                            mediaURL: data.mediaURL,
                            premium: (data as LevelData).premium });
        }
        return packs;
    }

    public static function getItemPacks (gameData :TypedArray, filter :Function) :Array
    {
        var packs :Array = [];
        for each (var data :GameData in gameData) {
            if (data.getType() != GameData.ITEM_DATA) {
                continue;
            }
            var count :int = (filter == null) ? 1 : filter(data);
            if (count == 0) {
                continue;
            }
            packs.unshift({ ident: data.ident,
                            name: data.name,
                            mediaURL: data.mediaURL,
                            count :count });
        }
        return packs;
    }

    public static function loadPackData (
        gameObj :AVRGameObject, ident :String, type :int,
        onLoaded :Function, onFailure :Function) :void
    {
        var data :GameData = getGameData(gameObj, ident, type);
        if (data == null) {
            if (onFailure != null) {
                onFailure(new Error("Unknown data pack: " + ident));
            }
            return;
        }

        var loader :URLLoader = new URLLoader();
        loader.dataFormat = URLLoaderDataFormat.BINARY;

        loader.addEventListener(IOErrorEvent.IO_ERROR, function (evt :IOErrorEvent) :void {
            if (onFailure != null) {
                onFailure(new IOError("I/O Error: " + evt.text));
            }
        });
        loader.addEventListener(Event.COMPLETE, function (evt :Event) :void {
            // TODO: setting the position=0 should happen at a lower level
            var ba :ByteArray = ByteArray(loader.data);
            ba.position = 0;
            onLoaded(ba);
        });
        loader.load(new URLRequest(data.mediaURL));
    }

    protected static function getGameData (
        gameObj :AVRGameObject, ident :String, type :int) :GameData
    {
        for each (var data :GameData in gameObj.gameData) {
            if (data.getType() == type && data.ident == ident) {
                return data;
            }
        }
        return null;
    }

    public static function sendMessage (
        svc :WhirledGameMessageService, msgName :String, msgValue :Object, svcName :String) :void
    {
        var encoded :Object = ObjectMarshaller.encode(msgValue, false);
        svc.sendMessage(msgName, encoded, loggingInvocationListener(svcName + " sendMessage"));
    }

    public static function sendPrivateMessage (
        svc :WhirledGameMessageService, receiverId :int, msgName :String,
        msgValue :Object, svcName :String) :void
    {
        var encoded :Object = ObjectMarshaller.encode(msgValue, false);
        var targets :TypedArray = TypedArray.create(int);
        targets.push(receiverId);
        svc.sendPrivateMessage(msgName, encoded, targets,
            loggingInvocationListener(svcName + " sendPrivateMessage"));
    }

    public static function holdsTrophy (
        playerId :int, ident :String, playerOwnsData :Function) :Boolean
    {
        return playerOwnsData(GameData.TROPHY_DATA, ident, playerId);
    }

    public static function getPlayerItemPacks (
        gameData :TypedArray, playerId :int, countPlayerData :Function) :Array
    {
        return getItemPacks(gameData, function (data :GameData) :int {
            return countPlayerData(data.getType(), data.ident, playerId);
        });
    }

    public static function getPlayerLevelPacks (
        gameData :TypedArray, playerId :int, playerOwnsData :Function) :Array
    {
        return getLevelPacks(gameData, function (data :GameData) :Boolean {
            return playerOwnsData(data.getType(), data.ident, playerId);
        });
    }

    /**
     * Sets the location of an avatar in its room.
     * @throws UserError if the player is not in the game or room
     */
    public static function setAvatarLocation (
        gameObj :AVRGameObject, room :RoomObject, playerId :int,
        x :Number, y :Number, z: Number, orient :Number) :void
    {
        resolvePlayerIdent(gameObj, room, playerId, function (ident :ItemIdent) :void {
            var newLoc :MsoyLocation = new MsoyLocation(x, y, z, orient);
            room.roomService.changeLocation(ident, newLoc);
        });
    }

    /**
     * Tells an avatar to play a custom action.
     * @throws UserError if the player is not in the game or room
     */
    public static function playAvatarAction (
        gameObj :AVRGameObject, room :RoomObject, playerId :int, action :String) :void
    {
        resolvePlayerIdent(gameObj, room, playerId, function (ident :ItemIdent) :void {
            var data :ByteArray = null;
            var isAction :Boolean = true;
            room.roomService.sendSpriteMessage(ident, action, data, isAction);
        });
    }

    /**
     * Sets the state of an avatar.
     * @throws UserError if the player is not in the game or room
     */
    public static function setAvatarState (
        gameObj :AVRGameObject, room :RoomObject, playerId :int, state :String) :void
    {
        resolvePlayerIdent(gameObj, room, playerId, function (ident :ItemIdent) :void {
            var actorOid :int = resolvePlayerWorldInfo(gameObj, room, playerId).bodyOid;
            room.roomService.setActorState(ident, actorOid, state);
        });
    }

    /**
     * Sets the move speed of an avatar.
     * @throws UserError if the player is not in the game or room
     */
    public static function setAvatarMoveSpeed (
        gameObj :AVRGameObject, room :RoomObject, playerId :int, pixelsPerSecond :Number) :void
    {
        resolvePlayerIdent(gameObj, room, playerId, function (ident :ItemIdent) :void {
            // todo
        });
    }

    /**
     * Sets the move speed of an avatar.
     */
    public static function setAvatarOrientation (
        gameObj :AVRGameObject, room :RoomObject, playerId :int, orient :Number) :void
    {
        resolvePlayerIdent(gameObj, room, playerId, function (ident :ItemIdent) :void {
            // todo
        });
    }

    /**
     * Creates a confirm listener that will log failures with a service name and optionally
     * call a function on success.
     */
    public static function loggingConfirmListener (
        svc :String, processed :Function = null, altOutputFn :Function = null)
        :InvocationService_ConfirmListener
    {
        return new ConfirmAdapter(processed,
            function (cause :String) :void {
                if (altOutputFn != null) {
                    var msg :String = new Date().toLocaleTimeString();
                    msg += ": service failure [service=" + svc + ", cause=" + cause + "]";
                    altOutputFn(msg);

                } else {
                    log.warning("Service failure", "service", svc, "cause", cause);
                }
            });
    }

    /**
     * Creates an invocation listener that will log failures with a service name.
     */
    public static function loggingInvocationListener (svc :String, altOutputFn :Function = null)
        :InvocationService_InvocationListener
    {
        return new InvocationAdapter(function (cause :String) :void {
            if (altOutputFn != null) {
                var msg :String = new Date().toLocaleTimeString();
                msg += ": service failure [service=" + svc + ", cause=" + cause + "]";
                altOutputFn(msg);

            } else {
                log.warning("Service failure", "service", svc, "cause", cause);
            }
        });
    }

    /**
     * Extracts the room occupant info for the given player id, also making sure the player is in
     * the game.
     * @throws UserError if the player is not in the game or room.
     */
    public static function resolvePlayerWorldInfo (
        gameObj :AVRGameObject, roomObj :RoomObject, playerId :int) :OccupantInfo
    {
        const nameKey :MemberName = new MemberName("", playerId);
        const occInfo :OccupantInfo = roomObj.getOccupantInfo(nameKey);
        // make sure they're in the room and that they're actually players in the game
        if (null == occInfo || null == gameObj.getOccupantInfo(nameKey)) {
            throw new UserError("Player not in room or game [playerId=" + playerId + "]");
        }
        return occInfo;
    }

    /**
     * Extracts the ident of an actor in a room and passes it to a function.
     * @throws UserError if the ident could not be obtained
     */
    public static function resolvePlayerIdent (
        gameObj :AVRGameObject, roomObj :RoomObject, playerId :int, fn :Function) :void
    {
        var occInfo :OccupantInfo = resolvePlayerWorldInfo(gameObj, roomObj, playerId);
        var actorInfo :ActorInfo = occInfo as ActorInfo;
        if (actorInfo == null) {
            // This is pretty weird
            log.warning("Resolving ident of non-actor", "occInfo", occInfo);
            return;
        }
        var ident :ItemIdent = actorInfo.getItemIdent();
        fn(ident);
    }
}
}
