//
// $Id$

package com.threerings.msoy.party.data;

import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PriceQuote;
import com.threerings.msoy.party.client.PartyBoardService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.dobj.InvocationResponseEvent;

/**
 * Provides the implementation of the {@link PartyBoardService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class PartyBoardMarshaller extends InvocationMarshaller
    implements PartyBoardService
{
    /**
     * Marshalls results to implementations of {@link PartyBoardService.JoinListener}.
     */
    public static class JoinMarshaller extends ListenerMarshaller
        implements JoinListener
    {
        /** The method id used to dispatch {@link #foundParty}
         * responses. */
        public static final int FOUND_PARTY = 1;

        // from interface JoinMarshaller
        public void foundParty (int arg1, String arg2, int arg3)
        {
            _invId = null;
            omgr.postEvent(new InvocationResponseEvent(
                               callerOid, requestId, FOUND_PARTY,
                               new Object[] { Integer.valueOf(arg1), arg2, Integer.valueOf(arg3) }, transport));
        }

        /** The method id used to dispatch {@link #priceUpdated}
         * responses. */
        public static final int PRICE_UPDATED = 2;

        // from interface JoinMarshaller
        public void priceUpdated (PriceQuote arg1)
        {
            _invId = null;
            omgr.postEvent(new InvocationResponseEvent(
                               callerOid, requestId, PRICE_UPDATED,
                               new Object[] { arg1 }, transport));
        }

        @Override // from InvocationMarshaller
        public void dispatchResponse (int methodId, Object[] args)
        {
            switch (methodId) {
            case FOUND_PARTY:
                ((JoinListener)listener).foundParty(
                    ((Integer)args[0]).intValue(), (String)args[1], ((Integer)args[2]).intValue());
                return;

            case PRICE_UPDATED:
                ((JoinListener)listener).priceUpdated(
                    (PriceQuote)args[0]);
                return;

            default:
                super.dispatchResponse(methodId, args);
                return;
            }
        }
    }

    /** The method id used to dispatch {@link #createParty} requests. */
    public static final int CREATE_PARTY = 1;

    // from interface PartyBoardService
    public void createParty (Client arg1, Currency arg2, int arg3, String arg4, int arg5, boolean arg6, PartyBoardService.JoinListener arg7)
    {
        PartyBoardMarshaller.JoinMarshaller listener7 = new PartyBoardMarshaller.JoinMarshaller();
        listener7.listener = arg7;
        sendRequest(arg1, CREATE_PARTY, new Object[] {
            arg2, Integer.valueOf(arg3), arg4, Integer.valueOf(arg5), Boolean.valueOf(arg6), listener7
        });
    }

    /** The method id used to dispatch {@link #getCreateCost} requests. */
    public static final int GET_CREATE_COST = 2;

    // from interface PartyBoardService
    public void getCreateCost (Client arg1, InvocationService.ResultListener arg2)
    {
        InvocationMarshaller.ResultMarshaller listener2 = new InvocationMarshaller.ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(arg1, GET_CREATE_COST, new Object[] {
            listener2
        });
    }

    /** The method id used to dispatch {@link #getPartyBoard} requests. */
    public static final int GET_PARTY_BOARD = 3;

    // from interface PartyBoardService
    public void getPartyBoard (Client arg1, String arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, GET_PARTY_BOARD, new Object[] {
            arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #getPartyDetail} requests. */
    public static final int GET_PARTY_DETAIL = 4;

    // from interface PartyBoardService
    public void getPartyDetail (Client arg1, int arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, GET_PARTY_DETAIL, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #locateParty} requests. */
    public static final int LOCATE_PARTY = 5;

    // from interface PartyBoardService
    public void locateParty (Client arg1, int arg2, PartyBoardService.JoinListener arg3)
    {
        PartyBoardMarshaller.JoinMarshaller listener3 = new PartyBoardMarshaller.JoinMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, LOCATE_PARTY, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }
}
