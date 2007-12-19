////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.states
{

import flash.display.DisplayObject;
import flash.display.DisplayObjectContainer;
import mx.core.mx_internal;
import mx.core.UIComponent;

/**
 *
 *  The RemoveChild class removes a child display object, such as a component, 
 *  from a container as part of a view state.
 *  The child is only removed from the display list, it is not deleted.
 *  You use this class in the <code>overrides</code> property of the State class.
 *
 *  @mxml
 *
 *  <p>The <code>&lt;mx:RemoveChild&gt;</code> tag
 *  has the following attributes:</p>
 *  
 *  <pre>
 *  &lt;mx:RemoveChild
 *  <b>Properties</b>
 *  target="null"
 *  /&gt;
 *  </pre>
 *
 *  @see mx.states.State
 *  @see mx.states.AddChild
 *  @see mx.states.Transition
 *  @see mx.effects.RemoveChildAction
 *
 *  @includeExample examples/StatesExample.mxml
 */
public class RemoveChild implements IOverride
{
	include "../core/Version.as";

    //--------------------------------------------------------------------------
    //
    //  Constructor
    //
    //--------------------------------------------------------------------------

    /**
     *  Constructor.
	 *
	 *  @param target The child to remove from the view.
     */
	public function RemoveChild(target:DisplayObject = null)
	{
		super();

		this.target = target;
	}

    //--------------------------------------------------------------------------
    //
    //  Variables
    //
    //--------------------------------------------------------------------------

    /**
     *  @private
     *  Parent of the removed child.
     */
	private var oldParent:DisplayObjectContainer;

    /**
     *  @private
     *  Index of the removed child.
     */
	private var oldIndex:int;
	
	/**
	 *  @private
	 */
	private var removed:Boolean;

    //--------------------------------------------------------------------------
    //
    //  Properties
    //
    //--------------------------------------------------------------------------

    //----------------------------------
	//  target
    //----------------------------------

	[Inspectable(category="General")]

	/**
	 *  The child to remove from the view.
	 */
	public var target:DisplayObject;

    //--------------------------------------------------------------------------
    //
    //  Methods
    //
    //--------------------------------------------------------------------------

    /**
     *  IOverride interface method; this class implements it as an empty method.
	 * 
	 *  @copy IOverride#initialize()
     */
    public function initialize():void
    {
    }

    /**
     *  @inheritDoc
     */
	public function apply(parent:UIComponent):void
	{
		removed = false;
		
		if (target.parent)
		{
			oldParent = target.parent;
			oldIndex = oldParent.getChildIndex(target);
			oldParent.removeChild(target);
			removed = true;
		}
	}

	/**
     *  @inheritDoc
	 */
	public function remove(parent:UIComponent):void
	{
		if (removed)
		{
			oldParent.addChildAt(target, oldIndex);

			// Make sure any changes made while the child was removed are reflected
			// properly.
			if (target is UIComponent)
				UIComponent(target).mx_internal::updateCallbacks();

			removed = false;
		}
	}
}

}
