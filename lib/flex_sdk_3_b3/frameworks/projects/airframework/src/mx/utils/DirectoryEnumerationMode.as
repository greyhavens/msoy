////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2007 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.utils
{

[ExcludeClass]

/**
 *  @private
 *
 *  The DirectoryEnumerationMode class defines the constant values
 *  for the <code>enumerationMode</code> property
 *  of the DirectoryEnumeration class.
 *
 *  @see mx.utils.DirectorEnumeration#enumerationMode
 */
public final class DirectoryEnumerationMode
{
    include "../core/Version.as";

    //--------------------------------------------------------------------------
    //
    //  Class constants
    //
    //--------------------------------------------------------------------------

    /**
     *  Specifies to show files but not directories.
     */
    public static const FILES_ONLY:String = "filesOnly";
    
    /**
     *  Specifies to show directories but not files.
     */
    public static const DIRECTORIES_ONLY:String = "directoriesOnly";
    
    /**
     *  Specifies to show files first, then directories.
     */
    public static const FILES_FIRST:String = "filesFirst";
    
    /**
     *  Specifies to show directories, then files.
     */
    public static const DIRECTORIES_FIRST:String = "directoriesFirst";
    
    /**
     *  Specifies to show both files and directories, mixed together.
     */
     public static const FILES_AND_DIRECTORIES:String = "filesAndDirectories";
}

}
