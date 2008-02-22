/*************************************************************************
 * 
 * ADOBE CONFIDENTIAL
 * __________________
 * 
 *  [2002] - [2007] Adobe Systems Incorporated 
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of Adobe Systems Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Adobe Systems Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe Systems Incorporated.
 */
package
{

/**
 *  @private
 *  This class is used to link additional classes into airframework.swc
 *  beyond those that are found by dependecy analysis starting
 *  from the classes specified in manifest.xml.
 */
internal class AIRFrameworkClasses
{
	import mx.managers.NativeDragManagerImpl; NativeDragManagerImpl;
	import mx.skins.halo.StatusBarBackgroundSkin; StatusBarBackgroundSkin;
	import mx.skins.halo.WindowBackground; WindowBackground
	import mx.skins.halo.ApplicationTitleBarBackgroundSkin; ApplicationTitleBarBackgroundSkin;
	import mx.skins.halo.WindowCloseButtonSkin; WindowCloseButtonSkin;
	import mx.skins.halo.WindowMinimizeButtonSkin; WindowMinimizeButtonSkin;
	import mx.skins.halo.WindowMaximizeButtonSkin; WindowMaximizeButtonSkin;
	import mx.skins.halo.WindowRestoreButtonSkin; WindowRestoreButtonSkin;
}

}
