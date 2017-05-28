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
import java.util.LinkedList;

import controller.CarController;
import tiles.MapTile;
import utilities.Coordinate;
import world.Car;
import world.WorldSpatial;
import world.WorldSpatial.RelativeDirection;

public class MyAIController extends CarController{
	
	// ATTRIBUTES FROM AIController CLASS
	// How many minimum units the wall is away from the player.
	private int wallSensitivity = 2;
	
	
	private boolean isFollowingWall = false; // This is initialized when the car sticks to a wall.
	private WorldSpatial.RelativeDirection lastTurnDirection = null; // Shows the last turn direction the car takes.
	private boolean isTurningLeft = false;
	private boolean isTurningRight = false; 
	private WorldSpatial.Direction previousState = null; // Keeps track of the previous state
	
	// Car Speed to move at
	private final float CAR_SPEED = 3;
	
	// Offset used to differentiate between 0 and 360 degrees
	private int EAST_THRESHOLD = 3;
	
	// OWN ATTRIBUTES FROM THIS POINT
	
	// Need to use these so car will not speed up too quickly when turning into trap
	// Some instances they are magic numbers since they were used for testing and due to time constraints
	private static final int DIVIDE_CAR_SPEED_BY_3 = 3;
	private static final int DIVIDE_CAR_SPEED_BY_2 = 2;
	
	// Attributes for reversing, encoutering dead ends
	private boolean isDeadEnd = false;
	private boolean onTheWayOut = false; // was in sequence diagram but was not included in class diagram
	private boolean isThreePointTurning = false;
	private boolean isUTurning = false;
	private boolean isReversingOut = false;
	
	// Relate to Traps
	private String bestPositionString;
	private int bestScore = 100;
	
	// used to check whether the right turn is finished
	private WorldSpatial.Direction directionAfterTurnRight;
	private WorldSpatial.Direction directionAfterTurnLeft;
	
	/**
	 * enum for possible states when dealing with traps
	 * IDLE no traps nearby
	 * TURNING_RIGHT when trap ahead and the road is enough to turn AND no trap at right side, then turn right
	 * SERACHING after turning right, try to find a best place to turn left
	 * TURNING_LEFT as per title
	 * BACK go back to best place
	 * STOP stop at best place
	 * PASSING use the fastest speed pass the trap
	 * END after passing the trap, we need to slow down
	 *
	 */
	private enum StatesForTraps { IDLE, TURNING_RIGHT, SEARCHING, TURNING_LEFT, BACK, STOP, PASSING, END }

	/* Previously called state in sequence diagram, also changed to an enum from a String */
	private StatesForTraps currStateForTraps = StatesForTraps.IDLE; 
	
	
	/** 
	 * enum for RelativeDirection
	 * Had our own one here in MyAIController since we cannot modify World.Spatial
	 * in the world package, which is what we done previously
	 * */
	private static enum RelativeDirection { FRONT, BACK, LEFT, RIGHT }
	public MyAIController(Car car) {
		super(car);
	}

