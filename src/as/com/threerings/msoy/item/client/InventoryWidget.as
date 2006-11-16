//
// $Id$

package com.threerings.msoy.item.client {

import mx.core.ClassFactory;

import mx.controls.Tree;

import mx.collections.ICollectionView;
import mx.collections.ListCollectionView;

import mx.core.ScrollPolicy;

import mx.events.TreeEvent;

import com.threerings.presents.client.ResultWrapper;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.item.web.Item;

public class InventoryWidget extends Tree
{
    public function InventoryWidget (
        ctx :MsoyContext, soleType :int = Item.NOT_A_TYPE)
    {
        _ctx = ctx;
        verticalScrollPolicy = ScrollPolicy.ON;
        variableRowHeight = true;

        dragEnabled = true;
        allowMultipleSelection = true;
        itemRenderer = new ClassFactory(ItemTreeRenderer);

        addEventListener(TreeEvent.ITEM_OPENING, handleItemOpening);
        configureItemCategories(soleType);
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var coll :ListCollectionView = ListCollectionView(dataProvider);
        if (coll.length == 1) {
            loadCategory(coll.getItemAt(0));
        }
    }

    /**
     * Set up the top-level nodes of the tree.
     */
    protected function configureItemCategories (soleType :int) :void
    {
        var itemTypes :Array;

        if (soleType == Item.NOT_A_TYPE) {
            itemTypes = Item.getTypes();
        } else {
            itemTypes = [ soleType ];
        }
        var nodes :Array = [ ];
        for each (var itemType :int in itemTypes) {
            var desc :Object = {
                label: Msgs.ITEM.get("t.items_" + Item.getTypeName(itemType)),
                itemType: itemType,
                children: [
                    Msgs.ITEM.get("m.retrieving")
                ]
            };
            nodes.push(desc);
        }

        dataProvider = nodes;
    }

    protected function handleItemOpening (event :TreeEvent) :void
    {
        if (event.opening) {
            loadCategory(event.item);

        } else if (dataProvider.length == 1) {
            // if there is only one type, prevent closeage
            event.preventDefault();
        }
    }

    protected function loadCategory (node :Object) :void
    {
        var type :int = int(node.itemType);
        if (null != _loadedTypes[type]) {
            return;
        }

        _loadedTypes[type] = true;

        var svc :ItemService =
            (_ctx.getClient().requireService(ItemService) as ItemService);
        svc.getInventory(_ctx.getClient(), type,
            new ResultWrapper(
            function (cause :String) :void {
                // TODO: report status somewhere
                Log.getLog(this).warning("Error retrieving inventory: " +
                    cause);
                delete _loadedTypes[type];
            },
            function (items :Array) :void {
                dataDescriptor.removeChildAt(node, null, 0);
                //collection.removeAll();
                if (items.length == 0) {
                    dataDescriptor.addChildAt(
                        node, Msgs.ITEM.get("m.no_items"), 0);

                } else {
                    var count :int = 0;
                    for each (var item :Object in items) {
                        dataDescriptor.addChildAt(node, item, count++);
                    }
                }

                // this works best of all for invalidating the list
                expandItem(node, false);
                expandItem(node, true, true);
            }));
    }

    protected var _loadedTypes :Object = new Object();
    protected var _ctx :MsoyContext;
}
}
