package utils;

import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.Random;
import java.util.function.Function;

import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.util.CombinatoricsUtils;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.cyberneko.html.HTMLScanner.CurrentEntity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import charts.LineChart;
import constants.MinMaxOdds;
import entries.AsianEntry;
import entries.FinalEntry;
import entries.FullEntry;
import entries.HTEntry;
import main.ExtendedFixture;
import main.FullFixture;
import main.GoalLines;
import main.Line;
import main.Player;
import main.PlayerFixture;
import main.Result;
import main.SQLiteJDBC;
import main.Test.DataType;
import results.Results;
import scraper.Names;
import scraper.Scraper;
import settings.Settings;
import tables.Position;
import tables.Table;
import xls.AsianUtils;
import xls.XlSUtils;
import xls.XlSUtils.MaximizingBy;

/**
 * PJDCC - Summary for class responsabilities.
 *
 * @author fourplus <fourplus1718@gmail.com>
 * @since 1.0
 * @version 11 Changes done
 */
public class Utils {
    /**
     * This field sets the variable of class DateFormat
     */
	public static final DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    /**
     * This field sets the variable of class String
     */
	public static final String TOKEN = "19f6c3cd0bd54c4286322c08734b53bd";
	static int count = 50;
	static long start = System.currentTimeMillis();

	@SuppressWarnings("unused")
	public static ArrayList<ExtendedFixture> createFixtureList(JSONArray arr) throws JSONException, ParseException {
		ArrayList<ExtendedFixture> fixtures = new ArrayList<>();
		try {
			for (int i = 0; i < arr.length(); i++) {
				JSONObject f = arr.getJSONObject(i);
				String date = f.getString("date");
				String status;
				
				status = f.getString("status");
				
				int matchday = f.getInt("matchday");
				String homeTeamName = f.getString("homeTeamName");
				String awayTeamName = f.getString("awayTeamName");
				int goalsHomeTeam = f.getJSONObject("result").getInt("goalsHomeTeam");
				int goalsAwayTeam = f.getJSONObject("result").getInt("goalsAwayTeam");
				String links_homeTeam = f.getJSONObject("_links").getJSONObject("homeTeam").getString("href");
				String links_awayTeam = f.getJSONObject("_links").getJSONObject("awayTeam").getString("href");
				String competition = f.getJSONObject("_links").getJSONObject("soccerseason").getString("href");
					
				synchronized(format){
				ExtendedFixture ef = new ExtendedFixture(format.parse(date), homeTeamName, awayTeamName,
						new Result(goalsHomeTeam, goalsAwayTeam), competition).withMatchday(matchday);
				}
				
				fixtures.add(ef);
			}
		} catch (Exception e) {
			status = "FINISHED";
		}
		return fixtures;
	}

	@SuppressWarnings("unused")
	public static ArrayList<ExtendedFixture> createFixtureList(JSONObject obj) throws JSONException, ParseException {
		ArrayList<ExtendedFixture> fixtures = new ArrayList<>();
		String date = obj.getString("date");
		String status = obj.getString("status");
		int matchday = obj.getInt("matchday");
		String homeTeamName = obj.getString("homeTeamName");
		String awayTeamName = obj.getString("awayTeamName");
		int goalsHomeTeam = obj.getJSONObject("result").getInt("goalsHomeTeam");
		int goalsAwayTeam = obj.getJSONObject("result").getInt("goalsAwayTeam");
		String links_homeTeam = obj.getJSONObject("_links").getJSONObject("homeTeam").getString("href");
		String links_awayTeam = obj.getJSONObject("_links").getJSONObject("homeTeam").getString("href");
		String competition = obj.getJSONObject("_links").getJSONObject("soccerseason").getString("href");

		synchronized(format){
		ExtendedFixture f = new ExtendedFixture(format.parse(date), homeTeamName, awayTeamName,
				new Result(goalsHomeTeam, goalsAwayTeam), competition).withMatchday(matchday);
		}
		fixtures.add(f);
		return fixtures;
	}

	// get last n fixtures from a list
	// assumes the ordering is the from older to newer
	// public static ArrayList<Fixture> getLastFixtures(ArrayList<Fixture>
	// fixtures, int n) {
	// ArrayList<Fixture> last = new ArrayList<>();
	// int returnedSize = fixtures.size() >= n ? n : fixtures.size();
	// Collections.sort(fixtures, Collections.reverseOrder());
	// for (int i = 0; i < returnedSize; i++) {
	// last.add(fixtures.get(i));
	// }
	// return last;
	// }

	public static ArrayList<ExtendedFixture> getLastFixtures(ArrayList<ExtendedFixture> fixtures, int n) {
		ArrayList<ExtendedFixture> last = new ArrayList<>();
		int returnedSize = fixtures.size() >= n ? n : fixtures.size();
		Collections.sort(fixtures, Collections.reverseOrder());
		for (int i = 0; i < returnedSize; i++) {
			last.add(fixtures.get(i));
		}
		return last;
	}