	@Override
	public void update(float delta) {
		
		// Gets what the car can see
		HashMap<Coordinate, MapTile> currentView = getView();
		
		checkStateChange();
		// check if car is at dead end
		isDeadEnd = isDeadEndAhead(getOrientation(), currentView);
		int roadWidth = getRoadWidth(getOrientation(), currentView);
		if (isDeadEnd) {
			if (roadWidth <= 1) {
				// Reverse the car, road too narrow
				applyReverseOut();
			}
			else if (roadWidth > 1 && roadWidth <= 3) {
				//not that wide, use 3 point turn
				applyThreePointTurn(getOrientation(), delta);
			}
			else if (roadWidth >= 3) {
				// Enough width for a U-Turn
				applyUTurn(getOrientation(), currentView, delta);
			}
		}
		// DO OUR SEQ DIAGRAM SHIT HERE!	
		if (currStateForTraps == StatesForTraps.IDLE && isTrapAhead(currentView, getOrientation())) {
			
			LinkedList<MapTile> tilesRight = getViewOfASide(RelativeDirection.RIGHT, getOrientation(),
					currentView);
			// the road is too narrow to turn or the tile on the right side is a trap
			// just go through
			if (roadWidth == 1 || tilesRight.get(0).getName().equals("Trap")) {
				currStateForTraps = StatesForTraps.PASSING;
				return;
			}

			directionAfterTurnRight = getDirectionAfterTurnRight(getOrientation());
			currStateForTraps = StatesForTraps.TURNING_RIGHT;
			bestPositionString = getPosition();

			bestScore = damageFromTraps(RelativeDirection.LEFT, currentView, getOrientation());
			return;
		}
		if (currStateForTraps == StatesForTraps.TURNING_RIGHT) {
			if (getVelocity() < (CAR_SPEED / DIVIDE_CAR_SPEED_BY_3)) {
				applyForwardAcceleration();
			}
			if (getOrientation() != directionAfterTurnRight) {
				lastTurnDirection = WorldSpatial.RelativeDirection.RIGHT;
				applyRightTurn(getOrientation(), delta);
			} else {
				currStateForTraps = StatesForTraps.SEARCHING;
			}
			return;
		}
		if (currStateForTraps == StatesForTraps.SEARCHING) {
			if (isTrapOrWallAhead(getOrientation(), currentView)) {
				currStateForTraps = StatesForTraps.BACK;
				return;
			}
			// accelerate
			if (getVelocity() < (CAR_SPEED / DIVIDE_CAR_SPEED_BY_3)) {
				applyForwardAcceleration();
			}
			int score = damageFromTraps(RelativeDirection.LEFT, currentView, getOrientation());
			// no traps
			if (score == 0) {
				currStateForTraps = StatesForTraps.TURNING_LEFT;
				directionAfterTurnLeft = getDirectionAfterTurnLeft(getOrientation());
				return;
			}
			Coordinate currentPosition = new Coordinate(getPosition());
			Coordinate bestPosition = new Coordinate(bestPositionString);
			if(currentPosition.equals(bestPosition)){
				bestPosition = currentPosition;
			}
			// better place with less traps
			if (score < bestScore) {

				bestScore = score;
				bestPosition = currentPosition;
			}
			return;
		}

		// go back to the best place
		if (currStateForTraps == StatesForTraps.BACK) {
			if (shouldBrake(getPosition(), bestPositionString, getVelocity(), getOrientation())) {
				currStateForTraps = StatesForTraps.STOP;
			} else {
				// Backward accelerate
				if (getVelocity() < CAR_SPEED) {
					applyReverseAcceleration();
				}
				return;
			}

		}
		
		if(currStateForTraps == StatesForTraps.STOP){
			if (getVelocity() != 0) {
				applyBrake();
				return;
			} else {
				// Coordinate currentPosition = new Coordinate(getPosition());
				currStateForTraps = StatesForTraps.TURNING_LEFT;
				directionAfterTurnLeft = getDirectionAfterTurnLeft(getOrientation());
				return;
			}
		}

		if (currStateForTraps == StatesForTraps.TURNING_LEFT) {

			if (getOrientation() != directionAfterTurnLeft) {
				if (getVelocity() < (CAR_SPEED / DIVIDE_CAR_SPEED_BY_2)) {
					applyForwardAcceleration();
				}

				lastTurnDirection = WorldSpatial.RelativeDirection.LEFT;
				applyLeftTurn(getOrientation(), delta);
				return;
			} else {
				currStateForTraps = StatesForTraps.PASSING;
			}
			return;
		}

		// use the fastest speed possible to traverse it
		if (currStateForTraps == StatesForTraps.PASSING) {
			if (getVelocity() < CAR_SPEED / 1.3) {
				applyForwardAcceleration();
				return;
			} else {
				currStateForTraps = StatesForTraps.END;
				return;
			}
		}

		// cause the speed is very fast in order to pass the traps
		// after we traverse the trap, we need to slow down
		if (currStateForTraps == StatesForTraps.END) {
			if (getVelocity() > (CAR_SPEED / DIVIDE_CAR_SPEED_BY_2)) {
				applyBrake();
				return;
			} else {
				currStateForTraps = StatesForTraps.IDLE;
				return;
			}
		}

		
		// If you are not following a wall initially, find a wall to stick to!
		if(!isFollowingWall){
			
			if(getVelocity() < CAR_SPEED / DIVIDE_CAR_SPEED_BY_2){
				applyForwardAcceleration();
			}
			// Turn towards the north
			if(!getOrientation().equals(WorldSpatial.Direction.NORTH)){
				lastTurnDirection = WorldSpatial.RelativeDirection.LEFT;
				applyLeftTurn(getOrientation(),delta);
			}
			if(checkNorth(currentView)){
				// Turn right until we go back to east!
				if(!getOrientation().equals(WorldSpatial.Direction.EAST)){
					lastTurnDirection = WorldSpatial.RelativeDirection.RIGHT;
					applyRightTurn(getOrientation(),delta);
				}
				else{
					isFollowingWall = true;
				}
			}
		}
		// Once the car is already stuck to a wall, apply the following logic
		else if(isFollowingWall){
			
			// Readjusts the car if it is not aligned.
			readjust(lastTurnDirection,delta);
			
			if(isTurningRight){
				applyRightTurn(getOrientation(),delta);
			}
			else if(isTurningLeft){
				// Apply the left turn if you are not currently near a wall.
				if(!checkFollowingWall(getOrientation(),currentView)){
					applyLeftTurn(getOrientation(),delta);
				}
				else{
					isTurningLeft = false;
				}
			}
			// Try to determine whether or not the car is next to a wall.
			else if(checkFollowingWall(getOrientation(),currentView)){
				// Maintain some velocity
				if(getVelocity() < CAR_SPEED / DIVIDE_CAR_SPEED_BY_2){
					applyForwardAcceleration();
				}
				// If there is wall ahead, turn right!
				if(checkWallAhead(getOrientation(),currentView)){
					lastTurnDirection = WorldSpatial.RelativeDirection.RIGHT;
					isTurningRight = true;				
					
				}

			}
			// This indicates that I can do a left turn if I am not turning right
			else{
				lastTurnDirection = WorldSpatial.RelativeDirection.LEFT;
				isTurningLeft = true;
			}
		}
	}
	
