package {

import flash.display.DisplayObject;

/**
 * An interface that allows access to common methods on driving surfaces, and also requires that
 * the implementor have access to a DisplayObject that can be added to the display.
 */
public interface DrivingSurface 
{
    /** 
     * Return the display object to be shown on the display.
     */
    function getDisplayObject () :DisplayObject;

    /**
     * Returns true if the current kart location is on a road surface.
     */
    function drivingOnRoad () :Boolean;
}
}
