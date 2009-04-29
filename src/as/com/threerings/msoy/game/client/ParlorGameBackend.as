//
// $Id$

package com.threerings.msoy.game.client {

import flash.display.DisplayObject;

import flash.geom.Point;

import com.threerings.util.MessageBundle;

import com.threerings.crowd.data.OccupantInfo;

import com.whirled.game.client.Thumbnail;
import com.whirled.game.client.WhirledGameBackend;
import com.whirled.game.data.GameContentOwnership;
import com.whirled.game.data.GameData;
import com.whirled.game.data.WhirledPlayerObject;

import com.threerings.msoy.client.DeploymentConfig;
import com.threerings.msoy.data.all.VizMemberName;

import com.threerings.msoy.ui.ScalingMediaContainer;

import com.threerings.msoy.item.data.all.ItemTypes;

import com.threerings.msoy.game.data.MsoyGameCodes;
import com.threerings.msoy.game.data.ParlorGameConfig;
import com.threerings.msoy.game.data.ParlorGameObject;

/**
 * Implements the various Msoy specific parts of the Whirled Game backend.
 */
public class ParlorGameBackend extends WhirledGameBackend
{
    public function ParlorGameBackend (
        ctx :GameContext, gameObj :ParlorGameObject, ctrl :ParlorGameController)
    {
        super(ctx, gameObj, ctrl);
    }

    // from WhirledGameBackend
    override protected function getHeadShot_v2 (occupantId :int) :DisplayObject
    {
        validateConnected();
        for (OccupantInfo info : _gameObj.occupantInfo) {
            if (getPlayerPersistentId(info) == occupantId) {
                var vizName :VizMemberName = info.username as VizMemberName;
                if (vizName != null) {
                    // now, we return a new one every time (in case the game wants to use two.)
                    return new Thumbnail(ScalingMediaContainer.createView(vizName.getPhoto()));
                }
                break;
            }
        }

        log.warning("Unable to find occupant, or username is not a VizMemberName",
            "occId", occupantId);
        return super.getHeadShot_v2(occupantId); // return something that works anyway
    }

    // from WhirledGameBackend
    override protected function getSize_v1 () :Point
    {
        var p :Point = super.getSize_v1();
        p.x = Math.max(p.x, 700);
        p.y = Math.max(p.y, 500);
        return p;
    }

    // from WhirledGameBackend
    override protected function isEmbedded_v1 () :Boolean
    {
        return (_ctx as GameContext).getWorldContext().getMsoyClient().isEmbedded();
    }

    // from WhirledGameBackend
    override protected function requestConsumeItemPack_v1 (ident :String, msg :String) :Boolean
    {
        if (countPlayerData(GameData.ITEM_DATA, ident, getMyId_v1()) < 1) {
            return false;
        }
        return ConsumeItemPackDialog.show((_ctx as GameContext).getWorldContext(), _ctx.getClient(),
                                          _gameObj.contentService, _gameObj.gameData, ident, msg);
    }

    override protected function systemMessage (bundle :String, msg :String) :void
    {
        (_ctx as GameContext).getWorldContext().getNotificationDirector().addGameSystemMessage(
            bundle, msg);
    }

    // from BaseGameBackend
    override protected function reportServiceFailure (service :String, cause :String) :void
    {
        // translate the error message and report it using the standard mechanism
        reportGameError((_ctx as GameContext).getWorldContext().getMessageManager().
            getBundle(MsoyGameCodes.GAME_BUNDLE).
            xlate(MessageBundle.compose("e.game_error", cause)));
    }

    // from WhirledGameBackend
    override protected function showPage_v1 (token :String) :Boolean
    {
        // handleViewUrl will do the "right thing"
        (_ctx as GameContext).getWorldContext().getMsoyController().handleViewUrl(
            DeploymentConfig.serverURL + "#" + token);
        return true;
    }

    // from WhirledGameBackend
    override protected function showGameLobby_v1 (multiplayer :Boolean) :void
    {
        // multiplayer is now ignored; it's always multiplayer
        (_ctx as GameContext).showGameLobby();
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
    override protected function showAllGames_v1 () :void
    {
        (_ctx as GameContext).getWorldContext().getMsoyController().handleViewGames();
    }

    // from WhirledGameBackend
    override protected function showInvitePage_v1 (defmsg :String, token :String = "") :void
    {
    	(_ctx as GameContext).showInvitePage(defmsg, token);
    }

    // from WhirledGameBackend
    override protected function getInviteToken_v1 () :String
    {
    	return (_ctx as GameContext).getInviteToken();
    }

    // from WhirledGameBackend
    override protected function getInviterMemberId_v1 () :int
    {
    	return (_ctx as GameContext).getInviterMemberId();
    }

    // from WhirledGameBackend
    override protected function showTrophies_v1 () :void
    {
        (_ctx as GameContext).showTrophies();
    }
}
}