	/* METHODS FROM AIController CLASS */
	/**
	 * Readjust the car to the orientation we are in.
	 * @param lastTurnDirection
	 * @param delta
	 */
	private void readjust(WorldSpatial.RelativeDirection lastTurnDirection, float delta) {
		if(lastTurnDirection != null){
			if(!isTurningRight && lastTurnDirection.equals(WorldSpatial.RelativeDirection.RIGHT)){
				adjustRight(getOrientation(),delta);
			}
			else if(!isTurningLeft && lastTurnDirection.equals(WorldSpatial.RelativeDirection.LEFT)){
				adjustLeft(getOrientation(),delta);
			}
		}
		
	}
	
	/**
	 * Try to orient myself to a degree that I was supposed to be at if I am
	 * misaligned.
	 */
	private void adjustLeft(WorldSpatial.Direction orientation, float delta) {
		
		switch(orientation){
		case EAST:
			if(getAngle() > WorldSpatial.EAST_DEGREE_MIN+EAST_THRESHOLD){
				turnRight(delta);
			}
			break;
		case NORTH:
			if(getAngle() > WorldSpatial.NORTH_DEGREE){
				turnRight(delta);
			}
			break;
		case SOUTH:
			if(getAngle() > WorldSpatial.SOUTH_DEGREE){
				turnRight(delta);
			}
			break;
		case WEST:
			if(getAngle() > WorldSpatial.WEST_DEGREE){
				turnRight(delta);
			}
			break;
			
		default:
			break;
		}
		
	}

	private void adjustRight(WorldSpatial.Direction orientation, float delta) {
		switch(orientation){
		case EAST:
			if(getAngle() > WorldSpatial.SOUTH_DEGREE && getAngle() < WorldSpatial.EAST_DEGREE_MAX){
				turnLeft(delta);
			}
			break;
		case NORTH:
			if(getAngle() < WorldSpatial.NORTH_DEGREE){
				turnLeft(delta);
			}
			break;
		case SOUTH:
			if(getAngle() < WorldSpatial.SOUTH_DEGREE){
				turnLeft(delta);
			}
			break;
		case WEST:
			if(getAngle() < WorldSpatial.WEST_DEGREE){
				turnLeft(delta);
			}
			break;
			
		default:
			break;
		}
		
	}
	
