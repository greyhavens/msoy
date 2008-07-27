//
// $Id: PopupMenu.java 9948 2008-07-22 17:43:35Z mdb $

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
        addStyleName("actionLabel");
        addLoadListener(new LoadListener() {
            public void onLoad (Widget sender) {}
            
            public void onError (Widget sender)
            {
                removeLoadListener(this);
                setUrl(DEFAULT_SNAPSHOT);
            }
        });
        
        setUrl(SNAPSHOT_DIR + sceneId + ".jpg");
    }
    
    protected static final String SNAPSHOT_DIR = DeploymentConfig.mediaURL + "/snapshot/";
    protected static final String DEFAULT_SNAPSHOT = DeploymentConfig.staticMediaURL + 
        "snapshot/default.jpg";
}
