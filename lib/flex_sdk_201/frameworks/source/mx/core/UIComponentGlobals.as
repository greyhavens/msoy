package mx.core
{

import flash.display.InteractiveObject;
import flash.geom.Matrix;
import mx.managers.ILayoutManager;

public class UIComponentGlobals
{
    /**
     *  @private
	 *  A reference to the sole instance of the LayoutManager
	 *  used by all components.
	 *
	 *  <p>This property is set in the constructor of the Application class.
	 *  If you need to override or replace LayoutManager,
	 *  set UIComponent.layoutManager in your application's constructor
	 *  after calling super().</p>
     */
    mx_internal static var layoutManager:ILayoutManager;

    /**
     *  @private
     *  When this variable is non-zero, no methods queued
	 *  by the <code>callLater()</code> method get invoked.
     *  This is used to allow short effects to play without interruption.
     *  This counter is incremented by suspendBackgroundProcessing(),
     *  decremented by resumeBackgroundProcessing(), and checked by
     *  callLaterDispatcher().
     */
    mx_internal static var callLaterSuspendCount:int = 0;

    /**
     *  @private
	 *  There is a bug (139390) where setting focus from within callLaterDispatcher
	 *  screws up the ActiveX player.  We defer focus until enterframe.
     */
    mx_internal static var callLaterDispatcherCount:int = 0;

    /**
     *  @private
	 *  There is a bug (139390) where setting focus from within callLaterDispatcher
	 *  screws up the ActiveX player.  We defer focus until enterframe.
     */
    mx_internal static var nextFocusObject:InteractiveObject;

	/**
	 *  @private
	 *  This single Matrix is used to pass information from the
	 *  horizontalGradientMatrix() or verticalGradientMatrix()
	 *  utility methods to the drawRoundRect() method.
	 *  Each call to horizontalGradientMatrix() or verticalGradientMatrix()
	 *  simply calls createGradientBox() to stuff this Matrix with new values.
	 *  We can keep restuffing the same Matrix object because these utility
	 *  methods are only used inside a call to drawRoundRect()
	 *  and the Matrix isn't needed after drawRoundRect() returns.
	 */
	mx_internal static var tempMatrix:Matrix = new Matrix();

    /**
     *  @private
	 *  Used by FlexBuilder.
     */
	mx_internal static var designTime:Boolean = false;
}

}

