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

/**
 * @author robin
 *
 */
public class LiveViewUtil
{
    public static Widget makeLiveViewWidget (MediaDesc snapshot, ClickListener onClick) {
        if (snapshot != null) {
            return LiveViewUtil.makeCanonicalImageWidget(snapshot, onClick);
        } else {
            final Image clickToPlayImage = MsoyUI.createActionImage(
                "/images/landing/whirled_click_here.jpg", "", onClick);
            return clickToPlayImage;
        }
    }

    protected static Widget makeCanonicalImageWidget (MediaDesc snapshot, ClickListener onClick) {
        FlowPanel panel = new FlowPanel();
        Widget image = MediaUtil.createMediaView(snapshot, MediaDesc.CANONICAL_IMAGE_SIZE, onClick);
        panel.add(image);
        Image overlay = MsoyUI.createActionImage("/images/landing/click_overlay.png", onClick);
        overlay.addStyleName("LiveViewOverlay");
        panel.add(overlay);
        return panel;
    }

}
