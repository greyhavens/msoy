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

package com.jswiff.xml;

import com.jswiff.swfrecords.AlignmentZone;
import com.jswiff.swfrecords.BlendMode;
import com.jswiff.swfrecords.ButtonRecord;
import com.jswiff.swfrecords.CXform;
import com.jswiff.swfrecords.KerningRecord;
import com.jswiff.swfrecords.LangCode;
import com.jswiff.swfrecords.Matrix;
import com.jswiff.swfrecords.MorphFillStyles;
import com.jswiff.swfrecords.MorphLineStyles;
import com.jswiff.swfrecords.Rect;
import com.jswiff.swfrecords.Shape;
import com.jswiff.swfrecords.ShapeWithStyle;
import com.jswiff.swfrecords.TextRecord;
import com.jswiff.swfrecords.ZlibBitmapData;
import com.jswiff.swfrecords.tags.DefineBits;
import com.jswiff.swfrecords.tags.DefineBitsJPEG2;
import com.jswiff.swfrecords.tags.DefineBitsJPEG3;
import com.jswiff.swfrecords.tags.DefineBitsLossless;
import com.jswiff.swfrecords.tags.DefineBitsLossless2;
import com.jswiff.swfrecords.tags.DefineButton;
import com.jswiff.swfrecords.tags.DefineButton2;
import com.jswiff.swfrecords.tags.DefineButtonCXform;
import com.jswiff.swfrecords.tags.DefineButtonSound;
import com.jswiff.swfrecords.tags.DefineEditText;
import com.jswiff.swfrecords.tags.DefineFont;
import com.jswiff.swfrecords.tags.DefineFont2;
import com.jswiff.swfrecords.tags.DefineFont3;
import com.jswiff.swfrecords.tags.DefineFontAlignment;
import com.jswiff.swfrecords.tags.DefineFontInfo;
import com.jswiff.swfrecords.tags.DefineFontInfo2;
import com.jswiff.swfrecords.tags.DefineMorphShape;
import com.jswiff.swfrecords.tags.DefineMorphShape2;
import com.jswiff.swfrecords.tags.DefineShape;
import com.jswiff.swfrecords.tags.DefineShape2;
import com.jswiff.swfrecords.tags.DefineShape3;
import com.jswiff.swfrecords.tags.DefineShape4;
import com.jswiff.swfrecords.tags.DefineSound;
import com.jswiff.swfrecords.tags.DefineSprite;
import com.jswiff.swfrecords.tags.DefineText;
import com.jswiff.swfrecords.tags.DefineText2;
import com.jswiff.swfrecords.tags.DefineVideoStream;
import com.jswiff.swfrecords.tags.DoAction;
import com.jswiff.swfrecords.tags.DoInitAction;
import com.jswiff.swfrecords.tags.EnableDebugger;
import com.jswiff.swfrecords.tags.EnableDebugger2;
import com.jswiff.swfrecords.tags.ExportAssets;
import com.jswiff.swfrecords.tags.FlashTypeSettings;
import com.jswiff.swfrecords.tags.FrameLabel;
import com.jswiff.swfrecords.tags.FreeCharacter;
import com.jswiff.swfrecords.tags.ImportAssets;
import com.jswiff.swfrecords.tags.ImportAssets2;
import com.jswiff.swfrecords.tags.JPEGTables;
import com.jswiff.swfrecords.tags.Metadata;
import com.jswiff.swfrecords.tags.PlaceObject;
import com.jswiff.swfrecords.tags.PlaceObject2;
import com.jswiff.swfrecords.tags.PlaceObject3;
import com.jswiff.swfrecords.tags.Protect;
import com.jswiff.swfrecords.tags.RemoveObject;
import com.jswiff.swfrecords.tags.RemoveObject2;
import com.jswiff.swfrecords.tags.Scale9Grid;
import com.jswiff.swfrecords.tags.ScriptLimits;
import com.jswiff.swfrecords.tags.SetTabIndex;
import com.jswiff.swfrecords.tags.ShowFrame;
import com.jswiff.swfrecords.tags.SoundStreamBlock;
import com.jswiff.swfrecords.tags.SoundStreamHead;
import com.jswiff.swfrecords.tags.SoundStreamHead2;
import com.jswiff.swfrecords.tags.StartSound;
import com.jswiff.swfrecords.tags.Tag;
import com.jswiff.swfrecords.tags.TagConstants;
import com.jswiff.swfrecords.tags.UnknownTag;
import com.jswiff.swfrecords.tags.VideoFrame;
import com.jswiff.util.Base64;

import org.dom4j.Attribute;
import org.dom4j.Element;

import java.util.Iterator;
import java.util.List;


