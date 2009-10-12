//
// $Id$

package client.shop;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.gwt.util.ChainedCallback;
import com.threerings.gwt.util.DataModel;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.gwt.CatalogListing;
import com.threerings.msoy.item.gwt.CatalogQuery;
import com.threerings.msoy.item.gwt.CatalogService;
import com.threerings.msoy.item.gwt.CatalogServiceAsync;
import com.threerings.msoy.item.gwt.ListingCard;
import com.threerings.msoy.item.gwt.CatalogService.CatalogResult;
import com.threerings.msoy.item.gwt.CatalogService.FavoritesResult;

import client.util.LazyDataModel;
import client.util.InfoCallback;

/**
 * Maintains information on catalog listings.
 */
public class CatalogModels
{
    public static class Jumble implements DataModel<ListingCard>
    {
        public int getItemCount () {
            return -1;
        }

        public void doFetchRows (
            int start, int count, final AsyncCallback<List<ListingCard>> callback) {
            _catalogsvc.loadJumble(start, count,
                new InfoCallback<CatalogResult>() {
                    public void onSuccess (CatalogResult result) {
                        callback.onSuccess(result.listings);
                    }
                });
        }

        public void removeItem (ListingCard item) {
            // do nothing
        }
    }

    public static class Listings implements DataModel<ListingCard>
    {
        public GroupName theme;

        public Listings (CatalogQuery query) {
            _query = query;
        }

        public byte getType () {
            return _query.itemType;
        }

        public int getItemCount () {
            return -1;
        }

        public void doFetchRows (
            int start, int count, final AsyncCallback<List<ListingCard>> callback) {
            _catalogsvc.loadCatalog(_query, start, count,
                new InfoCallback<CatalogResult>() {
                    public void onSuccess (CatalogResult data) {
                        Listings.this.theme = data.theme;
                        callback.onSuccess(data.listings);
                    }
                });
        }

        public void removeItem (ListingCard item) {
            // do nothing
        }

        protected CatalogQuery _query;
    }

    public static class MemberFavorites extends LazyDataModel<ListingCard>
    {
        public MemberFavorites (int memberId, byte type) {
            _memberId = memberId;
            _type = type;
        }

        public MemberName getNoter () {
            return _noter;
        }

        protected void fetchData (AsyncCallback<List<ListingCard>> callback) {
            _catalogsvc.loadFavorites(_memberId, _type,
                new ChainedCallback<FavoritesResult, List<ListingCard>>(callback) {
                public void onSuccess (FavoritesResult result) {
                    _noter = result.noter;
                    forwardSuccess(result.favorites);
                }
            });
        }

        protected int _memberId;
        protected byte _type;
        protected MemberName _noter;
    }

    public Listings getListingsModel (CatalogQuery query)
    {
        Listings model = _lmodels.get(query);
        if (model == null) {
            _lmodels.put(query, model = new Listings(query));
        }
        return model;
    }

    public MemberFavorites getFavoritesModel (int memberId, byte itemType)
    {
        String key = memberId + ":" + itemType;
        MemberFavorites faves = _fmodels.get(key);
        if (faves == null) {
            _fmodels.put(key, faves = new MemberFavorites(memberId, itemType));
        }
        return faves;
    }

    public void getSuite (byte itemType, int catalogId,
                          final AsyncCallback<CatalogService.SuiteResult> callback)
    {
        final ItemIdent key = new ItemIdent(itemType, catalogId);
        CatalogService.SuiteResult result = _suites.get(key);
        if (result != null) {
            callback.onSuccess(result);
        } else {
            _catalogsvc.loadSuite(
                itemType, catalogId, new InfoCallback<CatalogService.SuiteResult>() {
                public void onSuccess (CatalogService.SuiteResult result) {
                    _suites.put(key, result);
                    callback.onSuccess(result);
                }
            });
        }
    }

    public void getSuite (int gameId, final AsyncCallback<CatalogService.SuiteResult> callback)
    {
        getSuite(Item.NOT_A_TYPE, gameId, callback); // hack-a-saur
    }

    public void itemDelisted (CatalogListing listing)
    {
        // fake up a listing card as that's what we remove from our models
        ListingCard card = new ListingCard();
        card.itemType = listing.detail.item.getType();
        card.catalogId = listing.catalogId;

        // now scan through our models and remove that listing from any that might contain it
        for (Map.Entry<CatalogQuery, Listings> entry : _lmodels.entrySet()) {
            if (entry.getKey().itemType == listing.detail.item.getType()) {
                entry.getValue().removeItem(card);
            }
        }
    }

    protected Map<CatalogQuery, Listings> _lmodels = new HashMap<CatalogQuery, Listings>();
    protected Map<String, MemberFavorites> _fmodels = new HashMap<String, MemberFavorites>();
    protected Map<ItemIdent, CatalogService.SuiteResult> _suites =
        new HashMap<ItemIdent, CatalogService.SuiteResult>();

    protected static final CatalogServiceAsync _catalogsvc = GWT.create(CatalogService.class);
}
