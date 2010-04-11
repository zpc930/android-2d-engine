package org.gs.components.graphics;

/**
 * AccelerometerHandler allow you be notified of shakes and accelerometer changes
 */
public class AccelerometerHandler {
	public Mover simulationRuntime;
	public AccelerometerHandler(Mover m) {
		simulationRuntime = m;
	}
	private float lx, ly;
	
	public void onChanged(float x, float y, float z) {
		float maxInertia = 3.0f;
		lx = -x/5.0f;
//		lx = lx > maxInertia ? lx : maxInertia;
		ly = y/5.0f;
//		ly = ly > maxInertia ? ly : maxInertia;
		simulationRuntime.setMovement(lx, ly);
		
	}

	
	public void onShake(float intensity) {
		// TODO Auto-generated method stub
		
	}
	
}