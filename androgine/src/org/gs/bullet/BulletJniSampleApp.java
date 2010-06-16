/*
Bullet Continuous Collision Detection and Physics Library for Android NDK
Copyright (c) 2006-2009 Noritsuna Imamura  http://www.siprop.org/

This software is provided 'as-is', without any express or implied warranty.
In no event will the authors be held liable for any damages arising from the use of this software.
Permission is granted to anyone to use this software for any purpose,
including commercial applications, and to alter it and redistribute it freely,
subject to the following restrictions:

1. The origin of this software must not be misrepresented; you must not claim that you wrote the original software. If you use this software in a product, an acknowledgment in the product documentation would be appreciated but is not required.
2. Altered source versions must be plainly marked as such, and must not be misrepresented as being the original software.
3. This notice may not be removed or altered from any source distribution.
*/
package org.gs.bullet;

import java.util.Map;

import org.gs.bullet.shape.SphereShape;
import org.gs.bullet.shape.StaticPlaneShape;
import org.gs.bullet.util.Point3;
import org.gs.bullet.util.ShapeType;
import org.gs.bullet.util.Vector3;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;


public class BulletJniSampleApp extends Activity
{
	private Bullet bullet = new Bullet();
	private BallView  bv;
	private Thread runBullt;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        PhysicsWorld physicsWorld = bullet.createPhysicsWorld(
        		new Vector3( 0.0f, 0.0f, 0.0f),
        		new Vector3( 320.0f,  480.0f,  480.0f),
        		1024,
        		new Vector3(0.0f, -9.8f, 0.0f));
        
        StaticPlaneShape floarShape = new StaticPlaneShape(new Vector3(0.0f, 1.0f, 0.0f), 0.0f);
    	Geometry floarGeom = bullet.createGeometry(floarShape,
				   									0.0f,
				   									new Vector3(0.0f, 0.0f, 0.0f));  
    	
    	MotionState floarState = new MotionState();
    	RigidBody floarBody = bullet.createAndAddRigidBody(floarGeom,
    													   floarState);
    	//Share posions.
        float[] ball_pos = new float[12];
        ball_pos[0] = 300.0f;
        ball_pos[1] = 138.0f;
        ball_pos[2] = 400.0f;
        ball_pos[3] = 130.0f;
        ball_pos[4] = 11.0f;
        ball_pos[5] = 154.0f;
        ball_pos[6] = 11.0f;
        ball_pos[7] = 132.0f;
        ball_pos[8] = 11.0f;
        ball_pos[9] = 110.0f;
        
    	{
	    	SphereShape sphereShape = new SphereShape(11.0f);
	    	Geometry sphereGeom = bullet.createGeometry(sphereShape,
					   									50.0f,
					   									new Vector3(0.0f, 0.0f, 0.0f));  
	    	
	    	MotionState sphereState = new MotionState();
	    	sphereState.worldTransform = new Transform(new Point3(ball_pos[1], ball_pos[0], 0.0f));
	    	RigidBody sphereBody = bullet.createAndAddRigidBody(sphereGeom,
    														sphereState);
    	}

    	{
	    	SphereShape sphereShape = new SphereShape(11.0f);
	    	Geometry sphereGeom = bullet.createGeometry(sphereShape,
					   									1.0f,
					   									new Vector3(0.0f, 0.0f, 0.0f));  
	    	
	    	MotionState sphereState = new MotionState();
	    	sphereState.worldTransform = new Transform(new Point3(ball_pos[3], ball_pos[2], 0.0f));
	    	RigidBody sphereBody = bullet.createAndAddRigidBody(sphereGeom,
    														sphereState);
    	}
    	
    	{
	    	SphereShape sphereShape = new SphereShape(11.0f);
	    	Geometry sphereGeom = bullet.createGeometry(sphereShape,
					   									0.2f,
					   									new Vector3(0.0f, 0.0f, 0.0f));  
	    	
	    	MotionState sphereState = new MotionState();
	    	sphereState.worldTransform = new Transform(new Point3(ball_pos[5], ball_pos[4], 0.0f));
	    	RigidBody sphereBody = bullet.createAndAddRigidBody(sphereGeom,
    														sphereState);
    	}
    	
