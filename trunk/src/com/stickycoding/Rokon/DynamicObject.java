package com.stickycoding.Rokon;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

import android.os.Build;

import com.stickycoding.Rokon.Handlers.DynamicsHandler;

/**
 * @author Richard
 * Handles all positions, rotations and movement
 * Extended by Sprite, Emitter, Particle 
 * Created to bring together all dynamic methods for different types of object
 */
public class DynamicObject {
	public static final int MAX_MODIFIERS = 5;
	private int i, j, r;
	
	private SpriteModifier[] _modifierArr = new SpriteModifier[MAX_MODIFIERS];
	public Texture _texture;
	public int _tileX;
	public int _tileY;
	public ByteBuffer _texBuffer;

	private float _startX, _startY, _startWidth, _startHeight;
	private float _x, _y, _offsetX, _offsetY;
	private float _rotation, _rotationPivotX, _rotationPivotY;
	private boolean _rotationPivotRelative = true;
	private float _width, _height, _scaleX, _scaleY;

	public float _red;
	public float _green;
	public float _blue;
	public float _alpha;
	
	private float _terminalVelocityX, _terminalVelocityY;
	private float _velocityX, _velocityY;
	private float _accelerationX, _accelerationY;
	private boolean _stopAtTerminalVelocity, _triggeredReachTerminalVelocityX, _triggeredReachTerminalVelocityY;
	private long _lastUpdate;
	
	private DynamicsHandler _dynamicsHandler;
	
	private long _timeDiff;
	private float _timeDiffModifier;
	public boolean _visible;

	private ByteBuffer _vertexBuffer;
	
	public DynamicObject(float x, float y, float width, float height) {
		_x = x;
		_y = y;
		_alpha = 1;
		_startX = x;
		_startY = y;
		_startWidth = width;
		_startHeight = height;
		_width = width;
		_height = height;
		_scaleX = 1;
		_scaleY = 1;
		_offsetX = 0;
		_offsetY = 0;
		_rotationPivotX = (_width / 2);
		_rotationPivotY = (_height / 2);
		_visible = true;

		if(Build.VERSION.SDK == "3")
			_vertexBuffer = ByteBuffer.allocate(8*4);
		else
			_vertexBuffer = ByteBuffer.allocateDirect(8*4);
		_vertexBuffer.order(ByteOrder.nativeOrder());
		setLastUpdate();
	}
	/**
	 * Updates the texture buffers used by OpenGL, there should be no need to call this
	 */
	public void updateBuffers() {
		_updateTextureBuffer();
	}
	/**
	 * @param tileIndex the index of the Texture tile to be used by the Sprite, 1-based
	 */
	public void setTileIndex(int tileIndex) {
		if(_texture == null) {
			Debug.print("Error - Tried setting tileIndex of null texture");
			return;			
		}
		tileIndex -= 1;
		_tileX = (tileIndex % _texture.getTileColumnCount()) + 1;
		_tileY = ((tileIndex - (_tileX - 1)) / _texture.getTileColumnCount()) + 1;
		tileIndex += 1;
		//Debug.print("Updating tile index idx=" + tileIndex + " x=" + _tileX + " y=" + _tileY);
		_updateTextureBuffer();
	}
	/**
	 * @param spriteModifier a SpriteModifier to add the Sprite 
	 */
	public void addModifier(SpriteModifier spriteModifier) {
		j = -1;
		for(i = 0; i < MAX_MODIFIERS; i++)
			if(_modifierArr[i] == null)
				j = i;
		if(j == -1) {
			Debug.print("TOO MANY SPRITE MODIFIERS");
			return;
		}
		_modifierArr[j] = spriteModifier;
	}
	
	/**
	 * @param spriteModifier a SpriteModifier to remove from the Sprite
	 */
	public void removeModifier(SpriteModifier spriteModifier) {
		for(i = 0; i < MAX_MODIFIERS; i++)
			if(_modifierArr[i].equals(spriteModifier))
				_modifierArr[i] = null;
	}
	