class TagXMLReader {
  static Tag readTag(Element tagElement, int tagCode) {
    Tag tag;
    switch (tagCode) {
      case TagConstants.DEFINE_BITS:
        tag = readDefineBits(tagElement);
        break;
      case TagConstants.DEFINE_BITS_JPEG_2:
        tag = readDefineBitsJPEG2(tagElement);
        break;
      case TagConstants.DEFINE_BITS_JPEG_3:
        tag = readDefineBitsJPEG3(tagElement);
        break;
      case TagConstants.DEFINE_BITS_LOSSLESS:
        tag = readDefineBitsLossless(tagElement);
        break;
      case TagConstants.DEFINE_BITS_LOSSLESS_2:
        tag = readDefineBitsLossless2(tagElement);
        break;
      case TagConstants.DEFINE_BUTTON:
        tag = readDefineButton(tagElement);
        break;
      case TagConstants.DEFINE_BUTTON_2:
        tag = readDefineButton2(tagElement);
        break;
      case TagConstants.DEFINE_BUTTON_C_XFORM:
        tag = readDefineButtonCXform(tagElement);
        break;
      case TagConstants.DEFINE_BUTTON_SOUND:
        tag = readDefineButtonSound(tagElement);
        break;
      case TagConstants.DEFINE_EDIT_TEXT:
        tag = readDefineEditText(tagElement);
        break;
      case TagConstants.DEFINE_FONT:
        tag = readDefineFont(tagElement);
        break;
      case TagConstants.DEFINE_FONT_2:
        tag = readDefineFont2(tagElement);
        break;
      case TagConstants.DEFINE_FONT_3:
        tag = readDefineFont3(tagElement);
        break;
      case TagConstants.DEFINE_FONT_INFO:
        tag = readDefineFontInfo(tagElement);
        break;
      case TagConstants.DEFINE_FONT_INFO_2:
        tag = readDefineFontInfo2(tagElement);
        break;
      case TagConstants.DEFINE_FONT_ALIGNMENT:
        tag = readDefineFontAlignment(tagElement);
        break;
      case TagConstants.DEFINE_MORPH_SHAPE:
        tag = readDefineMorphShape(tagElement);
        break;
      case TagConstants.DEFINE_MORPH_SHAPE_2:
        tag = readDefineMorphShape2(tagElement);
        break;
      case TagConstants.DEFINE_SHAPE:
        tag = readDefineShape(tagElement);
        break;
      case TagConstants.DEFINE_SHAPE_2:
        tag = readDefineShape2(tagElement);
        break;
      case TagConstants.DEFINE_SHAPE_3:
        tag = readDefineShape3(tagElement);
        break;
      case TagConstants.DEFINE_SHAPE_4:
        tag = readDefineShape4(tagElement);
        break;
      case TagConstants.DEFINE_SOUND:
        tag = readDefineSound(tagElement);
        break;
      case TagConstants.DEFINE_SPRITE:
        tag = readDefineSprite(tagElement);
        break;
      case TagConstants.DEFINE_TEXT:
        tag = readDefineText(tagElement);
        break;
      case TagConstants.DEFINE_TEXT_2:
        tag = readDefineText2(tagElement);
        break;
      case TagConstants.DEFINE_VIDEO_STREAM:
        tag = readDefineVideoStream(tagElement);
        break;
      case TagConstants.DO_ACTION:
        tag = readDoAction(tagElement);
        break;
      case TagConstants.DO_INIT_ACTION:
        tag = readDoInitAction(tagElement);
        break;
      case TagConstants.ENABLE_DEBUGGER:
        tag = readEnableDebugger(tagElement);
        break;
      case TagConstants.ENABLE_DEBUGGER_2:
        tag = readEnableDebugger2(tagElement);
        break;
      case TagConstants.EXPORT_ASSETS:
        tag = readExportAssets(tagElement);
        break;
      case TagConstants.FLASHTYPE_SETTINGS:
        tag = readFlashTypeSettings(tagElement);
        break;
      case TagConstants.FRAME_LABEL:
        tag = readFrameLabel(tagElement);
        break;
      case TagConstants.FREE_CHARACTER:
        tag = readFreeCharacter(tagElement);
        break;
      case TagConstants.IMPORT_ASSETS:
        tag = readImportAssets(tagElement);
        break;
      case TagConstants.IMPORT_ASSETS_2:
        tag = readImportAssets2(tagElement);
        break;
      case TagConstants.JPEG_TABLES:
        tag = readJPEGTables(tagElement);
        break;
      case TagConstants.MALFORMED:
        tag = readMalformedTag(tagElement);
        break;
      case TagConstants.METADATA:
        tag = readMetadata(tagElement);
        break;
      case TagConstants.PLACE_OBJECT:
        tag = readPlaceObject(tagElement);
        break;
      case TagConstants.PLACE_OBJECT_2:
        tag = readPlaceObject2(tagElement);
        break;
      case TagConstants.PLACE_OBJECT_3:
        tag = readPlaceObject3(tagElement);
        break;
      case TagConstants.PROTECT:
        tag = readProtect(tagElement);
        break;
      case TagConstants.REMOVE_OBJECT:
        tag = readRemoveObject(tagElement);
        break;
      case TagConstants.REMOVE_OBJECT_2:
        tag = readRemoveObject2(tagElement);
        break;
      case TagConstants.SCRIPT_LIMITS:
        tag = readScriptLimits(tagElement);
        break;
      case TagConstants.SCALE_9_GRID:
        tag = readScale9Grid(tagElement);
        break;
      case TagConstants.SET_TAB_INDEX:
        tag = readSetTabIndex(tagElement);
        break;
      case TagConstants.SHOW_FRAME:
        tag = readShowFrame(tagElement);
        break;
      case TagConstants.SOUND_STREAM_BLOCK:
        tag = readSoundStreamBlock(tagElement);
        break;
      case TagConstants.SOUND_STREAM_HEAD:
        tag = readSoundStreamHead(tagElement);
        break;
      case TagConstants.SOUND_STREAM_HEAD_2:
        tag = readSoundStreamHead2(tagElement);
        break;
      case TagConstants.START_SOUND:
        tag = readStartSound(tagElement);
        break;
      case TagConstants.VIDEO_FRAME:
        tag = readVideoFrame(tagElement);
        break;
      default:
        tag = readUnknownTag(tagElement);
    }
    return tag;
  }

  static Tag readUnknownTag(Element tagElement) {
    short code  = RecordXMLReader.getShortAttribute("code", tagElement);
    byte[] data = Base64.decode(tagElement.getText());
    return new UnknownTag(code, data);
  }

  private static byte getSoundFormat(String format) {
    if (format.equals("adpcm")) {
      return DefineSound.FORMAT_ADPCM;
    } else if (format.equals("mp3")) {
      return DefineSound.FORMAT_MP3;
    } else if (format.equals("nellymoser")) {
      return DefineSound.FORMAT_NELLYMOSER;
    } else if (format.equals("uncompressed")) {
      return DefineSound.FORMAT_UNCOMPRESSED;
    } else if (format.equals("uncompressedle")) {
      return DefineSound.FORMAT_UNCOMPRESSED_LITTLE_ENDIAN;
    } else {
      throw new IllegalArgumentException("Illegal sound format: " + format);
    }
  }

  private static byte getSoundRate(String rate) {
    if (rate.equals("5500")) {
      return DefineSound.RATE_5500_HZ;
    } else if (rate.equals("11000")) {
      return DefineSound.RATE_11000_HZ;
    } else if (rate.equals("22000")) {
      return DefineSound.RATE_22000_HZ;
    } else if (rate.equals("44000")) {
      return DefineSound.RATE_44000_HZ;
    } else {
      throw new IllegalArgumentException("Illegal sound rate: " + rate);
    }
  }

  private static String getStringAttribute(
    String attributeName, Element parentElement) {
    Attribute attribute = (Attribute) parentElement.selectSingleNode(
        "@" + attributeName);
    if (attribute == null) {
      throw new MissingAttributeException(
        attributeName, parentElement.getPath());
    }
    return attribute.getValue();
  }

  private static short getVideoCodecId(String codec) {
    if (codec.equals("screenvideo")) {
      return DefineVideoStream.CODEC_SCREEN_VIDEO;
    } else if (codec.equals("h263")) {
      return DefineVideoStream.CODEC_SORENSON_H263;
    } else if (codec.equals("vp6")) {
      return DefineVideoStream.CODEC_VP6;
    } else if (codec.equals("vp6alpha")) {
      return DefineVideoStream.CODEC_VP6_ALPHA;
    } else if (codec.equals("screenvideov2")) {
      return DefineVideoStream.CODEC_SCREEN_VIDEO_V2;
    } else if (codec.equals("undefined")) {
      return DefineVideoStream.CODEC_UNDEFINED;
    } else {
      throw new IllegalArgumentException("Illegal video codec ID: " + codec);
    }
  }

  private static byte getVideoDeblocking(String deblocking) {
    if (deblocking.equals("on")) {
      return DefineVideoStream.DEBLOCKING_ON;
    } else if (deblocking.equals("off")) {
      return DefineVideoStream.DEBLOCKING_OFF;
    } else if (deblocking.equals("packet")) {
      return DefineVideoStream.DEBLOCKING_PACKET;
    } else {
      throw new IllegalArgumentException(
        "Illegal video deblocking setting: " + deblocking);
    }
  }

  private static Tag readDefineBits(Element tagElement) {
    int characterId = RecordXMLReader.getCharacterId(tagElement);
    byte[] jpegData = RecordXMLReader.getDataElement("jpegdata", tagElement);
    return new DefineBits(characterId, jpegData);
  }

  private static Tag readDefineBitsJPEG2(Element tagElement) {
    int characterId = RecordXMLReader.getCharacterId(tagElement);
    byte[] jpegData = RecordXMLReader.getDataElement("jpegdata", tagElement);
    return new DefineBitsJPEG2(characterId, jpegData);
  }

