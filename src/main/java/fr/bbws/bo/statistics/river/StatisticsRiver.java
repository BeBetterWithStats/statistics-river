package fr.bbws.bo.statistics.river;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import fr.bbws.bo.statistics.river.mapper.ElasticSearchMapper;
import fr.bbws.bo.statistics.river.model.KEY_WORDS;
import fr.bbws.bo.statistics.river.model.Player;
import fr.bbws.bo.statistics.utils.SearchInFileUtils;

public class StatisticsRiver {


	public static void main(String[] args) {

		long begin = System.currentTimeMillis();

		ArrayList<Path> file_directories = new ArrayList<Path>();
		file_directories.add(Paths.get("/Users/alexandrelods/Documents/Developpement/bbws/Games/ForTest"));
//		file_directories.add(Paths.get("D:\\A352189\\Document(s) projet\\Baseball\\stats 2018 ROUEN\\Game test"));

		System.out.println("##########  ----------------------------- ##########");
		System.out.println("##########  BASEBALL GAME UTILS GENERATOR ##########");
		System.out.println("##########  ----------------------------- ##########");

		// ############## PARCOURIR LE REPERTOIRE DES FEUILLES DE MATCH
		// ############## ET LE STOCKER EN MEMOIRE

		StringBuffer buffer = new StringBuffer();

		for (Path file_dir : file_directories) {

			try {

				DirectoryStream<Path> stream = Files.newDirectoryStream(file_dir); // repertoire contenant les fichiers HTML

				try {
					
					Iterator<Path> iterator = stream.iterator();


					// ############## 
					// ############## POUR CHAQUE FICHIER DU REPERTOIRE
					// ############## 
					while (iterator.hasNext()) {

						Path current_file = iterator.next();
						
						if (!current_file.toString().endsWith("DS_Store")) { // pour éviter les pb sur MAC OS
							
							List<String> lines = Files.readAllLines(current_file, Charset.forName("ISO-8859-1"));
							buffer.delete(0, buffer.length()); 
							// ####  1  ### on déverse les lignes du fichier courant dans un StringBuffer
							for (String line : lines) { // pour chaque ligne du fichier
								buffer.append(line).append(" ");
							}
							
							// ####  2  ### on recherche les attributs 'field', 'date' et 'home plat umpire'
							System.out.println("\n");
							
							DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm", Locale.ENGLISH);
							LocalDateTime localDate = LocalDateTime.parse(searchDate(buffer.toString()) + " " + searchTime(buffer.toString()), formatter);
							Date _date = Timestamp.valueOf( localDate);
							String _umpire = searchHomePlateUmpire(buffer.toString());
							String _field = searchField(buffer.toString());
							
							System.out.println("Home plate umpire = " + _umpire);
							System.out.println("Date & Heure = " + _date);
							System.out.println("Field = " + _field);
							
							
							// ####  3  ### on recherche le nom de chaque équipe
							String _awayTeamName = searchAwayTeam(buffer.toString());
							String _homeTeamName = searchHomeTeam(buffer.toString());
							
							
							
							// ####  4  ### on recherche le line up de chaque équipe
							List<Player> _awayTeam = searchAwayTeamStartingLineUp( buffer.toString());
							
							System.out.println("Away team = " + _awayTeamName);
							System.out.println("Away team = " + _awayTeam);
							
							for (Player _player : _awayTeam) {
								ElasticSearchMapper.generateDocuments(
																current_file,
																_player,
																_field,
																_homeTeamName,
																_umpire,
																_date
																);
							}
							
							
							
							List<Player> _homeTeam = searchHomeTeamStartingLineUp( buffer.toString());
							
							System.out.println("Home team = " + _homeTeamName);
							System.out.println("Home team = " + _homeTeam);
							
							for (Player _player : _homeTeam) {
								ElasticSearchMapper.generateDocuments(
																current_file,
																_player,
																_field,
																_awayTeamName,
																_umpire,
																_date
																);
							}
							
							System.out.println("\n");
						}					
						
					} // ############## FIN DU FICHIER COURANT
				} finally {
					stream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		
		
		long time = System.currentTimeMillis() - begin;
		System.out.println("##########  [INFO] Execution time = " + time);
	}

	private static String searchHomePlateUmpire(String file) {

		String[] lines = file.split("Umpires - ");
		String umpires = lines[1].split("<br>")[0];
		return SearchInFileUtils.searchFirstUppercaseWordAfter(umpires, "HP: ");
	}

	private static String searchDate(String file) {

		try {
			
			String[] lines = file.split("</title>");
			return lines[0].split("\\(")[1].replaceFirst("\\)", "");
			
		} catch (Exception e) {
			return "#erreur#";
		}
	}
	
	private static String searchTime(String file) {

		try {
			
			String[] lines = file.split("<br>Start: ");
			return lines[1].split(" ")[0];
			
		} catch (Exception e) {
			return "#erreur#";
		}
	}

//	@Deprecated
//	private static Map<KEY_WORDS, String> searchAwayTeamStartingLineUp(String file) {
//		return searchStartingLineUp(file, 5);
//	}
//
//	@Deprecated
//	private static Map<KEY_WORDS, String> searchHomeTeamStartingLineUp(String file) {
//		return searchStartingLineUp(file, 6);
//	}
	
	private static List<Player> searchAwayTeamStartingLineUp(String file) {
		return searchStartingLineUp_v2(file, 5, searchAwayTeam(file));
	}

	private static List<Player> searchHomeTeamStartingLineUp(String file) {
		return searchStartingLineUp_v2(file, 6, searchHomeTeam(file));
	}
	
	private static String searchAwayTeam(String file) {
		return searchTeam(file, 5);
	}

	private static String searchHomeTeam(String file) {
		return searchTeam(file, 6);
	}
	
	private static String searchField(String file) {
		
		String[] lines = file.split( searchDate(file) + " at ");
		return lines[1].split("<br>")[0].trim();
	}
	
	
	/**
	 * 
	 * @param file
	 * @param line, le lineup de l'équipe visiteuse est en position 5, le lineup de l'équipe recevante est en position 6
	 * @return le nom des joueurs par position sur le terrain
	 */
	private static List<Player> searchStartingLineUp_v2(String file, int line, String team) {

		String[] lines = file.split("<font face=verdana size=2>");
		List<Player> lineup = new ArrayList<Player>();

		// en découpant le fichier suivant la chaine de caractère "<font face=verdana size=2>"
		// le lineup de l'équipe visiteuse est en position 5
		// le lineup de l'équipe recevante est en position 6
		// c'est cette valeur qui doit etre passé dans le @param line
		String[] _strings = lines[line].replaceFirst(team + " starters:", "").split(";");
		
		String jersey = null;
		String position = null;
		String name = null;
		Player player = null;
		int order = 0;
		
		for (String _string : _strings) {
			
			// COMMENT TO DEV : _string.trim() = 25/lf MARTINEZ R
			try {
				
				jersey = SearchInFileUtils.searchBefore(_string.trim(), "/");
				position  = SearchInFileUtils.searchBefore(_string.trim().substring(jersey.length() + 1), " ");
				name = _string.trim().substring(jersey.length() + position.length() + 1).trim();
				player = new Player(name, team, jersey, null, 0);
			
			} catch (Exception e) {
				// ne rien faire
				// le bloc switch se chargera de ne pas alimenter la liste
				jersey = "N/A";
				position = "N/A";
				player = null;
			}
			
			switch (position) {
				case "p":
					player.setFieldPosition(KEY_WORDS.PITCHER);
					player.setBattingOrder(++order);
					lineup.add(player);
					break;
				
				case "c":
					player.setFieldPosition(KEY_WORDS.CATCHER);
					player.setBattingOrder(++order);
					lineup.add(player);
					break;
				
				case "1b":
					player.setFieldPosition(KEY_WORDS.FIRST_BASE);
					player.setBattingOrder(++order);
					lineup.add(player);
					break;
				
				case "2b":
					player.setFieldPosition(KEY_WORDS.SECOND_BASE);
					player.setBattingOrder(++order);
					lineup.add(player);
					break;
				
				case "3b":
					player.setFieldPosition(KEY_WORDS.THIRD_BASE);
					player.setBattingOrder(++order);
					lineup.add(player);
					break;
				
				case "ss":
					player.setFieldPosition(KEY_WORDS.SHORTSTOP);
					player.setBattingOrder(++order);
					lineup.add(player);
					break;
				
				case "lf":
					player.setFieldPosition(KEY_WORDS.LEFT_FIELD);
					player.setBattingOrder(++order);
					lineup.add(player);
					break;
				
				case "cf":
					player.setFieldPosition(KEY_WORDS.CENTER_FIELD);
					player.setBattingOrder(++order);
					lineup.add(player);
					break;
				
				case "rf":
					player.setFieldPosition(KEY_WORDS.RIGHT_FIELD);
					player.setBattingOrder(++order);
					lineup.add(player);
					break;
				
				case "dh":
					player.setFieldPosition(KEY_WORDS.DESIGNATED_HITTER);
					player.setBattingOrder(++order);
					lineup.add(player);
					break;
					
				default:
					break;
			}
		}
		
		return lineup;
	}
	
	/**
	 * 
	 * @param file
	 * @param line, le lineup de l'équipe visiteuse est en position 5, le lineup de l'équipe recevante est en position 6
	 * @return le nom de l'équipe
	 */
	private static String searchTeam(String file, int line) {

		String[] lines = file.split("<font face=verdana size=2>");
		
		// en découpant le fichier suivant la chaine de caractère "<font face=verdana size=2>"
		// le lineup de l'équipe visiteuse est en position 5
		// le lineup de l'équipe recevante est en position 6
		// c'est cette valeur qui doit etre passé dans le @param line
		
		return lines[line].split(" starters:")[0].trim();
	}

}
