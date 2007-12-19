////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.containers
{

//--------------------------------------
//  Excluded APIs
//--------------------------------------

[Exclude(name="direction", kind="property")]
[Exclude(name="focusEnabled", kind="property")]
[Exclude(name="focusManager", kind="property")]
[Exclude(name="focusPane", kind="property")]
[Exclude(name="mouseFocusEnabled", kind="property")]

[Exclude(name="adjustFocusRect", kind="method")]
[Exclude(name="getFocus", kind="method")]
[Exclude(name="isOurFocus", kind="method")]
[Exclude(name="setFocus", kind="method")]

[Exclude(name="focusIn", kind="event")]
[Exclude(name="focusOut", kind="event")]
[Exclude(name="move", kind="event")]

[Exclude(name="focusBlendMode", kind="style")]
[Exclude(name="focusSkin", kind="style")]
[Exclude(name="focusThickness", kind="style")]
[Exclude(name="horizontalGap", kind="style")]
[Exclude(name="verticalGap", kind="style")]

[Exclude(name="focusInEffect", kind="effect")]
[Exclude(name="focusOutEffect", kind="effect")]
[Exclude(name="moveEffect", kind="effect")]

/**
 *  The GridItem container defines a grid cell in GridRow container.
 *  (The GridRow container, in turn, defines a row in a Grid container.)
 *  The GridItem container can contain any number of children,
 *  which are laid out as in an HBox container.
 *  If you do not want HBox layout, create a container, such as a VBox
 *  container, as a child of the GridItem control and put other 
 *  components in this child container.
 *
 *  @mxml
 *
 *  <p>The <code>&lt;mx:GridItem&gt;</code> tag must be a child of the 
 *  <code>&lt;GridRow&gt;</code> tag, which defines a grid row.
 *  The <code>&lt;mx:GridItem&gt;</code> container inherits the
 *  tag attributes of its superclass, and adds the following tag attributes.</p>
 *
 *  <pre>
 *  &lt;mx:Grid&gt;
 *    &lt;mx:GridRow&gt;
 *      &lt;mx:GridItem
 *        rowSpan="1"
 *        colSpan="1">
 *          <i>child components</i>
 *      &lt;/mx:GridItem&gt;
 *      ...
 *    &lt;/mx:GridRow&gt;
 *    ...
 *  &lt;/mx:Grid&gt;
 *  </pre>
 *
 *  @see mx.containers.Grid
 *  @see mx.containers.GridRow
 *
 *  @includeExample examples/GridLayoutExample.mxml
 */
public class GridItem extends HBox
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
	public function GridItem()
	{
		super();
	}

	//--------------------------------------------------------------------------
	//
	//  Properties
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 */
	internal var colIndex:int = 0;

	//--------------------------------------------------------------------------
	//
	//  Public Properties
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  colSpan
	//----------------------------------

	/**
	 *  @private
	 *  Storage for the colSpan property.
	 */
	private var _colSpan:int = 1;

	[Inspectable(category="General", defaultValue="1")]

	/**
	 *  Number of columns of the Grid container spanned by the cell.
	 *
	 *  @default 1
	 */
	public function get colSpan():int
	{
		return _colSpan;
	}

	/**
	 *  @private
	 */
	public function set colSpan(value:int):void
	{
		_colSpan = value;

		invalidateSize();
	}

	//----------------------------------
	//  rowSpan
	//----------------------------------

	/**
	 *  @private
	 *  Storage for the rowSpan property.
	 */
	private var _rowSpan:int = 1;

	[Inspectable(category="General", defaultValue="1")]

	/**
	 *  Number of rows of the Grid container spanned by the cell.
	 *  You cannot extend a cell past the number of rows in the Grid.
	 *
	 *  @default 1
	 */
	public function get rowSpan():int
	{
		return _rowSpan;
	}

	/**
	 *  @private
	 */
	public function set rowSpan(value:int):void
	{
		_rowSpan = value;

		invalidateSize();
	}
}

}
