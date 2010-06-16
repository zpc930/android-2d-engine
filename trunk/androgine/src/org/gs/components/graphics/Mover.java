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

package org.gs.components.graphics;

import java.util.Collection;
import java.util.Map;

import org.gs.bullet.Bullet;
import org.gs.bullet.PhysicsWorld;
import org.gs.bullet.RigidBody;
import org.gs.bullet.interfaces.ResultSimulationCallback;
import org.gs.bullet.util.ShapeType;
import org.gs.bullet.util.Vector3;

import android.os.SystemClock;
import android.util.Log;

/**
 * A simple runnable that updates the position of each sprite on the screen
 * every frame by applying a very simple gravity and bounce simulation.  The
 * sprites are jumbled with random velocities every once and a while.
 */
public class Mover implements Runnable {
    private Renderable[] mRenderables;
	private Bullet bullet;
	Map<Integer, RigidBody> rigidBodies;
	int i, j = 0;
	Vector3 vector3;
	public Mover(Bullet mBullet) {
		bullet = mBullet;
		vector3 = new Vector3();
	}

    public void run() {

		// simulation.
		rigidBodies = bullet.doSimulation(1.0f / 30.0f, 10);
		i = j = 0;
		
		for (RigidBody body : rigidBodies.values()) {
			if (body.geometry.shape.getType() == ShapeType.SPHERE_SHAPE_PROXYTYPE) {
				vector3.x = x*30.0f;
				vector3.y = -y*30.0f;
				bullet.applyCentralImpulse(body, vector3);
				mRenderables[i].x = body.motionState.resultSimulation.originPoint.x;
				mRenderables[i].y = body.motionState.resultSimulation.originPoint.y;
				
				i++;
			}
		}
    }
    
    public void setRenderables(Renderable[] renderables) {
        mRenderables = renderables;
    }
        
    private float x, y;
    public void setMovement(float x, float y) {
    	this.x = x;
    	this.y = y;
    }

}
