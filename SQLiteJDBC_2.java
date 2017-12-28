package main;

import java.io.IOException;
import java.sql.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import entries.AllEntry;
import entries.FinalEntry;
import entries.HTEntry;
import settings.Settings;
import utils.Lines;
import utils.Utils;
import xls.XlSUtils;

/**
 * PJDCC - Summary for class responsabilities.
 *
 * @author fourplus <fourplus1718@gmail.com>
 * @since 1.0
 * @version 11 Changes done
 */
public class SQLiteJDBC_2 {

	// selects all fixtures for a given season from the database
	// without cl and wc and from 11 matchday up
	public static ArrayList<ExtendedFixture> select(int season) {
		ArrayList<ExtendedFixture> results = new ArrayList<>();

		Connection c = null;
		Statement stmt = null;
		try {
			Class.forName("org.sqlite.JDBC");
			c = DriverManager.getConnection("jdbc:sqlite:test.db");
			c.setAutoCommit(false);

			stmt = c.createStatement();
			ResultSet rs = stmt.executeQuery(
					"select * from results" + season + " where matchday > 10 and competition not in ('CL' ,'WC');");
			while (rs.next()) {
				String date = rs.getString("date");
				String homeTeamName = rs.getString("hometeamname");
				String awayTeamName = rs.getString("awayteamname");
				int homeGoals = rs.getInt("homegoals");
				int awayGoals = rs.getInt("awaygoals");
				String competition = rs.getString("competition");
				int matchday = rs.getInt("matchday");
				synchronized(format){
					ExtendedFixture ef = new ExtendedFixture(format.parse(date), homeTeamName, awayTeamName,
							new Result(homeGoals, awayGoals), competition).withMatchday(matchday);
				}
				results.add(ef);
			}
			rs.close();
			stmt.close();
			c.close();
		} catch (Exception e) {
			System.err.println("Something was wrong");
			System.exit(0);
		}
		return results;
	}

	public static ArrayList<ExtendedFixture> selectLastAll(String team, int count, int season, int matchday,
			String competition) {
		ArrayList<ExtendedFixture> results = new ArrayList<>();

		Connection c = null;
		Statement stmt = null;
		try {
			Class.forName("org.sqlite.JDBC");
			c = DriverManager.getConnection("jdbc:sqlite:test.db");
			c.setAutoCommit(false);

			stmt = c.createStatement();
			ResultSet rs = stmt.executeQuery("select * from results" + season + " where matchday < " + matchday
					+ " and competition='" + competition + "' and ((hometeamname = '" + team + "') or (awayteamname = '"
					+ team + "')) order by matchday" + " desc limit " + count + ";");
			while (rs.next()) {
				String date = rs.getString("date");
				String homeTeamName = rs.getString("hometeamname");
				String awayTeamName = rs.getString("awayteamname");
				int homeGoals = rs.getInt("homegoals");
				int awayGoals = rs.getInt("awaygoals");
				String competit = rs.getString("competition");
				int matchd = rs.getInt("matchday");
				synchronized(format){
					ExtendedFixture ef = new ExtendedFixture(format.parse(date), homeTeamName, awayTeamName,
							new Result(homeGoals, awayGoals), competit).withMatchday(matchd);
				}
				results.add(ef);
			}
			rs.close();
			stmt.close();
			c.close();
		} catch (Exception e) {
			System.err.println("Something was wrong");
			System.exit(0);
		}
		return results;
	}

	public static ArrayList<ExtendedFixture> selectLastHome(String team, int count, int season, int matchday,
			String competition) {
		ArrayList<ExtendedFixture> results = new ArrayList<>();

		Connection c = null;
		Statement stmt = null;
		try {
			Class.forName("org.sqlite.JDBC");
			c = DriverManager.getConnection("jdbc:sqlite:test.db");
			c.setAutoCommit(false);

			stmt = c.createStatement();
			ResultSet rs = stmt.executeQuery("select * from results" + season + " where matchday < " + matchday
					+ " and competition='" + competition + "' and (hometeamname = '" + team + "')  order by matchday"
					+ " desc limit " + count + ";");
			while (rs.next()) {
				String date = rs.getString("date");
				String homeTeamName = rs.getString("hometeamname");
				String awayTeamName = rs.getString("awayteamname");
				int homeGoals = rs.getInt("homegoals");
				int awayGoals = rs.getInt("awaygoals");
				String competit = rs.getString("competition");
				int matchd = rs.getInt("matchday");
				synchronized(format){
				ExtendedFixture ef = new ExtendedFixture(format.parse(date), homeTeamName, awayTeamName,
						new Result(homeGoals, awayGoals), competit).withMatchday(matchd);
				}
				results.add(ef);
			}
			rs.close();
			stmt.close();
			c.close();
		} catch (Exception e) {
			System.err.println("Something was wrong");
			System.exit(0);
		}
		return results;
	}

