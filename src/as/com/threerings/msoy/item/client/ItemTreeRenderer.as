//
// $Id$
package com.threerings.msoy.item.client {

import mx.controls.listClasses.BaseListData;

import mx.controls.treeClasses.TreeItemRenderer;
import mx.controls.treeClasses.TreeListData;

import com.threerings.util.MediaContainer;

import com.threerings.msoy.ui.ScalingMediaContainer;

import com.threerings.msoy.item.web.Item;

public class ItemTreeRenderer extends TreeItemRenderer
{
    override public function set listData (value :BaseListData) :void
    {
        super.listData = value;
        if (!(value is TreeListData)) {
            return;
        }

        TreeListData(value).icon = null;
        var node :Object = TreeListData(value).item;
        if (node is Item) {
            if (_media == null) {
                _media = new ScalingMediaContainer(
                    MAX_MEDIA_WIDTH, MAX_MEDIA_HEIGHT);
            }
            _media.setMedia(Item(node).getThumbnailPath());

        } else if (_media != null) {
            _media.shutdown();
            _media = null;
        }
    }

    override protected function commitProperties () :void
    {
        super.commitProperties();
        if (!(listData is TreeListData)) {
            return;
        }

        var node :Object = TreeListData(listData).item;
        if (node is Item) {
            var item :Item = Item(node);
            label.text = item.getDescription();
            icon = _media;
            addChild(_media);
        }
    }

    override protected function measure () :void
    {
        super.measure();
        if (!(listData is TreeListData)) {
            return;
        }

        if (TreeListData(listData).item is Item) {
            measuredHeight = Math.max(measuredHeight, MAX_MEDIA_HEIGHT);
        }
    }

    protected var _media :MediaContainer;

    protected static const MAX_MEDIA_WIDTH :int = 120;
    protected static const MAX_MEDIA_HEIGHT :int = 100;
}
}
