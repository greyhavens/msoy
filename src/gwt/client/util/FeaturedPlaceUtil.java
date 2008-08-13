//
// $Id$

package client.util;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Panel;

import com.threerings.msoy.web.client.WebUserService;
import com.threerings.msoy.web.client.WebUserServiceAsync;
import com.threerings.msoy.web.data.ConnectConfig;

import client.shell.CShell;

/**
 * Utility method for displaying the Featured Place client.
 */
public class FeaturedPlaceUtil
{
    /**
     * Displays a scene in a mini-world client. The scene will not display chat, and the player
     * will not have an avatar in the scene. Clicking the scene will take the player there.
     */
    public static boolean displayFeaturedPlace (final int sceneId, final Panel container)
    {
        if (_defaultServer == null) {
            _usersvc.getConnectConfig(new MsoyCallback<ConnectConfig>() {
                public void onSuccess (ConnectConfig config) {
                    _defaultServer = config;
                    displayFeaturedPlace(sceneId, container);
                }
            });
            return;
        }

        String flashArgs = "sceneId=" + sceneId + "&featuredPlace=true";
        if (!clientGo("featuredplace", flashArgs)) {
            flashArgs += "&host=" + _defaultServer.server + "&port=" + _defaultServer.port;
            String partner = CShell.getPartner();
            if (partner != null) {
                flashArgs += "&partner=" + partner;
            }
            container.clear();
            FlashClients.embedFeaturedPlaceView(container, flashArgs);
        }
    }

    /**
     * Tells the featured place client to go to a particular location.
     */
    protected static native boolean clientGo (String id, String where) /*-{
        var client = $doc.getElementById(id);
        if (client) {
            // exception from JavaScript break GWT; don't let that happen
            try { return client.clientGo(where); } catch (e) {}
        }
        return false;
    }-*/;

    /** Our default world server. Configured the first time Flash is used. */
    protected static ConnectConfig _defaultServer;

    protected static final WebUserServiceAsync _usersvc = (WebUserServiceAsync)
        ServiceUtil.bind(GWT.create(WebUserService.class), WebUserService.ENTRY_POINT);
}
