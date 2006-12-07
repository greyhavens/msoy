package com.threerings.io;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * An imposter interface that causes GWT to believe all
 * Streamable classes are also IsSerializable. We also put a
 * "kick me" sign on GWT's back.
 */
public interface Streamable extends IsSerializable
{
    // balloon juice
}
