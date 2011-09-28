//
// $Id$

package com.threerings.msoy.room.client {

import flash.display.DisplayObject;
import flash.events.Event;
import flash.events.KeyboardEvent;
import flash.events.MouseEvent;
import flash.events.TimerEvent;
import flash.geom.Point;
import flash.ui.Keyboard;
import flash.utils.ByteArray;
import flash.utils.Dictionary;
import flash.utils.Timer;
import flash.utils.getTimer;

import mx.core.Application;
import mx.core.IToolTip;
import mx.core.UIComponent;
import mx.events.MenuEvent;
import mx.managers.ISystemManager;
import mx.managers.ToolTipManager;

import com.threerings.util.Log;
import com.threerings.util.Map;
import com.threerings.util.Maps;
import com.threerings.util.ObjectMarshaller;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.whirled.client.SceneController;

import com.threerings.flex.CommandMenu;
import com.threerings.flex.PopUpUtil;

import com.threerings.msoy.chat.client.ChatOverlay;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.PlaceBox;
import com.threerings.msoy.client.UberClient;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.ItemTypes;
import com.threerings.msoy.room.data.ActorInfo;
import com.threerings.msoy.room.data.EntityMemories;
import com.threerings.msoy.room.data.FurniData;
import com.threerings.msoy.room.data.MsoyLocation;
import com.threerings.msoy.world.client.WorldContext;
import com.threerings.msoy.world.client.WorldControlBar;

/**
 * Manages the various interactions that take place in a room scene.
 */
public class RoomController extends SceneController
{
    /** Logging facilities. */
    protected static const log :Log = Log.getLog(RoomController);

    /** Some commands. */
    public static const FURNI_CLICKED :String = "FurniClicked";
    public static const AVATAR_CLICKED :String = "AvatarClicked";
    public static const PET_CLICKED :String = "PetClicked";

    public static const ORDER_PET :String = "OrderPet";

    public static const MAX_ENCODED_MESSAGE_LENGTH :int = 1024;

    /** The entity type groupings for querying for property owners. */
    public static const ENTITY_TYPES :Dictionary = new Dictionary();
        // static initialization...
        ENTITY_TYPES[ItemTypes.FURNITURE] = "furni";
        ENTITY_TYPES[ItemTypes.TOY] = "furni";
        ENTITY_TYPES[ItemTypes.DECOR] = "furni";
        ENTITY_TYPES[ItemTypes.AVATAR] = "avatar";
        ENTITY_TYPES[ItemTypes.OCCUPANT] = "avatar";
        ENTITY_TYPES[ItemTypes.PET] = "pet";
        // end: static initialization

    // documentation inherited
    override public function init (ctx :CrowdContext, config :PlaceConfig) :void
    {
        _wdctx = (ctx as WorldContext);
        _throttleChecker.addEventListener(TimerEvent.TIMER, handleCheckThrottles);
        super.init(ctx, config);
    }

    // documentation inherited
    override public function willEnterPlace (plobj :PlaceObject) :void
    {
        super.willEnterPlace(plobj);

        _throttleChecker.start();
    }

    // documentation inherited
    override public function didLeavePlace (plobj :PlaceObject) :void
    {
        super.didLeavePlace(plobj);

        _throttleChecker.stop();
        // see if this is causing the drop of any messages
        for each (var throttler :Throttler in _throttlers.values()) {
            throttler.noteDrop();
        }
        _throttlers.clear();
    }

    // documentation inherited
    override protected function createPlaceView (ctx :CrowdContext) :PlaceView
    {
        _roomView = new RoomView(_wdctx, this);
        return _roomView;
    }

    /**
     * Get the instanceId of all the entity instances in the room.  This is used so that two
     * instances of a pet can negotiate which client will control it, for example.
     */
    public function getEntityInstanceId () :int
    {
        // we use the memberId, which is valid (but negative) even if you're a guest
        return _wdctx.getMyId();
    }

    /**
     * Get the display name of the user viewing a particular instance.
     */
    public function getViewerName (instanceId :int = 0) :String
    {
        var name :MemberName = _wdctx.getMyName();
        if (instanceId == 0 || instanceId == name.getId()) {
            return name.toString();
        }
        return null;
        // see subclasses for more...
    }

    /**
     * Return the environment in which the entities are running.
     * See EntityControl's ENV_VIEWER, ENV_SHOP, and ENV_ROOM.
     */
    public function getEnvironment () :String
    {
        return "viewer"; // ENV_VIEWER. See subclasses.
    }

    /**
     * Return true if we're in a place in which memories will save.
     */
    public function memoriesWillSave () :Boolean
    {
        return true;
    }

    /**
     * Get the specified item's room memories in a map.
     */
    public function getMemories (ident :ItemIdent) :Object
    {
        return {}; // see subclasses
    }

    /**
     * Look up a particular memory key for the specified item.
     */
    public function lookupMemory (ident :ItemIdent, key :String) :Object
    {
        return null; // see subclasses
    }

    /**
     * Does this user have management permission in this room?
     */
    public function canManageRoom (memberId :int = 0, allowSupport :Boolean = true) :Boolean
    {
        return false;
    }

    /**
     * Requests that this client be given control of the specified item.
     */
    public function requestControl (ident :ItemIdent) :void
    {
        // see subclasses
    }

    /**
     * Requests that an item be removed from the owner's inventory.
     */
    public function deleteItem (ident :ItemIdent) :void
    {
        // see subclasses
    }

    /**
     * Requests to rate this room.
     */
    public function rateRoom (rating :Number, onSuccess :Function) :void
    {
        // see subclasses
    }

    /**
     * Called when control of an entity is assigned to us.
     */
    public function dispatchEntityGotControl (ident :ItemIdent) :void
    {
        var sprite :EntitySprite = _roomView.getEntity(ident);
        if (sprite != null) {
            sprite.gotControl();
        } else {
            log.info("Received got control for unknown sprite", "item", ident);
        }
    }

    /**
     * Handles a request by an item in our room to send an "action" (requires control) or a
     * "message" (doesn't require control).
     */
    public function sendSpriteMessage (
        ident :ItemIdent, name :String, arg :Object, isAction :Boolean) :void
    {
        if (isAction && !checkCanRequest(ident, "triggerAction")) {
            log.info("Dropping message for lack of control", "ident", ident, "name", name);
            return;
        }

        // send the request off to the server
        var data :ByteArray = ObjectMarshaller.validateAndEncode(arg, MAX_ENCODED_MESSAGE_LENGTH);
        sendSpriteMessage2(ident, name, data, isAction);
    }

    /**
     * Handles a request by an item in our room to send a "signal" to all the instances of
     * all the entities in the room. This does not require control.
     */
    public function sendSpriteSignal (ident :ItemIdent, name :String, arg :Object) :void
    {
        var data :ByteArray = ObjectMarshaller.validateAndEncode(arg, MAX_ENCODED_MESSAGE_LENGTH);
        sendSpriteSignal2(ident, name, data);
    }

    /**
     * Handles a request by an actor item to change its persistent state.  Requires control.
     */
    public function setActorState (ident :ItemIdent, actorOid :int, state :String) :void
    {
        if (!checkCanRequest(ident, "setState")) {
            log.info("Dropping state change for lack of control", "ident", ident, "state", state);
            return;
        }

        setActorState2(ident, actorOid, state);
    }

    /**
     * Handles a request by an entity item to send a chat message.
     */
    public function sendPetChatMessage (msg :String, info :ActorInfo) :void
    {
        if (checkCanRequest(info.getItemIdent(), "PetService")) {
            sendPetChatMessage2(msg, info);
        }
    }

    /**
     * Handles a request by an item in our room to update its memory.
     */
    public function updateMemory (
        ident :ItemIdent, key :String, value: Object, callback :Function) :void
    {
        // NOTE: there is no need to be "in control" to update memory.

//        validateKey(key);
        // This will validate that the memory being set isn't greater than the maximum
        // alloted space for all memories, becauses that will surely fail on the server,
        // but the server will do further checks to ensure that this entry can be
        // safely added to the memory set such that combined they're all under the maximum.
        var data :ByteArray = ObjectMarshaller.validateAndEncode(value,
                EntityMemories.MAX_ENCODED_MEMORY_LENGTH);
        updateMemory2(ident, key, data, callback);
    }

    /**
     * Retrieves a published property of a given entity.
     */
    public function getEntityProperty (ident :ItemIdent, key :String) :Object
    {
        var sprite :EntitySprite = _roomView.getEntity(ident);

        return (sprite == null) ? null : sprite.lookupEntityProperty(key);
    }

    /**
     * Get the ID strings of all entities of the specified type, or all if type is null.
     * @see ENTITY_TYPES
     */
    public function getEntityIds (type :String = null) :Array
    {
        var idents :Array = _roomView.getItemIdents();

        // Filter for items of the correct type, if necessary
        if (type != null) {
            idents = idents.filter(
                function (id :ItemIdent, ... etc) :Boolean {
                    // Is the entity a valid item of this type?
                    return (ENTITY_TYPES[id.type] == type);
                });
        }

        // Convert from ItemIdents to entityId Strings
        return idents.map(
            function (id :ItemIdent, ... etc) :String {
                return id.toString();
            });
    }

    /**
     * Handles a request by an actor to change its location. Returns true if the request was
     * dispatched, false if funny business prevented it.
     */
    public function requestMove (ident :ItemIdent, newloc :MsoyLocation) :Boolean
    {
        // handled in subclasses
        return false;
    }

    /**
     * Called to enact the avatar action globally: all users will see it.
     */
    public function doAvatarAction (action :String) :void
    {
        sendSpriteMessage(_roomView.getMyAvatar().getItemIdent(), action, null, true);
    }

    /**
     * Called to enact an avatar state change.
     */
    public function doAvatarState (state :String) :void
    {
        var avatar :MemberSprite = _roomView.getMyAvatar();
        setActorState(avatar.getItemIdent(), avatar.getOid(), state);
    }

    /**
     * Handles FURNI_CLICKED.
     */
    public function handleFurniClicked (furni :FurniData) :void
    {
        // see subclasses
    }

    /**
     * Either mutes or unmutes the sounds belonging to a memberId's avatars and pets.
     */
    public function squelchPlayer (player :MemberName, squelch :Boolean) :void
    {
        // see subclasses
    }

    /**
     * Add menu items for triggering actions and changing state on our avatar.
     */
    protected function addSelfMenuItems (
        avatar :MemberSprite, menuItems :Array, canControl :Boolean) :void
    {
        CommandMenu.addSeparator(menuItems);

        // create a sub-menu for playing avatar actions
        var actions :Array = avatar.getAvatarActions();
        if (actions.length > 0) {
            var worldActions :Array = [];
            for each (var act :String in actions) {
                worldActions.push({ label: act, callback: doAvatarAction, arg: act });
            }
            menuItems.push({ label: Msgs.GENERAL.get("l.avAction"),
                children: worldActions, enabled: canControl });
        }

        // create a sub-menu for changing avatar states
        var states :Array = avatar.getAvatarStates();
        if (states.length > 0) {
            var worldStates :Array = [];
            var curState :String = avatar.getState();
            // NOTE: we usually want to translate null into the first state, however, if there's
            // only one state registered, let them select it anyway by not translating.
            if (states.length > 1 && curState == null) {
                curState = states[0];
            }
            for each (var state :String in states) {
                worldStates.push({ label: state, callback: doAvatarState, arg :state,
                    enabled: (curState != state) });
            }
            menuItems.push({ label: Msgs.GENERAL.get("l.avStates"),
                children: worldStates, enabled: canControl });
        }

        // custom config
        if (avatar.hasCustomConfigPanel()) {
            menuItems.push({ label: Msgs.GENERAL.get("b.config_item", "avatar"),
                callback: showConfigPopup, arg: avatar, enabled: memoriesWillSave() });
        }
    }

    /**
     * Pop up an actor menu.
     */
    protected function popActorMenu (sprite :ActorSprite, menuItems :Array) :void
    {
        // pop up the menu where the mouse is
        if (menuItems.length > 0) {
            var menu :CommandMenu = CommandMenu.createMenu(menuItems, _roomView);
            menu.setBounds(_wdctx.getTopPanel().getPlaceViewBounds());
            menu.popUpAtMouse();
            menu.addEventListener(MenuEvent.MENU_HIDE, function (event :MenuEvent) :void {
                if (event.menu == menu) {
                    _clickSuppress = sprite;
                    _wdctx.getTopPanel().systemManager.topLevelSystemManager.getSandboxRoot().
                        addEventListener(MouseEvent.MOUSE_DOWN, clearClickSuppress, false, 0, true);
                }
            });

//            var menu :RadialMenu = new RadialMenu();
//            menu.dataProvider = menuItems;
//            menu.popUp(UberClient.getApplication());
        }
    }

    /**
     * Handles AVATAR_CLICKED.
     */
    public function handleAvatarClicked (avatar :MemberSprite) :void
    {
        // nada here, see subclasses
    }

    /**
     * Handles PET_CLICKED.
     */
    public function handlePetClicked (pet :ActorSprite) :void
    {
        // see subclasses
    }

    /**
     * Handles ORDER_PET.
     */
    public function handleOrderPet (petId :int, command :int) :void
    {
        // see subclasses / TODO
    }

    /**
     * Get the top-most sprite mouse-capturing sprite with a non-transparent pixel at the specified
     * location.
     *
     * @return undefined if the mouse isn't in our bounds, or null, or an EntitySprite.
     */
    public function getHitSprite (stageX :Number, stageY :Number, all :Boolean = false) :EntitySprite
    {
        // check to make sure we're within the bounds of the place container
        var container :PlaceBox = _wdctx.getTopPanel().getPlaceContainer();
        var containerP :Point = container.localToGlobal(new Point());
        if (stageX < containerP.x || stageX > containerP.x + container.width ||
            stageY < containerP.y || stageY > containerP.y + container.height) {
            return undefined;
        }

        // first, avoid any popups
        var smgr :ISystemManager = UberClient.getApplication().systemManager;
        var ii :int;
        var disp :DisplayObject;
        for (ii = smgr.numChildren - 1; ii >= 0; ii--) {
            disp = smgr.getChildAt(ii)
            if ((disp is Application) || (disp is UIComponent && !UIComponent(disp).visible)) {
                continue;
            }
            if (disp.hitTestPoint(stageX, stageY)) {
                return undefined;
            }
        }

        // then check with the PlaceBox
        if (container.overlaysMousePoint(stageX, stageY)) {
            return undefined;
        }

        // then avoid any chat glyphs that are clickable
        var overlay :ChatOverlay = _wdctx.getTopPanel().getChatOverlay();
        if (overlay != null && overlay.hasClickableGlyphsAtPoint(stageX, stageY)) {
            return undefined;
        }

        // we search from last-drawn to first drawn to get the topmost...
        for (var dex :int = _roomView.numChildren - 1; dex >= 0; dex--) {
            var viz :DisplayObject = _roomView.getChildAt(dex);
            var el :RoomElement = _roomView.vizToEntity(viz);
            if (el == null || !(el is EntitySprite)) {
                continue;
            }
            var spr :EntitySprite = (el as EntitySprite);
            if ((all || (spr.isActive() && spr.capturesMouse())) &&
                viz.hitTestPoint(stageX, stageY, true)) {
                return spr;
            }
        }

        return null;
    }

    /**
     * Handles ENTER_FRAME and see if the mouse is now over anything.  Normally the flash player
     * will dispatch mouseOver/mouseLeft for an object even if the mouse isn't moving: the sprite
     * could move.  Since we're hacking in our own mouseOver handling, we emulate that.  Gah.
     */
    protected function checkMouse (event :Event) :void
    {
        // skip if supressed, and freak not out if we're temporarily removed from the stage
        if (_suppressNormalHovering || _roomView.stage == null || !_roomView.isShowing()) {
            return;
        }

//        // in case a mouse event was captured by an entity, prevent it from adding a popup later
//        _entityAllowedToPop = null;
        checkMouse2(false, false, null);
    }

    /**
     * Helper for checkMouse. Broken out for subclasses to override.
     *
     * @param grabAll if true, grab all sprites
     * @param allowMovement only consulted when grabAll is true.
     * @param setHitter only used when grabAll is true.
     */
    protected function checkMouse2 (
        grabAll :Boolean, allowMovement :Boolean, setHitter :Function) :void
    {
        var sx :Number = _roomView.stage.mouseX;
        var sy :Number = _roomView.stage.mouseY;
        var showWalkTarget :Boolean = false;
        var showFlyTarget :Boolean = false;
        var hoverTarget :EntitySprite = null;

        // if shift is being held down, we're looking for locations only, so
        // skip looking for hitSprites.
        var hit :* = (_shiftDownSpot == null) ? getHitSprite(sx, sy, grabAll) : null;
        var hitter :EntitySprite = (hit as EntitySprite);
        // ensure we hit no pop-ups
        if (hit !== undefined) {
            hoverTarget = hitter;
            if (hitter == null) {
                var cloc :ClickLocation = _roomView.layout.pointToAvatarLocation(
                    sx, sy, _shiftDownSpot, RoomMetrics.N_UP);

                if (cloc != null && _wdctx.worldProps.userControlsAvatar) {
                    addAvatarYOffset(cloc);
                    if (cloc.loc.y != 0) {
                        _flyTarget.setLocation(cloc.loc);
                        _roomView.layout.updateScreenLocation(_flyTarget);
                        showFlyTarget = true;

                        // Set the Y to 0 and use it for the walkTarget
                        cloc.loc.y = 0;
                        _walkTarget.alpha = .5;

                    } else {
                        _walkTarget.alpha = 1;
                    }

                    // don't show the walk target if we're "in front" of the room view
                    showWalkTarget = (cloc.loc.z >= 0);
                    _walkTarget.setLocation(cloc.loc);
                    _roomView.layout.updateScreenLocation(_walkTarget);
                }

            } else if (!hoverTarget.hasAction()) {
                // it may have captured the mouse, but it doesn't actually
                // have any action, so we don't hover it.
                hoverTarget = null;
            }

            // if we're in grab-all mode, we operate slightly differently: don't actually highlight
            if (grabAll) {
                hoverTarget = null;

                // possibly hide walk targets too
                showWalkTarget = (showWalkTarget && allowMovement);
                showFlyTarget = (showFlyTarget && allowMovement);

                // indicate which sprite is being hovered
                setHitter(hitter);
            }
        }

        _walkTarget.visible = showWalkTarget;
        _flyTarget.visible = showFlyTarget;

        setHoverSprite(hoverTarget, sx, sy);
    }

    /**
     * Set the special singleton sprite that the mouse is hovering over.
     */
    protected function setHoverSprite (
        sprite :EntitySprite, stageX :Number = NaN, stageY :Number = NaN) :void
    {
        if (sprite is FurniSprite && WorldControlBar(_wdctx.getControlBar()).hotZoneBtn.selected) {
            return; // not right now, they're all hovered
        }

        // iff the sprite has changed..
        if (_hoverSprite != sprite) {
            // unglow the old sprite (and remove any tooltip)
            if (_hoverSprite != null) {
                setSpriteHovered(_hoverSprite, false, stageX, stageY);
            }

            // assign the new hoversprite, maybe assigning to null
            _hoverSprite = sprite;
        }

        // glow the sprite (and give it a chance update the tip)
        if (_hoverSprite != null) {
            setSpriteHovered(_hoverSprite, true, stageX, stageY);
        }
    }

    /**
     * This sets the particular sprite to be hovered or no. This can be called even for
     * sprites other than the _hoverSprite.
     */
    public function setSpriteHovered (
        sprite :EntitySprite, hovered: Boolean, stageX :Number = NaN, stageY :Number = NaN) :void
    {
        // update the glow on the sprite
        var text :Object = sprite.setHovered(hovered, stageX, stageY);
        if (hovered && text === true) {
            return; // this is a special-case shortcut we use to save time.
        }
        var tip :IToolTip = IToolTip(_hoverTips[sprite]);
        // maybe remove the tip
        if ((tip != null) && (!hovered || text == null || tip.text != text)) {
            delete _hoverTips[sprite];
            ToolTipManager.destroyToolTip(tip);
            tip = null;
        }
        // maybe add a new tip
        if (hovered && (text != null)) {
            if (isNaN(stageX) || isNaN(stageY)) {
                var p :Point = sprite.viz.localToGlobal(sprite.getLayoutHotSpot());
                stageX = p.x;
                stageY = p.y;
            }
            _hoverTips[sprite] = addHoverTip(
                sprite, String(text), stageX, stageY + MOUSE_TOOLTIP_Y_OFFSET);
        }
    }

    /**
     * Utility method to create and style the hover tip for a sprite.
     */
    protected function addHoverTip (
        sprite :EntitySprite, tipText :String, stageX :Number, stageY :Number) :IToolTip
    {
        var tip :IToolTip = ToolTipManager.createToolTip(tipText, stageX, stageY);
        var tipComp :UIComponent = UIComponent(tip);
        tipComp.styleName = "roomToolTip";
        tipComp.x -= tipComp.width/2;
        tipComp.y -= tipComp.height/2;
        PopUpUtil.fitInRect(tipComp, _wdctx.getTopPanel().getPlaceViewBounds());
        var hoverColor :uint = sprite.getHoverColor();
        tipComp.setStyle("color", hoverColor);
        if (hoverColor == 0) {
            tipComp.setStyle("backgroundColor", 0xFFFFFF);
        }
        return tip;
    }

    protected function mouseClicked (event :MouseEvent) :void
    {
        if (_suppressNormalHovering) {
            return;
        }

//        // at this point, the mouse click is bubbling back out, and the entity is no
//        // longer allowed to pop up a popup.
//        _entityAllowedToPop = null;

        // if shift is being held down, we're looking for locations only, so skip looking for
        // hitSprites.
        var hit :* = (_shiftDownSpot == null) ? getHitSprite(event.stageX, event.stageY) : null;
        if (hit === undefined) {
            return;
        }

        // deal with the target
        var hitter :EntitySprite = (hit as EntitySprite);
        if (hitter != null) {
            // let the sprite decide what to do with it
            if (hitter != _clickSuppress) {
                hitter.mouseClick(event);
            }

        } else if (_wdctx.worldProps.userControlsAvatar) {
            var curLoc :MsoyLocation = _roomView.getMyCurrentLocation();
            if (curLoc == null) {
                return; // we've already left, ignore the click
            }

            // calculate where the location is
            var cloc :ClickLocation = _roomView.layout.pointToAvatarLocation(
                event.stageX, event.stageY, _shiftDownSpot, RoomMetrics.N_UP);
            // disallow clicking in "front" of the scene when minimized
            if (cloc != null && cloc.loc.z >= 0) {
                // orient the location as appropriate
                addAvatarYOffset(cloc);
                var newLoc :MsoyLocation = cloc.loc;
                var degrees :Number = 180 / Math.PI *
                    Math.atan2(newLoc.z - curLoc.z, newLoc.x - curLoc.x);
                // we rotate so that 0 faces forward
                newLoc.orient = (degrees + 90 + 360) % 360;
                // effect the move
                requestAvatarMove(newLoc);
            }
        }
    }

    /**
     * Clear out the click suppress sprite.
     */
    protected function clearClickSuppress (event :MouseEvent) :void
    {
        _clickSuppress = null;
        _wdctx.getTopPanel().systemManager.topLevelSystemManager.getSandboxRoot().
            removeEventListener(MouseEvent.MOUSE_DOWN, clearClickSuppress);
    }

    /**
     * Effect a move for our avatar.
     */
    protected function requestAvatarMove (newLoc :MsoyLocation) :void
    {
        // nada here, see subclasses
    }

    /**
     * Once the state change has been validated, effect it.
     */
    protected function setActorState2 (ident :ItemIdent, actorOid :int, state :String) :void
    {
        // see subclasses
    }

    /**
     * Once a sprite message is validated and ready to go, it is sent here.
     */
    protected function sendSpriteMessage2 (
        ident :ItemIdent, name :String, data :ByteArray, isAction :Boolean) :void
    {
        // see subclasses
    }

    /**
     * Once a sprite signal is validated and ready to go, it is sent here.
     */
    protected function sendSpriteSignal2 (ident :ItemIdent, name :String, data :ByteArray) :void
    {
        // see subclasses
    }

    /**
     * Once a pet chat is validated and ready to go, it is sent here.
     */
    protected function sendPetChatMessage2 (msg :String, info :ActorInfo) :void
    {
        // see subclasses
    }

    /**
     * Once a memory update is validated and ready to go, it is sent here.
     */
    protected function updateMemory2 (
        ident :ItemIdent, key :String, data :ByteArray, callback :Function) :void
    {
        // see subclasses
    }

    protected function keyEvent (event :KeyboardEvent) :void
    {
        try {
            var keyDown :Boolean = event.type == KeyboardEvent.KEY_DOWN;
            switch (event.keyCode) {
            case Keyboard.F4:
                _roomView.dimAvatars(keyDown);
                return;

            case Keyboard.F5:
                _roomView.dimFurni(keyDown);
                return;

            case Keyboard.SHIFT:
                _shiftDown = keyDown;
                if (keyDown) {
                    if (_walkTarget.visible && (_shiftDownSpot == null)) {
                        // record the y position at this
                        _shiftDownSpot = new Point(_roomView.stage.mouseX, _roomView.stage.mouseY);
                    }

                } else {
                    _shiftDownSpot = null;
                }
                return;
            }

            if (keyDown) {
                switch (event.charCode) {
                case 91: // '['
                    _roomView.scrollViewBy(-ROOM_SCROLL_INCREMENT);
                    break;

                case 93: // ']'
                    _roomView.scrollViewBy(ROOM_SCROLL_INCREMENT);
                    break;
                }
            }

        } finally {
            event.updateAfterEvent();
        }
    }

    /**
     * Return the avatar's preferred y offset for normal mouse positioning,
     * unless shift is being held down, in which case use 0 so that the user
     * can select their height precisely.
     */
    protected function addAvatarYOffset (cloc :ClickLocation) :void
    {
        if (_shiftDownSpot != null) {
            return;
        }

        var av :MemberSprite = _roomView.getMyAvatar();
        if (av != null) {
            var prefY :Number = av.getPreferredY() / _roomView.layout.metrics.sceneHeight;
            // but clamp their preferred Y into the normal range
            cloc.loc.y = Math.min(1, Math.max(0, prefY));
        }
    }

    /**
     * Ensures that we can issue a request to update the distributed state of the specified item,
     * returning true if so, false if we don't yet have a room object or are not in control of that
     * item.
     */
    protected function checkCanRequest (ident :ItemIdent, from :String) :Boolean
    {
        // make sure we are in control of this entity (or that no one has control)
        var result :Object = hasEntityControl(ident);
        if (result == null || result == true) {
            // it's ok if nobody has control
            return true;
        }

        log.info("Dropping request as we are not controller", "from", from, "item", ident);
        return false;
    }

//    protected function validateKey (key :String) :void
//    {
//        if (key == null) {
//            throw new Error("Null is an invalid key!");
//        }
//        for (var ii :int = 0; ii < key.length; ii++) {
//            if (key.charAt(ii) > "\u007F") {
//                throw new Error("Unicode characters not supported in keys.");
//            }
//        }
//    }

    /**
     * Does this client have control over the specified entity?
     *
     * Side-effect: The gotControl() will always be re-dispatched to the entity if it does.
     * The newest EntityControl will suppress repeats.
     *
     * @returns true, false, or null if nobody currently has control.
     */
    protected function hasEntityControl (ident :ItemIdent) :Object
    {
        return true;
    }

    protected function throttle (ident :ItemIdent, fn :Function, ... args) :void
    {
        var av :EntitySprite = _roomView.getMyAvatar();
        if (av != null && av.getItemIdent().equals(ident)) {
            // our own avatar is never throttled
            fn.apply(null, args);

        } else {
            // else, subject to throttling
            var throttler :Throttler = _throttlers.get(ident) as Throttler;
            if (throttler == null) {
                throttler = new Throttler(ident);
                _throttlers.put(ident, throttler);
            }
            throttler.processOp(fn, args);
        }
    }

    /**
     * Check queued messages to see if there are any we should send now.
     */
    protected function handleCheckThrottles (... ignored) :void
    {
        var now :int = getTimer();
        for each (var throttler :Throttler in _throttlers.values()) {
            // make sure it's a valid entity still
            // (We do this here instead of at the time the sprite is removed because sometimes
            // things are removed and re-added when simply repositioned. This lets that settle..).
            if (null == _roomView.getEntity(throttler.ident)) {
                throttler.noteDrop();
                _throttlers.remove(throttler.ident);

            } else {
                // it's in the room, it's cool
                throttler.checkOps(now);
            }
        }
    }

    /**
     * Called to show the custom config panel for the specified FurniSprite in
     * a pop-up.
     */
    public function showConfigPopup (sprite :EntitySprite) :Boolean
    {
        if (_entityPopup != null && _entityPopup.getOwningEntity() == sprite) {
            return true;
        }

        var configger :DisplayObject = sprite.getCustomConfigPanel();
        if (configger == null) {
            return false;
        }
        return showEntityPopup(sprite, Msgs.GENERAL.get("t.config_item"), configger,
            configger.width, configger.height, 0xFFFFFF, 1.0, false);
    }

    /**
     * Called from user code to show a custom popup.
     */
    internal function showEntityPopup (
        sprite :EntitySprite, title :String, panel :DisplayObject, w :Number, h :Number,
        color :uint = 0xFFFFFF, alpha :Number = 1.0, mask :Boolean = true) :Boolean
    {
//        if (_entityAllowedToPop != sprite) {
//            return false;
//        }

        // other people's avatars cannot put a popup on our screen...
        if ((sprite is MemberSprite) && (sprite != _roomView.getMyAvatar())) {
            return false;
        }

        if (isNaN(w) || isNaN(h) || w <= 0 || h <= 0 || title == null || panel == null) {
            return false;
        }

        // close any existing popup
        if (_entityPopup != null) {
            _entityPopup.close();
        }

        _entityPopup = new EntityPopup(_wdctx, sprite, title, panel, w, h, color, alpha, mask);
        _entityPopup.addCloseCallback(entityPopupClosed);
        _entityPopup.open();
        return true;
    }

    /**
     * Clear any popup belonging to the specified sprite.
     */
    public function clearEntityPopup (sprite :EntitySprite) :void
    {
        if (_entityPopup != null && _entityPopup.getOwningEntity() == sprite) {
            _entityPopup.close(); // will trigger callback that clears _entityPopup
        }
    }

    /**
     * A callback from the EntityPopup to let us know that it's been closed.
     */
    protected function entityPopupClosed () :void
    {
        _entityPopup = null;
    }

    /** The number of pixels we scroll the room on a keypress. */
    protected static const ROOM_SCROLL_INCREMENT :int = 20;

    /** The event to send to GWT when a background property has changed. */
    protected static const BACKGROUND_CHANGED_EVENT :String = "backgroundChanged";

    /** The event to send to GWT when furni has been added or removed. */
    protected static const FURNI_CHANGED_EVENT :String = "furniChanged";

    /** The amount we alter the y coordinate of tooltips generated under the mouse. */
    protected static const MOUSE_TOOLTIP_Y_OFFSET :int = 50;

    /** The life-force of the client. */
    protected var _wdctx :WorldContext;

    /** The room view that we're controlling. */
    protected var _roomView :RoomView;

    /** Contains active throttlers. ItemIdent -> Throttler. */
    protected var _throttlers :Map = Maps.newMapOf(ItemIdent);

    protected var _throttleChecker :Timer = new Timer(500);

    /** If true, normal sprite hovering is disabled. */
    protected var _suppressNormalHovering :Boolean;

    /** The currently hovered sprite, or null. */
    protected var _hoverSprite :EntitySprite;

    /** All currently displayed sprite tips, indexed by EntitySprite. */
    protected var _hoverTips :Dictionary = new Dictionary(true);

    /** True if the shift key is currently being held down, false if not. */
    protected var _shiftDown :Boolean;

    /** If shift is being held down, the coordinates at which it was pressed. */
    protected var _shiftDownSpot :Point;

    /** Assigned to a sprite that we should not process clicks upon. */
    protected var _clickSuppress :Object;

    /** The "cursor" used to display that a location is walkable. */
    protected var _walkTarget :WalkTarget = new WalkTarget();

    protected var _flyTarget :WalkTarget = new WalkTarget(true);

    protected var _entityPopup :EntityPopup;

//    protected var _entityAllowedToPop :EntitySprite;
}
}

