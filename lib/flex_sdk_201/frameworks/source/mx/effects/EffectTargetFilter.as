////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.effects
{

import mx.effects.effectClasses.PropertyChanges;

/**
 *  The EffectTargetFilter class defines a custom filter that is executed 
 *  by each transition effect on each target of the effect. 
 *
 *  <p>The EffectTargetFilter class defines a
 *  <code>defaultFilterFunction()</code> method that uses the
 *  <code>filterProperties</code> and <code>filterStyles</code> properties
 *  to determine whether to play the effect on each effect target.</p>
 *  
 *  <p>You can also define a custom filter function
 *  to implement your own filtering logic.
 *  To do so, define your filter function, and then specify that function
 *  to an EffectTargetFilter object using the <code>filterFunction</code>
 *  property.</p>
 *  
 *  <p>To configure an effect to use a custom filter, you pass an 
 *  EffectTargetFilter object to the <code>Effect.customFilter</code> property 
 *  of the effect.</p>
 */
public class EffectTargetFilter
{
    include "../core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Constructor
	//
	//--------------------------------------------------------------------------

	/**
	 *  Constructor.
	 */
	public function EffectTargetFilter()
	{
		super();
	}
	
	//--------------------------------------------------------------------------
	//
	//  Properties
	//
	//--------------------------------------------------------------------------
	
	//----------------------------------
	//  filterFunction
	//----------------------------------

	/**
	 *  A function that defines custom filter logic.
	 *  Flex calls this method on every target of the effect.
	 *  If the function returns <code>true</code>,
	 *  the effect plays on the target;
	 *  if it returns <code>false</code>, the target is skipped by the effect.
	 *  A custom filter function gives you greater control over filtering
	 *  than the <code>Effect.filter</code> property. 
	 *
	 *  <p>The filter function has the following signature:</p>
	 *
	 *  <pre>
	 *  filterFunc(propChanges:Array, instanceTarget:Object):Boolean
	 *  {
	 *      // Return true to play the effect on instanceTarget, 
	 *      // or false to not play the effect.
     *  } 
     *  </pre>
	 *
     *  <p>where:</p>
     *  
     *  <p><code>propChanges</code> - An Array of PropertyChanges objects, 
     *  one object per target component of the effect.
	 *  If a property of a target is not modified by the transition,
	 *  it is not included in this Array.</p>
     *  
     *  <p><code>instanceTarget</code> - The specific target component
	 *  of the effect that you want to filter.
	 *  Within the custom filter function, you first search the
	 *  <code>propChanges</code> Array for the PropertyChanges object
	 *  that matches the <code>instanceTarget</code> argument
	 *  by comparing the <code>instanceTarget</code> argument
	 *  to the <code>propChanges.target</code> property.</p> 
     *
	 *  @see mx.effects.effectClasses.PropertyChanges 
	 */
	public var filterFunction:Function = defaultFilterFunction;
		
	//----------------------------------
	//  filterProperties
	//----------------------------------

	/** 
	 *  An Array of Strings specifying component properties. 
	 *  If any of the properties in the Array changed on the target component, 
	 *  play the effect on the target. 
	 *
	 *  <p>If you define a custom filter function, you can examine the 
	 *  <code>filterProperties</code> property from within your function.</p>
	 */
	public var filterProperties:Array = [];
	
	//----------------------------------
	//  filterStyles
	//----------------------------------

	/** 
	 *  An Array of Strings specifying style properties. 
	 *  If any of the style properties in the Array changed on the target component, 
	 *  play the effect on the target. 
	 *
	 *  <p>If you define a custom filter function, you can examine the 
	 *  <code>filterStyles</code> property from within your function.</p>
	 */
	public var filterStyles:Array = [];
	
	//--------------------------------------------------------------------------
	//
	//  Methods
	//
	//--------------------------------------------------------------------------

	/**
	 *  The default filter function for the EffectTargetFilter class. 
	 *  If the <code>instanceTarget</code> has different start and end values
	 *  for any of the values specified by the <code>filterProperties</code>
	 *  or <code>filterStyles</code> properties, play the effect on the target.
	 *
	 *  @param propChanges An Array of PropertyChanges objects.
	 *  The <code>target</code> property of each PropertyChanges object
	 *  is equal to the effect's target. 
	 *  If no properties changed for an effect target,
	 *  it is not included in this Array.
	 *  
	 *  @param instanceTarget The target of the EffectInstance
	 *  that calls this function.
	 *  If an effect has multiple targets,
	 *  this function is called once per target. 
	 *
	 *  @return Returns <code>true</code> to allow the effect instance to play. 
	 *
	 *  @see mx.effects.effectClasses.PropertyChanges 
	 */
	protected function defaultFilterFunction(propChanges:Array,
											 instanceTarget:Object):Boolean
	{
		var n:int = propChanges.length;
		for (var i:int = 0; i < n; i++)
		{
			var props:PropertyChanges = propChanges[i];
			if (props.target == instanceTarget)
			{
				var triggers:Array = filterProperties.concat(filterStyles);
				var m:int = triggers.length;
				for (var j:int = 0; j < m; j++)
				{
					if (props.start[triggers[j]] !== undefined &&
						props.end[triggers[j]] != props.start[triggers[j]])
					{
						return true;
					}
				}
			}
		}
			
		return false;
	}
}

}
