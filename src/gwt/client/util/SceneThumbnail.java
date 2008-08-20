//
// $Id$

package client.util;

import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.LoadListener;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.data.all.DeploymentConfig;

/**
 * Displays a thumbnail for a scene, or a default if none has been taken so far.
 */
public class SceneThumbnail extends Image 
{
    public SceneThumbnail (int sceneId)
    {
        this(sceneId, false);
    }

    public SceneThumbnail (int sceneId, final boolean halfSize)
    {
        addStyleName("actionLabel");
        addLoadListener(new LoadListener() {
            public void onLoad (Widget sender) {}
            
            public void onError (Widget sender)
            {
                removeLoadListener(this);
                setUrl(halfSize ? DEFAULT_HALFSIZE : DEFAULT_SNAPSHOT);
            }
        });

        setUrl(SNAPSHOT_DIR + sceneId + (halfSize ? "_t" : "") + ".jpg");
    }

    protected static final String SNAPSHOT_DIR = DeploymentConfig.mediaURL + "/snapshot/";
    protected static final String DEFAULT_SNAPSHOT = DeploymentConfig.staticMediaURL +
        "snapshot/default.jpg";
    protected static final String DEFAULT_HALFSIZE = DeploymentConfig.staticMediaURL +
        "snapshot/default_t.jpg";
}
