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
 *  A deferred instance factory that creates and returns an instance
 *  of a specified class.
 *  An application can use the <code>getInstance()</code> method to
 *  create an instance of the class when it is first needed and get
 *  a reference to the instance thereafter.
 *
 *  @see DeferredInstanceFromFunction
 */
public class DeferredInstanceFromClass implements IDeferredInstance
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
	 *  @param generator The class whose instance the <code>getInstance()</code>
	 *  method creates and returns.
     */
    public function DeferredInstanceFromClass(generator:Class)
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
     *	The generator class.
     */
    private var generator:Class;

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
	 *	Creates and returns an instance of the class specified in the
	 *  DeferredInstanceFromClass constructor, if it does not yet exist;
	 *  otherwise, returns the already-created class instance.
	 *
	 *  @return An instance of the class specified in the
	 *  DeferredInstanceFromClass constructor.
	 */
	public function getInstance():Object
	{
		if (!instance)
			instance = new generator();

		return instance;
	}
}

}
