/**
 * By Michael Lee (563550)
 * 
 * Package largely adapted from AIController in controller package
 * but with added attributes and methods to implement a better behaviour model
 */

package mycontroller;

import java.util.HashMap;
import java.util.LinkedList;

import controller.CarController;
import tiles.MapTile;
import utilities.Coordinate;
import world.Car;
import world.WorldSpatial;
import world.WorldSpatial.Direction;

public class MyAIController extends CarController {

	// How many minimum units the wall is away from the player.
	private int wallSensitivity = 2;

	private boolean isFollowingWall = false; // This is initialized when the car
												// sticks to a wall.
	private RelativeDirection lastTurnDirection = null; // Shows
														// the
														// last
														// turn
														// direction
														// the
														// car
														// takes.
	private boolean isTurningLeft = false;
	private boolean isTurningRight = false;
	private WorldSpatial.Direction previousState = null; // Keeps track of the
															// previous state

	// New Attributes
	private boolean isThreePointTurning = false;
	private boolean isUTurning = false;
	private boolean isReversingOut = false;

	// used to check whether the right turn is finished
	private WorldSpatial.Direction directionAfterTurnRight;
	private WorldSpatial.Direction directionAfterTurnLeft;

	// a state used to avoid traps
	/**
	 * IDLE detecting the trap TURNING1 when trap ahead and the road is enough
	 * to turn && no trap at right side, then turn right SERACHING, after
	 * turning right, try to find a best place to turn left TURING2 turn left
	 * BACK go back to best place STOP stop at best place PASSING use the
	 * fastest speed pass the trap END after passing the trap, we need to slow
	 * down
	 *
	 */
	public enum State {
		IDLE, TURNING1, SEARCHING, TURNING2, BACK, STOP, PASSING, END
	}

	/**
	 * we understand this should be inside world, but we cannot change other
	 * code except MyAIController so we put it here right now
	 * 
	 */
	public static enum RelativeDirection {
		FRONT, LEFT, RIGHT, BACK
	};

	private State state = State.IDLE;
	private String bestPosition_string;
	private int bestScore = 100;

	// Car Speed to move at
	private final float CAR_SPEED = 3;

	// Offset used to differentiate between 0 and 360 degrees
	private int EAST_THRESHOLD = 3;

	public MyAIController(Car car) {
		super(car);
	}

