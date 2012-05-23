//
// $Id$

package com.threerings.msoy.group.server;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.expression.ColumnExp;

import com.threerings.presents.annotation.BlockingThread;
import com.threerings.presents.server.PresentsDObjectMgr;

import com.threerings.web.gwt.ServiceException;

import com.threerings.msoy.admin.data.CostsConfigObject;
import com.threerings.msoy.admin.server.RuntimeConfig;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.HashMediaDesc;
import com.threerings.msoy.data.all.Theme;
import com.threerings.msoy.group.data.all.GroupMembership.Rank;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.group.server.persist.ThemeAvatarLineupRecord;
import com.threerings.msoy.group.server.persist.ThemeHomeTemplateRecord;
import com.threerings.msoy.group.server.persist.ThemeRecord;
import com.threerings.msoy.group.server.persist.ThemeRepository;
import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.MsoyItemType;
import com.threerings.msoy.item.server.ItemLogic;
import com.threerings.msoy.item.server.persist.AvatarRepository;
import com.threerings.msoy.item.server.persist.CatalogRecord;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PriceQuote;
import com.threerings.msoy.money.data.all.PurchaseResult;
import com.threerings.msoy.money.server.BuyResult;
import com.threerings.msoy.money.server.MoneyLogic.BuyOperation;
import com.threerings.msoy.money.server.MoneyLogic;
import com.threerings.msoy.room.server.persist.MsoySceneRepository;
import com.threerings.msoy.room.server.persist.SceneFurniRecord;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.web.gwt.ServiceCodes;

import static com.threerings.msoy.Log.log;

/**
 * Contains theme related services.
 */
@BlockingThread @Singleton
public class ThemeLogic
{
    public List<Avatar> loadLineup (int groupId)
    {
        return loadLineup(groupId, 0, -1);
    }

    public List<Avatar> loadLineup (int groupId, int offset, int count)
    {
        AvatarRepository repo = _itemLogic.getAvatarRepository();
        List<CatalogRecord> catalogRecords = repo.loadCatalog(Lists.transform(
            _themeRepo.loadAvatarLineup(groupId, offset, count),
            ThemeAvatarLineupRecord.GET_CATALOG_ID));

        return Lists.transform(catalogRecords, new Function<CatalogRecord, Avatar>() {
            public Avatar apply (CatalogRecord catRec) {
                return (Avatar)(catRec.item.toItem());
            }
        });
    }

    /**
     * Find if a certain item is used in any home template. This is not necessarily a cheap
     * operation, and it's somewhat questionable if we can keep doing it like this in the long
     * run. We'll have to see how it works out in practice.
     */
    public boolean isUsedInTemplate (MsoyItemType itemType, int catalogId)
        throws ServiceException
    {
        // first fetch a list of *all* home template scenes
        List<Integer> sceneIds = Lists.transform(
            _themeRepo.loadHomeTemplates(), ThemeHomeTemplateRecord.TO_SCENE_ID);

        // fetch *all* the itemIds of the correct itemType from *any* such scene
        Set<Integer> itemIds = Sets.newHashSet();
        for (SceneFurniRecord furni : _sceneRepo.loadFurni(itemType.toByte(), sceneIds)) {
            itemIds.add(furni.itemId);
        }

        // finally perform a special-purpose query among those itemIds to see if any of
        // them have the same catalogId as the item we're testing
        return _itemLogic.getRepository(itemType).containsListedItem(itemIds, catalogId);
    }

    public boolean isTheme (int groupId)
    {
        return _themeRepo.loadTheme(groupId) != null;
    }

    public Theme loadTheme (int groupId)
    {
        ThemeRecord rec = _themeRepo.loadTheme(groupId);
        return (rec != null) ? rec.toTheme(_groupRepo.loadGroupName(groupId)) : null;
    }

    /**
     * Return a price quote for creating a new theme.
     */
    public PriceQuote quoteCreateTheme (MemberRecord mrec)
        throws ServiceException
    {
        return _moneyLogic.securePrice(mrec.memberId, THEME_PURCHASE_KEY,
            Currency.BARS, getThemeBarCost(mrec), false);
    }

    /**
     * Create a new theme
     */
    public PurchaseResult<Theme> createTheme (MemberRecord mrec, final int groupId,
        Currency currency, int authedAmount)
        throws ServiceException
    {
        if (_groupRepo.loadGroup(groupId) == null) {
            log.warning("Attempt to create theme for non-existent group", "group", groupId);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }
        final GroupName groupName = _groupRepo.loadGroupName(groupId);

        BuyOperation<Theme> buyOperation = new MoneyLogic.BuyOperation<Theme>() {
            public Theme create (boolean magicFree, Currency currency, int amountPaid)
                throws ServiceException
            {
                final ThemeRecord trec = new ThemeRecord(groupId);
                _themeRepo.createTheme(trec);
                _omgr.postRunnable(new Runnable() {
                    public void run () {
                        _themeReg.newTheme(trec);
                    }
                });

                return trec.toTheme(groupName);
            }
        };

        BuyResult<Theme> buyResult = _moneyLogic.buyTheme(mrec, THEME_PURCHASE_KEY, currency,
            authedAmount, Currency.BARS, getThemeBarCost(mrec), groupName.toString(), buyOperation);
        return buyResult.toPurchaseResult();
    }

