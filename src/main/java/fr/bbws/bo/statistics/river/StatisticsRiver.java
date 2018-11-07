package fr.bbws.bo.statistics.river;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.bbws.bo.statistics.river.mapper.ConsoleMapper;
import fr.bbws.bo.statistics.river.model.Player;
import fr.bbws.bo.statistics.river.model.Position;
import fr.bbws.bo.statistics.utils.SearchInFileUtils;

public class StatisticsRiver {

	final static Logger logger = LogManager.getLogger(StatisticsRiver.class.getName());
	
	public static void main(String[] p_args) {

		long begin = System.currentTimeMillis();

		ArrayList<Path> file_directories = new ArrayList<Path>();
		// file_directories.add(Paths.get("/Users/alexandrelods/Documents/Developpement/bbws/Games/ForTest"));
		file_directories.add(Paths.get("D:\\A352189\\Sources\\Games\\ForTest"));

		logger.info("##########  ----------------------------- ##########");
		logger.info("##########  BASEBALL GAME UTILS GENERATOR ##########");
		logger.info("##########  ----------------------------- ##########");

		// ############## PARCOURIR LE REPERTOIRE DES FEUILLES DE MATCH
		// ############## ET LE STOCKER EN MEMOIRE

		StringBuffer buffer = new StringBuffer();

		for (Path _file_dir : file_directories) {

			try {

				DirectoryStream<Path> stream = Files.newDirectoryStream(_file_dir); // repertoire contenant les fichiers HTML

				try {
					
					Iterator<Path> iterator = stream.iterator();


					// ############## 
					// ############## POUR CHAQUE FICHIER DU REPERTOIRE
					// ############## 
					while (iterator.hasNext()) {

						Path _current_file = iterator.next();
						
						if (!_current_file.toString().endsWith("DS_Store")) { // pour eviter les pb sur MAC OS
							
							List<String> lines = Files.readAllLines(_current_file, Charset.forName("ISO-8859-1"));
							buffer.delete(0, buffer.length()); 
							// ####  1  ### on deverse les lignes du fichier courant dans un StringBuffer
							for (String line : lines) { // pour chaque ligne du fichier
								buffer.append(line).append(" ");
							}
							
							// ####  2  ### on recherche les attributs 'field', 'date' et 'home plat umpire'
														
							/*
							 * TODO revoir le format de la date
							 * 
							 * date_hour_minute_second or strict_date_hour_minute_second 
							 * A formatter that combines a full date, two digit hour of day, two digit minute of hour, and two digit second of minute : yyyy-MM-dd'T'HH:mm:ss.
							 */
							DateTimeFormatter _formatter = DateTimeFormatter.ofPattern("MMM d, yyyy HH:mm", Locale.ENGLISH);
							LocalDateTime _localDate = LocalDateTime.parse(searchDate(buffer.toString()) + " " + searchTime(buffer.toString()), _formatter);
							String _umpire = searchHomePlateUmpire(buffer.toString());
							String _field = searchField(buffer.toString());
							
							logger.info("_umpire = {}", _umpire);
							logger.info("_date & heure = {}", _localDate);
							logger.info("_field = {}", _field);
							
							
							// ####  3  ### on recherche le nom de chaque equipe
							String _awayTeamName = searchAwayTeam(buffer.toString());
							String _homeTeamName = searchHomeTeam(buffer.toString());
							
							
							
							// ####  4  ### on recherche le line up de chaque equipe
							List<Player> _awayTeam = searchAwayTeamStartingLineUp( buffer.toString());
							
							logger.info("_away team = {}", _awayTeamName);
							logger.info("_away team = {}", _awayTeam);
							
							ConsoleMapper.generateDocuments(
									_current_file,
									_awayTeam,
									_field,
									_homeTeamName,
									_umpire,
									_localDate
									);
							/*
							ElasticSearchMapper.generateDocuments(
															current_file,
															_player,
															_field,
															_homeTeamName,
															_umpire,
															_date
															);*/
							
							
							
							
							List<Player> _homeTeam = searchHomeTeamStartingLineUp( buffer.toString());
							
							logger.info("_home team = {}",_homeTeamName);
							logger.info("_home team = {}", _homeTeam);
							
							ConsoleMapper.generateDocuments(
									_current_file,
									_homeTeam,
									_field,
									_awayTeamName,
									_umpire,
									_localDate
									);
								
							/*ElasticSearchMapper.generateDocuments(
															current_file,
															_player,
															_field,
															_awayTeamName,
															_umpire,
															_date
															);*/
							
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
		logger.info("##########  [Execution time] = {}", time);
	}

	private static String searchHomePlateUmpire(String p_file) {

		String[] lines = p_file.split("Umpires - ");
		String umpires = lines[1].split("<br>")[0];
		return SearchInFileUtils.searchFirstUppercaseWordAfter(umpires, "HP: ");
	}

	private static String searchDate(String p_file) {

		try {
			
			String[] lines = p_file.split("</title>");
			return lines[0].split("\\(")[1].replaceFirst("\\)", "");
			
		} catch (Exception e) {
			return "#erreur#";
		}
	}
	
	private static String searchTime(String p_file) {

		try {
			
			String[] lines = p_file.split("<br>Start: ");
			return lines[1].split(" ")[0];
			
		} catch (Exception e) {
			return "#erreur#";
		}
	}

	private static List<Player> searchAwayTeamStartingLineUp(String p_file) {
		return searchStartingLineUp(p_file, 5, searchAwayTeam(p_file));
	}

	private static List<Player> searchHomeTeamStartingLineUp(String p_file) {
		return searchStartingLineUp(p_file, 6, searchHomeTeam(p_file));
	}
	
	private static String searchAwayTeam(String p_file) {
		return searchTeam(p_file, 5);
	}

	private static String searchHomeTeam(String p_file) {
		return searchTeam(p_file, 6);
	}
	
	private static String searchField(String p_file) {
		
		String[] lines = p_file.split( searchDate(p_file) + " at ");
		return lines[1].split("<br>")[0].trim();
	}
	
	
	/**
	 * 
	 * @param p_file
	 * @param line, le lineup de l'equipe visiteuse est en position 5, le lineup de l'equipe recevante est en position 6
	 * @return le nom des joueurs par position sur le terrain
	 */
	private static List<Player> searchStartingLineUp(String p_file, int p_line, String p_team) {

		String[] lines = p_file.split("<font face=verdana size=2>");
		List<Player> lineup = new ArrayList<Player>();

		// en decoupant le fichier suivant la chaine de caractère "<font face=verdana size=2>"
		// le lineup de l'equipe visiteuse est en position 5
		// le lineup de l'equipe recevante est en position 6
		// c'est cette valeur qui doit etre passe dans le @param line
		String[] strings = lines[p_line].replaceFirst(p_team + " starters:", "").split(";");
		
		String jersey = null;
		String position = null;
		String name = null;
		Player player = null;
		int order = 0;
		
		for (String _string : strings) {
			
			// COMMENT TO DEV : _string.trim() = 25/lf MARTINEZ R
			try {
				
				jersey = SearchInFileUtils.searchBefore(_string.trim(), "/");
				position  = SearchInFileUtils.searchBefore(_string.trim().substring(jersey.length() + 1), " ");
				name = _string.trim().substring(jersey.length() + position.length() + 1).trim();
				player = new Player(name, p_team, jersey, null, 0);
			
			} catch (Exception e) {
				// ne rien faire
				// le bloc switch se chargera de ne pas alimenter la liste
				jersey = "N/A";
				position = "N/A";
				player = null;
			}
			
			switch (position) {
				case "p":
					player.setFieldPosition(Position.PITCHER);
					player.setBattingOrder(++order);
					lineup.add(player);
					break;
				
				case "c":
					player.setFieldPosition(Position.CATCHER);
					player.setBattingOrder(++order);
					lineup.add(player);
					break;
				
				case "1b":
					player.setFieldPosition(Position.FIRST_BASE);
					player.setBattingOrder(++order);
					lineup.add(player);
					break;
				
				case "2b":
					player.setFieldPosition(Position.SECOND_BASE);
					player.setBattingOrder(++order);
					lineup.add(player);
					break;
				
				case "3b":
					player.setFieldPosition(Position.THIRD_BASE);
					player.setBattingOrder(++order);
					lineup.add(player);
					break;
				
				case "ss":
					player.setFieldPosition(Position.SHORTSTOP);
					player.setBattingOrder(++order);
					lineup.add(player);
					break;
				
				case "lf":
					player.setFieldPosition(Position.LEFT_FIELD);
					player.setBattingOrder(++order);
					lineup.add(player);
					break;
				
				case "cf":
					player.setFieldPosition(Position.CENTER_FIELD);
					player.setBattingOrder(++order);
					lineup.add(player);
					break;
				
				case "rf":
					player.setFieldPosition(Position.RIGHT_FIELD);
					player.setBattingOrder(++order);
					lineup.add(player);
					break;
				
				case "dh":
					player.setFieldPosition(Position.DESIGNATED_HITTER);
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
	 * @param p_file
	 * @param p_line, le lineup de l'equipe visiteuse est en position 5, le lineup de l'equipe recevante est en position 6
	 * @return le nom de l'equipe
	 */
	private static String searchTeam(String p_file, int p_line) {

		String[] lines = p_file.split("<font face=verdana size=2>");
		
		// en decoupant le fichier suivant la chaine de caractère "<font face=verdana size=2>"
		// le lineup de l'equipe visiteuse est en position 5
		// le lineup de l'equipe recevante est en position 6
		// c'est cette valeur qui doit etre passe dans le @param line
		
		return lines[p_line].split(" starters:")[0].trim();
	}
}