	@Override
	public void update(float delta) {

		// Gets what the car can see
		HashMap<Coordinate, MapTile> currentView = getView();
		checkStateChange();

		if (state == State.IDLE && isTrapAhead(currentView, getOrientation())) {

			int roadWidth = getRoadWidth(getOrientation(), currentView);
			LinkedList<MapTile> tilesRight = getViewOfASide(RelativeDirection.RIGHT, getOrientation(), currentView);
			// the road is too narrow to turn or the tile on the right side is
			// trap
			// just go through
			if (roadWidth == 1 || tilesRight.get(0).getName().equals("Trap")) {
				setState(State.PASSING);
				return;
			}

			directionAfterTurnRight = getDirectionAfterTurnRight(getOrientation());
			setState(State.TURNING1);
			bestPosition_string = getPosition();

			bestScore = scoreOfTrapsOnOneSide(RelativeDirection.LEFT, currentView, getOrientation());
			return;
		}
		if (state == State.TURNING1) {
			if (getVelocity() < (CAR_SPEED / 3)) {
				applyForwardAcceleration();
			}
			if (getOrientation() != directionAfterTurnRight) {
				lastTurnDirection = RelativeDirection.RIGHT;
				applyRightTurn(getOrientation(), delta);
			} else {
				setState(State.SEARCHING);
			}
			return;
		}
		if (state == State.SEARCHING) {

			if (isTrapOrWallAhead(getOrientation(), currentView)) {
				setState(State.BACK);
				return;
			}
			// accelerate
			if (getVelocity() < (CAR_SPEED / 3)) {
				applyForwardAcceleration();
			}
			int score = scoreOfTrapsOnOneSide(RelativeDirection.LEFT, currentView, getOrientation());
			// no traps
			if (score == 0) {
				setState(State.TURNING2);
				directionAfterTurnLeft = getDirectionAfterTurnLeft(getOrientation());
				return;
			}
			Coordinate currentPosition = new Coordinate(getPosition());
			Coordinate bestPosition   = new Coordinate(bestPosition_string);
			if (currentPosition.equals(bestPosition)) {
				bestPosition_string = getPosition();
			}
			// better place with less traps
			if (score < bestScore) {

				bestScore = score;
				bestPosition_string = getPosition();
			}
			return;
		}

		// go back to the best place
		if (state == State.BACK) {

			if (shouldBrake(getPosition(), bestPosition_string, getVelocity(), getOrientation())) {
				setState(State.STOP);

			} else {
				// Backward accelerate
				if (getVelocity() < (CAR_SPEED)) {
					applyReverseAcceleration();
				}
				return;
			}

		}

		if (state == State.STOP) {
			if (getVelocity() != 0) {
				applyBrake();
				return;
			} else {
				Coordinate currentPosition = new Coordinate(getPosition());
				setState(State.TURNING2);
				directionAfterTurnLeft = getDirectionAfterTurnLeft(getOrientation());
				return;
			}
		}

		if (state == State.TURNING2) {

			if (getOrientation() != directionAfterTurnLeft) {
				if (getVelocity() < (CAR_SPEED / 2)) {
					applyForwardAcceleration();
				}

				lastTurnDirection = RelativeDirection.LEFT;
				applyLeftTurn(getOrientation(), delta);
				return;
			} else {
				setState(State.PASSING);
			}
			return;
		}

		// use the fastest speed to overpass it
		if (state == State.PASSING) {
			if (getVelocity() < (CAR_SPEED) / 1.3) {
				applyForwardAcceleration();
				return;
			} else {
				setState(State.END);
				return;
			}
		}

		// cause the speed is very fast in order to pass the traps
		// after we overpassing the trap, we need to slow down
		if (state == State.END) {
			if (getVelocity() > (CAR_SPEED) / 2) {
				applyBrake();
				return;
			} else {
				setState(State.IDLE);
				return;
			}
		}

		// If you are not following a wall initially, find a wall to stick to!
		if (!isFollowingWall) {
			// the car_speed is intentionally dropped down to avoid run into the
			// trap
			if (getVelocity() < CAR_SPEED / 2) {
				applyForwardAcceleration();
			}
			// Turn towards the north
			if (!getOrientation().equals(WorldSpatial.Direction.NORTH)) {
				lastTurnDirection = RelativeDirection.LEFT;
				applyLeftTurn(getOrientation(), delta);
			}
			if (checkNorth(currentView)) {
				// Turn right until we go back to east!
				if (!getOrientation().equals(WorldSpatial.Direction.EAST)) {
					lastTurnDirection = RelativeDirection.RIGHT;
					applyRightTurn(getOrientation(), delta);
				} else {
					isFollowingWall = true;
				}
			}
		}
		// Once the car is already stuck to a wall, apply the following logic
		else {

			// Readjust the car if it is misaligned.
			readjust(lastTurnDirection, delta);

			if (isTurningRight) {
				applyRightTurn(getOrientation(), delta);
			} else if (isTurningLeft) {
				// Apply the left turn if you are not currently near a wall.
				if (!checkFollowingWall(getOrientation(), currentView)) {
					applyLeftTurn(getOrientation(), delta);
				} else {
					isTurningLeft = false;
				}
			}
			// Try to determine whether or not the car is next to a wall.
			else if (checkFollowingWall(getOrientation(), currentView)) {
				// Maintain some velocity
				// the car_speed is intentionally dropped down to avoid run into
				// the trap
				if (getVelocity() < CAR_SPEED / 2) {
					applyForwardAcceleration();
				}
				// If there is wall ahead, turn right!
				if (checkWallAhead(getOrientation(), currentView)) {
					lastTurnDirection = RelativeDirection.RIGHT;
					isTurningRight = true;

				}

			}
			// This indicates that I can do a left turn if I am not turning
			// right
			else {
				lastTurnDirection = RelativeDirection.LEFT;
				isTurningLeft = true;
			}
		}

	}

	/**
	 * Readjust the car to the orientation we are in.
	 * 
	 * @param lastTurnDirection
	 * @param delta
	 */
	private void readjust(RelativeDirection lastTurnDirection, float delta) {
		if (lastTurnDirection != null) {
			if (!isTurningRight && lastTurnDirection.equals(RelativeDirection.RIGHT)) {
				adjustRight(getOrientation(), delta);
			} else if (!isTurningLeft && lastTurnDirection.equals(RelativeDirection.LEFT)) {
				adjustLeft(getOrientation(), delta);
			}
		}

	}

	/**
	 * Try to orient myself to a degree that I was supposed to be at if I am
	 * misaligned.
	 */
	private void adjustLeft(WorldSpatial.Direction orientation, float delta) {

		switch (orientation) {
		case EAST:
			if (getAngle() > WorldSpatial.EAST_DEGREE_MIN + EAST_THRESHOLD) {
				turnRight(delta);
			}
			break;
		case NORTH:
			if (getAngle() > WorldSpatial.NORTH_DEGREE) {
				turnRight(delta);
			}
			break;
		case SOUTH:
			if (getAngle() > WorldSpatial.SOUTH_DEGREE) {
				turnRight(delta);
			}
			break;
		case WEST:
			if (getAngle() > WorldSpatial.WEST_DEGREE) {
				turnRight(delta);
			}
			break;

		default:
			break;
		}

	}