	public static boolean checkExistense(String hometeam, String awayteam, String date, int season) {
		boolean flag = false;

		Connection c = null;
		Statement stmt = null;
		try {
			Class.forName("org.sqlite.JDBC");
			c = DriverManager.getConnection("jdbc:sqlite:test.db");
			c.setAutoCommit(false);
			stmt = c.createStatement();

			ResultSet rs = stmt
					.executeQuery("select * from results" + season + " where hometeamname = " + addQuotes(hometeam)
							+ " and awayteamname = " + addQuotes(awayteam) + " and date = " + addQuotes(date));
			flag = rs.next();

			rs.close();
			stmt.close();
			c.close();
		} catch (Exception e) {
			System.err.println("Something was wrong");
			System.exit(0);
		}

		return flag;
	}

	public static ArrayList<String> getLeagues(int season) {
		ArrayList<String> leagues = new ArrayList<>();

		Connection c = null;
		Statement stmt = null;
		try {
			Class.forName("org.sqlite.JDBC");
			c = DriverManager.getConnection("jdbc:sqlite:test.db");
			c.setAutoCommit(false);
			stmt = c.createStatement();

			ResultSet rs = stmt.executeQuery("select distinct competition from results" + season);
			while (rs.next()) {
				leagues.add(rs.getString("competition"));
			}

			rs.close();
			stmt.close();
			c.close();
		} catch (Exception e) {
			System.err.println("Something was wrong");
			System.exit(0);
		}
		return leagues;
	}

	public static ArrayList<ExtendedFixture> selectLastAway(String team, int count, int season, int matchday,
			String competition) {
		ArrayList<ExtendedFixture> results = new ArrayList<>();

		Connection c = null;
		Statement stmt = null;
		try {
			Class.forName("org.sqlite.JDBC");
			c = DriverManager.getConnection("jdbc:sqlite:test.db");
			c.setAutoCommit(false);

			stmt = c.createStatement();
			ResultSet rs = stmt.executeQuery("select * from results" + season + " where matchday < " + matchday
					+ " and competition='" + competition + "' and  (awayteamname = '" + team + "') order by matchday"
					+ " desc limit " + count + ";");
			while (rs.next()) {
				String date = rs.getString("date");
				String homeTeamName = rs.getString("hometeamname");
				String awayTeamName = rs.getString("awayteamname");
				int homeGoals = rs.getInt("homegoals");
				int awayGoals = rs.getInt("awaygoals");
				String competit = rs.getString("competition");
				int matchd = rs.getInt("matchday");
				synchronized(format){
					ExtendedFixture ef = new ExtendedFixture(format.parse(date), homeTeamName, awayTeamName,
						new Result(homeGoals, awayGoals), competit).withMatchday(matchd).withStatus("FINISHED");
				}
				results.add(ef);
			}
			rs.close();
			stmt.close();
			c.close();
		} catch (Exception e) {
			System.err.println("Something was wrong");
			System.exit(0);
		}
		return results;
	}

	public static float selectAvgLeagueHome(String competition, int season, int matchday) {
		float average = -1.0f;
		Connection c = null;
		Statement stmt = null;
		try {
			Class.forName("org.sqlite.JDBC");
			c = DriverManager.getConnection("jdbc:sqlite:test.db");
			c.setAutoCommit(false);

			stmt = c.createStatement();
			ResultSet rs = stmt.executeQuery("select avg(homegoals) from results" + season + " where competition="
					+ addQuotes(competition) + " and matchday<" + matchday);
			average = rs.getFloat("avg(homegoals)");

			rs.close();
			stmt.close();
			c.close();
		} catch (Exception e) {
			System.err.println("Something was wrong");
			System.exit(0);
		}
		return average;
	}

	public static float selectAvgLeagueAway(String competition, int season, int matchday) {
		float average = -1.0f;
		Connection c = null;
		Statement stmt = null;
		try {
			Class.forName("org.sqlite.JDBC");
			c = DriverManager.getConnection("jdbc:sqlite:test.db");
			c.setAutoCommit(false);

			stmt = c.createStatement();
			ResultSet rs = stmt.executeQuery("select avg(awaygoals) from results" + season + " where competition="
					+ addQuotes(competition) + " and matchday<" + matchday);
			average = rs.getFloat("avg(awaygoals)");

			rs.close();
			stmt.close();
			c.close();
		} catch (Exception e) {
			System.err.println("Something was wrong");
			System.exit(0);
		}
		return average;
	}

