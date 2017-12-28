package scraper;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * PJDCC - Summary for class responsabilities.
 *
 * @author fourplus <fourplus1718@gmail.com>
 * @since 1.0
 * @version 36 Changes done
 */

public class Scraper_2 {
	/**
	 * Gets fixture with odds data
	 * 
	 * @param driver
	 * @param i
	 *            - link to the fixture
	 * @param competition
	 * @param liveMatchesFlag
	 *            - flag for matches that are currently live - true in this case
	 * @param onlyToday
	 *            - true if we want to update only todays matches (to be played)
	 * @return
	 * @throws ParseException
	 * @throws InterruptedException
	 * @throws IOException
	 */
		
	public static ExtendedFixture getOddsFixture(WebDriver driver, String i, String competition,
			boolean liveMatchesFlag, OnlyTodayMatches onlyToday)
					throws ParseException, InterruptedException, IOException {
		driver.navigate().to(i);

		String title = driver.findElement(By.xpath("//*[@id='col-content']/h1")).getText();
		String home = title.split(" - ")[0].trim();
		String away = title.split(" - ")[1].trim();
		System.out.println(home + " : " + away);

		String dateString = driver.findElement(By.xpath("//*[@id='col-content']/p[1]")).getText();
		dateString = dateString.split(",")[1] + dateString.split(",")[2];
		synchronized(FORMATFULL){
			Date date = FORMATFULL.parse(dateString);
		}
		System.out.println(date);

		// skipping to update matches that are played later than today (for performance in nextMatches())
		if (onlyToday.equals(OnlyTodayMatches.TRUE) && !Utils.isToday(date))
			return null;

		// Resultss
		Result fullResult = new Result(-1, -1);
		Result htResult = new Result(-1, -1);
		try {
				WebElement resElement = driver.findElement(By.xpath("//*[@id='event-status']/p"));
				if(resElement != null){
					String resString = resElement.getText();
				}
				
				ScraperControls.controlOneResElement(driver, liveMatchesFlag);
				
				ScraperControls.controlTwoResElement(resElement, resString, fullResult, htResult);
				
				ScraperControls.controlIfElseNotNull(resElement, resString, fullResult, htResult);
		} catch (Exception e) {
			System.out.println("next match");
		}

		WebElement table = driver.findElement(By.xpath("//div[@id='odds-data-table']"));
		// find the row
		List<WebElement> customer = table.findElements(By.xpath("//div[1]/table/tbody/tr"));
		int pinnIndex = -2;
		int Index365 = -2;

		ScraperControls.controlForIfPinnIndex(customer, pinnIndex, Index365);

		float homeOdds = Float
				.parseFloat(table.findElement(By.xpath("//div[1]/table/tbody/tr[" + pinnIndex + "]/td[2]")).getText());
		float drawOdds = Float
				.parseFloat(table.findElement(By.xpath("//div[1]/table/tbody/tr[" + pinnIndex + "]/td[3]")).getText());
		float awayOdds = Float
				.parseFloat(table.findElement(By.xpath("//div[1]/table/tbody/tr[" + pinnIndex + "]/td[4]")).getText());
		
		// Over and under odds
		float overOdds = -1f, underOdds = -1f;
		float overOdds365 = -1f, underOdds365 = -1f;
		List<WebElement> tabs = driver.findElements(By.xpath("//*[@id='bettype-tabs']/ul/li"));
		try {
			ScraperControls.controlClick(tabs);
		} catch (Exception e) {
			System.out.println("click error o/u");
			System.out.println("Something was wrong");
		}

		WebElement div25 = null;
		List<WebElement> divs = driver.findElements(By.xpath("//*[@id='odds-data-table']/div"));
		try {
			ScraperControls.controlDiv(divs, div25, driver);
		} catch (Exception e) {
			System.out.println("click error o/u 2.5");
			System.out.println("Something was wrong");
		}

		ScraperControls.controlOUTable(div25, overOdds, underOdds, overOdds365, underOdds365);
		
		// Asian handicap
		try {
			ScraperControls.controlContainsAh(tabs, "AH");
		} catch (Exception e) {
			System.out.println("click error AH");
		}

		// Asian with closest line
		WebElement opt = null;
		float min = 100f;
		List<WebElement> divsAsian = driver.findElements(By.xpath("//*[@id='odds-data-table']/div"));
		
		ScraperControls.controlTryForIfGetOddsFixture(divsAsian, min, opt, home, away, opt, driver);

		ExtendedFixture ef = new ExtendedFixture(date, home, away, fullResult, competition).withHTResult(htResult)
				.with1X2Odds(homeOdds, drawOdds, awayOdds).withAsian(line, asianHome, asianAway)
				.withOdds(overOdds, underOdds, overOdds, underOdds).withShots(-1, -1);
		return ef;
	}