  private static Tag readDefineBitsJPEG3(Element tagElement) {
    int characterId  = RecordXMLReader.getCharacterId(tagElement);
    byte[] jpegData  = RecordXMLReader.getDataElement("jpegdata", tagElement);
    byte[] alphaData = RecordXMLReader.getDataElement("alphadata", tagElement);
    return new DefineBitsJPEG3(characterId, jpegData, alphaData);
  }

  private static Tag readDefineBitsLossless(Element tagElement) {
    int characterId     = RecordXMLReader.getCharacterId(tagElement);
    String formatString = getStringAttribute("format", tagElement);
    short format;
    if (formatString.equals("24bit")) {
      format = DefineBitsLossless.FORMAT_24_BIT_RGB;
    } else if (formatString.equals("8bit")) {
      format = DefineBitsLossless.FORMAT_8_BIT_COLORMAPPED;
    } else if (formatString.equals("15bit")) {
      format = DefineBitsLossless.FORMAT_15_BIT_RGB;
    } else {
      throw new IllegalArgumentException(
        "Illegal lossless bitmap format: " + formatString);
    }
    int width           = RecordXMLReader.getIntAttribute("width", tagElement);
    int height          = RecordXMLReader.getIntAttribute("height", tagElement);
    ZlibBitmapData data = RecordXMLReader.readZlibBitmapData(tagElement);
    return new DefineBitsLossless(characterId, format, width, height, data);
  }

  private static Tag readDefineBitsLossless2(Element tagElement) {
    int characterId     = RecordXMLReader.getCharacterId(tagElement);
    String formatString = getStringAttribute("format", tagElement);
    short format;
    if (formatString.equals("32bit")) {
      format = DefineBitsLossless2.FORMAT_32_BIT_RGBA;
    } else {
      format = DefineBitsLossless2.FORMAT_8_BIT_COLORMAPPED;
    }
    int width           = RecordXMLReader.getIntAttribute("width", tagElement);
    int height          = RecordXMLReader.getIntAttribute("height", tagElement);
    ZlibBitmapData data = RecordXMLReader.readZlibBitmapData2(tagElement);
    return new DefineBitsLossless2(characterId, format, width, height, data);
  }

  private static Tag readDefineButton(Element tagElement) {
    int characterId           = RecordXMLReader.getCharacterId(tagElement);
    ButtonRecord[] characters = RecordXMLReader.readButtonRecords(tagElement);
    DefineButton defineButton = new DefineButton(characterId, characters);
    RecordXMLReader.readActionBlock(defineButton.getActions(), tagElement);
    return defineButton;
  }

  private static Tag readDefineButton2(Element tagElement) {
    int characterId             = RecordXMLReader.getCharacterId(tagElement);
    boolean trackAsMenu         = RecordXMLReader.getBooleanAttribute(
        "trackasmenu", tagElement);
    ButtonRecord[] characters   = RecordXMLReader.readButtonRecords(tagElement);
    DefineButton2 defineButton2 = new DefineButton2(
        characterId, characters, trackAsMenu);
    defineButton2.setActions(RecordXMLReader.readButtonCondActions(tagElement));
    return defineButton2;
  }

  private static Tag readDefineButtonCXform(Element tagElement) {
    int characterId       = RecordXMLReader.getCharacterId(tagElement);
    CXform colorTransform = RecordXMLReader.readCXform(
        RecordXMLReader.getElement("cxform", tagElement));
    return new DefineButtonCXform(characterId, colorTransform);
  }

  private static Tag readDefineButtonSound(Element tagElement) {
    int buttonId                        = RecordXMLReader.getIntAttribute(
        "buttonid", tagElement);
    DefineButtonSound defineButtonSound = new DefineButtonSound(buttonId);
    Element overUpToIdle                = tagElement.element("overuptoidle");
    if (overUpToIdle != null) {
      int soundId = RecordXMLReader.getIntAttribute("soundid", overUpToIdle);
      defineButtonSound.setOverUpToIdleSoundId(soundId);
      defineButtonSound.setOverUpToIdleSoundInfo(
        RecordXMLReader.readSoundInfo(overUpToIdle));
    }
    Element idleToOverUp = tagElement.element("idletooverup");
    if (idleToOverUp != null) {
      int soundId = RecordXMLReader.getIntAttribute("soundid", idleToOverUp);
      defineButtonSound.setIdleToOverUpSoundId(soundId);
      defineButtonSound.setIdleToOverUpSoundInfo(
        RecordXMLReader.readSoundInfo(idleToOverUp));
    }
    Element overUpToOverDown = tagElement.element("overuptooverdown");
    if (overUpToOverDown != null) {
      int soundId = RecordXMLReader.getIntAttribute(
          "soundid", overUpToOverDown);
      defineButtonSound.setOverUpToOverDownSoundId(soundId);
      defineButtonSound.setOverUpToOverDownSoundInfo(
        RecordXMLReader.readSoundInfo(overUpToOverDown));
    }
    Element overDownToOverUp = tagElement.element("overdowntooverup");
    if (overDownToOverUp != null) {
      int soundId = RecordXMLReader.getIntAttribute(
          "soundid", overDownToOverUp);
      defineButtonSound.setOverDownToOverUpSoundId(soundId);
      defineButtonSound.setOverDownToOverUpSoundInfo(
        RecordXMLReader.readSoundInfo(overDownToOverUp));
    }
    return defineButtonSound;
  }

  private static Tag readDefineEditText(Element tagElement) {
    int characterId               = RecordXMLReader.getCharacterId(tagElement);
    Rect bounds                   = RecordXMLReader.readRect(
        RecordXMLReader.getElement("bounds", tagElement));
    String var                    = tagElement.attributeValue("variable");
    DefineEditText defineEditText = new DefineEditText(
        characterId, bounds, var);
    if (RecordXMLReader.getBooleanAttribute("wordwrap", tagElement)) {
      defineEditText.setWordWrap(true);
    }
    if (RecordXMLReader.getBooleanAttribute("multiline", tagElement)) {
      defineEditText.setMultiline(true);
    }
    if (RecordXMLReader.getBooleanAttribute("password", tagElement)) {
      defineEditText.setPassword(true);
    }
    if (RecordXMLReader.getBooleanAttribute("readonly", tagElement)) {
      defineEditText.setReadOnly(true);
    }
    if (RecordXMLReader.getBooleanAttribute("autosize", tagElement)) {
      defineEditText.setAutoSize(true);
    }
    if (RecordXMLReader.getBooleanAttribute("noselect", tagElement)) {
      defineEditText.setNoSelect(true);
    }
    if (RecordXMLReader.getBooleanAttribute("border", tagElement)) {
      defineEditText.setBorder(true);
    }
    if (RecordXMLReader.getBooleanAttribute("border", tagElement)) {
      defineEditText.setBorder(true);
    }
    if (RecordXMLReader.getBooleanAttribute("html", tagElement)) {
      defineEditText.setHtml(true);
    }
    if (RecordXMLReader.getBooleanAttribute("useoutlines", tagElement)) {
      defineEditText.setUseOutlines(true);
    }
    Attribute maxLength = tagElement.attribute("maxlength");
    if (maxLength != null) {
      defineEditText.setMaxLength(Integer.parseInt(maxLength.getValue()));
    }
    Element initialText = tagElement.element("initialtext");
    if (initialText != null) {
      defineEditText.setInitialText(initialText.getText());
    }
    Element color = tagElement.element("color");
    if (color != null) {
      defineEditText.setTextColor(RecordXMLReader.readRGBA(color));
    }
    Element font = tagElement.element("font");
    if (font != null) {
      defineEditText.setFont(
        RecordXMLReader.getIntAttribute("fontid", font),
        RecordXMLReader.getIntAttribute("height", font));
    }
    Element layout = tagElement.element("layout");
    if (layout != null) {
      String alignString = getStringAttribute("align", layout);
      short align;
      if (alignString.equals("center")) {
        align = DefineEditText.ALIGN_CENTER;
      } else if (alignString.equals("justify")) {
        align = DefineEditText.ALIGN_JUSTIFY;
      } else if (alignString.equals("right")) {
        align = DefineEditText.ALIGN_RIGHT;
      } else {
        align = DefineEditText.ALIGN_LEFT;
      }
      int leftMargin  = RecordXMLReader.getIntAttribute("leftmargin", layout);
      int rightMargin = RecordXMLReader.getIntAttribute("rightmargin", layout);
      int indent      = RecordXMLReader.getIntAttribute("indent", layout);
      int leading     = RecordXMLReader.getIntAttribute("leading", layout);
      defineEditText.setLayout(align, leftMargin, rightMargin, indent, leading);
    }
    return defineEditText;
  }

