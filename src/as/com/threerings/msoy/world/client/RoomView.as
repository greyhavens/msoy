//
// $Id$

package com.threerings.msoy.world.client {

import flash.display.DisplayObject;
import flash.display.Graphics;
import flash.display.Shape;
import flash.display.Sprite;

import flash.events.Event;

import flash.geom.Point;
import flash.geom.Rectangle;

import flash.utils.ByteArray;
import flash.utils.getTimer; // function import

import com.threerings.util.ArrayUtil;
import com.threerings.util.ConfigValueSetEvent;
import com.threerings.util.HashMap;
import com.threerings.util.Iterator;
import com.threerings.util.Log;
import com.threerings.util.ObjectMarshaller;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.flash.DisplayUtil;
import com.threerings.flash.MenuUtil;

import com.threerings.whirled.spot.data.Location;
import com.threerings.whirled.spot.data.Portal;

import com.threerings.msoy.client.ChatPlaceView;
import com.threerings.msoy.client.ContextMenuProvider;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.MsoyPlaceView;
import com.threerings.msoy.client.PlaceBox;
import com.threerings.msoy.client.Prefs;
import com.threerings.msoy.client.UberClient;
import com.threerings.msoy.item.data.all.Decor;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.MediaDesc;

import com.threerings.msoy.world.client.layout.RoomLayout;
import com.threerings.msoy.world.client.layout.RoomLayoutFactory;
import com.threerings.msoy.world.data.EntityMemoryEntry;
import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.MsoySceneModel;
import com.threerings.msoy.world.data.RoomObject;
import com.threerings.msoy.world.data.RoomPropertyEntry;

/**
 * The base room view. Should not contain any RoomObject or other network-specific crap.
 */
public class RoomView extends Sprite
    implements MsoyPlaceView, ChatPlaceView, ContextMenuProvider
{
    /** Logging facilities. */
    protected static const log :Log = Log.getLog(RoomView);

    /**
     * Constructor.
     */
    public function RoomView (ctx :WorldContext, ctrl :RoomController)
    {
        _ctx = ctx;
        _ctrl = ctrl;
        _layout = RoomLayoutFactory.createLayout(null, this);

        // listen for preferences changes, update zoom
        Prefs.config.addEventListener(ConfigValueSetEvent.CONFIG_VALUE_SET,
            handlePrefsUpdated, false, 0, true);
    }

    /**
     * Returns the room controller.
     */
    public function getRoomController () :RoomController
    {
        return _ctrl;
    }

    /**
     * Returns the layout object responsible for room layout.
     */
    public function get layout () :RoomLayout
    {
        return _layout;
    }

    // from MsoyPlaceView
    public function setPlaceSize (unscaledWidth :Number, unscaledHeight :Number) :void
    {
        _actualWidth = unscaledWidth;
        _actualHeight = unscaledHeight;
        relayout();
    }

    // from MsoyPlaceView
    public function setIsShowing (showing :Boolean) :void
    {
        _showing = showing;
    }

    // from MsoyPlaceView
    public function padVertical () :Boolean
    {
        return true;
    }

    /**
     * Are we actually showing?
     */
    public function isShowing () :Boolean
    {
        return _showing;
    }

    // from ContextMenuProvider
    public function populateContextMenu (ctx :MsoyContext, menuItems :Array) :void
    {
        var hit :* = _ctrl.getHitSprite(stage.mouseX, stage.mouseY, true);
        if (hit === undefined) {
            return;
        } 
        var sprite :MsoySprite = (hit as MsoySprite);
        if (sprite == null) {
            if (_bg == null) {
                return;
            } else { 
                sprite = _bg;
            }
        } 

        populateSpriteContextMenu(sprite, menuItems);
    }

    /**
     * Called by MsoySprite instances when they've had their location updated.
     */
    public function locationUpdated (sprite :MsoySprite) :void
    {
        _layout.updateScreenLocation(sprite, sprite.getLayoutHotSpot());

        if (sprite == _bg && _scene.getSceneType() == Decor.FIXED_IMAGE) {
            sprite.x += getScrollOffset();
        }

        // if we moved the _centerSprite, possibly update the scroll position
        if (sprite == _centerSprite &&
                ((sprite != _bg) || _scene.getSceneType() != Decor.FIXED_IMAGE)) {
            scrollView();
        }
    }

    /**
     * A convenience function to get our personal avatar.
     */
    public function getMyAvatar () :MemberSprite
    {
        // see subclasses
        return null;
    }

    /**
     * Get the actions currently published by our own avatar.
     */
    public function getMyActions () :Array
    {
        var avatar :MemberSprite = getMyAvatar();
        return (avatar != null) ? avatar.getAvatarActions() : [];
    }

    /**
     * Get the states currently published by our own avatar.
     */
    public function getMyStates () :Array
    {
        var avatar :MemberSprite = getMyAvatar();
        return (avatar != null) ? avatar.getAvatarStates() : [];
    }

    /**
     * Return the current location of the avatar that represents our body.
     */
    public function getMyCurrentLocation () :MsoyLocation
    {
        var avatar :MemberSprite = getMyAvatar();
        if (avatar != null) {
            return avatar.getLocation();
        } else {
            return new MsoyLocation(-1, -1, -1);
        }
    }

    public function getMemories (ident :ItemIdent) :Object
    {
        return {};
    }

    public function lookupMemory (ident :ItemIdent, key :String) :Object
    {
        return null;
    }

    public function getRoomProperties () :Object
    {
        return {};
    }

    public function getRoomProperty (key :String) :Object
    {
        return null;
    }

    /**
     * A callback from occupant sprites.
     */
    public function moveFinished (sprite :OccupantSprite) :void
    {
        if (null != _pendingRemovals.remove(sprite.getOid())) {
            // trigger a portal traversal
            portalTraversed(sprite.getLocation(), false);
            // and remove the sprite
            removeSprite(sprite);
        } else {
            dispatchEntityMoved(sprite.getItemIdent());
        }
    }

    /**
     * Scroll the view by the specified number of pixels.
     *
     * @return true if the view is scrollable.
     */
    public function scrollViewBy (xpixels :int) :Boolean
    {
        var rect :Rectangle = scrollRect;
        if (rect == null) {
            return false;
        }

        var bounds :Rectangle = getScrollBounds();
        rect.x = Math.min(_scene.getWidth() - bounds.width, Math.max(0, rect.x + xpixels));
        scrollRect = rect;

        // remove any autoscrolling (if tick is not a registered listener this will noop)
        removeEventListener(Event.ENTER_FRAME, tick);
        _jumpScroll = false;

        return true;
    }

    /**
     * Get the current scroll value.
     */
    public function getScrollOffset () :Number
    {
        var r :Rectangle = scrollRect;
        return (r == null) ? 0 : r.x;
    }

    /**
     * Get the full boundaries of our scrolling area in scaled (decor pixel) dimensions.
     * The Rectangle returned may be destructively modified.
     */
    public function getScrollBounds () :Rectangle
    {
        var r :Rectangle = new Rectangle(0, 0, _actualWidth / scaleX, _actualHeight / scaleY);
        if (_scene != null) {
            r.width = Math.min(_scene.getWidth(), r.width);
            r.height = Math.min(_scene.getHeight(), r.height);
        }
        return r;
    }

    /**
     * Get the full boundaries of our scrolling area in unscaled (stage pixel) dimensions.
     * The Rectangle returned may be destructively modified.
     */
    public function getScrollSize () :Rectangle
    {
        // figure the upper left in decor pixels, taking into account scroll offset
        var topLeft :Point = new Point(getScrollOffset(), 0);

        // and the lower right, possibly cut off by the width of the underlying scene
        var farX :int = getScrollOffset() + _actualWidth / scaleX;
        var farY :int = _actualHeight / scaleY;
        if (_scene != null) {
            farX = Math.min(_scene.getWidth(), farX);
            farY = Math.min(_scene.getHeight(), farY);
        }
        var bottomRight :Point = new Point(farX, farY);

        // finally convert from decor to placebox coordinates
        var placeBox :PlaceBox = _ctx.getTopPanel().getPlaceContainer();
        topLeft = placeBox.globalToLocal(localToGlobal(topLeft));
        bottomRight = placeBox.globalToLocal(localToGlobal(bottomRight));

        // a last sanity check
        if (bottomRight == null || topLeft == null) {
            return null;
        }

        // and then return the result
        return new Rectangle(
            topLeft.x, topLeft.y, bottomRight.x - topLeft.x, bottomRight.y - topLeft.y);
    }

    /**
     * Set the sprite we should be following.
     */
    public function setCenterSprite (center :MsoySprite) :void
    {
        _centerSprite = center;
        scrollView();
    }

    /**
     * Set whether we instantly jump to center, or scroll there.
     */
    public function setFastCentering (fastCentering :Boolean) :void
    {
        _jumpScroll = fastCentering;
    }

    public function dimAvatars (setDim :Boolean) :void
    {
        setActive(_occupants, !setDim);
        setActive(_pendingRemovals, !setDim);
    }

    public function dimFurni (setDim :Boolean) :void
    {
        setActive(_furni, !setDim);
    }

    /**
     * Add the specified sprite to this display and have the room track it.
     */
    public function addOtherSprite (sprite :MsoySprite) :void
    {
        _otherSprites.push(sprite);
        addSprite(sprite);
        relayoutSprite(sprite);
    }

    /**
     * Remove the specified sprite.
     */
    public function removeOtherSprite (sprite :MsoySprite) :void
    {
        ArrayUtil.removeAll(_otherSprites, sprite);
        removeSprite(sprite);
    }

    /**
     * Sets the background sprite. If the data value is null, simply removes the old one.
     */
    public function setBackground (decor :Decor) :void
    {
        if (_bg != null) {
            removeSprite(_bg);
            _bg = null;
        }
        if (decor != null) {
            _bg = _ctx.getMediaDirector().getDecor(decor);
            addSprite(_bg);
            _bg.setEditing(_editing);
        }
    }

    /**
     * Retrieves the background sprite, if any.
     */
    public function getBackground () :DecorSprite
    {
        return _bg;
    }

    // documentation inherited from interface PlaceView
    public function willEnterPlace (plobj :PlaceObject) :void
    {
        // TODO
    }

    // documentation inherited from interface PlaceView
    public function didLeavePlace (plobj :PlaceObject) :void
    {
        // TODO?
        removeAll(_furni);
        setBackground(null);
        _scene = null;
    }

    /**
     * Set the scene to be displayed.
     */
    public function setScene (scene :MsoyScene) :void
    {
        _scene = scene;
        updateLayout(scene.getDecor());
        _backdrop.update(scene.getDecor());
        relayout();
    }

    /**
     * Updates the layout object, creating a new one if necessary.
     */
    protected function updateLayout (decor :Decor) :void
    {
        if (! (RoomLayoutFactory.isDecorSupported(_layout, decor))) {
            _layout = RoomLayoutFactory.createLayout(decor, this);
        }
        
        _layout.update(decor);
    }
    
    /**
     * Updates the background sprite, in case background data had changed.
     */
    public function updateBackground () :void
    {
        var decor :Decor = _scene.getDecor();
        if (_bg != null && decor != null) {
            dispatchEntityLeft(_bg.getItemIdent());

            spriteWillUpdate(_bg);
            _bg.updateFromDecor(decor);
            spriteDidUpdate(_bg);

            dispatchEntityEntered(_bg.getItemIdent());
        }
    }

    /**
     * Updates background and furniture sprites from their data objects.
     */
    public function updateAllFurni () :void
    {
        if (shouldLoadAll()) {
            for each (var furni :FurniData in _scene.getFurni()) {
                if (!furni.media.isAudio()) {
                    updateFurni(furni);
                }
            }
        }
    }
    
    override public function set scrollRect (r :Rectangle) :void
    {
        super.scrollRect = r;

        if (_bg != null && _scene.getSceneType() == Decor.FIXED_IMAGE) {
            locationUpdated(_bg);
        }
    }

    public function getEntity (ident :ItemIdent) :MsoySprite
    {
        return _entities.get(ident) as MsoySprite;
    }

    public function getItemIdents () :Array
    {
        return _entities.keys();
    }

    /*public function getItemIdents (type :String) :Array
    {
        var keys :Array = _entities.keys();

        if (type != null) {
            var valid :Array = ENTITY_TYPES[type];
            keys = keys.filter(
        }

        return keys;
    }*/

    /**
     * Called when control of an entity is assigned to us.
     */
    public function dispatchEntityGotControl (ident :ItemIdent) :void
    {
        var sprite :MsoySprite = (_entities.get(ident) as MsoySprite);
        if (sprite != null) {
            sprite.gotControl();
        } else {
            log.info("Received got control for unknown sprite [item=" + ident + "].");
        }
    }

    /**
     * Called when a sprite message arrives on the room object.
     */
    public function dispatchSpriteMessage (
        item :ItemIdent, name :String, arg :ByteArray, isAction :Boolean) :void
    {
        var sprite :MsoySprite = (_entities.get(item) as MsoySprite);
        if (sprite != null) {
            sprite.messageReceived(name, ObjectMarshaller.decode(arg), isAction);
        } else {
            log.info("Received sprite message for unknown sprite [item=" + item +
                     ", name=" + name + "].");
        }
    }

    /**
     * Called when a sprite signal arrives on the room object; iterates over the
     * entities present and notify them.
     */
    public function dispatchSpriteSignal (name :String, data :ByteArray) :void
    {
        // TODO: We are decoding the data for each sprite, because we can't trust
        // the usercode to not destructively modify the value in its event handler.
        // In the future, it might be good to rework this so that the value is not decoded
        // until the event handler actually requests the value using a getter(), but I
        // suspect that will require an increment to the version number in the function we
        // call... I don't want to do that just now.
        _entities.forEach(function (key :Object, sprite :Object) :void {
            if (sprite is MsoySprite) {
                MsoySprite(sprite).signalReceived(name, ObjectMarshaller.decode(data));
            } else {
                log.warning("Erk, non-sprite entity [key=" + key + ", entity=" + sprite + "]");
            }
        });
    }

    /**
     * Called when a memory entry is added or updated in the room object.
     */
    public function dispatchMemoryChanged (ident :ItemIdent, key :String, data :ByteArray) :void
    {
        var sprite :MsoySprite = (_entities.get(ident) as MsoySprite);
        if (sprite != null) {
            sprite.memoryChanged(key, ObjectMarshaller.decode(data));
        } else {
            log.info("Received memory update for unknown sprite [item=" + ident +
                ", key=" + key + "].");
        }
    }

    /**
     * Called when a memory entry is added or updated in the room object.
     */
    public function dispatchRoomPropertyChanged (key :String, data :ByteArray) :void
    {
        // TODO: We are decoding the data for each sprite, because we can't trust
        // the usercode to not destructively modify the value in its event handler.
        // In the future, it might be good to rework this so that the value is not decoded
        // until the event handler actually requests the value using a getter(), but I
        // suspect that will require an increment to the version number in the function we
        // call... I don't want to do that just now.
        _entities.forEach(function (mapKey :Object, sprite :Object) :void {
            if (sprite is MsoySprite) {
                MsoySprite(sprite).roomPropertyChanged(key, ObjectMarshaller.decode(data));
            } else {
                log.warning("Erk, non-sprite entity [key=" + mapKey + ", entity=" + sprite + "]");
            }
        });
    }

    public function dispatchEntityEntered (item :ItemIdent) :void
    {
        var entityId :String = item.toString();

        _entities.forEach(function (mapKey :Object, sprite :Object) :void {
            if (sprite is MsoySprite) {
                MsoySprite(sprite).entityEntered(entityId);
            } else {
                log.warning("Erk, non-sprite entity [key=" + mapKey + ", entity=" + sprite + "]");
            }
        });
    }

    public function dispatchEntityLeft (item :ItemIdent) :void
    {
        var entityId :String = item.toString();

        _entities.forEach(function (mapKey :Object, sprite :Object) :void {
            if (sprite is MsoySprite) {
                MsoySprite(sprite).entityLeft(entityId);
            } else {
                log.warning("Erk, non-sprite entity [key=" + mapKey + ", entity=" + sprite + "]");
            }
        });
    }

    public function dispatchEntityMoved (item :ItemIdent) :void
    {
        var entityId :String = item.toString();

        _entities.forEach(function (mapKey :Object, sprite :Object) :void {
            if (sprite is MsoySprite) {
                MsoySprite(sprite).entityMoved(entityId);
            } else {
                log.warning("Erk, non-sprite entity [key=" + mapKey + ", entity=" + sprite + "]");
            }
        });
    }
    
    /**
     * Populate the context menu for a sprite.
     */
    protected function populateSpriteContextMenu (sprite :MsoySprite, menuItems :Array) :void
    {
        if (sprite.getItemIdent() != null && (sprite is FurniSprite) && _ctrl.canManageRoom() &&
                (null != (sprite as FurniSprite).getCustomConfigPanel())) {
            var kind :String = Msgs.GENERAL.get(sprite.getDesc());
            menuItems.push(MenuUtil.createControllerMenuItem(
                Msgs.GENERAL.get("b.config_item", kind), _ctrl.showFurniConfigPopup, sprite));
        }
    }

    /**
     * Once the background image is finished, we want to load all the rest of the sprites.
     */
    protected function backgroundFinishedLoading () :void
    {
        _loadAllMedia = true;
        updateAllFurni();
        addAllOccupants();
    }

    protected function handlePrefsUpdated (event :ConfigValueSetEvent) :void
    {
        switch (event.name) {
        case Prefs.ZOOM:
            relayout();
            break;
        }
    }

    /**
     * Layout everything.
     */
    protected function relayout () :void
    {
        var scale :Number = computeScale();
        scaleY = scale;
        scaleX = scale;
        // TODO: What is this line of code actually doing? Can I omit in the
        // avatar viewer mode?
        if (!UberClient.isFeaturedPlaceView()) {
            y = (_actualHeight - _layout.metrics.sceneHeight * scale) / 2;
        }

        configureScrollRect();

        relayoutSprites(_furni.values());
        relayoutSprites(_otherSprites);
        relayoutSprites(_occupants.values());
        relayoutSprites(_pendingRemovals.values());
    }

    /**
     * Called from relayout(), relayout the specified sprites.
     */
    protected function relayoutSprites (sprites :Array) :void
    {
        for each (var sprite :MsoySprite in sprites) {
            relayoutSprite(sprite);
        }
    }

    /**
     * Do anything necessary to (re)layout a sprite.
     */
    protected function relayoutSprite (sprite :MsoySprite) :void
    {
        locationUpdated(sprite);
        sprite.roomScaleUpdated();
    }

    /**
     * Returns the scale at which to render our room view.
     */
    protected function computeScale () :Number
    {
        const maxScale :Number = _actualHeight / _layout.metrics.sceneHeight;
        if (isNaN(_fullSizeActualWidth) || !_ctx.getTopPanel().isMinimized()) {
            _fullSizeActualWidth = _actualWidth;
        }
        const minScale :Number = _fullSizeActualWidth / _layout.metrics.sceneWidth;
        if (!UberClient.isFeaturedPlaceView()) {
            const canScale :Boolean = maxScale > minScale;
            _ctx.getTopPanel().getControlBar().enableZoomControl(canScale);
            if (canScale) {
                return minScale + (maxScale - minScale) * getZoom();
            }
        }
        return maxScale;
    }

    /**
     * Get the zoom level of the room.
     */
    protected function getZoom () :Number
    {
        return Prefs.getZoom();
    }

    protected function scrollView () :void
    {
        if (_centerSprite == null) {
            return;
        }
        var rect :Rectangle = scrollRect;
        if (rect == null) { 
            return; // return if there's nothing to scroll
        }

        var centerX :int = _centerSprite.x + _centerSprite.getLayoutHotSpot().x;
        var newX :Number = centerX - (_actualWidth / scaleX)/2;
        newX = Math.min(_scene.getWidth() - rect.width, Math.max(0, newX));

        if (_jumpScroll) {
            rect.x = newX;

        } else if (Math.abs(rect.x - newX) > MAX_AUTO_SCROLL) {
            if (newX > rect.x) {
                rect.x += MAX_AUTO_SCROLL;
            } else {
                rect.x -= MAX_AUTO_SCROLL;
            }
            addEventListener(Event.ENTER_FRAME, tick);

        } else {
            rect.x = newX;
            removeEventListener(Event.ENTER_FRAME, tick);
            _jumpScroll = true;
        }

        // assign the new scrolling rectangle
        scrollRect = rect;
        _suppressAutoScroll = true;
    }

    protected function tick (event :Event) :void
    {
        if (!_suppressAutoScroll) {
            if (_centerSprite != null) {
                scrollView();

            } else {
                // stop scrolling
                removeEventListener(Event.ENTER_FRAME, tick);
            }
        }

        // and finally, we want ensure it can happen on the next frame if
        // our avatar doesn't move
        _suppressAutoScroll = false;
    }

    /**
     * Called when we detect a body being added or removed.
     */
    protected function portalTraversed (loc :Location, entering :Boolean) :void
    {
        var itr :Iterator = _scene.getPortals();
        while (itr.hasNext()) {
            var portal :Portal = (itr.next() as Portal);
            if (loc.equals(portal.loc)) {
                var sprite :FurniSprite = (_furni.get(portal.portalId) as FurniSprite);
                if (sprite != null) {
                    sprite.wasTraversed(entering);
                }
                return;
            }
        }
    }

    /**
     * Should we load everything that we know how to?  This is used by a subclass to restrict
     * loading to certain things when the room is first entered.
     */
    protected function shouldLoadAll () :Boolean
    {
        return _loadAllMedia;
    }

    /**
     * Configure the rectangle used to select a portion of the view that's showing.
     */
    protected function configureScrollRect () :void
    {
        if (_scene != null && _actualWidth >= _scene.getWidth()) {
            scrollRect = null;
        } else {
            scrollRect = getScrollBounds();
        }
    }

    /**
     * Sets all sprites in the supplied map to active or non-active.
     */
    protected function setActive (map :HashMap, active :Boolean) :void
    {
        for each (var sprite :MsoySprite in map.values()) {
            if (sprite != _bg) {
                sprite.setActive(active);
            }
        }
    }

    /**
     * Shutdown all the sprites in the specified map.
     */
    protected function removeAll (map :HashMap) :void
    {
        for each (var sprite :MsoySprite in map.values()) {
            removeSprite(sprite);
        }
        map.clear();
    }

    protected function addFurni (furni :FurniData) :FurniSprite
    {
        var sprite :FurniSprite = _ctx.getMediaDirector().getFurni(furni);
        addSprite(sprite);
        sprite.setLocation(furni.loc);
        sprite.roomScaleUpdated();
        sprite.setEditing(_editing);
        _furni.put(furni.id, sprite);
        return sprite;
    }

    protected function updateFurni (furni :FurniData) :void
    {
        var sprite :FurniSprite = (_furni.get(furni.id) as FurniSprite);
        if (sprite != null) {
            dispatchEntityLeft(sprite.getItemIdent());

            spriteWillUpdate(sprite);
            sprite.update(furni);
            spriteDidUpdate(sprite);

            dispatchEntityEntered(sprite.getItemIdent());
        } else {
            addFurni(furni);
        }
    }

    protected function removeFurni (furni :FurniData) :void
    {
        var sprite :FurniSprite = (_furni.remove(furni.id) as FurniSprite);
        if (sprite != null) {
            removeSprite(sprite);
        }
    }

    protected function addAllOccupants () :void
    {
        // see subclasses
    }

    protected function removeAllOccupants () :void
    {
        removeAll(_occupants);
        removeAll(_pendingRemovals);
    }

    /**
     * Add the specified sprite to the view.
     */
    protected function addSprite (sprite :MsoySprite) :void
    {
        var index :int = (sprite is DecorSprite) ? 0 : 1;
        addChildAt(sprite, index);
        addToEntityMap(sprite);
    }

    /**
     * Remove the specified sprite from the view.
     */
    protected function removeSprite (sprite :MsoySprite) :void
    {
        removeFromEntityMap(sprite);
        removeChild(sprite);
        _ctx.getMediaDirector().returnSprite(sprite);

        // clear any popup associated with it
        _ctrl.clearEntityPopup(sprite);

        if (sprite == _centerSprite) {
            _centerSprite = null;
        }
    }

    /**
     * Should be called prior to a sprite updating.
     */
    protected function spriteWillUpdate (sprite :MsoySprite) :void
    {
        removeFromEntityMap(sprite);
    }

    /**
     * Should be called after updating a sprite.
     */
    protected function spriteDidUpdate (sprite :MsoySprite) :void
    {
        addToEntityMap(sprite);
    }

    /**
     * Add the specified sprite to our entity map, if applicable.
     */
    protected function addToEntityMap (sprite :MsoySprite) :void
    {
        var ident :ItemIdent = sprite.getItemIdent();
        if (ident != null) {
            _entities.put(ident, sprite);
        }
    }

    /**
     * Remove the specified sprite to our entity map, if applicable.
     */
    protected function removeFromEntityMap (sprite :MsoySprite) :void
    {
        _entities.remove(sprite.getItemIdent()); // could be a no-op
    }

    /** Our controller. */
    protected var _ctrl :RoomController;

    /** When we first enter the room, we only load the background (if any). */
    protected var _loadAllMedia :Boolean = false;

    /** The spinner to show when we're loading room data. */
    protected var _loadingSpinner :DisplayObject;

    /** A map of bodyOid -> OccupantSprite. */
    protected var _occupants :HashMap = new HashMap();

    /** Maps ItemIdent -> MsoySprite for entities (furni, avatars, pets). */
    protected var _entities :HashMap = new HashMap();

    /** The sprite we should center on. */
    protected var _centerSprite :MsoySprite;

    /** A map of bodyOid -> OccupantSprite for those that we'll remove when they stop moving. */
    protected var _pendingRemovals :HashMap = new HashMap();

    /** If true, the scrolling should simply jump to the right position. */
    protected var _jumpScroll :Boolean = true;

    /** True if autoscroll should be supressed for the current frame. */
    protected var _suppressAutoScroll :Boolean = false;

    /** The msoy context. */
    protected var _ctx :WorldContext;

    /** Are we actually showing? */
    protected var _showing :Boolean = true;

    /** The model of the current scene. */
    protected var _scene :MsoyScene;

    /** The actual screen width of this component. */
    protected var _actualWidth :Number;

    /** The actual width we had last time we weren't minimized */
    protected var _fullSizeActualWidth :Number;

    /** The actual screen height of this component. */
    protected var _actualHeight :Number;

    /** Object responsible for our spatial layout. */
    protected var _layout :RoomLayout;

    /** Helper object that draws a room backdrop with four walls. */
    protected var _backdrop :RoomBackdrop = new RoomBackdrop();

    /** Our background sprite, if any. */
    protected var _bg :DecorSprite;

    /** A map of id -> Furni. */
    protected var _furni :HashMap = new HashMap();

    /** A list of other sprites (used during editing). */
    protected var _otherSprites :Array = new Array();

    /** Are we editing the scene? */
    protected var _editing :Boolean = false;

    /** The maximum number of pixels to autoscroll per frame. */
    protected static const MAX_AUTO_SCROLL :int = 15;
}
}
