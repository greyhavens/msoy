//
// $Id$

package client.catalog;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.web.client.DeploymentConfig;
import com.threerings.msoy.web.data.WebCreds;

import client.item.ItemEntryPoint;
import client.shell.Page;
import client.util.MsoyUI;

/**
 * Handles the MetaSOY inventory application.
 */
public class index extends ItemEntryPoint
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

    // @Override from Page
    public void onHistoryChanged (String token)
    {
        updateInterface(token);
    }

    // @Override // from Page
    protected String getPageId ()
    {
        return "catalog";
    }

    // @Override // from Page
    protected void initContext ()
    {
        super.initContext();

        // load up our translation dictionaries
        CCatalog.msgs = (CatalogMessages)GWT.create(CatalogMessages.class);
    }

    protected void updateInterface (String args)
    {
        // if we're not a dev deployment, disallow guests
        if (!DeploymentConfig.devDeployment && CCatalog.creds == null) {
            setContent(MsoyUI.createLabel(CCatalog.cmsgs.noGuests(), "infoLabel"));
            return;
        }

        if (_catalog == null) {
            setPageTitle(CCatalog.msgs.catalogTitle());
            setContent(_catalog = new CatalogPanel());
            setPageTabs(_catalog.getTabs());
        }

        int[] avals = Page.splitArgs(args);
        byte type = (avals[0] == -1) ? Item.AVATAR : (byte)avals[0];
        int pageNo = (avals.length > 1) ? avals[1] : 0;
        int itemId = (avals.length > 2) ? avals[2] : -1;
        _catalog.display(type, pageNo, itemId);
    }

    protected CatalogPanel _catalog;
}
