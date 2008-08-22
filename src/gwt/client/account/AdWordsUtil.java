//
// $Id$

package client.account;

import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.data.all.DeploymentConfig;

/**
 * Utility methods for tracking registrations using AdWords
 */
public class AdWordsUtil
{
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
    public static Widget createConversionTrackingPanel ()
    {
        if (! isEnabled()) {
            return null;
        }

        Frame tracker = new Frame("./googleconversion.html");
        tracker.setStyleName("AdWordsFrame");
        return tracker;
    }
}