import flash.display.DisplayObject;

import com.threerings.util.Log;
import com.threerings.util.Throttle;

import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.room.client.RoomElementSprite;
import com.threerings.msoy.room.data.RoomCodes;

class WalkTarget extends RoomElementSprite
{
    public function WalkTarget (fly :Boolean = false)
    {
        var targ :DisplayObject = (fly ? new FLYTARGET() : new WALKTARGET()) as DisplayObject;
        targ.x = -targ.width/2;
        targ.y = -targ.height/2;
        addChild(targ);
    }

    override public function getRoomLayer () :int
    {
        return RoomCodes.FURNITURE_LAYER;
    }

    // from RoomElement
    override public function setScreenLocation (x :Number, y :Number, scale :Number) :void
    {
        // don't let the target shrink too much - 0.25 of original size at most
        super.setScreenLocation(x, y, Math.max(0.25, scale));
    }

    [Embed(source="../../../../../../../rsrc/media/walkable.swf")]
    protected static const WALKTARGET :Class;

    [Embed(source="../../../../../../../rsrc/media/flyable.swf")]
    protected static const FLYTARGET :Class;
}

/**
 * Throttles entity communications.
 */
class Throttler
{
    public static const OPERATIONS :int = 5;
    public static const PERIOD :int = 1000;
    public static const MAX_QUEUE :int = 20;

