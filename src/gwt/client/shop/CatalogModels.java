//
// $Id$

package client.shop;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.gwt.util.DataModel;

import com.threerings.msoy.item.data.gwt.CatalogListing;
import com.threerings.msoy.web.client.CatalogService;
import com.threerings.msoy.web.data.CatalogQuery;

import client.util.MsoyCallback;

/**
 * Maintains information on catalog listings.
 */
public class CatalogModels
{
    public static class Listings implements DataModel
    {
        public Listings (CatalogQuery query) {
            _query = query;
        }

        public byte getType () {
            return _query.itemType;
        }

        public int getItemCount () {
            return _listingCount;
        }

        public void doFetchRows (int start, int count, final AsyncCallback callback) {
            CShop.catalogsvc.loadCatalog(
                CShop.ident, _query, start, count, _listingCount == -1, new MsoyCallback() {
                public void onSuccess (Object result) {
                    _result = (CatalogService.CatalogResult)result;
                    if (_listingCount == -1) {
                        _listingCount = _result.listingCount;
                    }
                    callback.onSuccess(_result.listings);
                }
            });
        }

        public void removeItem (Object item) {
            // currently we do no internal caching, no problem!
        }

        protected CatalogQuery _query;
        protected CatalogService.CatalogResult _result;
        protected int _listingCount = -1;
    }

    public Listings getModel (CatalogQuery query)
    {
        Listings model = (Listings)_models.get(query);
        if (model == null) {
            _models.put(query, model = new Listings(query));
        }
        return model;
    }

    public void itemDelisted (CatalogListing listing)
    {
        // TODO
    }

    protected Map _models = new HashMap(); /* CatalogQuery, Listings */
}
