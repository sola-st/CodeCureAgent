public class TriangleRenderer {

    private int[] pixel;
    private int[] zbuffer;
    private int width;
    private double clip;
    private int mode;
    private RGBColor color;

    public TriangleRenderer(int[] pixel, int[] zbuffer, int width, double clip, int mode, RGBColor color) {
        this.pixel = pixel;
        this.zbuffer = zbuffer;
        this.width = width;
        this.clip = clip;
        this.mode = mode;
        this.color = color;
    }

    public void renderTriangle(Vec2 pos1, double zf1, Vec2 pos2, double zf2, Vec2 pos3, double zf3, int height) {
        Triangle triangle = new Triangle(pos1, zf1, pos2, zf2, pos3, zf3);
        triangle.sortVerticesByY();

        TriangleRendererHelper rendererHelper = new TriangleRendererHelper(pixel, zbuffer, width, clip, mode, color);
        rendererHelper.renderTriangle(triangle, height);
    }
}

public class TriangleRendererHelper {

    private int[] pixel;
    private int[] zbuffer;
    private int width;
    private double clip;
    private int mode;
    private RGBColor color;

    public TriangleRendererHelper(int[] pixel, int[] zbuffer, int width, double clip, int mode, RGBColor color) {
        this.pixel = pixel;
        this.zbuffer = zbuffer;
        this.width = width;
        this.clip = clip;
        this.mode = mode;
        this.color = color;
    }

    public void renderTriangle(Triangle triangle, int height) {
        // Calculate variables for rendering
        // ...

        // Render the triangle
        // ...
    }
}

public class Triangle {

    private Vec2 pos1;
    private double zf1;
    private Vec2 pos2;
    private double zf2;
    private Vec2 pos3;
    private double zf3;

    public Triangle(Vec2 pos1, double zf1, Vec2 pos2, double zf2, Vec2 pos3, double zf3) {
        this.pos1 = pos1;
        this.zf1 = zf1;
        this.pos2 = pos2;
        this.zf2 = zf2;
        this.pos3 = pos3;
        this.zf3 = zf3;
    }

    public void sortVerticesByY() {
        // Sort vertices by y-coordinate
        // ...
    }
}