package edu.cg.scene.objects;

import edu.cg.UnimplementedMethodException;
import edu.cg.algebra.Hit;
import edu.cg.algebra.Ops;
import edu.cg.algebra.Point;
import edu.cg.algebra.Ray;
import edu.cg.algebra.Vec;

public class Sphere extends Shape {
	private Point center;
	private double radius;
	
	public Sphere(Point center, double radius) {
		this.center = center;
		this.radius = radius;
	}
	
	public Sphere() {
		this(new Point(0, -0.5, -6), 0.5);
	}

	public Point getCenter(){
		return this.center;
	}

	public double getRadius(){
		return this.radius;
	}
	
	@Override
	public String toString() {
		String endl = System.lineSeparator();
		return "Sphere:" + endl + 
				"Center: " + center + endl +
				"Radius: " + radius + endl;
	}
	
	public Sphere initCenter(Point center) {
		this.center = center;
		return this;
	}
	
	public Sphere initRadius(double radius) {
		this.radius = radius;
		return this;
	}
	
	@Override
	public Hit intersect(Ray ray) {
		
		Vec dir = ray.direction().normalize();
		Point source = ray.source();
		Vec l = Ops.sub(source, center);
		double a = dir.dot(dir);
		double b = 2 * (dir.dot(l)); 
		double c = l.dot(l) - (radius * radius);
		double t1 = -0.5 * b / a;
		double t2 = -0.5 * b / a;
		
		double bb4ac = b * b - 4 * a * c;

		if (bb4ac < 0) {

			return null;

		} else if ((bb4ac == 0)) {

				if (t1 < Ops.epsilon){

					return null;

				} else{

					Point hit = source.add(dir.mult(t1));
					Vec hitDir = Ops.sub(hit, center).normalize();
					return new Hit(t1, hitDir);

				}	

		} else {

			double q = (b > 0) ? -0.5 * (b + Math.sqrt(bb4ac)) : -0.5 * (b - Math.sqrt(bb4ac));
			t1 = q/a;
			t2 = c/q;

			if (t1 > Ops.epsilon && t2 > Ops.epsilon) {

				double minT = Math.min(t1, t2);
				Point hit = source.add(dir.mult(minT));
				Vec hitDir = Ops.sub(hit, center).normalize();
				return new Hit(minT, hitDir);

			} else if ((t1 < Ops.epsilon) && (t2 > Ops.epsilon)){

				Point hit = source.add(dir.mult(t2));
				Vec hitDir = Ops.sub(hit, center).normalize();
				return new Hit(t2, hitDir).setIsWithin(true);

			} else if ((t2 < Ops.epsilon) && (t1 > Ops.epsilon)){

				Point hit = source.add(dir.mult(t1));
				Vec hitDir = Ops.sub(hit, center).normalize();
				return new Hit(t2, hitDir).setIsWithin(true);

			} else {

				return null;
				
			}

		}
		
	}
			
}
