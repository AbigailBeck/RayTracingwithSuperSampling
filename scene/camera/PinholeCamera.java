package edu.cg.scene.camera;

import edu.cg.UnimplementedMethodException;
import edu.cg.algebra.Point;
import edu.cg.algebra.Vec;

public class PinholeCamera {
	
	private Point cameraPosition;
	private Point centrePointOnPlain;
	private Vec towardsVec;
	private Vec upVec;
	private Vec rightVec;
	private double distanceToPlain;
	private double plainWidth;
	private double plainHeight;
	private double pixelSize;
	private int numOfPixelsX;
	private int numOfPixelsY;
	private int midPlainPixelX;
	private int midPlainPixelY;



	/**
	 * Initializes a pinhole camera model with default resolution 200X200 (RxXRy)
	 * and View Angle 90.
	 * 
	 * @param cameraPosition  - The position of the camera.
	 * @param towardsVec      - The towards vector of the camera (not necessarily
	 *                        normalized).
	 * @param upVec           - The up vector of the camera.
	 * @param distanceToPlain - The distance of the camera (position) to the center
	 *                        point of the image-plain.
	 * 
	 */
	public PinholeCamera(Point cameraPosition, Vec towardsVec, Vec upVec, double distanceToPlain) {
		
		this.cameraPosition = cameraPosition;
		this.towardsVec = towardsVec.normalize();
		this.rightVec = towardsVec.cross(upVec).normalize();
		this.upVec = this.rightVec.cross(towardsVec).normalize();
		this.distanceToPlain = distanceToPlain;
		this.centrePointOnPlain = this.cameraPosition.add(this.towardsVec.mult(this.distanceToPlain));

	}

	/**
	 * Initializes the resolution and width of the image.
	 * 
	 * @param height    - the number of pixels in the y direction.
	 * @param width     - the number of pixels in the x direction.
	 * @param viewAngle - the view Angle.
	 */
	public void initResolution(int height, int width, double viewAngle) {
		
		this.plainWidth = (double) 2 * this.distanceToPlain * Math.tan(viewAngle/2);
		this.numOfPixelsX = width;
		this.numOfPixelsY = height;
		this.pixelSize = this.plainWidth / this.numOfPixelsX;
		this.plainHeight = this.numOfPixelsY * this.pixelSize;
		this.midPlainPixelX = (int)(this.numOfPixelsX / 2);
		this.midPlainPixelY = (int)(this.numOfPixelsY / 2);
		
	}

	/**
	 * Transforms from pixel coordinates to the center point of the corresponding
	 * pixel in model coordinates.
	 * 
	 * @param x - the pixel index in the x direction.
	 * @param y - the pixel index in the y direction.
	 * @return the middle point of the pixel (x,y) in the model coordinates.
	 */
	public Point transform(double x, double y) {
		
		Vec vRight = this.rightVec.mult((x - this.midPlainPixelX) * this.pixelSize);
		Vec vUp = this.upVec.mult((y - this.midPlainPixelY) * this.pixelSize);
		Vec vRight_minus_vUp = vRight.add(vUp.mult(-1));
		Point targetPoint = this.centrePointOnPlain.add(vRight_minus_vUp);
		
		return targetPoint;

	}

	/**
	 * Returns the camera position
	 * 
	 * @return a new point representing the camera position.
	 */
	public Point getCameraPosition() {
	
		return new Point(this.cameraPosition.x, this.cameraPosition.y, this.cameraPosition.z);

		
	}
}
