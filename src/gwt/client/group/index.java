//
// $Id$

package client.group;

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
        setPageTitle(CGroup.msgs.groupTitle());
        // if we're not a dev deployment, disallow guests
        if (!DeploymentConfig.devDeployment && CGroup.ident == null) {
            setContent(MsoyUI.createLabel(CGroup.cmsgs.noGuests(), "infoLabel"));
            return;
        }

        if (token.length() == 0) {
            setContent(new GroupList());
        } else if (token.startsWith("tag=")) {
            setContent(new GroupList(token.substring(4)));
        } else {
            setContent(new GroupView(Integer.parseInt(token)));
        }
    }

    // @Override // from Page
    protected String getPageId ()
    {
        return "group";
    }

    // @Override // from Page
    protected void initContext ()
    {
        super.initContext();

        // load up our translation dictionaries
        CGroup.msgs = (GroupMessages)GWT.create(GroupMessages.class);
    }
}
