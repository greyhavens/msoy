/*
 * JSwiff is an open source Java API for Macromedia Flash file generation
 * and manipulation
 *
 * Copyright (C) 2004-2006 Ralf Terdic (contact@jswiff.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.jswiff.investigator;

import com.jswiff.swfrecords.*;
import com.jswiff.swfrecords.actions.*;
import com.jswiff.swfrecords.tags.*;
import com.jswiff.swfrecords.tags.ExportAssets.ExportMapping;
import com.jswiff.swfrecords.tags.ImportAssets.ImportMapping;
import com.jswiff.util.HexUtils;

import java.util.Iterator;
import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;


/*
 * This class is used to build the model for the SWF tree representation.
 */
final class SWFTreeBuilder {
  private static int nodes;
  private static List constants;

  static void setNodes(int nodes) {
    SWFTreeBuilder.nodes = nodes;
  }

  static int getNodes() {
    return nodes;
  }

  static void addNode(DefaultMutableTreeNode node, Tag tag) {
    switch (tag.getCode()) {
      case TagConstants.DEFINE_BITS:
        addNode(node, (DefineBits) tag);
        break;
      case TagConstants.DEFINE_BITS_JPEG_2:
        addNode(node, (DefineBitsJPEG2) tag);
        break;
      case TagConstants.DEFINE_BITS_JPEG_3:
        addNode(node, (DefineBitsJPEG3) tag);
        break;
      case TagConstants.DEFINE_BITS_LOSSLESS:
        addNode(node, (DefineBitsLossless) tag);
        break;
      case TagConstants.DEFINE_BITS_LOSSLESS_2:
        addNode(node, (DefineBitsLossless2) tag);
        break;
      case TagConstants.DEFINE_BUTTON:
        addNode(node, (DefineButton) tag);
        break;
      case TagConstants.DEFINE_BUTTON_2:
        addNode(node, (DefineButton2) tag);
        break;
      case TagConstants.DEFINE_BUTTON_C_XFORM:
        addNode(node, (DefineButtonCXform) tag);
        break;
      case TagConstants.DEFINE_BUTTON_SOUND:
        addNode(node, (DefineButtonSound) tag);
        break;
      case TagConstants.DEFINE_EDIT_TEXT:
        addNode(node, (DefineEditText) tag);
        break;
      case TagConstants.DEFINE_FONT:
        addNode(node, (DefineFont) tag);
        break;
      case TagConstants.DEFINE_FONT_2:
        addNode(node, (DefineFont2) tag);
        break;
      case TagConstants.DEFINE_FONT_3:
        addNode(node, (DefineFont3) tag);
        break;
      case TagConstants.DEFINE_FONT_INFO:
        addNode(node, (DefineFontInfo) tag);
        break;
      case TagConstants.DEFINE_FONT_INFO_2:
        addNode(node, (DefineFontInfo2) tag);
        break;
      case TagConstants.FLASHTYPE_SETTINGS:
        addNode(node, (FlashTypeSettings) tag);
        break;
      case TagConstants.DEFINE_FONT_ALIGNMENT:
        addNode(node, (DefineFontAlignment) tag);
        break;
      case TagConstants.DEFINE_MORPH_SHAPE:
        addNode(node, (DefineMorphShape) tag);
        break;
      case TagConstants.DEFINE_MORPH_SHAPE_2:
        addNode(node, (DefineMorphShape2) tag);
        break;
      case TagConstants.DEFINE_SHAPE:
        addNode(node, (DefineShape) tag);
        break;
      case TagConstants.DEFINE_SHAPE_2:
        addNode(node, (DefineShape2) tag);
        break;
      case TagConstants.DEFINE_SHAPE_3:
        addNode(node, (DefineShape3) tag);
        break;
      case TagConstants.DEFINE_SHAPE_4:
        addNode(node, (DefineShape4) tag);
        break;
      case TagConstants.DEFINE_SOUND:
        addNode(node, (DefineSound) tag);
        break;
      case TagConstants.DEFINE_SPRITE:
        addNode(node, (DefineSprite) tag);
        break;
      case TagConstants.DEFINE_TEXT:
        addNode(node, (DefineText) tag);
        break;
      case TagConstants.DEFINE_TEXT_2:
        addNode(node, (DefineText2) tag);
        break;
      case TagConstants.DEFINE_VIDEO_STREAM:
        addNode(node, (DefineVideoStream) tag);
        break;
      case TagConstants.DO_ACTION:
        addNode(node, (DoAction) tag);
        break;
      case TagConstants.DO_INIT_ACTION:
        addNode(node, (DoInitAction) tag);
        break;
      case TagConstants.ENABLE_DEBUGGER_2:
        addNode(node, (EnableDebugger2) tag);
        break;
      case TagConstants.ENABLE_DEBUGGER:
        addNode(node, (EnableDebugger) tag);
        break;
      case TagConstants.EXPORT_ASSETS:
        addNode(node, (ExportAssets) tag);
        break;
      case TagConstants.FILE_ATTRIBUTES:
        addNode(node, (FileAttributes) tag);
        break;
      case TagConstants.FRAME_LABEL:
        addNode(node, (FrameLabel) tag);
        break;
      case TagConstants.IMPORT_ASSETS:
      case TagConstants.IMPORT_ASSETS_2:
        addNode(node, (ImportAssets) tag);
        break;
      case TagConstants.JPEG_TABLES:
        addNode(node, (JPEGTables) tag);
        break;
      case TagConstants.METADATA:
        addNode(node, (Metadata) tag);
        break;
      case TagConstants.PLACE_OBJECT:
        addNode(node, (PlaceObject) tag);
        break;
      case TagConstants.PLACE_OBJECT_2:
        addNode(node, (PlaceObject2) tag);
        break;
      case TagConstants.PLACE_OBJECT_3:
        addNode(node, (PlaceObject3) tag);
        break;
      case TagConstants.PROTECT:
        addNode(node, (Protect) tag);
        break;
      case TagConstants.REMOVE_OBJECT:
        addNode(node, (RemoveObject) tag);
        break;
      case TagConstants.REMOVE_OBJECT_2:
        addNode(node, (RemoveObject2) tag);
        break;
      case TagConstants.SCRIPT_LIMITS:
        addNode(node, (ScriptLimits) tag);
        break;
      case TagConstants.SET_BACKGROUND_COLOR:
        addNode(node, (SetBackgroundColor) tag);
        break;
      case TagConstants.SET_TAB_INDEX:
        addNode(node, (SetTabIndex) tag);
        break;
      case TagConstants.SHOW_FRAME:
        addNode(node, (ShowFrame) tag);
        break;
      case TagConstants.SCALE_9_GRID:
        addNode(node, (Scale9Grid) tag);
        break;
      case TagConstants.SOUND_STREAM_BLOCK:
        addNode(node, (SoundStreamBlock) tag);
        break;
      case TagConstants.SOUND_STREAM_HEAD:
        addNode(node, (SoundStreamHead) tag);
        break;
      case TagConstants.SOUND_STREAM_HEAD_2:
        addNode(node, (SoundStreamHead2) tag);
        break;
      case TagConstants.START_SOUND:
        addNode(node, (StartSound) tag);
        break;
      case TagConstants.VIDEO_FRAME:
        addNode(node, (VideoFrame) tag);
        break;
      case TagConstants.MALFORMED:
        addNode(node, (MalformedTag) tag);
        break;
      default:
        addNode(node, (UnknownTag) tag);
    }
  }

  static void addNode(DefaultMutableTreeNode node, SWFHeader header) {
    DefaultMutableTreeNode headerNode = addParentNode(
        node, "<html><id:1xf><font color=\"#00A000\">SWF Header</html>");
    addLeaf(headerNode, "compressed: " + header.isCompressed());
    addLeaf(headerNode, "version: " + header.getVersion());
    addLeaf(headerNode, "fileLength: " + header.getFileLength());
    addLeaf(headerNode, "frameSize: " + header.getFrameSize());
    addLeaf(headerNode, "frameRate: " + header.getFrameRate());
    addLeaf(headerNode, "frameCount: " + header.getFrameCount());
    // addLeaf(node, " ");
  }

  private static String getCapStyleString(byte capStyle) {
    switch (capStyle) {
      case LineStyle2.CAPS_NONE:
        return "none";
      case LineStyle2.CAPS_ROUND:
        return "round";
      case LineStyle2.CAPS_SQUARE:
        return "square";
      default:
        return "illegal value: " + capStyle;
    }
  }

  private static String getGridFitString(byte gridFit) {
    switch (gridFit) {
      case FlashTypeSettings.GRID_FIT_NONE:
        return "none";
      case FlashTypeSettings.GRID_FIT_PIXEL:
        return "pixel";
      case FlashTypeSettings.GRID_FIT_SUBPIXEL:
        return "subpixel";
      default:
        return "unknown value: " + gridFit;
    }
  }

  private static String getInterpolationMethodString(byte interpolationMethod) {
    switch (interpolationMethod) {
      case Gradient.INTERPOLATION_RGB:
        return "RGB";
      case Gradient.INTERPOLATION_LINEAR_RGB:
        return "linear RGB";
      default:
        return "unkown value: " + interpolationMethod;
    }
  }

  private static String getJointStyleString(byte jointStyle) {
    switch (jointStyle) {
      case LineStyle2.JOINT_BEVEL:
        return "bevel";
      case LineStyle2.JOINT_MITER:
        return "miter";
      case LineStyle2.JOINT_ROUND:
        return "round";
      default:
        return "illegal value: " + jointStyle;
    }
  }

  private static String getPushDescription(Push push) {
    String result = "Push";
    for (Iterator i = push.getValues().iterator(); i.hasNext();) {
      Push.StackValue value = (Push.StackValue) i.next();
      switch (value.getType()) {
        case Push.StackValue.TYPE_STRING:
          result += (" string: '" + value.getString() + "'");
          break;
        case Push.StackValue.TYPE_FLOAT:
          result += (" float: " + value.getFloat());
          break;
        case Push.StackValue.TYPE_REGISTER:
          result += (" register: " + value.getRegisterNumber());
          break;
        case Push.StackValue.TYPE_BOOLEAN:
          result += (" boolean: " + value.getBoolean());
          break;
        case Push.StackValue.TYPE_DOUBLE:
          result += (" double: " + value.getDouble());
          break;
        case Push.StackValue.TYPE_INTEGER:
          result += (" integer: " + value.getInteger());
          break;
        case Push.StackValue.TYPE_CONSTANT_8:
          int index8 = value.getConstant8();
          result += (" c8[" + index8 + "]: '" + constants.get(index8) + "'");
          break;
        case Push.StackValue.TYPE_CONSTANT_16:
          int index16 = value.getConstant16();
          result += (" c8[" + index16 + "]: '" + constants.get(index16) + "'");
          break;
        case Push.StackValue.TYPE_UNDEFINED:
          result += " <b>undefined</b>";
          break;
        case Push.StackValue.TYPE_NULL:
          result += " <b>null</b>";
          break;
      }
      result += ";";
    }
    return result;
  }

  private static String getScaleStrokeString(byte scaleStroke) {
    switch (scaleStroke) {
      case LineStyle2.SCALE_NONE:
        return "none";
      case LineStyle2.SCALE_HORIZONTAL:
        return "horizontal";
      case LineStyle2.SCALE_VERTICAL:
        return "vertical";
      case LineStyle2.SCALE_BOTH:
        return "both";
      default:
        return "illegal value: " + scaleStroke;
    }
  }

  private static String getSoundFormatString(byte format) {
    String result = null;
    switch (format) {
      case SoundStreamHead2.FORMAT_ADPCM:
        result = "ADPCM";
        break;
      case SoundStreamHead2.FORMAT_MP3:
        result = "mp3";
        break;
      case SoundStreamHead2.FORMAT_NELLYMOSER:
        result = "Nellymoser";
        break;
      case SoundStreamHead2.FORMAT_UNCOMPRESSED:
        result = "uncompressed";
        break;
      case SoundStreamHead2.FORMAT_UNCOMPRESSED_LITTLE_ENDIAN:
        result = "uncompresed little-endian";
        break;
      default:
        result = "unknown";
    }
    return result;
  }

