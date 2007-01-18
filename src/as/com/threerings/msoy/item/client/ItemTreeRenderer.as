//
// $Id$
package com.threerings.msoy.item.client {

import mx.containers.Canvas;

import mx.controls.listClasses.BaseListData;

import mx.controls.treeClasses.TreeItemRenderer;
import mx.controls.treeClasses.TreeListData;

import com.threerings.util.MediaContainer;

import com.threerings.msoy.ui.ScalingMediaBox;

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
                _media = new ScalingMediaBox(
                    MAX_MEDIA_WIDTH, MAX_MEDIA_HEIGHT);
            }
            var item :Item = Item(node);
            _media.setMedia(item.getThumbnailPath());

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
            label.text = item.name;
            icon = _media;
            addChild(_media);
        }
    }

    protected var _media :ScalingMediaBox;

    protected static const MAX_MEDIA_WIDTH :int = 120;
    protected static const MAX_MEDIA_HEIGHT :int = 100;
}
}
