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
 * Utilities for displaying a Whirled's canonical scene snapshot.
 *
 * @author robin
 */
public class LiveViewUtil
{
    /**
     * Creates a display of a Whirled's canonical home scene snapshot (or the default).
     */
    public static Widget makeLiveViewWidget (MediaDesc snapshot, ClickListener onClick)
    {
        if (snapshot == null) {
            return MsoyUI.createActionImage("/images/landing/whirled_click_here.jpg", "", onClick);
        }

        FlowPanel panel = new FlowPanel();
        panel.add(MediaUtil.createMediaView(snapshot, MediaDesc.SNAPSHOT_FULL_SIZE, onClick));
        Image overlay = MsoyUI.createActionImage("/images/landing/click_overlay.png", onClick);
        overlay.addStyleName("LiveViewOverlay");
        panel.add(overlay);
        return panel;
    }
}
