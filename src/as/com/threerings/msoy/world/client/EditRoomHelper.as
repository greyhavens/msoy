package com.threerings.msoy.world.client {

import flash.events.MouseEvent;
import flash.events.KeyboardEvent;

import flash.display.Graphics;
import flash.display.LineScaleMode;

import flash.geom.Point;

import flash.ui.Keyboard;

import com.threerings.util.ArrayUtil;
import com.threerings.util.Controller;
import com.threerings.util.Iterator;

import com.threerings.io.TypedArray;

import com.threerings.whirled.data.SceneUpdate;
import com.threerings.whirled.spot.data.ModifyPortalsUpdate;
import com.threerings.whirled.spot.data.Portal;

import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.data.MediaData;

import com.threerings.msoy.item.client.InventoryPanel;

import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.MsoyPortal;
import com.threerings.msoy.world.data.ModifyFurniUpdate;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyScene;

public class EditRoomHelper extends Controller
{
    public static const INSERT_PORTAL :String = "InsertPortal";
    public static const INSERT_FURNI :String = "InsertFurni";
    public static const DISCARD_EDITS :String = "DiscardEdits";
    public static const SAVE_EDITS :String = "SaveEdits";

    public function EditRoomHelper (
        ctx :MsoyContext, roomCtrl :RoomController, roomView :RoomView,
        scene :MsoyScene)
    {
        _ctx = ctx;
        _roomCtrl = roomCtrl;
        _roomView = roomView;
        _scene = scene;

        _nextPortalId = _scene.getNextPortalId();
        _roomView.setEditing(true, enableEditingVisitor);

        _panel = new EditRoomPanel(ctx, this, roomView);
        setControlledPanel(_panel);
    }

    /**
     * Called by the controller to end editing.
     */
    public function endEditing (saveEdits :Boolean) :void
    {
        _panel.close();
        if (_invPanel != null) {
            _invPanel.close();
            _invPanel = null;
        }

        // remove all sprites we've added
        while (_addedSprites.length > 0) {
            _roomView.removeChild(_addedSprites.pop() as MsoySprite);
        }

        var edits :TypedArray = null;
        if (saveEdits) {
            var sceneId :int = _scene.getId();
            var version :int = _scene.getVersion();

            edits = TypedArray.create(SceneUpdate);

            // configure any furniture updates
            if (_addedFurni.length > 0 || _removedFurni.length > 0) {
                var furniUpdate :ModifyFurniUpdate = new ModifyFurniUpdate();
                furniUpdate.initialize(sceneId, version++,
                    (_removedFurni.length > 0) ? _removedFurni : null,
                    (_addedFurni.length > 0) ? _addedFurni : null);
                edits.push(furniUpdate);
            }

            // configure any portal updates
            if (_addedPortals.length > 0 || _removedPortals.length > 0) {
                var portalUpdate :ModifyPortalsUpdate =
                    new ModifyPortalsUpdate();
                portalUpdate.initialize(sceneId, version++,
                    (_removedPortals.length > 0) ? _removedPortals : null,
                    (_addedPortals.length > 0) ? _addedPortals : null);
                edits.push(portalUpdate);
            }

            // return something, or null
            if (edits.length == 0) {
                edits = null;
            }
        }

        // turn off editing in the room, which restores original positions
        _roomView.setEditing(false, disableEditingVisitor);

        _roomCtrl.endEditing(edits);
    }

    /**
     * Handles INSERT_PORTAL.
     */
    public function handleInsertPortal () :void
    {
        // create a generic portal descriptor
        var portal :MsoyPortal = new MsoyPortal();
        portal.portalId = getNextPortalId();
        portal.loc = new MsoyLocation(.5, 0, .5);
        portal.media = new MediaData(1);
        portal.targetSceneId = 1;
        portal.targetPortalId = -1;

        // create a loose sprite to represent it, add it to the panel
        var sprite :PortalSprite = _ctx.getMediaDirector().getPortal(portal);
        _roomView.addChild(sprite);
        sprite.setLocation(portal.loc);

        _addedSprites.push(sprite);

        // set it up for editing
        sprite.setEditing(true);
        addEditingListeners(sprite);
    }

    /**
     * Handles INSERT_FURNI.
     */
    public function handleInsertFurni () :void
    {
        if (_invPanel == null) {
            _invPanel = new InventoryPanel(_ctx);
        }
    }

    /**
     * Handles SAVE_EDITS.
     */
    public function handleSaveEdits () :void
    {
        endEditing(true);
    }

    public function handleDiscardEdits () :void
    {
        endEditing(false);
    }

    protected function enableEditingVisitor (key :Object, value :Object) :void
    {
        var sprite :MsoySprite = (value as MsoySprite);
        sprite.setEditing(true);

        addEditingListeners(sprite);
    }

    protected function disableEditingVisitor (key :Object, value :Object) :void
    {
        var sprite :MsoySprite = (value as MsoySprite);

        sprite.removeEventListener(MouseEvent.MOUSE_DOWN, spritePressed);
        sprite.removeEventListener(MouseEvent.MOUSE_OVER, spriteRollOver);
        sprite.removeEventListener(MouseEvent.MOUSE_OUT, spriteRollOut);

        sprite.setEditing(false);
    }

    protected function recordMouseAnchor () :void
    {
        _anchor = new Point(_roomView.stage.mouseX, _roomView.stage.mouseY);
        _anchorY = _editSprite.loc.y;
    }

    protected function addEditingListeners (sprite :MsoySprite) :void
    {
        sprite.addEventListener(MouseEvent.MOUSE_DOWN, spritePressed);
        sprite.addEventListener(MouseEvent.MOUSE_OVER, spriteRollOver);
        sprite.addEventListener(MouseEvent.MOUSE_OUT, spriteRollOut);
    }

    protected function spritePressed (event :MouseEvent) :void
    {
        if (_editSprite != null) {
            return;
        }
        // Stop event prop, otherwise the roomView registers the click too
        // causing spritePositioned() to be called.
        event.stopPropagation();

        // determine the edit sprite
        _editSprite = (event.currentTarget as MsoySprite);

        // stop listening for these bits
        _editSprite.removeEventListener(MouseEvent.MOUSE_DOWN, spritePressed);
        _editSprite.removeEventListener(MouseEvent.MOUSE_OVER, spriteRollOver);
        _editSprite.removeEventListener(MouseEvent.MOUSE_OUT, spriteRollOut);

        // determine whether we're going to adjust scaling or position
        var w :Number = _editSprite.contentWidth;
        var h :Number = _editSprite.contentHeight;
        var xScaleTrigger :Number = (w > SCALING_TRIGGER_AREA * 2)
            ? SCALING_TRIGGER_AREA : 1;
        var yScaleTrigger :Number = (h > SCALING_TRIGGER_AREA * 2)
            ? SCALING_TRIGGER_AREA : 1;
        _scalingX = (event.localX < xScaleTrigger) ||
            (event.localX > (w - xScaleTrigger));
        _scalingY = (event.localY < yScaleTrigger) ||
            (event.localY > (h - yScaleTrigger));

        var hs :Point = _editSprite.localToGlobal(_editSprite.hotSpot);

        if (_scalingX || _scalingY) {
            // we are editing scale

            // TODO: I think this fucks up when either scale is negative
            _xoffset = (event.stageX < hs.x) ? 0 : w;
            _yoffset = (event.stageY < hs.y) ? 0 : h;

            _roomView.addEventListener(MouseEvent.MOUSE_MOVE, spriteScaling);
            _roomView.addEventListener(MouseEvent.MOUSE_UP, spriteScaled);

        } else {
            // we are editing position

            // figure out the offset to the hotspot
            _xoffset = hs.x - event.stageX;
            _yoffset = hs.y - event.stageY;

            _roomView.addEventListener(MouseEvent.MOUSE_MOVE, spritePositioning);
            _roomView.addEventListener(MouseEvent.MOUSE_UP, spritePositioned);
            _roomView.stage.addEventListener(KeyboardEvent.KEY_UP,
                spritePositioningKey);
            _roomView.stage.addEventListener(KeyboardEvent.KEY_DOWN,
                spritePositioningKey);

            if (event.shiftKey) {
                recordMouseAnchor();
                drawHeightPositioning(_editSprite);

            } else {
                drawPositioning(_editSprite);
            }
        }
    }

    protected function spriteRollOver (event :MouseEvent) :void
    {
        if (_editSprite != null) {
            return;
        }
        var sprite :MsoySprite = (event.currentTarget as MsoySprite);
        drawScaling(sprite);
    }

    protected function spriteRollOut (event :MouseEvent) :void
    {
        if (_editSprite != null) {
            return;
        }
        var sprite :MsoySprite = (event.currentTarget as MsoySprite);
        sprite.graphics.clear();
    }

    /**
     * Listens for MOUSE_MOVE when positioning a sprite.
     */
    protected function spritePositioning (event :MouseEvent) :void
    {
        // _xoffset and _yoffset are the stage offsets from the sprite's
        // hotspot to the initial click position, and we continue
        // using those to adjust the mouse position. It's good enough.
        // Ideally, we would solve for position and scale such that the
        // initally clicked spot is always under the mouse pointer, but
        // that's much harder to do.

        if (event.shiftKey) {
            // figure the distance from the anchor
            var ypixels :Number = _anchor.y - event.stageY;
            var loc :MsoyLocation = _editSprite.loc;
            loc.y = _anchorY + _roomView.getYDistance(loc.z, ypixels);
            _editSprite.setLocation(loc);

        } else {
            var newLoc :MsoyLocation = _roomView.pointToLocation(
                event.stageX, event.stageY);
            if (newLoc != null) {
                newLoc.y = _editSprite.loc.y;
                _editSprite.setLocation(newLoc);
            }
        }
    }

    protected function spritePositioningKey (event :KeyboardEvent) :void
    {
        if (event.keyCode != Keyboard.SHIFT) {
            return;
        }

        if (event.type == KeyboardEvent.KEY_DOWN) {
            recordMouseAnchor();
            drawHeightPositioning(_editSprite);

        } else {
            drawPositioning(_editSprite);
        }
    }

    /**
     * Listens for MOUSE_UP when positioning a sprite.
     */
    protected function spritePositioned (event :MouseEvent) :void
    {
        _roomView.removeEventListener(MouseEvent.MOUSE_MOVE, spritePositioning);
        _roomView.removeEventListener(MouseEvent.MOUSE_UP, spritePositioned);
        _roomView.stage.removeEventListener(KeyboardEvent.KEY_UP,
            spritePositioningKey);
        _roomView.stage.removeEventListener(KeyboardEvent.KEY_DOWN,
            spritePositioningKey);

        spriteUpdated(_editSprite);

        _editSprite.graphics.clear();
        addEditingListeners(_editSprite);
        _editSprite = null;
    }

    /**
     * Listens for MOUSE_MOVE when scaling a sprite.
     */
    protected function spriteScaling (event :MouseEvent) :void
    {
        // TODO: this will clean up more when I revamp the perspective
        // math in the RoomView.
        // Until then, there's no point in trying to make it less annoying.

        var p :Point = _editSprite.globalToLocal(
            new Point(event.stageX, event.stageY));
        var hs :Point = _editSprite.hotSpot;

        if (_scalingX) {
            var ox :Number = _editSprite.getMediaScaleX();
            var sx :Number = (ox == 0) ? 1 : ox;
            sx *= (p.x - hs.x) / (_xoffset - hs.x);
            if (sx != 0 && !isNaN(sx) && isFinite(sx)) {
                _editSprite.setMediaScaleX(sx);
                if (_editSprite.contentWidth < 2) {
                    // re-set the old scale
                    _editSprite.setMediaScaleX(ox);
                }
            }
        }
        if (_scalingY) {
            var oy :Number = _editSprite.getMediaScaleY();
            var sy :Number = (oy == 0) ? 1 : oy;
            sy *= (p.y - hs.y) / (_yoffset - hs.y);
            if (sy != 0 && !isNaN(sy) && isFinite(sy)) {
                _editSprite.setMediaScaleY(sy);
                if (_editSprite.contentHeight < 2) {
                    // re-set the old scale
                    _editSprite.setMediaScaleY(oy);
                }
            }
        }

        // since the scale has changed, update the scaling hint graphics
        drawScaling(_editSprite);
    }

    /**
     * Listens for MOUSE_UP when scaling a sprite.
     */
    protected function spriteScaled (event :MouseEvent) :void
    {
        _roomView.removeEventListener(MouseEvent.MOUSE_MOVE, spriteScaling);
        _roomView.removeEventListener(MouseEvent.MOUSE_UP, spriteScaled);

        spriteUpdated(_editSprite);

        _editSprite.graphics.clear();
        addEditingListeners(_editSprite);
        _editSprite = null;
    }

    /**
     * Called on each sprite after we've manipulated in some way.
     */
    protected function spriteUpdated (sprite :MsoySprite) :void
    {
        var scene :MsoyScene = (_ctx.getSceneDirector().getScene() as MsoyScene);

        if (sprite is FurniSprite) {
            var furni :FurniData = (sprite as FurniSprite).getFurniData();
            // copy the edited location back into the descriptor
            furni.loc = (sprite.loc.clone() as MsoyLocation);

            // first remove any instances from our removed/added
            ArrayUtil.removeAll(_removedFurni, furni);
            ArrayUtil.removeAll(_addedFurni, furni);

            // now compare it to the original
            var ofurni :FurniData;
            for each (var f :FurniData in scene.getFurni()) {
                if (furni.equals(f)) {
                    ofurni = f;
                    break;
                }
            }
            if (ofurni == null) {
                _addedFurni.push(furni);

            } else if (!furni.equivalent(ofurni)) {
                _addedFurni.push(furni);
                _removedFurni.push(ofurni);
            }

        } else if (sprite is PortalSprite) {
            var portal :MsoyPortal = (sprite as PortalSprite).getPortal();
            // copy the edited location back into the descriptor
            portal.loc = (sprite.loc.clone() as MsoyLocation);

            // first remove any instances from our removed/added
            ArrayUtil.removeAll(_removedPortals, portal);
            ArrayUtil.removeAll(_addedPortals, portal);

            // now compare to the original
            var oportal :MsoyPortal;
            var itr :Iterator = scene.getPortals();
            while (itr.hasNext()) {
                var iportal :MsoyPortal = (itr.next() as MsoyPortal);
                if (portal.equals(iportal)) {
                    oportal = iportal;
                    break;
                }
            }
            if (oportal == null) {
                _addedPortals.push(portal);

            } else if (!portal.equivalent(oportal)) {
                _addedPortals.push(portal);
                _removedPortals.push(oportal);
            }

        } else {
            throw new Error("Unknown sprite type edited");
        }
    }

    protected function drawPositioning (sprite :MsoySprite) :void
    {
        var w :Number = sprite.contentWidth;
        var h :Number = sprite.contentHeight;
        var wo :Number = SCALE_TARGET_LENGTHS / sprite.scaleX;
        var ho :Number = SCALE_TARGET_LENGTHS / sprite.scaleY;
        var g :Graphics = sprite.graphics;

        g.clear();
        for (var ii :int = 0; ii < 2; ii++) {
            if (ii == 0) {
                g.lineStyle(3, 0xFFFFFF, 1, false, LineScaleMode.NONE);
            } else {
                g.lineStyle(1, 0x000000, 1, false, LineScaleMode.NONE);
            }

            g.moveTo(w/2, (h - ho) / 2);
            g.lineTo(w/2, (h + ho) / 2);

            g.moveTo((w - wo) / 2, h/2);
            g.lineTo((w + wo) / 2, h/2);
        }
    }

    protected function drawHeightPositioning (sprite :MsoySprite) :void
    {
        var wo :Number = SCALE_TARGET_LENGTHS / sprite.scaleX;
        var ho :Number = SCALE_TARGET_LENGTHS / sprite.scaleY;
        var g :Graphics = sprite.graphics;
        var hs :Point = sprite.hotSpot;

        g.clear();
        for (var ii :int = 0; ii < 2; ii++) {
            if (ii == 0) {
                g.lineStyle(3, 0xFFFFFF, 1, false, LineScaleMode.NONE);
            } else {
                g.lineStyle(1, 0x000000, 1, false, LineScaleMode.NONE);
            }

            g.moveTo(hs.x - wo/2, hs.y);
            g.lineTo(hs.x + wo/2, hs.y);

            g.moveTo(hs.x, hs.y - ho/2);
            g.lineTo(hs.x, hs.y + ho/2);
        }
    }

    protected function drawScaling (sprite :MsoySprite) :void
    {
        var w :Number = sprite.contentWidth;
        var h :Number = sprite.contentHeight;
        var wo :Number = SCALE_TARGET_LENGTHS / sprite.scaleX;
        var ho :Number = SCALE_TARGET_LENGTHS / sprite.scaleY;
        var g :Graphics = sprite.graphics;

        g.clear();
        for (var ii :int = 0; ii < 2; ii++) {
            if (ii == 0) {
                g.lineStyle(3, 0xFFFFFF, 1, false, LineScaleMode.NONE);
            } else {
                g.lineStyle(1, 0x000000, 1, false, LineScaleMode.NONE);
            }
            g.moveTo(0, ho);
            g.lineTo(0, 0);
            g.lineTo(wo, 0);

            g.moveTo(w - wo, 0);
            g.lineTo(w, 0);
            g.lineTo(w, ho);

            g.moveTo(w, h - ho);
            g.lineTo(w, h);
            g.lineTo(w - wo, h);

            g.moveTo(wo, h);
            g.lineTo(0, h);
            g.lineTo(0, h - ho);

            g.moveTo((w - wo) / 2, 0);
            g.lineTo((w + wo) / 2, 0);

            g.moveTo(0, (h - ho) / 2);
            g.lineTo(0, (h + ho) / 2);

            g.moveTo((w - wo) / 2, h);
            g.lineTo((w + wo) / 2, h);

            g.moveTo(w, (h - ho) / 2);
            g.lineTo(w, (h + ho) / 2);
        }
    }

    protected function getNextPortalId () :int
    {
        // TODO: cope with ids getting too large.
        // we don't ask the scene, because it doesn't know about our
        // editing portals yet
        return _nextPortalId++;
    }

    protected var _ctx :MsoyContext;

    protected var _scene :MsoyScene;

    protected var _panel :EditRoomPanel;

    protected var _roomCtrl :RoomController;

    /** The room view. */
    protected var _roomView :RoomView;

    protected var _nextPortalId :int;

    protected var _invPanel :InventoryPanel;

    protected var _removedFurni :TypedArray = TypedArray.create(FurniData);
    protected var _addedFurni :TypedArray = TypedArray.create(FurniData);

    protected var _removedPortals :TypedArray = TypedArray.create(Portal);
    protected var _addedPortals :TypedArray = TypedArray.create(Portal);

    protected var _addedSprites :Array = new Array();
    protected var _removedSprites :Array = new Array();

    /** The offset from the clicked point to the object's hotspot. */
    protected var _xoffset :Number;
    protected var _yoffset :Number;

    /** An anchor point used when positioning. */
    protected var _anchor :Point;
    protected var _anchorY :Number;

    /** Are we currently working on scaling the edit sprite? */
    protected var _scalingX :Boolean = false;
    protected var _scalingY :Boolean = false;

    protected var _editSprite :MsoySprite;

    protected static const SCALE_TARGET_LENGTHS :int = 15;
    protected static const SCALING_TRIGGER_AREA :int = 5;
}
}