  private static String getSoundRateString(byte rate) {
    String result = null;
    switch (rate) {
      case SoundStreamHead.RATE_5500_HZ:
        result = "5.5 kHz";
        break;
      case SoundStreamHead.RATE_11000_HZ:
        result = "11 kHz";
        break;
      case SoundStreamHead.RATE_22000_HZ:
        result = "22 kHz";
        break;
      case SoundStreamHead.RATE_44000_HZ:
        result = "44 kHz";
        break;
      default:
        result = "unknown";
    }
    return result;
  }

  private static String getSpreadMethodString(byte spreadMethod) {
    switch (spreadMethod) {
      case Gradient.SPREAD_PAD:
        return "pad";
      case Gradient.SPREAD_REFLECT:
        return "reflect";
      case Gradient.SPREAD_REPEAT:
        return "repeat";
      default:
        return "unknown value: " + spreadMethod;
    }
  }

  private static String getThicknessString(byte thickness) {
    switch (thickness) {
      case DefineFontAlignment.THIN:
        return "thin";
      case DefineFontAlignment.MEDIUM:
        return "medium";
      case DefineFontAlignment.THICK:
        return "thick";
      default:
        return "unknown value: " + thickness;
    }
  }

  private static void addLeaf(DefaultMutableTreeNode node, String string) {
    node.insert(new DefaultMutableTreeNode(string), node.getChildCount());
    nodes++;
  }