    public static const log :Log = Log.getLog(Throttler);

    /** The ident associated with this Throttler. */
    public var ident :ItemIdent;

    public function Throttler (ident :ItemIdent)
    {
        this.ident = ident;
    }

    public function processOp (fn :Function, args :Array) :void
    {
        if (_throttle.throttleOp()) {
            if (_queue.length < MAX_QUEUE) {
                _queue.push([ fn, args ]);
                log.info("Queued entity message", "ident", ident, "queueSize", _queue.length);

            } else {
                log.warning("Dropping entity message", "ident", ident);
            }

        } else {
            fn.apply(null, args);
            //log.debug("Message not throttled", "ident", ident);
        }
    }

    public function checkOps (now :int) :void
    {
        while (_queue.length > 0) {
            if (_throttle.throttleOpAt(now)) {
                break;
            }
            var item :Array = _queue.shift() as Array;
            (item[0] as Function).apply(null, item[1] as Array);
            //log.debug("Sent queued message", "ident", ident);
        }
    }

    public function noteDrop () :void
    {
        if (_queue.length > 0) {
            log.warning("Queued messages dropped", "ident", ident, "queueSize", _queue.length);
        }
    }

    protected var _throttle :Throttle = new Throttle(OPERATIONS, PERIOD);

    protected var _queue :Array = [];
}
