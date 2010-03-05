package com.stickycoding.Rokon;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.microedition.khronos.opengles.GL10;

import android.os.Build;

import com.stickycoding.Rokon.Handlers.AnimationHandler;
import com.stickycoding.Rokon.Handlers.CollisionHandler;


/**
 * Sprite handles both visual and physical
 * parts of in game objects. Basic dynamics can be applied.
 *
 * @author Richard
 */
public class Sprite extends DynamicObject {
	public static final int MAX_COLLIDERS = 0;
	
	private int i, j;
	
	private Sprite[] _collidersArr = new Sprite[MAX_COLLIDERS];
	private int _colliderCount = 0;
	
	private AnimationHandler _animationHandler;
	private CollisionHandler _collisionHandler;

	private boolean _killMe;
	private boolean _animating;
	private int _animateStartTile;
	private int _animateEndTile;
	private int _animateRemainingLoops;
	private float _animateTime;
	private boolean _animateReturnToStart;
	private long _animateLastUpdate;
	private boolean _animateRandom;
	private int [] _customTile;
	private int _currentTile;
	
	public int intVar1, intVar2, intVar3;
	
	public Sprite(float x, float y, float width, float height) {
		this(x, y, width, height, null);
	}
		
	public Sprite(float x, float y, float width, float height, Texture texture) {
		super(x, y, width, height);
		_red = 1;
		_green = 1;
		_blue = 1;
		_killMe = false;

		if(Build.VERSION.SDK == "3")
			_texBuffer = ByteBuffer.allocate(8*4);
		else
			_texBuffer = ByteBuffer.allocateDirect(8*4);
		_texBuffer.order(ByteOrder.nativeOrder());
		
		if(texture != null)
			setTexture(texture);
		resetDynamics();
		updateVertexBuffer();
	}
	
	public Sprite(float x, float y, Texture texture) {
		this(x, y, 0, 0, texture);
		setWidth(texture.getWidth() / texture.getTileColumnCount(), true);
		setHeight(texture.getHeight() / texture.getTileRowCount(), true);
		updateVertexBuffer();
	}
	
	public Sprite(Texture texture) {
		this(0, 0, 0, 0, texture);
		setWidth(texture.getWidth() / texture.getTileColumnCount(), true);
		setHeight(texture.getHeight() / texture.getTileRowCount(), true);
		updateVertexBuffer();
	}
	
	/**
	 * @return TRUE if the Sprite is marked for removal from the Layer after the next frame.
	 */
	public boolean isDead() {
		return _killMe;
	}
	
	/**
	 * Marks the Sprite for removal, it will be taken off the Layer at the end of the current frame 
	 */
	public void markForRemoval() {
		_killMe = true;
	}
	
	/**
	 * Removes the Texture that has been applied to the Sprite
	 */
	public void resetTexture() {
		_texture = null;
	}
	
	/**
	 * @return the current Texture tile index that is being used by the Sprite
	 */
	public int getTileIndex() {
		int tileIndex = 0;
		tileIndex += _tileX;
		tileIndex += (_tileY - 1) * _texture.getTileColumnCount();
		return tileIndex;
	}	
	

	/**
	 * @param red 0.0 to 1.0
	 */
	public void setRed(float red) {
		_red = red;
	}

	/**
	 * @param red 0.0 to 1.0
	 */
	public void setGreen(float green) {
		_green = green;
	}

	/**
	 * @param red 0.0 to 1.0
	 */
	public void setBlue(float blue) {
		_blue = blue;
	}

	/**
	 * @param red 0 to 255
	 */
	public void setRedInt(int red) {
		_red = (float)red / 255f;
	}

	/**
	 * @param red 0 to 255
	 */
	public void setGreenInt(int green) {
		_green = (float)green / 255f;
	}

	/**
	 * @param red 0 to 255
	 */
	public void setBlueInt(int blue) {
		_blue = (float)blue / 255f;
	}

	/**
	 * @param red 0 to 255
	 */
	public void setAlphaInt(int alpha) {
		_alpha = (float)alpha / 255f;
	}
	
	/**
	 * Sets the color of the Sprite, this is still applied when a textured. 1,1,1,1 is white and 0,0,0,1 is black 
	 * @param red 0.0 to 1.0
	 * @param green 0.0 to 1.0
	 * @param blue 0.0 to 1.0 
	 * @param alpha 0.0 to 1.0
	 */
	public void setColor(float red, float green, float blue, float alpha) {
		setRed(red);
		setGreen(green);
		setBlue(blue);
		_alpha = alpha;
	}
	
	/**
	 * @return current red value, 0.0 to 1.0
	 */
	public float getRed() {
		return _red;
	}
	
	/**
	 * @return current green value, 0.0 to 1.0
	 */
	public float getGreen() {
		return _green;
	}

	/**
	 * @return current blue value, 0.0 to 1.0
	 */
	public float getBlue() {
		return _blue;
	}

