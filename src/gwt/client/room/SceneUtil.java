//
// $Id$

package client.room;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.orth.data.MediaDesc;
import com.threerings.orth.data.MediaDescSize;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.MediaMimeTypes;
import com.threerings.msoy.data.all.StaticMediaDesc;
import com.threerings.msoy.web.gwt.Pages;

import client.ui.MsoyUI;
import client.util.FeaturedPlaceUtil;
import client.util.Link;
import client.util.MediaUtil;

/**
 * Scene related utilities.
 */
public class SceneUtil
{
    /**
     * Creates a display of a scene's canonical home scene snapshot (or the default if snapshot is
     * null) and wires up click to enter and click for a live view. Currently we only have click
     * for a live view, but soon we'll do things proper.
     */
    public static Widget createSceneView (final int sceneId, MediaDesc snapshot)
    {
        return addSceneView(sceneId, snapshot, new SimplePanel());
    }

    /**
     * Adds to the supplied panel, a display of the specified scene's canonical home scene snapshot
     * (or the default if snapshot is null) and wires up click to enter and click for a live
     * view. Currently we only have click for a live view, but soon we'll do things proper.
     */
    public static Widget addSceneView (
        final int sceneId, MediaDesc snapshot, final SimplePanel container)
    {
        if (snapshot == null) {
            snapshot = new StaticMediaDesc(MediaMimeTypes.IMAGE_JPEG, "snapshot", "default");
        }

        FlowPanel panel = new FlowPanel();
        panel.addStyleName("sceneView");
        panel.add(MediaUtil.createMediaView(snapshot, MediaDescSize.SNAPSHOT_FULL_SIZE, null));

        PushButton liveButton = MsoyUI.createImageButton("liveButton", new ClickHandler() {
            public void onClick (ClickEvent event) {
                container.clear();
                FeaturedPlaceUtil.displayFeaturedPlace(sceneId, container);
            }
        });

        PushButton enterButton = MsoyUI.createImageButton("enterButton",
            Link.createHandler(Pages.WORLD, "s" + sceneId));

        panel.add(enterButton);
        panel.add(liveButton);
        container.setWidget(panel);

        return container;
    }

    /**
     * Creates a thumbnail image for a scene using the supplied scene thumbnail descriptor. If the
     * descriptor is null, a default image will be shown.
     *
     * @param onClick a click listener to be added to the thumbnail view, or null.
     */
    public static Widget createSceneThumbView (MediaDesc thumb, ClickHandler onClick)
    {
        if (thumb != null) {
            return MediaUtil.createMediaView(thumb, MediaDescSize.SNAPSHOT_THUMB_SIZE, onClick);
        } else {
            Image image = new Image();
            if (onClick != null) {
                image.addClickHandler(onClick);
            }
            image.setUrl(DEFAULT_HALFSIZE);
            return image;
        }
    }

    /** The default image for a scene snapshot. */
    protected static final String DEFAULT_FULLSIZE =
        DeploymentConfig.staticMediaURL + "snapshot/default.jpg";

    /** The default image for a scene thumbnail. */
    protected static final String DEFAULT_HALFSIZE =
        DeploymentConfig.staticMediaURL + "snapshot/default_t.jpg";

    protected static final String LIVE_VIEW_IMAGE = "/images/ui/click_overlay.png";
}
