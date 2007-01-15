package
{
    import flash.display.Bitmap;
    import flash.display.BitmapData;
    
    public class CellMatrix extends Bitmap
    {
        public var matrixWidth :int;
        public var matrixHeight :int;
        public var src :Array;
        public var dst :Array;
        public var light :Array;
        
        public function CellMatrix (matrixWidth :int, matrixHeight :int)
        {
            super(new BitmapData(matrixWidth, matrixHeight));
            this.matrixWidth = matrixWidth;
            this.matrixHeight = matrixHeight;
            src = new Array(matrixWidth * matrixHeight);
            dst = new Array(matrixWidth * matrixHeight);
            light = new Array(matrixWidth * matrixHeight);
            for (var i :int = 0; i < src.length; i ++) {
                src[i] = new Cell();
                dst[i] = new Cell();
                light[i] = new Color(0, 0, 0);
            }
        }

        public function setLight (x :int, y :int, c :Color) :void
        {
            light[x + matrixWidth*y].setColor(c);
        }

        public function transformMatrix () :void
        {
            trace("In transform (" + matrixWidth + ", " + matrixHeight + ")...");
            for (var x :int = 0; x < matrixWidth; x ++) {
                for (var y :int = 0; y < matrixHeight; y ++) {
                    var i :int = x + matrixWidth*y;
                    transformCell(light[i], x, y, dst[i]);
//                    bitmapData.setPixel(x, y, (dst[i] as Cell).strength * 255);
                }
            }
            // swap src and dst
            var tmp :Array = src;
            src = dst;
            dst = tmp;
        }

        /**
         * Input for CA algorithm:
         *  - light reaching cell
         *  - current configuration
         *  - immediate neighbour configurations
         *  - larger neighbourhood demographics?
         * 
         * Cell configuration, then:
         *  - hue
         *  - strength
         */
        public function transformCell (c :Color, x :int, y :int, outCell :Cell) :void
        {
            // if we have immediate neighbours, let them spread by blending, for now
            var neighbors :Array = getNeighbors(x, y);
            outCell.strength = 0;
            outCell.hue = 0;
            var maxStrength :Number = 0;
            var s2sum :Number = 0;
            for (var i :int = 0; i < neighbors.length; i ++) {
                var neighbor :Cell = neighbors[i] as Cell;
                outCell.strength += neighbor.strength;
                var s2 :Number = neighbor.strength * neighbor.strength;
                s2sum += s2;
                outCell.hue += s2 * neighbor.hue;
            }
            var lightLevel :Number = c.getHSV().val;
            if (outCell.strength > 0) {
                outCell.hue /= s2sum;        // weighted average
                outCell.strength = outCell.strength * 2 / neighbors.length / (1 + 3*lightLevel);
            } else if (Math.random() < 1/2000) {
                // on empty spots, there is a random chance of spontaneous growth
                outCell.strength = .3;
                outCell.hue = Math.random() * 360;
            }
            outCell.strength = Math.min(outCell.strength, 1);
        }

        public function getNeighbors (x :int, y :int) :Array
        {
          var result :Array = new Array();
            result.push(getCell(x, y));
            if (y > 0) {
                result.push(getCell(x, y-1));
                if (x > 0) {
                    result.push(getCell(x-1, y-1));
                }
                if (x < matrixWidth-1) {
                    result.push(getCell(x+1, y-1));
                }
            }
            if (x > 0) {
                result.push(getCell(x-1, y));
            }
            if (x < matrixWidth-1) {
                result.push(getCell(x+1, y));
            }
            if (y < matrixHeight-1) {
                result.push(getCell(x, y+1));
                if (x > 0) {
                    result.push(getCell(x-1, y+1));
                }
                if (x < matrixWidth-1) {
                    result.push(getCell(x+1, y+1));
                }
            }
            return result;
        }

        public function getCell (x :int, y :int) :Cell
        {
            if (x < 0 || x > matrixWidth || y < 0 || y > matrixWidth) trace("bad getCell(" + x + ", " + y + ")");
            return src[x + y*matrixWidth];
        }
    }
}