	/**
	 * Checks whether the car's state has changed or not, stops turning if it
	 *  already has.
	 */
	private void checkStateChange() {
		if(previousState == null){
			previousState = getOrientation();
		}
		else{
			if(previousState != getOrientation()){
				if(isTurningLeft){
					isTurningLeft = false;
				}
				if(isTurningRight){
					isTurningRight = false;
				}
				previousState = getOrientation();
			}
		}
	}
	
	/**
	 * Turn the car counter clock wise (think of a compass going counter clock-wise)
	 */
	private void applyLeftTurn(WorldSpatial.Direction orientation, float delta) {
		switch(orientation){
		case EAST:
			if(!getOrientation().equals(WorldSpatial.Direction.NORTH)){
				turnLeft(delta);
			}
			break;
		case NORTH:
			if(!getOrientation().equals(WorldSpatial.Direction.WEST)){
				turnLeft(delta);
			}
			break;
		case SOUTH:
			if(!getOrientation().equals(WorldSpatial.Direction.EAST)){
				turnLeft(delta);
			}
			break;
		case WEST:
			if(!getOrientation().equals(WorldSpatial.Direction.SOUTH)){
				turnLeft(delta);
			}
			break;
		default:
			break;
		
		}
		
	}
	
	/**
	 * Turn the car clock wise (think of a compass going clock-wise)
	 */
	private void applyRightTurn(WorldSpatial.Direction orientation, float delta) {
		switch(orientation){
		case EAST:
			if(!getOrientation().equals(WorldSpatial.Direction.SOUTH)){
				turnRight(delta);
			}
			break;
		case NORTH:
			if(!getOrientation().equals(WorldSpatial.Direction.EAST)){
				turnRight(delta);
			}
			break;
		case SOUTH:
			if(!getOrientation().equals(WorldSpatial.Direction.WEST)){
				turnRight(delta);
			}
			break;
		case WEST:
			if(!getOrientation().equals(WorldSpatial.Direction.NORTH)){
				turnRight(delta);
			}
			break;
		default:
			break;
		
		}
		
	}

	/**
	 * Check if you have a wall in front of you!
	 * @param orientation the orientation we are in based on WorldSpatial
	 * @param currentView what the car can currently see
	 * @return
	 */
	private boolean checkWallAhead(WorldSpatial.Direction orientation, HashMap<Coordinate, MapTile> currentView){
		switch(orientation){
		case EAST:
			return checkEast(currentView);
		case NORTH:
			return checkNorth(currentView);
		case SOUTH:
			return checkSouth(currentView);
		case WEST:
			return checkWest(currentView);
		default:
			return false;
		
		}
	}
	
	/**
	 * Check if the wall is on your left hand side given your orientation
	 * @param orientation
	 * @param currentView
	 * @return
	 */
	private boolean checkFollowingWall(WorldSpatial.Direction orientation, HashMap<Coordinate, MapTile> currentView) {
		
		switch(orientation){
		case EAST:
			return checkNorth(currentView);
		case NORTH:
			return checkWest(currentView);
		case SOUTH:
			return checkEast(currentView);
		case WEST:
			return checkSouth(currentView);
		default:
			return false;
		}
		
	}
	

	/**
	 * Method below just iterates through the list and check in the correct coordinates.
	 * i.e. Given your current position is 10,10
	 * checkEast will check up to wallSensitivity amount of tiles to the right.
	 * checkWest will check up to wallSensitivity amount of tiles to the left.
	 * checkNorth will check up to wallSensitivity amount of tiles to the top.
	 * checkSouth will check up to wallSensitivity amount of tiles below.
	 */
	public boolean checkEast(HashMap<Coordinate, MapTile> currentView){
		// Check tiles to my right
		Coordinate currentPosition = new Coordinate(getPosition());
		for(int i = 0; i <= wallSensitivity; i++){
			MapTile tile = currentView.get(new Coordinate(currentPosition.x+i, currentPosition.y));
			if(tile.getName().equals("Wall")){
				return true;
			}
		}
		return false;
	}
	
