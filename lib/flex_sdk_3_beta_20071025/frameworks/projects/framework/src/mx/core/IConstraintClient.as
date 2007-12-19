////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.core
{

/**
 *  The IConstraintClient interface defines the interface for components that
 *  support layout constraints. This interface is only used by implementations
 *  of constraint-based layout. 
 */

public interface IConstraintClient
{
	//--------------------------------------------------------------------------
	//
	//  Methods
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  getConstraintValue
	//----------------------------------

	/**
	 *  Returns the specified constraint value.
	 *
	 *  @param constraintName name of the constraint value. Constraint parameters are
	 *  "<code>baseline</code>", "<code>bottom</code>", "<code>horizontalCenter</code>", 
       *  "<code>left</code>", "<code>right</code>", "<code>top</code>", and 
       *  "<code>verticalCenter</code>".
	 *
	 *  @return The constraint value, or null if it is not defined.
       *
       *  <p>For more information about these parameters, see the Canvas and Panel containers and 
       *  Styles Metadata AnchorStyles.</p>
       *  @see mx.containers.Canvas
       *  @see mx.containers.Panel
       *  @see mx.styles.metadata.AnchorStyles
	 */
	function getConstraintValue(constraintName:String):*;

	//----------------------------------
	//  setConstraintValue
	//----------------------------------

	/**
	 *  Sets the specified constraint value.
	 *
	 *  @param constraintName name of the constraint value. Constraint parameters are
	 *  "<code>baseline</code>", "<code>bottom</code>", "<code>horizontalCenter</code>", 
       *  "<code>left</code>", "<code>right</code>", "<code>top</code>", and 
       *  "<code>verticalCenter</code>".
	 *
	 *  @param value The new value for the constraint.
       *
       *  For more information about these parameters, see the Canvas and Panel containers and 
       *  Styles Metadata AnchorStyles.
       *  @see mx.containers.Canvas
       *  @see mx.containers.Panel
       *  @see mx.styles.metadata.AnchorStyles
	 */
	function setConstraintValue(constraintName:String, value:*):void;
}
}