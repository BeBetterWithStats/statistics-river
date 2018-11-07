package fr.bbws.bo.statistics.river.mapper;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.bbws.bo.statistics.river.model.GameSheetConfiguration;
import fr.bbws.bo.statistics.river.model.Play;
import fr.bbws.bo.statistics.river.model.Player;
import fr.bbws.bo.statistics.utils.SearchInFileUtils;

public class ConsoleMapper {

	/*
	static Map<String, KEY_WORDS> plate_appearance_words = new HashMap<String, KEY_WORDS>();

	static Map<String, KEY_WORDS> hit_key_words = new HashMap<String, KEY_WORDS>();
	
	static Map<String, KEY_WORDS> slugging_key_words = new HashMap<String, KEY_WORDS>();
	
	static List<String> noplay_key_words = new ArrayList<String>();
	*/
	
	final static Logger logger = LogManager.getLogger(ConsoleMapper.class.getName());
	
	public static void generateDocuments(Path file, Player player, String field, String oppositeTeam, String umpire, Date date) {

//		initialise();
		
// ############## PARCOURIR LE FICHIER
// ############## ET LE STOCKER EN MEMOIRE
		
		StringBuffer buffer = new StringBuffer();
		
		try {

			List<String> lignestmp = Files.readAllLines(file, Charset.forName("ISO-8859-1"));
					
					//
					// ON NE VA EXTRAIRE QUE LES LIGNES
					// QUI NOUS INTERESSENT
					// A SAVOIR CELLE DU PLAY BY PLAY
					//
					boolean b = false;
					for (String ligne : lignestmp) { // pour chaque ligne du fichier
						
						if (ligne.startsWith("<font size=2><b>")) {
							
							buffer.append(ligne);
							b = true;
							
						} else if (ligne.startsWith("</font>")) {
							
							b = false;
							
						} else {
							if (b) {
								buffer.append(" ").append(ligne);
							}
						}
					}
					// FIN - EXTRACTION
					
		} catch (IOException e) {
			e.printStackTrace();
		}
// ############## FIN -- PARCOURIR LE FICHIER
// ############## FIN -- ET LE STOCKER EN MEMOIRE


		

// ############## ON DECOUPE LE FICHIER HTML EN INNING

		String[] html = buffer.toString().split("<font size=2><b>");
		
		List<String> innings = new ArrayList<String>();
		for (String div : html) {
			innings.add( div);
		}
		
		innings.remove(0);
		
		
		
// ############## ON DECOUPE CHAQUE INNING EN ACTION
		
		boolean print_error = true;
		Map<String, Object> json = new TreeMap<String, Object>();
		Map<String, Play> play_key_words = GameSheetConfiguration.getInstance().loadAllPlays();
		
		for (String inning : innings) {
			
			List<String> plays = SearchInFileUtils.searchAfterWithAfterWordAndSpaceIncluded(inning, player.getID());
			
			if (null != plays) {
				
				for (String play : plays) {
					
					json.clear(); // reinitilisation du json
					print_error = true;
					
					for (String key : play_key_words.keySet()) {
						
						if (play.startsWith(player.getID() + key)) { // si un des mot clé apparait

							print_error = false;
							
							//
							// ############## CONSTRUCTION DU DOCUMENT JSON
							// ############## POUR CHAQUE ACTION DU JOUEUR
							
							json.put("day", date.toString());
							json.put("field", field);
							json.put("opposite-team", oppositeTeam);
							json.put("umpire-id", umpire);
							json.put("player-id", player.getID());
							json.put("player-team", player.getTeam());
							json.put("player-field-position", player.getFieldPosition());
							json.put("player-batting-order", player.getBattingOrder());
							json.put("play-when", SearchInFileUtils.searchBetween(inning, player.getTeam(), "- </b>"));
							json.put("play-what", play_key_words.get(key));
							json.put("play-where", "TODO");  // TODO where
							json.put("play-against", "TODO"); // TODO opposite pitcher
							
							logger.debug("[JSON] = {}", json);
							break;							
						}
					}
					
					if (print_error) {
						
						logger.error("[MISSING PLAY] = {} in file = {}]", play, file);
						print_error = false;
					}
				}
			}
		}
		// FIN - DECOUPAGE DE CHAQUE INNING EN ACTION
	}
	

