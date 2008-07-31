//
// $Id$

package com.threerings.msoy.landing.server;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ExpiringReference;

import com.threerings.presents.data.InvocationCodes;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.gwt.CatalogQuery;
import com.threerings.msoy.item.gwt.ListingCard;
import com.threerings.msoy.item.gwt.ShopData;
import com.threerings.msoy.item.server.ItemLogic;
import com.threerings.msoy.item.server.ItemManager;
import com.threerings.msoy.item.server.persist.CatalogRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;

import com.threerings.msoy.game.server.GameLogic;
import com.threerings.msoy.group.gwt.GalaxyData;
import com.threerings.msoy.group.gwt.GroupCard;
import com.threerings.msoy.group.server.persist.GroupRecord;
import com.threerings.msoy.group.server.persist.GroupRepository;

import com.threerings.msoy.server.MemberManager;
import com.threerings.msoy.server.PopularPlacesSnapshot;

import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.server.MsoyServiceServlet;

import com.threerings.msoy.landing.gwt.LandingData;
import com.threerings.msoy.landing.gwt.LandingService;

import static com.threerings.msoy.Log.log;

/**
 * Implements the {@link LandingService}.
 */
public class LandingServlet extends MsoyServiceServlet
    implements LandingService
{
    // from interface LandingService
    public LandingData getLandingData ()
        throws ServiceException
    {
        LandingData data = ExpiringReference.get(_landingData);
        if (data != null) {
            return data;
        }

        try {
            data = new LandingData();

            // determine our featured whirled based on who's online now
            PopularPlacesSnapshot pps = _memberMan.getPPSnapshot();
            List<GroupCard> popWhirleds = Lists.newArrayList();
            for (PopularPlacesSnapshot.Place card : pps.getTopWhirleds()) {
                GroupRecord group = _groupRepo.loadGroup(card.placeId);
                if (group != null) {
                    GroupCard gcard = group.toGroupCard();
                    gcard.population = card.population;
                    popWhirleds.add(gcard);
                    if (popWhirleds.size() == GalaxyData.FEATURED_WHIRLED_COUNT) {
                        break;
                    }
                }
            }
            // if we don't have enough people online, supplement with other groups
            if (popWhirleds.size() < GalaxyData.FEATURED_WHIRLED_COUNT) {
                int count = GalaxyData.FEATURED_WHIRLED_COUNT - popWhirleds.size();
                for (GroupRecord group : _groupRepo.getGroupsList(0, count)) {
                    popWhirleds.add(group.toGroupCard());
                }
            }
            data.featuredWhirleds = popWhirleds.toArray(new GroupCard[popWhirleds.size()]);

            // determine the "featured" games
            data.topGames = _gameLogic.loadTopGames(pps);

            // select the top rated avatars
            ItemRepository<?> repo = _itemMan.getAvatarRepository();
            List<ListingCard> cards = Lists.newArrayList();
            for (CatalogRecord crec : repo.loadCatalog(CatalogQuery.SORT_BY_RATING, false, null, 0,
                                                       0, null, 0, ShopData.TOP_ITEM_COUNT)) {
                cards.add(crec.toListingCard());
            }
            _itemLogic.resolveCardNames(cards);
            data.topAvatars = cards.toArray(new ListingCard[cards.size()]);

            _landingData = ExpiringReference.create(data, LANDING_DATA_EXPIRY);
            return data;

        } catch (PersistenceException pe) {
            log.warning("Failed to load landing data.", pe);
            throw new ServiceException(InvocationCodes.E_INTERNAL_ERROR);
        }
    }

    // our dependencies
    @Inject protected MemberManager _memberMan;
    @Inject protected ItemManager _itemMan;
    @Inject protected GameLogic _gameLogic;
    @Inject protected ItemLogic _itemLogic;
    @Inject protected GroupRepository _groupRepo;

    /** Contains a cached copy of our WhatIsWhirled data. */
    protected ExpiringReference<LandingData> _landingData;

    protected static final long LANDING_DATA_EXPIRY = /* 60*60* */ 1000L;
}
