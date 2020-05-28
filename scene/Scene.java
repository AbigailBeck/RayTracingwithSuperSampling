package edu.cg.scene;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import edu.cg.Logger;
import edu.cg.UnimplementedMethodException;
import edu.cg.algebra.Hit;
import edu.cg.algebra.Ops;
import edu.cg.algebra.Point;
import edu.cg.algebra.Ray;
import edu.cg.algebra.Vec;
import edu.cg.scene.camera.PinholeCamera;
import edu.cg.scene.lightSources.Light;
import edu.cg.scene.objects.Surface;

public class Scene {
	private String name = "scene";
	private int maxRecursionLevel = 1;
	private int antiAliasingFactor = 1; // gets the values of 1, 2 and 3
	private boolean renderRefarctions = false;
	private boolean renderReflections = false;

	private PinholeCamera camera;
	private Vec ambient = new Vec(1, 1, 1); // white
	private Vec backgroundColor = new Vec(0, 0.5, 1); // blue sky
	private List<Light> lightSources = new LinkedList<>();
	private List<Surface> surfaces = new LinkedList<>();

	// MARK: initializers
	public Scene initCamera(Point eyePoistion, Vec towardsVec, Vec upVec, double distanceToPlain) {
		this.camera = new PinholeCamera(eyePoistion, towardsVec, upVec, distanceToPlain);
		return this;
	}

	public Scene initAmbient(Vec ambient) {
		this.ambient = ambient;
		return this;
	}

	public Scene initBackgroundColor(Vec backgroundColor) {
		this.backgroundColor = backgroundColor;
		return this;
	}

	public Scene addLightSource(Light lightSource) {
		lightSources.add(lightSource);
		return this;
	}

	public Scene addSurface(Surface surface) {
		surfaces.add(surface);
		return this;
	}

	public Scene initMaxRecursionLevel(int maxRecursionLevel) {
		this.maxRecursionLevel = maxRecursionLevel;
		return this;
	}

	public Scene initAntiAliasingFactor(int antiAliasingFactor) {
		this.antiAliasingFactor = antiAliasingFactor;
		return this;
	}

	public Scene initName(String name) {
		this.name = name;
		return this;
	}

	public Scene initRenderRefarctions(boolean renderRefarctions) {
		this.renderRefarctions = renderRefarctions;
		return this;
	}

	public Scene initRenderReflections(boolean renderReflections) {
		this.renderReflections = renderReflections;
		return this;
	}

	// MARK: getters
	public String getName() {
		return name;
	}

	public int getFactor() {
		return antiAliasingFactor;
	}

	public int getMaxRecursionLevel() {
		return maxRecursionLevel;
	}

	public boolean getRenderRefarctions() {
		return renderRefarctions;
	}

	public boolean getRenderReflections() {
		return renderReflections;
	}

	@Override
	public String toString() {
		String endl = System.lineSeparator();
		return "Camera: " + camera + endl + "Ambient: " + ambient + endl + "Background Color: " + backgroundColor + endl
				+ "Max recursion level: " + maxRecursionLevel + endl + "Anti aliasing factor: " + antiAliasingFactor
				+ endl + "Light sources:" + endl + lightSources + endl + "Surfaces:" + endl + surfaces;
	}

	private transient ExecutorService executor = null;
	private transient Logger logger = null;
	private transient int imgWidth;
	private transient int imgHeight;

	private void initSomeFields(int imgWidth, int imgHeight, Logger logger) {
		this.logger = logger;
		this.imgWidth = imgWidth;
		this.imgHeight = imgHeight;
	}

	public BufferedImage render(int imgWidth, int imgHeight, double viewAngle, Logger logger)
			throws InterruptedException, ExecutionException, IllegalArgumentException {

		initSomeFields(imgWidth, imgHeight, logger);

		BufferedImage img = new BufferedImage(imgWidth, imgHeight, BufferedImage.TYPE_INT_RGB);
		camera.initResolution(imgHeight, imgWidth, viewAngle);
		int nThreads = Runtime.getRuntime().availableProcessors();
		nThreads = nThreads < 2 ? 2 : nThreads;
		this.logger.log("Intitialize executor. Using " + nThreads + " threads to render " + name);
		executor = Executors.newFixedThreadPool(nThreads);

		@SuppressWarnings("unchecked")
		Future<Color>[][] futures = (Future<Color>[][]) (new Future[imgHeight][imgWidth]);

		this.logger.log("Starting to shoot " + (imgHeight * imgWidth * antiAliasingFactor * antiAliasingFactor)
				+ " rays over " + name);

		for (int y = 0; y < imgHeight; ++y)
			for (int x = 0; x < imgWidth; ++x){
				futures[y][x] = calcColor(x, y);
			}

		this.logger.log("Done shooting rays.");
		this.logger.log("Wating for results...");

		for (int y = 0; y < imgHeight; ++y)
			for (int x = 0; x < imgWidth; ++x) {
				Color color = futures[y][x].get();
				img.setRGB(x, y, color.getRGB());
			}

		executor.shutdown();

		this.logger.log("Ray tracing of " + name + " has been completed.");

		executor = null;
		this.logger = null;

		return img;
	}

