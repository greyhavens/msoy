//
// $Id$

package com.threerings.msoy.avrg.client {

import com.threerings.util.Log;

import com.threerings.presents.client.Client;

/**
 * Encapsulates the code for bracketing transactions on an arbitrary collection of clients. An
 * instance of this class is used by the AVRG server agent to implement the <code>doBatch</code>
 * API call. This may become important for AVRGs, particularly their server agents, to overcome the
 * message rate limit. The means of starting and committing transactions is to use the methods in
 * <code>InvocationDirector</code>. Therefore the technique only actually batches invocation
 * requests, not <code>DObject</code> activity.
 *
 * <p>Note that using <code>InvocationDirector</code> transactions in no way affects the number of
 * outgoing messages on the server. Therefore this is an incomplete solution and extensive work
 * will be needed on the client and server in order to remove the potential risk of a large number
 * of outgoing messages from the server as a result of a small number of incoming message from the
 * AVRG.</p>
 */
public class Transactions
{
    public var log :Log = Log.getLog(this);

    /**
     * Prepares to issue transactions. No actual transactions are started here, but all subsequent
     * calls to <code>addClient</code> will ensure that the client's invocation director is in a
     * transaction.
     */
    public function start () :void
    {
        // Prepare to receive transactions
        _transactionStack.push([]);
    }

    /**
     * Ends all transactions started by <code>addClient</code> since <code>start</code> was called.
     */
    public function commit () :void
    {
        if (_transactionStack.length == 0) {
            throw new Error("No batch started, cannot commit");
        }

        var transactions :Array = _transactionStack.pop() as Array;

        // Commit transactions
        for each (var client :Client in transactions) {
            client.getInvocationDirector().commitTransaction();
        }
    }

    /**
     * If <code>start</code> has been called one or more times, makes sure that the given client's
     * invocation director has started a transaction since the most recent call. Otherwise does
     * nothing.
     */
    public function addClient (client :Client) :void
    {
        var count :int = _transactionStack.length;
        if (count > 0) {
            var current :Array = _transactionStack[count - 1] as Array;
            var idx :int = current.indexOf(client);
            if (idx < 0) {
                current.push(client);
                log.debug("Entering transaction [client=" + client + "]");
                client.getInvocationDirector().startTransaction();
            }
        }
    }

    /** Contains arrays of <code>Client<code>s that are in transactions. */
    protected var _transactionStack :Array = [];
}
}