	public static FullFixture getFullFixtureTest(WebDriver driver, String i, String competition)
			throws ParseException, InterruptedException {
		driver.navigate().to(i);

		String title = driver.findElement(By.xpath("//*[@id='col-content']/h1")).getText();
		String home = title.split(" - ")[0].trim();
		String away = title.split(" - ")[1].trim();
		System.out.println(home + " : " + away);

		String dateString = driver.findElement(By.xpath("//*[@id='col-content']/p[1]")).getText();
		dateString = dateString.split(",")[1] + dateString.split(",")[2];
		synchronized(FORMATFULL){
			Date date = FORMATFULL.parse(dateString);
		}

		System.out.println(date);

		// Result
		Result fullResult = new Result(-1, -1);
		Result htResult = new Result(-1, -1);
		try {
			WebElement resElement = driver.findElement(By.xpath("//*[@id='event-status']/p"));
			String resString = resElement.getText();
			ScraperControls.controlResElement(resElement, resString, fullResult, htResult, home, away);
			
			if (resElement != null && (resString.contains("(") && resString.contains(")"))) {
				String full = resString.split(" ")[2];
				String half = resString.split(" ")[3].substring(1, 4);
				fullResult = new Result(Integer.parseInt(full.split(":")[0]), Integer.parseInt(full.split(":")[1]));
				htResult = new Result(Integer.parseInt(half.split(":")[0]), Integer.parseInt(half.split(":")[1]));
			} else {
				fullResult = new Result(-1, -1);
				htResult = new Result(-1, -1);
			}

		} catch (Exception e) {
			System.out.println("next match");
		}

		// match odds analysis over pinnacle
		fullMatchOddsOverPinnacle(driver);
		System.out.println("------------------------------------------------------");

		overUnderOverPinnacle(driver);
		System.out.println("------------------------------------------------------");

		asianOverPinnacle(driver);
		System.out.println("========================================================");
		return null;

	}

	static void asianOverPinnacle(WebDriver driver) {
		
		long start = System.currentTimeMillis();
		List<WebElement> tabs = driver.findElements(By.xpath("//*[@id='bettype-tabs']/ul/li"));
		ScraperControls.controlContainsAh(tabs, "AH");

		WebElement opt = null;
		float min = 100f;
		List<WebElement> divsAsian = driver.findElements(By.xpath("//*[@id='odds-data-table']/div"));
		try {
			for (WebElement div : divsAsian) {
				String text = div.getText();
				ScraperControls.controlText(text, min, opt, div);
			}
		} catch (Exception e) {
			// System.out.println("asian problem" + home + " " + away);
		}

		int indexOfOptimal = opt == null ? -1 : divsAsian.indexOf(opt);

		if (opt != null)

		{
			int lower = (indexOfOptimal - 5) < 0 ? 0 : (indexOfOptimal - 5);
			int higher = (indexOfOptimal + 5) > (divsAsian.size() - 1) ? (divsAsian.size() - 1) : (indexOfOptimal + 5);

			ScraperControls.controlTryForAsian(lower, higher, divsAsian, driver);
		}
		System.out.println("asian total time " + (System.currentTimeMillis() - start) / 1000d + "sec");
	}
	
	public static void overUnderOverPinnacle(WebDriver driver) {
		
		long start = System.currentTimeMillis();
		// Over and under odds
		List<WebElement> tabs = driver.findElements(By.xpath("//*[@id='bettype-tabs']/ul/li"));
		ScraperControls.controlContainsAh(tabs, "O/U");

		WebElement optGoals = null;
		float minGoals = 100f;
		List<WebElement> divsGoals = driver.findElements(By.xpath("//*[@id='odds-data-table']/div"));
		try {
			for (WebElement div : divsGoals) {
				ScraperControls.controlSplit(div, minGoals, optGoals);
			}
		} catch (Exception e) {
				// System.out.println("asian problem" + home + " " + away);
		}

		int indexOfOptimalGoals = optGoals == null ? -1 : divsGoals.indexOf(optGoals);

		if (optGoals == null)
			return;
		int lower = (indexOfOptimalGoals - 6) < 0 ? 0 : (indexOfOptimalGoals - 6);
		int higher = (indexOfOptimalGoals + 6) > (divsGoals.size() - 1) ? (divsGoals.size() - 1)
				: (indexOfOptimalGoals + 6);

		ScraperControls.controlBigForOverUnderOver(lower, higher, divsGoals, driver);

		System.out.println("over under total time " + (System.currentTimeMillis() - start) / 1000d + "sec");
	}

