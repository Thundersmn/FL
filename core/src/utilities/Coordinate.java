package utilities;

import java.util.Objects;

import world.WorldSpatial;

public class Coordinate {
	private int x;
	private int y;
	
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
			this.x = Integer.parseInt(splitCoordinate[X_POS]);
			this.y = Integer.parseInt(splitCoordinate[Y_POS]);
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
		return x+","+y;
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
	public  boolean closeTo(Coordinate other, WorldSpatial.Direction orientation){
		System.out.println(orientation);
		switch(orientation){
		case EAST:
		case WEST:
			if(Math.abs(other.x - this.x) < 1){
				return true;
			}
			break;
		case NORTH:
		case SOUTH:
			if(Math.abs(other.y - this.y) < 1){
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