  private static Tag readDefineFont(Element tagElement) {
    int characterId         = RecordXMLReader.getCharacterId(tagElement);
    Element glyphShapeTable = RecordXMLReader.getElement(
        "glyphshapetable", tagElement);
    List shapeElements      = glyphShapeTable.selectNodes("shape");
    int arrayLength         = shapeElements.size();
    Shape[] shapes          = new Shape[arrayLength];
    for (int i = 0; i < arrayLength; i++) {
      Element shapeElement = (Element) shapeElements.get(i);
      shapes[i] = RecordXMLReader.readShape(shapeElement);
    }
    DefineFont defineFont = new DefineFont(characterId, shapes);
    return defineFont;
  }

  private static Tag readDefineFont2(Element tagElement) {
    int characterId                = RecordXMLReader.getCharacterId(tagElement);
    String fontName                = getStringAttribute("fontname", tagElement);
    Shape[] glyphShapes            = null;
    char[] codeTable               = null;
    short[] advanceTable           = null;
    Rect[] boundsTable             = null;
    boolean hasLayout              = false;
    Element glyphShapeTableElement = tagElement.element("glyphshapetable");
    if (glyphShapeTableElement != null) {
      List glyphElements = glyphShapeTableElement.elements();
      int numGlyphs      = glyphElements.size();
      advanceTable       = new short[numGlyphs];
      boundsTable        = new Rect[numGlyphs];
      glyphShapes        = new Shape[numGlyphs];
      codeTable          = new char[numGlyphs];
      for (int i = 0; i < numGlyphs; i++) {
        Element glyphElement = (Element) glyphElements.get(i);
        codeTable[i]     = getStringAttribute("char", glyphElement).charAt(0);
        glyphShapes[i]   = RecordXMLReader.readShape(
            RecordXMLReader.getElement("shape", glyphElement));
        Attribute advance = glyphElement.attribute("advance");
        if (advance != null) {
          hasLayout         = true;
          advanceTable[i]   = Short.parseShort(advance.getValue());
          boundsTable[i]    = RecordXMLReader.readRect(
              RecordXMLReader.getElement("bounds", glyphElement));
        }
      }
    }
    DefineFont2 defineFont2 = new DefineFont2(
        characterId, fontName, glyphShapes, codeTable);
    if (hasLayout) {
      Element layout               = RecordXMLReader.getElement(
          "layout", tagElement);
      short ascent                 = RecordXMLReader.getShortAttribute(
          "ascent", layout);
      short descent                = RecordXMLReader.getShortAttribute(
          "descent", layout);
      short leading                = RecordXMLReader.getShortAttribute(
          "leading", layout);
      KerningRecord[] kerningTable = null;
      Element kerningTableElement  = layout.element("kerningtable");
      if (kerningTableElement != null) {
        List recordElements = kerningTableElement.elements();
        int arrayLength     = recordElements.size();
        kerningTable        = new KerningRecord[arrayLength];
        for (int i = 0; i < arrayLength; i++) {
          Element recordElement = (Element) recordElements.get(i);
          char left             = getStringAttribute("left", recordElement)
                                    .charAt(0);
          char right            = getStringAttribute("right", recordElement)
                                    .charAt(0);
          short adjustment      = RecordXMLReader.getShortAttribute(
              "adjust", recordElement);
          kerningTable[i]       = new KerningRecord(left, right, adjustment);
        }
      }
      defineFont2.setLayout(
        ascent, descent, leading, advanceTable, boundsTable, kerningTable);
    }
    if (RecordXMLReader.getBooleanAttribute("ansi", tagElement)) {
      defineFont2.setANSI(true);
    }
    if (RecordXMLReader.getBooleanAttribute("shiftjis", tagElement)) {
      defineFont2.setShiftJIS(true);
    }
    if (RecordXMLReader.getBooleanAttribute("bold", tagElement)) {
      defineFont2.setBold(true);
    }
    if (RecordXMLReader.getBooleanAttribute("italic", tagElement)) {
      defineFont2.setItalic(true);
    }
    if (RecordXMLReader.getBooleanAttribute("smalltext", tagElement)) {
      defineFont2.setSmallText(true);
    }
    defineFont2.setLanguageCode(readLangCode(tagElement));
    return defineFont2;
  }

  private static Tag readDefineFont3(Element tagElement) {
    int characterId                = RecordXMLReader.getCharacterId(tagElement);
    String fontName                = getStringAttribute("fontname", tagElement);
    Shape[] glyphShapes            = null;
    char[] codeTable               = null;
    short[] advanceTable           = null;
    Rect[] boundsTable             = null;
    boolean hasLayout              = false;
    Element glyphShapeTableElement = tagElement.element("glyphshapetable");
    if (glyphShapeTableElement != null) {
      List glyphElements = glyphShapeTableElement.elements();
      int numGlyphs      = glyphElements.size();
      advanceTable       = new short[numGlyphs];
      boundsTable        = new Rect[numGlyphs];
      glyphShapes        = new Shape[numGlyphs];
      codeTable          = new char[numGlyphs];
      for (int i = 0; i < numGlyphs; i++) {
        Element glyphElement = (Element) glyphElements.get(i);
        codeTable[i]     = getStringAttribute("char", glyphElement).charAt(0);
        glyphShapes[i]   = RecordXMLReader.readShape(
            RecordXMLReader.getElement("shape", glyphElement));
        Attribute advance = glyphElement.attribute("advance");
        if (advance != null) {
          hasLayout         = true;
          advanceTable[i]   = Short.parseShort(advance.getValue());
          boundsTable[i]    = RecordXMLReader.readRect(
              RecordXMLReader.getElement("bounds", glyphElement));
        }
      }
    }
    DefineFont3 defineFont3 = new DefineFont3(
        characterId, fontName, glyphShapes, codeTable);
    if (hasLayout) {
      Element layout               = RecordXMLReader.getElement(
          "layout", tagElement);
      short ascent                 = RecordXMLReader.getShortAttribute(
          "ascent", layout);
      short descent                = RecordXMLReader.getShortAttribute(
          "descent", layout);
      short leading                = RecordXMLReader.getShortAttribute(
          "leading", layout);
      KerningRecord[] kerningTable = null;
      Element kerningTableElement  = layout.element("kerningtable");
      if (kerningTableElement != null) {
        List recordElements = kerningTableElement.elements();
        int arrayLength     = recordElements.size();
        kerningTable        = new KerningRecord[arrayLength];
        for (int i = 0; i < arrayLength; i++) {
          Element recordElement = (Element) recordElements.get(i);
          char left             = getStringAttribute("left", recordElement)
                                    .charAt(0);
          char right            = getStringAttribute("right", recordElement)
                                    .charAt(0);
          short adjustment      = RecordXMLReader.getShortAttribute(
              "adjust", recordElement);
          kerningTable[i]       = new KerningRecord(left, right, adjustment);
        }
      }
      defineFont3.setLayout(
        ascent, descent, leading, advanceTable, boundsTable, kerningTable);
    }
    if (RecordXMLReader.getBooleanAttribute("bold", tagElement)) {
      defineFont3.setBold(true);
    }
    if (RecordXMLReader.getBooleanAttribute("italic", tagElement)) {
      defineFont3.setItalic(true);
    }
    if (RecordXMLReader.getBooleanAttribute("smalltext", tagElement)) {
      defineFont3.setSmallText(true);
    }
    defineFont3.setLanguageCode(readLangCode(tagElement));
    return defineFont3;
  }

