//
// $Id$

package com.threerings.msoy.money.server;

//import java.util.Collections;
//import java.util.Date;
//import java.util.List;
//
//import com.google.common.collect.Iterables;
//import com.google.common.collect.Lists;
//import com.google.inject.Inject;
//
//import com.samskivert.io.PersistenceException;
//import com.samskivert.util.IntMap;
//import com.samskivert.util.Tuple;
//
//import com.threerings.presents.data.InvocationCodes;
//
//import com.threerings.msoy.data.StatType;
//import com.threerings.msoy.data.all.MemberName;
//import com.threerings.msoy.server.StatLogic;
//import com.threerings.msoy.server.persist.MemberRecord;
//import com.threerings.msoy.server.persist.TagHistoryRecord;
//import com.threerings.msoy.server.persist.TagNameRecord;
//
//import com.threerings.msoy.item.data.ItemCodes;
//import com.threerings.msoy.item.data.all.ItemIdent;
//import com.threerings.msoy.item.data.all.ItemListInfo;
//import com.threerings.msoy.item.data.all.ItemListQuery;
//import com.threerings.msoy.item.data.all.Photo;
//import com.threerings.msoy.item.gwt.ItemService;
//import com.threerings.msoy.item.server.persist.AvatarRecord;
//import com.threerings.msoy.item.server.persist.AvatarRepository;
//import com.threerings.msoy.item.server.persist.CatalogRecord;
//import com.threerings.msoy.item.server.persist.ItemRecord;
//import com.threerings.msoy.item.server.persist.ItemRepository;
//import com.threerings.msoy.item.server.persist.PhotoRecord;
//import com.threerings.msoy.item.server.persist.PhotoRepository;
//
import java.util.List;

import com.google.inject.Inject;

import com.threerings.msoy.web.data.ServiceCodes;
import com.threerings.msoy.web.data.ServiceException;
//import com.threerings.msoy.web.data.TagHistory;
import com.threerings.msoy.web.server.MsoyServiceServlet;

import com.threerings.msoy.money.gwt.MoneyService;
import com.threerings.msoy.money.server.persist.MoneyRepository;

import static com.threerings.msoy.Log.log;

/**
 * Provides the server implementation of {@link MoneyService}.
 */
public class MoneyServlet extends MsoyServiceServlet
    implements MoneyService
{
    public List<Integer> getTransactionHistory (int memberId)
        throws ServiceException
    {
        log.warning("============= Woot");
        return new java.util.ArrayList<Integer>();
    }
   // TODO: this is dormant right now, but we might need something like it when we
    // enable listing purchased remixables.
//    // from interface ItemService
//    public Item remixItem (final ItemIdent iident)
//        throws ServiceException
//    {
//        MemberRecord memrec = requireAuthedUser();
//        ItemRepository<ItemRecord> repo = _itemMan.getRepository(iident.type);
//
//        try {
//            // load a copy of the clone to modify
//            final ItemRecord item = repo.loadClone(iident.itemId);
//            if (item == null) {
//                throw new ServiceException(ItemCodes.E_NO_SUCH_ITEM);
//            }
//            if (item.ownerId != memrec.memberId) {
//                throw new ServiceException(ItemCodes.E_ACCESS_DENIED);
//            }
//            // TODO: make sure item is remixable
//
//            // prep the item for remixing and insert it as a new original item
//            int originalId = item.sourceId;
//            item.prepareForRemixing();
//            repo.insertOriginalItem(item, false);
//
//            // delete the old clone
//            repo.deleteItem(iident.itemId);
//
//            // copy tags from the original to the new item
//            repo.getTagRepository().copyTags(
//                originalId, item.itemId, item.ownerId, System.currentTimeMillis());
//
//            // let the item manager know that we've created a new item
//            postDObjectAction(new Runnable() {
//                public void run () {
//                    _itemMan.itemCreated(item);
//                }
//            });
//
//            return item.toItem();
//
//        } catch (PersistenceException pe) {
//            log.warning("Failed to remix item [item=" + iident +
//                    ", for=" + memrec.memberId + "]", pe);
//            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
//        }
//    }

    @Inject protected MoneyLogic _moneyLogic;
    @Inject protected MoneyRepository _moneyRepo;
}