    	{
	    	SphereShape sphereShape = new SphereShape(11.0f);
	    	Geometry sphereGeom = bullet.createGeometry(sphereShape,
					   									0.4f,
					   									new Vector3(0.0f, 0.0f, 0.0f));  
	    	
	    	MotionState sphereState = new MotionState();
	    	sphereState.worldTransform = new Transform(new Point3(ball_pos[7], ball_pos[6], 0.0f));
	    	RigidBody sphereBody = bullet.createAndAddRigidBody(sphereGeom,
    														sphereState);
    	}
    	{
	    	SphereShape sphereShape = new SphereShape(11.0f);
	    	Geometry sphereGeom = bullet.createGeometry(sphereShape,
					   									0.1f,
					   									new Vector3(0.0f, 0.0f, 0.0f));  
	    	
	    	MotionState sphereState = new MotionState();
	    	sphereState.worldTransform = new Transform(new Point3(ball_pos[9], ball_pos[8], 0.0f));
	    	RigidBody sphereBody = bullet.createAndAddRigidBody(sphereGeom,
    														sphereState);
    	}

        bv = new BallView(getApplication());
        bv.update(ball_pos);
        setContentView(bv);
        
    }

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == 23) {
			if(runBullt == null) {
				bv.setIsStart(true);
				runBullt = new Thread(new BulletResultSimulationCallback(bullet, bv, mHandler));
				runBullt.start();
			}
		}
		return super.onKeyDown(keyCode, event);
	}
	
	public static int UPDATE_DRAW = 12321;
	
    private Handler mHandler = new MainHandler(); 
    private class MainHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 12321: {
                	bv.draw();
                    break;
                }
            }
        }
    }
	@Override
	protected void onStop() {
		runBullt.stop();
		bullet.destory();
		super.onStop();
	}


}

class BulletResultSimulationCallback implements Runnable {
	
    private Bullet bullet;
    private BallView bv;
    private Handler mHandler;

	public BulletResultSimulationCallback(Bullet bullet, BallView bv, Handler mHandler) {
		this.bullet = bullet;
		this.bv = bv;
		this.mHandler = mHandler;
	}
	
	
	int frames = 0;
	long time = 0;
	@Override
	public void run() {
		float[] ball_pos = new float[12];
		Map<Integer, RigidBody> rigidBodies;
		int j = 0;
		while(true) {
			
			// simulation.
			rigidBodies = bullet.doSimulation(1.0f / 60.0f ,10);
			j = 0;
			for(RigidBody body: rigidBodies.values()) {
				
				if(body.geometry.shape.getType() == ShapeType.SPHERE_SHAPE_PROXYTYPE) {
					ball_pos[j] = body.motionState.resultSimulation.originPoint.x;
					j++;
					ball_pos[j] = body.motionState.resultSimulation.originPoint.y;
					j++;
				}
			}
			bv.update(ball_pos);
			mHandler.sendMessage(mHandler.obtainMessage(BulletJniSampleApp.UPDATE_DRAW));
		}
	}
}


class BallView extends View {
    private Paint myPaint = new Paint();
    private Bitmap ballBitmap;
    
    private int[] ball = new int[12];
    private boolean isStart = false;
    
    public BallView(Context c) {
        super(c);
        Resources res = this.getContext().getResources();
        ballBitmap = BitmapFactory.decodeResource(res, R.drawable.ball);        
    }
    
    public void update(float[] ball_pos) {
    	this.ball[0] = (int)ball_pos[0];
    	this.ball[1] = 415 - (int)ball_pos[1];
    	this.ball[2] = (int)ball_pos[2];
    	this.ball[3] = 415 - (int)ball_pos[3];
    	this.ball[4] = (int)ball_pos[4];
    	this.ball[5] = 415 - (int)ball_pos[5];
    	this.ball[6] = (int)ball_pos[6];
    	this.ball[7] = 415 - (int)ball_pos[7];
    	this.ball[8] = (int)ball_pos[8];
    	this.ball[9] = 415 - (int)ball_pos[9];
    	
    }
    public void draw() {
    	invalidate();
    }
    
    public void setIsStart(boolean isStart) {
    	this.isStart = isStart;
    }
 
    protected void onDraw(Canvas canvas) {
    	
    	Log.d("BallView", "Call onDraw.");
        canvas.drawColor(Color.BLACK);
        if(isStart) {
	        canvas.drawBitmap(ballBitmap, ball[0], ball[1], myPaint);
	        canvas.drawBitmap(ballBitmap, ball[2], ball[3], myPaint);
	        canvas.drawBitmap(ballBitmap, ball[4], ball[5], myPaint);
	        canvas.drawBitmap(ballBitmap, ball[6], ball[7], myPaint);
	        canvas.drawBitmap(ballBitmap, ball[8], ball[9], myPaint);
        } else {
        	myPaint.setColor(Color.RED);
        	canvas.drawText("Please push tracball.", 80.0f, 100.0f, myPaint);
        }
    }

}