	public static FullFixture getFullFixture(WebDriver driver, String i, String competition)
			throws ParseException, InterruptedException {

		driver.navigate().to(i);

		String title = driver.findElement(By.xpath("//*[@id='col-content']/h1")).getText();
		String home = title.split(" - ")[0].trim();
		String away = title.split(" - ")[1].trim();
		System.out.println(home + " : " + away);

		String dateString = driver.findElement(By.xpath("//*[@id='col-content']/p[1]")).getText();
		synchronized(FORMATFULL){
			Date date = FORMATFULL.parse(dateString);
		}

		System.out.println(date);

		// Result
		Result fullResult = new Result(-1, -1);
		Result htResult = new Result(-1, -1);
		try {
			WebElement resElement = driver.findElement(By.xpath("//*[@id='event-status']/p"));
			String resString = resElement.getText();
			
			ScraperControls.controlResElement(resElement, resString, fullResult, htResult);
			
		} catch (Exception e) {
			System.out.println("next match");
		}

		WebElement table = driver.findElement(By.xpath("//div[@id='odds-data-table']"));
		List<WebElement> rows = table.findElements(By.xpath("//div[1]/table/tbody/tr"));
		Odds pinnOdds = null;

		ScraperControls.controlTripleFloat(rows, pinnOdds);

		// Over and under odds
		float overOdds = -1f, underOdds = -1f;
		List<WebElement> tabs = driver.findElements(By.xpath("//*[@id='bettype-tabs']/ul/li"));
		ScraperControls.controlContainsAh(tabs, "O/U");

		WebElement div25 = null;
		main.Line twoAndHalf = null;

		main.Line def = new Line(-1f, -1f, -1f, "Pinn");
		GoalLines GLS = new GoalLines(def, def, def, def, def);

		WebElement optGoals = null;
		float minGoals = 100f;
		List<WebElement> divsGoals = driver.findElements(By.xpath("//*[@id='odds-data-table']/div"));
		
		try {
			ScraperControls.controlForDiv25(divsGoals, div25, div, minGoals, optGoals);
		} catch (Exception e) {
			// System.out.println("asian problem" + home + " " + away);
		}

		int indexOfOptimalGoals = optGoals == null ? -1 : divsGoals.indexOf(optGoals); //QUESTO E' IF

		ArrayList<main.Line> goalLines  = new ArrayList<>();

		ScraperControls.controlBigIfGetFullFixture(optGoals, indexOfOptimalGoals, divsGoals, driver, twoAndHalf, goalLines,
				GLS, div25, overOdds, underOdds);

		// -----------------------------------------------------------------------
		// Asian handicap
		ScraperControls.controlContainsAh(tabs, "AH");

		// Asian with closest line
		AsianLines asianLines = new AsianLines(def, def, def, def, def);

		WebElement opt = null;
		float min = 100f;
		List<WebElement> divsAsian = driver.findElements(By.xpath("//*[@id='odds-data-table']/div"));
		try {
			for (WebElement div : divsAsian) {
				String text = div.getText();
				controlText(text, min, opt, div);
			}
		} catch (Exception e) {
			// System.out.println("asian problem" + home + " " + away);
		}

		int indexOfOptimal = opt == null ? -1 : divsAsian.indexOf(opt);

		ArrayList<main.Line> lines = new ArrayList<>();

		ScraperControls.controlFinalBigIf(opt, indexOfOptimal, divsAsian, driver, lines, asianLines);
		
		ExtendedFixture ef = new FullFixture(date, home, away, fullResult, competition).withHTResult(htResult)
				.with1X2Odds(-1f, -1f, -1f).withAsian(asianLines.main.line, asianLines.main.home, asianLines.main.away)
				.withOdds(overOdds, underOdds, overOdds, underOdds).withShots(-1, -1);

		return ((FullFixture) ef).withAsianLines(asianLines).withGoalLines(GLS);

	}

