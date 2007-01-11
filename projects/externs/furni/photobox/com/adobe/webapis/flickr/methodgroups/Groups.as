/*
Adobe Systems Incorporated(r) Source Code License Agreement
Copyright(c) 2005 Adobe Systems Incorporated. All rights reserved.
	
Please read this Source Code License Agreement carefully before using
the source code.
	
Adobe Systems Incorporated grants to you a perpetual, worldwide, non-exclusive,
no-charge, royalty-free, irrevocable copyright license, to reproduce,
prepare derivative works of, publicly display, publicly perform, and
distribute this source code and such derivative works in source or
object code form without any attribution requirements.
	
The name "Adobe Systems Incorporated" must not be used to endorse or promote products
derived from the source code without prior written permission.
	
You agree to indemnify, hold harmless and defend Adobe Systems Incorporated from and
against any loss, damage, claims or lawsuits, including attorney's
fees that arise or result from your use or distribution of the source
code.
	
THIS SOURCE CODE IS PROVIDED "AS IS" AND "WITH ALL FAULTS", WITHOUT
ANY TECHNICAL SUPPORT OR ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING,
BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE ARE DISCLAIMED. ALSO, THERE IS NO WARRANTY OF
NON-INFRINGEMENT, TITLE OR QUIET ENJOYMENT. IN NO EVENT SHALL MACROMEDIA
OR ITS SUPPLIERS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOURCE CODE, EVEN IF
ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.adobe.webapis.flickr.methodgroups {
	
	import com.adobe.webapis.flickr.events.FlickrResultEvent;
	import com.adobe.webapis.flickr.*;
	import flash.events.Event;
	import flash.net.URLLoader;
	
		/**
		 * Broadcast as a result of the browse method being called
		 *
		 * The event contains the following properties
		 *	success	- Boolean indicating if the call was successful or not
		 *	data - When success is true, contains a "category" Category instance
		 *		   When success is false, contains an "error" FlickrError instance
		 *
		 * @see #browse
		 * @see com.adobe.service.flickr.FlickrError
		 * @langversion ActionScript 3.0
		 * @playerversion Flash 8.5
		 * @tiptext
		 */
		[Event(name="groupsBrowse", 
			 type="com.adobe.webapis.flickr.events.FlickrResultEvent")]
			 
		/**
		 * Broadcast as a result of the getInfo method being called
		 *
		 * The event contains the following properties
		 *	success	- Boolean indicating if the call was successful or not
		 *	data - When success is true, contains a "group" Group instance
		 *		   When success is false, contains an "error" FlickrError instance
		 *
		 * @see #getInfo
		 * @see com.adobe.service.flickr.FlickrError
		 * @langversion ActionScript 3.0
		 * @playerversion Flash 8.5
		 * @tiptext
		 */
		[Event(name="groupsGetInfo", 
			 type="com.adobe.webapis.flickr.events.FlickrResultEvent")]
			 
		/**
		 * Broadcast as a result of the search method being called
		 *
		 * The event contains the following properties
		 *	success	- Boolean indicating if the call was successful or not
		 *	data - When success is true, contains a "groups" PagedGroupList instance
		 *		   When success is false, contains an "error" FlickrError instance
		 *
		 * @see #search
		 * @see com.adobe.service.flickr.FlickrError
		 * @langversion ActionScript 3.0
		 * @playerversion Flash 8.5
		 * @tiptext
		 */
		[Event(name="groupsSearch", 
			 type="com.adobe.webapis.flickr.events.FlickrResultEvent")]	
	
	/**
	 * Contains the methods for the Groups method group in the Flickr API.
	 * 
	 * Even though the events are listed here, they're really broadcast
	 * from the FlickrService instance itself to make using the service
	 * easier.
	 */
	public class Groups {
			 
		/** 
		 * A reference to the FlickrService that contains the api key
		 * and logic for processing API calls/responses
		 */
		private var _service:FlickrService;
		
		/**
		 * Private variable that we provide read-only access to
		 */
		private var _pools:Pools;
	
		/**
		 * Construct a new Blogs "method group" class
		 *
		 * @param service The FlickrService this method group
		 *		is associated with.
		 * @langversion ActionScript 3.0
		 * @playerversion Flash 8.5
		 * @tiptext
		 */
		public function Groups( service:FlickrService ) {
			_service = service;
			_pools = new Pools( service );
		}
		
		/**
		 * Provide read-only access to the Pools method group in the Flickr API
		 *
		 * @langversion ActionScript 3.0
		 * @playerversion Flash 8.5
		 * @tiptext
		 */
		public function get pools():Pools {
			return _pools;	
		}
	
		/**
		 * Browse the group category tree, finding groups and sub-categories.
		 *
		 * This method requires authentication with READ permission.
		 *
		 * @param cat_id (Optional) The category id to fetch a list of groups 
		 *			and sub-categories for. If not specified, it defaults 
		 *			to zero, the root of the category tree.
		 * @see http://www.flickr.com/services/api/flickr.groups.browse.html
		 * @langversion ActionScript 3.0
		 * @playerversion Flash 8.5
		 * @tiptext
		 */
		public function browse( cat_id:String = "0" ):void {
			// Let the Helper do the work to invoke the method			
			MethodGroupHelper.invokeMethod( _service, browse_result, 
								   "flickr.groups.browse", 
								   false,
								   new NameValuePair( "cat_id", cat_id ) );
		}
		
		/**
		 * Capture the result of the browse call, and dispatch
		 * the event to anyone listening.
		 *
		 * @param event The complete event generated by the URLLoader
		 * 			that was used to communicate with the Flickr API
		 *			from the invokeMethod method in 
		 */
		private function browse_result( event:Event ):void {
			// Create a GROUPS_BROWSE event
			var result:FlickrResultEvent = new FlickrResultEvent( FlickrResultEvent.GROUPS_BROWSE );

			// Have the Helper handle parsing the result from the server - get the data
			// from the URLLoader which correspondes to the result from the API call
			MethodGroupHelper.processAndDispatch( _service, 
												  URLLoader( event.target ).data, 
												  result,
												  "category",
												  MethodGroupHelper.parseGroupCategory );
		}
		
		/**
		 * Get information about a group.
		 *
		 * @param group_id The NSID of the group to fetch information for.
		 * @see http://www.flickr.com/services/api/flickr.groups.getInfo.html
		 * @langversion ActionScript 3.0
		 * @playerversion Flash 8.5
		 * @tiptext
		 */
		public function getInfo( group_id:String ):void {
			// Let the Helper do the work to invoke the method			
			MethodGroupHelper.invokeMethod( _service, getInfo_result, 
								   "flickr.groups.getInfo", 
								   false,
								   new NameValuePair( "group_id", group_id ) );
		}
		
		/**
		 * Capture the result of the getInfo call, and dispatch
		 * the event to anyone listening.
		 *
		 * @param event The complete event generated by the URLLoader
		 * 			that was used to communicate with the Flickr API
		 *			from the invokeMethod method in MethodGroupHelper
		 */
		private function getInfo_result( event:Event ):void {
			// Create a GROUPS_GET_INFO event
			var result:FlickrResultEvent = new FlickrResultEvent( FlickrResultEvent.GROUPS_GET_INFO );

			// Have the Helper handle parsing the result from the server - get the data
			// from the URLLoader which correspondes to the result from the API call
			MethodGroupHelper.processAndDispatch( _service, 
												  URLLoader( event.target ).data, 
												  result,
												  "group",
												  MethodGroupHelper.parseGroup );
		}
		
		/**
		 * Search for groups. 18+ groups will only be returned for authenticated 
		 * calls where the authenticated user is over 18.
		 *
		 * @param text The text to search for.
		 * @param per_page (Optional) Number of groups to return per page. If this
		 *			argument is ommited, it defaults to 100. The maximum allowed 
		 *			value is 500.
		 * @param page (Optional) The page of results to return. If this argument 
		 *			is ommited, it defaults to 1.
		 * @see http://www.flickr.com/services/api/flickr.blogs.postPhoto.html
		 * @langversion ActionScript 3.0
		 * @playerversion Flash 8.5
		 * @tiptext
		 */
		public function search( text:String, per_page:Number = 100, page:Number = 1 ):void {
			// Let the Helper do the work to invoke the method			
			MethodGroupHelper.invokeMethod( _service, search_result, 
								   "flickr.groups.search", 
								   false,
								   new NameValuePair( "text", text ),
								   new NameValuePair( "per_page", per_page.toString() ),
								   new NameValuePair( "page", page.toString() ) );
		}
		
		/**
		 * Capture the result of the search call, and dispatch
		 * the event to anyone listening.
		 *
		 * @param event The complete event generated by the URLLoader
		 * 			that was used to communicate with the Flickr API
		 *			from the invokeMethod method in MethodGroupHelper
		 */
		private function search_result( event:Event ):void {
			// Create a GROUPS_SEARCH event
			var result:FlickrResultEvent = new FlickrResultEvent( FlickrResultEvent.GROUPS_SEARCH );

			// Have the Helper handle parsing the result from the server - get the data
			// from the URLLoader which correspondes to the result from the API call
			MethodGroupHelper.processAndDispatch( _service, 
												  URLLoader( event.target ).data, 
												  result,
												  "groups",
												  MethodGroupHelper.parsePagedGroupList );
		}
		
	}	
	
}