  private static Tag readDefineFontInfo(Element tagElement) {
    int fontId        = RecordXMLReader.getIntAttribute("fontid", tagElement);
    String fontName   = getStringAttribute("fontname", tagElement);
    List charElements = tagElement.elements();
    int arrayLength   = charElements.size();
    char[] codeTable  = new char[arrayLength];
    for (int i = 0; i < arrayLength; i++) {
      Element charElement = (Element) charElements.get(i);
      codeTable[i] = charElement.getText().charAt(0);
    }
    DefineFontInfo defineFontInfo = new DefineFontInfo(
        fontId, fontName, codeTable);
    if (RecordXMLReader.getBooleanAttribute("ansi", tagElement)) {
      defineFontInfo.setANSI(true);
    }
    if (RecordXMLReader.getBooleanAttribute("shiftjis", tagElement)) {
      defineFontInfo.setShiftJIS(true);
    }
    if (RecordXMLReader.getBooleanAttribute("bold", tagElement)) {
      defineFontInfo.setBold(true);
    }
    if (RecordXMLReader.getBooleanAttribute("italic", tagElement)) {
      defineFontInfo.setItalic(true);
    }
    if (RecordXMLReader.getBooleanAttribute("smalltext", tagElement)) {
      defineFontInfo.setSmallText(true);
    }
    return defineFontInfo;
  }

  private static Tag readDefineFontInfo2(Element tagElement) {
    int fontId        = RecordXMLReader.getIntAttribute("fontid", tagElement);
    String fontName   = getStringAttribute("fontname", tagElement);
    List charElements = tagElement.elements();
    int arrayLength   = charElements.size();
    char[] codeTable  = new char[arrayLength];
    for (int i = 0; i < arrayLength; i++) {
      Element charElement = (Element) charElements.get(i);
      codeTable[i] = charElement.getText().charAt(0);
    }
    DefineFontInfo2 defineFontInfo2 = new DefineFontInfo2(
        fontId, fontName, codeTable, readLangCode(tagElement));
    if (RecordXMLReader.getBooleanAttribute("bold", tagElement)) {
      defineFontInfo2.setBold(true);
    }
    if (RecordXMLReader.getBooleanAttribute("italic", tagElement)) {
      defineFontInfo2.setItalic(true);
    }
    if (RecordXMLReader.getBooleanAttribute("smalltext", tagElement)) {
      defineFontInfo2.setSmallText(true);
    }
    return defineFontInfo2;
  }

  private static Tag readDefineFontAlignment(Element tagElement) {
    byte thickness = -1;
    int fontId = RecordXMLReader.getIntAttribute("fontid", tagElement);
    String thicknessString = RecordXMLReader.getStringAttribute("thickness", tagElement);
    if (thicknessString.equals("thin")) {
      thickness = DefineFontAlignment.THIN;
    } else if (thicknessString.equals("medium")) {
      thickness = DefineFontAlignment.MEDIUM;
    } else if (thicknessString.equals("thick")) {
      thickness = DefineFontAlignment.THICK;
    }
    AlignmentZone[] alignmentZones = RecordXMLReader.readAlignmentZones(tagElement);
    DefineFontAlignment defineFontAlignment = new DefineFontAlignment(fontId, thickness, alignmentZones);
    return defineFontAlignment;
  }
  
  private static Tag readDefineMorphShape(Element tagElement) {
    int characterId                 = RecordXMLReader.getCharacterId(
        tagElement);
    Element startElement            = RecordXMLReader.getElement(
        "start", tagElement);
    Rect startBounds                = RecordXMLReader.readRect(
        RecordXMLReader.getElement("bounds", startElement));
    Element endElement              = RecordXMLReader.getElement(
        "end", tagElement);
    Rect endBounds                  = RecordXMLReader.readRect(
        RecordXMLReader.getElement("bounds", endElement));
    Element startShapeElement       = startElement.element("shape");
    Element endShapeElement         = endElement.element("shape");
    Shape startShape                = null;
    Shape endShape                  = null;
    MorphLineStyles morphLineStyles = null;
    MorphFillStyles morphFillStyles = null;
    if ((startShapeElement != null) && (endShapeElement != null)) {
      startShape        = RecordXMLReader.readShape(startShapeElement);
      endShape          = RecordXMLReader.readShape(
          RecordXMLReader.getElement("shape", endElement));
      morphLineStyles   = RecordXMLReader.readMorphLineStyles(tagElement);
      morphFillStyles   = RecordXMLReader.readMorphFillStyles(tagElement);
    }
    return new DefineMorphShape(
      characterId, startBounds, endBounds, morphFillStyles, morphLineStyles,
      startShape, endShape);
  }
  
  private static Tag readDefineMorphShape2(Element tagElement) {
    int characterId                 = RecordXMLReader.getCharacterId(
        tagElement);
    Element startElement            = RecordXMLReader.getElement(
        "start", tagElement);
    Rect startShapeBounds                = RecordXMLReader.readRect(
        RecordXMLReader.getElement("shapebounds", startElement));
    Rect startEdgeBounds                = RecordXMLReader.readRect(
        RecordXMLReader.getElement("edgebounds", startElement));
    Element endElement              = RecordXMLReader.getElement(
        "end", tagElement);
    Rect endShapeBounds                  = RecordXMLReader.readRect(
        RecordXMLReader.getElement("shapebounds", endElement));
    Rect endEdgeBounds                  = RecordXMLReader.readRect(
        RecordXMLReader.getElement("edgebounds", endElement));
    Element startShapeElement       = startElement.element("shape");
    Element endShapeElement         = endElement.element("shape");
    Shape startShape                = null;
    Shape endShape                  = null;
    MorphLineStyles morphLineStyles = null;
    MorphFillStyles morphFillStyles = null;
    if ((startShapeElement != null) && (endShapeElement != null)) {
      startShape        = RecordXMLReader.readShape(startShapeElement);
      endShape          = RecordXMLReader.readShape(
          RecordXMLReader.getElement("shape", endElement));
      morphLineStyles   = RecordXMLReader.readMorphLineStyles(tagElement);
      morphFillStyles   = RecordXMLReader.readMorphFillStyles(tagElement);
    }
    return new DefineMorphShape2(
      characterId, startShapeBounds, endShapeBounds, startEdgeBounds, endEdgeBounds, morphFillStyles, morphLineStyles,
      startShape, endShape);
  }

