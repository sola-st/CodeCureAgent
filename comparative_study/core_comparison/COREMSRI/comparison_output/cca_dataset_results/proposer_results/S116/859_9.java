package com.openhtmltopdf.java2d.api;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * PageProcessor to render everything to buffered images
 */
public class BufferedImagePageProcessor implements FSPageProcessor {
	private final double scale;
	private final int imageType;

	private List<BufferedImagePage> pages = new ArrayList<>();

	private class BufferedImagePage implements FSPage {
		BufferedImage image;

		BufferedImagePage(BufferedImage image) {
			this.image = image;
		}

		@Override
		public Graphics2D getGraphics() {
			Graphics2D graphics = image.createGraphics();
			if (image.getColorModel().hasAlpha()) {
				graphics.clearRect(0, 0, image.getWidth(), image.getHeight());
			} else {
				graphics.setColor(Color.WHITE);
				graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
			}

			/*
			 * Apply the scale on the bitmap
			 */
			graphics.scale(scale, scale);
			return graphics;
		}
	}

	/**
	 *
	 * @param imageType
	 *            Type of the BufferedImage, e.g. BufferedImage#TYPE_INT_ARGB
	 * @param scale
	 *            scale factor. You can control what resolution of the images
	 *            you want
	 */
	public BufferedImagePageProcessor(int imageType, double scale) {
		this.imageType = imageType;
		this.scale = scale;
	}

	@Override
	public FSPage createPage(int zeroBasedPageNumber, int width, int height) {
		BufferedImage image = new BufferedImage((int) (width * scale), (int) (height * scale), imageType);
		BufferedImagePage bufferedImagePage = new BufferedImagePage(image);
		pages.add(bufferedImagePage);
		return bufferedImagePage;
	}

	@Override
	public void finishPage(FSPage pg) {
		/*
		 * We don't need to do anything here.
		 */
	}

	public List<BufferedImage> getPageImages() {
		List<BufferedImage> images = new ArrayList<>();
		for (BufferedImagePage page : pages) {
			images.add(page.image);
		}
		return images;
	}
}