	private void adjustRight(WorldSpatial.Direction orientation, float delta) {
		switch (orientation) {
		case EAST:
			if (getAngle() > WorldSpatial.SOUTH_DEGREE && getAngle() < WorldSpatial.EAST_DEGREE_MAX) {
				turnLeft(delta);
			}
			break;
		case NORTH:
			if (getAngle() < WorldSpatial.NORTH_DEGREE) {
				turnLeft(delta);
			}
			break;
		case SOUTH:
			if (getAngle() < WorldSpatial.SOUTH_DEGREE) {
				turnLeft(delta);
			}
			break;
		case WEST:
			if (getAngle() < WorldSpatial.WEST_DEGREE) {
				turnLeft(delta);
			}
			break;

		default:
			break;
		}

	}

	/**
	 * Checks whether the car's state has changed or not, stops turning if it
	 * already has.
	 */
	private void checkStateChange() {
		if (previousState == null) {
			previousState = getOrientation();
		} else {
			if (previousState != getOrientation()) {
				if (isTurningLeft) {
					isTurningLeft = false;
				}
				if (isTurningRight) {
					isTurningRight = false;
				}
				previousState = getOrientation();
			}
		}
	}

	/**
	 * Turn the car counter clock wise (think of a compass going counter
	 * clock-wise)
	 */
	private void applyLeftTurn(WorldSpatial.Direction orientation, float delta) {
		switch (orientation) {
		case EAST:
			if (!getOrientation().equals(WorldSpatial.Direction.NORTH)) {
				turnLeft(delta);
			}
			break;
		case NORTH:
			if (!getOrientation().equals(WorldSpatial.Direction.WEST)) {
				turnLeft(delta);
			}
			break;
		case SOUTH:
			if (!getOrientation().equals(WorldSpatial.Direction.EAST)) {
				turnLeft(delta);
			}
			break;
		case WEST:
			if (!getOrientation().equals(WorldSpatial.Direction.SOUTH)) {
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
		switch (orientation) {
		case EAST:
			if (!getOrientation().equals(WorldSpatial.Direction.SOUTH)) {
				turnRight(delta);
			}
			break;
		case NORTH:
			if (!getOrientation().equals(WorldSpatial.Direction.EAST)) {
				turnRight(delta);
			}
			break;
		case SOUTH:
			if (!getOrientation().equals(WorldSpatial.Direction.WEST)) {
				turnRight(delta);
			}
			break;
		case WEST:
			if (!getOrientation().equals(WorldSpatial.Direction.NORTH)) {
				turnRight(delta);
			}
			break;
		default:
			break;

		}

	}

	/**
	 * Check if you have a wall in front of you!
	 * 
	 * @param orientation
	 *            the orientation we are in based on WorldSpatial
	 * @param currentView
	 *            what the car can currently see
	 * @return
	 */
	private boolean checkWallAhead(WorldSpatial.Direction orientation, HashMap<Coordinate, MapTile> currentView) {
		switch (orientation) {
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
	 * 
	 * @param orientation
	 * @param currentView
	 * @return
	 */
	private boolean checkFollowingWall(WorldSpatial.Direction orientation, HashMap<Coordinate, MapTile> currentView) {

		switch (orientation) {
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
	 * Method below just iterates through the list and check in the correct
	 * coordinates. i.e. Given your current position is 10,10 checkEast will
	 * check up to wallSensitivity amount of tiles to the right. checkWest will
	 * check up to wallSensitivity amount of tiles to the left. checkNorth will
	 * check up to wallSensitivity amount of tiles to the top. checkSouth will
	 * check up to wallSensitivity amount of tiles below.
	 */
	public boolean checkEast(HashMap<Coordinate, MapTile> currentView) {
		// Check tiles to my right
		Coordinate currentPosition = new Coordinate(getPosition());
		for (int i = 0; i <= wallSensitivity; i++) {
			MapTile tile = currentView.get(new Coordinate(currentPosition.x + i, currentPosition.y));
			if (tile.getName().equals("Wall")) {
				return true;
			}
		}
		return false;
	}

	public boolean checkWest(HashMap<Coordinate, MapTile> currentView) {
		// Check tiles to my left
		Coordinate currentPosition = new Coordinate(getPosition());
		for (int i = 0; i <= wallSensitivity; i++) {
			MapTile tile = currentView.get(new Coordinate(currentPosition.x - i, currentPosition.y));
			if (tile.getName().equals("Wall")) {
				return true;
			}
		}
		return false;
	}

	public boolean checkNorth(HashMap<Coordinate, MapTile> currentView) {
		// Check tiles to towards the top
		Coordinate currentPosition = new Coordinate(getPosition());
		for (int i = 0; i <= wallSensitivity; i++) {
			MapTile tile = currentView.get(new Coordinate(currentPosition.x, currentPosition.y + i));
			if (tile.getName().equals("Wall")) {
				return true;
			}
		}
		return false;
	}

	public boolean checkSouth(HashMap<Coordinate, MapTile> currentView) {
		// Check tiles towards the bottom
		Coordinate currentPosition = new Coordinate(getPosition());
		for (int i = 0; i <= wallSensitivity; i++) {
			MapTile tile = currentView.get(new Coordinate(currentPosition.x, currentPosition.y - i));
			if (tile.getName().equals("Wall")) {
				return true;
			}
		}
		return false;
	}

	/* OWN METHODS */

	private void setState(State stateToBe) {
		this.state = stateToBe;
	}

	/**
	 * Check if trap is ahead
	 * 
	 * @param currentView
	 * @return
	 */
	private boolean isTrapAhead(HashMap<Coordinate, MapTile> currentView, WorldSpatial.Direction orientation) {
		LinkedList<MapTile> tilesAhead = getViewOfASide(RelativeDirection.FRONT, orientation, currentView);
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
	 * Check if encountering dead end
	 * 
	 * @param currentView
	 * @return
	 */
	private boolean isInDeadEnd(HashMap<Coordinate, MapTile> currentView) {
		// TODO
		return false;
	}

	/**
	 * Calculating the score of the traps
	 * 
	 * @param currentView
	 * @return
	 */
	private int scoreOfTrapsOnOneSide(RelativeDirection relativeDirection, HashMap<Coordinate, MapTile> currentView,
			Direction orientation) {
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
	 * 
	 * @param position
	 * @return
	 */
	private void setBestPosition(Coordinate position) {
		// TODO
	}

	/**
	 * Move the car to the best position
	 * 
	 * @param position
	 * @return
	 */
	private void moveToBestPosition(Coordinate position) {
		// TODO
	}

	/**
	 * Checks if the car can proceed, basically seeing if there is a trap or
	 * wall up ahead Previously named carGo
	 * 
	 * @param orientation
	 * @param currentView
	 * @return
	 */
	private boolean isTrapOrWallAhead(WorldSpatial.Direction orientation, HashMap<Coordinate, MapTile> currentView) {
		int sensity = 1;

		LinkedList<MapTile> tilesAhead = getViewOfASide(RelativeDirection.FRONT, orientation, currentView);
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
	 * 
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
	 * 
	 * @param orientation
	 * @param delta
	 * @return
	 */
	private void applyThreePointTurn(WorldSpatial.Direction orientation, float delta) {
		// TODO
	}

	/**
	 * Use a U-Turn on the car
	 * 
	 * @param orientation
	 * @param delta
	 * @return
	 */
	private void applyUTurn(WorldSpatial.Direction orientation, float delta) {
		// TODO
	}

	/**
	 * Have the car reverse out
	 * 
	 * @param orientation
	 * @param delta
	 * @return
	 */
	private void applyReverseOut(WorldSpatial.Direction orientation, float delta) {
		// TODO
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

	private LinkedList<MapTile> getViewOfASide(RelativeDirection relativeDirection, WorldSpatial.Direction orientation,
			HashMap<Coordinate, MapTile> currentView) {
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
	 * get view of a certain direction(east, west, south, north)
	 * 
	 * @param relativeDirection
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
		roadWidth = i + j - 1;
		return roadWidth;
	}

	public State getState() {
		return this.state;
	}

	private boolean shouldBrake(String currentPosition, String bestPosition, float v,
			WorldSpatial.Direction orientation) {
		int X_POS = 0;
		int Y_POS = 1;
		// this is the distance for braking
		float a = -2F;
		float t = v / (-a);
		// s = vt + 1/2 * at^2
		float distance = v * t + (0.5F) * a * t * t;
		System.out.println(distance);
		String[] splitCoordinate = currentPosition.split(",");
		float currentPosition_x_float = Float.parseFloat((splitCoordinate[X_POS]));
		float currentPosition_y_float = Float.parseFloat((splitCoordinate[Y_POS]));

		splitCoordinate = bestPosition.split(",");
		float bestPosition_x_float = Float.parseFloat((splitCoordinate[X_POS]));
		float bestPosition_y_float = Float.parseFloat((splitCoordinate[Y_POS]));

		switch (orientation) {
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
