/**
 * By Michael Lee (563550)
 * 
 * Package largely adapted from AIController in controller package
 * but with added attributes and methods to implement a better behaviour model
 * In our sequence diagram, we had MyAIController be a child class of CarController but for the
 * implementation, we decided to have it as a child class under AIController since we are
 * making use of its methods and attributes
 */

package mycontroller;

import java.util.HashMap;
import controller.AIController;
import tiles.MapTile;
import utilities.Coordinate;
import world.Car;
import world.WorldSpatial;

public class MyAIController extends AIController{
	
	// New Attributes
	private boolean isThreePointTurning = false;
	private boolean isUTurning = false;
	private boolean isReversingOut = false;
	// Previously called state in sequence diagram, also changed to an enum from a String
	private statesForTraps currStateForTraps = statesForTraps.IDLE; 
	private Coordinate bestPosition;
	

	public MyAIController(Car car) {
		super(car);
	}

	@Override
	public void update(float delta) {

		
		// Gets what the car can see
		HashMap<Coordinate, MapTile> currentView = getView();
		
		
		checkStateChange();


		// If you are not following a wall initially, find a wall to stick to!
		if(!getIsFollowingWall()){
			if(getVelocity() < getCAR_SPEED()){
				applyForwardAcceleration();
			}
			// Turn towards the north
			if(!getOrientation().equals(WorldSpatial.Direction.NORTH)){
				setLastTurnDirection(WorldSpatial.RelativeDirection.LEFT);
				applyLeftTurn(getOrientation(),delta);
			}
			if(checkNorth(currentView)){
				// Turn right until we go back to east!
				if(!getOrientation().equals(WorldSpatial.Direction.EAST)){
					setLastTurnDirection(WorldSpatial.RelativeDirection.RIGHT);
					applyRightTurn(getOrientation(),delta);
				}
				else{
					setIsFollowingWall(true);
				}
			}
		}
		// Once the car is already stuck to a wall, apply the following logic
		else{
			
			// Readjusts the car if it is not aligned.
			readjust(getLastTurnDirection(),delta);
			
			if(getIsTurningRight()){
				applyRightTurn(getOrientation(),delta);
			}
			else if(getIsTurningLeft()){
				// Apply the left turn if you are not currently near a wall.
				if(!checkFollowingWall(getOrientation(),currentView)){
					applyLeftTurn(getOrientation(),delta);
				}
				else{
					setIsTurningLeft(false);
				}
			}
			// Try to determine whether or not the car is next to a wall.
			else if(checkFollowingWall(getOrientation(),currentView)){
				// Maintain some velocity
				if(getVelocity() < getCAR_SPEED()){
					applyForwardAcceleration();
				}
				// If there is wall ahead, turn right!
				if(checkWallAhead(getOrientation(),currentView)){
					setLastTurnDirection(WorldSpatial.RelativeDirection.RIGHT);
					setIsTurningRight(true);				
					
				}

			}
			// This indicates that I can do a left turn if I am not turning right
			else{
				setLastTurnDirection(WorldSpatial.RelativeDirection.LEFT);
				setIsTurningLeft(true);
			}
		}
		
	}
	
	/* OWN METHODS */
	
	/* enum for possible states when dealing with traps, default is IDLE*/
	private enum statesForTraps { IDLE, SEARCHING, END }
	
	/**
	 * Check if trap is ahead
	 * @param currentView
	 * @return
	 */
	private boolean isTrapAhead(HashMap<Coordinate, MapTile> currentView) {
		// TODO
		return false;
	}
	
	/**
	 * Check if encountering dead end
	 * @param currentView
	 * @return
	 */
	private boolean isInDeadEnd(HashMap<Coordinate, MapTile> currentView) {
		// TODO
		return false;
	}
	
	/**
	 * Calculating the score of the traps, more or less the damage they deal to the car
	 * @param currentView
	 * @return
	 */
	private int scoreOfTraps(HashMap<Coordinate, MapTile> currentView) {
		// TODO
		return 0;
	}
	
	/**
	 * Setting the best position the car can be
	 * @param position
	 * @return
	 */
	private void setBestPosition(Coordinate position) {
		// TODO
	}
	
	/**
	 * Move the car to the best position
	 * @param position
	 * @return
	 */
	private void moveToBestPosition(Coordinate position) {
		// TODO
	}
	
	/**
	 * Checks if the car can proceed, basically seeing if there is a trap or wall up ahead
	 * Previously named carGo
	 * @param orientation
	 * @param currentView
	 * @return
	 */
	private boolean isTrapOrWallAhead(WorldSpatial.Direction orientation, HashMap<Coordinate, MapTile> currentView) {
		// TODO
		return false;
	}
	
	/**
	 * Checks if the car is able to turn to the right
	 * @param orientation
	 * @param currentView
	 * @return
	 */
	private boolean isOkToTurnRight(WorldSpatial.Direction orientation, HashMap<Coordinate, MapTile> currentView) {
		// TODO
		return false;
	}
	
	/**
	 * Use the 3-point turn on the car
	 * @param orientation
	 * @param delta
	 * @return
	 */
	private void applyThreePointTurn(WorldSpatial.Direction orientation, float delta) {
		// TODO
	}

	/**
	 * Use a U-Turn on the car
	 * @param orientation
	 * @param delta
	 * @return
	 */
	private void applyUTurn(WorldSpatial.Direction orientation, float delta) {
		// TODO
	}
	
	/**
	 * Have the car reverse out
	 * @param orientation
	 * @param delta
	 * @return
	 */
	private void applyReverseOut(WorldSpatial.Direction orientation, float delta) {
		// TODO
	}

}