  private static Tag readDefineShape(Element tagElement) {
    int characterId       = RecordXMLReader.getCharacterId(tagElement);
    Rect shapeBounds      = RecordXMLReader.readRect(
        RecordXMLReader.getElement("bounds", tagElement));
    ShapeWithStyle shapes = RecordXMLReader.readShapeWithStyle(tagElement);
    return new DefineShape(characterId, shapeBounds, shapes);
  }

  private static Tag readDefineShape2(Element tagElement) {
    int characterId       = RecordXMLReader.getCharacterId(tagElement);
    Rect shapeBounds      = RecordXMLReader.readRect(
        RecordXMLReader.getElement("bounds", tagElement));
    ShapeWithStyle shapes = RecordXMLReader.readShapeWithStyle(tagElement);
    return new DefineShape2(characterId, shapeBounds, shapes);
  }

  private static Tag readDefineShape3(Element tagElement) {
    int characterId       = RecordXMLReader.getCharacterId(tagElement);
    Rect shapeBounds      = RecordXMLReader.readRect(
        RecordXMLReader.getElement("bounds", tagElement));
    ShapeWithStyle shapes = RecordXMLReader.readShapeWithStyle(tagElement);
    return new DefineShape3(characterId, shapeBounds, shapes);
  }

  private static Tag readDefineShape4(Element tagElement) {
    int characterId       = RecordXMLReader.getCharacterId(tagElement);
    Rect shapeBounds      = RecordXMLReader.readRect(
        RecordXMLReader.getElement("shapebounds", tagElement));
    Rect edgeBounds       = RecordXMLReader.readRect(
        RecordXMLReader.getElement("edgebounds", tagElement));
    ShapeWithStyle shapes = RecordXMLReader.readShapeWithStyle(tagElement);
    return new DefineShape4(characterId, shapeBounds, edgeBounds, shapes);
  }

  private static Tag readDefineSound(Element tagElement) {
    int characterId       = RecordXMLReader.getCharacterId(tagElement);
    byte format           = getSoundFormat(
        getStringAttribute("format", tagElement));
    byte rate             = getSoundRate(
        getStringAttribute("rate", tagElement));
    boolean is16BitSample = RecordXMLReader.getBooleanAttribute(
        "sample16bit", tagElement);
    boolean isStereo      = RecordXMLReader.getBooleanAttribute(
        "stereo", tagElement);
    long sampleCount      = RecordXMLReader.getLongAttribute(
        "samplecount", tagElement);
    byte[] soundData      = Base64.decode(tagElement.getText());
    return new DefineSound(
      characterId, format, rate, is16BitSample, isStereo, sampleCount, soundData);
  }

  private static Tag readDefineSprite(Element tagElement) {
    int characterId           = RecordXMLReader.getCharacterId(tagElement);
    List controlTagElements   = tagElement.elements();
    DefineSprite defineSprite = new DefineSprite(characterId);
    for (Iterator it = controlTagElements.iterator(); it.hasNext();) {
      Element controlTagElement = (Element) it.next();
      Tag tag;
      String tagName            = controlTagElement.getName();
      if (tagName.equals("showframe")) {
        tag = readShowFrame(controlTagElement);
      } else if (tagName.equals("placeobject")) {
        tag = readPlaceObject(controlTagElement);
      } else if (tagName.equals("placeobject2")) {
        tag = readPlaceObject2(controlTagElement);
      } else if (tagName.equals("placeobject3")) {
        tag = readPlaceObject3(controlTagElement);
      } else if (tagName.equals("removeobject")) {
        tag = readRemoveObject(controlTagElement);
      } else if (tagName.equals("removeobject2")) {
        tag = readRemoveObject2(controlTagElement);
      } else if (tagName.equals("startsound")) {
        tag = readStartSound(controlTagElement);
      } else if (tagName.equals("framelabel")) {
        tag = readFrameLabel(controlTagElement);
      } else if (tagName.equals("soundstreamhead")) {
        tag = readSoundStreamHead(controlTagElement);
      } else if (tagName.equals("soundstreamhead2")) {
        tag = readSoundStreamHead2(controlTagElement);
      } else if (tagName.equals("soundstreamblock")) {
        tag = readSoundStreamBlock(controlTagElement);
      } else if (tagName.equals("doaction")) {
        tag = readDoAction(controlTagElement);
      } else if (tagName.equals("doinitaction")) {
        tag = readDoInitAction(controlTagElement);
      } else if (tagName.equals("settabindex")) {
        tag = readSetTabIndex(controlTagElement);
      } else if (tagName.equals("videoframe")) {
        tag = readVideoFrame(controlTagElement);
      } else if (tagName.equals("unknowntag")) {
        tag = readUnknownTag(controlTagElement);
      } else if (tagName.equals("malformedtag")) {
        tag = readMalformedTag(controlTagElement);
      } else {
        throw new IllegalArgumentException(
          tagName + " tag not expected within definesprite!");
      }
      defineSprite.addControlTag(tag);
    }
    return defineSprite;
  }

  private static Tag readDefineText(Element tagElement) {
    int characterId          = RecordXMLReader.getCharacterId(tagElement);
    Rect textBounds          = RecordXMLReader.readRect(
        RecordXMLReader.getElement("bounds", tagElement));
    Matrix textMatrix        = RecordXMLReader.readMatrix("matrix", tagElement);
    TextRecord[] textRecords = RecordXMLReader.readTextRecords(tagElement);
    return new DefineText(characterId, textBounds, textMatrix, textRecords);
  }

  private static Tag readDefineText2(Element tagElement) {
    int characterId          = RecordXMLReader.getCharacterId(tagElement);
    Rect textBounds          = RecordXMLReader.readRect(
        RecordXMLReader.getElement("bounds", tagElement));
    Matrix textMatrix        = RecordXMLReader.readMatrix("matrix", tagElement);
    TextRecord[] textRecords = RecordXMLReader.readTextRecords(tagElement);
    return new DefineText2(characterId, textBounds, textMatrix, textRecords);
  }

  private static Tag readDefineVideoStream(Element tagElement) {
    int characterId   = RecordXMLReader.getCharacterId(tagElement);
    int numFrames     = RecordXMLReader.getIntAttribute(
        "numframes", tagElement);
    int width         = RecordXMLReader.getIntAttribute("width", tagElement);
    int height        = RecordXMLReader.getIntAttribute("height", tagElement);
    byte deblocking   = getVideoDeblocking(
        getStringAttribute("deblocking", tagElement));
    boolean smoothing = RecordXMLReader.getBooleanAttribute(
        "smoothing", tagElement);
    short codecId     = getVideoCodecId(
        getStringAttribute("codec", tagElement));
    return new DefineVideoStream(
      characterId, numFrames, width, height, deblocking, smoothing, codecId);
  }

  private static Tag readDoAction(Element tagElement) {
    DoAction doAction = new DoAction();
    RecordXMLReader.readActionBlock(doAction.getActions(), tagElement);
    return doAction;
  }

