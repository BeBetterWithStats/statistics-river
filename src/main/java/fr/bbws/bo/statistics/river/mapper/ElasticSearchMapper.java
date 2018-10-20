package fr.bbws.bo.statistics.river.mapper;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.bbws.bo.statistics.river.model.KEY_WORDS;
import fr.bbws.bo.statistics.river.model.Player;
import fr.bbws.bo.statistics.utils.SearchInFileUtils;

public class ElasticSearchMapper {

	static Map<String, KEY_WORDS> at_bat_key_words = new HashMap<String, KEY_WORDS>();

	static Map<String, KEY_WORDS> hit_key_words = new HashMap<String, KEY_WORDS>();
	
	static Map<String, KEY_WORDS> slugging_key_words = new HashMap<String, KEY_WORDS>();
	
	static List<String> noplay_key_words = new ArrayList<String>();

	
	public static void generateDocuments(Path file, Player player, String field, String oppositeTeam, String umpire, Date date) {

		initialise();
		
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
		StringBuffer json = new StringBuffer();
		
		for (String inning : innings) {
			
			List<String> plays = SearchInFileUtils.searchAfterWithAfterWordAndSpaceIncluded(inning, player.getName());
			
			if (null != plays) {
				
				for (String play : plays) {
					
					json.delete(0, json.length()); // reinitilisation du json
					print_error = true;
					
					for (String key : at_bat_key_words.keySet()) {
						
						if (play.startsWith(player.getName() + key)) { // si un des mot clé apparait

							print_error = false;
							
							//
							// ############## CONSTRUCTION DU DOCUMENT JSON
							// ############## POUR CHAQUE ACTION DU JOUEUR
							
							/* 
							//  ############## exemple de JSON en sortie
							 
							{
							    "day": "2018-05-28 14:05:00",
							    "field": "Templiers Stadium",
							    "opposite team": "Rouen",
							    "umpire": {
							            "id": "AZERTY",
										"prenom": "azerty",
							            "nom": "azerty"
							    },
							    "player": {
							            "id": "AZERTY",
										"prenom": "azerty",
							            "nom": "Leroux",
										"fielding position": 4,
										"lineup position": 1,
										"team": "azerty"
							    },
								"play": {
									"slugging": 2,
									"hitting position": 6,
									"inning": "2nd",
									"opposite pitcher": "QWERTY"
								}
							}
							*/
							
							json.append("{");
							json.append("\"day\"").append(": ").append("\"").append(date.toString()).append("\"").append(",");
							json.append("\"field\"").append(": ").append("\"").append(field).append("\"").append(","); 
							json.append("\"opposite team\"").append(": ").append("\"").append(oppositeTeam).append("\"").append(",");
							json.append("\"umpire\"").append(": {")
								.append("\"").append("id").append("\"").append(": ").append("\"").append(umpire).append("\"").append(",")
								.append("\"").append("prenom").append("\"").append(": ").append("\"").append("TODO").append("\"").append(",") // TODO prenom
								.append("\"").append("nom").append("\"").append(": ").append("\"").append("TODO").append("\"") // TODO nom
								.append("}").append(","); 
							json.append("\"player\"").append(": {")
								.append("\"").append("id").append("\"").append(": ").append("\"").append(player.getName()).append("\"").append(",")
								.append("\"").append("prenom").append("\"").append(": ").append("\"").append("TODO").append("\"").append(",") // TODO prenom
								.append("\"").append("nom").append("\"").append(": ").append("\"").append("TODO").append("\"").append(",") // TODO nom
								.append("\"").append("position in field").append("\"").append(": ").append(player.getFieldPosition()).append(",") 
								.append("\"").append("batting order").append("\"").append(": ").append(player.getBattingOrder()).append(",")
								.append("\"").append("team").append("\"").append(": ").append("\"").append(player.getTeam()).append("\"") 
								.append("}").append(","); 
							json.append("\"play\"").append(": {")
								.append("\"").append("when").append("\"").append(": ").append("\"").append(SearchInFileUtils.searchBetween(inning, player.getTeam(), "- </b>")).append("\"").append(",")
								.append("\"").append("where").append("\"").append(": ").append(at_bat_key_words.get(key).intValue()).append(",") 
								.append("\"").append("slugging").append("\"").append(": ").append(slugging_key_words.get(key) != null ? slugging_key_words.get(key).intValue() : KEY_WORDS.SLUGGING_ZERO.intValue()).append(",")
								.append("\"").append("against").append("\"").append(": ").append("\"").append("TODO").append("\"") // TODO opposite pitcher
								.append("}");  
							json.append("}");
							
							System.out.println("[DEBUG] json = " + json);
							// TODO mettre ici l'envoi du message dans Elastic Search
							break;							
						}
					}
					
					if (print_error) {
						
						for (String key : noplay_key_words) {
							
							if (play.startsWith(player.getName() + key)) {
								print_error = false;
							}
						}
						
						if (print_error) {
							System.out.println("[ERROR] missing play = '" + play + "' [" + file + "]");
							print_error = false;
						} else {
							// NE RIEN FAIRE
						}
					}
				}
			}
		}
		// FIN - DECOUPAGE DE CHAQUE INNING EN ACTION
	}
	
