//
// $Id$

package com.threerings.msoy.client {

import com.threerings.crowd.client.PlaceView;

/**
 * An interface that defines no methods, but indicates that the msoy PlaceView implementing it would
 * prefer chat to be shown on top of it, via ChatOverlay or ComicOverlay.
 */
public interface ChatPlaceView extends PlaceView
{
}
}
