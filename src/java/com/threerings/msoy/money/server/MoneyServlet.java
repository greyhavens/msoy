//
// $Id$

package com.threerings.msoy.money.server;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.samskivert.util.IntMap;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.money.data.MoneyCodes;
import com.threerings.msoy.money.data.all.BlingExchangeResult;
import com.threerings.msoy.money.data.all.BlingInfo;
import com.threerings.msoy.money.data.all.CashOutBillingInfo;
import com.threerings.msoy.money.data.all.CashOutEntry;
import com.threerings.msoy.money.data.all.CashOutInfo;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.MoneyTransaction;
import com.threerings.msoy.money.data.all.ReportType;
import com.threerings.msoy.money.data.all.TransactionPageResult;
import com.threerings.msoy.money.gwt.MoneyService;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.server.persist.MemberRepository;
import com.threerings.msoy.web.data.ServiceCodes;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.server.MsoyServiceServlet;

/**
 * Provides the server implementation of {@link MoneyService}.
 */
public class MoneyServlet extends MsoyServiceServlet
    implements MoneyService
{
    public TransactionPageResult getTransactionHistory (
        int memberId, ReportType report, int from, int count)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        if (mrec.memberId != memberId && !mrec.isSupport()) {
            throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
        }

        List<MoneyTransaction> page = _moneyLogic.getTransactions(
            memberId, report.transactions, report.currency, from, count, true, mrec.isSupport());
        int total = _moneyLogic.getTransactionCount(memberId,
            report.transactions, report.currency);
        return new TransactionPageResult(total, page, getBlingInfo(memberId));
    }
    
    public BlingExchangeResult exchangeBlingForBars (int memberId, int blingAmount)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        // TODO: I guess someday we'll allow support+ to exchange for others
        if (mrec.memberId != memberId) {
            throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
        }
        try {
            return _moneyLogic.exchangeBlingForBars(memberId, blingAmount);
        } catch (NotEnoughMoneyException neme) {
            // TODO: return new balance, brah
            throw new ServiceException(MoneyCodes.E_INSUFFICIENT_BLING);
        }
    }
    
    public BlingInfo requestCashOutBling (int memberId, int blingAmount, CashOutBillingInfo info)
        throws ServiceException
    {
        try {
            return _moneyLogic.requestCashOutBling(memberId, blingAmount, info);
        } catch (NotEnoughMoneyException neme) {
            throw new ServiceException(MoneyCodes.E_INSUFFICIENT_BLING);
        } catch (AlreadyCashedOutException acoe) {
            throw new ServiceException(MoneyCodes.E_ALREADY_CASHED_OUT);
        } catch (BelowMinimumBlingException bmbe) {
            throw new ServiceException(MoneyCodes.E_BELOW_MINIMUM_BLING);
        }
    }

    public List<CashOutEntry> getBlingCashOutRequests ()
        throws ServiceException
    {
        Map<Integer, CashOutInfo> blingMap = _moneyLogic.getBlingCashOutRequests();
        
        // Get all member names for the members in the map.
        final IntMap<MemberName> names = _memberRepo.loadMemberNames(blingMap.keySet());
        
        // Transform the list into cash out entries.
        return Lists.newArrayList(Iterables.transform(blingMap.entrySet(), 
            new Function<Map.Entry<Integer, CashOutInfo>, CashOutEntry>() {
                public CashOutEntry apply (Entry<Integer, CashOutInfo> entry) {
                    return new CashOutEntry(entry.getKey(), names.get(entry.getKey()).getNormal(), 
                        entry.getValue(), "todo@email.com");
                }
        }));
    }

    public void supportAdjust (int memberId, Currency currency, int delta)
        throws ServiceException
    {
        MemberRecord mrec = requireSupportUser();

        // Additional safety checks in MoneyLogic
        _moneyLogic.supportAdjust(memberId, currency, delta, mrec.memberId, mrec.name);
    }
    
    /**
     * Not a public service method, Called by getTransactionHistory
     */
    protected BlingInfo getBlingInfo (int memberId)
        throws ServiceException
    {
        return _moneyLogic.getBlingInfo(memberId);
    }

    @Inject protected MoneyLogic _moneyLogic;
    @Inject protected MemberRepository _memberRepo;
}
