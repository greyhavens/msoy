//
// $Id$

package com.threerings.msoy.money.server;

import com.google.inject.Inject;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.gwt.HistoryListResult;
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
    public HistoryListResult getTransactionHistory (
        int memberId, Currency currency, int from, int count)
        throws ServiceException
    {
        MemberRecord mrec = requireAuthedUser();
        if (mrec.memberId != memberId && !mrec.isSupport()) {
            throw new ServiceException(ServiceCodes.E_ACCESS_DENIED);
        }

        HistoryListResult result = new HistoryListResult();
        result.history = _moneyLogic.getTransactions(memberId, currency, null, from, count, true);
        result.totalCount = _moneyLogic.getTransactionCount(memberId, currency, null);
        return result;
    }

    @Inject protected MoneyLogic _moneyLogic;
    @Inject protected MoneyRepository _moneyRepo;
}
