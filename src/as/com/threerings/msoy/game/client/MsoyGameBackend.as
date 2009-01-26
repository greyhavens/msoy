//
// $Id$

package com.threerings.msoy.game.client {

import flash.display.DisplayObject;

import flash.geom.Point;

import com.threerings.util.MessageBundle;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.SetAdapter;

import com.threerings.crowd.data.OccupantInfo;

import com.whirled.game.client.HeadShot;
import com.whirled.game.client.WhirledGameBackend;
import com.whirled.game.data.GameContentOwnership;
import com.whirled.game.data.WhirledGameObject;

import com.threerings.msoy.data.all.VizMemberName;

import com.threerings.msoy.ui.ScalingMediaContainer;

import com.threerings.msoy.item.data.all.ItemTypes;

import com.threerings.msoy.game.data.MsoyGameCodes;
import com.threerings.msoy.game.data.MsoyGameConfig;
import com.threerings.msoy.game.data.PlayerObject;

/**
 * Implements the various Msoy specific parts of the Whirled Game backend.
 */
public class MsoyGameBackend extends WhirledGameBackend
{
    public function MsoyGameBackend (
        ctx :GameContext, gameObj :WhirledGameObject, ctrl :MsoyGameController)
    {
        super(ctx, gameObj, ctrl);

        _ctx.getClient().getClientObject().addListener(_contentListener);
    }

    // from WhirledGameBackend
    override public function shutdown () :void
    {
        super.shutdown();

        _ctx.getClient().getClientObject().removeListener(_contentListener);
    }

    // from WhirledGameBackend
    override protected function getHeadShot_v2 (occupant :int) :DisplayObject
    {
        validateConnected();
        var info :OccupantInfo = _gameObj.occupantInfo.get(occupant) as OccupantInfo;
        if (info != null) {
            var vizName :VizMemberName = info.username as VizMemberName;
            if (vizName != null) {
                // now, we return a new one every time (in case the game wants to use two.)
                return new HeadShot(ScalingMediaContainer.createView(vizName.getPhoto()));
            }
        }

        log.warning("Unable to find occupant, or username is not a VizMemberName: " + occupant);
        return super.getHeadShot_v2(occupant); // return something that works anyway
    }

    // from WhirledGameBackend
    override protected function getSize_v1 () :Point
    {
        var p :Point = super.getSize_v1();
        p.x = Math.max(p.x, 700);
        p.y = Math.max(p.y, 500);
        return p;
    }

    // from BaseGameBackend
    override protected function reportServiceFailure (service :String, cause :String) :void
    {
        // translate the error message and report it using the standard mechanism
        reportGameError((_ctx as GameContext).getMsoyContext().getMessageManager().
            getBundle(MsoyGameCodes.GAME_BUNDLE).
            xlate(MessageBundle.compose("e.game_error", cause)));
    }

    // from BaseGameBackend
    override protected function playerOwnsData (type :int, ident :String, playerId :int) :Boolean
    {
        if (playerId != CURRENT_USER && playerId != getMyId_v1()) {
            throw new Error("Query of other user data not allowed");
        }
        var cfg :MsoyGameConfig = (_ctrl.getPlaceConfig() as MsoyGameConfig);
        return (_ctx as GameContext).getPlayerObject().ownsGameContent(cfg.getGameId(), type, ident)
    }

    // TEMP: provide the 'back to lobby' link for games that have it.
    // TODO: Remove once we have the new standard game-over display sorted out
    override protected function backToWhirled_v1 (showLobby :Boolean = false) :void
    {
        (_ctx as GameContext).backToWhirled(true); // always show lobby
    }

    // from WhirledGameBackend
    override protected function showGameLobby_v1 (multiplayer :Boolean) :void
    {
        (_ctx as GameContext).showGameLobby(multiplayer);
    }    
    
    // from WhirledGameBackend
    override protected function showGameShop_v1 (itemType :String, catalogId :int = 0) :void
    {
        // hide the integer item codes from the sdk
        var itemTypeCode :int;
        switch (itemType) {
            case "item_packs":
                itemTypeCode = ItemTypes.ITEM_PACK;
                break;
            case "level_packs":
                itemTypeCode = ItemTypes.LEVEL_PACK;
                break;
            case "avatars":
                itemTypeCode = ItemTypes.AVATAR;
                break;
            case "furniture":
                itemTypeCode = ItemTypes.FURNITURE;
                break;
            case "backdrops":
                itemTypeCode = ItemTypes.DECOR;
                break;
            case "toys":
                itemTypeCode = ItemTypes.TOY;
                break;
            case "pets":
                itemTypeCode = ItemTypes.PET;
                break;
            default:
                // if code is unknown, do nothing.
                return;
        }
        (_ctx as GameContext).showGameShop(itemTypeCode, catalogId);
    }

    // from WhirledGameBackend
    override protected function showTrophies_v1 () :void
    {
        (_ctx as GameContext).showTrophies();
    }

    protected function entryAddedOnUserObject (event :EntryAddedEvent) :void
    {
        var name :String = event.getName();
        if (name == PlayerObject.GAME_CONTENT) {
            var content :GameContentOwnership = (event.getEntry() as GameContentOwnership);
            // it should not be possible for a player to have content dynamically added for a game
            // other than the one they are playing, but let's be extra specially safe
            var cfg :MsoyGameConfig = (_ctrl.getPlaceConfig() as MsoyGameConfig);
            if (cfg.getGameId() == content.gameId) {
                // TODO: use playerId when we switch to that from playerOid
                notifyGameContentAdded(content.type, content.ident, event.getTargetOid());
            }
        }
    }

    protected var _contentListener :SetAdapter = new SetAdapter(entryAddedOnUserObject);
}
}
