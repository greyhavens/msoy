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
        if ("mywhirled".equals(token)) {
            setPageTitle(CWhirled.msgs.titleMyWhirled());
            setContent(new MyWhirled());
        } else if ("whirledwide".equals(token)) {
            setPageTitle(CWhirled.msgs.titleWhirledwide());
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
