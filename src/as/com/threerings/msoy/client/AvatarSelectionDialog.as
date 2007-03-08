//
// $Id$

package com.threerings.msoy.client {

import flash.display.DisplayObject;

import flash.events.Event;
import flash.events.MouseEvent;

import mx.containers.Grid;
import mx.containers.GridItem;
import mx.containers.GridRow;
import mx.containers.BoxDirection;
import mx.controls.Label;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.item.client.ItemRenderer;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.data.MemberObject;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.msoy.item.web.Avatar;
import com.threerings.msoy.ui.FloatingPanel;


/** Avatar picker; replicates some of the functionality of an inventory picker,
 *  except in a larger, grid-based format (which makes it less suitable for embedding
 *  in other screen elements). */
public class AvatarSelectionDialog extends FloatingPanel
    implements AttributeChangeListener
{
    /** How many avatars per grid row? */
    public static const ITEMS_PER_ROW :Number = 5;

    /** How many rows should we have on screen at a time? */
    public static const MAX_ROWS :Number = 3; 
    
    public function AvatarSelectionDialog (ctx :WorldContext)
    {
        super(ctx, Msgs.GENERAL.get("t.avatar_select"));

        addEventListener(Event.ADDED_TO_STAGE, handleAddRemove);
        addEventListener(Event.REMOVED_FROM_STAGE, handleAddRemove);
        open(true);
    }

    // from AttributeChangeListener
    public function attributeChanged (evt :AttributeChangedEvent) :void
    {
        if (evt.getName() == MemberObject.LOADED_INVENTORY &&
            _memberObj.isInventoryLoaded(Item.AVATAR))
        {
            fillWithAvatars();
            unwatchPlayer();
        }
    }

    protected function handleAddRemove (event :Event) :void
    {
        if (event.type == Event.ADDED_TO_STAGE) {
            // We just got created - check if we need to start loading the inventory.
            _memberObj = _ctx.getMemberObject();
            if (inventoryReady()) {
                // Inventory was loaded even before we got here - if controls
                // have already been set up, just fill them with data.
                if (_avatars != null) {   
                    fillWithAvatars();
                }
            } else {
                // Wait for inventory to arrive.
                _memberObj.addListener(this);
                _ctx.getItemDirector().loadInventory(Item.AVATAR);
            }

        } else {
            // We're getting destroyed - clean up.
            unwatchPlayer();
        }
    }

    protected function unwatchPlayer () :void
    {
        if (_memberObj != null) {
            _memberObj.removeListener(this);
            _memberObj = null;
        }
    }

    protected function inventoryReady () :Boolean
    {
        return (_memberObj != null && _memberObj.isInventoryLoaded(Item.AVATAR));
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        // Simple label
        var label :Label = new Label();
        label.text = Msgs.GENERAL.get("m.avatars_available");
        addChild(label);
        
        // Initializes a grid 
        _avatars = new Grid();
        _avatars.percentWidth = 100;
        _avatars.maxHeight = ItemRenderer.ITEM_SIZE * MAX_ROWS;
        addChild(_avatars);

        if (inventoryReady()) {
            fillWithAvatars (); 
        } else {
            // Add a single "loading..." label
            var row :GridRow = new GridRow();
            _avatars.addChild(row);
            var item :GridItem = new GridItem();
            row.addChild(item);
            var b :Label = new Label();
            b.text = Msgs.GENERAL.get("m.avatar_pending");
            item.addChild(b);
        }

        addButtons(CANCEL_BUTTON);
    }
    
    /** As the name says, fills the grid. */
    protected function fillWithAvatars () :void
    {
        if (inventoryReady()) {
            _avatars.removeAllChildren();
            
            // Get avatars from player's inventory
            var items :Array = _memberObj.getItems(Item.AVATAR);

            // hide any 'used' avatars for now
            for (var ii :int = items.length - 1; ii >= 0; ii--) {
                if ((items[ii] as Item).isUsed()) {
                    items.splice(ii, 1);
                }
            }

            // Add a default avatar
            var defaultAvatar :Avatar = new Avatar();
            defaultAvatar.name = Msgs.GENERAL.get("m.default_avatar");
            defaultAvatar.avatarMedia = defaultAvatar.thumbMedia =
                Avatar.getDefaultMemberAvatarMedia();
            items.push(defaultAvatar); 

            // Fill the grid
            var row :GridRow = null;
            for (var i :int = 0; i < items.length; i++) {
                // Add a new row if necessary
                if (i % ITEMS_PER_ROW == 0) {
                    row = new GridRow();
                    _avatars.addChild(row);
                }

                // Add item
                var cell :GridItem = new GridItem();
                var render :ItemRenderer = new ItemRenderer(BoxDirection.VERTICAL);
                render.data = items[i];
                cell.addChild(render);
                cell.addEventListener (MouseEvent.CLICK, clickHandler, false, 0, true);
                row.addChild(cell);

                // Should this item be marked as selected?
                if (_memberObj.avatar == null) {
                    if (items[i] == defaultAvatar) {
                        cell.styleName = "avatarCellSelected"; 
                    }
                } else if (_memberObj.avatar.itemId == items[i].itemId) {
                    cell.styleName = "avatarCellSelected";     
                }
            }
        }
    }
    
    /** Handle user's avatar selection. */
    protected function clickHandler (event :MouseEvent) :void
    {
        var t :GridItem = event.currentTarget as GridItem;

        // Get the first child of this grid item - it should be an ItemRenderer
        if (t != null && t.rawChildren.numChildren > 0) {
            var render :ItemRenderer = t.getChildAt(0) as ItemRenderer;
            if (render != null) {
                // Pull out the avatar, and set it!
                var item :Item = render.data as Item;
                if (item != null) {
                    _ctx.getWorldDirector().setAvatar(item.itemId);
                    close();
                }
            }
        }
            
    }

    /** Client's object. */
    protected var _memberObj :MemberObject;

    /** Grid that will be filled with avatars. */
    protected var _avatars :Grid;
}
}
