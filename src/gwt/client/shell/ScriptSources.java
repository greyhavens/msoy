//
// $Id$

package client.shell;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;

import com.threerings.msoy.web.gwt.ArgNames;
import com.threerings.msoy.web.gwt.SharedNaviUtil;

/**
 * Allows index.html files for the top-level frame and pages to refer to scripts only by id and
 * have the src attribute injected once the module loads.
 */
public class ScriptSources
{
    /**
     * Injects the src tags of all known scripts using the given application id where appropriate.
     */
    public static void inject (int appId)
    {
        // load up various JavaScript dependencies
        String appIdStr = String.valueOf(appId);
        for (int ii = 0; ii < SOURCE_MAP.length; ii += 2) {
            Element e = DOM.getElementById(SOURCE_MAP[ii]);
            if (e != null) {
                DOM.setElementAttribute(e, "src",
                    SOURCE_MAP[ii+1].replace(APP_ID_WILDCARD, appIdStr));
            }
        }
    }

    /** For replacing in the script sources below. */
    protected static final String APP_ID_WILDCARD = "{app_id}";

    /** Enumerates our Javascript source locations by id. */
    protected static final String[] SOURCE_MAP = {
        "fbhelper", SharedNaviUtil.buildRequest("/js/facebook.js", ArgNames.APP, APP_ID_WILDCARD)
    };
}
