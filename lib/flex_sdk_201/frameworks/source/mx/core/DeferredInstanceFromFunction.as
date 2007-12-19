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
 *  A deferred instance factory that uses a generator function
 *  to create an instance of the required object.
 *  An application uses the <code>getInstance()</code> method to
 *  create an instance of an object when it is first needed and get
 *  a reference to the object thereafter.
 *
 *  @see DeferredInstanceFromClass
 */
public class DeferredInstanceFromFunction implements IDeferredInstance
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
	 *  @param generator A function that creates and returns an instance
	 *  of the required object.
     */
    public function DeferredInstanceFromFunction(generator:Function)
    {
		super();

    	this.generator = generator;
    }

	//--------------------------------------------------------------------------
	//
	//  Variables
	//
	//--------------------------------------------------------------------------

    /**
	 * 	@private
     *	The generator function.
     */
    private var generator:Function;

	/**
	 * 	@private
	 * 	The generated value.
	 */
	private var instance:Object = null;

	//--------------------------------------------------------------------------
	//
	//  Methods
	//
	//--------------------------------------------------------------------------

	/**
	 *	Returns a reference to an instance of the desired object.
	 *  If no instance of the required object exists, calls the function
	 *  specified in this class' <code>generator</code> constructor parameter.
	 * 
	 *  @return An instance of the object.
	 */
	public function getInstance():Object
	{
		if (!instance)
			instance = generator();

		return instance;
	}
}

}
