//
// $Id$

package com.threerings.msoy.room.client {

import flash.display.DisplayObject;
import flash.display.InteractiveObject;
import flash.events.Event;
import flash.events.KeyboardEvent;
import flash.events.MouseEvent;
import flash.geom.Point;
import flash.geom.Rectangle;
import flash.ui.ContextMenuItem;
import flash.ui.Keyboard;
import flash.utils.ByteArray;
import flash.utils.Dictionary;

import mx.controls.Button;
import mx.core.Application;
import mx.core.IChildList;
import mx.core.IToolTip;
import mx.core.UIComponent;
import mx.managers.ISystemManager;
import mx.managers.ToolTipManager;

import com.threerings.util.ArrayUtil;
import com.threerings.util.ClassUtil;
import com.threerings.util.Integer;
import com.threerings.util.Log;
import com.threerings.util.MessageBundle;
import com.threerings.util.ObjectMarshaller;
import com.threerings.util.ValueEvent;

import com.threerings.flash.MenuUtil;
import com.threerings.flex.CommandMenu;
import com.threerings.flex.PopUpUtil;

import com.threerings.presents.client.ClientAdapter;
import com.threerings.presents.client.ConfirmAdapter;
import com.threerings.presents.client.ResultWrapper;

import com.threerings.presents.dobj.ChangeListener;
import com.threerings.presents.dobj.MessageAdapter;
import com.threerings.presents.dobj.MessageEvent;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.whirled.client.SceneController;
import com.threerings.whirled.data.Scene;

import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.LogonPanel;
import com.threerings.msoy.client.MemberService;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyClient;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.client.PlaceBox;
import com.threerings.msoy.client.Prefs;
import com.threerings.msoy.client.TopPanel;
import com.threerings.msoy.client.TopPanel;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.ui.MediaWrapper;
import com.threerings.msoy.ui.RadialMenu;

import com.threerings.msoy.item.client.ItemService;
import com.threerings.msoy.item.data.all.Audio;
import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Decor;
import com.threerings.msoy.item.data.all.Furniture;
import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.ItemTypes;
import com.threerings.msoy.item.data.all.Pet;

import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.chat.client.ChatOverlay;
import com.threerings.msoy.chat.client.MsoyChatDirector;
import com.threerings.msoy.chat.client.ReportingListener;

import com.threerings.msoy.room.client.MsoySprite;
import com.threerings.msoy.room.data.ActorInfo;
import com.threerings.msoy.room.data.AudioData;
import com.threerings.msoy.room.data.Controllable;
import com.threerings.msoy.room.data.ControllableAVRGame;
import com.threerings.msoy.room.data.ControllableEntity;
import com.threerings.msoy.room.data.EffectData;
import com.threerings.msoy.room.data.EntityControl;
import com.threerings.msoy.room.data.EntityMemoryEntry;
import com.threerings.msoy.room.data.FurniData;
import com.threerings.msoy.room.data.MemberInfo;
import com.threerings.msoy.room.data.MsoyLocation;
import com.threerings.msoy.room.data.PetInfo;
import com.threerings.msoy.room.data.RoomObject;

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
        super.init(ctx, config);
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
        return _wdctx.getMyName().getMemberId();
    }

    /**
     * Get the display name of the user viewing a particular instance.
     */
    public function getViewerName (instanceId :int = 0) :String
    {
        var name :MemberName = _wdctx.getMyName();
        if (instanceId == 0 || instanceId == name.getMemberId()) {
            return name.toString();
        }
        return null;
        // see subclasses for more...
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
    public function canManageRoom () :Boolean
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
     * Handles a request by an item in our room to send an "action" (requires control) or a
     * "message" (doesn't require control).
     */
    public function sendSpriteMessage (
        ident :ItemIdent, name :String, arg :Object, isAction :Boolean) :void
    {
        if (isAction && !checkCanRequest(ident, "triggerAction")) {
            log.info("Dropping message for lack of control [ident=" + ident +
                     ", name=" + name + "].");
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
    public function sendSpriteSignal (name :String, arg :Object) :void
    {
        var data :ByteArray = ObjectMarshaller.validateAndEncode(arg, MAX_ENCODED_MESSAGE_LENGTH);
        sendSpriteSignal2(name, data);
    }

    /**
     * Handles a request by an actor item to change its persistent state.  Requires control.
     */
    public function setActorState (ident :ItemIdent, actorOid :int, state :String) :void
    {
        if (!checkCanRequest(ident, "setState")) {
            log.info("Dropping state change for lack of control [ident=" + ident +
                ", state=" + state + "].");
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
    public function updateMemory (ident :ItemIdent, key :String, value: Object) :Boolean
    {
        // TODO: I want to know wtf is requesting to up-mem after it's been shut down...
//        if (_roomObj == null) {
//            log.info("Dropping memory update, not in room [ident=" + ident + ", key=" + key + "].");        
//            return false;
//        }
    
        // NOTE: there is no need to be "in control" to update memory.
      
        // This will validate that the memory being set isn't greater than the maximum
        // alloted space for all memories, becauses that will surely fail on the server,
        // but the server will do further checks to ensure that this entry can be
        // safely added to the memory set such that combined they're all under the maximum.
        var data :ByteArray = ObjectMarshaller.validateAndEncode(value,
                EntityMemoryEntry.MAX_ENCODED_MEMORY_LENGTH);
        updateMemory2(ident, key, data);
        return true;
    }

    /**
     * Retrieves a published property of a given entity.
     */
    public function getEntityProperty (ident :ItemIdent, key :String) :Object
    {
        var sprite :MsoySprite = _roomView.getEntity(ident);

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
     * Add menu items for triggering actions and changing state on our avatar.
     */
    protected function addSelfMenuItems (
        avatar :MemberSprite, menuItems :Array, canControl :Boolean) :void
    {
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
    }

    /**
     * Pop up an actor menu.
     */
    protected function popActorMenu (sprite :ActorSprite, menuItems :Array) :void
    {
        // pop up the menu where the mouse is
        if (menuItems.length > 0) {
            var menu :CommandMenu = CommandMenu.createMenu(menuItems, _roomView);
            menu.variableRowHeight = true;
            menu.setBounds(_wdctx.getTopPanel().getPlaceViewBounds());
            menu.popUpAtMouse();

//            var menu :RadialMenu = new RadialMenu(100);
//            menu.dataProvider = menuItems;
//            menu.popUp(Application(Application.application));
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
     * @return undefined if the mouse isn't in our bounds, or null, or an MsoySprite.
     */
    public function getHitSprite (stageX :Number, stageY :Number, all :Boolean = false) :*
    {
        // check to make sure we're within the bounds of the place container
        var container :PlaceBox = _wdctx.getTopPanel().getPlaceContainer();
        var containerP :Point = container.localToGlobal(new Point());
        if (stageX < containerP.x || stageX > containerP.x + container.width ||
            stageY < containerP.y || stageY > containerP.y + container.height) {
            return undefined;
        }

        // first, avoid any popups
        var smgr :ISystemManager = Application.application.systemManager as ISystemManager;
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
            var spr :MsoySprite = (_roomView.getChildAt(dex) as MsoySprite);
            if ((spr != null) && (all || (spr.isActive() && spr.capturesMouse())) &&
                spr.hitTestPoint(stageX, stageY, true))
            {
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
        var hoverTarget :MsoySprite = null;

        // if shift is being held down, we're looking for locations only, so
        // skip looking for hitSprites.
        var hit :* = (_shiftDownSpot == null) ? getHitSprite(sx, sy, grabAll) : null;
        var hitter :MsoySprite = (hit as MsoySprite);
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
     * Set the sprite that the mouse is hovering over.
     */
    protected function setHoverSprite (
        sprite :MsoySprite, stageX :Number = NaN, stageY :Number = NaN) :void
    {
        // if the same sprite is glowing, we don't have to change as much..
        if (_hoverSprite == sprite) {
            updateHovered(stageX, stageY);
            if (_hoverSprite != null) {
                // but we do want to tell it about it, in case it wants
                // to glow differently depending on the location...
                _hoverSprite.setHovered(true, stageX, stageY);
            }
            return;
        }

        // otherwise, unglow the old sprite (and remove any tooltip)
        if (_hoverSprite != null) {
            _hoverSprite.setHovered(false);
            removeHoverTips();
        }

        // assign the new hoversprite
        _hoverSprite = sprite;

        // and glow the new hoversprite
        updateHovered(stageX, stageY);
    }

    /**
     * Update the hovered status of the current _hoverSprite.
     */
    protected function updateHovered (stageX :Number, stageY :Number) :void
    {
        if (_hoverSprite == null) {
            return;
        }

        var text :Object = _hoverSprite.setHovered(true, stageX, stageY);
        if (text === true) {
            return;
        }
        var tip :IToolTip = (_hoverTips.length == 1) ? IToolTip(_hoverTips[0]) : null;
        if (tip != null && tip.text != text) {
            removeHoverTips();
            tip = null;
        }
        if (tip == null && text != null) {
            addHoverTip(_hoverSprite, String(text), stageX, stageY + MOUSE_TOOLTIP_Y_OFFSET);
        }
    }

    /**
     * Utility method to create and style the hover tip for a sprite.
     */
    protected function addHoverTip (
        sprite :MsoySprite, tipText :String, stageX :Number, stageY :Number) :void
    {
        var tip :IToolTip = ToolTipManager.createToolTip(tipText, stageX, stageY);
        var tipComp :UIComponent = UIComponent(tip);
        tipComp.styleName = "roomToolTip";
        tipComp.x -= tipComp.width/2;
        tipComp.y -= tipComp.height/2;
        PopUpUtil.fit(tipComp);
        var hoverColor :uint = sprite.getHoverColor();
        tipComp.setStyle("color", hoverColor);
        if (hoverColor == 0) {
            tipComp.setStyle("backgroundColor", 0xFFFFFF);
        }

        _hoverTips.push(tip);
    }

    /**
     * Remove all currently-shown hover tips. Does not unglow the sprites.
     */
    protected function removeHoverTips () :void
    {
        for each (var tip :IToolTip in _hoverTips) {
            ToolTipManager.destroyToolTip(tip);
        }
        _hoverTips.length = 0; // truncate
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
        var hitter :MsoySprite = (hit as MsoySprite);
        if (hitter != null) {
            // let the sprite decide what to do with it
            hitter.mouseClick(event);

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
    protected function sendSpriteSignal2 (name :String, data :ByteArray) :void
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
    protected function updateMemory2 (ident :ItemIdent, key :String, data :ByteArray) :void
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

        log.info("Dropping request as we are not controller [from=" + from +
                 ", item=" + ident + "].");
        return false;
    }

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

    /**
     * Called to show the custom config panel for the specified FurniSprite in 
     * a pop-up.
     */
    public function showFurniConfigPopup (sprite :FurniSprite) :void
    {
        var configger :DisplayObject = sprite.getCustomConfigPanel();
        if (configger == null) {
            return;
        }
        showEntityPopup(sprite, Msgs.GENERAL.get("t.config_item"), configger, configger.width,
            configger.height, 0xFFFFFF, 1.0, false);
    }

    /**
     * Called from user code to show a custom popup.
     */
    internal function showEntityPopup (
        sprite :MsoySprite, title :String, panel :DisplayObject, w :Number, h :Number,
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

        _entityPopup = new EntityPopup(_wdctx, sprite, this, title, panel, w, h, color, alpha, mask);
        _entityPopup.open();
        return true;
    }

    /**
     * Clear any popup belonging to the specified sprite.
     */
    internal function clearEntityPopup (sprite :MsoySprite) :void
    {
        if (_entityPopup != null && _entityPopup.getOwningEntity() == sprite) {
            _entityPopup.close(); // will trigger callback that clears _entityPopup
        }
    }

    /**
     * A callback from the EntityPopup to let us know that it's been closed.
     */
    internal function entityPopupClosed () :void
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

    /** If true, normal sprite hovering is disabled. */
    protected var _suppressNormalHovering :Boolean;

    /** The currently hovered sprite, or null. */
    protected var _hoverSprite :MsoySprite;

    /** All currently displayed sprite tips. */
    protected var _hoverTips :Array = [];

    /** True if the shift key is currently being held down, false if not. */
    protected var _shiftDown :Boolean;

    /** If shift is being held down, the coordinates at which it was pressed. */
    protected var _shiftDownSpot :Point;

    /** The "cursor" used to display that a location is walkable. */
    protected var _walkTarget :WalkTarget = new WalkTarget();

    protected var _flyTarget :WalkTarget = new WalkTarget(true);

    protected var _entityPopup :EntityPopup;

//    protected var _entityAllowedToPop :MsoySprite;
}
}

import flash.display.BitmapData;
import flash.display.DisplayObject;
import flash.display.Sprite;

import flash.geom.Matrix;

import com.threerings.msoy.room.client.RoomElement;
import com.threerings.msoy.room.data.MsoyLocation;
import com.threerings.msoy.room.data.RoomCodes;

class WalkTarget extends Sprite
    implements RoomElement
{
    public function WalkTarget (fly :Boolean = false)
    {
        var targ :DisplayObject = (fly ? new FLYTARGET() : new WALKTARGET() as DisplayObject);
        targ.x = -targ.width/2;
        targ.y = -targ.height/2;
        addChild(targ);
    }

    // from RoomElement
    public function setLocation (newLoc :Object) :void
    {
        _loc.set(newLoc);
    }

    // from RoomElement
    public function getLocation () :MsoyLocation
    {
        return _loc;
    }

    // from RoomElement
    public function isImportant () :Boolean
    {
        return false;
    }

    // from RoomElement
    public function snapshot (bitmapData :BitmapData, matrix :Matrix) :Boolean
    {
        return true; // we do nothing, innocuously
    }

    // from RoomElement
    public function getLayoutType () :int
    {
        return RoomCodes.LAYOUT_NORMAL;
    }

    // from RoomElement
    public function getRoomLayer () :int
    {
        return RoomCodes.FURNITURE_LAYER;
    }

    // from RoomElement
    public function setScreenLocation (x :Number, y :Number, scale :Number) :void
    {
        this.x = x
        this.y = y

        // don't let the target shrink too much - 0.25 of original size at most
        var clampedScale :Number = Math.max(0.25, scale);
        this.scaleX = clampedScale;
        this.scaleY = clampedScale;
    }

    /** Our logical location. */
    protected const _loc :MsoyLocation = new MsoyLocation();

    [Embed(source="../../../../../../../rsrc/media/walkable.swf")]
    protected static const WALKTARGET :Class;

    [Embed(source="../../../../../../../rsrc/media/flyable.swf")]
    protected static const FLYTARGET :Class;
}