	public static float selectAvgHomeTeamFor(String competition, String team, int season, int matchday) {
		float average = -1.0f;
		Connection c = null;
		Statement stmt = null;
		try {
			Class.forName("org.sqlite.JDBC");
			c = DriverManager.getConnection("jdbc:sqlite:test.db");
			c.setAutoCommit(false);

			stmt = c.createStatement();
			ResultSet rs = stmt.executeQuery("select avg(homegoals) from results" + season + " where competition="
					+ addQuotes(competition) + " and matchday<" + matchday + " and hometeamname=" + addQuotes(team));
			average = rs.getFloat("avg(homegoals)");

			rs.close();
			stmt.close();
			c.close();
		} catch (Exception e) {
			System.err.println("Something was wrong");
			System.exit(0);
		}
		return average;
	}

	public static float selectAvgHomeTeamAgainst(String competition, String team, int season, int matchday) {
		float average = -1.0f;
		Connection c = null;
		Statement stmt = null;
		try {
			Class.forName("org.sqlite.JDBC");
			c = DriverManager.getConnection("jdbc:sqlite:test.db");
			c.setAutoCommit(false);

			stmt = c.createStatement();
			ResultSet rs = stmt.executeQuery("select avg(awaygoals) from results" + season + " where competition="
					+ addQuotes(competition) + " and matchday<" + matchday + " and hometeamname=" + addQuotes(team));
			average = rs.getFloat("avg(awaygoals)");

			rs.close();
			stmt.close();
			c.close();
		} catch (Exception e) {
			System.err.println("Something was wrong");
			System.exit(0);
		}
		return average;
	}

	public static float selectAvgAwayTeamFor(String competition, String team, int season, int matchday) {
		float average = -1.0f;
		Connection c = null;
		Statement stmt = null;
		try {
			Class.forName("org.sqlite.JDBC");
			c = DriverManager.getConnection("jdbc:sqlite:test.db");
			c.setAutoCommit(false);

			stmt = c.createStatement();
			ResultSet rs = stmt.executeQuery("select avg(awaygoals) from results" + season + " where competition="
					+ addQuotes(competition) + " and matchday<" + matchday + " and awayteamname=" + addQuotes(team));
			average = rs.getFloat("avg(awaygoals)");

			rs.close();
			stmt.close();
			c.close();
		} catch (Exception e) {
			System.err.println("Something was wrong");
			System.exit(0);
		}
		return average;
	}

	public static float selectAvgAwayTeamAgainst(String competition, String team, int season, int matchday) {
		float average = -1.0f;
		Connection c = null;
		Statement stmt = null;
		try {
			Class.forName("org.sqlite.JDBC");
			c = DriverManager.getConnection("jdbc:sqlite:test.db");
			c.setAutoCommit(false);

			stmt = c.createStatement();
			ResultSet rs = stmt.executeQuery("select avg(homegoals) from results" + season + " where competition="
					+ addQuotes(competition) + " and matchday<" + matchday + " and awayteamname=" + addQuotes(team));
			average = rs.getFloat("avg(homegoals)");

			rs.close();
			stmt.close();
			c.close();
		} catch (Exception e) {
			System.err.println("Something was wrong");
			System.exit(0);
		}
		return average;
	}

	// update database with all results up to date for a season 30 days back
	public static void update(int season) throws ParseException {
		try {
			JSONArray arr = new JSONArray(
					Utils.query("http://api.football-data.org/alpha/soccerseasons/?season=" + season));
			for (int i = 0; i < arr.length(); i++) {
				String address = arr.getJSONObject(i).getJSONObject("_links").getJSONObject("fixtures")
						.getString("href") + "/?timeFrame=p30";
				String league = arr.getJSONObject(i).getString("league");
				JSONObject obj = createJSONObject(address);
				obj.getJSONArray("fixtures");
				JSONArray jsonFixtures = obj.getJSONArray("fixtures");

				ArrayList<ExtendedFixture> fixtures = Utils.createFixtureList(jsonFixtures);
				for (ExtendedFixture f : fixtures) {
					synchronized(format){
						if (f.status.equals("FINISHED") && !SQLiteJDBC.checkExistense(f.homeTeam, f.awayTeam, format.format(f.date), season))
							SQLiteJDBC.insert(f, league, "RESULTS" + season);
					}
				}
			}
		} catch (IOException | JSONException e) {
			System.out.println("Something was wrong");
		}
	}
	
}
