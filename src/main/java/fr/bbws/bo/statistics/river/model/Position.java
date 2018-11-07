package fr.bbws.bo.statistics.river.model;

public enum Position {

	DESIGNATED_HITTER(0),
	PITCHER (1),
	CATCHER (2),
	FIRST_BASE (3),
	SECOND_BASE (4),
	THIRD_BASE (5),
	SHORTSTOP (6),
	LEFT_FIELD (7),
	CENTER_FIELD (8),
	RIGHT_FIELD (9),
	UNKNOW_LOCATION_OF_BATTED_BALL (10),
	
	UNKNOWN (-99);

	private int key = 0;
	
	Position(int p_key) {
		this.key = p_key;
	}
	
	public int intValue() {
		return this.key;
	}
}