	public boolean checkWest(HashMap<Coordinate,MapTile> currentView){
		// Check tiles to my left
		Coordinate currentPosition = new Coordinate(getPosition());
		for(int i = 0; i <= wallSensitivity; i++){
			MapTile tile = currentView.get(new Coordinate(currentPosition.x-i, currentPosition.y));
			if(tile.getName().equals("Wall")){
				return true;
			}
		}
		return false;
	}
	
	public boolean checkNorth(HashMap<Coordinate,MapTile> currentView){
		// Check tiles to towards the top
		Coordinate currentPosition = new Coordinate(getPosition());
		for(int i = 0; i <= wallSensitivity; i++){
			MapTile tile = currentView.get(new Coordinate(currentPosition.x, currentPosition.y+i));
			if(tile.getName().equals("Wall")){
				return true;
			}
		}
		return false;
	}
	
	public boolean checkSouth(HashMap<Coordinate,MapTile> currentView){
		// Check tiles towards the bottom
		Coordinate currentPosition = new Coordinate(getPosition());
		for(int i = 0; i <= wallSensitivity; i++){
			MapTile tile = currentView.get(new Coordinate(currentPosition.x, currentPosition.y-i));
			if(tile.getName().equals("Wall")){
				return true;
			}
		}
		return false;
	}
	
	/* OWN METHODS */
	
	/**
	 * Check if trap is ahead
	 * @param currentView
	 * @param orientation
	 * @return
	 */
	private boolean isTrapAhead(HashMap<Coordinate, MapTile> currentView, WorldSpatial.Direction orientation) {
		LinkedList<MapTile> tilesAhead = getViewOfASide(RelativeDirection.FRONT,orientation,currentView);
		int sensity = 2;
		for (int i = 0; i < sensity; i++) {
			MapTile tile = tilesAhead.get(i);
			if (tile.getName().equals("Trap")) {
				return true;
			}
			// traps behind the wall do not count
			else if (tile.getName().equals("Wall")) {
				return false;
			}
		}
		return false;
	}
	
	/**
	 * Check if encountering dead end, previously called isInDeadEnd in class diagram
	 * @param orientation
	 * @param currentView
	 * @return
	 */
	private boolean isDeadEndAhead(WorldSpatial.Direction orientation, HashMap<Coordinate, MapTile> currentView) {
		switch(orientation){
		case EAST:
			if (checkEast(currentView) && checkNorth(currentView) && checkSouth(currentView)) {
				return true;
			}
			else {
				return false;
			}
		case NORTH:
			if (checkNorth(currentView) && checkEast(currentView) && checkWest(currentView)) {
				return true;
			}
			else {
				return false;
			}
		case SOUTH:
			if (checkSouth(currentView) && checkEast(currentView) && checkWest(currentView)) {
				return true;
			}
			else {
				return false;
			}
		case WEST:
			if (checkWest(currentView) && checkNorth(currentView) && checkSouth(currentView)) {
				return true;
			}
			else {
				return false;
			}
		default:
			return false;
		
		}
	}
	
