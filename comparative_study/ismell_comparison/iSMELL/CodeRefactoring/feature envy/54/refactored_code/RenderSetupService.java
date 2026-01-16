public class RenderSetupService {
    public Image setupRenderImage(Camera camera, int[] imagePixel, MemoryImageSource imageSource, int imageWidth, int imageHeight, int samplesPerPixel) {
        Dimension dim = camera.getSize();
        if (imagePixel == null || imageWidth != dim.width || imageHeight != dim.height) {
            imageWidth = dim.width;
            imageHeight = dim.height;
            imagePixel = new int[imageWidth * imageHeight];
            imageSource = new MemoryImageSource(imageWidth, imageHeight, imagePixel, 0, imageWidth);
            imageSource.setAnimated(true);
        }
        return Toolkit.getDefaultToolkit().createImage(imageSource);
    }
}
