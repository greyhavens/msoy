//
// $Id$

package com.threerings.msoy.swiftly.client.model;

/**
 * Responsible for generating RequestIds in a unique manner per factory instance.
 */
public class RequestIdFactory
{
    public RequestId generateId ()
    {
        _currentId++;
        return new RequestId(_currentId);
    }

    public int _currentId = 0;
}