	public void resetModifiers() {
		for(i = 0; i < MAX_MODIFIERS; i++)
			//TODO dont know why of this try catch
			try {
				_modifierArr[i] = null;
			} catch (Exception e) { }
	}
	
	protected void updateModifiers() {
		for(r = 0; r < MAX_MODIFIERS; r++)
			if(_modifierArr[r] != null) {
				_modifierArr[r].onUpdate(this);
				if(_modifierArr[r].isExpired())
					_modifierArr[r] = null;
			}
	}
	private float x1, y1, x2, y2, xs, ys, fx1, fx2, fy1, fy2;
	private void _updateTextureBuffer() {		
		if(_texture == null)
			return;
		
		if(_texture.getTextureAtlas() == null)
			return;
		
		x1 = _texture.getAtlasX();
		y1 = _texture.getAtlasY();
		x2 = _texture.getAtlasX() + _texture.getWidth();
		y2 = _texture.getAtlasY() + _texture.getHeight();

		xs = (x2 - x1) / _texture.getTileColumnCount();
		ys = (y2 - y1) / _texture.getTileRowCount();

		x1 = _texture.getAtlasX() + (xs * (_tileX - 1));
		x2 = _texture.getAtlasX() + (xs * (_tileX - 1)) + xs; 
		y1 = _texture.getAtlasY() + (ys * (_tileY - 1));
		y2 = _texture.getAtlasY() + (ys * (_tileY - 1)) + ys; 
		
		fx1 = x1 / (float)_texture.getTextureAtlas().getWidth();
		fx2 = x2 / (float)_texture.getTextureAtlas().getWidth();
		fy1 = y1 / (float)_texture.getTextureAtlas().getHeight();
		fy2 = y2 / (float)_texture.getTextureAtlas().getHeight();
		
		if(!_texture.isFlipped()) {
			_texBuffer.position(0);		
			_texBuffer.putFloat(fx1); _texBuffer.putFloat(fy1);
			_texBuffer.putFloat(fx2); _texBuffer.putFloat(fy1);
			_texBuffer.putFloat(fx1); _texBuffer.putFloat(fy2);
			_texBuffer.putFloat(fx2); _texBuffer.putFloat(fy2);		
			_texBuffer.position(0);
		} else {
			_texBuffer.position(0);		
			_texBuffer.putFloat(fx1); _texBuffer.putFloat(fy2);
			_texBuffer.putFloat(fx2); _texBuffer.putFloat(fy2);	
			_texBuffer.putFloat(fx1); _texBuffer.putFloat(fy1);
			_texBuffer.putFloat(fx2); _texBuffer.putFloat(fy1);	
			_texBuffer.position(0);
		}
	}
	
	/**
	 * @param texture applies a Texture to the Sprite
	 */
	public void setTexture(Texture texture) {
		_texture = texture;
		_tileX = 1;
		_tileY = 1;
		_updateTextureBuffer();
	}
	/**
	 * Sets the Texture tile index to be used by the Sprite by columns and rows, rather than index
	 * @param tileX column
	 * @param tileY row
	 */
	public void setTile(int tileX, int tileY) {
		_tileX = tileX;
		_tileY = tileY;
		_updateTextureBuffer();
	}
	
	/**
	 * Sets the offset at which the sprite is drawn on screen
	 * @param offsetX
	 * @param offsetY
	 */
	public void setOffset(float offsetX, float offsetY) {
		_offsetX = offsetX;
		_offsetY = offsetY;
		updateVertexBuffer();
	}
	
	/**
	 * Updates the vertex buffer, should be done if you modify any variables manually
	 */
	public void updateVertexBuffer() {
		_vertexBuffer.position(0);
		
		_vertexBuffer.putFloat(_x + _offsetX);
		_vertexBuffer.putFloat(_y + _offsetY);

		_vertexBuffer.putFloat(_x + _offsetX + (_width * _scaleX));
		_vertexBuffer.putFloat(_y + _offsetY);

		_vertexBuffer.putFloat(_x + _offsetX);
		_vertexBuffer.putFloat(_y + _offsetX + (_height * _scaleY));

		_vertexBuffer.putFloat(_x + _offsetX + (_width * _scaleX));
		_vertexBuffer.putFloat(_y + _offsetX + (_height * _scaleY));
		
		_vertexBuffer.position(0);
	}
	
