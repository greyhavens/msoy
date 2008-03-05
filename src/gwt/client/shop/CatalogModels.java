//
// $Id$

package client.shop;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.gwt.util.DataModel;

import com.threerings.msoy.item.data.gwt.CatalogListing;
import com.threerings.msoy.web.client.CatalogService;
import com.threerings.msoy.web.data.CatalogQuery;
import com.threerings.msoy.web.data.ListingCard;

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
                    CatalogService.CatalogResult data = (CatalogService.CatalogResult)result;
                    if (_listingCount == -1) {
                        _listingCount = data.listingCount;
                    }
                    callback.onSuccess(data.listings);
                }
            });
        }

        public void removeItem (Object item) {
            // currently we do no internal caching, so just decrement our listing count
            _listingCount--;
        }

        protected CatalogQuery _query;
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
        // fake up a listing card as that's what we remove from our models
        ListingCard card = new ListingCard();
        card.itemType = listing.detail.item.getType();
        card.catalogId = listing.catalogId;

        // now scan through our models and remove that listing from any that might contain it
        for (Iterator iter = _models.entrySet().iterator(); iter.hasNext(); ) {
            Map.Entry entry = (Map.Entry)iter.next();
            CatalogQuery query = (CatalogQuery)entry.getKey();
            if (query.itemType != listing.detail.item.getType()) {
                continue;
            }
            ((Listings)entry.getValue()).removeItem(card);
        }
    }

    protected Map _models = new HashMap(); /* CatalogQuery, Listings */
}
