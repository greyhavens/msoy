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

import mx.core.mx_internal;
import mx.effects.EffectInstance;

/**
 *  The ActionEffectInstance class is the superclass for all 
 *  action effect instance classes.
 */  
 public class ActionEffectInstance extends EffectInstance
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
	public function ActionEffectInstance(target:Object)
	{
		super(target);
	}

	//--------------------------------------------------------------------------
	//
	//  Variables
	//
	//--------------------------------------------------------------------------

	/**
	 *  Indicates whether the effect has been played (<code>true</code>), 
	 *  or not (<code>false</code>). 
	 *
	 *  <p>The <code>play()</code> method sets this property to 
	 *  <code>true</code> after the effect plays;
	 *  you do not set it directly.</p> 
	 */
	protected var playedAction:Boolean = false;
	
	/**
	 *  @private
	 */
	private var _startValue:*;
	
	//--------------------------------------------------------------------------
	//
	//  Methods
	//
	//--------------------------------------------------------------------------
	
	/**
	 *  Subclasses implement this method to save the starting state
	 *  before the effect plays.
	 */
	protected function saveStartValue():*
	{
	}
	
	/**
	 *  Used internally to retrieve the values saved by 
	 *  the <code>saveStartValue()</code> method.
	 */
	protected function getStartValue():*
	{
		return _startValue;
	}
		
	//--------------------------------------------------------------------------
	//
	//  Overridden methods
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 */
	override public function play():void
	{
		super.play();	
		
		// Don't save the value if we are playing in reverse.
		if (!mx_internal::playReversed)
			_startValue = saveStartValue();
		
		playedAction = true;
	}
	
	/**
	 *  @private
	 */
    override public function end():void
    {
    	if (!playedAction)
    		play();
    	
    	super.end();
    }
}

}
