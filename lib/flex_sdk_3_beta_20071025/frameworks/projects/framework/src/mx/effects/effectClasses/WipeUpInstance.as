////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.effects.effectClasses
{

import mx.controls.SWFLoader;

/**
 *  The WipeUpInstance class implements the instance class
 *  for the WipeUp effect.
 *  Flex creates an instance of this class when it plays a WipeUp effect;
 *  you do not create one yourself.
 *
 *  @see mx.effects.WipeUp
 */  
public class WipeUpInstance extends MaskEffectInstance
{
    include "../../core/Version.as";

 	//--------------------------------------------------------------------------
	//
	//  Constructor
	//
	//--------------------------------------------------------------------------

	/**
	 *  Constructor.
	 *
	 *  @param target The Object to animate with this effect.
	 */
	public function WipeUpInstance(target:Object)
	{	
		super(target);
	}

 	//--------------------------------------------------------------------------
	//
	//  Overridden methods
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 */
	override protected function initMaskEffect():void
	{	
		super.initMaskEffect();
		
		var targetHeight:Number = target is SWFLoader && target.content ?
								  SWFLoader(target).contentHeight :
								  targetVisualBounds.height / Math.abs(target.scaleY);

		if (target.rotation != 0)
		{
			// The target.width and target.height are expressed in terms of
			// rotated coordinates, but we need to get the object's height 
			// in terms of unrotated coordinates.

			var angle:Number = target.rotation * Math.PI / 180;
			targetHeight = Math.abs(targetVisualBounds.width * Math.sin(angle) +
						   		    targetVisualBounds.height * Math.cos(angle));
		}
		
		if (showTarget)
		{
			xFrom = targetVisualBounds.x;
			yFrom = targetHeight + targetVisualBounds.y;
			xTo = targetVisualBounds.x;
			// Line up bottoms of the mask and target
			yTo = effectMask.height <= targetHeight ?
				  targetVisualBounds.y :
				  targetHeight - effectMask.height + targetVisualBounds.y;
		}
		else
		{
			xFrom = targetVisualBounds.x;
			// Line up bottoms of the mask and target
			yFrom = effectMask.height <= targetHeight ?
					targetHeight - effectMask.height  + targetVisualBounds.y:
					targetVisualBounds.y;
			xTo = targetVisualBounds.x;
			yTo = -effectMask.height + targetVisualBounds.y;
		}
	}
}

}