  private static void addNode(DefaultMutableTreeNode node, MalformedTag tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node,
        "<html><body bgcolor=\"#FF0000\"><font color=\"#FFFFFF\">Malformed tag</font></body></html>");
    short code                     = tag.getTagHeader().getCode();
    addLeaf(
      tagNode, "code: " + code + " (" + TagConstants.getTagName(code) + ")");
    addLeaf(tagNode, "data size: " + tag.getTagHeader().getLength() + " bytes");
    addLeaf(tagNode, "error: " + tag.getException().getMessage());
  }

  private static void addNode(DefaultMutableTreeNode node, DefineBits tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatDefTag("DefineBits"));
    addLeaf(tagNode, "characterId: " + tag.getCharacterId());
    addLeaf(tagNode, "jpegData: " + " byte[" + tag.getJpegData().length + "]");
  }

  private static void addNode(DefaultMutableTreeNode node, DefineBitsJPEG2 tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatDefTag("DefineBitsJPEG2"));
    addLeaf(tagNode, "characterId: " + tag.getCharacterId());
    addLeaf(tagNode, "jpegData: " + " byte[" + tag.getJpegData().length + "]");
  }

  private static void addNode(DefaultMutableTreeNode node, DefineBitsJPEG3 tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatDefTag("DefineBitsJPEG3"));
    addLeaf(tagNode, "characterId: " + tag.getCharacterId());
    addLeaf(tagNode, "jpegData: " + " byte[" + tag.getJpegData().length + "]");
    addLeaf(
      tagNode,
      "bitmapAlphaData: " + " byte[" + tag.getBitmapAlphaData().length + "]");
  }

  private static void addNode(
    DefaultMutableTreeNode node, DefineBitsLossless tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatDefTag("DefineBitsLossless"));
    addLeaf(tagNode, "characterId: " + tag.getCharacterId());
    short format        = tag.getFormat();
    String formatString = "";
    switch (format) {
      case DefineBitsLossless.FORMAT_8_BIT_COLORMAPPED:
        formatString = "8-bit colormapped image";
        break;
      case DefineBitsLossless.FORMAT_15_BIT_RGB:
        formatString = "15-bit RGB image";
        break;
      case DefineBitsLossless.FORMAT_24_BIT_RGB:
        formatString = "24-bit RGB image";
        break;
    }
    addLeaf(tagNode, "format: " + formatString);
    addLeaf(tagNode, "width: " + tag.getWidth());
    addLeaf(tagNode, "height: " + tag.getHeight());
    if (format == DefineBitsLossless.FORMAT_8_BIT_COLORMAPPED) {
      addNode(
        tagNode, "zlibBitmapData: ", (ColorMapData) tag.getZlibBitmapData());
    } else {
      addNode(
        tagNode, "zlibBitmapData: ", (BitmapData) tag.getZlibBitmapData(),
        format);
    }
  }

  private static void addNode(
    DefaultMutableTreeNode node, String var, BitmapData data, short format) {
    DefaultMutableTreeNode newNode = addParentNode(node, var + "BitmapData");
    if (format == DefineBitsLossless.FORMAT_15_BIT_RGB) {
      addLeaf(
        newNode,
        "bitmapPixelData: Pix15[" + data.getBitmapPixelData().length + "]");
    } else {
      addLeaf(
        newNode,
        "bitmapPixelData: Pix24[" + data.getBitmapPixelData().length + "]");
    }
  }

  private static void addNode(
    DefaultMutableTreeNode node, String var, ColorMapData data) {
    DefaultMutableTreeNode newNode        = addParentNode(
        node, var + "ColorMapData");
    RGB[] colorTable                      = data.getColorTableRGB();
    DefaultMutableTreeNode colorTableNode = addParentNode(
        newNode, "colorTableRGB: RGB[" + colorTable.length + "]");
    for (int i = 0; i < colorTable.length; i++) {
      addLeaf(colorTableNode, colorTable[i].toString());
    }
    addLeaf(
      newNode,
      "colorMapPixelData: short[" + data.getColorMapPixelData().length + "]");
  }

  private static void addNode(
    DefaultMutableTreeNode node, String var, AlphaColorMapData data) {
    DefaultMutableTreeNode newNode        = addParentNode(
        node, var + "AlphaColorMapData");
    RGBA[] colorTable                     = data.getColorTableRGBA();
    DefaultMutableTreeNode colorTableNode = addParentNode(
        newNode, "colorTableRGBA: RGBA[" + colorTable.length + "]");
    for (int i = 0; i < colorTable.length; i++) {
      addLeaf(colorTableNode, i + ": " + colorTable[i].toString());
    }
    addLeaf(
      newNode,
      "colorMapPixelData: short[" + data.getColorMapPixelData().length + "]");
  }

  private static void addNode(
    DefaultMutableTreeNode node, DefineBitsLossless2 tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatDefTag("DefineBitsLossless2"));
    addLeaf(tagNode, "characterId: " + tag.getCharacterId());
    short format        = tag.getFormat();
    String formatString = "";
    switch (format) {
      case DefineBitsLossless2.FORMAT_8_BIT_COLORMAPPED:
        formatString = "8-bit colormapped image";
        break;
      case DefineBitsLossless2.FORMAT_32_BIT_RGBA:
        formatString = "32-bit RGBA image";
        break;
    }
    addLeaf(tagNode, "format: " + formatString);
    addLeaf(tagNode, "width: " + tag.getWidth());
    addLeaf(tagNode, "height: " + tag.getHeight());
    if (format == DefineBitsLossless.FORMAT_8_BIT_COLORMAPPED) {
      addNode(
        tagNode, "zlibBitmapData: ", (AlphaColorMapData) tag.getZlibBitmapData());
    } else {
      DefaultMutableTreeNode zlibNode = addParentNode(
          tagNode, "zlibBitmapData: AlphaBitMapData");
      addLeaf(
        zlibNode,
        "bitmapPixelData: RGBA[" +
        ((AlphaBitmapData) (tag.getZlibBitmapData())).getBitmapPixelData().length +
        "]");
    }
  }

  private static void addNode(DefaultMutableTreeNode node, DefineButton tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatDefTag("DefineButton"));
    addLeaf(tagNode, "characterId: " + tag.getCharacterId());
    addNode(tagNode, "characters: ", tag.getCharacters());
    addNode(tagNode, "actions: ", tag.getActions());
  }

  private static void addNode(DefaultMutableTreeNode node, DefineButton2 tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatDefTag("DefineButton2"));
    addLeaf(tagNode, "characterId: " + tag.getCharacterId());
    addLeaf(tagNode, "trackAsMenu: " + tag.isTrackAsMenu());
    addNode(tagNode, "characters: ", tag.getCharacters());
    if (tag.getActions() != null) {
      addNode(tagNode, "actions: ", tag.getActions());
    }
  }

  private static void addNode(
    DefaultMutableTreeNode node, String var, ButtonCondAction[] actions) {
    DefaultMutableTreeNode newNode = addParentNode(
        node, var + "ButtonCondAction[" + actions.length + "]");
    for (int i = 0; i < actions.length; i++) {
      DefaultMutableTreeNode actionNode = addParentNode(
          newNode, "ButtonCondAction");
      addLeaf(actionNode, "idleToOverDown: " + actions[i].isIdleToOverDown());
      addLeaf(actionNode, "outDownToIdle: " + actions[i].isOutDownToIdle());
      addLeaf(
        actionNode, "outDownToOverDown: " + actions[i].isOutDownToOverDown());
      addLeaf(
        actionNode, "overDownToOutDown: " + actions[i].isOverDownToOutDown());
      addLeaf(
        actionNode, "overDownToOverUp: " + actions[i].isOverDownToOverUp());
      addLeaf(
        actionNode, "overUpToOverDown: " + actions[i].isOverUpToOverDown());
      addLeaf(actionNode, "overUpToIdle: " + actions[i].isOverUpToIdle());
      addLeaf(actionNode, "idleToOverUp: " + actions[i].isIdleToOverUp());
      addLeaf(actionNode, "keyPress: " + actions[i].getKeyPress());
      addLeaf(actionNode, "overDownToIdle: " + actions[i].isOverDownToIdle());
      addNode(actionNode, "actions: ", actions[i].getActions());
    }
  }

  private static void addNode(
    DefaultMutableTreeNode node, String var, ButtonRecord[] characters) {
    DefaultMutableTreeNode newNode = addParentNode(
        node, var + "ButtonRecord[" + characters.length + "]");
    for (int i = 0; i < characters.length; i++) {
      DefaultMutableTreeNode recordNode = addParentNode(
          newNode, "ButtonRecord");
      addLeaf(recordNode, "hitState: " + characters[i].isHitState());
      addLeaf(recordNode, "downState: " + characters[i].isDownState());
      addLeaf(recordNode, "overState: " + characters[i].isOverState());
      addLeaf(recordNode, "upState: " + characters[i].isUpState());
      addLeaf(recordNode, "characterId: " + characters[i].getCharacterId());
      addLeaf(recordNode, "placeDepth: " + characters[i].getPlaceDepth());
      addLeaf(recordNode, "placeMatrix: " + characters[i].getPlaceMatrix());
      CXformWithAlpha colorTransform = characters[i].getColorTransform();
      if (colorTransform != null) {
        addNode(
          recordNode, "colorTransform: ", characters[i].getColorTransform());
      }
    }
  }

  private static void addNode(
    DefaultMutableTreeNode node, DefineButtonCXform tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatDefTag("DefineButtonCXform"));
    addLeaf(tagNode, "characterId: " + tag.getCharacterId());
    addNode(tagNode, "buttonColorTransform: ", tag.getColorTransform());
  }

  private static void addNode(
    DefaultMutableTreeNode node, DefineButtonSound tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatDefTag("DefineButtonSound"));
    addLeaf(tagNode, "buttonId: " + tag.getButtonId());
    if (tag.getOverUpToIdleSoundId() != 0) {
      addLeaf(tagNode, "overUpToIdleSoundId: " + tag.getOverUpToIdleSoundId());
      addNode(
        tagNode, "overUpToIdleSoundInfo: ", tag.getOverUpToIdleSoundInfo());
    }
    if (tag.getIdleToOverUpSoundId() != 0) {
      addLeaf(tagNode, "idleToOverUpSoundId: " + tag.getIdleToOverUpSoundId());
      addNode(
        tagNode, "idleToOverUpSoundInfo: ", tag.getIdleToOverUpSoundInfo());
    }
    if (tag.getOverUpToOverDownSoundId() != 0) {
      addLeaf(
        tagNode, "overUpToOverDownSoundId: " +
        tag.getOverUpToOverDownSoundId());
      addNode(
        tagNode, "overUpToOverDownSoundInfo: ",
        tag.getOverUpToOverDownSoundInfo());
    }
    if (tag.getOverDownToOverUpSoundId() != 0) {
      addLeaf(
        tagNode, "overDownToOverUpSoundId: " +
        tag.getOverDownToOverUpSoundId());
      addNode(
        tagNode, "overDownToOverUpSoundInfo: ",
        tag.getOverDownToOverUpSoundInfo());
    }
  }

  private static void addNode(DefaultMutableTreeNode node, DefineEditText tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatDefTag("DefineEditText"));
    addLeaf(tagNode, "characterId: " + tag.getCharacterId());
    addLeaf(tagNode, "bounds: " + tag.getBounds());
    addLeaf(tagNode, "wordWrap: " + tag.isWordWrap());
    addLeaf(tagNode, "multiline: " + tag.isMultiline());
    addLeaf(tagNode, "password: " + tag.isPassword());
    addLeaf(tagNode, "readOnly: " + tag.isReadOnly());
    addLeaf(tagNode, "autoSize: " + tag.isAutoSize());
    addLeaf(tagNode, "noSelect: " + tag.isNoSelect());
    addLeaf(tagNode, "border: " + tag.isBorder());
    addLeaf(tagNode, "html: " + tag.isHtml());
    addLeaf(tagNode, "useOutlines: " + tag.isUseOutlines());
    if (tag.getFontId() > 0) {
      addLeaf(tagNode, "fontId: " + tag.getFontId());
      addLeaf(tagNode, "fontHeight: " + tag.getFontHeight());
    }
    if (tag.getTextColor() != null) {
      addLeaf(tagNode, "textColor: " + tag.getTextColor());
    }
    if (tag.getMaxLength() > 0) {
      addLeaf(tagNode, "maxLength: " + tag.getMaxLength());
    }
    if (tag.hasLayout()) {
      addLeaf(tagNode, "align: " + tag.getAlign());
      addLeaf(tagNode, "leftMargin: " + tag.getLeftMargin());
      addLeaf(tagNode, "rightMargin: " + tag.getRightMargin());
      addLeaf(tagNode, "indent: " + tag.getIndent());
      addLeaf(tagNode, "leading: " + tag.getLeading());
    }
    addLeaf(tagNode, "variableName: " + tag.getVariableName());
    if (tag.getInitialText() != null) {
      addLeaf(tagNode, "initialText: " + tag.getInitialText());
    }
  }

  private static void addNode(DefaultMutableTreeNode node, DefineFont tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatDefTag("DefineFont"));
    addLeaf(tagNode, "characterId: " + tag.getCharacterId());
    Shape[] shapes                   = tag.getGlyphShapeTable();
    DefaultMutableTreeNode shapeNode = addParentNode(
        tagNode, "glyphShapeTable: Shape[" + shapes.length + "]");
    for (int i = 0; i < shapes.length; i++) {
      addNode(shapeNode, "", shapes[i]);
    }
  }

  private static void addNode(DefaultMutableTreeNode node, DefineFont2 tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatDefTag("DefineFont2"));
    addLeaf(tagNode, "characterId: " + tag.getCharacterId());
    addLeaf(tagNode, "shiftJIS: " + tag.isShiftJIS());
    addLeaf(tagNode, "smallText: " + tag.isSmallText());
    addLeaf(tagNode, "ansi: " + tag.isANSI());
    addLeaf(tagNode, "italic: " + tag.isItalic());
    addLeaf(tagNode, "bold: " + tag.isBold());
    addLeaf(tagNode, "LanguageCode: " + tag.getLanguageCode());
    addLeaf(tagNode, "fontName: " + tag.getFontName());
    addLeaf(tagNode, "hasLayout: " + tag.hasLayout());
    Shape[] shapes = tag.getGlyphShapeTable();
    if (shapes == null) {
      addLeaf(tagNode, "numGlyphs: 0");
    } else {
      addLeaf(tagNode, "numGlyphs: " + shapes.length);
    }
    if (shapes != null) {
      DefaultMutableTreeNode shapeNode = addParentNode(
          tagNode, "glyphShapeTable: Shape[" + shapes.length + "]");
      for (int i = 0; i < shapes.length; i++) {
        addNode(shapeNode, "", shapes[i]);
      }
      char[] table                         = tag.getCodeTable();
      DefaultMutableTreeNode codeTableNode = addParentNode(
          tagNode, "codeTable: char[" + table.length + "]");
      for (int i = 0; i < table.length; i++) {
        addLeaf(codeTableNode, "code " + i + ": " + table[i]);
      }
    }
    if (tag.hasLayout()) {
      addLeaf(tagNode, "ascent: " + tag.getAscent());
      addLeaf(tagNode, "descent: " + tag.getDescent());
      addLeaf(tagNode, "leading: " + tag.getLeading());
      addLeaf(tagNode, "fontAscent: " + tag.getAscent());
      if (shapes != null) {
        short[] advanceTable                    = tag.getAdvanceTable();
        DefaultMutableTreeNode advanceTableNode = addParentNode(
            tagNode, "advanceTable: short[" + advanceTable.length + "]");
        for (int i = 0; i < advanceTable.length; i++) {
          addLeaf(advanceTableNode, i + ": " + advanceTable[i]);
        }
        Rect[] boundsTable                     = tag.getBoundsTable();
        DefaultMutableTreeNode boundsTableNode = addParentNode(
            tagNode, "boundsTable: Rect[" + boundsTable.length + "]");
        for (int i = 0; i < boundsTable.length; i++) {
          addLeaf(boundsTableNode, i + ": " + boundsTable[i]);
        }
      }
      KerningRecord[] kerningTable = tag.getKerningTable();
      if ((kerningTable != null) && (kerningTable.length > 0)) {
        DefaultMutableTreeNode kerningTableNode = addParentNode(
            tagNode, "kerningTable: KerningRecord[" + kerningTable.length +
            "]");
        for (int i = 0; i < kerningTable.length; i++) {
          KerningRecord kerningRecord              = kerningTable[i];
          DefaultMutableTreeNode kerningRecordNode = addParentNode(
              kerningTableNode, "KerningRecord");
          addLeaf(kerningRecordNode, "left: " + kerningRecord.getLeft());
          addLeaf(kerningRecordNode, "right: " + kerningRecord.getRight());
          addLeaf(
            kerningRecordNode, "adjustment: " + kerningRecord.getAdjustment());
        }
      }
    }
  }

  private static void addNode(DefaultMutableTreeNode node, DefineFont3 tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatDefTag("DefineFont3"));
    addLeaf(tagNode, "characterId: " + tag.getCharacterId());
    addLeaf(tagNode, "smallText: " + tag.isSmallText());
    addLeaf(tagNode, "italic: " + tag.isItalic());
    addLeaf(tagNode, "bold: " + tag.isBold());
    addLeaf(tagNode, "LanguageCode: " + tag.getLanguageCode());
    addLeaf(tagNode, "fontName: " + tag.getFontName());
    addLeaf(tagNode, "hasLayout: " + tag.hasLayout());
    Shape[] shapes = tag.getGlyphShapeTable();
    if (shapes == null) {
      addLeaf(tagNode, "numGlyphs: 0");
    } else {
      addLeaf(tagNode, "numGlyphs: " + shapes.length);
    }
    if (shapes != null) {
      DefaultMutableTreeNode shapeNode = addParentNode(
          tagNode, "glyphShapeTable: Shape[" + shapes.length + "]");
      for (int i = 0; i < shapes.length; i++) {
        addNode(shapeNode, "", shapes[i]);
      }
      char[] table                         = tag.getCodeTable();
      DefaultMutableTreeNode codeTableNode = addParentNode(
          tagNode, "codeTable: char[" + table.length + "]");
      for (int i = 0; i < table.length; i++) {
        addLeaf(codeTableNode, "code " + i + ": " + table[i]);
      }
    }
    if (tag.hasLayout()) {
      addLeaf(tagNode, "ascent: " + tag.getAscent());
      addLeaf(tagNode, "descent: " + tag.getDescent());
      addLeaf(tagNode, "leading: " + tag.getLeading());
      addLeaf(tagNode, "fontAscent: " + tag.getAscent());
      if (shapes != null) {
        short[] advanceTable                    = tag.getAdvanceTable();
        DefaultMutableTreeNode advanceTableNode = addParentNode(
            tagNode, "advanceTable: short[" + advanceTable.length + "]");
        for (int i = 0; i < advanceTable.length; i++) {
          addLeaf(advanceTableNode, i + ": " + advanceTable[i]);
        }
        Rect[] boundsTable                     = tag.getBoundsTable();
        DefaultMutableTreeNode boundsTableNode = addParentNode(
            tagNode, "boundsTable: Rect[" + boundsTable.length + "]");
        for (int i = 0; i < boundsTable.length; i++) {
          addLeaf(boundsTableNode, i + ": " + boundsTable[i]);
        }
      }
      KerningRecord[] kerningTable = tag.getKerningTable();
      if ((kerningTable != null) && (kerningTable.length > 0)) {
        DefaultMutableTreeNode kerningTableNode = addParentNode(
            tagNode, "kerningTable: KerningRecord[" + kerningTable.length +
            "]");
        for (int i = 0; i < kerningTable.length; i++) {
          KerningRecord kerningRecord              = kerningTable[i];
          DefaultMutableTreeNode kerningRecordNode = addParentNode(
              kerningTableNode, "KerningRecord");
          addLeaf(kerningRecordNode, "left: " + kerningRecord.getLeft());
          addLeaf(kerningRecordNode, "right: " + kerningRecord.getRight());
          addLeaf(
            kerningRecordNode, "adjustment: " + kerningRecord.getAdjustment());
        }
      }
    }
  }

  private static void addNode(DefaultMutableTreeNode node, DefineFontInfo tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatDefTag("DefineFontInfo"));
    addLeaf(tagNode, "fontId: " + tag.getFontId());
    addLeaf(tagNode, "fontName: " + tag.getFontName());
    addLeaf(tagNode, "smallText: " + tag.isSmallText());
    addLeaf(tagNode, "shiftJIS: " + tag.isShiftJIS());
    addLeaf(tagNode, "ansi: " + tag.isANSI());
    addLeaf(tagNode, "italic: " + tag.isItalic());
    addLeaf(tagNode, "bold: " + tag.isBold());
    char[] table                         = tag.getCodeTable();
    DefaultMutableTreeNode codeTableNode = addParentNode(
        tagNode, "codeTable: char[" + table.length + "]");
    for (int i = 0; i < table.length; i++) {
      addLeaf(codeTableNode, "code " + i + ": " + table[i]);
    }
  }

  private static void addNode(DefaultMutableTreeNode node, DefineFontInfo2 tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatDefTag("DefineFontInfo2"));
    addLeaf(tagNode, "fontId: " + tag.getFontId());
    addLeaf(tagNode, "fontName: " + tag.getFontName());
    addLeaf(tagNode, "smallText: " + tag.isSmallText());
    addLeaf(tagNode, "italic: " + tag.isItalic());
    addLeaf(tagNode, "bold: " + tag.isBold());
    addLeaf(tagNode, "langCode: " + tag.getLangCode());
    char[] table                         = tag.getCodeTable();
    DefaultMutableTreeNode codeTableNode = addParentNode(
        tagNode, "codeTable: char[" + table.length + "]");
    for (int i = 0; i < table.length; i++) {
      addLeaf(codeTableNode, "code " + i + ": " + table[i]);
    }
  }

  private static void addNode(
    DefaultMutableTreeNode node, FlashTypeSettings tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatDefTag("FlashTypeSettings"));
    addLeaf(tagNode, "textId: " + tag.getTextId());
    addLeaf(tagNode, "flashType: " + tag.isFlashType());
    addLeaf(tagNode, "gridFit: " + getGridFitString(tag.getGridFit()));
    addLeaf(tagNode, "thickness: " + tag.getThickness());
    addLeaf(tagNode, "sharpness: " + tag.getSharpness());
  }

  private static void addNode(
    DefaultMutableTreeNode node, DefineFontAlignment tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatDefTag("DefineFontAlignment"));
    addLeaf(tagNode, "fontId: " + tag.getFontId());
    addLeaf(tagNode, "thickness: " + getThicknessString(tag.getThickness()));
    AlignmentZone[] alignmentZones = tag.getAlignmentZones();
    addNode(tagNode, alignmentZones);
  }

  private static void addNode(
    DefaultMutableTreeNode node, AlignmentZone[] alignmentZones) {
    DefaultMutableTreeNode newNode = addParentNode(
        node, "alignmentZones: AlignmentZone[" + alignmentZones.length + "]");
    for (int i = 0; i < alignmentZones.length; i++) {
      AlignmentZone zone              = alignmentZones[i];
      DefaultMutableTreeNode zoneNode = addParentNode(
          newNode, "AlignmentZone " + i);
      if (zone.hasX()) {
        addLeaf(
          zoneNode, "x: left=" + zone.getLeft() + " width=" + zone.getWidth());
      }
      if (zone.hasY()) {
        addLeaf(
          zoneNode,
          "y: baseline=" + zone.getBaseline() + " height=" + zone.getHeight());
      }
    }
  }

  private static void addNode(
    DefaultMutableTreeNode node, DefineMorphShape tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatDefTag("DefineMorphShape"));
    addLeaf(tagNode, "characterId: " + tag.getCharacterId());
    addLeaf(tagNode, "startBounds: " + tag.getStartBounds());
    addLeaf(tagNode, "endBounds: " + tag.getEndBounds());
    MorphFillStyles morphFillStyles = tag.getMorphFillStyles();
    if (morphFillStyles != null) {
      addNode(tagNode, "morphFillStyles: ", morphFillStyles);
    }
    MorphLineStyles morphLineStyles = tag.getMorphLineStyles();
    if (morphLineStyles != null) {
      addNode(tagNode, "morphLineStyles: ", morphLineStyles);
    }
    Shape startEdges = tag.getStartShape();
    if (startEdges != null) {
      addNode(tagNode, "startEdges: ", startEdges);
    }
    Shape endEdges = tag.getEndShape();
    if (endEdges != null) {
      addNode(tagNode, "endEdges: ", endEdges);
    }
  }

  private static void addNode(
    DefaultMutableTreeNode node, DefineMorphShape2 tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatDefTag("DefineMorphShape2"));
    addLeaf(tagNode, "characterId: " + tag.getCharacterId());
    addLeaf(tagNode, "startBounds: " + tag.getStartShapeBounds());
    addLeaf(tagNode, "endBounds: " + tag.getEndShapeBounds());
    addLeaf(tagNode, "startEdgeBounds: " + tag.getStartEdgeBounds());
    addLeaf(tagNode, "endEdgeBounds: " + tag.getEndEdgeBounds());
    MorphFillStyles morphFillStyles = tag.getMorphFillStyles();
    if (morphFillStyles != null) {
      addNode(tagNode, "morphFillStyles: ", morphFillStyles);
    }
    MorphLineStyles morphLineStyles = tag.getMorphLineStyles();
    if (morphLineStyles != null) {
      addNode(tagNode, "morphLineStyles: ", morphLineStyles);
    }
    Shape startShape = tag.getStartShape();
    if (startShape != null) {
      addNode(tagNode, "startShape: ", startShape);
    }
    Shape endShape = tag.getEndShape();
    if (endShape != null) {
      addNode(tagNode, "endShape: ", endShape);
    }
  }

  private static void addNode(
    DefaultMutableTreeNode node, String var, Shape shape) {
    DefaultMutableTreeNode newNode = addParentNode(node, var + "Shape");
    ShapeRecord[] shapeRecords     = shape.getShapeRecords();
    if (shapeRecords.length > 0) {
      DefaultMutableTreeNode recordsNode = addParentNode(
          newNode, "shapeRecords: ShapeRecord[" + shapeRecords.length + "]");
      for (int i = 0; i < shapeRecords.length; i++) {
        addNode(recordsNode, shapeRecords[i]);
      }
    }
  }

  private static void addNode(
    DefaultMutableTreeNode node, String var, MorphLineStyles morphLineStyles) {
    DefaultMutableTreeNode newNode = addParentNode(
        node, var + "MorphLineStyles (" + morphLineStyles.getSize() +
        " styles)");
    for (int i = 1; i <= morphLineStyles.getSize(); i++) {
      DefaultMutableTreeNode styleNode = addParentNode(
          newNode, "MorphLineStyle " + i);
      Object style                     = morphLineStyles.getStyle(i);
      if (style instanceof MorphLineStyle) {
        addNode(styleNode, (MorphLineStyle) style);
      } else {
        addNode(styleNode, (MorphLineStyle2) style);
      }
    }
  }

  private static void addNode(
    DefaultMutableTreeNode styleNode, MorphLineStyle style) {
    addLeaf(styleNode, "startWidth: " + style.getStartWidth());
    addLeaf(styleNode, "endWidth: " + style.getEndWidth());
    addLeaf(styleNode, "startColor: " + style.getStartColor());
    addLeaf(styleNode, "endColor: " + style.getEndColor());
  }

  private static void addNode(
    DefaultMutableTreeNode styleNode, MorphLineStyle2 style) {
    addLeaf(styleNode, "startWidth: " + style.getStartWidth());
    addLeaf(styleNode, "endWidth: " + style.getEndWidth());
    addLeaf(
      styleNode, "startCapStyle: " +
      getCapStyleString(style.getStartCapStyle()));
    addLeaf(
      styleNode, "endCapStyle: " + getCapStyleString(style.getEndCapStyle()));
    byte jointStyle = style.getJointStyle();
    addLeaf(styleNode, "jointStyle: " + getJointStyleString(jointStyle));
    if (jointStyle == EnhancedStrokeStyle.JOINT_MITER) {
      addLeaf(styleNode, "miterLimit: " + style.getMiterLimit());
    }
    addLeaf(styleNode, "pixelHinting: " + style.isPixelHinting());
    addLeaf(styleNode, "close: " + style.isClose());
    addLeaf(
      styleNode, "scaleStroke: " +
      getScaleStrokeString(style.getScaleStroke()));
    MorphFillStyle fillStyle = style.getFillStyle();
    if (fillStyle == null) {
      addLeaf(styleNode, "startColor: " + style.getStartColor());
      addLeaf(styleNode, "endColor: " + style.getEndColor());
    } else {
      addNode(styleNode, fillStyle, 0);
    }
  }

  private static void addNode(
    DefaultMutableTreeNode node, String var, MorphFillStyles morphFillStyles) {
    DefaultMutableTreeNode newNode = addParentNode(
        node, var + "MorphFillStyles (" + morphFillStyles.getSize() +
        " styles)");
    for (int i = 1; i <= morphFillStyles.getSize(); i++) {
      addNode(newNode, morphFillStyles.getStyle(i), i);
    }
  }

  private static void addNode(
    DefaultMutableTreeNode node, MorphFillStyle fillStyle, int index) {
    DefaultMutableTreeNode newNode = addParentNode(
        node, "MorphFillStyle " + index);
    short type                     = fillStyle.getType();
    switch (type) {
      case MorphFillStyle.TYPE_SOLID:
        addLeaf(newNode, "type: solid");
        addLeaf(newNode, "startColor: " + fillStyle.getStartColor());
        addLeaf(newNode, "endColor: " + fillStyle.getEndColor());
        break;
      case MorphFillStyle.TYPE_LINEAR_GRADIENT:
        addLeaf(newNode, "type: linear gradient");
        addLeaf(
          newNode, "startGradientMatrix: " +
          fillStyle.getStartGradientMatrix());
        addLeaf(
          newNode, "endGradientMatrix: " + fillStyle.getStartGradientMatrix());
        addNode(newNode, fillStyle.getGradient());
        break;
      case MorphFillStyle.TYPE_RADIAL_GRADIENT:
        addLeaf(newNode, "type: radial gradient");
        addLeaf(
          newNode, "startGradientMatrix: " +
          fillStyle.getStartGradientMatrix());
        addLeaf(
          newNode, "endGradientMatrix: " + fillStyle.getStartGradientMatrix());
        addNode(newNode, fillStyle.getGradient());
        break;
      case MorphFillStyle.TYPE_FOCAL_RADIAL_GRADIENT:
        addLeaf(newNode, "type: focal radial gradient");
        addLeaf(
          newNode, "startGradientMatrix: " +
          fillStyle.getStartGradientMatrix());
        addLeaf(
          newNode, "endGradientMatrix: " + fillStyle.getStartGradientMatrix());
        addNode(newNode, fillStyle.getGradient());
        break;
      case MorphFillStyle.TYPE_TILED_BITMAP:
        addLeaf(newNode, "type: repeating bitmap");
        addLeaf(newNode, "bitmapId: " + fillStyle.getBitmapId());
        addLeaf(
          newNode, "startBitmapMatrix: " + fillStyle.getStartBitmapMatrix());
        addLeaf(newNode, "endBitmapMatrix: " + fillStyle.getEndBitmapMatrix());
        break;
      case MorphFillStyle.TYPE_CLIPPED_BITMAP:
        addLeaf(newNode, "type: clipped bitmap");
        addLeaf(newNode, "bitmapId: " + fillStyle.getBitmapId());
        addLeaf(
          newNode, "startBitmapMatrix: " + fillStyle.getStartBitmapMatrix());
        addLeaf(newNode, "endBitmapMatrix: " + fillStyle.getEndBitmapMatrix());
        break;
      case MorphFillStyle.TYPE_NONSMOOTHED_TILED_BITMAP:
        addLeaf(newNode, "type: non-smoothed repeating bitmap");
        addLeaf(newNode, "bitmapId: " + fillStyle.getBitmapId());
        addLeaf(
          newNode, "startBitmapMatrix: " + fillStyle.getStartBitmapMatrix());
        addLeaf(newNode, "endBitmapMatrix: " + fillStyle.getEndBitmapMatrix());
        break;
      case MorphFillStyle.TYPE_NONSMOOTHED_CLIPPED_BITMAP:
        addLeaf(newNode, "type: non-smoothed clipped bitmap");
        addLeaf(newNode, "bitmapId: " + fillStyle.getBitmapId());
        addLeaf(
          newNode, "startBitmapMatrix: " + fillStyle.getStartBitmapMatrix());
        addLeaf(newNode, "endBitmapMatrix: " + fillStyle.getEndBitmapMatrix());
        break;
      default:
        addLeaf(newNode, "unknown type: " + type);
    }
  }

  private static void addNode(
    DefaultMutableTreeNode node, MorphGradient gradient) {
    DefaultMutableTreeNode morphGradNode = addParentNode(
        node,
        ((gradient instanceof FocalMorphGradient) ? "FocalMorphGradient"
                                                  : "MorphGradient"));
    addLeaf(
      morphGradNode,
      "spreadMethod: " + getSpreadMethodString(gradient.getSpreadMethod()));
    addLeaf(
      morphGradNode,
      "interpolationMethod: " +
      getInterpolationMethodString(gradient.getInterpolationMethod()));
    if (gradient instanceof FocalMorphGradient) {
      FocalMorphGradient focalMorphGradient = (FocalMorphGradient) gradient;
      addLeaf(
        morphGradNode,
        "startFocalPointRatio: " +
        (focalMorphGradient).getStartFocalPointRatio());
      addLeaf(
        morphGradNode,
        "endFocalPointRatio: " + (focalMorphGradient).getEndFocalPointRatio());
    }
    MorphGradRecord[] records = gradient.getGradientRecords();
    for (int i = 0; i < records.length; i++) {
      DefaultMutableTreeNode recordNode = addParentNode(
          morphGradNode, "MorphGradRecord");
      addLeaf(recordNode, "startRatio: " + records[i].getStartRatio());
      addLeaf(recordNode, "startColor: " + records[i].getStartColor());
      addLeaf(recordNode, "endRatio: " + records[i].getEndRatio());
      addLeaf(recordNode, "endColor: " + records[i].getEndColor());
    }
  }

  private static void addNode(DefaultMutableTreeNode node, DefineShape tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatDefTag("DefineShape"));
    addLeaf(tagNode, "characterID: " + tag.getCharacterId());
    addLeaf(tagNode, "shapeBounds: " + tag.getShapeBounds());
    addNode(tagNode, "shapes: ", tag.getShapes());
  }

  private static void addNode(DefaultMutableTreeNode node, DefineShape2 tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatDefTag("DefineShape2"));
    addLeaf(tagNode, "characterID: " + tag.getCharacterId());
    addLeaf(tagNode, "shapeBounds: " + tag.getShapeBounds());
    addNode(tagNode, "shapes: ", tag.getShapes());
  }

  private static void addNode(DefaultMutableTreeNode node, DefineShape3 tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatDefTag("DefineShape3"));
    addLeaf(tagNode, "characterID: " + tag.getCharacterId());
    addLeaf(tagNode, "shapeBounds: " + tag.getShapeBounds());
    addNode(tagNode, "shapes: ", tag.getShapes());
  }

  private static void addNode(DefaultMutableTreeNode node, DefineShape4 tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatDefTag("DefineShape4"));
    addLeaf(tagNode, "characterID: " + tag.getCharacterId());
    addLeaf(tagNode, "shapeBounds: " + tag.getShapeBounds());
    addLeaf(tagNode, "edgeBounds: " + tag.getEdgeBounds());
    addNode(tagNode, "shapes: ", tag.getShapes());
  }

  private static void addNode(
    DefaultMutableTreeNode node, String var, ShapeWithStyle shapeWithStyle) {
    DefaultMutableTreeNode newNode = addParentNode(
        node, var + "ShapeWithStyle");
    addNode(newNode, "fillStyles: ", shapeWithStyle.getFillStyles());
    addNode(newNode, "lineStyles: ", shapeWithStyle.getLineStyles());
    ShapeRecord[] shapeRecords = shapeWithStyle.getShapeRecords();
    if (shapeRecords.length > 0) {
      DefaultMutableTreeNode recordsNode = addParentNode(
          newNode, "shapeRecords: ShapeRecord[" + shapeRecords.length + "]");
      for (int i = 0; i < shapeRecords.length; i++) {
        addNode(recordsNode, shapeRecords[i]);
      }
    }
  }

  private static void addNode(DefaultMutableTreeNode node, ShapeRecord record) {
    if (record instanceof StyleChangeRecord) {
      addNode(node, (StyleChangeRecord) record);
    } else if (record instanceof StraightEdgeRecord) {
      addNode(node, (StraightEdgeRecord) record);
    } else {
      addNode(node, (CurvedEdgeRecord) record);
    }
  }

  private static void addNode(
    DefaultMutableTreeNode node, StraightEdgeRecord record) {
    DefaultMutableTreeNode newNode = addParentNode(node, "StraightEdgeRecord");
    addLeaf(newNode, "deltaX: " + record.getDeltaX());
    addLeaf(newNode, "deltaY: " + record.getDeltaY());
  }

  private static void addNode(
    DefaultMutableTreeNode node, CurvedEdgeRecord record) {
    DefaultMutableTreeNode newNode = addParentNode(node, "CurvedEdgeRecord");
    addLeaf(newNode, "controlDeltaX: " + record.getControlDeltaX());
    addLeaf(newNode, "controlDeltaX: " + record.getControlDeltaY());
    addLeaf(newNode, "anchorDeltaX: " + record.getAnchorDeltaX());
    addLeaf(newNode, "anchorDeltaY: " + record.getAnchorDeltaY());
  }

  private static void addNode(
    DefaultMutableTreeNode node, StyleChangeRecord record) {
    DefaultMutableTreeNode newNode = addParentNode(node, "StyleChangeRecord");
    if (record.hasMoveTo()) {
      addLeaf(newNode, "moveToX: " + record.getMoveToX());
      addLeaf(newNode, "moveToY: " + record.getMoveToY());
    }
    if (record.hasFillStyle0()) {
      addLeaf(newNode, "fillStyle0: " + record.getFillStyle0());
    }
    if (record.hasFillStyle1()) {
      addLeaf(newNode, "fillStyle1: " + record.getFillStyle1());
    }
    if (record.hasLineStyle()) {
      addLeaf(newNode, "lineStyle: " + record.getLineStyle());
    }
    if (record.hasNewStyles()) {
      DefaultMutableTreeNode newStylesNode = addParentNode(
          newNode, "NewStyles");
      addNode(newStylesNode, "newFillStyles: ", record.getNewFillStyles());
      addNode(newStylesNode, "newLineStyles: ", record.getNewLineStyles());
    }
  }

  private static void addNode(
    DefaultMutableTreeNode node, String var, FillStyleArray fillStyleArray) {
    DefaultMutableTreeNode newNode = addParentNode(
        node, var + "FillStyleArray (" + fillStyleArray.getSize() + " styles)");
    for (int i = 1; i <= fillStyleArray.getSize(); i++) {
      addNode(newNode, fillStyleArray.getStyle(i), i);
    }
  }

  private static void addNode(
    DefaultMutableTreeNode node, FillStyle fillStyle, int index) {
    DefaultMutableTreeNode newNode;
    if (index > 0) {
      newNode = addParentNode(node, "FillStyle " + index);
    } else {
      newNode = addParentNode(node, "FillStyle");
    }
    switch (fillStyle.getType()) {
      case FillStyle.TYPE_SOLID:
        addLeaf(newNode, "type: solid");
        addLeaf(newNode, "color: " + fillStyle.getColor());
        break;
      case FillStyle.TYPE_LINEAR_GRADIENT:
        addLeaf(newNode, "type: linear gradient");
        addLeaf(newNode, "gradientMatrix: " + fillStyle.getGradientMatrix());
        addNode(newNode, "gradient", fillStyle.getGradient());
        break;
      case FillStyle.TYPE_RADIAL_GRADIENT:
        addLeaf(newNode, "type: radial gradient");
        addLeaf(newNode, "gradientMatrix: " + fillStyle.getGradientMatrix());
        addNode(newNode, "gradient", fillStyle.getGradient());
        break;
      case FillStyle.TYPE_FOCAL_RADIAL_GRADIENT:
        addLeaf(newNode, "type: focal radial gradient");
        addLeaf(newNode, "gradientMatrix: " + fillStyle.getGradientMatrix());
        addNode(newNode, "gradient", fillStyle.getGradient());
        break;
      case FillStyle.TYPE_TILED_BITMAP:
        addLeaf(newNode, "type: repeating bitmap");
        addLeaf(newNode, "bitmapId: " + fillStyle.getBitmapId());
        addLeaf(newNode, "bitmapMatrix: " + fillStyle.getBitmapMatrix());
        break;
      case FillStyle.TYPE_CLIPPED_BITMAP:
        addLeaf(newNode, "type: clipped bitmap");
        addLeaf(newNode, "bitmapId: " + fillStyle.getBitmapId());
        addLeaf(newNode, "bitmapMatrix: " + fillStyle.getBitmapMatrix());
        break;
      case FillStyle.TYPE_NONSMOOTHED_TILED_BITMAP:
        addLeaf(newNode, "type: non-smoothed repeating bitmap");
        addLeaf(newNode, "bitmapId: " + fillStyle.getBitmapId());
        addLeaf(newNode, "bitmapMatrix: " + fillStyle.getBitmapMatrix());
        break;
      case FillStyle.TYPE_NONSMOOTHED_CLIPPED_BITMAP:
        addLeaf(newNode, "type: non-smoothed clipped bitmap");
        addLeaf(newNode, "bitmapId: " + fillStyle.getBitmapId());
        addLeaf(newNode, "bitmapMatrix: " + fillStyle.getBitmapMatrix());
        break;
      default:
        addLeaf(newNode, "unknown fill type: " + fillStyle.getType());
    }
  }

  private static void addNode(
    DefaultMutableTreeNode node, LineStyle lineStyle, int index) {
    DefaultMutableTreeNode newNode = addParentNode(node, "LineStyle " + index);
    addLeaf(newNode, "width: " + lineStyle.getWidth());
    addLeaf(newNode, "color: " + lineStyle.getColor());
  }

  private static void addNode(
    DefaultMutableTreeNode node, LineStyle2 lineStyle, int index) {
    DefaultMutableTreeNode newNode = addParentNode(node, "LineStyle2 " + index);
    addLeaf(newNode, "width: " + lineStyle.getWidth());
    addLeaf(
      newNode,
      "startCapStyle: " + getCapStyleString(lineStyle.getStartCapStyle()));
    addLeaf(
      newNode, "endCapStyle: " + getCapStyleString(lineStyle.getEndCapStyle()));
    byte jointStyle = lineStyle.getJointStyle();
    addLeaf(newNode, "jointStyle: " + getJointStyleString(jointStyle));
    if (jointStyle == EnhancedStrokeStyle.JOINT_MITER) {
      addLeaf(newNode, "miterLimit: " + lineStyle.getMiterLimit());
    }
    addLeaf(newNode, "pixelHinting: " + lineStyle.isPixelHinting());
    addLeaf(newNode, "close: " + lineStyle.isClose());
    addLeaf(
      newNode,
      "scaleStroke: " + getScaleStrokeString(lineStyle.getScaleStroke()));
    FillStyle fillStyle = lineStyle.getFillStyle();
    if (fillStyle == null) {
      addLeaf(newNode, "color: " + lineStyle.getColor());
    } else {
      addNode(newNode, fillStyle, 0);
    }
  }

  private static void addNode(
    DefaultMutableTreeNode node, String var, Gradient gradient) {
    DefaultMutableTreeNode newNode = addParentNode(
        node,
        ((gradient instanceof FocalGradient) ? "FocalGradient" : "Gradient"));
    addLeaf(
      newNode,
      "spreadMethod: " + getSpreadMethodString(gradient.getSpreadMethod()));
    addLeaf(
      newNode,
      "interpolationMethod: " +
      getInterpolationMethodString(gradient.getInterpolationMethod()));
    if (gradient instanceof FocalGradient) {
      addLeaf(
        newNode,
        "focalPointRatio: " + ((FocalGradient) gradient).getFocalPointRatio());
    }
    GradRecord[] records = gradient.getGradientRecords();
    for (int i = 0; i < records.length; i++) {
      addNode(newNode, records[i]);
    }
  }

  private static void addNode(DefaultMutableTreeNode node, GradRecord record) {
    DefaultMutableTreeNode newNode = addParentNode(node, "GradientRecord");
    addLeaf(newNode, "ratio: " + record.getRatio());
    addLeaf(newNode, "color: " + record.getColor());
  }

  private static void addNode(
    DefaultMutableTreeNode node, String var, LineStyleArray lineStyleArray) {
    DefaultMutableTreeNode newNode = addParentNode(
        node, var + "LineStyleArray (" + lineStyleArray.getSize() + " styles)");
    for (int i = 1; i <= lineStyleArray.getSize(); i++) {
      Object style = lineStyleArray.getStyle(i);
      if (style instanceof LineStyle) {
        addNode(newNode, (LineStyle) style, i);
      } else {
        addNode(newNode, (LineStyle2) style, i);
      }
    }
  }

  private static void addNode(DefaultMutableTreeNode node, DefineSound tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatDefTag("DefineSound"));
    addLeaf(tagNode, "characterId: " + tag.getCharacterId());
    byte format = tag.getFormat();
    addLeaf(
      tagNode, "format: " + format + " (" + getSoundFormatString(format) + ")");
    byte rate = tag.getRate();
    addLeaf(tagNode, "rate: " + rate + " (" + getSoundRateString(rate) + ")");
    int sampleSize = (tag.is16BitSample()) ? 16 : 8;
    addLeaf(tagNode, "sampleSize: " + sampleSize + " bits");
    String type = tag.isStereo() ? "stereo" : "mono";
    addLeaf(tagNode, "type: " + type);
    addLeaf(tagNode, "sampleCount: " + tag.getSampleCount());
    addLeaf(
      tagNode, "soundData: " + " byte[" + tag.getSoundData().length + "]");
  }

  private static void addNode(DefaultMutableTreeNode node, DefineSprite tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatDefTag("DefineSprite"));
    addLeaf(tagNode, "characterId: " + tag.getCharacterId());
    addLeaf(tagNode, "frameCount: " + tag.getFrameCount());
    List ctrlTags                      = tag.getControlTags();
    DefaultMutableTreeNode ctrlTagNode = addParentNode(
        tagNode, "controlTags: Tag[" + ctrlTags.size() + "]");
    for (int i = 0; i < ctrlTags.size(); i++) {
      addNode(ctrlTagNode, (Tag) ctrlTags.get(i));
    }
  }

  private static void addNode(DefaultMutableTreeNode node, DefineText tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatDefTag("DefineText"));
    addLeaf(tagNode, "characterId: " + tag.getCharacterId());
    addLeaf(tagNode, "textBounds: " + tag.getTextBounds());
    addLeaf(tagNode, "textMatrix: " + tag.getTextMatrix());
    addNode(tagNode, "textRecords: ", tag.getTextRecords());
  }

  private static void addNode(DefaultMutableTreeNode node, DefineText2 tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatDefTag("DefineText2"));
    addLeaf(tagNode, "characterId: " + tag.getCharacterId());
    addLeaf(tagNode, "textBounds: " + tag.getTextBounds());
    addLeaf(tagNode, "textMatrix: " + tag.getTextMatrix());
    addNode(tagNode, "textRecords: ", tag.getTextRecords());
  }

  private static void addNode(
    DefaultMutableTreeNode node, String var, TextRecord[] textRecords) {
    DefaultMutableTreeNode newNode = addParentNode(
        node, var + "TextRecord[" + textRecords.length + "]");
    for (int i = 0; i < textRecords.length; i++) {
      TextRecord record              = textRecords[i];
      DefaultMutableTreeNode recNode = addParentNode(newNode, "TextRecord");
      if (record.getFontId() > 0) {
        addLeaf(recNode, "fontId: " + record.getFontId());
        addLeaf(recNode, "textHeight: " + record.getTextHeight());
      }
      if (record.getTextColor() != null) {
        addLeaf(recNode, "textColor: " + record.getTextColor());
      }
      if (record.getXOffset() != 0) {
        addLeaf(recNode, "xOffset: " + record.getXOffset());
      }
      if (record.getYOffset() != 0) {
        addLeaf(recNode, "yOffset: " + record.getYOffset());
      }
      addNode(recNode, "glyphEntries: ", record.getGlyphEntries());
    }
  }

  private static void addNode(
    DefaultMutableTreeNode node, String var, GlyphEntry[] glyphEntries) {
    DefaultMutableTreeNode newNode = addParentNode(
        node, var + "GlyphEntry[" + glyphEntries.length + "]");
    for (int i = 0; i < glyphEntries.length; i++) {
      GlyphEntry entry                 = glyphEntries[i];
      DefaultMutableTreeNode entryNode = addParentNode(newNode, "GlyphEntry");
      addLeaf(entryNode, "glyphIndex: " + entry.getGlyphIndex());
      addLeaf(entryNode, "glyphAdvance: " + entry.getGlyphAdvance());
    }
  }

  private static void addNode(
    DefaultMutableTreeNode node, DefineVideoStream tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatDefTag("DefineVideoStream"));
    addLeaf(tagNode, "characterId: " + tag.getCharacterId());
    addLeaf(tagNode, "numFrames: " + tag.getNumFrames());
    addLeaf(tagNode, "width: " + tag.getWidth());
    addLeaf(tagNode, "height: " + tag.getHeight());
    String deblocking = "unknown value";
    switch (tag.getDeblocking()) {
      case DefineVideoStream.DEBLOCKING_OFF:
        deblocking = "off";
        break;
      case DefineVideoStream.DEBLOCKING_ON:
        deblocking = "on";
        break;
      case DefineVideoStream.DEBLOCKING_PACKET:
        deblocking = "use video packet setting";
        break;
    }
    addLeaf(tagNode, "deblocking: " + deblocking);
    addLeaf(tagNode, "smoothing: " + (tag.isSmoothing() ? "on" : "off"));
    String codec  = "unknown codec";
    short codecId = tag.getCodecId();
    switch (codecId) {
      case DefineVideoStream.CODEC_SORENSON_H263:
        codec = "Sorenson H.263";
        break;
      case DefineVideoStream.CODEC_SCREEN_VIDEO:
        codec = "Screen Video";
        break;
      case DefineVideoStream.CODEC_VP6:
        codec = "On2 VP6";
        break;
      case DefineVideoStream.CODEC_VP6_ALPHA:
        codec = "On2 VP6 with alpha";
        break;
      case DefineVideoStream.CODEC_SCREEN_VIDEO_V2:
        codec = "Screen Video V2";
        break;
      default:
        codec = "unknown codec: " + codecId;
    }
    addLeaf(tagNode, "codec: " + codec);
  }

  private static void addNode(DefaultMutableTreeNode node, DoAction tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatControlTag("DoAction"));
    addNode(tagNode, "actions: ", tag.getActions());
  }

  private static void addNode(DefaultMutableTreeNode node, DoInitAction tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatControlTag("DoInitAction"));
    addLeaf(tagNode, "spriteId: " + tag.getSpriteId());
    addNode(tagNode, "actions: ", tag.getInitActions());
  }

  private static void addNode(DefaultMutableTreeNode node, EnableDebugger tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatControlTag("EnableDebugger"));
    String password                = tag.getPassword();
    if (password == null) {
      addLeaf(tagNode, "No password");
    } else {
      addLeaf(tagNode, "password: " + password);
    }
  }

  private static void addNode(DefaultMutableTreeNode node, EnableDebugger2 tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatControlTag("EnableDebugger2"));
    String password                = tag.getPassword();
    if (password == null) {
      addLeaf(tagNode, "No password");
    } else {
      addLeaf(tagNode, "password: " + password);
    }
  }

  private static void addNode(DefaultMutableTreeNode node, ExportAssets tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatControlTag("ExportAssets"));
    ExportMapping[] exportMappings = tag.getExportMappings();
    for (int i = 0; i < exportMappings.length; i++) {
      addLeaf(
        tagNode,
        "characterId: " + exportMappings[i].getCharacterId() + ", name: " +
        exportMappings[i].getName());
    }
  }

  private static void addNode(DefaultMutableTreeNode node, FileAttributes tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatControlTag("FileAttributes"));
    addLeaf(tagNode, "allowNetworkAccess: " + tag.isAllowNetworkAccess());
    addLeaf(tagNode, "hasMetadata: " + tag.hasMetadata());
  }

  private static void addNode(DefaultMutableTreeNode node, FrameLabel tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatControlTag("FrameLabel"));
    addLeaf(tagNode, "name: " + tag.getName());
    addLeaf(tagNode, "namedAnchor: " + tag.isNamedAnchor());
  }

  private static void addNode(DefaultMutableTreeNode node, ImportAssets tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node,
        formatControlTag(
          (tag instanceof ImportAssets2) ? "ImportAssets2" : "ImportAssets"));
    addLeaf(tagNode, "url: " + tag.getUrl());
    ImportMapping[] importMappings      = tag.getImportMappings();
    DefaultMutableTreeNode mappingsNode = addParentNode(
        tagNode, "importMappings[" + importMappings.length + "]");
    for (int i = 0; i < importMappings.length; i++) {
      addLeaf(
        mappingsNode,
        "name: " + importMappings[i].getName() + ", characterId: " +
        importMappings[i].getCharacterId());
    }
  }

  private static void addNode(DefaultMutableTreeNode node, JPEGTables tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatDefTag("JPEGTables"));
    addLeaf(tagNode, "jpegData: " + " byte[" + tag.getJpegData().length + "]");
  }

  private static void addNode(DefaultMutableTreeNode node, Metadata tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatControlTag("Metadata"));
    addLeaf(tagNode, "data: " + tag.getDataString());
  }

  private static void addNode(DefaultMutableTreeNode node, PlaceObject tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatControlTag("PlaceObject"));
    addLeaf(tagNode, "characterId: " + tag.getCharacterId());
    addLeaf(tagNode, "depth: " + tag.getDepth());
    addLeaf(tagNode, "matrix: " + tag.getMatrix());
    if (tag.getColorTransform() != null) {
      addNode(tagNode, "colorTransform: ", tag.getColorTransform());
    }
  }

  private static void addNode(
    DefaultMutableTreeNode node, String var, CXform cXform) {
    DefaultMutableTreeNode newNode = addParentNode(node, var + "CXform");
    if (cXform.hasMultTerms()) {
      addLeaf(newNode, "redMultTerm: " + cXform.getRedMultTerm());
      addLeaf(newNode, "greenMultTerm: " + cXform.getGreenMultTerm());
      addLeaf(newNode, "blueMultTerm: " + cXform.getBlueMultTerm());
    } else {
      addLeaf(newNode, "no multiplication transform");
    }
    if (cXform.hasAddTerms()) {
      addLeaf(newNode, "redAddTerm: " + cXform.getRedAddTerm());
      addLeaf(newNode, "greenAddTerm: " + cXform.getGreenAddTerm());
      addLeaf(newNode, "blueAddTerm: " + cXform.getBlueAddTerm());
    } else {
      addLeaf(newNode, "no addition transform");
    }
  }

  private static void addNode(
    DefaultMutableTreeNode node, String var, CXformWithAlpha xform) {
    DefaultMutableTreeNode newNode = addParentNode(
        node, var + "CXformWithAlpha");
    if (xform.hasMultTerms()) {
      addLeaf(newNode, "redMultTerm: " + xform.getRedMultTerm());
      addLeaf(newNode, "greenMultTerm: " + xform.getGreenMultTerm());
      addLeaf(newNode, "blueMultTerm: " + xform.getBlueMultTerm());
      addLeaf(newNode, "alphaMultTerm: " + xform.getAlphaMultTerm());
    } else {
      addLeaf(newNode, "no multiplication transform");
    }
    if (xform.hasAddTerms()) {
      addLeaf(newNode, "redAddTerm: " + xform.getRedAddTerm());
      addLeaf(newNode, "greenAddTerm: " + xform.getGreenAddTerm());
      addLeaf(newNode, "blueAddTerm: " + xform.getBlueAddTerm());
      addLeaf(newNode, "alphaAddTerm: " + xform.getAlphaAddTerm());
    } else {
      addLeaf(newNode, "no addition transform");
    }
  }

  private static void addNode(DefaultMutableTreeNode node, PlaceObject2 tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatControlTag("PlaceObject2"));
    addLeaf(tagNode, "depth: " + tag.getDepth());
    addLeaf(tagNode, "move: " + tag.isMove());
    if (tag.hasCharacter()) {
      addLeaf(tagNode, "characterId: " + tag.getCharacterId());
    }
    if (tag.hasMatrix()) {
      addLeaf(tagNode, "matrix: " + tag.getMatrix());
    }
    if (tag.hasColorTransform()) {
      addNode(tagNode, "colorTransform: ", tag.getColorTransform());
    }
    if (tag.hasRatio()) {
      addLeaf(tagNode, "ratio: " + tag.getRatio());
    }
    if (tag.hasName()) {
      addLeaf(tagNode, "name: " + tag.getName());
    }
    if (tag.hasClipDepth()) {
      addLeaf(tagNode, "clipDepth: " + tag.getClipDepth());
    }
    if (tag.hasClipActions()) {
      addNode(tagNode, "clipActions: ", tag.getClipActions());
    }
  }

  private static void addNode(DefaultMutableTreeNode node, PlaceObject3 tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatControlTag("PlaceObject3"));
    addLeaf(tagNode, "depth: " + tag.getDepth());
    addLeaf(tagNode, "move: " + tag.isMove());
    if (tag.hasCharacter()) {
      addLeaf(tagNode, "characterId: " + tag.getCharacterId());
    }
    if (tag.hasMatrix()) {
      addLeaf(tagNode, "matrix: " + tag.getMatrix());
    }
    if (tag.hasColorTransform()) {
      addNode(tagNode, "colorTransform: ", tag.getColorTransform());
    }
    if (tag.hasRatio()) {
      addLeaf(tagNode, "ratio: " + tag.getRatio());
    }
    if (tag.hasName()) {
      addLeaf(tagNode, "name: " + tag.getName());
    }
    if (tag.hasClipDepth()) {
      addLeaf(tagNode, "clipDepth: " + tag.getClipDepth());
    }
    addLeaf(tagNode, "cacheAsBitmap: " + tag.isCacheAsBitmap());
    if (tag.hasFilters()) {
      List filters                       = tag.getFilters();
      int count                          = filters.size();
      DefaultMutableTreeNode filtersNode = addParentNode(
          tagNode, "filters: Filter[" + count + "]");
      for (int i = 0; i < count; i++) {
        addNode(filtersNode, (Filter) filters.get(i));
      }
    }
    if (tag.hasBlendMode()) {
      short blendMode = tag.getBlendMode();
      addLeaf(tagNode, "blendMode: " + BlendMode.getDescription(blendMode));
    }
    if (tag.hasClipActions()) {
      addNode(tagNode, "clipActions: ", tag.getClipActions());
    }
  }

  private static void addNode(DefaultMutableTreeNode node, Filter filter) {
    DefaultMutableTreeNode filterNode;
    if (filter instanceof ColorMatrixFilter) {
      ColorMatrixFilter colorMatrixFilter = (ColorMatrixFilter) filter;
      filterNode = addParentNode(node, "ColorMatrixFilter");
      float[] matrix            = colorMatrixFilter.getMatrix();
      StringBuffer matrixBuffer = new StringBuffer("matrix: ");
      for (int i = 0; i < matrix.length; i++) {
        matrixBuffer.append(matrix[i]);
        matrixBuffer.append(" ");
      }
      addLeaf(filterNode, matrixBuffer.toString());
    } else if (filter instanceof ConvolutionFilter) {
      ConvolutionFilter convolutionFilter = (ConvolutionFilter) filter;
      filterNode = addParentNode(node, "ConvolutionFilter");
      addLeaf(filterNode, "matrixRows: " + convolutionFilter.getMatrixRows());
      float[] matrix            = convolutionFilter.getMatrix();
      StringBuffer matrixBuffer = new StringBuffer("matrix: ");
      for (int i = 0; i < matrix.length; i++) {
        matrixBuffer.append(matrix[i]);
        matrixBuffer.append(" ");
      }
      addLeaf(filterNode, matrixBuffer.toString());
      addLeaf(filterNode, "color: " + convolutionFilter.getColor());
      addLeaf(filterNode, "divisor: " + convolutionFilter.getDivisor());
      addLeaf(filterNode, "bias: " + convolutionFilter.getBias());
      addLeaf(filterNode, "clamp: " + convolutionFilter.isClamp());
      addLeaf(
        filterNode, "preserveAlpha: " + convolutionFilter.isPreserveAlpha());
    } else if (filter instanceof BlurFilter) {
      BlurFilter blurFilter = (BlurFilter) filter;
      filterNode = addParentNode(node, "BlurFilter");
      addLeaf(filterNode, "x: " + blurFilter.getX());
      addLeaf(filterNode, "y: " + blurFilter.getY());
      addLeaf(filterNode, "quality: " + blurFilter.getQuality());
    } else if (filter instanceof DropShadowFilter) {
      DropShadowFilter dropShadowFilter = (DropShadowFilter) filter;
      filterNode = addParentNode(node, "DropShadowFilter");
      addLeaf(filterNode, "color: " + dropShadowFilter.getColor());
      addLeaf(filterNode, "x: " + dropShadowFilter.getX());
      addLeaf(filterNode, "y: " + dropShadowFilter.getY());
      addLeaf(filterNode, "angle: " + dropShadowFilter.getAngle());
      addLeaf(filterNode, "distance: " + dropShadowFilter.getDistance());
      addLeaf(filterNode, "strength: " + dropShadowFilter.getStrength());
      addLeaf(filterNode, "quality: " + dropShadowFilter.getQuality());
      addLeaf(filterNode, "inner: " + dropShadowFilter.isInner());
      addLeaf(filterNode, "knockout: " + dropShadowFilter.isKnockout());
      addLeaf(filterNode, "hideObject: " + dropShadowFilter.isHideObject());
    } else if (filter instanceof GlowFilter) {
      GlowFilter glowFilter = (GlowFilter) filter;
      filterNode = addParentNode(node, "GlowFilter");
      addLeaf(filterNode, "color: " + glowFilter.getColor());
      addLeaf(filterNode, "x: " + glowFilter.getX());
      addLeaf(filterNode, "y: " + glowFilter.getY());
      addLeaf(filterNode, "strength: " + glowFilter.getStrength());
      addLeaf(filterNode, "quality: " + glowFilter.getQuality());
      addLeaf(filterNode, "inner: " + glowFilter.isInner());
      addLeaf(filterNode, "knockout: " + glowFilter.isKnockout());
    } else if (filter instanceof BevelFilter) {
      BevelFilter bevelFilter = (BevelFilter) filter;
      filterNode = addParentNode(node, "BevelFilter");
      addLeaf(filterNode, "highlightColor: " + bevelFilter.getHighlightColor());
      addLeaf(filterNode, "shadowColor: " + bevelFilter.getShadowColor());
      addLeaf(filterNode, "x: " + bevelFilter.getX());
      addLeaf(filterNode, "y: " + bevelFilter.getY());
      addLeaf(filterNode, "angle: " + bevelFilter.getAngle());
      addLeaf(filterNode, "distance: " + bevelFilter.getDistance());
      addLeaf(filterNode, "strength: " + bevelFilter.getStrength());
      addLeaf(filterNode, "quality: " + bevelFilter.getQuality());
      addLeaf(filterNode, "inner: " + bevelFilter.isInner());
      addLeaf(filterNode, "knockout: " + bevelFilter.isKnockout());
      addLeaf(filterNode, "onTop: " + bevelFilter.isOnTop());
    } else if (filter instanceof GradientGlowFilter) {
      GradientGlowFilter gradientGlowFilter = (GradientGlowFilter) filter;
      filterNode = addParentNode(node, "GradientGlowFilter");
      RGBA[] colors                            = gradientGlowFilter.getColors();
      short[] ratios                           = gradientGlowFilter.getRatios();
      int controlPointsCount                   = colors.length;
      DefaultMutableTreeNode controlPointsNode = addParentNode(
          filterNode, "control points[" + controlPointsCount + "]");
      for (int i = 0; i < controlPointsCount; i++) {
        addLeaf(
          controlPointsNode,
          "color " + i + ": " + colors[i] + " ratio: " + ratios[i]);
      }
      addLeaf(filterNode, "x: " + gradientGlowFilter.getX());
      addLeaf(filterNode, "y: " + gradientGlowFilter.getY());
      addLeaf(filterNode, "angle: " + gradientGlowFilter.getAngle());
      addLeaf(filterNode, "distance: " + gradientGlowFilter.getDistance());
      addLeaf(filterNode, "strength: " + gradientGlowFilter.getStrength());
      addLeaf(filterNode, "quality: " + gradientGlowFilter.getQuality());
      addLeaf(filterNode, "inner: " + gradientGlowFilter.isInner());
      addLeaf(filterNode, "knockout: " + gradientGlowFilter.isKnockout());
      addLeaf(filterNode, "onTop: " + gradientGlowFilter.isOnTop());
    } else if (filter instanceof GradientBevelFilter) {
      GradientBevelFilter gradientBevelFilter = (GradientBevelFilter) filter;
      filterNode = addParentNode(node, "GradientBevelFilter");
      RGBA[] colors                            = gradientBevelFilter.getColors();
      short[] ratios                           = gradientBevelFilter.getRatios();
      int controlPointsCount                   = colors.length;
      DefaultMutableTreeNode controlPointsNode = addParentNode(
          filterNode, "control points[" + controlPointsCount + "]");
      for (int i = 0; i < controlPointsCount; i++) {
        addLeaf(
          controlPointsNode,
          "color " + i + ": " + colors[i] + " ratio: " + ratios[i]);
      }
      addLeaf(filterNode, "x: " + gradientBevelFilter.getX());
      addLeaf(filterNode, "y: " + gradientBevelFilter.getY());
      addLeaf(filterNode, "angle: " + gradientBevelFilter.getAngle());
      addLeaf(filterNode, "distance: " + gradientBevelFilter.getDistance());
      addLeaf(filterNode, "strength: " + gradientBevelFilter.getStrength());
      addLeaf(filterNode, "quality: " + gradientBevelFilter.getQuality());
      addLeaf(filterNode, "inner: " + gradientBevelFilter.isInner());
      addLeaf(filterNode, "knockout: " + gradientBevelFilter.isKnockout());
      addLeaf(filterNode, "onTop: " + gradientBevelFilter.isOnTop());
    }
  }

  private static void addNode(
    DefaultMutableTreeNode node, String var, ClipActions clipActions) {
    DefaultMutableTreeNode newNode = addParentNode(node, var + "ClipActions");
    addNode(newNode, "allEventFlags: ", clipActions.getEventFlags());
    List records = clipActions.getClipActionRecords();
    for (int i = 0; i < records.size(); i++) {
      addNode(newNode, (ClipActionRecord) records.get(i));
    }
  }

  private static void addNode(
    DefaultMutableTreeNode node, ClipActionRecord clipActionRecord) {
    DefaultMutableTreeNode newNode = addParentNode(node, "ClipActionRecord");
    addNode(newNode, "eventFlags: ", clipActionRecord.getEventFlags());
    if (clipActionRecord.getEventFlags().isKeyPress()) {
      addLeaf(newNode, "keyCode: " + clipActionRecord.getKeyCode());
    }
    addNode(newNode, "actions: ", clipActionRecord.getActions());
  }

  private static void addNode(
    DefaultMutableTreeNode node, String var, ActionBlock actionBlock) {
    List actionRecords             = actionBlock.getActions();
    DefaultMutableTreeNode newNode = addParentNode(
        node,
        var + actionRecords.size() + " actions; size : " +
        actionBlock.getSize());
    for (int i = 0; i < actionRecords.size(); i++) {
      addNode(newNode, (Action) actionRecords.get(i));
    }
  }

  private static void addNode(DefaultMutableTreeNode node, Action action) {
    String actionDescription = "<html>";
    if (action.getLabel() != null) {
      actionDescription += ("<code>" + action.getLabel() + "</code> @ ");
    }
    actionDescription += (action.getOffset() + " (" + action.getSize() + "): ");
    switch (action.getCode()) {
      case ActionConstants.PUSH:
        actionDescription += getPushDescription((Push) action);
        break;
      case ActionConstants.TRY:
        actionDescription += "Try";
        break;
      case ActionConstants.IF:
        actionDescription += ("If branchLabel: <code>" +
        ((If) action).getBranchLabel() + "</code> " + "branchOffset: " +
        ((If) action).getBranchOffset());
        break;
      case ActionConstants.JUMP:
        actionDescription += ("Jump branchLabel: <code>" +
        ((Jump) action).getBranchLabel() + "</code> " + "branchOffset: " +
        ((Jump) action).getBranchOffset());
        break;
      default:
        actionDescription += action.toString();
    }
    actionDescription += "</html>";
    DefaultMutableTreeNode actionNode = addParentNode(node, actionDescription);
    switch (action.getCode()) {
      case ActionConstants.CONSTANT_POOL:
        ConstantPool constantPool = (ConstantPool) action;
        constants = constantPool.getConstants();
        String constStr = "c" + ((constants.size() > 255) ? "16" : "8") + "[";
        for (int i = 0; i < constants.size(); i++) {
          addLeaf(actionNode, constStr + i + "]: " + constants.get(i));
        }
        break;
      case ActionConstants.WITH:
        addNode(actionNode, (With) action);
        break;
      case ActionConstants.TRY:
        addNode(actionNode, (Try) action);
        break;
      case ActionConstants.DEFINE_FUNCTION:
        addNode(actionNode, (DefineFunction) action);
        break;
      case ActionConstants.DEFINE_FUNCTION_2:
        addNode(actionNode, (DefineFunction2) action);
        break;
    }
  }

  private static void addNode(DefaultMutableTreeNode node, With action) {
    addNode(node, "withBlock: ", action.getWithBlock());
  }

  private static void addNode(DefaultMutableTreeNode node, Try action) {
    boolean catchInRegister = action.catchInRegister();
    addLeaf(node, "catchInRegister: " + catchInRegister);
    if (catchInRegister) {
      addLeaf(node, "catchRegister: " + action.getCatchRegister());
    } else {
      addLeaf(node, "catchVariable: " + action.getCatchVariable());
    }
    addNode(node, "tryBlock: ", action.getTryBlock());
    if (action.hasCatchBlock()) {
      addNode(node, "catchBlock: ", action.getCatchBlock());
    }
    if (action.hasFinallyBlock()) {
      addNode(node, "finallyBlock: ", action.getFinallyBlock());
    }
  }

  private static void addNode(
    DefaultMutableTreeNode node, DefineFunction defineFunction) {
    String[] parameters = defineFunction.getParameters();
    String paramList    = "";
    for (int i = 0; i < parameters.length; i++) {
      paramList += (parameters[i]);
      if (i != (parameters.length - 1)) {
        paramList += ", ";
      }
    }
    addLeaf(node, "parameters: " + paramList);
    addNode(node, "body: ", defineFunction.getBody());
  }

  private static void addNode(
    DefaultMutableTreeNode node, DefineFunction2 defineFunction2) {
    DefaultMutableTreeNode headerNode = addParentNode(node, "header");
    RegisterParam[] regParameters     = defineFunction2.getParameters();
    DefaultMutableTreeNode paramsNode = addParentNode(
        headerNode, "parameters: RegisterParam[" + regParameters.length + "]");
    for (int i = 0; i < regParameters.length; i++) {
      DefaultMutableTreeNode paramNode = addParentNode(
          paramsNode, "RegisterParam");
      RegisterParam regParam           = regParameters[i];
      addLeaf(paramNode, "register: " + regParam.getRegister());
      addLeaf(paramNode, "paramName: " + regParam.getParamName());
    }
    addLeaf(headerNode, "registerCount: " + defineFunction2.getRegisterCount());
    addLeaf(headerNode, "suppressThis: " + defineFunction2.suppressesThis());
    addLeaf(headerNode, "preloadThis: " + defineFunction2.preloadsThis());
    addLeaf(
      headerNode, "suppressArguments: " +
      defineFunction2.suppressesArguments());
    addLeaf(
      headerNode, "preloadArguments: " + defineFunction2.preloadsArguments());
    addLeaf(headerNode, "suppressSuper: " + defineFunction2.suppressesSuper());
    addLeaf(headerNode, "preloadSuper: " + defineFunction2.preloadsSuper());
    addLeaf(headerNode, "preloadRoot: " + defineFunction2.preloadsRoot());
    addLeaf(headerNode, "preloadParent: " + defineFunction2.preloadsParent());
    addLeaf(headerNode, "preloadGlobal: " + defineFunction2.preloadsGlobal());
    addNode(node, "body: ", defineFunction2.getBody());
  }

  private static void addNode(
    DefaultMutableTreeNode node, String var, ClipEventFlags clipEventFlags) {
    DefaultMutableTreeNode newNode = addParentNode(
        node, var + "ClipEventFlags");
    addLeaf(newNode, "keyUp: " + clipEventFlags.isKeyUp());
    addLeaf(newNode, "keyDown: " + clipEventFlags.isKeyDown());
    addLeaf(newNode, "mouseUp: " + clipEventFlags.isMouseUp());
    addLeaf(newNode, "mouseDown: " + clipEventFlags.isMouseDown());
    addLeaf(newNode, "mouseMove: " + clipEventFlags.isMouseMove());
    addLeaf(newNode, "unload: " + clipEventFlags.isUnload());
    addLeaf(newNode, "enterFrame: " + clipEventFlags.isEnterFrame());
    addLeaf(newNode, "load: " + clipEventFlags.isLoad());
    addLeaf(newNode, "dragOver: " + clipEventFlags.isDragOver());
    addLeaf(newNode, "rollOut: " + clipEventFlags.isRollOut());
    addLeaf(newNode, "rollOver: " + clipEventFlags.isRollOver());
    addLeaf(newNode, "releaseOutside: " + clipEventFlags.isReleaseOutside());
    addLeaf(newNode, "release: " + clipEventFlags.isRelease());
    addLeaf(newNode, "press: " + clipEventFlags.isPress());
    addLeaf(newNode, "initialize: " + clipEventFlags.isInitialize());
    addLeaf(newNode, "data: " + clipEventFlags.isData());
    addLeaf(newNode, "construct: " + clipEventFlags.isConstruct());
    addLeaf(newNode, "keyPress: " + clipEventFlags.isKeyPress());
    addLeaf(newNode, "dragOut: " + clipEventFlags.isDragOut());
  }

  private static void addNode(DefaultMutableTreeNode node, Protect tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatControlTag("Protect"));
    String password                = tag.getPassword();
    if (password == null) {
      addLeaf(tagNode, "No password");
    } else {
      addLeaf(tagNode, "password: " + password);
    }
  }

  private static void addNode(DefaultMutableTreeNode node, RemoveObject tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatControlTag("RemoveObject"));
    addLeaf(tagNode, "characterId: " + tag.getCharacterId());
    addLeaf(tagNode, "depth: " + tag.getDepth());
  }

  private static void addNode(DefaultMutableTreeNode node, RemoveObject2 tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatControlTag("RemoveObject2"));
    addLeaf(tagNode, "depth: " + tag.getDepth());
  }

  private static void addNode(DefaultMutableTreeNode node, ScriptLimits tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatControlTag("ScriptLimits"));
    addLeaf(tagNode, "maxRecursionDepth: " + tag.getMaxRecursionDepth());
    addLeaf(tagNode, "scriptTimeoutSeconds: " + tag.getScriptTimeoutSeconds());
  }

  private static void addNode(
    DefaultMutableTreeNode node, SetBackgroundColor tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatControlTag("SetBackgroundColor"));
    addLeaf(tagNode, "color: " + tag.getColor());
  }

  private static void addNode(DefaultMutableTreeNode node, SetTabIndex tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatControlTag("SetTabIndex"));
    addLeaf(tagNode, "depth: " + tag.getDepth());
    addLeaf(tagNode, "tabIndex: " + tag.getTabIndex());
  }

  private static void addNode(DefaultMutableTreeNode node, ShowFrame tag) {
    addLeaf(node, formatControlTag("ShowFrame"));
  }

  private static void addNode(
    DefaultMutableTreeNode node, SoundStreamBlock tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatControlTag("SoundStreamBlock"));
    addLeaf(
      tagNode, "streamSoundData: byte[" + tag.getStreamSoundData().length +
      "]");
  }

  private static void addNode(DefaultMutableTreeNode node, Scale9Grid tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatControlTag("Scale9Grid"));
    addLeaf(tagNode, "characterId: " + tag.getCharacterId());
    addLeaf(tagNode, "grid: " + tag.getGrid());
  }

  private static void addNode(DefaultMutableTreeNode node, SoundStreamHead tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatControlTag("SoundStreamHead"));
    byte rate                      = tag.getPlaybackRate();
    addLeaf(tagNode, "playbackRate: " + getSoundRateString(rate));
    String type = tag.isPlaybackStereo() ? "stereo" : "mono";
    addLeaf(tagNode, "playbackType: " + type);
    byte format = tag.getStreamFormat();
    addLeaf(tagNode, "streamFormat: " + getSoundFormatString(format));
    rate = tag.getStreamRate();
    addLeaf(tagNode, "streamRate: " + getSoundRateString(rate));
    type = tag.isStreamStereo() ? "stereo" : "mono";
    addLeaf(tagNode, "streamType: " + type);
    addLeaf(tagNode, "streamSampleCount: " + tag.getStreamSampleCount());
    if (format == SoundStreamHead.FORMAT_MP3) {
      addLeaf(tagNode, "latencySeek: " + tag.getLatencySeek());
    }
  }

  private static void addNode(
    DefaultMutableTreeNode node, SoundStreamHead2 tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatControlTag("SoundStreamHead2"));
    byte rate                      = tag.getPlaybackRate();
    addLeaf(tagNode, "playbackRate: " + getSoundRateString(rate));
    String size = tag.isPlayback16BitSample() ? "16 bit" : "8 bit";
    addLeaf(tagNode, "playbackSize: " + size);
    String type = tag.isPlaybackStereo() ? "stereo" : "mono";
    addLeaf(tagNode, "playbackType: " + type);
    byte format = tag.getStreamFormat();
    addLeaf(tagNode, "streamFormat: " + getSoundFormatString(format));
    rate = tag.getStreamRate();
    addLeaf(tagNode, "streamRate: " + getSoundRateString(rate));
    size = tag.isStream16BitSample() ? "16 bit" : "8 bit";
    addLeaf(tagNode, "playbackSize: " + size);
    type = tag.isStreamStereo() ? "stereo" : "mono";
    addLeaf(tagNode, "streamType: " + type);
    addLeaf(tagNode, "streamSampleCount: " + tag.getStreamSampleCount());
    if (format == SoundStreamHead.FORMAT_MP3) {
      addLeaf(tagNode, "latencySeek: " + tag.getLatencySeek());
    }
  }

  private static void addNode(DefaultMutableTreeNode node, StartSound tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatControlTag("StartSound"));
    addLeaf(tagNode, "soundId: " + tag.getSoundId());
    addNode(tagNode, "soundInfo: ", tag.getSoundInfo());
  }

  private static void addNode(
    DefaultMutableTreeNode node, String var, SoundInfo info) {
    DefaultMutableTreeNode siNode = addParentNode(node, var + "SoundInfo");
    addLeaf(siNode, "syncStop: " + info.isSyncStop());
    addLeaf(siNode, "syncNoMultiple: " + info.isSyncNoMultiple());
    if (info.getInPoint() != 0) {
      addLeaf(siNode, "inPoint: " + info.getInPoint());
    }
    if (info.getOutPoint() != 0) {
      addLeaf(siNode, "outPoint: " + info.getOutPoint());
    }
    if (info.getLoopCount() != 0) {
      addLeaf(siNode, "loopCount: " + info.getLoopCount());
    }
    if (info.getEnvelopeRecords() != null) {
      SoundEnvelope[] records               = info.getEnvelopeRecords();
      DefaultMutableTreeNode envRecordsNode = addParentNode(
          siNode, "envelopeRecords: SoundEnvelope[" + records.length + "]");
      for (int i = 0; i < records.length; i++) {
        SoundEnvelope env              = records[i];
        DefaultMutableTreeNode recNode = addParentNode(
            envRecordsNode, "SoundEnvelope");
        addLeaf(recNode, "pos44: " + env.getPos44());
        addLeaf(recNode, "leftLevel: " + env.getLeftLevel());
        addLeaf(recNode, "rightLevel: " + env.getRightLevel());
      }
    }
  }

  private static void addNode(DefaultMutableTreeNode node, UnknownTag tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatControlTag("Unknown tag (" + tag.getCode() + ")"));
    byte[] data                    = tag.getData();
    addLeaf(tagNode, "size: " + data.length);
    addLeaf(tagNode, "data: " + HexUtils.toHex(data));
  }

  private static void addNode(DefaultMutableTreeNode node, VideoFrame tag) {
    DefaultMutableTreeNode tagNode = addParentNode(
        node, formatControlTag("VideoFrame"));
    addLeaf(tagNode, "streamId: " + tag.getStreamId());
    addLeaf(tagNode, "frameNum: " + tag.getFrameNum());
    addLeaf(tagNode, "videoData: " + tag.getVideoData().length + " bytes");
  }

  private static DefaultMutableTreeNode addParentNode(
    DefaultMutableTreeNode node, String string) {
    DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(string);
    node.insert(newNode, node.getChildCount());
    nodes++;
    return newNode;
  }

  private static String formatControlTag(String tagName) {
    return "<html><font color=\"#0000B0\">" + tagName + "</font></html>";
  }

  private static String formatDefTag(String tagName) {
    return "<html><font color=\"#B00000\">" + tagName + "</font></html>";
  }
}
