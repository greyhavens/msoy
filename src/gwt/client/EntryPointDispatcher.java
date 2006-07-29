//
// $Id$

package client;

import java.util.HashMap;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * Due to the way GWT is structured, we have a single {@link EntryPoint} to our
 * entire application and it then looks for an argument passed by the page to
 * the dispatcher entry point and dynamically loads the appropriate "real"
 * entry point for that page. If it were possible to share code and data
 * between modules, we wouldn't have to do this.
 */
public class EntryPointDispatcher
    implements EntryPoint
{
    // from interface EntryPoint
    public void onModuleLoad ()
    {
        // create our static page mappings (we can't load classes by name in
        // wacky JavaScript land so we have to hardcode the mappings)
        createMappings();

        // construct the classname of the desired entry point
        String entry = getProperty("page");
        if (entry == null || entry.length() == 0) {
            GWT.log("Missing '<meta name='gwt:property' content=" +
                    "'page=PAGE'/>' page configuration.", null);
            return;
        }

        // locate the entry point creator for this page
        MsoyEntryPoint.Creator creator =
            (MsoyEntryPoint.Creator)_pages.get(entry);
        if (creator == null) {
            GWT.log("Page maps to unknown entry point " +
                    "[entry=" + entry + "].", null);
            return;
        }

        if (RootPanel.get("logon") == null) {
            GWT.log("No go. Try again!", null);
            return;
        }

        // create the entry point and fire it up
        MsoyEntryPoint point = creator.createEntryPoint();
        point.onModuleLoad();
    }

    /**
     * Reads a <gwt:property> property from the host page.
     */
    public static native String getProperty (String name) /*-{
        return $wnd.__gwt_getMetaProperty(name);
    }-*/;

    protected void createMappings ()
    {
        _pages.put("index", client.index.getCreator());
        _pages.put("profile", client.profile.index.getCreator());
        _pages.put("inventory", client.inventory.index.getCreator());
    }

    protected HashMap _pages = new HashMap();
}