	public static ExtendedFixture getFastOddsFixture(WebDriver driver, String i, String competition)
			throws ParseException, IOException {
		driver.navigate().to(i);

		Document fixture = Jsoup.connect(i).get();

		String title = fixture.select("h1").first().text();

		String home = title.split(" - ")[0].trim();
		String away = title.split(" - ")[1].trim();
		System.out.println(home + " : " + away);

		Element dt = fixture.select("p[class^=date]").first();

		String millisString = dt.outerHtml().split(" ")[3].split("-")[0].substring(1);

		long timeInMillis = Long.parseLong(millisString) * 1000;

		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(timeInMillis);
		Date date = cal.getTime();
		System.out.println(date);

		// Result
		Result fullResult = new Result(-1, -1);
		Result htResult = new Result(-1, -1);
		try {

			Element resultJsoup = fixture.select("p[class^=result]").first();
			System.out.println(resultJsoup.text());

			String resString = resultJsoup.text();
			
			WebElement resElement = divsGoals.get(0); //aggiunto da tonio e mela, messo solo per usare metodo esistente, va bene qualsiasi valore != null
			ScraperControls.controlResElement(resElement, resString, fullResult, htResult, away, home);
			
			ScraperControls.controlIfElse(resString, fullResult, htResult);

		} catch (Exception e) {
			System.out.println("next match");
		}

		System.out.println(fullResult + " " + htResult);

		WebElement table = driver.findElement(By.xpath("//div[@id='odds-data-table']"));

		String winnerSuffix = "#1X2;2";
		fixture = Jsoup.connect(i + winnerSuffix).get();

		Elements tableJsoup = fixture.select("div#odds-data-table").select("div");
		System.out.println(fixture.text());

		Elements rowsJsoup = tableJsoup.select("tr");

		// find the row
		List<WebElement> customer = table.findElements(By.xpath("//div[1]/table/tbody/tr"));
		int pinnIndex = -2;
		int Index365 = -2;

		for (WebElement row : customer) {
			ScraperControls.controlPinnIndex(row, customer, pinnIndex);
		}
		
		if (pinnIndex < 0) {
			System.out.println("Could not find pinnacle");
			pinnIndex = Index365;
			pinnIndex = 2;
		}
		
		float homeOdds = Float
				.parseFloat(table.findElement(By.xpath("//div[1]/table/tbody/tr[" + pinnIndex + "]/td[2]")).getText());
		float drawOdds = Float
				.parseFloat(table.findElement(By.xpath("//div[1]/table/tbody/tr[" + pinnIndex + "]/td[3]")).getText());
		float awayOdds = Float
				.parseFloat(table.findElement(By.xpath("//div[1]/table/tbody/tr[" + pinnIndex + "]/td[4]")).getText());

		// Over and under odds
		float overOdds = -1f, underOdds = -1f;
		List<WebElement> tabs = driver.findElements(By.xpath("//*[@id='bettype-tabs']/ul/li"));
		ScraperControls.controlContainsAh(tabs, "O/U");

		WebElement div25 = null;
		List<WebElement> divs = driver.findElements(By.xpath("//*[@id='odds-data-table']/div"));
		ScraperControls.controlDiv25(divs, div25);

		WebElement OUTable = div25.findElement(By.xpath("//table"));

		// find the row
		List<WebElement> rows = OUTable.findElements(By.xpath("//tr"));

		controlfirstOdds(row, overOdds, underOdds);

		// Asian handicap
		ScraperControls.controlContainsAh(tabs, "AH");

		// Asian with closest line
		WebElement opt = null;
		float min = 100f;
		List<WebElement> divsAsian = driver.findElements(By.xpath("//*[@id='odds-data-table']/div"));
		try {
			for (WebElement div : divsAsian) {
				String text = div.getText();
				ScraperControls.controlText(text, min, opt, div);
			}
		} catch (Exception e) {
			System.out.println("asian problem" + home + " " + away);
		}

		float line = -1f, asianHome = -1f, asianAway = -1f;

		if (opt != null) {
			opt.click();

			WebElement AHTable = opt.findElement(By.xpath("//table"));

			// find the row
			List<WebElement> rowsAsian = AHTable.findElements(By.xpath("//tr"));

			ScraperControls.controlRow(rowsAsian, line, asianHome, asianAway);
		}

		ExtendedFixture ef = new ExtendedFixture(date, home, away, fullResult, competition).withHTResult(htResult)
				.with1X2Odds(homeOdds, drawOdds, awayOdds).withAsian(line, asianHome, asianAway)
				.withOdds(overOdds, underOdds, overOdds, underOdds).withShots(-1, -1);
		return ef;
	}

	static String getAway(String teams) {
		String[] split = teams.split(" vs. ");

		if (!split[1].contains("-"))
			return split[1];
		else {
			String[] splitAway = split[1].split(" ");
			String awayTeam = "";
			for (int j = 0; j < splitAway.length - 3; j++)
				awayTeam += splitAway[j] + " ";

			return awayTeam.trim();
		}
	}

	static String getHome(String teams) {
		String[] split = teams.split(" vs. ");
		return split[0].trim();
	}

	public static boolean isScore(String text) {
		String[] splitted = text.split("-");

		return splitted.length == 2 && isNumeric(splitted[0].trim()) && isNumeric(splitted[1].trim());
	}

	public static Result getResult(String text) {
		String[] splitted = text.split("-");

		return new Result(Integer.parseInt(splitted[0].trim()), Integer.parseInt(splitted[1].trim()));
	}

	public static boolean isNumeric(String str) {
		try {
			int d = Integer.parseInt(str);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}
	
	static void controlWhileCollectFull(int fixtureCount, ArrayList<PlayerFixture> result) {
		while (true) {
			Document fixture = Jsoup.connect(BASE + linkM.attr("href")).timeout(30 * 1000).get();
			ArrayList<PlayerFixture> ef = getFixtureFull(fixture, competition);
			fixtureCount++;
			result.addAll(ef);
			break;
			}
	}
}