    /**
     * Updates information for a particular group.
     */
    public void updateTheme (MemberRecord mrec, Theme theme)
        throws ServiceException
    {
        if (!mrec.isSupport() &&
                _groupRepo.getRank(theme.getGroupId(), mrec.memberId) != Rank.MANAGER) {
            log.warning("in updateGroup, invalid permissions");
            throw new ServiceException("m.invalid_permissions");
        }

        ThemeRecord trec = _themeRepo.loadTheme(theme.getGroupId());
        if (trec == null) {
            log.warning("Cannot update non-existent theme", "id", theme.group);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        Map<ColumnExp<?>, Object> updates = Maps.newHashMap();
        if (theme.playOnEnter != trec.playOnEnter) {
            updates.put(ThemeRecord.PLAY_ON_ENTER, theme.playOnEnter);
        }
        if (theme.logo instanceof HashMediaDesc && !theme.logo.equals(trec.toLogo())) {
            updates.put(ThemeRecord.LOGO_MEDIA_HASH, HashMediaDesc.unmakeHash(theme.logo));
            updates.put(ThemeRecord.LOGO_MIME_TYPE, theme.logo.getMimeType());
            updates.put(ThemeRecord.LOGO_MEDIA_CONSTRAINT, theme.logo.getConstraint());
        }
        if (theme.navButton instanceof HashMediaDesc && !theme.navButton.equals(trec.toNavButton())) {
            updates.put(ThemeRecord.NAV_MEDIA_HASH, HashMediaDesc.unmakeHash(theme.navButton));
            updates.put(ThemeRecord.NAV_MIME_TYPE, theme.navButton.getMimeType());
            updates.put(ThemeRecord.NAV_MEDIA_CONSTRAINT, theme.navButton.getConstraint());
        }
        if (theme.navSelButton instanceof HashMediaDesc && !theme.navSelButton.equals(trec.toNavSelButton())) {
            updates.put(ThemeRecord.NAV_SEL_MEDIA_HASH, HashMediaDesc.unmakeHash(theme.navSelButton));
            updates.put(ThemeRecord.NAV_SEL_MIME_TYPE, theme.navSelButton.getMimeType());
            updates.put(ThemeRecord.NAV_SEL_MEDIA_CONSTRAINT, theme.navSelButton.getConstraint());
        }
        if (theme.cssMedia instanceof HashMediaDesc && !theme.cssMedia.equals(trec.toCssMedia())) {
            updates.put(ThemeRecord.CSS_MEDIA_HASH, HashMediaDesc.unmakeHash(theme.cssMedia));
        }
        if (theme.navColor != trec.navColor) {
            updates.put(ThemeRecord.NAV_COLOR, theme.navColor);
        }
        if (theme.navSelColor != trec.navSelColor) {
            updates.put(ThemeRecord.NAV_SEL_COLOR, theme.navSelColor);
        }
        if (theme.statusLevelsColor != trec.statusLevelsColor) {
            updates.put(ThemeRecord.STATUS_LEVELS_COLOR, theme.statusLevelsColor);
        }
        if (theme.statusLinksColor != trec.statusLinksColor) {
            updates.put(ThemeRecord.STATUS_LINKS_COLOR, theme.statusLinksColor);
        }
        if (theme.backgroundColor != trec.backgroundColor) {
            updates.put(ThemeRecord.BACKGROUND_COLOR, theme.backgroundColor);
        }
        if (theme.titleBackgroundColor != trec.titleBackgroundColor) {
            updates.put(ThemeRecord.TITLE_BACKGROUND_COLOR, theme.titleBackgroundColor);
        }
        if (updates.size() > 0) {
            _themeRepo.updateTheme(theme.getGroupId(), updates);
        }
    }

    /**
     * Return the current cost of creating a Whirled for the given member.
     */
    protected int getThemeBarCost (MemberRecord mrec)
    {
        return _runtime.getBarCost(CostsConfigObject.NEW_THEME_NONSUB);

    }

    /** An arbitrary key for quoting group creation (purchase). */
    protected static final Object THEME_PURCHASE_KEY = new Object();

    // our dependencies
    @Inject protected GroupRepository _groupRepo;
    @Inject protected ItemLogic _itemLogic;
    @Inject protected MoneyLogic _moneyLogic;
    @Inject protected MsoySceneRepository _sceneRepo;
    @Inject protected PresentsDObjectMgr _omgr;
    @Inject protected RuntimeConfig _runtime;
    @Inject protected ThemeRegistry _themeReg;
    @Inject protected ThemeRepository _themeRepo;
}