	/**
	 * Draws the Sprite to the OpenGL object, should be no need to call this
	 * @param gl
	 */
	private boolean hasTexture;
	public void drawFrame(GL10 gl) {
		if(!_visible)
			return;
		
		if(notOnScreen())
			return;
		
		if(_texture == null)
			hasTexture = false;
		else
			hasTexture = true;
		
		if(!hasTexture) {
			gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
			gl.glDisable(GL10.GL_TEXTURE_2D);
		} else {
			_texture.select(gl);
		}
		
		gl.glLoadIdentity();
		gl.glVertexPointer(2, GL11.GL_FLOAT, 0, getVertexBuffer());

		for(i = 0; i < MAX_MODIFIERS; i++)
			if(_modifierArr[i] != null)
				_modifierArr[i].onDraw(this, gl);

		if(getRotation() != 0) {
			if (getRotationPivotRelative()) {
				gl.glTranslatef(getX() + (getScaleX() * getRotationPivotX()), getY() + (getScaleY() * getRotationPivotY()), 0);
				gl.glRotatef(getRotation(), 0, 0, 1);
				gl.glTranslatef(-1 * (getX() + (getScaleX() * getRotationPivotX())), -1 * (getY() + (getScaleY() * getRotationPivotY())), 0);
			} else {
				gl.glTranslatef(getRotationPivotX(), getRotationPivotY(), 0);
				gl.glRotatef(getRotation(), 0, 0, 1);
				gl.glTranslatef(-1 * getRotationPivotX(), -1 * getRotationPivotY(), 0);
			}
		}

		gl.glColor4f(_red, _green, _blue, _alpha);
		if(hasTexture)
			gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, _texBuffer);	

