package scraper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.interactions.Actions;

import constants.MinMaxOdds;
import main.AsianLines;
import main.ExtendedFixture;
import main.FullFixture;
import main.GoalLines;
import main.Line;
import main.PlayerFixture;
import main.Result;
import main.SQLiteJDBC;
import odds.AsianOdds;
import odds.MatchOdds;
import odds.Odds;
import odds.OverUnderOdds;
import predictions.Predictions.OnlyTodayMatches;
import predictions.UpdateType;
import runner.RunnerOdds;
import runner.UpdateRunner;
import utils.Utils;
import xls.XlSUtils;

/**
 * PJDCC - Summary for class responsabilities.
 *
 * @author fourplus <fourplus1718@gmail.com>
 * @since 1.0
 * @version 36 Changes done
 */
public class Scraper {
    /**
     * This field sets the variable of class DateFormat
     */
	public static final DateFormat OPTAFORMAT = new SimpleDateFormat("dd MMMM yyyy", Locale.US);
    /**
     * This field sets the variable of class DateFormat
     */
	public static final DateFormat FORMATFULL = new SimpleDateFormat("dd MMMM yyyy HH:mm", Locale.US);
    /**
     * This field sets the variable of class String
     */
	public static final String BASE = "http://int.soccerway.com/";
    /**
     * This field sets the variable of class String
     */
	public static final String OUSUFFIX = "#over-under;2;2.50;0";
    /**
     * This field sets the final int variable
     */
	public static final int CURRENT_YEAR = 2017;

	public static void main(String[] args)
			throws IOException, ParseException, InterruptedException, ExecutionException {
		long start = System.currentTimeMillis();

		 checkAndUpdate("BRA", OnlyTodayMatches.FALSE);

		Runtime.getRuntime().exec("taskkill /F /IM chromedriver.exe /T");
		System.out.println((System.currentTimeMillis() - start) / 1000d + "sec");
	}

	private static Runnable createRunnable(String i, ArrayList<String> onlyToday){
		return new UpdateRunner(i, onlyToday);
	}
	