	private static void initialise() {
		
		
		//############## MOTS CLES > POSITION
		at_bat_key_words.put(" advanced", KEY_WORDS.UNKNOWN);
		hit_key_words.put(" doubled, ground-rule", KEY_WORDS.LEFT_FIELD);
		at_bat_key_words.put(" doubled, ground-rule", KEY_WORDS.LEFT_FIELD);
		hit_key_words.put(" doubled to shortstop", KEY_WORDS.SHORTSTOP);
		at_bat_key_words.put(" doubled to shortstop", KEY_WORDS.SHORTSTOP);
		hit_key_words.put(" doubled down the lf line", KEY_WORDS.LEFT_FIELD);
		at_bat_key_words.put(" doubled down the lf line", KEY_WORDS.LEFT_FIELD);
		hit_key_words.put(" doubled down the rf line", KEY_WORDS.RIGHT_FIELD);
		at_bat_key_words.put(" doubled down the rf line", KEY_WORDS.RIGHT_FIELD);
		hit_key_words.put(" doubled through the left side", KEY_WORDS.LEFT_FIELD);
		at_bat_key_words.put(" doubled through the left side", KEY_WORDS.LEFT_FIELD);
		hit_key_words.put(" doubled through the right side", KEY_WORDS.RIGHT_FIELD);
		at_bat_key_words.put(" doubled through the right side", KEY_WORDS.RIGHT_FIELD);
		hit_key_words.put(" doubled to center field", KEY_WORDS.CENTER_FIELD);
		at_bat_key_words.put(" doubled to center field", KEY_WORDS.CENTER_FIELD);
		hit_key_words.put(" doubled to left center", KEY_WORDS.LEFT_FIELD);
		at_bat_key_words.put(" doubled to left center", KEY_WORDS.LEFT_FIELD);
		hit_key_words.put(" doubled to left field", KEY_WORDS.LEFT_FIELD);
		at_bat_key_words.put(" doubled to left field", KEY_WORDS.LEFT_FIELD);
		hit_key_words.put(" doubled to right center", KEY_WORDS.RIGHT_FIELD);
		at_bat_key_words.put(" doubled to right center", KEY_WORDS.RIGHT_FIELD);
		hit_key_words.put(" doubled to right field", KEY_WORDS.RIGHT_FIELD);
		at_bat_key_words.put(" doubled to right field", KEY_WORDS.RIGHT_FIELD);
		hit_key_words.put(" doubled to third base", KEY_WORDS.THIRD_BASE);
		at_bat_key_words.put(" doubled to third base", KEY_WORDS.THIRD_BASE);
		at_bat_key_words.put(" flied into double play 1b", KEY_WORDS.FIRST_BASE);
		at_bat_key_words.put(" flied into double play 2b", KEY_WORDS.SECOND_BASE);
		at_bat_key_words.put(" flied into double play 3b", KEY_WORDS.THIRD_BASE);
		at_bat_key_words.put(" flied into double play cf", KEY_WORDS.CENTER_FIELD);
		at_bat_key_words.put(" flied into double play lf", KEY_WORDS.LEFT_FIELD);
		at_bat_key_words.put(" flied into double play p", KEY_WORDS.PITCHER);
		at_bat_key_words.put(" flied into double play rf", KEY_WORDS.RIGHT_FIELD);
		at_bat_key_words.put(" flied into double play ss", KEY_WORDS.SHORTSTOP);
		at_bat_key_words.put(" flied out to 1b", KEY_WORDS.FIRST_BASE);
		at_bat_key_words.put(" flied out to 2b", KEY_WORDS.SECOND_BASE);
		at_bat_key_words.put(" flied out to 3b", KEY_WORDS.THIRD_BASE);
		at_bat_key_words.put(" flied out to cf", KEY_WORDS.CENTER_FIELD);
		at_bat_key_words.put(" flied out to lf", KEY_WORDS.LEFT_FIELD);
		at_bat_key_words.put(" flied out to p", KEY_WORDS.PITCHER);
		at_bat_key_words.put(" flied out to rf", KEY_WORDS.RIGHT_FIELD);
		at_bat_key_words.put(" flied out to ss", KEY_WORDS.SHORTSTOP);
		at_bat_key_words.put(" fouled into double play 1b", KEY_WORDS.FIRST_BASE);
		at_bat_key_words.put(" fouled into double play 2b", KEY_WORDS.SECOND_BASE);
		at_bat_key_words.put(" fouled into double play 3b", KEY_WORDS.THIRD_BASE);
		at_bat_key_words.put(" fouled into double play c", KEY_WORDS.CATCHER);
		at_bat_key_words.put(" fouled into double play lf", KEY_WORDS.LEFT_FIELD);
		at_bat_key_words.put(" fouled into double play p", KEY_WORDS.PITCHER);
		at_bat_key_words.put(" fouled into double play rf", KEY_WORDS.RIGHT_FIELD);
		at_bat_key_words.put(" fouled into double play ss", KEY_WORDS.SHORTSTOP);
		at_bat_key_words.put(" fouled out to 1b", KEY_WORDS.FIRST_BASE);
		at_bat_key_words.put(" fouled out to 2b", KEY_WORDS.FIRST_BASE);
		at_bat_key_words.put(" fouled out to 3b", KEY_WORDS.THIRD_BASE);
		at_bat_key_words.put(" fouled out to c", KEY_WORDS.CATCHER);
		at_bat_key_words.put(" fouled out to lf", KEY_WORDS.LEFT_FIELD);
		at_bat_key_words.put(" fouled out to p", KEY_WORDS.PITCHER);
		at_bat_key_words.put(" fouled out to rf", KEY_WORDS.RIGHT_FIELD);
		at_bat_key_words.put(" fouled out to ss", KEY_WORDS.THIRD_BASE);
		at_bat_key_words.put(" grounded into double play 1b", KEY_WORDS.FIRST_BASE);
		at_bat_key_words.put(" grounded into double play 2b", KEY_WORDS.SECOND_BASE);
		at_bat_key_words.put(" grounded into double play 3b", KEY_WORDS.THIRD_BASE);
		at_bat_key_words.put(" grounded into double play c", KEY_WORDS.CATCHER);
		at_bat_key_words.put(" grounded into double play p", KEY_WORDS.PITCHER);
		at_bat_key_words.put(" grounded into double play ss", KEY_WORDS.SHORTSTOP);
		at_bat_key_words.put(" grounded out to 1b", KEY_WORDS.FIRST_BASE);
		at_bat_key_words.put(" grounded out to 2b", KEY_WORDS.SECOND_BASE);
		at_bat_key_words.put(" grounded out to 3b", KEY_WORDS.THIRD_BASE);
		at_bat_key_words.put(" grounded out to c", KEY_WORDS.CATCHER);
		at_bat_key_words.put(" grounded out to p", KEY_WORDS.PITCHER);
		at_bat_key_words.put(" grounded out to ss", KEY_WORDS.SHORTSTOP);
		at_bat_key_words.put(" hit by pitch", KEY_WORDS.HIT_BY_PITCH);
		at_bat_key_words.put(" hit into double play 1b", KEY_WORDS.FIRST_BASE);
		at_bat_key_words.put(" hit into double play 2b", KEY_WORDS.SECOND_BASE);
		at_bat_key_words.put(" hit into double play 3b", KEY_WORDS.THIRD_BASE);
		at_bat_key_words.put(" hit into double play c", KEY_WORDS.CATCHER);
		at_bat_key_words.put(" hit into double play p", KEY_WORDS.PITCHER);
		at_bat_key_words.put(" hit into double play ss", KEY_WORDS.SHORTSTOP);
		hit_key_words.put(" homered down the lf line", KEY_WORDS.LEFT_FIELD);
		at_bat_key_words.put(" homered down the lf line", KEY_WORDS.LEFT_FIELD);
		hit_key_words.put(" homered down the rf line", KEY_WORDS.RIGHT_FIELD);
		at_bat_key_words.put(" homered down the rf line", KEY_WORDS.RIGHT_FIELD);
		hit_key_words.put(" homered to center field", KEY_WORDS.CENTER_FIELD);
		at_bat_key_words.put(" homered to center field", KEY_WORDS.CENTER_FIELD);
		hit_key_words.put(" homered to left center", KEY_WORDS.LEFT_FIELD);
		at_bat_key_words.put(" homered to left center", KEY_WORDS.LEFT_FIELD);
		hit_key_words.put(" homered to left field", KEY_WORDS.LEFT_FIELD);
		at_bat_key_words.put(" homered to left field", KEY_WORDS.LEFT_FIELD);
		hit_key_words.put(" homered to right center", KEY_WORDS.RIGHT_FIELD);
		at_bat_key_words.put(" homered to right center", KEY_WORDS.RIGHT_FIELD);
		hit_key_words.put(" homered to right field", KEY_WORDS.RIGHT_FIELD);
		at_bat_key_words.put(" homered to right field", KEY_WORDS.RIGHT_FIELD);
		at_bat_key_words.put(" infield fly to 1b", KEY_WORDS.FIRST_BASE);
		at_bat_key_words.put(" infield fly to 2b", KEY_WORDS.SECOND_BASE);
		at_bat_key_words.put(" infield fly to 3b", KEY_WORDS.THIRD_BASE);
		at_bat_key_words.put(" infield fly to c", KEY_WORDS.CATCHER);
		at_bat_key_words.put(" infield fly to p", KEY_WORDS.PITCHER);
		at_bat_key_words.put(" infield fly to ss", KEY_WORDS.SHORTSTOP);
		at_bat_key_words.put(" infield fly double play c", KEY_WORDS.CATCHER);
		at_bat_key_words.put(" infield fly double play p", KEY_WORDS.PITCHER);
		at_bat_key_words.put(" infield fly double play 1b", KEY_WORDS.FIRST_BASE);
		at_bat_key_words.put(" infield fly double play 2b", KEY_WORDS.SECOND_BASE);
		at_bat_key_words.put(" infield fly double play 3b", KEY_WORDS.THIRD_BASE);
		at_bat_key_words.put(" infield fly double play ss", KEY_WORDS.SHORTSTOP);
		at_bat_key_words.put(" intentionally walked", KEY_WORDS.WALK);
		at_bat_key_words.put(" lined into double play 1b", KEY_WORDS.FIRST_BASE);
		at_bat_key_words.put(" lined into double play 2b", KEY_WORDS.SECOND_BASE);
		at_bat_key_words.put(" lined into double play 3b", KEY_WORDS.THIRD_BASE);
		at_bat_key_words.put(" lined into double play cf", KEY_WORDS.CENTER_FIELD);
		at_bat_key_words.put(" lined into double play lf", KEY_WORDS.LEFT_FIELD);
		at_bat_key_words.put(" lined into double play p", KEY_WORDS.PITCHER);
		at_bat_key_words.put(" lined into double play rf", KEY_WORDS.RIGHT_FIELD);
		at_bat_key_words.put(" lined into double play ss", KEY_WORDS.SHORTSTOP);
		at_bat_key_words.put(" lined into triple play 1b", KEY_WORDS.FIRST_BASE);
		at_bat_key_words.put(" lined into triple play 2b", KEY_WORDS.SECOND_BASE);
		at_bat_key_words.put(" lined into triple play 3b", KEY_WORDS.THIRD_BASE);
		at_bat_key_words.put(" lined into triple play cf", KEY_WORDS.CENTER_FIELD);
		at_bat_key_words.put(" lined into triple play lf", KEY_WORDS.LEFT_FIELD);
		at_bat_key_words.put(" lined into triple play p", KEY_WORDS.PITCHER);
		at_bat_key_words.put(" lined into triple play rf", KEY_WORDS.RIGHT_FIELD);
		at_bat_key_words.put(" lined into triple play ss", KEY_WORDS.SHORTSTOP);
		at_bat_key_words.put(" lined out to 1b", KEY_WORDS.FIRST_BASE);
		at_bat_key_words.put(" lined out to 2b", KEY_WORDS.SECOND_BASE);
		at_bat_key_words.put(" lined out to 3b", KEY_WORDS.THIRD_BASE);
		at_bat_key_words.put(" lined out to cf", KEY_WORDS.CENTER_FIELD);
		at_bat_key_words.put(" lined out to lf", KEY_WORDS.LEFT_FIELD);
		at_bat_key_words.put(" lined out to p", KEY_WORDS.PITCHER);
		at_bat_key_words.put(" lined out to rf", KEY_WORDS.RIGHT_FIELD);
		at_bat_key_words.put(" lined out to ss", KEY_WORDS.SHORTSTOP);
		at_bat_key_words.put(" out at first 1b unassisted", KEY_WORDS.FIRST_BASE);
		at_bat_key_words.put(" out at first 1b to 2b", KEY_WORDS.FIRST_BASE);
		at_bat_key_words.put(" out at first 1b to p", KEY_WORDS.FIRST_BASE);
		at_bat_key_words.put(" out at first 2b to p", KEY_WORDS.SECOND_BASE);
		at_bat_key_words.put(" out at first 2b to ss to 1b", KEY_WORDS.SECOND_BASE);
		at_bat_key_words.put(" out at first 3b to 2b, SAC", KEY_WORDS.THIRD_BASE);
		at_bat_key_words.put(" out at first c to 2b", KEY_WORDS.CATCHER);
		at_bat_key_words.put(" out at first p to 1b, on appeal", KEY_WORDS.UNKNOWN);
		at_bat_key_words.put(" out at first p to 3b to 1b", KEY_WORDS.PITCHER);
		at_bat_key_words.put(" out at first p to ss to 1b", KEY_WORDS.PITCHER);
		at_bat_key_words.put(" out at first p to 2b to 1b", KEY_WORDS.PITCHER);
		at_bat_key_words.put(" out at first p to 2b", KEY_WORDS.PITCHER);
		at_bat_key_words.put(" out at first p to 2b, on appeal", KEY_WORDS.UNKNOWN);
		at_bat_key_words.put(" out at first p to 3b, on appeal", KEY_WORDS.UNKNOWN);
		at_bat_key_words.put(" out at first rf to 1b", KEY_WORDS.RIGHT_FIELD);
		at_bat_key_words.put(" out at first ss to 2b", KEY_WORDS.SHORTSTOP);
		at_bat_key_words.put(" out at third 3b unassisted", KEY_WORDS.THIRD_BASE);
		at_bat_key_words.put(" out on batter's interference", KEY_WORDS.OBR);
		at_bat_key_words.put(" popped into double play 1b to 2b", KEY_WORDS.FIRST_BASE);
		at_bat_key_words.put(" popped into double play 2b to 1b", KEY_WORDS.SECOND_BASE);
		at_bat_key_words.put(" popped up to 1b", KEY_WORDS.FIRST_BASE);
		at_bat_key_words.put(" popped up to 2b", KEY_WORDS.SECOND_BASE);
		at_bat_key_words.put(" popped up to 3b", KEY_WORDS.THIRD_BASE);
		at_bat_key_words.put(" popped up to c", KEY_WORDS.CATCHER);
		at_bat_key_words.put(" popped up to p", KEY_WORDS.PITCHER);
		at_bat_key_words.put(" popped up to ss", KEY_WORDS.SHORTSTOP);
		//at_bat_key_words.put(" reach on a fielding error by 1b, SAC", FieldingPosition.SACRIFICE);
		//at_bat_key_words.put(" reach on a fielding error by 2b, SAC", FieldingPosition.SACRIFICE);
		//at_bat_key_words.put(" reach on a fielding error by 3b, SAC", FieldingPosition.SACRIFICE);
		//at_bat_key_words.put(" reach on a fielding error by c, SAC", FieldingPosition.SACRIFICE);
		//at_bat_key_words.put(" reach on a fielding error by p, SAC", FieldingPosition.SACRIFICE);
		//at_bat_key_words.put(" reach on a fielding error by ss, SAC", FieldingPosition.SACRIFICE);
		at_bat_key_words.put(" reached on a dropped fly by 1b", KEY_WORDS.FIRST_BASE);
		at_bat_key_words.put(" reached on a dropped fly by 2b", KEY_WORDS.SECOND_BASE);
		at_bat_key_words.put(" reached on a dropped fly by 3b", KEY_WORDS.THIRD_BASE);
		at_bat_key_words.put(" reached on a dropped fly by catcher", KEY_WORDS.CATCHER);
		at_bat_key_words.put(" reached on a dropped fly by cf", KEY_WORDS.CENTER_FIELD);
		at_bat_key_words.put(" reached on a dropped fly by lf", KEY_WORDS.LEFT_FIELD);
		at_bat_key_words.put(" reached on a dropped fly by p", KEY_WORDS.PITCHER);
		at_bat_key_words.put(" reached on a dropped fly by rf", KEY_WORDS.RIGHT_FIELD);
		at_bat_key_words.put(" reached on a dropped fly by ss", KEY_WORDS.SHORTSTOP);
		at_bat_key_words.put(" reached on a dropped fly by c;", KEY_WORDS.CATCHER);
		at_bat_key_words.put(" reached on a dropped fly by c.", KEY_WORDS.CATCHER);
		at_bat_key_words.put(" reached on a fielder's choice to catcher", KEY_WORDS.CATCHER);
		at_bat_key_words.put(" reached on a fielder's choice to center field", KEY_WORDS.CENTER_FIELD);
		at_bat_key_words.put(" reached on a fielder's choice to first base", KEY_WORDS.FIRST_BASE);
		at_bat_key_words.put(" reached on a fielder's choice to left field", KEY_WORDS.LEFT_FIELD);
		at_bat_key_words.put(" reached on a fielder's choice to pitcher", KEY_WORDS.PITCHER);
		at_bat_key_words.put(" reached on a fielder's choice to right field", KEY_WORDS.RIGHT_FIELD);
		at_bat_key_words.put(" reached on a fielder's choice to second base", KEY_WORDS.SECOND_BASE);
		at_bat_key_words.put(" reached on a fielder's choice to shortstop", KEY_WORDS.SHORTSTOP);
		at_bat_key_words.put(" reached on a fielder's choice to third base", KEY_WORDS.THIRD_BASE);
		at_bat_key_words.put(" reached on a fielder's choice", KEY_WORDS.UNKNOWN);
		at_bat_key_words.put(" reached on a fielding error by 1b", KEY_WORDS.FIRST_BASE);
		at_bat_key_words.put(" reached on a fielding error by 2b", KEY_WORDS.SECOND_BASE);
		at_bat_key_words.put(" reached on a fielding error by 3b", KEY_WORDS.THIRD_BASE);
		at_bat_key_words.put(" reached on a fielding error by c", KEY_WORDS.CATCHER);
		at_bat_key_words.put(" reached on a fielding error by p", KEY_WORDS.PITCHER);
		at_bat_key_words.put(" reached on a fielding error by ss", KEY_WORDS.SHORTSTOP);
		at_bat_key_words.put(" reached on a muffed throw by 1b", KEY_WORDS.FIRST_BASE);
		at_bat_key_words.put(" reached on a muffed throw by 2b", KEY_WORDS.SECOND_BASE);
		at_bat_key_words.put(" reached on a muffed throw by 3b", KEY_WORDS.THIRD_BASE);
		at_bat_key_words.put(" reached on a muffed throw by c", KEY_WORDS.CATCHER);
		at_bat_key_words.put(" reached on a muffed throw by p", KEY_WORDS.PITCHER);
		at_bat_key_words.put(" reached on a muffed throw by ss", KEY_WORDS.SHORTSTOP);
		at_bat_key_words.put(" reached on a throwing error by 1b", KEY_WORDS.FIRST_BASE);
		at_bat_key_words.put(" reached on a throwing error by 2b", KEY_WORDS.SECOND_BASE);
		at_bat_key_words.put(" reached on a throwing error by 3b", KEY_WORDS.THIRD_BASE);
		at_bat_key_words.put(" reached on a throwing error by cf", KEY_WORDS.RIGHT_FIELD);
		at_bat_key_words.put(" reached on a throwing error by c", KEY_WORDS.CATCHER);
		at_bat_key_words.put(" reached on a throwing error by lf", KEY_WORDS.RIGHT_FIELD);
		at_bat_key_words.put(" reached on a throwing error by p", KEY_WORDS.PITCHER);
		at_bat_key_words.put(" reached on a throwing error by rf", KEY_WORDS.RIGHT_FIELD);
		at_bat_key_words.put(" reached on a throwing error by ss", KEY_WORDS.SHORTSTOP);
		at_bat_key_words.put(" reached on an error by 1b", KEY_WORDS.FIRST_BASE);
		at_bat_key_words.put(" reached on an error by 2b", KEY_WORDS.SECOND_BASE);
		at_bat_key_words.put(" reached on an error by 3b", KEY_WORDS.THIRD_BASE);
		at_bat_key_words.put(" reached on an error by ss", KEY_WORDS.SHORTSTOP);
		at_bat_key_words.put(" reached on an error by p", KEY_WORDS.PITCHER);
		at_bat_key_words.put(" reached on an error by cf", KEY_WORDS.CENTER_FIELD);
		at_bat_key_words.put(" reached on an error by lf", KEY_WORDS.LEFT_FIELD);
		at_bat_key_words.put(" reached on an error by rf", KEY_WORDS.RIGHT_FIELD);
		hit_key_words.put(" singled down the lf line", KEY_WORDS.LEFT_FIELD);
		at_bat_key_words.put(" singled down the lf line", KEY_WORDS.LEFT_FIELD);
		hit_key_words.put(" singled down the rf line", KEY_WORDS.RIGHT_FIELD);
		at_bat_key_words.put(" singled down the rf line", KEY_WORDS.RIGHT_FIELD);
		hit_key_words.put(" singled through the left side", KEY_WORDS.LEFT_FIELD);
		at_bat_key_words.put(" singled through the left side", KEY_WORDS.LEFT_FIELD);
		hit_key_words.put(" singled through the right side", KEY_WORDS.RIGHT_FIELD);
		at_bat_key_words.put(" singled through the right side", KEY_WORDS.RIGHT_FIELD);
		hit_key_words.put(" singled to 1b", KEY_WORDS.FIRST_BASE);
		at_bat_key_words.put(" singled to 1b", KEY_WORDS.FIRST_BASE);
		hit_key_words.put(" singled to 2b", KEY_WORDS.SECOND_BASE);
		at_bat_key_words.put(" singled to 2b", KEY_WORDS.SECOND_BASE);
		hit_key_words.put(" singled to 3b", KEY_WORDS.THIRD_BASE);
		at_bat_key_words.put(" singled to 3b", KEY_WORDS.THIRD_BASE);
		hit_key_words.put(" singled to catcher", KEY_WORDS.CATCHER);
		at_bat_key_words.put(" singled to catcher", KEY_WORDS.CATCHER);
		hit_key_words.put(" singled to center field", KEY_WORDS.CENTER_FIELD);
		at_bat_key_words.put(" singled to center field", KEY_WORDS.CENTER_FIELD);
		hit_key_words.put(" singled to first base", KEY_WORDS.FIRST_BASE);
		at_bat_key_words.put(" singled to first base", KEY_WORDS.FIRST_BASE);
		hit_key_words.put(" singled to left center", KEY_WORDS.LEFT_FIELD);
		at_bat_key_words.put(" singled to left center", KEY_WORDS.LEFT_FIELD);
		hit_key_words.put(" singled to left field", KEY_WORDS.LEFT_FIELD);
		at_bat_key_words.put(" singled to left field", KEY_WORDS.LEFT_FIELD);
		hit_key_words.put(" singled to pitcher", KEY_WORDS.PITCHER);
		at_bat_key_words.put(" singled to pitcher", KEY_WORDS.PITCHER);
		hit_key_words.put(" singled to right center", KEY_WORDS.RIGHT_FIELD);
		at_bat_key_words.put(" singled to right center", KEY_WORDS.RIGHT_FIELD);
		hit_key_words.put(" singled to right field", KEY_WORDS.RIGHT_FIELD);
		at_bat_key_words.put(" singled to right field", KEY_WORDS.RIGHT_FIELD);
		hit_key_words.put(" singled to second base", KEY_WORDS.SECOND_BASE);
		at_bat_key_words.put(" singled to second base", KEY_WORDS.SECOND_BASE);
		hit_key_words.put(" singled to shortstop", KEY_WORDS.SHORTSTOP);
		at_bat_key_words.put(" singled to shortstop", KEY_WORDS.SHORTSTOP);
		hit_key_words.put(" singled to ss", KEY_WORDS.SHORTSTOP);
		at_bat_key_words.put(" singled to ss", KEY_WORDS.SHORTSTOP);
		hit_key_words.put(" singled to third base", KEY_WORDS.THIRD_BASE);
		at_bat_key_words.put(" singled to third base", KEY_WORDS.THIRD_BASE);
		hit_key_words.put(" singled up the middle", KEY_WORDS.CENTER_FIELD);
		at_bat_key_words.put(" singled up the middle", KEY_WORDS.CENTER_FIELD);
		at_bat_key_words.put(" stole", KEY_WORDS.STOLEN_BASE);
		at_bat_key_words.put(" struck out.", KEY_WORDS.K_LOOKING);
		at_bat_key_words.put(" struck out looking", KEY_WORDS.K_LOOKING);
		at_bat_key_words.put(" struck out swinging", KEY_WORDS.K_SWINGING);
		at_bat_key_words.put(" struck out, out at first", KEY_WORDS.K_SWINGING);
		at_bat_key_words.put(" struck out to catcher", KEY_WORDS.K_SWINGING);
		at_bat_key_words.put(" struck out, reached first on a passed ball", KEY_WORDS.K_SWINGING);
		at_bat_key_words.put(" struck out, reached first on a throwing error", KEY_WORDS.K_SWINGING);
		at_bat_key_words.put(" struck out, reached first on a muffed throw by", KEY_WORDS.K_SWINGING);
		at_bat_key_words.put(" struck out, reached first on a wild pitch", KEY_WORDS.K_SWINGING);
		hit_key_words.put(" tripled to first base", KEY_WORDS.FIRST_BASE);
		at_bat_key_words.put(" tripled to first base", KEY_WORDS.FIRST_BASE);
		hit_key_words.put(" tripled down the lf line", KEY_WORDS.LEFT_FIELD);
		at_bat_key_words.put(" tripled down the lf line", KEY_WORDS.LEFT_FIELD);
		hit_key_words.put(" tripled down the rf line", KEY_WORDS.RIGHT_FIELD);
		at_bat_key_words.put(" tripled down the rf line", KEY_WORDS.RIGHT_FIELD);
		hit_key_words.put(" tripled through the left side", KEY_WORDS.LEFT_FIELD);
		at_bat_key_words.put(" tripled through the left side", KEY_WORDS.LEFT_FIELD);
		hit_key_words.put(" tripled through the right side", KEY_WORDS.RIGHT_FIELD);
		at_bat_key_words.put(" tripled through the right side", KEY_WORDS.RIGHT_FIELD);
		hit_key_words.put(" tripled to center field", KEY_WORDS.CENTER_FIELD);
		at_bat_key_words.put(" tripled to center field", KEY_WORDS.CENTER_FIELD);
		hit_key_words.put(" tripled to left center", KEY_WORDS.LEFT_FIELD);
		at_bat_key_words.put(" tripled to left center", KEY_WORDS.LEFT_FIELD);
		hit_key_words.put(" tripled to left field", KEY_WORDS.LEFT_FIELD);
		at_bat_key_words.put(" tripled to left field", KEY_WORDS.LEFT_FIELD);
		hit_key_words.put(" tripled to right center", KEY_WORDS.RIGHT_FIELD);
		at_bat_key_words.put(" tripled to right center", KEY_WORDS.RIGHT_FIELD);
		hit_key_words.put(" tripled to right field", KEY_WORDS.RIGHT_FIELD);
		at_bat_key_words.put(" tripled to right field", KEY_WORDS.RIGHT_FIELD);
		at_bat_key_words.put(" walked", KEY_WORDS.WALK);
		
		slugging_key_words.put(" doubled, ground-rule", KEY_WORDS.SLUGGING_2B);
		slugging_key_words.put(" doubled to shortstop", KEY_WORDS.SLUGGING_2B);
		slugging_key_words.put(" doubled down the lf line", KEY_WORDS.SLUGGING_2B);
		slugging_key_words.put(" doubled down the rf line", KEY_WORDS.SLUGGING_2B);
		slugging_key_words.put(" doubled through the left side", KEY_WORDS.SLUGGING_2B);
		slugging_key_words.put(" doubled through the right side", KEY_WORDS.SLUGGING_2B);
		slugging_key_words.put(" doubled to center field", KEY_WORDS.SLUGGING_2B);
		slugging_key_words.put(" doubled to left center", KEY_WORDS.SLUGGING_2B);
		slugging_key_words.put(" doubled to left field", KEY_WORDS.SLUGGING_2B);
		slugging_key_words.put(" doubled to right center", KEY_WORDS.SLUGGING_2B);
		slugging_key_words.put(" doubled to right field", KEY_WORDS.SLUGGING_2B);
		slugging_key_words.put(" doubled to third base", KEY_WORDS.SLUGGING_2B);
		slugging_key_words.put(" homered down the lf line", KEY_WORDS.SLUGGING_4B);
		slugging_key_words.put(" homered down the rf line", KEY_WORDS.SLUGGING_4B);
		slugging_key_words.put(" homered to center field", KEY_WORDS.SLUGGING_4B);
		slugging_key_words.put(" homered to left center", KEY_WORDS.SLUGGING_4B);
		slugging_key_words.put(" homered to left field", KEY_WORDS.SLUGGING_4B);
		slugging_key_words.put(" homered to right center", KEY_WORDS.SLUGGING_4B);
		slugging_key_words.put(" homered to right field", KEY_WORDS.SLUGGING_4B);
		slugging_key_words.put(" singled down the lf line", KEY_WORDS.SLUGGING_1B);
		slugging_key_words.put(" singled down the rf line", KEY_WORDS.SLUGGING_1B);
		slugging_key_words.put(" singled through the left side", KEY_WORDS.SLUGGING_1B);
		slugging_key_words.put(" singled through the right side", KEY_WORDS.SLUGGING_1B);
		slugging_key_words.put(" singled to 1b", KEY_WORDS.SLUGGING_1B);
		slugging_key_words.put(" singled to 2b", KEY_WORDS.SLUGGING_1B);
		slugging_key_words.put(" singled to 3b", KEY_WORDS.SLUGGING_1B);
		slugging_key_words.put(" singled to catcher", KEY_WORDS.SLUGGING_1B);
		slugging_key_words.put(" singled to center field", KEY_WORDS.SLUGGING_1B);
		slugging_key_words.put(" singled to first base", KEY_WORDS.SLUGGING_1B);
		slugging_key_words.put(" singled to left center", KEY_WORDS.SLUGGING_1B);
		slugging_key_words.put(" singled to left field", KEY_WORDS.SLUGGING_1B);
		slugging_key_words.put(" singled to pitcher", KEY_WORDS.SLUGGING_1B);
		slugging_key_words.put(" singled to right center", KEY_WORDS.SLUGGING_1B);
		slugging_key_words.put(" singled to right field", KEY_WORDS.SLUGGING_1B);
		slugging_key_words.put(" singled to second base", KEY_WORDS.SLUGGING_1B);
		slugging_key_words.put(" singled to shortstop", KEY_WORDS.SLUGGING_1B);
		slugging_key_words.put(" singled to ss", KEY_WORDS.SLUGGING_1B);
		slugging_key_words.put(" singled to third base", KEY_WORDS.SLUGGING_1B);
		slugging_key_words.put(" singled up the middle", KEY_WORDS.SLUGGING_1B);
		slugging_key_words.put(" tripled down the lf line", KEY_WORDS.SLUGGING_3B);
		slugging_key_words.put(" tripled to first base", KEY_WORDS.SLUGGING_3B);
		slugging_key_words.put(" tripled down the rf line", KEY_WORDS.SLUGGING_3B);
		slugging_key_words.put(" tripled through the left side", KEY_WORDS.SLUGGING_3B);
		slugging_key_words.put(" tripled through the right side", KEY_WORDS.SLUGGING_3B);
		slugging_key_words.put(" tripled to center field", KEY_WORDS.SLUGGING_3B);
		slugging_key_words.put(" tripled to left center", KEY_WORDS.SLUGGING_3B);
		slugging_key_words.put(" tripled to left field", KEY_WORDS.SLUGGING_3B);
		slugging_key_words.put(" tripled to right center", KEY_WORDS.SLUGGING_3B);
		slugging_key_words.put(" tripled to right field", KEY_WORDS.SLUGGING_3B);
		slugging_key_words.put(" grounded into double play 1b", KEY_WORDS.SLUGGING_GDP);
		slugging_key_words.put(" grounded into double play 2b", KEY_WORDS.SLUGGING_GDP);
		slugging_key_words.put(" grounded into double play 3b", KEY_WORDS.SLUGGING_GDP);
		slugging_key_words.put(" grounded into double play c", KEY_WORDS.SLUGGING_GDP);
		slugging_key_words.put(" grounded into double play p", KEY_WORDS.SLUGGING_GDP);
		slugging_key_words.put(" grounded into double play ss", KEY_WORDS.SLUGGING_GDP);
		slugging_key_words.put(" hit into double play 1b", KEY_WORDS.SLUGGING_GDP);
		slugging_key_words.put(" hit into double play 2b", KEY_WORDS.SLUGGING_GDP);
		slugging_key_words.put(" hit into double play 3b", KEY_WORDS.SLUGGING_GDP);
		slugging_key_words.put(" hit into double play c", KEY_WORDS.SLUGGING_GDP);
		slugging_key_words.put(" hit into double play p", KEY_WORDS.SLUGGING_GDP);
		slugging_key_words.put(" hit into double play ss", KEY_WORDS.SLUGGING_GDP);
		slugging_key_words.put(" lined into double play 1b", KEY_WORDS.SLUGGING_GDP);
		slugging_key_words.put(" lined into double play 2b", KEY_WORDS.SLUGGING_GDP);
		slugging_key_words.put(" lined into double play 3b", KEY_WORDS.SLUGGING_GDP);
		slugging_key_words.put(" lined into double play cf", KEY_WORDS.SLUGGING_GDP);
		slugging_key_words.put(" lined into double play lf", KEY_WORDS.SLUGGING_GDP);
		slugging_key_words.put(" lined into double play p", KEY_WORDS.SLUGGING_GDP);
		slugging_key_words.put(" lined into double play rf", KEY_WORDS.SLUGGING_GDP);
		slugging_key_words.put(" lined into double play ss", KEY_WORDS.SLUGGING_GDP);
		slugging_key_words.put(" lined into triple play 1b", KEY_WORDS.SLUGGING_GDP);
		slugging_key_words.put(" lined into triple play 2b", KEY_WORDS.SLUGGING_GDP);
		slugging_key_words.put(" lined into triple play 3b", KEY_WORDS.SLUGGING_GDP);
		slugging_key_words.put(" lined into triple play cf", KEY_WORDS.SLUGGING_GDP);
		slugging_key_words.put(" lined into triple play lf", KEY_WORDS.SLUGGING_GDP);
		slugging_key_words.put(" lined into triple play p", KEY_WORDS.SLUGGING_GDP);
		slugging_key_words.put(" lined into triple play rf", KEY_WORDS.SLUGGING_GDP);
		slugging_key_words.put(" lined into triple play ss", KEY_WORDS.SLUGGING_GDP);
		
		noplay_key_words.add(".");
		noplay_key_words.add(";");
		noplay_key_words.add(" .");
		noplay_key_words.add(" ;");
		noplay_key_words.add(" failed pickoff attempt");
		noplay_key_words.add(" out at first c to 1b, picked off");
		noplay_key_words.add(" out at first p to 1b, picked off");
		noplay_key_words.add(" out at first p to 1b, caught stealing");
		noplay_key_words.add(" out at first c to 1b, caught stealing");
		noplay_key_words.add(" out at second p to");
		noplay_key_words.add(" out at second c to");
		noplay_key_words.add(" out at second 2b to");
		noplay_key_words.add(" out at second ss to");
		noplay_key_words.add(" out at second 3b to");
		noplay_key_words.add(" out at second 1b to");
		noplay_key_words.add(" out at second rf to");
		noplay_key_words.add(" out at second cf to");
		noplay_key_words.add(" out at second lf to");
		noplay_key_words.add(" out at second ss unassisted");
		noplay_key_words.add(" out at second 2b unassisted");
		noplay_key_words.add(" out at second 3b unassisted");
		noplay_key_words.add(" out on the play");
		noplay_key_words.add(" out at third p to");
		noplay_key_words.add(" out at third c to");
		noplay_key_words.add(" out at third 2b to");
		noplay_key_words.add(" out at third ss to");
		noplay_key_words.add(" out at third 3b to");
		noplay_key_words.add(" out at third 1b to");
		noplay_key_words.add(" out at third rf to");
		noplay_key_words.add(" out at third cf to");
		noplay_key_words.add(" out at third lf to");
		noplay_key_words.add(" out at home p unassisted");
		noplay_key_words.add(" out at home c unassisted");
		noplay_key_words.add(" out at home c to");
		noplay_key_words.add(" out at home p to");
		noplay_key_words.add(" out at home 3b to");
		noplay_key_words.add(" out at home 1b to");
		noplay_key_words.add(" out at home 2b to");
		noplay_key_words.add(" out at home ss to");
		noplay_key_words.add(" scored");
		noplay_key_words.add(" to rf.");
		noplay_key_words.add(" to cf.");
		noplay_key_words.add(" to lf.");
		noplay_key_words.add(" to 3b.");
		noplay_key_words.add(" to 2b.");
		noplay_key_words.add(" to 1b.");
		noplay_key_words.add(" to ss.");
		noplay_key_words.add(" to p.");
		noplay_key_words.add(" to c.");
		noplay_key_words.add(" to dh.");
		noplay_key_words.add(" to rf for");
		noplay_key_words.add(" to cf for");
		noplay_key_words.add(" to lf for");
		noplay_key_words.add(" to 3b for");
		noplay_key_words.add(" to 2b for");
		noplay_key_words.add(" to 1b for");
		noplay_key_words.add(" to ss for");
		noplay_key_words.add(" to p for");
		noplay_key_words.add(" to c for");
		noplay_key_words.add(" to dh for");
		noplay_key_words.add(" pinch hit for");
		noplay_key_words.add(" placed on first");
		noplay_key_words.add(" placed on second");
		noplay_key_words.add(" picked off");
		
		
		
		noplay_key_words.add(" out by rule - Infield fly not caught :::");
		noplay_key_words.add(" out by rule - Running out of line :::");
		noplay_key_words.add(" out by rule - Touched by own batted ball :::");
		noplay_key_words.add(" out by rule - Runner interfered with fielder :::");
		noplay_key_words.add(" out by rule - Interference by the batter :::");
		noplay_key_words.add(" out by rule - Interference by preceding runner :::");
		noplay_key_words.add(" - OBR - Running out of line :::");
		noplay_key_words.add(" out by rule - Bunted foul on third strike :::");
		//############## FIN -- MOTS CLES > POSITION
	}
}
