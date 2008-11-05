//
// $Id$

package com.threerings.msoy.money.data.all;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.threerings.msoy.data.all.MemberName;

/**
 * Contains the history of a single transaction involving coins, bars, or bling.
 */
public class MoneyTransaction
    implements IsSerializable
{
    public int memberId;
    public Date timestamp;
    public TransactionType transactionType;
    public Currency currency;
    public int amount;
    public int balance;
    public String description;
//    public Object subject; // maybe someday we'll want to return this..

    public int referenceTxId;
    public MemberName referenceMemberName;

    // Required by for serializing
    public MoneyTransaction () { }

    public MoneyTransaction (
        int memberId, Date timestamp, TransactionType transactionType, 
        Currency currency, int amount, int balance,
        String description) //, Object subject)
    {
        this.memberId = memberId;
        this.timestamp = timestamp;
        this.transactionType = transactionType;
        this.currency = currency;
        this.amount = amount;
        this.balance = balance;
        this.description = description;
    }
}
