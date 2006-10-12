package com.threerings.msoy.ui {

import mx.containers.GridItem;
import mx.containers.GridRow;

import mx.core.UIComponent;

/**
 * Extends Grid with a few useful methods.
 */
public class Grid extends mx.containers.Grid
{
    /**
     * Add a new row to the grid, containing the specified
     * components.
     * 
     * @param specs a list of components, or you can follow any component
     * with a two-dimensional array that specifies grid width/height.
     *
     * Example: addRow(_label, _entryField, _bigThing, [2, 2], _smallThing);
     *
     * All will be put in the same row, but bigThing will have
     *  colspan=2 rowspan=2
     */
    public function addRow (... specs) :GridRow
    {
        var row :GridRow = new GridRow();
        var lastItem :GridItem;
        for each (var o :Object in specs) {
            if (o is UIComponent) {
                lastItem = addToRow(row, UIComponent(o));

            } else if (o is Array) {
                var arr :Array = (o as Array);
                lastItem.colSpan = int(arr[0]);
                lastItem.rowSpan = int(arr[1]);

            } else {
                throw new ArgumentError();
            }
        }
        addChild(row);
        return row;
    }

    /**
     * A convenience function to the specified component to the
     * specified row.
     */
    public function addToRow (row :GridRow, comp :UIComponent) :GridItem
    {
        var item :GridItem = new GridItem();
        item.addChild(comp);
        row.addChild(item);
        return item;
    }
}
}
