//
// $Id$

package client.group;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.web.client.DeploymentConfig;

import client.msgs.MsgsEntryPoint;
import client.msgs.ThreadPanel;
import client.shell.Args;
import client.shell.Page;
import client.util.HashIntMap;
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
    public void onHistoryChanged (Args args)
    {
        setPageTitle(CGroup.msgs.groupTitle());

        // if we're not a dev deployment, disallow guests
        if (!DeploymentConfig.devDeployment && CGroup.ident == null) {
            setContent(MsoyUI.createLabel(CGroup.cmsgs.noGuests(), "infoLabel"));
            return;
        }

        if (args.get(0, "").equals("tag")) {
            setContent(new GroupList(args.get(1, "")));
        } else if (args.get(0, 0) != 0) {
            setContent(_gview);
            _gview.setGroup(args.get(0, 0));
        } else if (args.get(0, "").equals("t")) {
            setContent(new ThreadPanel(args.get(1, 0), _gmodels));
        } else {
            setContent(new GroupList());
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

    protected HashIntMap _gmodels = new HashIntMap();
    protected GroupView _gview = new GroupView(this, _gmodels);
}