	/**
	 * Calculating the damage traps deal to car, previously called scoreOfTraps
	 * @param relativeDirection
	 * @param currentView
	 * @return
	 */
	private int damageFromTraps(RelativeDirection relativeDirection, HashMap<Coordinate, MapTile> currentView,
								WorldSpatial.Direction orientation) {
		int score = 0;
		LinkedList<MapTile> tiles = getViewOfASide(relativeDirection, orientation, currentView);
		for (int i = 0; i < tiles.size(); i++) {
			MapTile tile = tiles.get(i);
			if (tile.getName().equals("Trap")) {
				score += 1;
			} else if (tile.getName().equals("Wall")) {
				score += 100;
			}
		}
		return score;
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
		int sensity = 1;
		
		LinkedList<MapTile> tilesAhead = getViewOfASide(RelativeDirection.FRONT,orientation,currentView);
		for (int i = 0; i < sensity; i++) {
			MapTile tile = tilesAhead.get(i);
			if (tile.getName().equals("Trap")) {
				return true;
			}
			// traps behind the wall do not count
			else if (tile.getName().equals("Wall")) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Checks if the car is able to turn to the right
	 * @param orientation
	 * @param currentView
	 * @return
	 */
	private boolean isOkToTurnRight(WorldSpatial.Direction orientation, HashMap<Coordinate, MapTile> currentView) {
		// TODO Not time for implementation
		return false;
	}
	
	/**
	 * Use the 3-point turn on the car
	 * @param orientation
	 * @param delta
	 * @return
	 */
	private void applyThreePointTurn(WorldSpatial.Direction orientation, float delta) {
		// TODO No time for implementation
	}

	/**
	 * Use a U-Turn on the car
	 * @param orientation
	 * @param delta
	 * @return
	 */
	private void applyUTurn(WorldSpatial.Direction orientation, HashMap<Coordinate, MapTile> currentView, float delta) {
		// Check: See if sticking to left or right side of wall
		// apply u-turn toward left or right, depending on check
		if ( checkFollowingWall(getOrientation(),currentView) ) {
			applyRightTurn(orientation, delta);
		}
		if ( !checkFollowingWall(getOrientation(),currentView) ) {
			applyLeftTurn(orientation, delta);
		}
	}
	
	/**
	 * Have the car reverse out, no need arguments since reversing
	 * @param orientation
	 * @param delta
	 * @return
	 */
	private void applyReverseOut() {
		// TODO
		if(getVelocity() < CAR_SPEED){
			applyReverseAcceleration();
		}
	}

	
	private WorldSpatial.Direction getDirectionAfterTurnRight(WorldSpatial.Direction orientation) {
		switch (orientation) {
		case WEST:
			return WorldSpatial.Direction.NORTH;
		case NORTH:
			return WorldSpatial.Direction.EAST;
		case EAST:
			return WorldSpatial.Direction.SOUTH;
		case SOUTH:
			return WorldSpatial.Direction.WEST;
		default:
			return null;
		}
	}

	private WorldSpatial.Direction getDirectionAfterTurnLeft(WorldSpatial.Direction orientation) {
		switch (orientation) {
		case WEST:
			return WorldSpatial.Direction.SOUTH;
		case NORTH:
			return WorldSpatial.Direction.WEST;
		case EAST:
			return WorldSpatial.Direction.NORTH;
		case SOUTH:
			return WorldSpatial.Direction.EAST;
		default:
			return null;
		}
	}
	
	/**
	 * get view of a side (Front, Back, Right, Left) based on the direction (east, west, south, north)
	 * 
	 * @param relativeDirection
	 * @param orientation
	 * @param currentView
	 * @return
	 */
	private LinkedList<MapTile> getViewOfASide(RelativeDirection relativeDirection,
			WorldSpatial.Direction orientation, HashMap<Coordinate, MapTile> currentView) {
		switch (orientation) {
		case WEST:
			switch (relativeDirection) {
			case FRONT:
				return getViewOfADirection(WorldSpatial.Direction.WEST, currentView);
			case BACK:
				return getViewOfADirection(WorldSpatial.Direction.EAST, currentView);
			case RIGHT:
				return getViewOfADirection(WorldSpatial.Direction.NORTH, currentView);
			case LEFT:
				return getViewOfADirection(WorldSpatial.Direction.SOUTH, currentView);
			}
		case EAST:
			switch (relativeDirection) {
			case FRONT:
				return getViewOfADirection(WorldSpatial.Direction.EAST, currentView);
			case BACK:
				return getViewOfADirection(WorldSpatial.Direction.WEST, currentView);
			case RIGHT:
				return getViewOfADirection(WorldSpatial.Direction.SOUTH, currentView);
			case LEFT:
				return getViewOfADirection(WorldSpatial.Direction.NORTH, currentView);
			}
		case SOUTH:
			switch (relativeDirection) {
			case FRONT:
				return getViewOfADirection(WorldSpatial.Direction.SOUTH, currentView);
			case BACK:
				return getViewOfADirection(WorldSpatial.Direction.NORTH, currentView);
			case RIGHT:
				return getViewOfADirection(WorldSpatial.Direction.WEST, currentView);
			case LEFT:
				return getViewOfADirection(WorldSpatial.Direction.EAST, currentView);
			}
		case NORTH:
			switch (relativeDirection) {
			case FRONT:
				return getViewOfADirection(WorldSpatial.Direction.NORTH, currentView);
			case BACK:
				return getViewOfADirection(WorldSpatial.Direction.SOUTH, currentView);
			case RIGHT:
				return getViewOfADirection(WorldSpatial.Direction.EAST, currentView);
			case LEFT:
				return getViewOfADirection(WorldSpatial.Direction.WEST, currentView);
			}


		}
		return null;
	}
	
	/**
	 * get view of a certain direction (east, west, south, north)
	 * 
	 * @param orientation
	 * @param currentView
	 * @return
	 */
	private LinkedList<MapTile> getViewOfADirection(WorldSpatial.Direction orientation,
			HashMap<Coordinate, MapTile> currentView) {
		Coordinate currentPosition = new Coordinate(getPosition());
		LinkedList<MapTile> tiles = new LinkedList<MapTile>();
		switch (orientation) {
		case EAST:
			for (int i = 1; i <= Car.VIEW_SQUARE; i++) {
				tiles.addLast(currentView.get(new Coordinate(currentPosition.x + i, currentPosition.y)));
			}
			break;
		case WEST:
			for (int i = 1; i <= Car.VIEW_SQUARE; i++) {
				tiles.addLast(currentView.get(new Coordinate(currentPosition.x - i, currentPosition.y)));
			}
			break;
		case NORTH:
			for (int i = 1; i <= Car.VIEW_SQUARE; i++) {
				tiles.addLast(currentView.get(new Coordinate(currentPosition.x, currentPosition.y + i)));
			}
			break;
		case SOUTH:
			for (int i = 1; i <= Car.VIEW_SQUARE; i++) {
				tiles.addLast(currentView.get(new Coordinate(currentPosition.x, currentPosition.y - i)));
			}
			break;
		}
		return tiles;
	}
	
	/** 
	 * Obtain road with where the car is at
	 * @param orientation
	 * @param currentview
	 */ 
	private int getRoadWidth(WorldSpatial.Direction orientation, HashMap<Coordinate, MapTile> currentView) {
		int roadWidth = 0;
		LinkedList<MapTile> tilesLeft = getViewOfASide(RelativeDirection.LEFT, orientation, currentView);
		LinkedList<MapTile> tilesRight = getViewOfASide(RelativeDirection.RIGHT, orientation, currentView);
		int i, j;
		for (i = 0; i < tilesLeft.size(); i++) {
			MapTile tile = tilesLeft.get(i);
			if (tile.getName().equals("Wall")) {
				break;
			}
		}
		for (j = 0; j < tilesRight.size(); j++) {
			MapTile tile = tilesRight.get(j);
			if (tile.getName().equals("Wall")) {
				break;
			}
		}
		roadWidth = i + j;
		return roadWidth;
	}
	
	/** 
	 * Defined in order to use it as keys in a hashmap
	 * @param other
	 * @param orientation
	 * @param speed
	 * @param currentPos
	 */ 
	  private boolean shouldBrake(String currentPosition, String bestPosition, float v,
		      WorldSpatial.Direction orientation) {
		 
	    int X_POS = 0;
	    int Y_POS = 1;
 
		    // this is the distance for braking	 
	    float a = -2F;
	    float t = v / (-a);
	 
	    // s = vt + 1/2 * at^2
	 
	    float distance = v * t + (0.5F) * a * t * t;
	    String[] splitCoordinate = currentPosition.split(","); 
	    float currentPosition_x_float = Float.parseFloat((splitCoordinate[X_POS])); 
	    float currentPosition_y_float = Float.parseFloat((splitCoordinate[Y_POS]));

	 
	    splitCoordinate = bestPosition.split(",");
	    float bestPosition_x_float = Float.parseFloat((splitCoordinate[X_POS]));
	    float bestPosition_y_float = Float.parseFloat((splitCoordinate[Y_POS]));

	    switch(orientation) {
		 
	    case EAST: 
	    case WEST: 
	    	if (Math.abs(currentPosition_x_float - bestPosition_x_float) <= distance) {     	 
	            return true;
	          }  
	          break;
	    case NORTH:
	    case SOUTH:
	      if (Math.abs(currentPosition_y_float - bestPosition_y_float) <= distance) {
	        return true;
	      } 
	      break;
	    }
	 
	    return false; 
	  } 
}
