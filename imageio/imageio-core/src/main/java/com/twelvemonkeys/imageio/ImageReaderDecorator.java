package com.twelvemonkeys.imageio;

import javax.imageio.*;
import javax.imageio.event.IIOReadProgressListener;
import javax.imageio.event.IIOReadUpdateListener;
import javax.imageio.event.IIOReadWarningListener;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.ImageReaderSpi;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.*;

public abstract class ImageReaderDecorator extends ImageReader {

	protected final ImageReader delegate;

	protected ImageReaderDecorator(final ImageReader delegate) {
		super(delegate.getOriginatingProvider());
		this.delegate = delegate;
	}

	@Override
	public String getFormatName() throws IOException {
		return delegate.getFormatName();
	}

	@Override
	public ImageReaderSpi getOriginatingProvider() {
		return delegate.getOriginatingProvider();
	}

	@Override
	public void setInput(Object input, boolean seekForwardOnly, boolean ignoreMetadata) {
		delegate.setInput(input, seekForwardOnly, ignoreMetadata);
	}

	@Override
	public void setInput(Object input, boolean seekForwardOnly) {
		delegate.setInput(input, seekForwardOnly);
	}

	@Override
	public void setInput(Object input) {
		delegate.setInput(input);
	}

	@Override
	public Object getInput() {
		return delegate.getInput();
	}

	@Override
	public boolean isSeekForwardOnly() {
		return delegate.isSeekForwardOnly();
	}

	@Override
	public boolean isIgnoringMetadata() {
		return delegate.isIgnoringMetadata();
	}

	@Override
	public int getMinIndex() {
		return delegate.getMinIndex();
	}

	@Override
	public Locale[] getAvailableLocales() {
		return delegate.getAvailableLocales();
	}

	@Override
	public void setLocale(Locale locale) {
		delegate.setLocale(locale);
	}

	@Override
	public Locale getLocale() {
		return delegate.getLocale();
	}

	@Override
	public int getNumImages(boolean allowSearch) throws IOException {
		return delegate.getNumImages(allowSearch);
	}

	@Override
	public int getWidth(int imageIndex) throws IOException {
		return delegate.getWidth(imageIndex);
	}

	@Override
	public int getHeight(int imageIndex) throws IOException {
		return delegate.getHeight(imageIndex);
	}

	@Override
	public boolean isRandomAccessEasy(int imageIndex) throws IOException {
		return delegate.isRandomAccessEasy(imageIndex);
	}

	@Override
	public float getAspectRatio(int imageIndex) throws IOException {
		return delegate.getAspectRatio(imageIndex);
	}

	@Override
	public ImageTypeSpecifier getRawImageType(int imageIndex) throws IOException {
		return delegate.getRawImageType(imageIndex);
	}

	@Override
	public Iterator<ImageTypeSpecifier> getImageTypes(int imageIndex) throws IOException {
		return delegate.getImageTypes(imageIndex);
	}

	@Override
	public ImageReadParam getDefaultReadParam() {
		return delegate.getDefaultReadParam();
	}

	@Override
	public IIOMetadata getStreamMetadata() throws IOException {
		return delegate.getStreamMetadata();
	}

	@Override
	public IIOMetadata getStreamMetadata(String formatName, Set<String> nodeNames) throws IOException {
		return delegate.getStreamMetadata(formatName, nodeNames);
	}

	@Override
	public IIOMetadata getImageMetadata(int imageIndex) throws IOException {
		return delegate.getImageMetadata(imageIndex);
	}

	@Override
	public IIOMetadata getImageMetadata(int imageIndex, String formatName, Set<String> nodeNames) throws IOException {
		return delegate.getImageMetadata(imageIndex, formatName, nodeNames);
	}

	@Override
	public BufferedImage read(int imageIndex) throws IOException {
		return delegate.read(imageIndex);
	}

	@Override
	public BufferedImage read(int imageIndex, ImageReadParam param) throws IOException {
		return delegate.read(imageIndex, param);
	}

	@Override
	public IIOImage readAll(int imageIndex, ImageReadParam param) throws IOException {
		return delegate.readAll(imageIndex, param);
	}