	private Future<Color> calcColor(int x, int y) {
			
			if (this.antiAliasingFactor == 1){
				return executor.submit(() -> {
					Point centerPoint = camera.transform(x, y);
					Ray ray = new Ray(camera.getCameraPosition(), centerPoint);
					Vec color = calcColor(ray, 0);
					return color.toColor();
				});

			} else{
				return executor.submit(() -> {
					int numOfSamples = this.antiAliasingFactor*this.antiAliasingFactor;
					Vec ssVal = new Vec(0, 0, 0);
					double h = x;
					double v = y;
					double factor = 1 / (double) this.antiAliasingFactor;
					
					for (int i = 0; i < this.antiAliasingFactor; i++){

						h = h + ((double) i * factor) ; 

						for(int j = 0; j < this.antiAliasingFactor; j++){
							System.out.println("x: " + x + "y:" + y + "h:" + h + "v + j * factor: " + ((double) v + j * factor) + "this.antialiasingf:" + this.antiAliasingFactor + "factor:" + factor);
							Point centerPoint = camera.transform(h, v + (double) j * factor);
							Ray ray = new Ray(camera.getCameraPosition(), centerPoint);
							Vec color = calcColor(ray, 0);
							ssVal = ssVal.add(color);
							v = (double) y;
						}
						h = (double) x;
					}

					double avg = 1 / (double) numOfSamples;
					ssVal = ssVal.mult(avg);
					System.out.println("ssVal:" + ssVal.toString());
					return ssVal.toColor();

				});
			}
	}

	private Vec calcColor(Ray ray, int recusionLevel) {
		
		if(recusionLevel == this.maxRecursionLevel){
			return new Vec(0, 0, 0);
		}

		recusionLevel += 1;
		Vec color = new Vec(0, 0, 0);
		Hit minHit = findIntersection(ray);
		if (minHit == null){
			color =  this.backgroundColor;
		}else{
			Surface surface = minHit.getSurface();
			Point hittingPoint = ray.getHittingPoint(minHit);
			color = color.add(calcAmbient(surface));
			
			for (Light light : this.lightSources){
				
				if (isOccluded(light, light.rayToLight(hittingPoint))){
					continue;
				} 

				color = color.add(calcDiffusionColor(minHit, hittingPoint, light));
				color = color.add(calcSpecularColor(ray, minHit, hittingPoint, light));

			}

			if(this.renderReflections){

				Vec reflection = Ops.reflect(ray.direction(), minHit.getNormalToSurface()).normalize();
				Ray reflectedRay = new Ray(hittingPoint, reflection);
				Vec reflectionColor = calcColor(reflectedRay, recusionLevel).mult(surface.reflectionIntensity());
				color = color.add(reflectionColor);

			}

			if(this.renderRefarctions && surface.isTransparent()){

				Vec refraction = Ops.refract(ray.direction(), minHit.getNormalToSurface(), surface.n1(minHit), surface.n2(minHit)).normalize();
				Ray refractedRay = new Ray(hittingPoint, refraction);
				Vec refractionColor = calcColor(refractedRay, recusionLevel).mult(surface.reflectionIntensity());
				color = color.add(refractionColor);

			}

		}

		return color;
		
	}

	private Hit findIntersection(Ray ray){
	
		Double minT = Double.MAX_VALUE;
		Hit minHit = null;
		for(Surface surface: this.surfaces){
			Hit currHit = surface.intersect(ray);
			if((currHit != null) && (currHit.t() < minT)){
				minHit = currHit;
				minT = currHit.t();
			}
		}

		return minHit;
		
	}

	private Boolean isOccluded(Light light, Ray rayToLight){
		
		for(Surface surface : this.surfaces){
			if(!surface.isTransparent() && light.isOccludedBy(surface, rayToLight)) {
				return true;
			}
		}

		return false;

	}

	private Vec calcAmbient(Surface hitSurface){
		
		double red = hitSurface.Ka().x * this.ambient.x;
		double green = hitSurface.Ka().y * this.ambient.y;
		double blue = hitSurface.Ka().z * this.ambient.z;

		return new Vec(red, green, blue);
		
	}

	private Vec calcDiffusionColor(Hit hit, Point hittingPoint, Light lightSource){
		
		
		Vec diffusionCoeff = hit.getSurface().Kd();
		Ray rayToLight = lightSource.rayToLight(hittingPoint);
		Vec lightIntensity = lightSource.intensity(hittingPoint, rayToLight);
		Vec normal = hit.getNormalToSurface();
		double cosTheta = rayToLight.direction().normalize().dot(normal);
	
		return cosTheta > 0 ? diffusionCoeff.mult(cosTheta).mult(lightIntensity) : new Vec(0, 0, 0);
		
	}

	private Vec calcSpecularColor(Ray hitRay, Hit hit, Point intersectingPoint, Light lightSource){
		
		Vec specularCoeff = hit.getSurface().Ks();
		Ray rayToLight = lightSource.rayToLight(intersectingPoint);
		Vec lightIntensity = lightSource.intensity(intersectingPoint, rayToLight);
		Vec reflection = Ops.reflect(rayToLight.direction().mult(-1).normalize(), hit.getNormalToSurface());
		Vec rayToCamera = hitRay.direction().mult(-1).normalize();
		Double pow = Math.pow(rayToCamera.dot(reflection), hit.getSurface().shininess());
		
		return specularCoeff.mult(pow).mult(lightIntensity); 
		
	}


}
