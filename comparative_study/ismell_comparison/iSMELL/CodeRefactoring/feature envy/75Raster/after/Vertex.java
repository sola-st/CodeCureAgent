package artofillusion.raster;

import artofillusion.*;
import artofillusion.util.*;
import artofillusion.image.*;
import artofillusion.material.*;
import artofillusion.math.*;
import artofillusion.object.*;
import artofillusion.texture.*;
import artofillusion.ui.*;
import buoy.event.*;
import buoy.widget.*;
import java.awt.*;
import java.awt.image.*;
import java.util.*;

public class Vertex {
    Vec2 pos;
    float z;
    Vec3 vert, norm;
    double u, v;
    RGBColor diffuse;

    public Vertex(Vec2 pos, float z, Vec3 vert, Vec3 norm, double u, double v, RGBColor diffuse) {
        this.pos = pos;
        this.z = z;
        this.vert = vert;
        this.norm = norm;
        this.u = u;
        this.v = v;
        this.diffuse = diffuse;
    }

    // Method to translate the vertex
    public void translate(Vec2 translation) {
        this.pos = this.pos.add(translation);
    }

    // Method to scale the vertex
    public void scale(float scaleFactor) {
        this.pos = this.pos.scale(scaleFactor);
        this.z *= scaleFactor;
    }

    // Method to transform the vertex with a 4x4 matrix
    public void transform(Mat4x4 matrix) {
        Vec4 homogenousPos = new Vec4(this.pos.x, this.pos.y, this.z, 1.0);
        Vec4 transformedPos = matrix.multiply(homogenousPos);
        this.pos = new Vec2(transformedPos.x, transformedPos.y);
        this.z = transformedPos.z;
    }

    // Method to calculate the distance to another vertex
    public float distanceTo(Vertex other) {
        return this.pos.distanceTo(other.pos);
    }

    // Method to interpolate between this vertex and another
    public Vertex interpolate(Vertex other, float t) {
        Vec2 pos = this.pos.interpolate(other.pos, t);
        float z = this.z * (1 - t) + other.z * t;
        Vec3 vert = this.vert.interpolate(other.vert, t);
        Vec3 norm = this.norm.interpolate(other.norm, t);
        double u = this.u * (1 - t) + other.u * t;
        double v = this.v * (1 - t) + other.v * t;
        RGBColor diffuse = this.diffuse.interpolate(other.diffuse, t);
        return new Vertex(pos, z, vert, norm, u, v, diffuse);
    }

    // And so on for other operations that could be performed on a vertex
}
