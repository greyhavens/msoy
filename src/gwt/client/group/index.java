//
// $Id$

package client.group;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.web.client.DeploymentConfig;

import client.msgs.ForumModels;
import client.msgs.ForumPanel;
import client.msgs.MsgsEntryPoint;
import client.msgs.ThreadPanel;
import client.shell.Args;
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
    public void onHistoryChanged (Args args)
    {
        setPageTitle(CGroup.msgs.groupTitle());

        // if we're not a dev deployment, disallow guests
        if (!DeploymentConfig.devDeployment && CGroup.ident == null) {
            setContent(MsoyUI.createLabel(CGroup.cmsgs.noGuests(), "infoLabel"));
            return;
        }

        if (args.get(0, 0) != 0) {
            setContent(_gview);
            _gview.setGroup(args.get(0, 0));

        } else if (args.get(0, "").equals("unread")) {
            ForumPanel fpanel = new ForumPanel(_fmodels);
            fpanel.displayUnreadThreads(false);
            setContent(fpanel);

        } else if (args.get(0, "").equals("t")) {
            int threadId = args.get(1, 0), page = args.get(2, 0), scrollToId = args.get(3, 0);
            setContent(new ThreadPanel(this, threadId, page, scrollToId, _fmodels));

        } else {
            if (_glist == null) {
                _glist = new GroupList();
            }
            setContent(_glist);
            _glist.setArgs(args);
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

    protected ForumModels _fmodels = new ForumModels();
    protected GroupView _gview = new GroupView(this, _fmodels);
    protected GroupList _glist;
}
