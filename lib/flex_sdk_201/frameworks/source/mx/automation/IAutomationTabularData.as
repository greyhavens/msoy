////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.automation
{

/**
 *  The IAutomationTabularData interface is implemented by components 
 *  which can provide their content information in a tabular form.
 */
public interface IAutomationTabularData
{

    /**
     *  The index of the first visible child.
     */
    function get firstVisibleRow():int;
    
    /**
     *  The index of the last visible child.
     */
    function get lastVisibleRow():int;

    /**
     *  The total number of rows of data available.
     */
    function get numRows():int;

    /**
     *  The total number of columns in the data available.
     */
    function get numColumns():int;

    /**
     *  Names of all columns in the data.
     */
    function get columnNames():Array;

    /**
     *  Returns a matrix containing the automation values of all parts of the components.
     *  Should be row-major (return value is an Array of rows, each of which is
     *  an Array of "items").
     *
     *  @param start The index of the starting child. 
     *
     *  @param end The index of the ending child.
     *
     *  @return A matrix containing the automation values of all parts of the components.
     */
    function getValues(start:uint = 0, end:uint = 0):Array;
    
    /**
     *  Returns the values being displayed by the component for the given data.
     *  
     *  @param data The object representing the data.
     * 
     *  @return Array containing the values being displayed by the component.
     */
    function getAutomationValueForData(data:Object):Array;
    
}

}
