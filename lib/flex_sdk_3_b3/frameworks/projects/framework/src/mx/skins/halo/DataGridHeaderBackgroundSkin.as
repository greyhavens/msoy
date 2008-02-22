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
package mx.skins.halo
{
    import flash.display.GradientType;
    import flash.display.Graphics;
    import flash.geom.Matrix;
    import mx.styles.StyleManager;
    import mx.skins.ProgrammaticSkin;
    
    /**
     *  The skin for the background of the column headers in a DataGrid control.
     *
     *  @see mx.controls.DataGrid
     */
    public class DataGridHeaderBackgroundSkin extends ProgrammaticSkin
    {
            include "../../core/Version.as";

        //--------------------------------------------------------------------------
        //
        //  Constructor
        //
        //--------------------------------------------------------------------------
    
        /**
         *  Constructor.
         */
        public function DataGridHeaderBackgroundSkin()
        {
            super();
        }
        
        //--------------------------------------------------------------------------
        //
        //  Overridden methods
        //
        //--------------------------------------------------------------------------
    
        /**
         *  @private
         */
        override protected function updateDisplayList(w:Number, h:Number):void
        {
            var g:Graphics = graphics;
            g.clear();
            
            var colors:Array = getStyle("headerColors");
            StyleManager.getColorNames(colors);
            
            var matrix:Matrix = new Matrix();
            matrix.createGradientBox(w, h + 1, Math.PI/2, 0, 0);
    
            colors = [ colors[0], colors[0], colors[1] ];
            var ratios:Array = [ 0, 60, 255 ];
            var alphas:Array = [ 1.0, 1.0, 1.0 ];
    
            g.beginGradientFill(GradientType.LINEAR, colors, alphas, ratios, matrix);
            g.lineStyle(0, 0x000000, 0);
            g.moveTo(0, 0);
            g.lineTo(w, 0);
            g.lineTo(w, h - 0.5);
            g.lineStyle(0, getStyle("borderColor"), 100);
            g.lineTo(0, h - 0.5);
            g.lineStyle(0, 0x000000, 0);
            g.endFill();
        }
        
    }
}