	/**
	 * Updates list of leagues in parallel
	 * 
	 * @param list
	 *            - list of leagues to be updated
	 * @param n
	 *            - number of leagues updating in parallel
	 * @param onlyToday
	 *            - flag for getting next matches only for today (for speed up)
	 * @param automatic
	 *            - type of update - manual - hardcoded leagues, automatic -
	 *            tracking leagues that have games today
	 * @param k
	 * @param j
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static void updateInParallel(ArrayList<String> list, int n, OnlyTodayMatches onlyToday, UpdateType automatic,
			int day, int month) throws IOException, InterruptedException {

		ExecutorService executor = Executors.newFixedThreadPool(n);
		ArrayList<String> leagues = automatic.equals(UpdateType.AUTOMATIC) ? getTodaysLeagueList(day, month) : list;
		System.out.println("Updating for: ");
		System.out.println(leagues);
		for (String i : leagues) {
			Runnable worker = createRunnable(i, onlyToday);
			executor.execute(worker);
		}
		// This will make the executor accept no new threads
		// and finish all existing threads in the queue
		executor.shutdown();
		// Wait until all threads are finish
		// executor.awaitTermination(0, null);
	}

	public static ArrayList<String> getTodaysLeagueList(int day, int month) throws IOException {
		ArrayList<String> result = new ArrayList<>();

		Document page = Jsoup
				.connect("http://www.soccerway.com/matches/2017/" + month + "/" + (day >= 10 ? day : ("0" + day)) + "/")
				.timeout(0).get();

		HashMap<String, String> leagueDescriptions = EntryPoints.getTrackingLeagueDescriptions();
		Elements linksM = page.select("th.competition-link");
		for (Element i : linksM) {
			String href = i.childNode(1).attr("href");
			if (href.contains("national")) {
				href = href.substring(0, StringUtils.ordinalIndexOf(href, "/", 4) + 1);
				if (leagueDescriptions.keySet().contains(href))
					result.add(leagueDescriptions.get(href));
			}
		}
		return result;
	}
	
	public static void checkAndUpdate(String competition, OnlyTodayMatches onlyTodaysMatches)
			throws IOException, ParseException, InterruptedException {
		String base = new File("").getAbsolutePath();
		int collectYear = Arrays.asList(EntryPoints.SUMMER).contains(competition) ? EntryPoints.SUMMERCURRENT
				: EntryPoints.CURRENT;

		FileInputStream file;
		try {
			file = new FileInputStream(new File(base + "\\data\\odds" + collectYear + ".xls"));
			HSSFWorkbook workbook = new HSSFWorkbook(file);
			HSSFSheet sh = workbook.getSheet(competition);
		} catch (Exception e) {
			System.out.println("Something was wrong");
		} finally {
			controlFileFinally(file);
		}

		ArrayList<ExtendedFixture> all = sh == null ? new ArrayList<>() : XlSUtils.selectAll(sh, 0);
		// problem when no pendingma fixtures?
		Date oldestTocheck = Utils.findLastPendingFixture(all);
		System.out.println(oldestTocheck);

		ArrayList<ExtendedFixture> odds = oddsUpToDate(competition, collectYear, Utils.getYesterday(oldestTocheck),
				null);
		System.out.println(odds.size() + " odds ");

		ArrayList<ExtendedFixture> list = new ArrayList<>();
		controlList(list,competition, collectYear, oldestTocheck);

		System.out.println(list.size() + "shots");

		HashMap<String, String> dictionary = XlSUtils.deduceDictionary(odds, list);

		ArrayList<ExtendedFixture> combined = XlSUtils.combineWithDictionary(odds, list, competition, dictionary);
		System.out.println(combined.size() + " combined");
		System.out.println(
				competition + " " + (combined.size() == list.size() ? " combined successfull" : " combined failed"));
		workbook.close();

		ArrayList<ExtendedFixture> toAdd = new ArrayList<>();

		toAdd.addAll(combined);

		controlFirstDoubleFor(all, combined, toAdd);

		System.out.println("to add " + toAdd.size());
		ArrayList<ExtendedFixture> next = new ArrayList<>();

		next = nextMatches(competition, null, onlyTodaysMatches);

		ArrayList<ExtendedFixture> withNext = new ArrayList<>();

		controlSecondDoubleFor(toAdd, next, withNext);

		withNext.addAll(next);

		if (withNext.size() >= all.size()) {
			XlSUtils.storeInExcel(withNext, competition, CURRENT_YEAR, "odds");
			XlSUtils.fillMissingShotsData(competition, CURRENT_YEAR, false);
		}

		System.out.println(competition + " successfully updated");

	}
	
	private static ArrayList<ExtendedFixture> oddsUpToDate(String competition, int currentYear, Date yesterday,
			String add) throws InterruptedException {
		String address;
		if (add == null) {
			address = EntryPoints.getOddsLink(competition, currentYear);
		} else
			address = add;
		System.out.println(address);

		Set<ExtendedFixture> result = new HashSet<>();

		System.setProperty("webdriver.chrome.drive", "chromedriver.exe");
		WebDriver driver = new ChromeDriver();
		driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
		driver.manage().window().maximize();
		driver.navigate().to(address + "/results/");

		login(driver);

		driver.navigate().to(address + "/results/");

		// Get page count
		int maxPage = 1;
		List<WebElement> pages = driver.findElements(By.cssSelector("a[href*='#/page/']"));
		for (WebElement i : pages) {
			if (isNumeric(i.getText()) && Integer.parseInt(i.getText().trim()) > maxPage)
				maxPage = Integer.parseInt(i.getText().trim());
		}

		controlOddsUpToDate(driver, address, competition, yesterday, result, maxPage);
		
			if (breakFlag) {
				page = maxPage + 1;
				break;
			}
		
		driver.close();

		ArrayList<ExtendedFixture> fin = new ArrayList<>();
		fin.addAll(result);
		return fin;
	}

	private static ArrayList<ExtendedFixture> nextMatches(String competition, Object object,
			OnlyTodayMatches onlyTodaysMatches) throws ParseException, InterruptedException, IOException {
		String address = EntryPoints.getOddsLink(competition, EntryPoints.CURRENT);
		System.out.println(address);

		ArrayList<ExtendedFixture> result = new ArrayList<>();
		Set<String> teams = new HashSet<>();

		System.setProperty("webdriver.chrome.drive", "chromedriver.exe");
		WebDriver driver = new ChromeDriver();
		driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
		driver.manage().window().maximize();
		driver.navigate().to(address);

		login(driver);

		driver.navigate().to(address);

		String[] splitAddress = address.split("/");
		String leagueYear = splitAddress[splitAddress.length - 1];
		List<WebElement> list = driver.findElements(By.cssSelector("a[href*='" + leagueYear + "']"));
		ArrayList<String> links = new ArrayList<>();
		HashMap<String, String> texts = new HashMap<>();
		for (WebElement i : list) {
			if (i.getText().contains("-")) {
				String href = i.getAttribute("href");
				links.add(href);
				texts.put(href, i.getText());
			}
		}

		for (String i : links) {
			String homeTeam = texts.get(i).split("-")[0].trim();
			String awayTeam = texts.get(i).split("-")[1].trim();
			if (teams.contains(homeTeam) && teams.contains(awayTeam))
				continue;

			ExtendedFixture ef = getOddsFixture(driver, i, competition, true, onlyTodaysMatches);
			if (ef != null && ef.result.goalsHomeTeam == -1 && !teams.contains(ef.homeTeam)
					&& !teams.contains(ef.awayTeam)) {
				result.add(ef);
				teams.add(ef.awayTeam);
				teams.add(ef.homeTeam);
			}
		}
		driver.close();

		System.out.println(result);
		return result;
	}

	public static ArrayList<ExtendedFixture> collect(String competition, int year, String add)
			throws IOException, ParseException, InterruptedException {
		ArrayList<ExtendedFixture> result = new ArrayList<>();
		Set<ExtendedFixture> set = new HashSet<>();
		String address;
		if (add == null) {
			address = EntryPoints.getLink(competition, year);
			System.out.println(address);
		} else
			address = add;

		System.setProperty("webdriver.chrome.drive", "chromedriver.exe");
		WebDriver driver = new ChromeDriver();
		driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
		driver.manage().window().maximize();
		driver.navigate().to(address);

		while (true) {
			String html = driver.getPageSource();
			Document matches = Jsoup.parse(html);
			int setSize = set.size();
			Element list = matches.select("table[class=matches   ]").first();
			Elements linksM = list.select("a[href]");
			for (Element linkM : linksM) {
				if (isScore(linkM.text())) {
					Document fixture = Jsoup.connect(BASE + linkM.attr("href")).get();
					ExtendedFixture ef = getFixture(fixture, competition);
					result.add(ef);
					set.add(ef);
				}
			}

			Actions actions = createAction(driver);
			actions.moveToElement(driver.findElement(By.className("previous"))).click().perform();
			Thread.sleep(1000);
			String htmlAfter = driver.getPageSource();

			if (html.equals(htmlAfter))
				break;

		}

		driver.close();
		System.out.println(result.size());
		System.out.println(set.size());

		ArrayList<ExtendedFixture> setlist = new ArrayList<>();
		set.addAll(result);
		setlist.addAll(set);
		return setlist;

	}

	public static ArrayList<PlayerFixture> collectFull(String competition, int year, String add)
			throws IOException, ParseException, InterruptedException {
		ArrayList<PlayerFixture> result = new ArrayList<>();
		Set<PlayerFixture> set = new HashSet<>();
		String address;
		if (add == null) {
			address = EntryPoints.getLink(competition, year);
			System.out.println(address);
		} else
			address = add;

		System.setProperty("webdriver.chrome.drive", "chromedriver.exe");
		WebDriver driver = new ChromeDriver();
		driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
		driver.manage().window().maximize();
		driver.navigate().to(address /* + "/matches/" */ );

		int fixtureCount = 0;
		while (true) {
			String html = driver.getPageSource();
			Document matches = Jsoup.parse(html);

			Elements linksM = matches.select("a[href]");
			
			ScaperControls.controlForCollectFull(fixtureCount, result, linksM);
			
			}

			driver.findElement(By.className("previous")).click();
			Thread.sleep(1000);
			String htmlAfter = driver.getPageSource();

			if (html.equals(htmlAfter))
				break;

		driver.close();
		System.out.println(fixtureCount + " fixtures");
		System.out.println(result.size());

		ArrayList<PlayerFixture> setlist = new ArrayList<>();
		setlist = Utils.removeRepeats(result);
		System.out.println(setlist.size());
		return setlist;

	}

	public static ExtendedFixture getFixture(Document fixture, String competition) throws IOException, ParseException {

		Result ht = new Result(-1, -1);
		try {
			ht = getResult(fixture.select("dt:contains(Half-time) + dd").first().text());
		} catch (Exception e) {
			System.out.println("No ht result!");
		}
		Result result = new Result(-1, -1);
		try {
			result = getResult(fixture.select("dt:contains(Full-time) + dd").first().text());
		} catch (Exception e) {
			System.out.println("No full time result!");
			return null;
		}
		int matchday = -1;
		try {
			matchday = Integer.parseInt(fixture.select("dt:contains(Game week) + dd").first().text());
		} catch (Exception e) {
			
		}

		String iframe;

		int shotsHome = -1, shotsAway = -1;

		Elements frames = fixture.select("iframe");
		try {
				controlForGetFixture(frames, shotsHome, shotsAway);
			} catch (Exception exp) {
					
				}
				break;

		System.out.println(shotsHome + " s " + shotsAway);

		String dateString = fixture.select("dt:contains(Date) + dd").first().text();
		String timeString = "21:00";
		try {
			timeString = fixture.select("dt:contains(Kick-off) + dd").first().text();

		} catch (Exception e) {
			System.out.println("DA");
		}
		// the time is in gmt+1
		long hour = 3600 * 1000;
		synchronized(FORMATFULL){
			Date date = new Date(FORMATFULL.parse(dateString + " " + timeString).getTime() + hour);
		}
		System.out.println(date);

		String teams = fixture.select("h1").first().text();
		String homeTeam = Utils.replaceNonAsciiWhitespace(getHome(teams));
		String awayTeam = Utils.replaceNonAsciiWhitespace(getAway(teams));

		ExtendedFixture ef = new ExtendedFixture(date, homeTeam, awayTeam, result, "BRA").withHTResult(ht)
				.withShots(shotsHome, shotsAway);
		if (matchday != -1)
			ef = ef.withMatchday(matchday);
		System.out.println(ef);

		return ef;
	}

	public static ArrayList<PlayerFixture> getFixtureFull(Document fixture, String competition)
			throws IOException, ParseException {
		boolean verbose = false;

		Result ht = new Result(-1, -1);
		try {
			ht = getResult(fixture.select("dt:contains(Half-time) + dd").first().text());
		} catch (Exception e) {
			System.out.println("No ht result!");
		}

		Result result = new Result(-1, -1);
		try {
			result = getResult(fixture.select("dt:contains(Full-time) + dd").first().text());
		} catch (Exception e) {
			System.out.println("No full time result!");
			return null;
		}

		int matchday = -1;
		try {
			matchday = Integer.parseInt(fixture.select("dt:contains(Game week) + dd").first().text());
		} catch (Exception e) {

		}

		synchronized(OPTAFORMAT){
			Date date = OPTAFORMAT.parse(fixture.select("dt:contains(Date) + dd").first().text());
		}

		String teams = fixture.select("h1").first().text();
		String homeTeam = Utils.replaceNonAsciiWhitespace(getHome(teams));
		String awayTeam = Utils.replaceNonAsciiWhitespace(getAway(teams));

		// Lineups
		// =====================================================================
		ExtendedFixture fix = new ExtendedFixture(date, homeTeam, awayTeam, result, competition);
		System.out.println(fix);

		ArrayList<PlayerFixture> playerFixtures = new ArrayList<>();

		Element divLineups = fixture.getElementsByClass("combined-lineups-container").first();
		if (divLineups != null) {
			Element tableHome = divLineups.select("div.container.left").first();
			Element tableAway = divLineups.select("div.container.right").first();

			Elements rowsHome = tableHome.select("table").first().select("tr");
			try {
				ScraperControls.controlFirstForGetFixtureFull(rowsHome, fix, homeTeam);
				} catch (Exception e) {
					System.out.println("Empty column when parsing startin 11");
					continue;
				}
			
			Elements rowsAway = tableAway.select("table").first().select("tr");
			
			ScraperControls.controlFirstForGetFixtureFull(rowsAway, fix, awayTeam);
		}

		// Substitutes
		// ==========================================================
		Elements divsPlayers = fixture.getElementsByClass("combined-lineups-container");
		Element divSubstitutes = divsPlayers.size() > 1 ? divsPlayers.get(1) : null;
		
		ScraperControls.controlSubstitutes(divSubstitutes, divsPlayers, homeTeam, awayTeam, fix, playerFixtures);

		// Goals and assists
		// ========================================================================================
		Element divGoals = fixture.select("div[id*=match_goals").first();
		ScraperControls.controlGoalAssist(verbose, divGoals);
		return playerFixtures;
	}

	public static ArrayList<ExtendedFixture> odds(String competition, int year, String add)
			throws IOException, ParseException, InterruptedException {

		String address;
		if (add == null) {
			address = EntryPoints.getOddsLink(competition, year);
		} else
			address = add;
		System.out.println(address);

		Set<ExtendedFixture> result = new HashSet<>();
		
		WebDriver driver = new /* HtmlUnitDriver(); */ ChromeDriver();
		driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
		driver.manage().window().maximize();
		driver.navigate().to(address + "/results/");

		login(driver);

		driver.navigate().to(address + "/results/");

		// Get page count
		int maxPage = 1;
		try {
			WebElement pagin = driver.findElement(By.xpath("//*[@id='pagination']"));
			List<WebElement> spans = pagin.findElements(By.tagName("span"));
			for (WebElement i : spans) {
				if (isNumeric(i.getText()) && Integer.parseInt(i.getText().trim()) > maxPage) {
						maxPage = Integer.parseInt(i.getText().trim());
				}
			}
		} catch (Exception e) {

		}

		try {
			 for (int page = 1; page <= maxPage; page++) {
				driver.navigate().to(address + "/results/#/page/" + page + "/");

				String[] splitAddress = address.split("/");
				String leagueYear = splitAddress[splitAddress.length - 1];
				List<WebElement> list = driver.findElements(By.cssSelector("a[href*='" + leagueYear + "']"));
				ArrayList<String> links = createArrayListString();
				
				ScraperControls.controlFirstForOdds(list, links);

				ScraperControls.controlSecondForOdds(links, result);
			}
		} catch (Exception e) {
				System.out.println("Something was wrong");
				page--;
				System.out.println("Starting over from page:" + page);
				driver.close();
				Thread.sleep(30000);
				driver = new ChromeDriver();
				driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
				driver.manage().window().maximize();

				driver.navigate().to(address + "/results/");
				login(driver);
				driver.navigate().to(address + "/results/");
			}

		driver.close();

		ArrayList<ExtendedFixture> fin = new ArrayList<>();
		fin.addAll(result);
		System.out.println(fin.size());
		return fin;
	}

	public static ArrayList<FullFixture> fullOdds(String competition, int year, String add)
			throws IOException, ParseException, InterruptedException {

		String address;
		if (add == null) {
			address = EntryPoints.getOddsLink(competition, year);
		} else
			address = add;
		System.out.println(address);

		Set<FullFixture> result = new HashSet<>();

		System.setProperty("webdriver.chrome.drive", "chromedriver.exe");
		WebDriver driver = new ChromeDriver();
		driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
		driver.manage().window().maximize();
		driver.navigate().to(address + "/results/");

		// login
		login(driver);
		driver.navigate().to(address + "/results/");

		// Get page count
		int maxPage = 1;
		try {
			WebElement pagin = driver.findElement(By.xpath("//*[@id='pagination']"));
			List<WebElement> spans = pagin.findElements(By.tagName("span"));
			for (WebElement i : spans) {
				if (isNumeric(i.getText()) && Integer.parseInt(i.getText().trim()) > maxPage) {
					maxPage = Integer.parseInt(i.getText().trim());
				}
			} 
		}catch (Exception e) {

		}

		try {
			 for (int page = 1; page <= maxPage; page++) {
				driver.navigate().to(address + "/results/#/page/" + page + "/");

				String[] splitAddress = address.split("/");
				String leagueYear = splitAddress[splitAddress.length - 1];
				WebElement table = driver.findElement(By.xpath("//div[@id='tournamentTable']"));
				List<WebElement> el = table.findElements(By.cssSelector("a[href*='" + leagueYear + "']"));
				ArrayList<String> links = createArrayListString();
				
				ScraperControls.controlFirstForFullOdds(el, links);

				ScraperControls.controlSecondForFullOdds(links, result);
			}
		} catch (Exception e) {
				System.out.println("Something was wrong");
				//e.printStackTrace();
				page--;
				System.out.println("Starting over from page:" + page);
				driver.close();
				Thread.sleep(5000);
				driver = new ChromeDriver();
				driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
				driver.manage().window().maximize();

				driver.navigate().to(address + "/results/");
				login(driver);
				driver.navigate().to(address + "/results/");
			}

		driver.close();

		ArrayList<FullFixture> fin = new ArrayList<>();
		fin.addAll(result);
		System.out.println(fin.size());
		return fin;
	}

	public static ArrayList<ExtendedFixture> oddsByPage(String competition, int year, String add, int page) {
		String address;
		if (add == null)
			address = EntryPoints.getOddsLink(competition, year);
		else
			address = add;

		Set<ExtendedFixture> result = new HashSet<>();

		System.setProperty("webdriver.chrome.drive", "chromedriver.exe");
		WebDriver driver = new ChromeDriver();
		driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
		driver.manage().window().maximize();

		try {
			 while (true) {
				driver.navigate().to(address + "/results/#/page/" + page + "/");

				String[] splitAddress = address.split("/");
				String leagueYear = splitAddress[splitAddress.length - 1];
				List<WebElement> list = driver.findElements(By.cssSelector("a[href*='" + leagueYear + "']"));
				ArrayList<String> links = createArrayListString();
				
				ScraperControls.controlFirstForOddsByPage(list, links);
				
				ScraperControls.controlSecondForOddsByPage(links, result);	
				break;
			} 
		} catch (Exception e) {
				System.out.println("Something was wrong");
				System.out.println("Starting over from page:" + page);
				driver.close();
				try {
					Thread.sleep(10000);
				} catch (Exception e2) {
					System.out.println("Something was wrong");
					System.out.println("Thread sleep problem");
				}
				driver = new ChromeDriver();
				driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
				driver.manage().window().maximize();
			}

		driver.close();

		ArrayList<ExtendedFixture> fin = new ArrayList<>();
		fin.addAll(result);
		System.out.println("Thread at page " + page + "finished successfuly with " + fin.size());
		return fin;

	}

	public static ArrayList<ExtendedFixture> oddsInParallel(String competition, int year, String add)
			throws InterruptedException, ExecutionException {
		ArrayList<ExtendedFixture> result = new ArrayList<>();

		String address;
		if (add == null) {
			address = EntryPoints.getOddsLink(competition, year);
			System.out.println(address);
		} else
			address = add;
		System.out.println(address);

		WebDriver driver = new ChromeDriver();
		driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
		driver.manage().window().maximize();
		driver.navigate().to(address + "/results/");

		// Get page count
		int maxPage = 1;
		try {
			WebElement pagin = driver.findElement(By.xpath("//*[@id='pagination']"));
			List<WebElement> spans = pagin.findElements(By.tagName("span"));
			for (WebElement i : spans) {
				if (isNumeric(i.getText()) && Integer.parseInt(i.getText().trim()) > maxPage) {
					maxPage = Integer.parseInt(i.getText().trim());
				}
			} 
		}catch (Exception e) {

		}

		driver.close();

		ExecutorService pool = Executors.newFixedThreadPool(2);
		ArrayList<Future<ArrayList<ExtendedFixture>>> threadArray = new ArrayList<Future<ArrayList<ExtendedFixture>>>();
		for (int i = 1; i <= maxPage; i++) {
			threadArray.add(pool.submit(new RunnerOdds(competition, year, add, i)));
		}

		for (Future<ArrayList<ExtendedFixture>> fd : threadArray) {
			result.addAll(fd.get());
		}

		pool.shutdown();

		System.out.println("Final odds size " + result.size());
		return result;
	}
	
	public static ArrayList<ExtendedFixture> fastOdds(String competition, int year, String add)
			throws IOException, ParseException, InterruptedException {

		String address;
		if (add == null) {
			address = EntryPoints.getOddsLink(competition, year);
			System.out.println(address);
		} else
			address = add;
		System.out.println(address);

		Set<ExtendedFixture> result = new HashSet<>();

		System.setProperty("webdriver.chrome.drive", "chromedriver.exe");
		WebDriver driver = new ChromeDriver();
		driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
		driver.manage().window().maximize();
		driver.navigate().to(address + "/results/");

		// Get page count
		int maxPage = 1;
		List<WebElement> pages = driver.findElements(By.cssSelector("a[href*='#/page/']"));
		
		ScraperControls.controlPages(pages, maxPage); 
		
		try {
			 for (int page = 1; page <= maxPage; page++) {
				driver.navigate().to(address + "/results/#/page/" + page + "/");

				String[] splitAddress = address.split("/");
				String leagueYear = splitAddress[splitAddress.length - 1];
				List<WebElement> list = driver.findElements(By.cssSelector("a[href*='" + leagueYear + "']"));
				ArrayList<String> links = createArrayListString();
				ScraperControls.controlListLinks(list, links);

				System.out.println(links);
				ScraperControls.controlLinks(links, driver, competition, result);
			} 
		} catch (Exception e) {
				System.out.println("Something was wrong");
				page--;
				System.out.println("Starting over from page:" + page);
				driver.close();
				Thread.sleep(30000);
				driver = new ChromeDriver();
				driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
				driver.manage().window().maximize();
			}

		driver.close();

		ArrayList<ExtendedFixture> fin = new ArrayList<>();
		fin.addAll(result);
		System.out.println(fin.size());
		return fin;
	}

	
}