	/**
	 * @return current red value, 0 to 255
	 */
	public int getRedInt() {
		return Math.round(_red * 255);
	}

	/**
	 * @return current green value, 0 to 255
	 */
	public int getGreenInt() {
		return Math.round(_green * 255);
	}

	/**
	 * @return current blue value, 0 to 255
	 */
	public int getBlueInt() {
		return Math.round(_blue * 255);
	}

	/**
	 * @return current alpha value, 0 to 255
	 */
	public int getAlphaInt() {
		return Math.round(_alpha * 255);
	}

	/**
	 * Draws the Sprite to the OpenGL object, should be no need to call this
	 * @param gl
	 */
	public void drawFrame(GL10 gl) {
		_detectCollisions();
		super.drawFrame(gl);
	}
	
	/**
	 * Note, this is very basic and does represents only the rectangular Sprite
	 * @param x
	 * @param y
	 * @return TRUE if the Sprite is colliding with the given coordinates
	 */
	public boolean isAt(int x, int y) {
		if(x < getX() || x > getX() + getWidth())
			return false;
		if(y < getY() || y > getY() + getHeight())
			return false;
		return true;
	}
	
	/**
	 * Updates the movement, animation and modifiers. This is called automatically, no need to use this.
	 */
	private long timeDiff;
	private int nextTile;
	public void updateMovement() {
		//	if this is the first update, forget about it
		if(getLastUpdate() == 0) {
			setLastUpdate();
			return;
		}
		
		super.updateMovement();
		
		//	update animation
		if(_animating) {
			timeDiff = Rokon.getTime() - _animateLastUpdate;
			if(timeDiff >= _animateTime) {
				if (_customTile == null) {
					nextTile = getTileIndex() + 1;
				} else {
					nextTile = _currentTile + 1;
				}
				if(nextTile > _animateEndTile) {
					if(_animateRemainingLoops > -1)
						if(_animateRemainingLoops <= 1) {
							_animating = false;
							if(_animateReturnToStart)
								nextTile = _animateStartTile;
							if(_animationHandler != null)
								_animationHandler.finished(this);
						} else {
							nextTile = _animateStartTile;
							_animateRemainingLoops--;
							if(_animationHandler != null)
								_animationHandler.endOfLoop(_animateRemainingLoops);
						}
					else {
						nextTile = _animateStartTile;
						if(_animationHandler != null)
							_animationHandler.endOfLoop(_animateRemainingLoops);
					}
				}
				if (_customTile == null){
					if(_animateRemainingLoops > 1 || _animateRemainingLoops == -1)
						if(_animateRandom)
							setTileIndex((int)(Math.random() * (_animateEndTile - _animateStartTile)) + _animateStartTile);
						else
							setTileIndex(nextTile);
				} else {
					_currentTile = nextTile;
					if(_animateRemainingLoops > 1 || _animateRemainingLoops == -1)
						if(_animateRandom)
							setTileIndex(_customTile[(int)(Math.random() * (_animateEndTile - _animateStartTile)) + _animateStartTile]);
						else
							setTileIndex(_customTile[nextTile]);
				}
				_animateLastUpdate = Rokon.getTime();
			}
		}
		updateModifiers();
	}
	
	/**
	 * @param collisionHandler defines a CollisionHandler which the Sprite should trigger if it collides with any of the registered targets
	 */
	public void setCollisionHandler(CollisionHandler collisionHandler) {
		_collisionHandler = collisionHandler;
	}
	
	/**
	 * Removes the CollisionHandler, and no longer checks for collisions
	 */
	public void resetCollisionHandler() {
		_collisionHandler = null;
		//TODO modifierArr reset in collision handler ?
		resetModifiers();
	}
	
	/**
	 * @param target adds a target Sprite for the CollisionHandler to check for each frame
	 */
	public void addCollisionSprite(Sprite target) {
		j = -1;
		for(i = 0; i < MAX_COLLIDERS; i++)
			if(_collidersArr[i] == null)
				j = i;
		if(j == -1) {
			Debug.print("TOO MANY SPRITE COLLIDERS");
			return;
		}
		_collidersArr[j] = target;
		_colliderCount++;
	}
	
	/**
	 * @param target removes a target from the Sprite's list
	 */
	public void removeCollisionSprite(Sprite target) {
		for(i = 0; i < MAX_COLLIDERS; i++)
			if(_collidersArr[i] != null)
				if(_collidersArr[i].equals(target))
					_collidersArr[i] = null;
		_colliderCount--;
	}
	
	/**
	 * @param animationHandler sets an AnimationHandler, which can keep track of animation loops and ends
	 */
	public void setAnimationHandler(AnimationHandler animationHandler) {
		_animationHandler = animationHandler;
	}
	
	/**
	 * Removes the AnimationHandler from the Sprite
	 */
	public void resetAnimationHandler() {
		_animationHandler = null;
	}

