//
// $Id$

package client.util;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Panel;

import com.threerings.msoy.web.gwt.ConnectConfig;
import com.threerings.msoy.web.gwt.WebUserService;
import com.threerings.msoy.web.gwt.WebUserServiceAsync;

import client.shell.CShell;

/**
 * Utility method for displaying the Featured Place client.
 */
public class FeaturedPlaceUtil
{
    /**
     * Displays a scene in a mini-world client. The scene will not display chat, and the player
     * will not have an avatar in the scene. Clicking the scene will take the player there.
     *
     * @return true if the place was told to display the specified scene, false if we asked the
     * client to display the scene and it was unable to do so.
     */
    public static boolean displayFeaturedPlace (final int sceneId, final Panel container)
    {
        // if the client is already there, just tell it to change scenes
        String flashArgs = "sceneId=" + sceneId + "&featuredPlace=true";
        if (haveClient("featuredplace")) {
            return clientGo("featuredplace", flashArgs);
        }

        // otherwise we may have to look up our default server
        if (_defaultServer == null) {
            _usersvc.getConnectConfig(new MsoyCallback<ConnectConfig>() {
                public void onSuccess (ConnectConfig config) {
                    _defaultServer = config;
                    displayFeaturedPlace(sceneId, container);
                }
            });
            // this isn't strictly correct, but it's mostly correct as long as the above RPC call
            // doesn't take so long to return that the user clicks "next" and asks us to display a
            // new featured place before we know about our server
            return true;
        }

        flashArgs += "&host=" + _defaultServer.server + "&port=" + _defaultServer.port;
        String partner = CShell.getPartner();
        if (partner != null) {
            flashArgs += "&partner=" + partner;
        }
        container.clear();
        FlashClients.embedFeaturedPlaceView(container, flashArgs);
        return true;
    }

    /**
     * Returns the element that represents the Flash client.
     */
    protected static native boolean haveClient (String id) /*-{
        return $doc.getElementById(id) != null;
    }-*/;

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
