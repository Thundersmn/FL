package utilities;

import java.util.Objects;

import world.WorldSpatial;

public class Coordinate {
	private int x;
	private int y;
	private float x_float;
	private float y_float;
	
	private static final int X_POS = 0;
	private static final int Y_POS = 1;

	/**
	 * Constructs a coordinate object
	 * @param coordinate: "1,0" is an example of a coordinate string that will
	 * be deconstructed into a coordinate object.
	 */
	public Coordinate(String coordinate){
		// Split up coordinate
		try{
			String[] splitCoordinate = coordinate.split(",");
			this.x_float = Float.parseFloat((splitCoordinate[X_POS]));
			this.y_float = Float.parseFloat((splitCoordinate[Y_POS]));
			this.x = Math.round(this.x_float);
			this.y = Math.round(this.y_float);
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
	}
	/* Getters for x and y */
	public int getX() {
		return x;
	}
	public int getY() {
		return y;
	}
	
	
	
	
	public void setX(int x) {
		this.x = x;
	}
	public void setY(int y) {
		this.y = y;
	}
	public Coordinate(int x, int y){
		this.x = x;
		this.y = y;
	}
	
	public String toString(){
		return x+","+y + " float: " + x_float + "," + y_float;
	}
	
	
	
	
	/**
	 * Defined in order to use it as keys in a hashmap
	 */
	public boolean equals(Object c){
		if( c == this){
			return true;
		}
		if(!(c instanceof Coordinate)){
			return false;
		}
		
		Coordinate coordinate = (Coordinate) c;
		
		return (coordinate.x == this.x) && (coordinate.y == this.y);
	}
	
	/**
	 * Defined in order to use it as keys in a hashmap
	 */
	public  boolean shouldBrake(Coordinate other, WorldSpatial.Direction orientation, float speed){
		// this is the distance for break
		float a = 2F;
		float t = speed / a;
		float distance;
		distance = speed * t - t*t;
		switch(orientation){
		case EAST:
		case WEST:
			if(Math.abs(other.x_float- this.x_float) <= distance){
				return true;
			}
			break;
		case NORTH:
		case SOUTH:
			if(Math.abs(other.y_float - this.y_float) <= distance){
				return true;
			}
			break;
		}
		return false;
	}
	
	public int hashCode(){
		return Objects.hash(x,y);
	}
}