	public static void generateDocuments(Path p_file, List<Player> p_players, String p_field, String p_oppositeTeam, String p_umpire, Date p_date) {

// ############## PARCOURIR LE FICHIER
// ############## ET LE STOCKER EN MEMOIRE
		
		StringBuffer buffer = new StringBuffer();
		
		try {

			List<String> lignestmp = Files.readAllLines(p_file, Charset.forName("ISO-8859-1"));
					
					//
					// ON NE VA EXTRAIRE QUE LES LIGNES
					// QUI NOUS INTERESSENT
					// A SAVOIR CELLE DU PLAY BY PLAY
					//
					boolean b = false;
					for (String _ligne : lignestmp) { // pour chaque ligne du fichier
						
						if (_ligne.startsWith("<font size=2><b>")) {
							
							buffer.append(_ligne);
							b = true;
							
						} else if (_ligne.startsWith("</font>")) {
							
							b = false;
							
						} else {
							if (b) {
								buffer.append(" ").append(_ligne);
							}
						}
					}
					// FIN - EXTRACTION
					
		} catch (IOException e) {
			e.printStackTrace();
		}
// ############## FIN -- PARCOURIR LE FICHIER
// ############## FIN -- ET LE STOCKER EN MEMOIRE


		
		
		
// ############## DECOUPER LE FICHIER HTML EN INNING

		String[] html = buffer.toString().split("<font size=2><b>");
		
		List<String> innings = new ArrayList<String>();
		for (String _div : html) {
			innings.add( _div);
		}
		
		innings.remove(0);
		
// ############## FIN -- DECOUPER LE FICHIER HTML EN INNING
		
		
		
		
		
		final Map<String, Play> ALL_PLAYS = GameSheetConfiguration.getInstance().loadAllPlays();
		Map<String, Object> _json = new TreeMap<String, Object>();
		List<String> _plays = new ArrayList<String>();
		List<String> __plays = new ArrayList<String>();
		String _when = null;
		Player _who = null;
		Play _what = null;
		String _playerID = null;
		
		
// ############## DECOUPER CHAQUE INNING EN ACTION

		for (String _inning : innings) {
			
			_inning = _inning.replaceAll("</font>", "</font>#").replaceAll("\\. ", "\\.#");
			_when = SearchInFileUtils.searchBetween(_inning, p_players.get(0).getTeam(), "- </b>");
			logger.debug("[_WHEN] = {}", _when);
			
			if (null != _when) {
				// si l'inning correspond à l'equipe passee en parametre
				// on decoupe alors l'inning en action
				
				_plays.clear();
				Collections.addAll(_plays, _inning.split("#"));
				
				// on supprime deux actions qui sont
				// "<i><b>x runs, x hit, x error, x LOB.</b></i>"
				// et "xxxx - </b></font>"
				_plays.remove(_plays.size() - 1);
				_plays.remove(0);
				
				
				for (String _play : _plays) {
					
					logger.debug("  [_PLAY] = {}", _play);
					
					__plays.clear();
					Collections.addAll(__plays, _play.split("; "));
					
					for (String __play : __plays) {
					
						logger.debug("    [__PLAY] = {}", __play.replaceAll(", SAC", "")
																	.replaceAll(", SF", "")
																	.replaceAll(", 4 RBI", "")
																	.replaceAll(", 3 RBI", "")
																	.replaceAll(", 2 RBI", "")
																	.replaceAll(", RBI", ""));
						
						_playerID = SearchInFileUtils.searchUppercaseWordFromBeginning(__play);
						logger.debug("        [_PLAYERID] = {}", _playerID);
						logger.debug("        [__KEY_WORD] = {}", __play.replaceAll(", SAC", "")
																		.replaceAll(", SF", "")
																		.replaceAll(", 4 RBI", "")
																		.replaceAll(", 3 RBI", "")
																		.replaceAll(", 2 RBI", "")
																		.replaceAll(", RBI", "")
																		.substring( _playerID != null ? _playerID.length() + 1 : 0));

						// MATCH WITH ONE OF THE 
						// fr.bbws.bo.statistics.river.model.GameSheetConfiguration.getInstance().loadAllPlays() KEYWORDS
						_what = Play.UNKNOWN;
						for (String k : ALL_PLAYS.keySet()) {
							if (__play.replaceAll(", SAC", "")
										.replaceAll(", SF", "")
										.replaceAll(", 4 RBI", "")
										.replaceAll(", 3 RBI", "")
										.replaceAll(", 2 RBI", "")
										.replaceAll(", RBI", "")
										.substring( _playerID != null ? _playerID.length() + 1 : 0)
											.startsWith(k)) { 
								_what = ALL_PLAYS.get(k);
							}
						}
						
						
						// MATCH WITH ONE OF THE 
						// PLAYERS PUT IN PARAMS
						_who = null;
						for (int i = 0; i < p_players.size(); i++) {
							if ( _playerID.contentEquals( p_players.get(i).getID())) {
								_who = p_players.get(i);
								break;
							}
						}
						
						// CONSTRUCTION DU DOCUMENT JSON
						// POUR CHAQUE ACTION
						_json.clear();
						
						if ( _what != Play.UNKNOWN && _what != Play.NO_PLAY) {
							
							_json.put("creation_time", LocalDateTime.now().toString());
							_json.put("day", p_date.toString());
							_json.put("field", p_field);
							_json.put("opposite-pitcher", "undefined".toUpperCase()); // TODO opposite pitcher
							_json.put("opposite-team", p_oppositeTeam);
							_json.put("player-id", _who != null ? _who.getID() : _playerID);
							_json.put("player-team", _who != null ? _who.getTeam() : "undefined".toUpperCase());
							_json.put("player-field-position", _who != null ? _who.getFieldPosition() : "undefined".toUpperCase());
							_json.put("player-batting-order",  _who != null ? _who.getBattingOrder() : "undefined".toUpperCase());
							_json.put("play-when", _when);
							_json.put("play-what", _what);
							_json.put("play-where", "undefined".toUpperCase());  // TODO where
							_json.put("umpire-id", p_umpire);
							
							logger.info("    [_JSON] = {}", _json);
						}
					}
				} 
			}
		}
	}
}
