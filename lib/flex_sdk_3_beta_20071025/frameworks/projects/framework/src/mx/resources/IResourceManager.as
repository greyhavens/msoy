////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2007 Adobe Systems Incorporated
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.resources
{

import flash.events.IEventDispatcher;
import flash.system.ApplicationDomain;
import flash.system.SecurityDomain;
	
/**
 *  The APIs of the IResourceManager interface 
 *  provide localization support for Flex applications.
 *
 *  <p>There are three main concepts involved in localization:
 *  locales, resources, and resource bundles.</p>
 *
 *  <p>A locale specifies a language and a country
 *  for which your application has been localized.
 *  For example, the locale <code>"en_US"</code>
 *  specifies English as spoken in the United States.
 *  (See the mx.resources.Locale class for more information.)</p>
 *
 *  <p>A resource is a named value which is locale-dependent.
 *  For example, your application might have a resource
 *  whose name is <code>"OPEN"</code>
 *  and whose value for an English locale is <code>"Open"</code>
 *  but whose value for a French locale is <code>"Ouvrir"</code>.</p>
 *
 *  <p>A resource bundle is a named group of resources
 *  whose values have been localized for a particular locale.
 *  A resource bundle is identified by the combination of its
 *  <code>bundleName</code> and its <code>locale</code>,
 *  and has a <code>content</code> Object which contains
 *  the name/value pairs for the bundle's resources.
 *  See the documentation for mx.resources.IResourceBundle
 *  for information about how you typically create resource
 *  bundles from .properties files.</p>
 *
 *  <p>A single ResourceManager object implementing the IResourceManager
 *  interface manages multiple resource bundles, possibly for multiple
 *  locales, and provides access to the resources that they contain.
 *  For example, you can retrieve a specific resource as a String by calling
 *  <code>resourceManager.getString(bundleName, resourceName)</code>.</p>
 *
 *  <p>All classes that extend UIComponent, Formatter, or Validator
 *  have a <code>resourceManager</code> property
 *  which provides a reference to the object implementing this interface.
 *  Other classes can call <code>ResourceManager.getInstance()</code>
 *  to obtain this object.</p>
 *
 *  <p>Resource retrieval methods such as <code>getString()</code>
 *  search for resources in the locales specified
 *  by the <code>localeChain</code> property.
 *  By changing this property, you can make your application
 *  suddenly use, for example, Japanese rather than English resources.</p>
 *
 *  <p>When your application starts, the ResourceManager is automatically
 *  populated with whatever resource bundles were compiled
 *  into the application.
 *  If you create a code module, by default the resources that its classes
 *  need are compiled into the module.
 *  When the module is loaded into an application, any bundles which the
 *  application does not already have are added to the ResourceManager.</p>
 *
 *  <p>You can compile "resource modules" which have only resources in them,
 *  and load them with the <code>loadResourceModule()</code> method
 *  of IResourceManager.
 *  With resource modules you can support multiple locales by loading
 *  the resources you need at runtime rather than compiling them into
 *  your application.</p>
 *
 *  <p>Although the ResourceManager is normally populated with resource bundles
 *  that were compiled into your application or loaded from modules,
 *  you can also programmatically create resource bundles and add them
 *  to the ResourceManager yourself with the <code>addResourceBundle()</code>
 *  method.</p>
 *
 *  @see mx.resources.ResourceManager
 *  @see mx.resources.IResourceBundle
 *  @see mx.resources.ResourceBundle
 */
public interface IResourceManager extends IEventDispatcher
{
	//--------------------------------------------------------------------------
	//
	//  Properties
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  localeChain
	//----------------------------------

	/**
	 *  An Array of locale Strings, such as <code>[ "en_US" ]</code>,
	 *  which specifies one or more locales to be searched for resources.
	 *  
	 *  <p>When you call the ResourceManager methods <code>getObject()</code>,
	 *  <code>getString()</code>, <code>getStringArray()</code>,
	 *  <code>getNumber()</code>, <code>getInt()</code>,
	 *  <code>getUint()</code>, <code>getBoolean()</code>, or
	 *  <code>getClass()</code> to get the value of a resource,
	 *  you specify a bundle name and a resource name,
	 *  but not a locale.
	 *  The ResourceManager starts with the first locale in the
	 *  <code>localeChain</code> and looks for a ResourceBundle
	 *  with the specified bundle name for that locale.
	 *  If such a ResourceBundle exists, and the specified resource
	 *  exists in it, then the value of that resource is returned.
	 *  Otherwise, the ResourceManager proceeds on to the other
	 *  locales in the <code>localeChain</code>.</p>
	 *
	 *  <p>This scheme makes it possible to have locales which do not
	 *  necessarily contain a complete set of localized resources.
	 *  For example, if you are localizing your application for
	 *  Indian English rather than U.S. English, you need only
	 *  supply resources for the en_IN locale in which the
	 *  Indian spelling or usage differs from that in the U.S.,
	 *  and then set the <code>localeChain</code>
	 *  to <code>[ "en_IN", "en_US" ]</code>.</p>
	 *
	 *  <p>Many framework classes assume that they can always
	 *  obtain, from some locale, the resources that they expect,
	 *  and they will throw Errors if they cannot do so.
	 *  Therefore, you must ensure that the <code>localeChain</code>
	 *  always contains a complete set of resources.
	 *  Unless you have done a complete localization of all the
	 *  framework's resources as well as your own application's
	 *  resources, you can keep the <code>"en_US"</code> locale
	 *  at the end of your <code>localeChain</code> to ensure this.</p>
	 *
	 *  <p>Setting this property causes the ResourceManager to dispatch
	 *  a <code>"change"</code> Event.</p>
	 */
	function get localeChain():Array /* of String */;

	/**
	 *  @private
	 */
	function set localeChain(value:Array /* of String */):void;
	
	//--------------------------------------------------------------------------
	//
	//  Methods
	//
	//--------------------------------------------------------------------------

	/**
	 *  Begins loading a resource module containing resource bundles.
	 *
	 *  <p>Each call to this method returns a new event-dispatching object
	 *  that you can use to learn how the loading is progressing
	 *  and whether it completes successfully or results in an error.
	 *  This object dispatches <code>ResourceEvent.PROGRESS</code>,
	 *  <code>ResourceEvent.COMPLETE</code>, and
	 *  <code>ResourceEvent.ERROR</code> events.</p>
	 *
	 *  <p>When the module has been loaded, the resource bundles
	 *  are added to the ResourceManager, but the <code>localeChain</code>
	 *  is left unchanged.
	 *  If the <code>update</code> parameter is <code>true</code>,
	 *  the <code>update()</code> method will be called.</p>
	 *
	 *  @param url The URL from which to load the resource module.
	 *
	 *  @param update A flag indicating whether to call
	 *  the <code>update()</code> method when the module has been loaded.
	 *
	 *  @param applicationDomain The ApplicationDomain passed to the
	 *  <code>load()</code> method of the IModuleInfo
	 *  that loads the resource module.
	 *  This parameter is optional and defaults to <code>null</code>.
	 *
	 *  @param securityDomain The SecurityDomain passed to the
	 *  <code>load()</code> method of the IModuleInfo
	 *  that loads the resource module.
	 *  This parameter is optional and defaults to <code>null</code>.
     * 
	 *  @return An object associated with this particular load operation
	 *  which dispatches <code>ResourceEvent.PROGRESS</code>,
	 *  <code>ResourceEvent.COMPLETE</code>, and
	 *  <code>ResourceEvent.ERROR</code> events.
	 *
	 *  @see mx.events.ResourceEvent
	 *  @see mx.resources.IResourceManager#update()
	 */
    function loadResourceModule(url:String, update:Boolean = true,
								applicationDomain:ApplicationDomain = null,
								securityDomain:SecurityDomain = null):
								IEventDispatcher;

	/**
	 *  This method has not yet been implemented.
	 */
    function unloadResourceModule(url:String, update:Boolean = true):void;

	/**
	 *  Adds the specified ResourceBundle to the ResourceManager
	 *  so that its resources can be accessed by ResourceManager
	 *  methods such as <code>getString()</code>.
	 *
	 *  @param resourceBundle The resource bundle to be added.
	 */
	function addResourceBundle(resourceBundle:IResourceBundle):void;
	
	/**
	 *  Removes the specified ResourceBundle from the ResourceManager
	 *  so that its resources can no longer be accessed by ResourceManager
	 *  methods such as <code>getString()</code>.
	 *
	 *  @param locale A locale string such as <code>"en_US"</code>.
	 *
	 *  @param bundleName A bundle name such as <code>"MyResources"</code>.
	 *
	 *  @see mx.resources.IResourceBundle
	 */
	function removeResourceBundle(locale:String, bundleName:String):void;
	
	/**
	 *  Removes all ResourceBundles for the specified locale
	 *  from the ResourceManager so that their resources
	 *  can no longer be accessed by ResourceManager methods
	 *  such as <code>getString()</code>.
	 *
	 *  @param locale A locale string such as <code>"en_US"</code>.
	 *
	 *  @see mx.resources.IResourceBundle
	 */
	function removeResourceBundlesForLocale(locale:String):void;
	
	/**
	 *  Dispatches a <code>"change"</code> Event from the
	 *  resource manager.
	 *
	 *  <p>This will cause binding expressions to re-evaluate
	 *  if they involve the ResourceManager methods
	 *  <code>getObject()</code>, <code>getString()</code>, 
	 *  <code>getStringArray()</code>, <code>getNumber()</code>, 
	 *  <code>getInt()</code>, <code>getUint()</code>, 
	 *  <code>getBoolean()</code>, or <code>getClass()</code>.</p>
	 *
	 *  <p>It will also cause the resourcesChanged() method
	 *  of a UIComponent, Formatter, or Validator to execute.
	 *  Many components implement this method to update
	 *  their state based on the latest resources.</p>
	 */
	function update():void;

	/**
	 *  Returns an Array of Strings specifying all locales for which
	 *  ResourceBundles exist in the ResourceManager.
	 *
	 *  <p>The order of locales in this array is not specified.</p>
	 *
	 *  @return An Array of locale Strings.
	 */
	function getLocales():Array /* of String */;
	
	/**
	 *  Returns an Array of Strings specifying the bundle names
	 *  for all ResourceBundles which exist in the ResourceManager
	 *  and which belong to the specified locale.
	 *
	 *  <p>The order of bundle names in this array is not specified.</p>
	 *
	 *  @param locale A locale string such as <code>"en_US"</code>.
	 *
	 *  @return An Array of bundle names.
	 *
	 *  @see mx.resources.IResourceBundle
	 */
	function getBundleNamesForLocale(locale:String):Array /* of String */;

	/**
	 *  Returns a ResourceBundle with the specified <code>locale</code>
	 *  and <code>bundleName</code> which has been previously added
	 *  to the ResourceManager with <code>addResourceBundle()</code>.
	 *  If no such ResourceBundle exists, this method returns <code>null</code>.
	 *
	 *  @param locale A locale string such as <code>"en_US"</code>.
	 *
	 *  @param bundleName A bundle name such as <code>"MyResources"</code>.
	 *
	 *  @return The ResourceBundle with the specified <code>locale</code>
	 *  and <code>bundleName</code> if one exists; otherwise <code>null</code>.
	 *
	 *  @see mx.resources.IResourceBundle
	 */
	function getResourceBundle(locale:String,
							   bundleName:String):IResourceBundle;

	/**
	 *  Searches the locales in the <code>localeChain</code>
	 *  for the specified resource and returns
	 *  the first resource bundle in which it is found.
	 *  If the resource isn't found, this method returns <code>null</code>.
	 *
	 *  @param bundleName A bundle name such as <code>"MyResources"</code>.
	 *
	 *  @param resourceName The name of a resource in the resource bundle.
	 *
	 *  @return The first ResourceBundle in the <code>localeChain</code>
	 *  that contains the specified resource, or <code>null</code>.
	 */
	function findResourceBundleWithResource(
						bundleName:String,
						resourceName:String):IResourceBundle;

	[Bindable("change")]
	
	/**
	 *  Gets the value of a specified resource as an Object.
	 *
	 *  <p>The value is returned exactly as it is stored
	 *  in the <code>content</code> Object of the ResourceBundle,
	 *  with no conversion.
	 *  If the resource was compiled from a .properties files,
	 *  the resource value in the <code>content</code> Object 
	 *  is always a String unless you used the <code>Embed()</code>
	 *  or <code>ClassReference()</code> directive, in which case
	 *  it is a Class.
	 *  Use the <code>getString()</code>, <code>getStringArray()</code>, 
	 *  <code>getNumber()</code>, <code>getInt()</code>
	 *  <code>getUint()</code>, <code>getBoolean()</code>, and
	 *  <code>getClass()</code> methods to convert the value
	 *  to more specific types.</p>
	 *
	 *  <p>If the specified resource is not found,
	 *  this method returns <code>undefined</code>.</p>
	 *
	 *  @param bundleName The name of a resource bundle.
	 *
	 *  @param resourceName The name of a resource in the resource bundle.
	 *
	 *  @param locale A specific locale to be used for the lookup,
	 *  or <code>null</code> to search all locales
	 *  in the <code>localeChain</code>.
	 *  This parameter is optional and defaults to <code>null</code>;
	 *  you should seldom need to specify it.
	 *
	 *  @return The resource value, exactly as it is stored
	 *  in the <code>content</code> Object,
	 *  or <code>undefined</code> if the resource is not found.
	 */
	function getObject(bundleName:String, resourceName:String,
					   locale:String = null):*;

	[Bindable("change")]
	
	/**
	 *  Gets the value of a specified resource as a String,
	 *  after substituting specified values for placeholders.
	 *
	 *  <p>This method calls <code>getObject()</code>
	 *  and then casts the result to a String.</p>
	 *
	 *  <p>If a <code>parameters</code> Array is passed to this method,
	 *  the parameters in it are converted to Strings
	 *  and then substituted, in order, for the placeholders
	 *  <code>"{0}"</code>, <code>"{1}"</code>, etc., in the string
	 *  before it is returned.</p>
	 *
	 *  <p>If the specified resource is not found,
	 *  this method returns <code>null</code>.</p>
	 *
	 *  @param bundleName The name of a resource bundle.
	 *
	 *  @param resourceName The name of a resource in the resource bundle.
	 *
	 *  @param parameters An Array of parameters which are
	 *  substituted for the placeholders.
	 *  Each parameter is converted to a String with <code>toString()</code>
	 *  before being substituted.
	 *
	 *  @param locale A specific locale to be used for the lookup,
	 *  or <code>null</code> to search all locales
	 *  in the <code>localeChain</code>.
	 *  This parameter is optional and defaults to <code>null</code>;
	 *  you should seldom need to specify it.
	 *
	 *  @return The resource value, as a String,
	 *  or <code>null</code> if it is not found.
	 */
	function getString(bundleName:String, resourceName:String,
	                   parameters:Array = null,
					   locale:String = null):String;
	
	[Bindable("change")]
	
	/**
	 *  Gets the value of a specified resource as an Array of Strings.
	 *
	 *  <p>This method assumes that the resource value is a String
	 *  containing a comma-separated list of items.
	 *  It calls <code>getString()</code>, splits the String
	 *  into items at the commas, and trims whitespace
	 *  before and after each item.
	 *  It is useful if you have written a line such as</p>
	 *
	 *  <pre>
	 *  COUNTRIES=India, China, Japan
	 *  </pre>
	 *
	 *  <p>in a .properties file and want to obtain the value
	 *  <code>[ "India", "China", "Japan" ]</code>
	 *  rather than <code>"India, China, Japan"</code>.</p> 
	 *
	 *  <p>If the specified resource is not found,
	 *  this method returns <code>null</code>.</p>
	 *
	 *  @param bundleName The name of a resource bundle.
	 *
	 *  @param resourceName The name of a resource in the resource bundle.
	 *
	 *  @param locale A specific locale to be used for the lookup,
	 *  or <code>null</code> to search all locales
	 *  in the <code>localeChain</code>.
	 *  This parameter is optional and defaults to <code>null</code>;
	 *  you should seldom need to specify it.
	 *
	 *  @return The resource value, as an Array of Strings,
	 *  or <code>null</code> if it is not found.
	 */
	function getStringArray(bundleName:String,
							resourceName:String,
							locale:String = null):Array /* of String */;
	
	[Bindable("change")]
	
	/**
	 *  Gets the value of a specified resource as a Number.
	 *
	 *  <p>This method calls <code>getObject()</code>
	 *  and casts the result to a Number.
	 *  It is useful if you have written a line such as</p>
	 *
	 *  <pre>
	 *  LONGITUDE=170.3
	 *  </pre>
	 *
	 *  <p>in a .properties file and want to obtain the value
	 *  <code>170.3</code> rather than <code>"170.3"</code>.</p> 
	 *
	 *  <p>If the specified resource is not found,
	 *  this method returns <code>NaN</code>.</p>
	 *
	 *  @param bundleName The name of a resource bundle.
	 *
	 *  @param resourceName The name of a resource in the resource bundle.
	 *
	 *  @param locale A specific locale to be used for the lookup,
	 *  or <code>null</code> to search all locales
	 *  in the <code>localeChain</code>.
	 *  This parameter is optional and defaults to <code>null</code>;
	 *  you should seldom need to specify it.
	 *
	 *  @return The resource value, as a Number,
	 *  or <code>NaN</code> if it is not found.
	 */
	function getNumber(bundleName:String, resourceName:String,
					   locale:String = null):Number;
	
	[Bindable("change")]
	
	/**
	 *  Gets the value of a specified resource as an <code>int</code>.
	 *
	 *  <p>This method calls <code>getObject()</code>
	 *  and casts the result to an <code>int</code>.
	 *  It is useful if you have written a line such as</p>
	 *
	 *  <pre>
	 *  MINIMUM=5
	 *  </pre>
	 *
	 *  <p>in a .properties file and want to obtain the value
	 *  <code>5</code> rather than <code>"5"</code>.</p> 
	 *
	 *  <p>If the specified resource is not found,
	 *  this method returns <code>0</code>.</p>
	 *
	 *  @param bundleName The name of a resource bundle.
	 *
	 *  @param resourceName The name of a resource in the resource bundle.
	 *
	 *  @param locale A specific locale to be used for the lookup,
	 *  or <code>null</code> to search all locales
	 *  in the <code>localeChain</code>.
	 *  This parameter is optional and defaults to <code>null</code>;
	 *  you should seldom need to specify it.
	 *
	 *  @return The resource value, as an <code>int</code>,
	 *  or <code>0</code> if it is not found.
	 */
	function getInt(bundleName:String, resourceName:String,
					locale:String = null):int;
	
	[Bindable("change")]
	
	/**
	 *  Gets the value of a specified resource as a <code>uint</code>.
	 *
	 *  <p>This method calls <code>getObject()</code>
	 *  and casts the result to a <code>uint</code>.
	 *  It is useful if you have written a line such as</p>
	 *
	 *  <pre>
	 *  MINIMUM=5
	 *  </pre>
	 *
	 *  <p>in a .properties file and want to obtain the value
	 *  <code>5</code> rather than <code>"5"</code>.</p> 
	 *
	 *  <p>If the specified resource is not found,
	 *  this method returns <code>0</code>.</p>
	 *
	 *  @param bundleName The name of a resource bundle.
	 *
	 *  @param resourceName The name of a resource in the resource bundle.
	 *
	 *  @param locale A specific locale to be used for the lookup,
	 *  or <code>null</code> to search all locales
	 *  in the <code>localeChain</code>.
	 *  This parameter is optional and defaults to <code>null</code>;
	 *  you should seldom need to specify it.
	 *
	 *  @return The resource value, as a <code>uint</code>,
	 *  or <code>0</code> if it is not found.
	 */
	function getUint(bundleName:String, resourceName:String,
					 locale:String = null):uint;
	
	[Bindable("change")]
	
	/**
	 *  Gets the value of a specified resource as a Boolean.
	 *
	 *  <p>This method first calls <code>getString()</code>
	 *  and converts the result to lowercase.
	 *  It then returns <code>true</code>
	 *  if the result was <code>"true"</code>.
	 *  and <code>false</code> otherwise.</p>
	 *
	 *  <p>If the specified resource is not found,
	 *  this method returns <code>false</code>.</p>
	 *
	 *  @param bundleName The name of a resource bundle.
	 *
	 *  @param resourceName The name of a resource in the resource bundle.
	 *
	 *  @param locale A specific locale to be used for the lookup,
	 *  or <code>null</code> to search all locales
	 *  in the <code>localeChain</code>.
	 *  This parameter is optional and defaults to <code>null</code>;
	 *  you should seldom need to specify it.
	 *
	 *  @return The resource value, as a Boolean,
	 *  or <code>false</code> if it is not found.
	 */
	function getBoolean(bundleName:String, resourceName:String,
						locale:String = null):Boolean;

	[Bindable("change")]
	
	/**
	 *  Gets the value of a specified resource as a Class.
	 *
	 *  <p>This method calls <code>getObject()</code>
	 *  and coerces it to type Class using the <code>as</code> operator.
	 *  The result will be <code>null</code> if the resource value
	 *  was not a class reference.
	 *  It is useful if you have written a lines such as</p>
	 *
	 *  <pre>
	 *  IMAGE=Embed("image.jpg")
	 *  BUTTON_SKIN=ClassReference("skins.ButtonSkin_en_US")
	 *  </pre>
	 *
	 *  <p>in a .properties file and want to obtain
	 *  the Class that the <code>Embed()</code>
	 *  or <code>ClassReference()</code> directive produced.</p> 
	 *
	 *  <p>If the specified resource is not found,
	 *  this method returns <code>null</code>.</p>
	 *
	 *  @param bundleName The name of a resource bundle.
	 *
	 *  @param resourceName The name of a resource in the resource bundle.
	 *
	 *  @param locale A specific locale to be used for the lookup,
	 *  or <code>null</code> to search all locales
	 *  in the <code>localeChain</code>.
	 *  This parameter is optional and defaults to <code>null</code>;
	 *  you should seldom need to specify it.
	 *
	 *  @return The resource value, as a <code>Class</code>,
	 *  or <code>null</code> if it is not found.
	 */
	function getClass(bundleName:String, resourceName:String,
					  locale:String = null):Class;
}

}