	public static ArrayList<ExtendedFixture> getHomeFixtures(ExtendedFixture f, ArrayList<ExtendedFixture> fixtures) {
		ArrayList<ExtendedFixture> home = new ArrayList<>();
		for (ExtendedFixture i : fixtures) {
			if (f.homeTeam.equals(i.homeTeam))
				home.add(i);
		}
		return home;
	}

	public static ArrayList<ExtendedFixture> getAwayFixtures(ExtendedFixture f, ArrayList<ExtendedFixture> fixtures) {
		ArrayList<ExtendedFixture> away = new ArrayList<>();
		for (ExtendedFixture i : fixtures) {
			if (f.awayTeam.equals(i.awayTeam))
				away.add(i);
		}
		return away;
	}

	public static float countOverGamesPercent(ArrayList<ExtendedFixture> fixtures) {
		int count = 0;
		for (ExtendedFixture f : fixtures) {
			if (f.getTotalGoals() > 2.5d)
				count++;
		}

		return fixtures.size() == 0 ? 0 : ((float) count / fixtures.size());
	}

	public static float countBTSPercent(ArrayList<ExtendedFixture> fixtures) {
		int count = 0;
		for (ExtendedFixture f : fixtures) {
			if (f.bothTeamScore())
				count++;
		}

		return fixtures.size() == 0 ? 0 : ((float) count / fixtures.size());
	}

	public static float findAvg(ArrayList<ExtendedFixture> lastHomeTeam) {
		float total = 0;
		for (ExtendedFixture f : lastHomeTeam) {
			total += f.getTotalGoals();
		}
		return total / lastHomeTeam.size();
	}

	public static float poisson(float lambda, int goal) {
		return (float) (Math.pow(lambda, goal) * Math.exp(-lambda) / CombinatoricsUtils.factorial(goal));
	}

	public static float poissonOver(float lambda, float mu) {
		float home[] = new float[3];
		float away[] = new float[3];
		for (int i = 0; i < 3; i++) {
			home[i] = poisson(lambda, i);
			away[i] = poisson(mu, i);
		}
		float totalUnder = 0;
		totalUnder += home[0] * away[0] + home[0] * away[1] + home[0] * away[2] + home[1] * away[0] + home[2] * away[0]
				+ home[1] * away[1];
		return 1.0f - totalUnder;
	}

	public static float poissonHome(float lambda, float mu, int offset) {
		float home[] = new float[10];
		float away[] = new float[10];
		for (int i = 0; i < 10; i++) {
			home[i] = poisson(lambda, i);
			away[i] = poisson(mu, i);
		}

		float totalHome = 0f;
		if (offset >= 0) {
			for (int i = offset + 1; i < 10; i++) {
				for (int j = 0; j < i - offset; j++) {
					totalHome += home[i] * away[j];
				}
			}
		} else {
			for (int i = 0; i < 10 + offset; i++) {
				for (int j = 0; j < i - offset; j++) {
					totalHome += home[i] * away[j];
				}
			}
		}

		return totalHome;
	}

	public static float poissonExact(float lambda, float mu, int offset) {
		float home[] = new float[10];
		float away[] = new float[10];
		for (int i = 0; i < 10; i++) {
			home[i] = poisson(lambda, i);
			away[i] = poisson(mu, i);
		}

		float totalHome = 0f;
		if (offset <= 0) {
			for (int i = 0; i < 10 + offset; i++) {
				totalHome += home[i] * away[i - offset];
			}
		} else {
			for (int i = offset; i < 10; i++) {
				totalHome += home[i] * away[i - offset];
			}
		}
		return totalHome;
	}

	public static float poissonAway(float lambda, float mu, int offset) {
		float home[] = new float[10];
		float away[] = new float[10];
		for (int i = 0; i < 10; i++) {
			home[i] = poisson(lambda, i);
			away[i] = poisson(mu, i);
		}

		float totalHome = 0f;
		for (int i = offset + 1; i < 10; i++) {
			for (int j = 0; j < i - offset; j++) {
				totalHome += home[j] * away[i];
			}
		}
		return totalHome;
	}

