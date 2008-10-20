//
// $Id$

package client.account;

import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.data.all.DeploymentConfig;

/**
 * Utility methods for tracking registrations, for AdWords and other purposes
 */
public class ConversionTrackingUtil
{
    /**
     * Map from entry vectors to tracking beacon URLs. Beacons will be displayed after
     * successful registration.
     */
    public static final String[][] BEACONS = new String[][] {
        { "a.shizmoo", "http://server.cpmstar.com/action.aspx?advertiserid=275&gif=1" }
    };

    /**
     * Returns true if the conversion tracking is enabled.
     */
    public static boolean isEnabled ()
    {
        return !DeploymentConfig.devDeployment;
    }

    /**
     * Creates a tracking panel that, when displayed, pings the AdWords conversion tracker.
     * If AdWords are disabled (eg. in dev deployment) returns null.
     */
    public static Widget createAdWordsTracker ()
    {
        if (! isEnabled()) {
            return null;
        }

        Frame tracker = new Frame("./googleconversion.html");
        tracker.setStyleName("AdWordsFrame");
        return tracker;
    }

    /**
     * Creates a UI element that loads a tracking beacon appropriate for the entry vector.
     */
    public static Widget createBeacon (String vector)
    {
        if (! isEnabled() || vector == null) {
            return null;
        }

        for (String[] entry : BEACONS) {
            final String vectorPattern = entry[0];
            final String url = entry[1];
            if (vector.startsWith(vectorPattern)) {
                Image beacon = new Image(url);
                beacon.setHeight("1px");
                beacon.setWidth("1px");
                return beacon;
            }
        }

        return null;
    }
}