	private void _detectCollisions() {
		if(_collisionHandler == null || _colliderCount == 0)
			return;

		for(i = 0; i < MAX_COLLIDERS; i++) {
			if(_collidersArr[i] != null)
				if((getX() >= _collidersArr[i].getX() && getX() <= _collidersArr[i].getX() + _collidersArr[i].getWidth()) || (getX() <= _collidersArr[i].getX() && getX() + getWidth() >= _collidersArr[i].getX()))
					if((getY() >= _collidersArr[i].getY() && getY() <= _collidersArr[i].getY() + _collidersArr[i].getHeight()) || (getY() <= _collidersArr[i].getY() && getY() + getHeight() >= _collidersArr[i].getY()))
						_collisionHandler.collision(this, _collidersArr[i]);
		}
	}
	
	/**
	 * Animates a Sprite by using tiles from its Texture. This will loop indefinately.
	 * @param startTile the first tile index in the animation
	 * @param endTile the final tile index used in the animation
	 * @param time the time in milliseconds between each frame
	 */
	public void animate(int startTile, int endTile, float time) {
		_animating = true;
		_animateStartTile = startTile;
		_animateEndTile = endTile;
		_animateTime = time;
		_animateRemainingLoops = -1;
		_animateLastUpdate = Rokon.getTime();
		_animateRandom = false;
		setTileIndex(startTile);
	}
	
	/**
	 * Animates a Sprite by using tiles from its Texture
	 * @param startTile the first tile index in the animation
	 * @param endTile the final tile index used in the animation
	 * @param time the time in milliseconds between each frame
	 * @param loops the number of loops to go through the animation
	 * @param returnToStart TRUE if the Sprite should return to startTile when complete, FALSE if the Sprite should stay at endTile when complete
	 */
	public void animate(int startTile, int endTile, float time, int loops, boolean returnToStart) {
		_animating = true;
		_animateStartTile = startTile;
		_animateEndTile = endTile;
		_animateTime = time;
		_animateRemainingLoops = loops + 1;
		_animateReturnToStart = returnToStart;
		_animateLastUpdate = Rokon.getTime();
		_animateRandom = false;
		setTileIndex(startTile);
	}
	
	public void animateRandom(int startTile, int endTile, float time) {
		_animating = true;
		_animateStartTile = startTile;
		_animateEndTile = endTile;
		_animateTime = time;
		_animateRemainingLoops = -1;
		_animateLastUpdate = Rokon.getTime();
		_animateRandom = true;
		setTileIndex((int)(Math.random() * (endTile - startTile)) + startTile);
	}

	// Add Custom Tile Animation
	/**
	 * Animates a Sprite by using custom tiles from its Texture. This will loop indefinately.
	 * @param tileCustom is an array containing the tiles indexes of the animation
	 * @param time the time in milliseconds between each frame
	 */
	public void animateCustom(int [] tileCustom, float time) {
		_animating = true;
		_animateStartTile = 0;
		_animateEndTile = tileCustom.length-1;
		_customTile = tileCustom;
		_animateTime = time;
		_animateRemainingLoops = -1;
		_animateLastUpdate = Rokon.getTime();
		setTileIndex(tileCustom[0]);
	}
	
	/**
	 * Animates a Sprite by using custom tiles from its Texture
	 * @param tileCustom is an array containing the tiles indexes of the animation
	 * @param time the time in milliseconds between each frame
	 * @param loops the number of loops to go through the animation
	 * @param returnToStart TRUE if the Sprite should return to startTile when complete, FALSE if the Sprite should stay at endTile when complete
	 */
	public void animateCustom(int [] tileCustom, float time, int loops, boolean returnToStart) {
		_animating = true;
		_animateStartTile = tileCustom[0];
		_animateEndTile = tileCustom[tileCustom.length-1];
		_animateTime = time;
		_animateRemainingLoops = loops;
		_animateReturnToStart = returnToStart;
		_animateLastUpdate = Rokon.getTime();
		setTileIndex(tileCustom[0]);
	}
	// End Add

	/**
	 * Stops the animation, and leaving the Sprite at its current frame 
	 */
	public void stopAnimation() {
		_animating = false;
	}
	
	/**
	 * @return TRUE if the Sprite is being animated
	 */
	public boolean isAnimating() {
		return _animating;
	}
	
	
	/**
	 * Resets the sprite to the initial conditions
	 */
	public void reset() {
		reset(true);
	}
	
	/**
	 * Resets the sprite to the initial conditions
	 * @param resetTexture TRUE if the texture is to be reset to the first tile
	 */
	public void reset(boolean resetTexture) {
		super.reset();
		stop();
		stopAnimation();
		if(resetTexture)
			setTileIndex(1);
	}
	
	/**
	 * Revives the Sprite, so that it can be used again.
	 */
	public void revive() {
		_killMe = false;
	}
}