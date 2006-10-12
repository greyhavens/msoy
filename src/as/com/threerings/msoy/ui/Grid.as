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
     */
    public function addRow (... comps) :GridRow
    {
        var row :GridRow = new GridRow();
        for each (var comp :UIComponent in comps) {
            addToRow(row, comp);
        }
        addChild(row);
        return row;
    }

    /**
     * A convenience function to the specified component to the
     * specified row.
     */
    public function addToRow (row :GridRow, comp :UIComponent) :void
    {
        var item :GridItem = new GridItem();
        item.addChild(comp);
        row.addChild(item);
    }
}
}
