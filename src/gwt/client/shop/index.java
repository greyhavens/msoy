//
// $Id$

package client.shop;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.web.client.DeploymentConfig;

import client.item.ItemTypePanel;
import client.shell.Args;
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
        return SHOP;
    }

    // @Override // from Page
    protected void initContext ()
    {
        super.initContext();

        // load up our translation dictionaries
        CShop.msgs = (ShopMessages)GWT.create(ShopMessages.class);
    }

    protected void updateInterface (Args args)
    {
        // if we're not a dev deployment, disallow guests
        if (!DeploymentConfig.devDeployment && CShop.ident == null) {
            setContent(MsoyUI.createLabel(CShop.cmsgs.noGuests(), "infoLabel"));
            return;
        }

        if (_catalog == null) {
            ItemTypePanel typeTabs = new ItemTypePanel(SHOP);
            setContent(_catalog = new CatalogPanel(typeTabs));
            setPageTabs(typeTabs);
        }
        _catalog.display(args);
    }

    protected CatalogPanel _catalog;
}
