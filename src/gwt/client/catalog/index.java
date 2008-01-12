//
// $Id$

package client.catalog;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.web.client.DeploymentConfig;

import client.item.ItemTypePanel;
import client.shell.Args;
import client.shell.Frame;
import client.shell.Page;
import client.util.MsoyUI;

/**
 * Handles the MetaSOY inventory application.
 */
public class index extends Page
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
    public void onHistoryChanged (Args args)
    {
        updateInterface(args);
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

    protected void updateInterface (Args args)
    {
        // if we're not a dev deployment, disallow guests
        if (!DeploymentConfig.devDeployment && CCatalog.ident == null) {
            setContent(MsoyUI.createLabel(CCatalog.cmsgs.noGuests(), "infoLabel"));
            return;
        }

        if (_catalog == null) {
            Frame.setTitle(CCatalog.msgs.catalogTitle());
            ItemTypePanel typeTabs = new ItemTypePanel(CATALOG);
            setContent(_catalog = new CatalogPanel(typeTabs));
            setPageTabs(typeTabs);
        }
        _catalog.display(args);
    }

    protected CatalogPanel _catalog;
}
