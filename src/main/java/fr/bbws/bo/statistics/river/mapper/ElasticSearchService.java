package fr.bbws.bo.statistics.river.mapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.common.xcontent.XContentType;

import fr.bbws.bo.statistics.river.model.GameSheetConfiguration;
import fr.bbws.bo.statistics.river.model.Play;
import fr.bbws.bo.statistics.river.model.Player;
import fr.bbws.bo.statistics.river.model.Position;
import fr.bbws.bo.statistics.utils.SearchInFileUtils;

public class ElasticSearchService {


	final static Logger logger = LogManager.getLogger(ElasticSearchService.class.getName());
	
	/**
	 * Ne retourne que des actions qui ont amenes un frappeur a etre out ou safe en premire base
	 * Les autres actions : SCORE, STOLE BASE, RUN, PICK OFF, ... ne sont pas pris en compte
	 * 
	 * @param p_file
	 * @param p_players
	 * @param p_field
	 * @param p_oppositeTeam
	 * @param p_umpire
	 * @param p_date
	 */
	public static void generateDocuments(Path p_file, List<Player> p_players, String p_field, String p_oppositeTeam, String p_umpire, LocalDateTime p_date) {

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
		final Map<String, Position> ALL_POSITIONS = GameSheetConfiguration.getInstance().loadAllPositions();
		Map<String, Object> _json = new TreeMap<String, Object>();
		List<String> _plays = new ArrayList<String>();
		List<String> __plays = new ArrayList<String>();
		String _when = null;
		Player _who = null;
		Play _what = null;
		Position _where = null;
		String _playerID = null;
		String _keyword = null;
		
		
// ############## DECOUPER CHAQUE INNING EN ACTION

for (String _inning : innings) {
			
			_inning = _inning.replaceAll("</font>", "</font>#").replaceAll("\\. ", "\\.#");
			_when = SearchInFileUtils.searchBetween(_inning, p_players.get(0).getTeam(), "- </b>");
			logger.debug("[_WHEN] = {}", _when);
			
			if (null != _when) {
				// si l'inning correspond a l'equipe passee en parametre
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
					
						logger.debug("    [__PLAY] = {}", __play);
						
						_playerID = SearchInFileUtils.searchPlayerNameFromPlay(__play.replaceAll("::: ", ""));
						logger.debug("        [_PLAYERID] = {}", _playerID);
						
						_keyword = __play.replaceAll(", SAC", "")
											.replaceAll(", SF", "")
											.replaceAll(", 4 RBI", "")
											.replaceAll(", 3 RBI", "")
											.replaceAll(", 2 RBI", "")
											.replaceAll(", RBI", "")
											.replaceAll("::: ", "")
											.replaceAll(":::", "")
											.substring( _playerID != null ? _playerID.length() + 1 : 0);
						logger.debug("        [_KEY_WORD] = {}", _keyword);

						// MATCH WITH ONE OF THE 
						// fr.bbws.bo.statistics.river.model.GameSheetConfiguration.getInstance().loadAllPlays() KEYWORDS
						_what = Play.UNDEFINED;
						for (String key : ALL_PLAYS.keySet()) {
							if (_keyword.startsWith(key)) { 
								_what = ALL_PLAYS.get(key);
							}
						}
						
						if ( Play.UNDEFINED == _what) {
							logger.error("        [_WHAT] \'{}\' in file [{}] not found GameSheetConfiguration.loadAllPlays", _keyword, p_file); // TODO remettre en error
						}
						
						
						
						// EXACT MATCH WITH ONE OF THE 
						// fr.bbws.bo.statistics.river.model.GameSheetConfiguration.getInstance().loadAllPositions() KEYWORDS
						_where = Position.UNDEFINED;
						for (String key : ALL_POSITIONS.keySet()) {
							if (_keyword.contentEquals(key)) { 
								_where = ALL_POSITIONS.get(key);
							}
						}
						
						if ( Position.UNDEFINED == _where) {
							logger.error("        [_WHERE] \'{}\' in file [{}] not found GameSheetConfiguration.loadAllPositions", _keyword, p_file); // TODO remettre en error
						}
						
						// MATCH WITH ONE OF THE 
						// PLAYERS PUT IN PARAMS
						if ( _playerID != null) {
						
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
							
							if ( _what == Play.DOUBLE_PLAY
									|| _what == Play.HIT_BY_PITCH
									|| _what == Play.INTENTIONAL_WALK
									|| _what == Play.K_LOOKING
									|| _what == Play.K_SWINGING
									|| _what == Play.OBR
									|| _what == Play.OUT
									|| _what == Play.SACRIFICE_FLY
									|| _what == Play.SACRIFICE_HIT
									|| _what == Play.SAFE_ON_ERROR
									|| _what == Play.SAFE_ON_FIELDER_CHOICE
									|| _what == Play.SLUGGING_1B
									|| _what == Play.SLUGGING_2B
									|| _what == Play.SLUGGING_3B
									|| _what == Play.SLUGGING_4B
									|| _what == Play.WALK) {
								
								_json.put("created", LocalDateTime.now().toString());
								_json.put("day", p_date.toString());
								_json.put("field", p_field);
								_json.put("opposite_pitcher", "undefined".toUpperCase()); // TODO opposite pitcher
								_json.put("opposite_team", p_oppositeTeam);
								_json.put("player_id", _who != null ? _who.getID() : _playerID);
								_json.put("player_team", _who != null ? _who.getTeam() : "undefined".toUpperCase());
								_json.put("player_field_position", _who != null ? _who.getFieldPosition() : "undefined".toUpperCase());
								_json.put("player_batting_order",  _who != null ? _who.getBattingOrder() : -1);
								_json.put("when", _when);
								_json.put("what", _what);
								_json.put("where", _where);  // TODO where
								_json.put("umpire_id", p_umpire);
								
								logger.debug("    [_JSON] = {}", _json);
								
								IndexResponse response = ElasticSearchMapper.getInstance().open()
										.prepareIndex("baseball-eu", "pa").setSource(_json, XContentType.JSON).get();

								logger.info("    [STATUT POST] = HTTP {} - ID = {}", 200, "/pa/" + response.getId());
								
							}
						}
					}
				} 
			}
		}
	}
}
