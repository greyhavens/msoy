//
// $Id$

package com.threerings.msoy.world.client.editor {

import com.threerings.util.Util;

import com.threerings.msoy.item.client.ItemRenderer;

import com.threerings.msoy.world.client.MsoySprite;

import com.threerings.msoy.world.data.FurniData;

/**
 * An Item Renderer that also understands FurniData objects and treats
 * them as a 'props': furni with no item.
 */
public class FurniItemRenderer extends ItemRenderer
{
    override protected function configureItem () :Boolean
    {
        var mediaShown :Boolean = super.configureItem();

        if (data is FurniData) {
            mediaShown = true;
            var furni :FurniData = (data as FurniData);
            if (!Util.equals(furni, _furni)) {
                _furni = furni;
                trace("configuring: " + furni + "/" + furni.media.getMediaPath());

                _container.setMedia(furni.media.getMediaPath());
                _label.text = "<Prop>"
            }

        } else if (_furni != null) {
            _furni = null;
        }

        if (data is EntranceSprite) {
            mediaShown = true;
            if (!Util.equals(data, _spr)) {
                _spr = (data as EntranceSprite);
                _container.setMediaClass(EntranceSprite.MEDIA_CLASS);
                _label.text = "<Entrance>";
            }

        } else {
            _spr = null;
        }

        return mediaShown;
    }

    protected var _furni :FurniData; // the furni we're displaying
    protected var _spr :EntranceSprite;
}
}
