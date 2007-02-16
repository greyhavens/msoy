/*
 * JSwiff is an open source Java API for Macromedia Flash file generation
 * and manipulation
 *
 * Copyright (C) 2004-2005 Ralf Terdic (contact@jswiff.com)
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
package com.jswiff.swfrecords.tags;

import com.jswiff.io.InputBitStream;
import com.jswiff.io.OutputBitStream;

import java.io.IOException;


/**
 * This tag provides a single frame of streaming video data.  The format of the
 * video stream must be defined in a preceding <code>DefineVideoStream</code>
 * tag. The video frame rate is limited by the SWF frame rate, as an SWF frame
 * can contain at most one video frame.
 *
 * @see DefineVideoStream
 * @since SWF 6
 */
public final class VideoFrame extends Tag {
	private int streamId;
	private int frameNum;
	private byte[] videoData;

	/**
	 * Creates a new VideoFrame tag. Provide the character ID of the video
	 * stream, the sequential frame number and the raw video data contained in
	 * this frame.
	 *
	 * @param streamId character ID of video stream
	 * @param frameNum frame number
	 * @param videoData raw video frame data
	 */
	public VideoFrame(int streamId, int frameNum, byte[] videoData) {
		code			   = TagConstants.VIDEO_FRAME;
		this.streamId	   = streamId;
		this.frameNum	   = frameNum;
		this.videoData     = videoData;
	}

	VideoFrame() {
		// empty
	}

	/**
	 * Sets the frame number. Frame numbers are sequential and start at 0.
	 *
	 * @param frameNum sequential frame number
	 */
	public void setFrameNum(int frameNum) {
		this.frameNum = frameNum;
	}

	/**
	 * Returns the frame number. Frame numbers are sequential and start at 0.
	 *
	 * @return sequential frame number
	 */
	public int getFrameNum() {
		return frameNum;
	}

	/**
	 * Sets the character ID of the video stream this frame belongs to.
	 *
	 * @param streamId video stream character ID
	 */
	public void setStreamId(int streamId) {
		this.streamId = streamId;
	}

	/**
	 * Returns the character ID of the video stream this frame belongs to.
	 *
	 * @return video stream character ID
	 */
	public int getStreamId() {
		return streamId;
	}

	/**
	 * Specifies the raw data contained in this video frame.
	 *
	 * @param videoData video frame data (as byte array)
	 */
	public void setVideoData(byte[] videoData) {
		this.videoData = videoData;
	}

	/**
	 * Returns the raw data contained in this video frame.
	 *
	 * @return video frame data (as byte array)
	 */
	public byte[] getVideoData() {
		return videoData;
	}

	protected void writeData(OutputBitStream outStream)
		throws IOException {
		outStream.writeUI16(streamId);
		outStream.writeUI16(frameNum);
		outStream.writeBytes(videoData);
	}

	void setData(byte[] data) throws IOException {
		InputBitStream inStream = new InputBitStream(data);
		streamId	  = inStream.readUI16();
		frameNum	  = inStream.readUI16();
		int videoDataLength = data.length - 4;
		videoData     = new byte[videoDataLength];
		System.arraycopy(data, 4, videoData, 0, videoDataLength);
	}
}
