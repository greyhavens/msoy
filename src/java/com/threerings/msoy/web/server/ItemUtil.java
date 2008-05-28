//
// $Id$

package com.threerings.msoy.web.server;

import java.util.List;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.IntMap;
import com.samskivert.util.IntMaps;
import com.samskivert.util.IntSet;

import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.server.persist.MemberRepository;

import com.threerings.msoy.web.data.ListingCard;

/**
 * Contains catalog- and item-related utility methods used by servlets.
 */
public class ItemUtil
{
    /**
     * Resolves the member names in the supplied list of listing cards.
     */
    public static void resolveCardNames (MemberRepository memberRepo, List<ListingCard> list)
        throws PersistenceException
    {
        // determine which member names we need to look up
        IntSet members = new ArrayIntSet();
        for (ListingCard card : list) {
            members.add(card.creator.getMemberId());
        }
        // now look up the names and build a map of memberId -> MemberName
        IntMap<MemberName> map = IntMaps.newHashIntMap();
        for (MemberName record: memberRepo.loadMemberNames(members)) {
            map.put(record.getMemberId(), record);
        }
        // finally fill in the listings using the map
        for (ListingCard card : list) {
            card.creator = map.get(card.creator.getMemberId());
        }
    }
}
