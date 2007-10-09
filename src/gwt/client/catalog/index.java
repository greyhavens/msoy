//
// $Id$

package client.catalog;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.web.client.DeploymentConfig;
import client.item.ItemEntryPoint;
import client.shell.Args;
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
            setPageTitle(CCatalog.msgs.catalogTitle());
            setContent(_catalog = new CatalogPanel());
            setPageTabs(_catalog.getTabs());
        }
        _catalog.display(args);
    }

    protected CatalogPanel _catalog;
}
