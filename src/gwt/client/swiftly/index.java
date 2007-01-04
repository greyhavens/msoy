//
// $Id$

package client.swiftly;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

import com.threerings.msoy.web.data.LaunchConfig;

import client.shell.MsoyEntryPoint;
import client.shell.ShellContext;

/**
 * Displays a page that allows a player to launch swiftly for a given project.
 */
public class index extends MsoyEntryPoint
    implements HistoryListener
{
    /** Required to map this entry point to a page. */
    public static Creator getCreator ()
    {
        return new Creator() {
            public MsoyEntryPoint createEntryPoint () {
                return new index();
            }
        };
    }

    // from interface HistoryListener
    public void onHistoryChanged (String token)
    {
        try {
            // TODO: something
        } catch (Exception e) {
            // TODO: display error
        }
    }

    // @Override // from MsoyEntryPoint
    protected String getPageId ()
    {
        return "swiftly";
    }

    // @Override // from MsoyEntryPoint
    protected ShellContext createContext ()
    {
        return _ctx = new SwiftlyContext();
    }

    // @Override // from MsoyEntryPoint
    protected void initContext ()
    {
        super.initContext();
        // TODO: Wire up our services

        // load up our translation dictionaries
        _ctx.msgs = (SwiftlyMessages)GWT.create(SwiftlyMessages.class);
    }

    // @Override // from MsoyEntryPoint
    protected void onPageLoad ()
    {
        History.addHistoryListener(this);
        String initToken = History.getToken();
        if (initToken.length() > 0) {
            onHistoryChanged(initToken);
        } else {
            // TODO: display a list of this player's projects
        }
    }

    protected SwiftlyContext _ctx;
}
