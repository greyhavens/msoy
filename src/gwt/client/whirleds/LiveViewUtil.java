/**
 * 
 */
package client.whirleds;

import client.ui.MsoyUI;
import client.util.MediaUtil;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.group.gwt.CanonicalImageData;

/**
 * @author robin
 *
 */
public class LiveViewUtil
{
    public static Widget makeLiveViewWidget (CanonicalImageData data, ClickListener onClick) {
        if (data.getCanonicalImage() != null) {
            // display the scene image here...
            final Widget canonicalImageWidget = LiveViewUtil.makeCanonicalImageWidget(data,
                onClick);
            return canonicalImageWidget;
        } else {
            final Image clickToPlayImage = MsoyUI.createActionImage(
                "/images/landing/whirled_click_here.jpg", "", onClick);
            return clickToPlayImage;
        }
    }

    protected static Widget makeCanonicalImageWidget (CanonicalImageData data,
        ClickListener onClick) {
        FlowPanel panel = new FlowPanel();
        Widget image = MediaUtil.createMediaView(data.getCanonicalImage(),
            MediaDesc.CANONICAL_IMAGE_SIZE,
            onClick);

        panel.add(image);

        Image overlay = MsoyUI.createActionImage("/images/landing/click_overlay.png", onClick);
        overlay.addStyleName("LiveViewOverlay");

        panel.add(overlay);
        return panel;
    }

}
