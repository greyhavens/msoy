//
// $Id$

package com.threerings.msoy.game.client {

/**
 * This executor takes a predicate function, and will delay all jobs placed on its queue
 * until a point after the predicate becomes true.
 * 
 * <p>The predicate only gets checked when a new job is added, or when updated manually. 
 * As long as the predicate remains false, jobs will be queued up - but once it is true, 
 * the queue will be emptied, and jobs will be executed immediately.
 */
public class GatedExecutor
{
    /**
     * Creates a new executor
     * 
     * @param predicate Function with the signature: <code>function () :Boolean { }</code>
     */
    public function GatedExecutor (predicate :Function) 
    {
        this._predicate = predicate;
    }
    
    /**
     * Accepts a new job, to be executed when the predicate is true.
     * 
     * @param thunk Job function with the signature: <code>function () :void { }</code>
     */
    public function execute (thunk :Function) :void 
    {
        _queue.push(thunk);
        update();
    }
    
    /**
     * Checks the predicate, and drains the queue if it's true. 
     */
    public function update () :void
    {
        if (! _predicate()) { 
            return;
        }

        while (_queue.length > 0) {
            var delayed :Function = _queue.shift();
            delayed();
        }
    } 
    
    protected var _predicate :Function;
    protected var _queue :Array = new Array();
}
}
