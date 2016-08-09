package com.twelvemonkeys.imageio.plugins.jpeg.lossless;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferUShort;
import java.io.IOException;

/**
 * This class provides the conversion of a byte buffer 
 * containing a JPEGLossless to an BufferedImage. 
 * Therefore it uses the rii-mango JPEGLosslessDecoder
 * Library ( https://github.com/rii-mango/JPEGLosslessDecoder )
 * 
 * Take care, that only the following lossless formats are supported
 * 1.2.840.10008.1.2.4.57 JPEG Lossless, Nonhierarchical (Processes 14)
 * 1.2.840.10008.1.2.4.70 JPEG Lossless, Nonhierarchical (Processes 14 [Selection 1])
 * 
 * Currently the following conversions are supported
 * 	- 24Bit, RGB       -> BufferedImage.TYPE_INT_RGB
 *  -  8Bit, Grayscale -> BufferedImage.TYPE_BYTE_GRAY
 *  - 16Bit, Grayscale -> BufferedImage.TYPE_USHORT_GRAY
 * 
 * @author Hermann Kroll
 *
 */
public class JPEGLosslessDecoderWrapper {

	/**
	 * Converts a byte buffer (containing a jpeg lossless) 
	 * to an Java BufferedImage
	 * Currently the following conversions are supported
	 * 	- 24Bit, RGB       -> BufferedImage.TYPE_INT_RGB
	 *  -  8Bit, Grayscale -> BufferedImage.TYPE_BYTE_GRAY
	 *  - 16Bit, Grayscale -> BufferedImage.TYPE_USHORT_GRAY
	 * 
	 * @param data byte buffer which contains a jpeg lossless
	 * @return if successfully a BufferedImage is returned
	 * @throws IOException is thrown if the decoder failed or a conversion is not supported
	 */
	public BufferedImage readImage(byte[] data) throws IOException{
		JPEGLosslessDecoder decoder = new JPEGLosslessDecoder(data);
 
		
		int[][] decoded = decoder.decode();
		int width = decoder.getDimX();
		int height = decoder.getDimY();
		
		if(decoder.getNumComponents() == 1){
			switch(decoder.getPrecision())
			{
			case 8:
				return read8Bit1ComponentGrayScale(decoded, width, height);
			case 16:
				return read16Bit1ComponentGrayScale(decoded, width, height);
			default:
				throw new IOException("JPEG Lossless with " + decoder.getPrecision() + " bit precision and 1 component cannot be decoded");
			}
		}
		//rgb
		if(decoder.getNumComponents() == 3){
			switch(decoder.getPrecision())
			{
			case 24:
				return read24Bit3ComponentRGB(decoded, width, height);

			default:
				throw new IOException("JPEG Lossless with " + decoder.getPrecision() + " bit precision and 3 components cannot be decoded");
			}
		}
		
		throw new IOException("JPEG Lossless with " + decoder.getPrecision() + " bit precision and " + decoder.getNumComponents() + " component(s) cannot be decoded");
		
	}
	
	/**
	 * converts the decoded buffer into a BufferedImage
	 * precision: 16 bit, componentCount = 1
	 * @param decoded data buffer
	 * @param width of the image
	 * @param height of the image
	 * @return a BufferedImage.TYPE_USHORT_GRAY
	 */
	private BufferedImage read16Bit1ComponentGrayScale(int[][] decoded, int width, int height){
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_GRAY);
		short[] imageBuffer = ((DataBufferUShort) image.getRaster().getDataBuffer()).getData();

		for(int i = 0; i < imageBuffer.length; i++){
			imageBuffer[i] =  (short)decoded[0][i];
		}
		return image;
	}
	/**
	 * converts the decoded buffer into a BufferedImage
	 * precision: 8 bit, componentCount = 1
	 * @param decoded data buffer
	 * @param width of the image
	 * @param height of the image
	 * @return a BufferedImage.TYPE_BYTE_GRAY
	 */
	private BufferedImage read8Bit1ComponentGrayScale(int[][] decoded, int width, int height){
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		byte[] imageBuffer = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();

		for(int i = 0; i < imageBuffer.length; i++){
			imageBuffer[i] =  (byte)decoded[0][i];
		}
		return image;
	}
	
	/**
	 * converts the decoded buffer into a BufferedImage
	 * precision: 24 bit, componentCount = 3
	 * @param decoded data buffer
	 * @param width of the image
	 * @param height of the image
	 * @return a BufferedImage.TYPE_INT_RGB
	 */
	private BufferedImage read24Bit3ComponentRGB(int[][] decoded, int width, int height){
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		int[] imageBuffer = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

		for(int i = 0; i < imageBuffer.length; i++){
			//convert to RGB
			imageBuffer[i] =  (decoded[0][i] << 16) | (decoded[1][i] << 8) | (decoded[2][i]);
		}
		return image;
	}
	
}
