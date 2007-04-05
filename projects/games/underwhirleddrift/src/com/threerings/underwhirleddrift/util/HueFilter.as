package com.threerings.underwhirleddrift.util {

import flash.filters.ColorMatrixFilter

/**
 * Most of this came from http://www.kirupa.com/forum/showthread.php?t=230706
 */
public class HueFilter
{
    public static function getFilter (hue :int) :ColorMatrixFilter
    {
        var M1 :Array = [ 0.213, 0.715, 0.072,
                          0.213, 0.715, 0.072,
                          0.213, 0.715, 0.072 ];
        var M2 :Array = [ 0.787, -0.715, -0.072,
                         -0.212, 0.285, -0.072,
                         -0.213, -0.715, 0.928 ];
        var M3 :Array = [-0.213, -0.715, 0.928,
                          0.143, 0.140, -0.283,
                         -0.787, 0.715, 0.072 ];
        var M4 :Array = add(M1, add(multiply(Math.cos(hue * Math.PI / 180), M2), 
            multiply(Math.sin(hue * Math.PI / 180), M3)));

        return new ColorMatrixFilter(concat(identity(), [ M4[0], M4[1], M4[2], 0, 0,
                                                          M4[3], M4[4], M4[5], 0, 0,
                                                          M4[6], M4[7], M4[8], 0, 0,
                                                              0,     0,     0, 1, 0 ]));
    }

    protected static function identity () :Array
    {
        return [ 1, 0, 0, 0, 0,
                 0, 1, 0, 0, 0,
                 0, 0, 1, 0, 0,
                 0, 0, 0, 1, 0 ];
    }

    protected static function add (A :Array, B :Array) :Array
    {
        var C :Array = [];
        for(var ii :int = 0; ii < A.length; ii++)
        {
            C.push( A[ii] + B[ii] );
        }
        return C;
    }
    
    protected static function multiply(x :Number, B :Array) :Array
    {
        var A :Array = [];
        for each (var n :Number in B)
        {
            A.push(x * n);
        }
        return A;
    }
    
    protected static function concat(A :Array, B :Array) :Array
    {
        var nM :Array = [];
        
        nM[0] = (A[0] * B[0]) + (A[1] * B[5]) + (A[2] * B[10]) + (A[3] * B[15]);
        nM[1] = (A[0] * B[1]) + (A[1] * B[6]) + (A[2] * B[11]) + (A[3] * B[16]);
        nM[2] = (A[0] * B[2]) + (A[1] * B[7]) + (A[2] * B[12]) + (A[3] * B[17]);
        nM[3] = (A[0] * B[3]) + (A[1] * B[8]) + (A[2] * B[13]) + (A[3] * B[18]);
        nM[4] = (A[0] * B[4]) + (A[1] * B[9]) + (A[2] * B[14]) + (A[3] * B[19]) + A[4];
        
        nM[5] = (A[5] * B[0]) + (A[6] * B[5]) + (A[7] * B[10]) + (A[8] * B[15]);
        nM[6] = (A[5] * B[1]) + (A[6] * B[6]) + (A[7] * B[11]) + (A[8] * B[16]);
        nM[7] = (A[5] * B[2]) + (A[6] * B[7]) + (A[7] * B[12]) + (A[8] * B[17]);
        nM[8] = (A[5] * B[3]) + (A[6] * B[8]) + (A[7] * B[13]) + (A[8] * B[18]);
        nM[9] = (A[5] * B[4]) + (A[6] * B[9]) + (A[7] * B[14]) + (A[8] * B[19]) + A[9];
        
        nM[10] = (A[10] * B[0]) + (A[11] * B[5]) + (A[12] * B[10]) + (A[13] * B[15]);
        nM[11] = (A[10] * B[1]) + (A[11] * B[6]) + (A[12] * B[11]) + (A[13] * B[16]);
        nM[12] = (A[10] * B[2]) + (A[11] * B[7]) + (A[12] * B[12]) + (A[13] * B[17]);
        nM[13] = (A[10] * B[3]) + (A[11] * B[8]) + (A[12] * B[13]) + (A[13] * B[18]);
        nM[14] = (A[10] * B[4]) + (A[11] * B[9]) + (A[12] * B[14]) + (A[13] * B[19]) + A[14];
        
        nM[15] = (A[15] * B[0]) + (A[16] * B[5]) + (A[17] * B[10]) + (A[18] * B[15]);
        nM[16] = (A[15] * B[1]) + (A[16] * B[6]) + (A[17] * B[11]) + (A[18] * B[16]);
        nM[17] = (A[15] * B[2]) + (A[16] * B[7]) + (A[17] * B[12]) + (A[18] * B[17]);
        nM[18] = (A[15] * B[3]) + (A[16] * B[8]) + (A[17] * B[13]) + (A[18] * B[18]);
        nM[19] = (A[15] * B[4]) + (A[16] * B[9]) + (A[17] * B[14]) + (A[18] * B[19]) + A[19];

        return nM;
    }
}
}