	@Override
	public Iterator<IIOImage> readAll(Iterator<? extends ImageReadParam> params) throws IOException {
		return delegate.readAll(params);
	}

	@Override
	public boolean canReadRaster() {
		return delegate.canReadRaster();
	}

	@Override
	public Raster readRaster(int imageIndex, ImageReadParam param) throws IOException {
		return delegate.readRaster(imageIndex, param);
	}

	@Override
	public boolean isImageTiled(int imageIndex) throws IOException {
		return delegate.isImageTiled(imageIndex);
	}

	@Override
	public int getTileWidth(int imageIndex) throws IOException {
		return delegate.getTileWidth(imageIndex);
	}

	@Override
	public int getTileHeight(int imageIndex) throws IOException {
		return delegate.getTileHeight(imageIndex);
	}

	@Override
	public int getTileGridXOffset(int imageIndex) throws IOException {
		return delegate.getTileGridXOffset(imageIndex);
	}

	@Override
	public int getTileGridYOffset(int imageIndex) throws IOException {
		return delegate.getTileGridYOffset(imageIndex);
	}

	@Override
	public BufferedImage readTile(int imageIndex, int tileX, int tileY) throws IOException {
		return delegate.readTile(imageIndex, tileX, tileY);
	}

	@Override
	public Raster readTileRaster(int imageIndex, int tileX, int tileY) throws IOException {
		return delegate.readTileRaster(imageIndex, tileX, tileY);
	}

	@Override
	public RenderedImage readAsRenderedImage(int imageIndex, ImageReadParam param) throws IOException {
		return delegate.readAsRenderedImage(imageIndex, param);
	}

	@Override
	public boolean readerSupportsThumbnails() {
		return delegate.readerSupportsThumbnails();
	}

	@Override
	public boolean hasThumbnails(int imageIndex) throws IOException {
		return delegate.hasThumbnails(imageIndex);
	}

	@Override
	public int getNumThumbnails(int imageIndex) throws IOException {
		return delegate.getNumThumbnails(imageIndex);
	}

	@Override
	public int getThumbnailWidth(int imageIndex, int thumbnailIndex) throws IOException {
		return delegate.getThumbnailWidth(imageIndex, thumbnailIndex);
	}

	@Override
	public int getThumbnailHeight(int imageIndex, int thumbnailIndex) throws IOException {
		return delegate.getThumbnailHeight(imageIndex, thumbnailIndex);
	}

	@Override
	public BufferedImage readThumbnail(int imageIndex, int thumbnailIndex) throws IOException {
		return delegate.readThumbnail(imageIndex, thumbnailIndex);
	}

	@Override
	public synchronized void abort() {
		delegate.abort();
	}

	@Override
	public void addIIOReadWarningListener(IIOReadWarningListener listener) {
		delegate.addIIOReadWarningListener(listener);
	}

	@Override
	public void removeIIOReadWarningListener(IIOReadWarningListener listener) {
		delegate.removeIIOReadWarningListener(listener);
	}

	@Override
	public void removeAllIIOReadWarningListeners() {
		delegate.removeAllIIOReadWarningListeners();
	}

	@Override
	public void addIIOReadProgressListener(IIOReadProgressListener listener) {
		delegate.addIIOReadProgressListener(listener);
	}

	@Override
	public void removeIIOReadProgressListener(IIOReadProgressListener listener) {
		delegate.removeIIOReadProgressListener(listener);
	}

	@Override
	public void removeAllIIOReadProgressListeners() {
		delegate.removeAllIIOReadProgressListeners();
	}

	@Override
	public void addIIOReadUpdateListener(IIOReadUpdateListener listener) {
		delegate.addIIOReadUpdateListener(listener);
	}

	@Override
	public void removeIIOReadUpdateListener(IIOReadUpdateListener listener) {
		delegate.removeIIOReadUpdateListener(listener);
	}

	@Override
	public void removeAllIIOReadUpdateListeners() {
		delegate.removeAllIIOReadUpdateListeners();
	}

	@Override
	public void reset() {
		delegate.reset();
	}

	@Override
	public void dispose() {
		delegate.dispose();
	}
}
