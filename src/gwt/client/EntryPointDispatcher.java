//
// $Id$

package client;

import java.util.HashMap;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;

import client.inventory.index;

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
            RootPanel.get("content").clear();
            RootPanel.get("content").add(
                new Label("Page maps to unknown entry point '" + entry + "'."));
            return;
        }

        // work around a bug in the current version of GWT
        if (RootPanel.get("logon") == null) {
            GWT.log("Zoiks, trying again!", null);
            DeferredCommand.add(new Command() {
                public void execute () {
                    onModuleLoad();
                }
            });
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
        _pages.put("inventory", client.inventory.index.getCreator());
        _pages.put("person", client.person.index.getCreator());
        _pages.put("catalog", client.catalog.index.getCreator());
    }

    protected HashMap _pages = new HashMap();
}