	public static Pair poissonAsianHome(float lambda, float mu, float line, float asianHome, float asianAway) {

		float fraction = line % 1;
		int whole = (int) (line - fraction);

		if (Float.compare(fraction, 0f)==0) {
			float winChance = poissonHome(lambda, mu, -whole);
			float drawChance = poissonExact(lambda, mu, -whole);

			float home = winChance * asianHome + drawChance - (1f - winChance - drawChance);
			float away = (1f - winChance - drawChance) * asianAway + drawChance - winChance;
			return Pair.of(home, away);

		} else if (Float.compare(fraction, -0.5f)==0) {
			line = whole - 1;
			whole = (int) (line - fraction);
			float winChance = poissonHome(lambda, mu, -whole);

			float home = winChance * asianHome - (1f - winChance);
			float away = (1f - winChance) * asianAway - winChance;
			return Pair.of(home, away);

		} else if (Float.compare(fraction, 0.5f)==0) {
			line = (float) Math.ceil(line);
			fraction = line % 1;
			whole = (int) (line - fraction);
			
			float winChance = poissonHome(lambda, mu, -whole);

			float home = winChance * asianHome - (1f - winChance);
			float away = (1f - winChance) * asianAway - winChance;
			return Pair.of(home, away);

		} else if (Float.compare(fraction, -0.25f)==0) {
			line = whole - 1;
			whole = (int) (line - fraction);
			float winChance = poissonHome(lambda, mu, -whole);
			float drawChance = poissonExact(lambda, mu, -whole);

			float home = winChance * asianHome - drawChance / 2 - (1f - winChance - drawChance);
			float away = (1f - winChance - drawChance) * asianAway + drawChance * (1 + (asianAway - 1) / 2) - winChance;
			return Pair.of(home, away);

		} else if (Float.compare(fraction, 0.25f)==0) {
			line = (float) Math.floor(line);
			fraction = line % 1;
			whole = (int) (line - fraction);

			float winChance = poissonHome(lambda, mu, -whole);
			float drawChance = poissonExact(lambda, mu, -whole);

			float home = winChance * asianHome + drawChance * (1 + (asianHome - 1) / 2) - (1f - winChance - drawChance);
			float away = (1f - winChance - drawChance) * asianAway - drawChance / 2 - winChance;
			return Pair.of(home, away);

		} else if (Float.compare(fraction, -0.75f)==0) {
			line = whole - 1;
			fraction = line % 1;
			whole = (int) (line - fraction);
			float winChance = poissonHome(lambda, mu, -whole);
			float drawChance = poissonExact(lambda, mu, -whole);

			float home = winChance * asianHome + drawChance * (1 + (asianHome - 1) / 2) - (1f - winChance - drawChance);
			float away = (1f - winChance - drawChance) * asianAway - drawChance / 2 - winChance;
			return Pair.of(home, away);

		} else if (Float.compare(fraction, 0.75f)==0) {
			line = (float) Math.ceil(line);
			fraction = line % 1;
			whole = (int) (line - fraction);
			float winChance = poissonHome(lambda, mu, -whole);
			float drawChance = poissonExact(lambda, mu, -whole);

			float home = winChance * asianHome - drawChance / 2 - (1f - winChance - drawChance);
			float away = (1f - winChance - drawChance) * asianAway + (1 + (asianAway - 1) / 2) * drawChance - winChance;
			return Pair.of(home, away);
		} else {
			return Pair.of(-1, -1);
		}

	}

	public static float poissonDraw(float lambda, float mu, int offset) {
		float home[] = new float[5];
		float away[] = new float[5];
		for (int i = 0; i < 5; i++) {
			home[i] = poisson(lambda, i);
			away[i] = poisson(mu, i);
		}
		float totalUnder = 0;
		totalUnder += home[0] * away[0 + offset] + home[1] * away[1 + offset] + home[2] * away[2 + offset]
				+ home[3] * away[3 + offset] + home[4] * away[4 + offset];
		return totalUnder;
	}

	public static float avgFor(String team, ArrayList<ExtendedFixture> fixtures) {
		float total = 0;
		for (ExtendedFixture f : fixtures) {
			if (f.homeTeam.equals(team))
				total += f.result.goalsHomeTeam;
			if (f.awayTeam.equals(team))
				total += f.result.goalsAwayTeam;
		}
		return fixtures.size() == 0 ? 0 : total / fixtures.size();
	}

	public static float getSuccessRate(ArrayList<FinalEntry> list) {
		int success = 0;
		for (FinalEntry fe : list) {
			if (fe.success())
				success++;
		}
		return (float) success / list.size();
	}

	public static float getSuccessRate(ArrayList<FinalEntry> list, float threshold) {
		int success = 0;
		for (FinalEntry fe : list) {
			fe.threshold = threshold;
			if (fe.success())
				success++;
		}
		return (float) success / list.size();
	}

