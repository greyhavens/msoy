//
// $Id$

package client.item;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.msoy.item.data.all.Item;

import client.images.stuff.StuffImages;
import client.shell.CShell;
import client.shell.Pages;
import client.ui.MsoyUI;
import client.util.Link;

/**
 * Displays a way to navigate our stuff. Used on the Me page and the My Stuff page which is why it
 * lives in item.
 */
public class StuffNaviBar extends FlowPanel
{
    public StuffNaviBar (byte selectedType)
    {
        setStyleName("stuffNaviBar");

        for (byte type : Item.TYPES) {
            AbstractImagePrototype image = UP_IMAGES.get(type);
            if (image == null) {
                CShell.log("Missing stuff image for item type " + type + ".");
                continue;
            }
            add(MsoyUI.makeActionImage(image.createImage(), null, 
                Link.createListener(Pages.STUFF, "" + type)));
        }
        add(MsoyUI.createSimplePanel("clear", null));
    }

    protected static final StuffImages _images = GWT.create(StuffImages.class);
    protected static final Map<Byte, AbstractImagePrototype> UP_IMAGES =
        new HashMap<Byte, AbstractImagePrototype>();
    static {
        UP_IMAGES.put(Item.AVATAR, _images.avatar());
        UP_IMAGES.put(Item.FURNITURE, _images.furniture());
        UP_IMAGES.put(Item.DECOR, _images.decor());
        UP_IMAGES.put(Item.TOY, _images.toy());
        UP_IMAGES.put(Item.PET, _images.pet());
        UP_IMAGES.put(Item.GAME, _images.game());
        UP_IMAGES.put(Item.PHOTO, _images.photo());
        UP_IMAGES.put(Item.AUDIO, _images.audio());
        UP_IMAGES.put(Item.VIDEO, _images.video());
    }
}

