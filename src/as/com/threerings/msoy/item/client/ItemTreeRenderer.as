//
// $Id$
package com.threerings.msoy.item.client {

import mx.containers.Canvas;

import mx.controls.listClasses.BaseListData;

import mx.controls.treeClasses.TreeItemRenderer;
import mx.controls.treeClasses.TreeListData;

import com.threerings.msoy.ui.MediaWrapper;
import com.threerings.msoy.ui.ScalingMediaContainer;

import com.threerings.msoy.item.data.all.Item;

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
            var media :ScalingMediaContainer;
            if (_wrapper == null) {
                media = new ScalingMediaContainer(
                    MAX_MEDIA_WIDTH, MAX_MEDIA_HEIGHT);
                _wrapper = new MediaWrapper(media, MAX_MEDIA_WIDTH, MAX_MEDIA_HEIGHT);

            } else {
                media = _wrapper.getMediaContainer() as ScalingMediaContainer;
            }
            var item :Item = Item(node);
            media.setMediaDesc(item.getThumbnailMedia());

        } else if (_wrapper != null) {
            _wrapper.getMediaContainer().shutdown();
            _wrapper = null;
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
            icon = _wrapper;
            addChild(_wrapper);
        }
    }

    protected var _wrapper :MediaWrapper;

    //protected var _media :ScalingMediaBox;

    protected static const MAX_MEDIA_WIDTH :int = 120;
    protected static const MAX_MEDIA_HEIGHT :int = 100;
}
}
