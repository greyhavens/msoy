package com.threerings.msoy.item.client {

import flash.events.Event;
import flash.events.EventDispatcher;
    
import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.item.web.Item;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.AttributeChangeListener;


/**
 * Class that asynchronously starts loading inventory of a specific type.
 *
 * The loader is started manually, by calling the start() function. Once loading finishes
 * it dispatches the InventoryLoaded.SUCCESS event. The loader will only dispatch
 * the event after the first successful load; future re-loads, if they happen, will be ignored.
 *
 * This object can be used by classes that want to access player inventory without having to
 * listen to attribute changed events.
 */
public class InventoryLoader extends EventDispatcher
    implements AttributeChangeListener
{
    /** Name of the event that will be fired once the inventory was successfully loaded. */
    public static const SUCCESS :String = "InventoryLoader.SUCCESS";

    public function InventoryLoader (ctx :WorldContext, itemType :int = Item.NOT_A_TYPE)
    {
        _ctx = ctx;
        _type = itemType;
    }

    /** This function initiates inventory loading. */
    public function start () :void
    {
        startLoading();
        
        if (_ctx.getMemberObject().isInventoryLoaded(_type)) {
            reportSuccess(); // already loaded, clean up
        } 
    }
        
    // from AttributeChangeListener
    public function attributeChanged (evt :AttributeChangedEvent) :void
    {
        if (evt.getName() == MemberObject.LOADED_INVENTORY &&
            _ctx.getMemberObject().isInventoryLoaded(_type))
        {
            reportSuccess();
        }
    }

    /** Initiates inventory loading. */
    protected function startLoading () :void
    {
        _ctx.getMemberObject().addListener(this);
        _ctx.getItemDirector().loadInventory(_type);
    }

    /** Sends out a success message, and stops listening for inventory changes. */
    protected function reportSuccess () :void
    {
        var event :Event = new Event(SUCCESS);
        dispatchEvent(event);

        _ctx.getMemberObject().removeListener(this);
        _ctx = null;
    }

    /** World info. */
    protected var _ctx :WorldContext;

    /** Item type that we're trying to load. */
    protected var _type :int;
  
}
}