  private static Tag readDoInitAction(Element tagElement) {
    int spriteId              = RecordXMLReader.getIntAttribute(
        "spriteid", tagElement);
    DoInitAction doInitAction = new DoInitAction(spriteId);
    RecordXMLReader.readActionBlock(doInitAction.getInitActions(), tagElement);
    return doInitAction;
  }

  private static Tag readEnableDebugger(Element tagElement) {
    Attribute password = tagElement.attribute("password");
    return new EnableDebugger((password == null) ? null : password.getValue());
  }

  private static Tag readEnableDebugger2(Element tagElement) {
    Attribute password = tagElement.attribute("password");
    return new EnableDebugger2((password == null) ? null : password.getValue());
  }

  private static Tag readExportAssets(Element tagElement) {
    List mappingElements                  = tagElement.elements();
    int arrayLength                       = mappingElements.size();
    ExportAssets.ExportMapping[] mappings = new ExportAssets.ExportMapping[arrayLength];
    for (int i = 0; i < arrayLength; i++) {
      Element mappingElement = (Element) mappingElements.get(i);
      mappings[i] = new ExportAssets.ExportMapping(
          RecordXMLReader.getIntAttribute("charid", mappingElement),
          getStringAttribute("name", mappingElement));
    }
    return new ExportAssets(mappings);
  }

  private static Tag readFlashTypeSettings(Element tagElement) {
    FlashTypeSettings flashTypeSettings = new FlashTypeSettings(
        RecordXMLReader.getIntAttribute("textid", tagElement),
        RecordXMLReader.getBooleanAttribute("flashtype", tagElement));
    String gridFit                      = RecordXMLReader.getStringAttribute(
        "gridfit", tagElement);
    if (gridFit.equals("none")) {
      flashTypeSettings.setGridFit(FlashTypeSettings.GRID_FIT_NONE);
    } else if (gridFit.equals("pixel")) {
      flashTypeSettings.setGridFit(FlashTypeSettings.GRID_FIT_PIXEL);
    } else if (gridFit.equals("subpixel")) {
      flashTypeSettings.setGridFit(FlashTypeSettings.GRID_FIT_SUBPIXEL);
    }
    flashTypeSettings.setThickness(
      RecordXMLReader.getFloatAttribute("thickness", tagElement));
    flashTypeSettings.setSharpness(
      RecordXMLReader.getFloatAttribute("sharpness", tagElement));
    return flashTypeSettings;
  }

  private static Tag readFrameLabel(Element tagElement) {
    return new FrameLabel(
      getStringAttribute("name", tagElement),
      RecordXMLReader.getBooleanAttribute("namedanchor", tagElement));
  }

  private static Tag readFreeCharacter(Element tagElement) {
    return new FreeCharacter(RecordXMLReader.getCharacterId(tagElement));
  }

  private static Tag readImportAssets(Element tagElement) {
    List mappingElements                  = tagElement.elements();
    String url                            = getStringAttribute(
        "url", tagElement);
    int arrayLength                       = mappingElements.size();
    ImportAssets.ImportMapping[] mappings = new ImportAssets.ImportMapping[arrayLength];
    for (int i = 0; i < arrayLength; i++) {
      Element mappingElement = (Element) mappingElements.get(i);
      mappings[i] = new ImportAssets.ImportMapping(
          getStringAttribute("name", mappingElement),
          RecordXMLReader.getIntAttribute("charid", mappingElement));
    }
    return new ImportAssets(url, mappings);
  }

  private static Tag readImportAssets2(Element tagElement) {
    List mappingElements                   = tagElement.elements();
    String url                             = getStringAttribute(
        "url", tagElement);
    int arrayLength                        = mappingElements.size();
    ImportAssets2.ImportMapping[] mappings = new ImportAssets2.ImportMapping[arrayLength];
    for (int i = 0; i < arrayLength; i++) {
      Element mappingElement = (Element) mappingElements.get(i);
      mappings[i] = new ImportAssets2.ImportMapping(
          getStringAttribute("name", mappingElement),
          RecordXMLReader.getIntAttribute("charid", mappingElement));
    }
    return new ImportAssets2(url, mappings);
  }

  private static Tag readJPEGTables(Element tagElement) {
    Element jpegDataElement = RecordXMLReader.getElement(
        "jpegdata", tagElement);
    byte[] jpegData         = Base64.decode(jpegDataElement.getText());
    return new JPEGTables(jpegData);
  }

  private static LangCode readLangCode(Element parentElement) {
    String language   = getStringAttribute("language", parentElement);
    LangCode langCode;
    if (language.equals("latin")) {
      langCode = new LangCode(LangCode.LATIN);
    } else if (language.equals("undefined")) {
      langCode = new LangCode(LangCode.UNDEFINED);
    } else if (language.equals("japanese")) {
      langCode = new LangCode(LangCode.JAPANESE);
    } else if (language.equals("korean")) {
      langCode = new LangCode(LangCode.KOREAN);
    } else if (language.equals("simpchinese")) {
      langCode = new LangCode(LangCode.SIMPLIFIED_CHINESE);
    } else if (language.equals("tradchinese")) {
      langCode = new LangCode(LangCode.TRADITIONAL_CHINESE);
    } else {
      throw new IllegalArgumentException("Illegal language: " + language);
    }
    return langCode;
  }

  private static Tag readMalformedTag(Element tagElement) {
    throw new IllegalArgumentException(
      "'malformedtag' element encountered! Your source SWF is probably corrupted.");
  }

  private static Tag readMetadata(Element tagElement) {
    return new Metadata(tagElement.getText());
  }

  private static Tag readPlaceObject(Element tagElement) {
    int characterId               = RecordXMLReader.getCharacterId(tagElement);
    int depth                     = RecordXMLReader.getIntAttribute(
        "depth", tagElement);
    Matrix matrix                 = RecordXMLReader.readMatrix(
        RecordXMLReader.getElement("matrix", tagElement));
    Element colorTransFormElement = tagElement.element("cxform");
    CXform colorTransform         = null;
    if (colorTransFormElement != null) {
      colorTransform = RecordXMLReader.readCXform(colorTransFormElement);
    }
    return new PlaceObject(characterId, depth, matrix, colorTransform);
  }

  private static Tag readPlaceObject2(Element tagElement) {
    int depth                 = RecordXMLReader.getIntAttribute(
        "depth", tagElement);
    PlaceObject2 placeObject2 = new PlaceObject2(depth);
    Attribute characterId     = tagElement.attribute("charid");
    if (characterId != null) {
      placeObject2.setCharacterId(Integer.parseInt(characterId.getValue()));
    }
    Attribute name = tagElement.attribute("name");
    if (name != null) {
      placeObject2.setName(name.getValue());
    }
    if (RecordXMLReader.getBooleanAttribute("move", tagElement)) {
      placeObject2.setMove();
    }
    Element matrix = tagElement.element("matrix");
    if (matrix != null) {
      placeObject2.setMatrix(RecordXMLReader.readMatrix(matrix));
    }
    Element colorTransform = tagElement.element("cxformwithalpha");
    if (colorTransform != null) {
      placeObject2.setColorTransform(
        RecordXMLReader.readCXformWithAlpha(colorTransform));
    }
    Attribute ratio = tagElement.attribute("ratio");
    if (ratio != null) {
      placeObject2.setRatio(Integer.parseInt(ratio.getValue()));
    }
    Attribute clipDepth = tagElement.attribute("clipdepth");
    if (clipDepth != null) {
      placeObject2.setClipDepth(Integer.parseInt(clipDepth.getValue()));
    }
    Element clipActions = tagElement.element("clipactions");
    if (clipActions != null) {
      placeObject2.setClipActions(RecordXMLReader.readClipActions(clipActions));
    }
    return placeObject2;
  }

