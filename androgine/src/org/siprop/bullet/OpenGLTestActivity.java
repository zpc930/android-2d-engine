/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.siprop.bullet;

import org.gs.components.graphics.Accelerometer;
import org.gs.components.graphics.AccelerometerHandler;
import org.gs.components.graphics.GLSprite;
import org.gs.components.graphics.GLSurfaceView;
import org.gs.components.graphics.Grid;
import org.gs.components.graphics.Mover;
import org.gs.components.graphics.ProfileRecorder;
import org.gs.components.graphics.Renderable;
import org.gs.components.graphics.SimpleGLRenderer;
import org.siprop.bullet.collisionShapes.CollisionShape;
import org.siprop.bullet.interfaces.DynamicsWorld;
import org.siprop.bullet.shape.SphereShape;
import org.siprop.bullet.shape.StaticPlaneShape;
import org.siprop.bullet.solver.VoronoiSimplexSolver;
import org.siprop.bullet.util.Point3;
import org.siprop.bullet.util.Vector3;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
/**
 * Activity for testing OpenGL ES drawing speed. This activity sets up sprites
 * and passes them off to an OpenGLSurfaceView for rendering and movement.
 */
public class OpenGLTestActivity extends Activity {
	private Thread runBullt;

//	private _BallView bv;
	private final static int SPRITE_WIDTH = 64;
	private final static int SPRITE_HEIGHT = 64;

