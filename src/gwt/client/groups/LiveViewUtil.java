//
// $Id$

package client.groups;

import com.threerings.orth.data.MediaDesc;
import com.threerings.orth.data.MediaDescSize;

import client.ui.MsoyUI;
import client.util.MediaUtil;

import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

/**
 * Utilities for displaying a Group's canonical scene snapshot.
 *
 * @author robin
 */
public class LiveViewUtil
{
    /**
     * Creates a display of a Group's canonical home scene snapshot (or the default).
     */
    public static Widget makeLiveViewWidget (MediaDesc snapshot, ClickHandler onClick)
    {
        if (snapshot == null) {
            return MsoyUI.createActionImage("/images/landing/whirled_click_here.jpg", "", onClick);
        }

        FlowPanel panel = new FlowPanel();
        panel.add(MediaUtil.createMediaView(snapshot, MediaDescSize.SNAPSHOT_FULL_SIZE, onClick));
        Image overlay = MsoyUI.createActionImage("/images/landing/click_overlay.png", onClick);
        overlay.addStyleName("LiveViewOverlay");
        panel.add(overlay);
        return panel;
    }
}
