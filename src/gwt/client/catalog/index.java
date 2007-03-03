//
// $Id$

package client.catalog;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.web.client.DeploymentConfig;
import com.threerings.msoy.web.data.WebCreds;

import client.item.ItemEntryPoint;
import client.shell.MsoyEntryPoint;
import client.util.MsoyUI;

/**
 * Handles the MetaSOY inventory application.
 */
public class index extends ItemEntryPoint
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
        updateInterface(token);
    }

    // @Override // from MsoyEntryPoint
    protected String getPageId ()
    {
        return "catalog";
    }

    // @Override // from MsoyEntryPoint
    protected void initContext ()
    {
        super.initContext();

        // load up our translation dictionaries
        CCatalog.msgs = (CatalogMessages)GWT.create(CatalogMessages.class);
    }

    // @Override from MsoyEntryPoint
    protected void onPageLoad ()
    {
        History.addHistoryListener(this);
    }

    // @Override from MsoyEntryPoint
    protected boolean didLogon (WebCreds creds)
    {
        boolean header = super.didLogon(creds);
        updateInterface(History.getToken());
        return header;
    }

    // @Override from MsoyEntryPoint
    protected void didLogoff ()
    {
        super.didLogoff();
        updateInterface(History.getToken());
    }

    protected void updateInterface (String historyToken)
    {
        // if we're not a dev deployment, disallow guests
        if (!DeploymentConfig.devDeployment && CCatalog.creds == null) {
            setContent(MsoyUI.createLabel(CCatalog.cmsgs.noGuests(), "infoLabel"));
            return;
        }

        if (_catalog == null) {
            setContent(_catalog = new CatalogPanel());
        }

        byte type = Item.AVATAR;
        try {
            if (historyToken != null) {
                type = Byte.parseByte(historyToken);
            }
        } catch (Exception e) {
            // whatever, just show the default
        }
        _catalog.selectType(type);
    }

    protected CatalogPanel _catalog;
}
