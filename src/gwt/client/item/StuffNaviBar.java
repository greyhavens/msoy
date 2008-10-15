//
// $Id$

package client.item;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.PushButton;
import com.threerings.gwt.ui.FloatPanel;

import com.threerings.msoy.item.data.all.Item;

import client.images.stuff.StuffImages;
import client.shell.CShell;
import client.shell.Pages;
import client.util.Link;

/**
 * Displays a way to navigate our stuff. Used on the Me page and the My Stuff page which is why it
 * lives in item.
 */
public class StuffNaviBar extends FloatPanel
{
    public StuffNaviBar (byte selectedType)
    {
        super("stuffNaviBar");

        for (byte type : Item.SHOP_TYPES) {
            AbstractImagePrototype upImage = type == selectedType ? SELECTED_IMAGES.get(type)
                : UP_IMAGES.get(type);
            // over image == selected image
            AbstractImagePrototype overImage = SELECTED_IMAGES.get(type);
            AbstractImagePrototype downImage = DOWN_IMAGES.get(type);
            if (upImage == null || overImage == null || downImage == null) {
                CShell.log("Missing stuff image for item type " + type + ".");
                continue;
            }

            PushButton button = new PushButton(upImage.createImage(), downImage.createImage(),
                Link.createListener(Pages.STUFF, "" + type));
            button.getUpHoveringFace().setImage(overImage.createImage());
            add(button);
        }
    }

    protected static final StuffImages _images = GWT.create(StuffImages.class);
    protected static final Map<Byte, AbstractImagePrototype> UP_IMAGES =
        new HashMap<Byte, AbstractImagePrototype>();
    protected static final Map<Byte, AbstractImagePrototype> DOWN_IMAGES =
        new HashMap<Byte, AbstractImagePrototype>();
    protected static final Map<Byte, AbstractImagePrototype> SELECTED_IMAGES =
        new HashMap<Byte, AbstractImagePrototype>();
    static {
        UP_IMAGES.put(Item.AVATAR, _images.avatar());
        UP_IMAGES.put(Item.FURNITURE, _images.furniture());
        UP_IMAGES.put(Item.DECOR, _images.backdrop());
        UP_IMAGES.put(Item.TOY, _images.toy());
        UP_IMAGES.put(Item.PET, _images.pet());
        UP_IMAGES.put(Item.GAME, _images.game());
        UP_IMAGES.put(Item.PHOTO, _images.photo());
        UP_IMAGES.put(Item.AUDIO, _images.audio());
        UP_IMAGES.put(Item.VIDEO, _images.video());
    }
    static {
        DOWN_IMAGES.put(Item.AVATAR, _images.avatar_d());
        DOWN_IMAGES.put(Item.FURNITURE, _images.furniture_d());
        DOWN_IMAGES.put(Item.DECOR, _images.backdrop_d());
        DOWN_IMAGES.put(Item.TOY, _images.toy_d());
        DOWN_IMAGES.put(Item.PET, _images.pet_d());
        DOWN_IMAGES.put(Item.GAME, _images.game_d());
        DOWN_IMAGES.put(Item.PHOTO, _images.photo_d());
        DOWN_IMAGES.put(Item.AUDIO, _images.audio_d());
        DOWN_IMAGES.put(Item.VIDEO, _images.video_d());
    }
    static {
        SELECTED_IMAGES.put(Item.AVATAR, _images.avatar_s());
        SELECTED_IMAGES.put(Item.FURNITURE, _images.furniture_s());
        SELECTED_IMAGES.put(Item.DECOR, _images.backdrop_s());
        SELECTED_IMAGES.put(Item.TOY, _images.toy_s());
        SELECTED_IMAGES.put(Item.PET, _images.pet_s());
        SELECTED_IMAGES.put(Item.GAME, _images.game_s());
        SELECTED_IMAGES.put(Item.PHOTO, _images.photo_s());
        SELECTED_IMAGES.put(Item.AUDIO, _images.audio_s());
        SELECTED_IMAGES.put(Item.VIDEO, _images.video_s());
    }
}

