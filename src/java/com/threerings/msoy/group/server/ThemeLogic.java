//
// $Id$

package com.threerings.msoy.group.server;

import java.util.List;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.samskivert.depot.expression.ColumnExp;
import com.threerings.presents.annotation.BlockingThread;

import com.threerings.msoy.admin.data.CostsConfigObject;
import com.threerings.msoy.admin.server.RuntimeConfig;
import com.threerings.msoy.group.data.all.Theme;
import com.threerings.msoy.group.data.all.GroupMembership.Rank;
import com.threerings.msoy.group.server.persist.GroupRepository;
import com.threerings.msoy.group.server.persist.ThemeAvatarLineupRecord;
import com.threerings.msoy.group.server.persist.ThemeRecord;
import com.threerings.msoy.group.server.persist.ThemeRepository;
import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.server.ItemLogic;
import com.threerings.msoy.item.server.persist.AvatarRepository;
import com.threerings.msoy.item.server.persist.CatalogRecord;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PriceQuote;
import com.threerings.msoy.money.data.all.PurchaseResult;
import com.threerings.msoy.money.server.MoneyLogic;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.web.gwt.ServiceCodes;
import com.threerings.msoy.web.gwt.ServiceException;

import static com.threerings.msoy.Log.log;

/**
 * Contains theme related services.
 */
@BlockingThread @Singleton
public class ThemeLogic
{
    public List<Avatar> loadLineup (int groupId)
    {
        AvatarRepository repo = _itemLogic.getAvatarRepository();
        List<CatalogRecord> catalogRecords = repo.loadCatalog(Lists.transform(
            _themeRepo.loadAvatarLineup(groupId), ThemeAvatarLineupRecord.GET_CATALOG_ID));

        return Lists.transform(catalogRecords, new Function<CatalogRecord, Avatar>() {
            public Avatar apply (CatalogRecord catRec) {
                return (Avatar)(catRec.item.toItem());
            }
        });
    }

    public boolean isTheme (int groupId)
    {
        return _themeRepo.loadTheme(groupId) != null;
    }

    public Theme loadTheme (int groupId)
    {
        ThemeRecord rec = _themeRepo.loadTheme(groupId);
        return (rec != null) ? rec.toTheme() : null;
    }

    /**
     * Return a price quote for creating a new theme.
     */
    public PriceQuote quoteCreateTheme (MemberRecord mrec)
        throws ServiceException
    {
        return _moneyLogic.securePrice(mrec.memberId, THEME_PURCHASE_KEY,
            Currency.BARS, getThemeBarCost(), false);
    }

    /**
     * Create a new theme
     */
    public PurchaseResult<Theme> createTheme (
        final MemberRecord mrec, Theme theme, Currency currency, int authedAmount)
        throws ServiceException
    {
        if (_groupRepo.loadGroup(theme.groupId) == null) {
            log.warning("Attempt to create theme for non-existent group", "group", theme.groupId);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        final ThemeRecord trec = new ThemeRecord();
        trec.groupId = theme.groupId;
        if (theme.logo != null) {
            trec.logoMimeType = theme.logo.mimeType;
            trec.logoMediaHash = theme.logo.hash;
            trec.logoMediaConstraint = theme.logo.constraint;
        }
        trec.playOnEnter = theme.playOnEnter;

        // execute the purchase
        PurchaseResult<Theme> result = _moneyLogic.buyTheme(
            mrec, THEME_PURCHASE_KEY, currency, authedAmount, Currency.BARS, getThemeBarCost(),
            new MoneyLogic.BuyOperation<Theme>() {
            public Theme create (boolean magicFree, Currency currency, int amountPaid)
                throws ServiceException
            {
                _themeRepo.createTheme(trec);
                return trec.toTheme();
            }
        }).toPurchaseResult();

        return result;
    }

    /**
     * Updates information for a particular group.
     */
    public void updateTheme (MemberRecord mrec, Theme theme)
        throws ServiceException
    {
        if (!mrec.isSupport() &&
                _groupRepo.getRank(theme.groupId, mrec.memberId) != Rank.MANAGER) {
            log.warning("in updateGroup, invalid permissions");
            throw new ServiceException("m.invalid_permissions");
        }

        ThemeRecord trec = _themeRepo.loadTheme(theme.groupId);
        if (trec == null) {
            log.warning("Cannot update non-existent theme", "id", theme.groupId);
            throw new ServiceException(ServiceCodes.E_INTERNAL_ERROR);
        }

        Map<ColumnExp, Object> updates = Maps.newHashMap();
        if (theme.playOnEnter != trec.playOnEnter) {
            updates.put(ThemeRecord.PLAY_ON_ENTER, theme.playOnEnter);
        }
        if (theme.logo != null && !theme.logo.equals(trec.toLogo())) {
            updates.put(ThemeRecord.LOGO_MEDIA_HASH, theme.logo.hash);
            updates.put(ThemeRecord.LOGO_MIME_TYPE, theme.logo.mimeType);
            updates.put(ThemeRecord.LOGO_MEDIA_CONSTRAINT, theme.logo.constraint);
        }
        if (updates.size() > 0) {
            _themeRepo.updateTheme(theme.groupId, updates);
        }
    }

    /**
     * Return the current cost of forming a new group, in coins.
     */
    protected int getThemeBarCost ()
    {
        return _runtime.getBarCost(CostsConfigObject.NEW_THEME);
    }

    /** An arbitrary key for quoting group creation (purchase). */
    protected static final Object THEME_PURCHASE_KEY = new Object();

    // our dependencies
    @Inject protected ItemLogic _itemLogic;
    @Inject protected ThemeRepository _themeRepo;
    @Inject protected GroupRepository _groupRepo;
    @Inject protected MoneyLogic _moneyLogic;
    @Inject protected RuntimeConfig _runtime;

}