	/**
	 * Filter by min and max odds
	 * 
	 * @param finals
	 * @param minOdds
	 * @param maxOdds
	 * @param threshold
	 * @return
	 */
	public static ArrayList<FinalEntry> filterByOdds(ArrayList<FinalEntry> finals, float minOdds, float maxOdds) {
		ArrayList<FinalEntry> filtered = new ArrayList<>();
		for (FinalEntry fe : finals) {
			float coeff = fe.isOver() ? fe.fixture.maxOver : fe.fixture.maxUnder;
			if (coeff > minOdds && coeff <= maxOdds)
				filtered.add(fe);
		}
		return filtered;
	}

	public static float[] getScaledProfit(ArrayList<FinalEntry> finals, float f) {
		float profit = 0.0f;
		float staked = 0f;
		for (FinalEntry fe : finals) {
			float gain = fe.prediction > fe.upper ? fe.fixture.maxOver : fe.fixture.maxUnder;
			float certainty = fe.prediction > fe.threshold ? fe.prediction : (1f - fe.prediction);
			float cot = fe.prediction > fe.threshold ? (fe.prediction - fe.threshold) : (fe.threshold - fe.prediction);
			float betsize = 1;
			float value = certainty * gain;
			if (value > fe.value) {
				staked += betsize;
				if (fe.success()) {
					if (gain != -1.0d) {
						profit += gain * betsize;
					}
				}
			}
		}
		float[] result = new float[2];
		result[0] = profit - staked;
		result[1] = staked;
		return result;
	}

	public static ArrayList<ExtendedFixture> onlyFixtures(ArrayList<FinalEntry> finals) {
		ArrayList<ExtendedFixture> result = new ArrayList<>();
		for (FinalEntry fe : finals)
			result.add(fe.fixture);
		return result;
	}

	public static ArrayList<FinalEntry> underPredictions(ArrayList<FinalEntry> finals, Settings set) {
		ArrayList<FinalEntry> unders = new ArrayList<>(finals);
		for (FinalEntry fe : finals) {
			if (fe.prediction >= set.lowerBound)
				unders.remove(fe);
		}
		return unders;
	}

	public static ArrayList<FinalEntry> filterTrust(ArrayList<FinalEntry> finals, Settings trset) {
		ArrayList<FinalEntry> filtered = new ArrayList<>();
		for (FinalEntry fe : finals) {
			if (fe.prediction > trset.lowerBound && fe.prediction < trset.upperBound)
				continue;
			filtered.add(fe);
		}
		return filtered;
	}

	public static Settings getSettings(ArrayList<Settings> setts, int year) {
		for (Settings i : setts) {
			if (i.year == year)
				return i;
		}
		return null;
	}

	public static void overUnderStats(ArrayList<FinalEntry> finals) {
		int overCnt = 0, underCnt = 0;
		float overProfit = 0f, underProfit = 0f;
		for (FinalEntry i : finals) {
			if (i.prediction > i.upper) {
				overCnt++;
				overProfit += i.success() ? (i.fixture.maxOver - 1f) : -1f;
			}
			if (i.prediction < i.lower) {
				underCnt++;
				underProfit += i.success() ? (i.fixture.maxUnder - 1f) : -1f;
			}
		}

		System.out.println(overCnt + " overs with profit: " + overProfit);
		System.out.println(underCnt + " unders with profit: " + underProfit);
	}

	public static float countOverHalfTime(ArrayList<ExtendedFixture> fixtures, int i) {

		int count = 0;
		for (ExtendedFixture f : fixtures) {
			if (f.getHalfTimeGoals() >= i)
				count++;
		}

		return fixtures.size() == 0 ? 0 : ((float) count / fixtures.size());
	}

	public static float countHalfTimeGoalAvgExact(ArrayList<ExtendedFixture> fixtures, int i) {

		int count = 0;
		for (ExtendedFixture f : fixtures) {
			if (f.getHalfTimeGoals() == i)
				count++;
		}

		return fixtures.size() == 0 ? 0 : ((float) count / fixtures.size());
	}

	public static float countOversWhenDraw(ArrayList<ExtendedFixture> all) {
		int count = 0;
		for (ExtendedFixture i : all) {
			if (i.result.goalsHomeTeam == i.result.goalsAwayTeam && i.getTotalGoals() > 2.5f)
				count++;
		}
		return all.size() == 0 ? 0 : ((float) count / all.size());
	}

	public static float countOversWhenNotDraw(ArrayList<ExtendedFixture> all) {
		int count = 0;
		for (ExtendedFixture i : all) {
			if (i.result.goalsHomeTeam != i.result.goalsAwayTeam && i.getTotalGoals() > 2.5f)
				count++;
		}
		return all.size() == 0 ? 0 : ((float) count / all.size());
	}
	
	public static ArrayList<FinalEntry> gamesForDay(ArrayList<FinalEntry> pending, LocalDate newdate) {
		return (ArrayList<FinalEntry>) pending.stream()
				.filter(i -> newdate.equals(i.fixture.date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()))
				.collect(Collectors.toList());
	}

}

