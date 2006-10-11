package com.threerings.msoy.world.client.editor {

import com.threerings.util.Util;

import com.threerings.msoy.item.client.ItemRenderer;

import com.threerings.msoy.world.data.FurniData;

/**
 * An Item Renderer that also understands FurniData objects and treats
 * them as a 'props': furni with no item.
 */
public class FurniItemRenderer extends ItemRenderer
{
    override protected function recheckItem () :void
    {
        super.recheckItem();

        if (data is FurniData) {
            var furni :FurniData = (data as FurniData);
            if (!Util.equals(furni, _furni)) {
                _furni = furni;

                _container.setMedia(furni.media.getMediaPath());
                _label.text = "<Prop>"
            }

        } else if (_furni != null) {
            _furni = null;
            if (_item == null) {
                _container.shutdown();
            }
        }
    }

    protected var _furni :FurniData; // the furni we're displaying
}
}
