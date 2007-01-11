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
		 * Broadcast as a result of the getListPhoto method being called
		 *
		 * The event contains the following properties
		 *	success	- Boolean indicating if the call was successful or not
		 *	data - When success is true, a Photo instance
		 *		   When success is false, contains an "error" FlickrError instance
		 *
		 * @see #getListPhoto
		 * @see com.adobe.service.flickr.FlickrError
		 * @langversion ActionScript 3.0
		 * @playerversion Flash 8.5
		 * @tiptext
		 */
		[Event(name="tagsGetListPhoto", 
			 type="com.adobe.webapis.flickr.events.FlickrResultEvent")]
		
		/**
		 * Broadcast as a result of the getListUser method being called
		 *
		 * The event contains the following properties
		 *	success	- Boolean indicating if the call was successful or not
		 *	data - When success is true, a User instance
		 *		   When success is false, contains an "error" FlickrError instance
		 *
		 * @see #getListUser
		 * @see com.adobe.service.flickr.FlickrError
		 * @langversion ActionScript 3.0
		 * @playerversion Flash 8.5
		 * @tiptext
		 */
		[Event(name="tagsGetListUser", 
			 type="com.adobe.webapis.flickr.events.FlickrResultEvent")]
			 
		/**
		 * Broadcast as a result of the getListUserPopular method being called
		 *
		 * The event contains the following properties
		 *	success	- Boolean indicating if the call was successful or not
		 *	data - When success is true, a User instance
		 *		   When success is false, contains an "error" FlickrError instance
		 *
		 * @see #getListUserPopular
		 * @see com.adobe.service.flickr.FlickrError
		 * @langversion ActionScript 3.0
		 * @playerversion Flash 8.5
		 * @tiptext
		 */
		[Event(name="tagsGetListUserPopular", 
			 type="com.adobe.webapis.flickr.events.FlickrResultEvent")]
			 
		/**
		 * Broadcast as a result of the getRelated method being called
		 *
		 * The event contains the following properties
		 *	success	- Boolean indicating if the call was successful or not
		 *	data - When success is true, an Array of PhotoTag instances
		 *		   When success is false, contains an "error" FlickrError instance
		 *
		 * @see #getRelated
		 * @see com.adobe.service.flickr.FlickrError
		 * @langversion ActionScript 3.0
		 * @playerversion Flash 8.5
		 * @tiptext
		 */
		[Event(name="tagsGetRelated", 
			 type="com.adobe.webapis.flickr.events.FlickrResultEvent")]
	
	/**
	 * Contains the methods for the Tags method group in the Flickr API.
	 * 
	 * Even though the events are listed here, they're really broadcast
	 * from the FlickrService instance itself to make using the service
	 * easier.
	 */
	public class Tags {
	
		/** 
		 * A reference to the FlickrService that contains the api key
		 * and logic for processing API calls/responses
		 */
		private var _service:FlickrService;
	
		/**
		 * Construct a new Notes "method group" class
		 *
		 * @param service The FlickrService this method group
		 *		is associated with.
		 * @langversion ActionScript 3.0
		 * @playerversion Flash 8.5
		 * @tiptext
		 */
		public function Tags( service:FlickrService ) {
			_service = service;
		}
	
		/**
		 * Get the tag list for a given photo.
		 * 
		 * @param photo_id The id of the photo to return tags for.
		 * @see http://www.flickr.com/services/api/flickr.urls.getListPhoto.html
		 * @langversion ActionScript 3.0
		 * @playerversion Flash 8.5
		 * @tiptext
		 */
		public function getListPhoto( photo_id:String ):void {
			// Let the Helper do the work to invoke the method			
			MethodGroupHelper.invokeMethod( _service, getListPhoto_result, 
								   "flickr.tags.getListPhoto", 
								   false,
								   new NameValuePair( "photo_id", photo_id ) );
		}
		
		/**
		 * Capture the result of the getListPhoto call, and dispatch
		 * the event to anyone listening.
		 *
		 * @param event The complete event generated by the URLLoader
		 * 			that was used to communicate with the Flickr API
		 *			from the invokeMethod method in MethodGroupHelper
		 */
		private function getListPhoto_result( event:Event ):void {
			// Create a TAGS_GET_LIST_PHOTO event
			var result:FlickrResultEvent = new FlickrResultEvent( FlickrResultEvent.TAGS_GET_LIST_PHOTO );

			// Have the Helper handle parsing the result from the server - get the data
			// from the URLLoader which correspondes to the result from the API call
			MethodGroupHelper.processAndDispatch( _service, 
												  URLLoader( event.target ).data, 
												  result,
												  "photo",
												  MethodGroupHelper.parsePhoto );
		}
		
		/**
		 * Get the tag list for a given user (or the currently logged in user).
		 *
		 * @param user_id (Optional) The NSID of the user to fetch the tag list for. 
		 *			If this argument is not specified, the currently logged in 
		 *			user (if any) is assumed.
		 * @see http://www.flickr.com/services/api/flickr.urls.getListUser.html
		 * @langversion ActionScript 3.0
		 * @playerversion Flash 8.5
		 * @tiptext
		 */
		public function getListUser( user_id:String = "" ):void {
			// Let the Helper do the work to invoke the method			
			MethodGroupHelper.invokeMethod( _service, getListUser_result, 
								   "flickr.tags.getListUser", 
								   false,
								   new NameValuePair( "user_id", user_id ) );
		}
		
		/**
		 * Capture the result of the getListUser call, and dispatch
		 * the event to anyone listening.
		 *
		 * @param event The complete event generated by the URLLoader
		 * 			that was used to communicate with the Flickr API
		 *			from the invokeMethod method in MethodGroupHelper
		 */
		private function getListUser_result( event:Event ):void {
			// Create a TAGS_GET_LIST_USER event
			var result:FlickrResultEvent = new FlickrResultEvent( FlickrResultEvent.TAGS_GET_LIST_USER );

			// Have the Helper handle parsing the result from the server - get the data
			// from the URLLoader which correspondes to the result from the API call
			MethodGroupHelper.processAndDispatch( _service, 
												  URLLoader( event.target ).data,
												  result,
												  "user",
												  MethodGroupHelper.parseUserTags );
		}
		
		/**
		 * Get the popular tags for a given user (or the currently logged in user).
		 *
		 * @param user_id (Optional) The NSID of the user to fetch the tag list for. 
		 *			If this argument is not specified, the currently logged in user 
		 *			(if any) is assumed.
		 * @see http://www.flickr.com/services/api/flickr.urls.getListUserPopular.html
		 * @langversion ActionScript 3.0
		 * @playerversion Flash 8.5
		 * @tiptext
		 */
		public function getListUserPopular( user_id:String = "" ):void {
			// Let the Helper do the work to invoke the method			
			MethodGroupHelper.invokeMethod( _service, getListUserPopular_result, 
								   "flickr.tags.getListUserPopular", 
								   false,
								   new NameValuePair( "user_id", user_id ) );
		}
		
		/**
		 * Capture the result of the getListUserPopular call, and dispatch
		 * the event to anyone listening.
		 *
		 * @param event The complete event generated by the URLLoader
		 * 			that was used to communicate with the Flickr API
		 *			from the invokeMethod method in MethodGroupHelper
		 */
		private function getListUserPopular_result( event:Event ):void {
			// Create a TAGS_GET_LIST_USER_POPULAR event
			var result:FlickrResultEvent = new FlickrResultEvent( FlickrResultEvent.TAGS_GET_LIST_USER_POPULAR );

			// Have the Helper handle parsing the result from the server - get the data
			// from the URLLoader which correspondes to the result from the API call
			MethodGroupHelper.processAndDispatch( _service, 
												  URLLoader( event.target ).data, 
												  result,
												  "user",
												  MethodGroupHelper.parseUserTags );
		}
		
		/**
		 * Returns a list of tags 'related' to the given tag, based on 
		 * clustered usage analysis.
		 *
		 * @param tag The tag to fetch related tags for.
		 * @see http://www.flickr.com/services/api/flickr.urls.getRelated.html
		 * @langversion ActionScript 3.0
		 * @playerversion Flash 8.5
		 * @tiptext
		 */
		public function getRelated( tag:String ):void {
			// Let the Helper do the work to invoke the method			
			MethodGroupHelper.invokeMethod( _service, getRelated_result, 
								   "flickr.tags.getRelated", 
								   false,
								   new NameValuePair( "tag", tag ) );
		}
		
		/**
		 * Capture the result of the getRelated call, and dispatch
		 * the event to anyone listening.
		 *
		 * @param event The complete event generated by the URLLoader
		 * 			that was used to communicate with the Flickr API
		 *			from the invokeMethod method in MethodGroupHelper
		 */
		private function getRelated_result( event:Event ):void {
			// Create a TAGS_GET_RELATED event
			var result:FlickrResultEvent = new FlickrResultEvent( FlickrResultEvent.TAGS_GET_RELATED );

			// Have the Helper handle parsing the result from the server - get the data
			// from the URLLoader which correspondes to the result from the API call
			MethodGroupHelper.processAndDispatch( _service, 
												  URLLoader( event.target ).data, 
												  result,
												  "tags",
												  MethodGroupHelper.parseTagList );
		}
		
	}	
	
}