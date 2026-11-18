package me.andre111.dvz.disguise;

public enum SupportedDisguises {
	NOONE("Noone"),
	LIBSDISGUISES("Lib's Disguises");
	
	private String name;
	private SupportedDisguises(String n) {
		name = n;
	}
	
	public String getName() {
		return name;
	}
}