		gl.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4);
		
		if(!hasTexture) {
			gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
			gl.glEnable(GL10.GL_TEXTURE_2D);
		}
	}
	
	/**
	 * @param rotation angle, in degrees, to rotate the Sprite relative to its current angle
	 */
	public void rotate(float rotation) {
		_rotation += rotation;
	}
	
	
	/**
	 * @param rotation angle, in degrees, to set the Sprite's rotation
	 */
	public void setRotation(float rotation) {
		_rotation = rotation;
	}
	
	/**
	 * @return the current angle, in degrees, at which the Sprite is at
	 */
	public float getRotation() {
		return _rotation;
	}
	
	/**
	 * Sets the rotation pivot x coordinate
	 * @param x
	 */
	public void setRotationPivotX(float x) {
		_rotationPivotX = x;
	}
	
	/**
	 * Get rotation pivot x coordinate
	 */
	public float getRotationPivotX() {
		return _rotationPivotX;
	}
	
	/**
	 * Sets the rotation pivot x coordinate
	 * @param x
	 */
	public void setRotationPivotY(float y) {
		_rotationPivotY = y;
	}
	
	/**
	 * Get rotation pivot y coordinate
	 */
	public float getRotationPivotY() {
		return _rotationPivotY;
	}
	
	/**
	 * Defines rotation pivot coordinates as relative to the sprite. 
	 * This does not change the actual pivot coordinates, but defines how the
	 * pivot coordinates are interpreted when rotating
	 */
	public void setRotationPivotRelative() {
		_rotationPivotRelative = true;
	}
	
	/**
	 * Defines rotation pivot coordinates as absolute/fixed (not relative to sprite). 
	 * This does not change the actual pivot coordinates, but defines how the
	 * pivot coordinates are interpreted when rotating
	 */
	public void setRotationPivotAbsolute() {
		_rotationPivotRelative = false;
	}
	
	/**
	 * @return TRUE if rotation pivot is relative, FALSE if absolute/fixed
	 */
	public boolean getRotationPivotRelative() {
		return _rotationPivotRelative;
	}
	
	/**
	 * @param scaleX a multiplier to scale your Sprite in the X direction when drawing
	 */
	public void setScaleX(float scaleX) {
		_scaleX = scaleX;
		updateVertexBuffer();
	}
	
	/**
	 * @return the current scale multiplier in X direction
	 */
	public float getScaleX() {
		return _scaleX;
	}
	
	/**
	 * @param scaleY a multiplier to scale your Sprite in the Y direction when drawing
	 */
	public void setScaleY(float scaleY) {
		_scaleY = scaleY;
		updateVertexBuffer();
	}
	
	/**
	 * @return the current scale multiplier in Y direction
	 */
	public float getScaleY() {
		return _scaleY;
	}
	
	/**
	 * Note that scale is not considered in collisions
	 * @param scaleX a multiplier to scale your Sprite in the X direction when drawing
	 * @param scaleY a multiplier to scale your Sprite in the Y direction when drawing
	 */
	public void setScale(float scaleX, float scaleY) {
		_scaleX = scaleX;
		_scaleY = scaleY;
		updateVertexBuffer();
	}

	/**
	 * @param x the top left position of your Sprite, in the X direction
	 */
	public void setX(float x) {
		_x = x;
		updateVertexBuffer();
	}
	
	/**
	 * @param y the top left position of your Sprite, in the Y direction
	 */
	public void setY(float y) {
		_y = y;
		updateVertexBuffer();
	}
	
	/**
	 * Sets the position of the Sprite, in pixels
	 * @param x 
	 * @param y
	 */
	public void setXY(float x, float y) {
		_x = x;
		_y = y;
		updateVertexBuffer();
	}
	
	/**
	 * @param x number of pixels to move the Sprite relative to its current position
	 */
	public void moveX(float x) {
		_x += x;
		updateVertexBuffer();
	}

	
	/**
	 * @param u number of pixels to move the Sprite relative to its current position
	 */
	public void moveY(float y) {
		_y += y;
		updateVertexBuffer();
	}
	
	/**
	 * Moves the Sprite relative to its current position
	 * @param x
	 * @param y
	 */
	public void move(float x, float y) {
		_x += x;
		_y += y;
		updateVertexBuffer();
	}
	
	/**
	 * @return the top left X position of the Sprite
	 */
	public float getX() {
		return _x;
	}
	
	/**
	 * @return the top left X position of the Sprite, rounded to the nearest integer
	 */
	public int getScreenX() {
		return (int)_x;
	}
	
	/**
	 * @return the top left X position of the Sprite
	 */
	public float getY() {
		return _y;
	}
	
	/**
	 * @return the top left Y position of the Sprite, rounded to the nearest integer
	 */
	public int getScreenY() {
		return (int)_y;
	}
	/**
	 * @param width width of the Sprite, used for collisions and multiplied by scale when drawn
	 */
	public void setWidth(float width) {
		_width = width;
	}
	
	/**
	 * @param width width of the Sprite, used for collisions and multiplied by scale when drawn
	 * @param start TRUE if startWidth should be set also
	 */
	public void setWidth(float width, boolean start) {
		_width = width;
		_startWidth = width;
	}

	/**
	 * @param height height of the Sprite, used for collisions and multiplied by scale when drawn
	 */
	public void setHeight(float height) {
		_height = height;
	}
	
	/**
	 * @param height height of the Sprite, used for collisions and multiplied by scale when drawn
	 * @param start TRUE if startHeight should be set also
	 */
	public void setHeight(float height, boolean start) {
		_height = height;
		_startHeight = _height;
	}
	
	/**
	 * @return current width of the Sprite
	 */
	public float getWidth() {
		return _width;
	}
	
	/**
	 * @return current height of the Sprite
	 */
	public float getHeight() {
		return _height;
	}
	
	/**
	 * @return current width of the Sprite, rounded to the nearest Integer
	 */
	public int getScreenWidth() {
		return (int)_width;
	}
	
	/**
	 * @return current height of the Sprite, rounded to the nearest Integer
	 */
	public int getScreenHeight() {
		return (int)_height;
	}
	
	/**
	 * @return the ByteBuffer for vertices
	 */
	public ByteBuffer getVertexBuffer() {
		return _vertexBuffer;
	}
	
	/**
	 * @return the time of the last movement update to the DynamicObject
	 */
	public long getLastUpdate() {
		return _lastUpdate;
	}
	
	/**
	 * Sets the time of the last update to the current frame time
	 */
	public void setLastUpdate() {
		_lastUpdate = Rokon.time;
	}
	
	/**
	 * Sets the time of the last update to an arbitrary value
	 * @param time
	 */
	public void setLastUpdate(long time) {
		_lastUpdate = time;
	}
	
	/**
	 * Updates movement
	 */
	public void updateMovement() {
		updateModifiers();
		if(_accelerationX != 0 || _accelerationY != 0 || _velocityX != 0 || _velocityY != 0) {
			_timeDiff = Rokon.getTime() - _lastUpdate;
			_timeDiffModifier = (float)_timeDiff / 1000f;
			if(_accelerationX != 0 || _accelerationY != 0) {
				_velocityX += _accelerationX * _timeDiffModifier;
				_velocityY += _accelerationY * _timeDiffModifier;
				if(_stopAtTerminalVelocity) {
					if(!_triggeredReachTerminalVelocityX) {
						if((_accelerationX > 0.0f && _velocityX >= _terminalVelocityX)
						|| (_accelerationX < 0.0f && _velocityX <= _terminalVelocityX)) {
							if(_dynamicsHandler != null)
								_dynamicsHandler.reachedTerminalVelocityX();
							_accelerationX = 0;
							_velocityX = _terminalVelocityX;
							_triggeredReachTerminalVelocityX = true;
						}
					}
					if(!_triggeredReachTerminalVelocityY) {
						if((_accelerationY > 0.0f && _velocityY >= _terminalVelocityY)
						|| (_accelerationY < 0.0f && _velocityY <= _terminalVelocityY)) {
							if(_dynamicsHandler != null)
								_dynamicsHandler.reachedTerminalVelocityY();
							_accelerationY = 0;
							_velocityY = _terminalVelocityY;
							_triggeredReachTerminalVelocityY = true;
						}
					}
				}
			}
			_x += _velocityX * _timeDiffModifier;
			_y += _velocityY * _timeDiffModifier;
			updateVertexBuffer();
		}
		setLastUpdate();
	}

	/**
	 * @param dynamicsHandler sets a handler for the dynamics, this can track acceleration
	 */
	public void setDynamicsHandler(DynamicsHandler dynamicsHandler) {
		_dynamicsHandler = dynamicsHandler;
	}
	
	/**
	 * Removes the DynamicsHandler from the Sprite
	 */
	public void resetDynamicsHandler() {
		_dynamicsHandler = null;
	}
	
	/**
	 * Stops the Sprite, setting acceleration and velocities to zero
	 */
	public void stop() {
		resetDynamics();
	}
	
	public void resetDynamics() {
		_terminalVelocityX = 0;
		_terminalVelocityY = 0;
		_stopAtTerminalVelocity = false;
		_velocityX = 0;
		_velocityY = 0;
		_accelerationX = 0;
		_accelerationY = 0;
	}
	
	/**
	 * Accelerates a Sprite, note that this is relative to current Acceleration.
	 * @param accelerationX acceleration in X direction, pixels per second
	 * @param accelerationY acceleration in Y direction, pixels per second
	 * @param terminalVelocityX specifies a highest possible velocity in X direction, this will trigger reachedTerminalVelocityX
	 * @param terminalVelocityY specifies a highest possible velocity in Y direction, this will trigger reachedTerminalVelocityY
	 */
	public void accelerate(float accelerationX, float accelerationY, float terminalVelocityX, float terminalVelocityY) {
		_stopAtTerminalVelocity = true;
		_terminalVelocityX = terminalVelocityX;
		_terminalVelocityY = terminalVelocityY;
		_accelerationX += accelerationX;
		_accelerationY += accelerationY;
		_triggeredReachTerminalVelocityX = false;
		_triggeredReachTerminalVelocityY = false;
		setLastUpdate();
	}
	
	/**
	 * Accelerates a Sprite, note that this is relative to current Acceleration. Terminal velocity restrictions are removed.
	 * @param accelerationX acceleration in X direction, pixels per second
	 * @param accelerationY acceleration in Y direction, pixels per second
	 */
	public void accelerate(float accelerationX, float accelerationY) {
		_stopAtTerminalVelocity = false;
		_accelerationX += accelerationX;
		_accelerationY += accelerationY;
		setLastUpdate();
	}
	
	/**
	 * @return current acceleration in X direction, pixels per second
	 */
	public float getAccelerationX() {
		return _accelerationX;
	}
	/**
	 * @return current acceleration in Y direction, pixels per second
	 */
	public float getAccelerationY() {
		return _accelerationY;
	}
	
	/**
	 * @return current velocity in X direction, pixels per second
	 */
	public float getVelocityX() {
		return _velocityX;
	}
	
	/**
	 * @return current velocity in Y direction, pixels per second
	 */
	public float getVelocityY() {
		return _velocityY;
	}
	
	/**
	 * @param velocityX instantly sets the velocity of the Sprite in X direction, pixels per second
	 */
	public void setVelocityX(float velocityX) {
		_velocityX = velocityX;
	}
	
	/**
	 * @param velocityY instantly sets the velocity of the Sprite in Y direction, pixels per second
	 */
	public void setVelocityY(float velocityY) {
		_velocityY = velocityY;
	}
	
	/**
	 * Instantly sets the velocity of te Sprite in X and Y directions, pixels per second
	 * @param velocityX
	 * @param velocityY
	 */
	public void setVelocity(float velocityX, float velocityY) {
		_velocityX = velocityX;
		_velocityY = velocityY;
	}
	
	/**
	 * @return the current terminal velocity cap in X direction
	 */
	public float getTerminalVelocityX() {
		return _terminalVelocityX;
	}
	
	/**
	 * @return the current terminal velocity cap in Y direction
	 */
	public float getTerminalVelocityY() {
		return _terminalVelocityY;
	}
	
	/**
	 * @param stopAtTerminalVelocity TRUE if Sprite should stop at the terminal velocity, FALSE if it should continue accelerating
	 */
	public void setStopAtTerminalVelocity(boolean stopAtTerminalVelocity) {
		_stopAtTerminalVelocity = stopAtTerminalVelocity;
	}
	
	/**
	 * @return TRUE if the Sprite is going to stop when it reaches terminal velocity, FALSE if it will continue indefinately
	 */
	public boolean isStopAtTerminalVelocity() {
		return _stopAtTerminalVelocity;
	}
	
	/**
	 * Sets a terminal velocity at which the Sprite will stop accelerating, this will trigger reachedTerminalVelocityX and reachedTerminalVelocityY in your DynamicsHandler if set
	 * @param terminalVelocityX
	 * @param terminalVelocityY
	 */
	public void setTerminalVelocity(float terminalVelocityX, float terminalVelocityY) {
		_stopAtTerminalVelocity = true;
	}
	
	public void setTerminalVelocityX(float terminalVelocityX) {
		_terminalVelocityX = terminalVelocityX;
	}
	
	public void setTerminalVelocityY(float terminalVelocityY) {
		_terminalVelocityY = terminalVelocityY;
	}
	
	/**
	 * Increases the current velocity by a given value
	 * @param velocityX
	 * @param velocityY
	 */
	public void setVelocityRelative(float velocityX, float velocityY) {
		_velocityX += velocityX;
		_velocityY += velocityY;
	}
	
	/**
	 * Resets the X, Y, and dimenensions to their original value
	 */
	public void reset() {
		_x = _startX;
		_y = _startY;
		_width = _startWidth;
		_height = _startHeight;
	}
	
	/**
	 * @return TRUE if the DynamicObject does not appear to be on-screen
	 */
	public boolean notOnScreen() {
		if(Rokon.getRokon().isForceOffscreenRender())
			return false;
		if(_x + _width < 0 || _x > Rokon.getRokon().getWidth())
			return true;
		if(_y + _height < 0 || _y > Rokon.getRokon().getHeight())
			return true;
		return false;
	}
}
