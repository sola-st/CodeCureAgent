private class TriangleRenderer {
    private int mode;
    private RGBColor color;
    private int[] pixel;
    private int[] zbuffer;
    private int width;
    private double clip;
    private int clipDist;
    private int red, green, blue;

    public TriangleRenderer(int mode, RGBColor color, int[] pixel, int[] zbuffer, int width, double clip) {
        this.mode = mode;
        this.color = color;
        this.pixel = pixel;
        this.zbuffer = zbuffer;
        this.width = width;
        this.clip = clip;

        this.clipDist = (int)(clip*65535.0);
        if(mode==MODE_COPY) {
            this.red = this.green = this.blue = 0;
        } else {
            this.red = (int)(color.getRed()*255.0f);
            this.green = (int)(color.getGreen()*255.0f);
            this.blue = (int)(color.getBlue()*255.0f);
        }
    }

    public void renderFlatTriangle(Vec2 pos1, double zf1, Vec2 pos2, double zf2, Vec2 pos3, double zf3) {
        int x1, y1, z1, x2, y2, z2, x3, y3, z3;
        //...

        if(pos1.y<=pos2.y&&pos1.y<=pos3.y) {
            assignCoordinates(pos1, zf1, pos2, zf2, pos3, zf3);
        } else if(pos2.y<=pos1.y&&pos2.y<=pos3.y) {
            assignCoordinates(pos2, zf2, pos1, zf1, pos3, zf3);
        } else {
            assignCoordinates(pos3, zf3, pos1, zf1, pos2, zf2);
        }
        //...
    }

    private void assignCoordinates(Vec2 pos1, double zf1, Vec2 pos2, double zf2, Vec2 pos3, double zf3) {
        x1=((int)pos1.x)<<16;
        y1=((int)pos1.y);
        z1=(int)(zf1*65535.0);
        if(pos2.y<pos3.y) {
            x2=((int)pos2.x)<<16;
            y2=((int)pos2.y);
            z2=(int)(zf2*65535.0);
            x3=((int)pos3.x)<<16;
            y3=((int)pos3.y);
            z3=(int)(zf3*65535.0);
        } else {
            x2=((int)pos3.x)<<16;
            y2=((int)pos3.y);
            z2=(int)(zf3*65535.0);
            x3=((int)pos2.x)<<16;
            y3=((int)pos2.y);
            z3=(int)(zf2*65535.0);
        }
    }
    //...
}
    public void renderFlatTriangle(Vec2 pos1,double zf1,Vec2 pos2,double zf2,Vec2 pos3,double zf3,int width,int height,double clip,int mode,RGBColor color){
        TriangleRenderer renderer = new TriangleRenderer(mode, color, pixel, zbuffer, width, clip);
        renderer.renderFlatTriangle(pos1, zf1, pos2, zf2, pos3, zf3);
    }