  private static Tag readPlaceObject3(Element tagElement) {
    int depth                 = RecordXMLReader.getIntAttribute(
        "depth", tagElement);
    PlaceObject3 placeObject3 = new PlaceObject3(depth);
    Attribute characterId     = tagElement.attribute("charid");
    if (characterId != null) {
      placeObject3.setCharacterId(Integer.parseInt(characterId.getValue()));
    }
    Attribute name = tagElement.attribute("name");
    if (name != null) {
      placeObject3.setName(name.getValue());
    }
    if (RecordXMLReader.getBooleanAttribute("move", tagElement)) {
      placeObject3.setMove();
    }
    Element matrix = tagElement.element("matrix");
    if (matrix != null) {
      placeObject3.setMatrix(RecordXMLReader.readMatrix(matrix));
    }
    Element colorTransform = tagElement.element("cxformwithalpha");
    if (colorTransform != null) {
      placeObject3.setColorTransform(
        RecordXMLReader.readCXformWithAlpha(colorTransform));
    }
    Attribute ratio = tagElement.attribute("ratio");
    if (ratio != null) {
      placeObject3.setRatio(Integer.parseInt(ratio.getValue()));
    }
    Attribute clipDepth = tagElement.attribute("clipdepth");
    if (clipDepth != null) {
      placeObject3.setClipDepth(Integer.parseInt(clipDepth.getValue()));
    }
    Element clipActions = tagElement.element("clipactions");
    if (clipActions != null) {
      placeObject3.setClipActions(RecordXMLReader.readClipActions(clipActions));
    }
    Attribute blendMode = tagElement.attribute("blendmode");
    if (blendMode != null) {
      placeObject3.setBlendMode(
        BlendMode.getFromDescription(blendMode.getValue()));
    }
    placeObject3.setCacheAsBitmap(
      RecordXMLReader.getBooleanAttribute("cacheasbitmap", tagElement));
    Element filters = tagElement.element("filters");
    if (filters != null) {
      placeObject3.setFilters(RecordXMLReader.readFilters(filters));
    }
    return placeObject3;
  }

  private static Tag readProtect(Element tagElement) {
    Attribute password = tagElement.attribute("password");
    return new Protect((password == null) ? null : password.getValue());
  }

  private static Tag readRemoveObject(Element tagElement) {
    return new RemoveObject(
      RecordXMLReader.getCharacterId(tagElement),
      RecordXMLReader.getIntAttribute("depth", tagElement));
  }

  private static Tag readRemoveObject2(Element tagElement) {
    return new RemoveObject2(
      RecordXMLReader.getIntAttribute("depth", tagElement));
  }

  private static Tag readScale9Grid(Element tagElement) {
    int characterId       = RecordXMLReader.getIntAttribute(
        "charid", tagElement);
    Rect grid             = RecordXMLReader.readRect(
        RecordXMLReader.getElement("grid", tagElement));
    Scale9Grid scale9Grid = new Scale9Grid(characterId, grid);
    return scale9Grid;
  }

  private static Tag readScriptLimits(Element tagElement) {
    return new ScriptLimits(
      RecordXMLReader.getIntAttribute("maxrecursiondepth", tagElement),
      RecordXMLReader.getIntAttribute("scripttimeout", tagElement));
  }

  private static Tag readSetTabIndex(Element tagElement) {
    return new SetTabIndex(
      RecordXMLReader.getIntAttribute("depth", tagElement),
      RecordXMLReader.getIntAttribute("tabindex", tagElement));
  }

  private static Tag readShowFrame(Element tagElement) {
    return new ShowFrame();
  }

  private static Tag readSoundStreamBlock(Element tagElement) {
    return new SoundStreamBlock(
      Base64.decode(
        RecordXMLReader.getElement("streamsounddata", tagElement).getText()));
  }

  private static Tag readSoundStreamHead(Element tagElement) {
    byte format                     = getSoundFormat(
        getStringAttribute("streamformat", tagElement));
    byte rate                       = getSoundRate(
        getStringAttribute("streamrate", tagElement));
    boolean stereo                  = RecordXMLReader.getBooleanAttribute(
        "streamstereo", tagElement);
    int sampleCount                 = RecordXMLReader.getIntAttribute(
        "streamsamplecount", tagElement);
    SoundStreamHead soundStreamHead = new SoundStreamHead(
        format, rate, stereo, sampleCount);
    soundStreamHead.setPlaybackRate(
      getSoundRate(getStringAttribute("playbackrate", tagElement)));
    soundStreamHead.setPlaybackStereo(
      RecordXMLReader.getBooleanAttribute("playbackstereo", tagElement));
    if (soundStreamHead.getStreamFormat() == SoundStreamHead.FORMAT_MP3) {
      soundStreamHead.setLatencySeek(
        RecordXMLReader.getShortAttribute("latencyseek", tagElement));
    }
    return soundStreamHead;
  }

  private static Tag readSoundStreamHead2(Element tagElement) {
    byte format                       = getSoundFormat(
        getStringAttribute("streamformat", tagElement));
    byte rate                         = getSoundRate(
        getStringAttribute("streamrate", tagElement));
    boolean is16BitSample             = RecordXMLReader.getBooleanAttribute(
        "streamsample16bit", tagElement);
    boolean stereo                    = RecordXMLReader.getBooleanAttribute(
        "streamstereo", tagElement);
    int sampleCount                   = RecordXMLReader.getIntAttribute(
        "streamsamplecount", tagElement);
    SoundStreamHead2 soundStreamHead2 = new SoundStreamHead2(
        format, rate, is16BitSample, stereo, sampleCount);
    soundStreamHead2.setPlaybackRate(
      getSoundRate(getStringAttribute("playbackrate", tagElement)));
    soundStreamHead2.setPlayback16BitSample(
      RecordXMLReader.getBooleanAttribute("playbacksample16bit", tagElement));
    soundStreamHead2.setPlaybackStereo(
      RecordXMLReader.getBooleanAttribute("playbackstereo", tagElement));
    if (soundStreamHead2.getStreamFormat() == SoundStreamHead.FORMAT_MP3) {
      soundStreamHead2.setLatencySeek(
        RecordXMLReader.getShortAttribute("latencyseek", tagElement));
    }
    return soundStreamHead2;
  }

  private static Tag readStartSound(Element tagElement) {
    return new StartSound(
      RecordXMLReader.getIntAttribute("soundid", tagElement),
      RecordXMLReader.readSoundInfo(tagElement));
  }

  private static Tag readVideoFrame(Element tagElement) {
    int streamId     = RecordXMLReader.getIntAttribute("streamid", tagElement);
    int frameNum     = RecordXMLReader.getIntAttribute("framenum", tagElement);
    byte[] videoData = Base64.decode(tagElement.getText());
    return new VideoFrame(streamId, frameNum, videoData);
  }
}
