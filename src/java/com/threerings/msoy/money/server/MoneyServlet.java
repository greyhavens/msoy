//
// $Id$

package com.threerings.msoy.money.server;

import com.google.inject.Inject;

import com.threerings.gwt.util.PagedResult;

import com.threerings.msoy.money.data.all.BlingInfo;
import com.threerings.msoy.money.data.all.MemberMoney;
import com.threerings.msoy.money.data.all.MoneyTransaction;
import com.threerings.msoy.money.data.all.ReportType;
import com.threerings.msoy.money.gwt.MoneyService;
import com.threerings.msoy.money.server.persist.MoneyRepository;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.web.data.ServiceCodes;
import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.server.MsoyServiceServlet;

/**
 * Provides the server implementation of {@link MoneyService}.
 */
public class MoneyServlet extends MsoyServiceServlet
    implements MoneyService
{
    public PagedResult<MoneyTransaction> getTransactionHistory (
        int memberId, ReportType report, int from, int count)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        if (mrec.memberId != memberId && !mrec.isSupport()) {
            throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
        }

        PagedResult<MoneyTransaction> result = new PagedResult<MoneyTransaction>();
        // TODO: consider putting these queries together, since they use all the same search params
        result.page = _moneyLogic.getTransactions(
            memberId, report.transactions, report.currency, from, count, true);
        result.total = _moneyLogic.getTransactionCount(memberId,
            report.transactions, report.currency);
        return result;
    }

    public BlingInfo getBlingInfo (int memberId)
        throws ServiceException
    {
        MemberMoney money = _moneyLogic.getMoneyFor(memberId);
        int blingWorth = _moneyLogic.getBlingWorth(money.bling);
        return new BlingInfo(money.bling, blingWorth);
    }

    @Inject protected MoneyLogic _moneyLogic;
    @Inject protected MoneyRepository _moneyRepo;
}
