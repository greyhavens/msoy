//
// $Id$

package com.threerings.msoy.item.server;

import java.util.List;

import com.google.common.base.Function;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.IntMap;

import com.threerings.presents.annotation.BlockingThread;

import com.threerings.presents.dobj.RootDObjectManager;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.server.persist.MemberRecord;

import com.threerings.msoy.item.data.ItemCodes;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.SubItem;
import com.threerings.msoy.item.gwt.ListingCard;

import com.threerings.msoy.item.server.persist.ItemRecord;
import com.threerings.msoy.item.server.persist.ItemRepository;
import com.threerings.msoy.item.server.persist.SubItemRecord;

import com.threerings.msoy.web.data.ServiceCodes;
import com.threerings.msoy.web.data.ServiceException;

import static com.threerings.msoy.Log.log;

/**
 * Contains item related services used by servlets and other blocking thread code.
 */
@BlockingThread @Singleton
public class ItemLogic
{
    public Item createItem (MemberRecord memrec, Item item)
        throws ServiceException
    {
        return createItem(memrec, item, null);
    }

    public Item createItem (MemberRecord memrec, Item item, ItemIdent parent)
        throws ServiceException
    {
        // validate the item
        if (!item.isConsistent()) {
            log.warning("Got inconsistent item for upload? [from=" + memrec.who() +
                        ", item=" + item + "].");
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        // create the persistent item record
        ItemRepository<ItemRecord> repo = _itemMan.getRepository(item.getType());
        final ItemRecord record = repo.newItemRecord(item);

        // configure the item's creator and owner
        record.creatorId = memrec.memberId;
        record.ownerId = memrec.memberId;

        // determine this item's suite id if it is a subitem
        if (item instanceof SubItem) {
            if (parent == null) {
                log.warning("Requested to create sub-item with no parent [who=" + memrec.who() +
                            ", item=" + item + "].");
                throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
            }
            ItemRepository<ItemRecord> prepo = _itemMan.getRepository(parent.type);
            ItemRecord prec = null;
            try {
                prec = prepo.loadItem(parent.itemId);
            } catch (PersistenceException pe) {
                log.warning("Failed to load parent in createItem [who=" + memrec.who() +
                        ", item=" + item.getIdent() + ", parent=" + parent + "].");
                throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
            }
            if (prec == null) {
                log.warning("Requested to make item with missing parent [who=" + memrec.who() +
                            ", parent=" + parent + ", item=" + item + "].");
                throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
            }
            if (prec.ownerId != memrec.memberId) {
                log.warning("Requested to make item with invalid parent [who=" + memrec.who() +
                            ", parent=" + prec + ", item=" + item + "].");
                throw new ServiceException(ItemCodes.E_ACCESS_DENIED);
            }

            // if everything is kosher, we can initialize the subitem with info from its parent
            ((SubItemRecord)record).initFromParent(prec);
        }

        // TODO: validate anything else?

        // write the item to the database
        try {
            repo.insertOriginalItem(record, false);
        } catch (PersistenceException pe) {
            log.warning("Failed to create item " + item + ".", pe);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        // let the item manager know that we've created this item
        _omgr.postRunnable(new Runnable() {
            public void run () {
                _itemMan.itemCreated(record);
            }
        });

        return record.toItem();
    }

    /**
     * Resolves the member names in the supplied list of listing cards.
     */
    public void resolveCardNames (List<ListingCard> list)
        throws PersistenceException
    {
        // look up the names and build a map of memberId -> MemberName
        IntMap<MemberName> map = _memberRepo.loadMemberNames(
            list, new Function<ListingCard,Integer>() {
                public Integer apply (ListingCard card) {
                    return card.creator.getMemberId();
                }
            });
        // finally fill in the listings using the map
        for (ListingCard card : list) {
            card.creator = map.get(card.creator.getMemberId());
        }
    }

    @Inject protected MemberRepository _memberRepo;
    @Inject protected ItemManager _itemMan;
    @Inject protected RootDObjectManager _omgr;
}
