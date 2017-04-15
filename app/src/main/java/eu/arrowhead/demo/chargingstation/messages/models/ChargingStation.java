package eu.arrowhead.demo.chargingstation.messages.models;


public class ChargingStation {

	private double latitude;
	private double longitude;
	private int maxSlots;
	private int freeSlots;
	
	public ChargingStation() {
	}
	
	public ChargingStation(double latitude, double longitude, int maxSlots, int freeSlots) {
		super();
		this.latitude = latitude;
		this.longitude = longitude;
		this.maxSlots = maxSlots;
		this.freeSlots = freeSlots;
	}

	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public int getMaxSlots() {
		return maxSlots;
	}

	public void setMaxSlots(int maxSlots) {
		this.maxSlots = maxSlots;
	}

	public int getFreeSlots() {
		return freeSlots;
	}

	public void setFreeSlots(int freeSlots) {
		this.freeSlots = freeSlots;
	}
	
	
}
