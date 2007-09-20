//
// $Id$

package client.whirled;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.web.client.DeploymentConfig;
import client.msgs.MsgsEntryPoint;
import client.shell.Page;
import client.util.MsoyUI;

public class index extends MsgsEntryPoint
{
    /** Required to map this entry point to a page. */
    public static Creator getCreator ()
    {
        return new Creator() {
            public Page createPage () {
                return new index();
            }
        };
    }

    // @Override // from Page
    public void onHistoryChanged (String token)
    {
        // if we're not logged in, always display whirledwide, if we are logged in and we're just
        // hitting the start/default page, display my whirled instead.
        if (CWhirled.creds == null) {
            token = "whirledwide";
        } else if (token == null || "".equals(token)) {
            token = "mywhirled";
        }

        if ("whirledwide".equals(token)) {
            setPageTitle(CWhirled.msgs.titleWhirledwide());
            setContent(new Whirledwide());
        } else if ("mywhirled".equals(token)) {
            setPageTitle(CWhirled.msgs.titleMyWhirled());
            setContent(new MyWhirled());
        }
    }

    // @Override // from Page
    protected String getPageId ()
    {
        return "whirled";
    }

    // @Override // from Page
    protected void initContext ()
    {
        super.initContext();

        // load up our translation dictionaries
        CWhirled.msgs = (WhirledMessages)GWT.create(WhirledMessages.class);
    }
}
