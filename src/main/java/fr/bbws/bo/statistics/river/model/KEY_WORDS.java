package fr.bbws.bo.statistics.river.model;

public enum KEY_WORDS {

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
	
	SLUGGING_ZERO (20),
	SLUGGING_1B (21),
	SLUGGING_2B (22),
	SLUGGING_3B (23),
	SLUGGING_4B (24),
	SLUGGING_GDP (25),
	
	K_SWINGING (30),
	K_LOOKING (31),
	WALK (32),
	SACRIFICE_HIT (33),
	HIT_BY_PITCH (34),
	OBR (35),
	
	STOLEN_BASE (40),
	SCORE (41),
	RBI (42),
	RUNNER_ADVANCE (50),
	RUNNER_OUT_AT_FIRST (51),
	RUNNER_OUT_AT_SECOND (52),
	RUNNER_OUT_AT_THIRD (53),
	RUNNER_OUT_AT_HOME (54),
	RUNNER_PICKED_OFF (55),
	RUNNER_CAUGHT_STEALING_SECOND (56),
	RUNNER_CAUGHT_STEALING_THIRD (57),
	RUNNER_CAUGHT_STEALING_HOME (58),
	RUNNER_OUT_ON_THE_PLAY (59),
	
	UNKNOWN (99),
	MAX (100);
	
	private int key = 0;
	
	KEY_WORDS(int key) {
		this.key = key;
	}
	
	public int intValue() {
		return this.key;
	}
}