	private GLSurfaceView mGLSurfaceView;
	private Bullet bullet = new Bullet();
	GLSprite[] spriteArray;
	SimpleGLRenderer spriteRenderer = new SimpleGLRenderer(this);
	private Mover simulationRuntime;
	Context context;
	SensorManager sensorManager;
	boolean profile;
	@Override
	public void onCreate(Bundle savedInstanceState) {
//		Debug.startMethodTracing("b1");
		super.onCreate(savedInstanceState);
		
		mGLSurfaceView = new GLSurfaceView(this);
		sensorManager = (SensorManager)mGLSurfaceView.getContext().getSystemService(Context.SENSOR_SERVICE);

		
		// Clear out any old profile results.
		if (profile) {
			ProfileRecorder.sSingleton.resetAll();
		}

		final Intent callingIntent = getIntent();
		// Allocate our sprites and add them to an array.
		final int robotCount = callingIntent.getIntExtra("spriteCount", 10);
		spriteArray = new GLSprite[robotCount + 1];
		final boolean animate = callingIntent.getBooleanExtra("animate", true);
		profile = callingIntent.getBooleanExtra("profile", true);
		
		final boolean useVerts = callingIntent.getBooleanExtra("useVerts",
				false);
		final boolean useHardwareBuffers = callingIntent.getBooleanExtra(
				"useHardwareBuffers", false);

		// Allocate space for the robot sprites + one background sprite.

		// We need to know the width and height of the display pretty soon,
		// so grab the information now.
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		PhysicsWorld physicsWorld = bullet.createPhysicsWorld(new Vector3(0.0f,
				0.0f, 0.0f), new Vector3(dm.widthPixels, dm.heightPixels, 0), 64,
				new Vector3(0.0f, 0.0f, 0.0f));

		{
			StaticPlaneShape floarShape = new StaticPlaneShape(new Vector3(0.0f,
					1.0f, 0.0f), -20.0f);
			CollisionShape s;
			
			Geometry floarGeom = bullet.createGeometry(floarShape, 0.0f,
					new Vector3(0.0f, 0.0f, 0.0f));
			
			MotionState floarState = new MotionState();
			RigidBody floarBody = bullet.createAndAddRigidBody(floarGeom,
					floarState);
		}
		{
			StaticPlaneShape floarShape = new StaticPlaneShape(new Vector3(1.0f,
					0.0f, 0.0f), -4.0f);
			Geometry floarGeom = bullet.createGeometry(floarShape, 0.0f,
					new Vector3(0.0f, 0.0f, 0.0f));
			
			MotionState floarState = new MotionState();
			RigidBody floarBody = bullet.createAndAddRigidBody(floarGeom,
					floarState);
		}
		{
			StaticPlaneShape floarShape = new StaticPlaneShape(new Vector3(-1.0f,
					0.0f, 0.0f), -300.0f);
			Geometry floarGeom = bullet.createGeometry(floarShape, 0.0f,
					new Vector3(0.0f, 0.0f, 0.0f));
			
			MotionState floarState = new MotionState();
			RigidBody floarBody = bullet.createAndAddRigidBody(floarGeom,
					floarState);
		}
		{
			StaticPlaneShape floarShape = new StaticPlaneShape(new Vector3(0.0f,
					-1.0f, 0.0f), -440.0f);
			Geometry floarGeom = bullet.createGeometry(floarShape, 0.0f,
					new Vector3(0.0f, 0.0f, 0.0f));
			
			MotionState floarState = new MotionState();
			RigidBody floarBody = bullet.createAndAddRigidBody(floarGeom,
					floarState);
		}

		GLSprite background = new GLSprite(R.drawable.background);
		BitmapDrawable backgroundImage = (BitmapDrawable) getResources()
				.getDrawable(R.drawable.background);
		Bitmap backgoundBitmap = backgroundImage.getBitmap();
		background.width = backgoundBitmap.getWidth();
		background.height = backgoundBitmap.getHeight();
		if (useVerts) {
			// Setup the background grid. This is just a quad.
			Grid backgroundGrid = new Grid(2, 2, false);
			backgroundGrid.set(0, 0, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, null);
			backgroundGrid.set(1, 0, background.width, 0.0f, 0.0f, 1.0f, 1.0f,
					null);
			backgroundGrid.set(0, 1, 0.0f, background.height, 0.0f, 0.0f, 0.0f,
					null);
			backgroundGrid.set(1, 1, background.width, background.height, 0.0f,
					1.0f, 0.0f, null);
			background.setGrid(backgroundGrid);
		}
		spriteArray[0] = background;

		Grid spriteGrid = null;
		if (useVerts) {
			// Setup a quad for the sprites to use. All sprites will use the
			// same sprite grid intance.
			spriteGrid = new Grid(2, 2, false);
			spriteGrid.set(0, 0, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, null);
			spriteGrid.set(1, 0, SPRITE_WIDTH, 0.0f, 0.0f, 1.0f, 1.0f, null);
			spriteGrid.set(0, 1, 0.0f, SPRITE_HEIGHT, 0.0f, 0.0f, 0.0f, null);
			spriteGrid.set(1, 1, SPRITE_WIDTH, SPRITE_HEIGHT, 0.0f, 1.0f, 0.0f,
					null);
		}

		// This list of things to move. It points to the same content as the
		// sprite list except for the background.
		Renderable[] renderableArray = new Renderable[robotCount + 1];
		final int robotBucketSize = robotCount / 3;
		for (int x = 0; x < robotCount; x++) {
			GLSprite robot;
			// Our robots come in three flavors. Split them up accordingly.
			if (x < robotBucketSize) {
				robot = new GLSprite(R.drawable.skate1);
			} else if (x < robotBucketSize * 2) {
				robot = new GLSprite(R.drawable.skate2);
			} else {
				robot = new GLSprite(R.drawable.skate3);
			}

			robot.width = SPRITE_WIDTH;
			robot.height = SPRITE_HEIGHT;

			// Pick a random location for this sprite.
			robot.x = (float) (Math.random() * dm.widthPixels);
			robot.y = (float) (Math.random() * dm.heightPixels);

			{
				SphereShape sphereShape = new SphereShape(20.0f);
				Geometry sphereGeom = bullet.createGeometry(sphereShape, 10.0f,
						new Vector3(0.0f, 0.0f, 0.0f));

				MotionState sphereState = new MotionState();
				
				sphereState.worldTransform = new Transform(new Point3(robot.x,
						robot.y, 0.0f));
				
				RigidBody sphereBody = bullet.createAndAddRigidBody(sphereGeom,
						sphereState);

			}

			// All sprites can reuse the same grid. If we're running the
			// DrawTexture extension test, this is null.
			robot.setGrid(spriteGrid);

			// Add this robot to the spriteArray so it gets drawn and to the
			// renderableArray so that it gets moved.
			spriteArray[x + 1] = robot;
			renderableArray[x] = robot;
		}
        // Now's a good time to run the GC.  Since we won't do any explicit
        // allocation during the test, the GC should stay dormant and not
        // influence our results.
        Runtime r = Runtime.getRuntime();
        r.gc();
        
        spriteRenderer.setSprites(spriteArray);
        spriteRenderer.setVertMode(useVerts, useHardwareBuffers);
        
        mGLSurfaceView.setRenderer(spriteRenderer);
        mGLSurfaceView.profile = profile;
        
        if (animate) {
            simulationRuntime = new Mover(bullet);
            simulationRuntime.setRenderables(renderableArray);
            
            mGLSurfaceView.setEvent(simulationRuntime);
    		startAcelerometer();
        }
        setContentView(mGLSurfaceView);
	}
	/* (non-Javadoc)
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}
	/* (non-Javadoc)
	 * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
	 */
	private float localX = 0.0f, localY = 0.0f;
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (KeyEvent.KEYCODE_DPAD_LEFT == keyCode) {
			localX = localX - 1.0f;
		}
		if (KeyEvent.KEYCODE_DPAD_RIGHT == keyCode) {
			localX = localX + 1.0f;
		}
		if (KeyEvent.KEYCODE_DPAD_UP == keyCode) {
			localX = localY + 1.0f;
		}
		if (KeyEvent.KEYCODE_DPAD_DOWN == keyCode) {
			localX = localY - 1.0f;
		}
		Log.w("a", "keyCode" + keyCode);
		return super.onKeyDown(keyCode, event);
	}
	/* (non-Javadoc)
	 * @see android.app.Activity#onTouchEvent(android.view.MotionEvent)
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		System.out.println(event.getX());
		// TODO Auto-generated method stub
		return super.onTouchEvent(event);
	}
	/* (non-Javadoc)
	 * @see android.app.Activity#onTrackballEvent(android.view.MotionEvent)
	 */
	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		if (MotionEvent.ACTION_MOVE == event.getAction()) {
			localX = localX + event.getX();
			localY = localY - event.getY();
		//	simulationRuntime.setMovement(localX, localY);
		}
		// TODO Auto-generated method stub
		return super.onTrackballEvent(event);
	}
	public void startAcelerometer() {
		AccelerometerHandler ah = new AccelerometerHandler(simulationRuntime);
		Accelerometer.startListening(ah, sensorManager) ;		
	}
}


class SceneSolver extends VoronoiSimplexSolver {

	
}

class LocalDynamicsWorld implements DynamicsWorld  {

	@Override
	public int getType() {
		// TODO Auto-generated method stub
		return 0;
	}
	
}

