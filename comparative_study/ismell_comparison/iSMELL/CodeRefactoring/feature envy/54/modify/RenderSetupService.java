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

    public Fragment[] initializeFragments(int width, int height, Fragment BACKGROUND_FRAGMENT) {
        Fragment[] fragment = new Fragment[width * height];
        Arrays.fill(fragment, BACKGROUND_FRAGMENT);
        return fragment;
    }

    public RowLock[] initializeRowLocks(int height) {
        RowLock[] lock = new RowLock[height];
        for (int i = 0; i < lock.length; i++) {
            lock[i] = new RowLock();
        }
        return lock;
    }
}
