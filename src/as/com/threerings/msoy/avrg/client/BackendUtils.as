//
// $Id$

package com.threerings.msoy.avrg.client {

import flash.utils.ByteArray;

import com.threerings.io.TypedArray;

import com.threerings.util.Integer;
import com.threerings.util.Iterator;
import com.threerings.util.Log;
import com.threerings.util.ObjectMarshaller;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.ConfirmAdapter;
import com.threerings.presents.client.InvocationAdapter;
import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_InvocationListener;

import com.threerings.crowd.data.OccupantInfo;

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
        client :Client, obj :PropertySpaceObject, name :String, value :Object, key :Object, 
        isArray :Boolean, immediate :Boolean) :void
    {
        validatePropertyChange(name, value, isArray, key);

        var encoded :Object = PropertySpaceHelper.encodeProperty(value, (key == null));
        var ikey :Integer = (key == null) ? null : new Integer(int(key));

        obj.getPropService().setProperty(
            client, name, encoded, ikey, isArray, false, null, 
            loggingConfirmListener("setProperty"));

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
        gameObj :AVRGameObject, roomId :int, extraFilter :Function=null) :Array
    {
        // TODO: if searching a room, this could have a high miss rate, consider passing a roomObj
        var result :Array = new Array();
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
            result.push(memberId);
        }
        return result;
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
            if (data.getType() != GameData.ITEM_DATA || (filter != null && !filter(data))) {
                continue;
            }
            packs.unshift({ ident: data.ident,
                            name: data.name,
                            mediaURL: data.mediaURL });
        }
        return packs;
    }

    public static function sendMessage (
        svc :WhirledGameMessageService, client :Client, msgName :String, msgValue :Object, 
        svcName :String) :void
    {
        var encoded :Object = ObjectMarshaller.encode(msgValue, false);
        svc.sendMessage(
            client, msgName, encoded, loggingInvocationListener(svcName + " sendMessage"));
    }

    public static function sendPrivateMessage (
        svc :WhirledGameMessageService, client :Client, receiverId :int, msgName :String, 
        msgValue :Object, svcName :String) :void
    {
        var encoded :Object = ObjectMarshaller.encode(msgValue, false);
        var targets :TypedArray = TypedArray.create(int);
        targets.push(receiverId);
        svc.sendPrivateMessage(
            client, msgName, encoded, targets, 
            loggingInvocationListener(svcName + " sendPrivateMessage"));
    }

    public static function holdsTrophy (
        playerId :int, ident :String, playerOwnsData :Function) :Boolean
    {
        return playerOwnsData(GameData.TROPHY_DATA, ident, playerId);
    }

    public static function getPlayerItemPacks (
        gameData :TypedArray, playerId :int, playerOwnsData :Function) :Array
    {
        return getItemPacks(gameData, function (data :GameData) :Boolean {
            return playerOwnsData(data.getType(), data.ident, playerId);
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
        gameObj :AVRGameObject, room :RoomObject, client :Client, playerId :int, 
        x :Number, y :Number, z: Number, orient :Number) :void
    {
        resolvePlayerIdent(gameObj, room, playerId, function (ident :ItemIdent) :void {
            var newLoc :MsoyLocation = new MsoyLocation(x, y, z, orient);
            room.roomService.changeLocation(client, ident, newLoc);
        });
    }

    /**
     * Tells an avatar to play a custom action.
     * @throws UserError if the player is not in the game or room
     */
    public static function playAvatarAction (
        gameObj :AVRGameObject, room :RoomObject, client :Client, playerId :int, 
        action :String) :void
    {
        resolvePlayerIdent(gameObj, room, playerId, function (ident :ItemIdent) :void {
            var data :ByteArray = null;
            var isAction :Boolean = true;
            room.roomService.sendSpriteMessage(client, ident, action, data, isAction);
        });
    }

    /**
     * Sets the state of an avatar.
     * @throws UserError if the player is not in the game or room
     */
    public static function setAvatarState (
        gameObj :AVRGameObject, room :RoomObject, client :Client, playerId :int, 
        state :String) :void
    {
        resolvePlayerIdent(gameObj, room, playerId, function (ident :ItemIdent) :void {
            var actorOid :int = resolvePlayerWorldInfo(gameObj, room, playerId).bodyOid;
            room.roomService.setActorState(client, ident, actorOid, state);
        });
    }

    /**
     * Sets the move speed of an avatar.
     * @throws UserError if the player is not in the game or room
     */
    public static function setAvatarMoveSpeed (
        gameObj :AVRGameObject, room :RoomObject, client :Client, playerId :int, 
        pixelsPerSecond :Number) :void
    {
        resolvePlayerIdent(gameObj, room, playerId, function (ident :ItemIdent) :void {
            // todo
        });
    }

    /**
     * Sets the move speed of an avatar.
     */
    public static function setAvatarOrientation (
        gameObj :AVRGameObject, room :RoomObject, client :Client, playerId :int, 
        orient :Number) :void
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
        return new ConfirmAdapter(function (cause :String) :void {
            if (altOutputFn != null) {
                var msg :String = new Date().toLocaleTimeString();
                msg += ": service failure [service=" + svc + ", cause=" + cause + "]";
                altOutputFn(msg);

            } else {
                log.warning("Service failure", "service", svc, "cause", cause);
            }
        }, processed);
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
        var occInfo :OccupantInfo = gameObj.getOccupantInfo(new MemberName("", playerId));
        if (occInfo == null) {
            throw new UserError("Player not in game [playerId=" + playerId + "]");
        }
        occInfo = roomObj.getOccupantInfo(occInfo.username);
        if (occInfo == null) {
            throw new UserError("Player not in room [playerId=" + playerId + "]");
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
