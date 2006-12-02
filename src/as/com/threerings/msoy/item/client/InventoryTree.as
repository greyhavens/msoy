package com.threerings.msoy.item.client {

import flash.display.Sprite;

import mx.controls.Tree;
import mx.controls.listClasses.IListItemRenderer;

import mx.utils.ColorUtil;

import com.threerings.msoy.item.web.Item;

/**
 * A custom tree that renders "used" items in a slightly used color.
 */
public class InventoryTree extends Tree
{
    override protected function drawRowBackground (
        s :Sprite, rowIndex :int, y :Number, height :Number, color :uint,
        dataIndex :int) :void
    {
        color = tweakColor(color, rowInfo[rowIndex].data);
        super.drawRowBackground(s, rowIndex, y, height, color, dataIndex);
    }

    override protected function drawHighlightIndicator (
        indicator :Sprite, x :Number, y :Number, width :Number, height :Number,
        color :uint, itemRenderer :IListItemRenderer) :void
    {
        color = tweakColor(color, itemRenderer.data);
        super.drawHighlightIndicator(
            indicator, x, y, width, height, color, itemRenderer);
    }

//    override protected function drawCaretIndicator (
//        indicator :Sprite, x :Number, y :Number, width :Number, height :Number,
//        color :uint, itemRenderer :IListItemRenderer) :void
//    {
//        color = tweakColor(color, itemRenderer.data);
//        super.drawCaretIndicator(
//            indicator, x, y, width, height, color, itemRenderer);
//    }

    override protected function drawSelectionIndicator (
        indicator :Sprite, x :Number, y :Number, width :Number, height :Number,
        color :uint, itemRenderer :IListItemRenderer) :void
    {
        color = tweakColor(color, itemRenderer.data);
        super.drawSelectionIndicator(
            indicator, x, y, width, height, color, itemRenderer);
    }

    protected function tweakColor (color :uint, node :Object) :uint
    {
        if ((node is Item) && Item(node).isUsed()) {
            color = ColorUtil.adjustBrightness2(color, -25);
        }
        return color;
    }
}
}
