package main;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.json.JSONException;

import constants.MinMaxOdds;
import entries.AsianEntry;
import entries.FinalEntry;
import results.Results;
import runner.Runner;
import runner.RunnerAggregateInterval;
import runner.RunnerAllLines;
import runner.RunnerAsian;
import runner.RunnerAsianFinals;
import runner.RunnerDraws;
import runner.RunnerFinals;
import runner.RunnerIntersect;
import runner.RunnerOptimals;
import settings.Settings;
import settings.SettingsAsian;
import utils.Utils;
import xls.AsianUtils;
import xls.XlSUtils;

/**
 * PJDCC - Summary for class responsabilities.
 *
 * @author fourplus <fourplus1718@gmail.com>
 * @since 1.0
 * @version 11 Changes done
 */
public class Test {

	public static void main(String[] args) throws JSONException, IOException, InterruptedException, ExecutionException {
		long start = System.currentTimeMillis();
		System.out.println((System.currentTimeMillis() - start) / 1000d + "sec");

	}
	
	private static ArrayList<FinalEntry> createFinalEntry(){
		return new ArrayList<FinalEntry>();
	}

	public static float simulationAllLines(int year, boolean parsedLeagues)
			throws InterruptedException, ExecutionException, IOException {
		String base = new File("").getAbsolutePath();

		try {
			FileInputStream file;
			if (!parsedLeagues)
				file = new FileInputStream(new File(base + "\\data\\all-euro-data-" + year + "-" + (year + 1) + ".xls"));
			else
				file = new FileInputStream(new File(base + "\\data\\fullodds" + year + ".xls"));
	
			HSSFWorkbook workbook = new HSSFWorkbook(file);
		} catch (Exception e) {
			System.out.println("Something was wrong");
		} finally {
			if (file != null) {
				try {
					file.close (); // OK
				} catch (java.io.IOException e3) {
					System.out.println("I/O Exception");
               }
			}
		}
		Iterator<Sheet> sheet = workbook.sheetIterator();
		float totalProfit = 0.0f;

		ExecutorService pool = Executors.newFixedThreadPool(3);
		ArrayList<Future<Float>> threadArray = new ArrayList<Future<Float>>();
		while (sheet.hasNext()) {
			HSSFSheet sh = (HSSFSheet) sheet.next();
			// if (!sh.getSheetName().equals("IT"))
			// continue;
			// if (!Arrays.asList(MinMaxOdds.FULL).contains(sh.getSheetName()))
			// continue;

			// if
			// (!Arrays.asList(MinMaxOdds.MANUAL).contains(sh.getSheetName()))
			// continue;

			threadArray.add(pool.submit(new RunnerAllLines(sh, year)));
		}

		for (Future<Float> fd : threadArray) {
			totalProfit += fd.get();
			// System.out.println("Total profit: " + String.format("%.2f",
			// totalProfit));
		}
		System.out.println("Total profit for season " + year + " is " + String.format("%.2f", totalProfit));
		workbook.close();
		pool.shutdown();
		return totalProfit;
	}

	public static float asian(int year, boolean parsedLeagues)
			throws IOException, InterruptedException, ExecutionException {
		String base = new File("").getAbsolutePath();
		try {
			FileInputStream file;
			if (!parsedLeagues)
				file = new FileInputStream(new File(base + "\\data\\all-euro-data-" + year + "-" + (year + 1) + ".xls"));
			else
				file = new FileInputStream(new File(base + "\\data\\odds" + year + ".xls"));
			HSSFWorkbook workbook = new HSSFWorkbook(file);
		} catch (Exception e) {
			System.out.println("Something was wrong");
		} finally {
			if (file != null) {
				try {
					file.close (); // OK
				} catch (java.io.IOException e3) {
					System.out.println("I/O Exception");
               }
			}
		}
		Iterator<Sheet> sheet = workbook.sheetIterator();
		float totalProfit = 0.0f;

		ExecutorService pool = Executors.newFixedThreadPool(3);
		ArrayList<Future<Float>> threadArray = new ArrayList<Future<Float>>();
		while (sheet.hasNext()) {
			HSSFSheet sh = (HSSFSheet) sheet.next();
			if (!sh.getSheetName().equals("SPA2"))
				continue;

			threadArray.add(pool.submit(new RunnerAsian(sh, year)));
		}

		for (Future<Float> fd : threadArray) {
			totalProfit += fd.get();

		}
		System.out.println("Total profit for season " + year + " is " + String.format("%.2f", totalProfit));
		workbook.close();
		pool.shutdown();
		return totalProfit;
	}

	public static float draws(int year, boolean b) throws IOException, InterruptedException, ExecutionException {
		String base = new File("").getAbsolutePath();
		try {
			FileInputStream file;
			if (!b)
				file = new FileInputStream(new File(base + "\\data\\all-euro-data-" + year + "-" + (year + 1) + ".xls"));
			else
				file = new FileInputStream(new File(base + "\\data\\odds" + year + ".xls"));
			HSSFWorkbook workbook = new HSSFWorkbook(file);
		} catch (Exception e) {
			System.out.println("Something was wrong");
		} finally {
			if (file != null) {
				try {
					file.close (); // OK
				} catch (java.io.IOException e3) {
					System.out.println("I/O Exception");
               }
			}
		}
		Iterator<Sheet> sheet = workbook.sheetIterator();
		float totalProfit = 0.0f;

		ExecutorService pool = Executors.newFixedThreadPool(3);
		ArrayList<Future<Float>> threadArray = new ArrayList<Future<Float>>();
		while (sheet.hasNext()) {
			HSSFSheet sh = (HSSFSheet) sheet.next();
			if (!sh.getSheetName().equals("BRA"))
				continue;
			// if (!Arrays.asList(MinMaxOdds.SHOTS).contains(sh.getSheetName()))
			// continue;

			threadArray.add(pool.submit(new RunnerDraws(sh, year)));
		}

		for (Future<Float> fd : threadArray) {
			totalProfit += fd.get();
			// System.out.println("Total profit: " + String.format("%.2f",
			// totalProfit));
		}
		System.out.println("Total profit for season " + year + " is " + String.format("%.2f", totalProfit));
		workbook.close();
		pool.shutdown();
		return totalProfit;
	}

	public static float asianFinals(int year) throws IOException, InterruptedException, ExecutionException {
		String base = new File("").getAbsolutePath();
		
		FileInputStream file;
		try {
			file = new FileInputStream(
					new File(base + "\\data\\all-euro-data-" + year + "-" + (year + 1) + ".xls"));
		} catch (Exception e) {
			System.out.println("Something was wrong");
		} finally {
			if (file != null) {
				try {
					file.close (); // OK
				} catch (java.io.IOException e3) {
					System.out.println("I/O Exception");
               }
			}
		}
		HSSFWorkbook workbook = new HSSFWorkbook(file);
		Iterator<Sheet> sheet = workbook.sheetIterator();
		ArrayList<AsianEntry> all = new ArrayList<>();
		float totalProfit = 0.0f;

		ExecutorService pool = Executors.newFixedThreadPool(3);
		ArrayList<Future<ArrayList<AsianEntry>>> threadArray = new ArrayList<Future<ArrayList<AsianEntry>>>();
		while (sheet.hasNext()) {
			HSSFSheet sh = (HSSFSheet) sheet.next();

			threadArray.add(pool.submit(new RunnerAsianFinals(sh, year)));
		}

		for (Future<ArrayList<AsianEntry>> fd : threadArray) {
			all.addAll(fd.get());
		}

		AsianUtils.analysis(all);
		
		workbook.close();
		pool.shutdown();
		return totalProfit;
	}
	
	private static String createString(){
		return new File("").getAbsolutePath();
	}
	
	private static FileInputStream createFileInputStream(String base,int year){
		FileInputStream file;
		try {
			file = new FileInputStream(new File(base + "\\data\\all-euro-data-" + year + "-" + (year + 1) + ".xls"));
		} catch (Exception e) {
			System.out.println("Something was wrong");
		}finally {
			if (file != null) {
				try {
					file.close (); // OK
				}catch (java.io.IOException e3) {
					System.out.println("I/O Exception");
				}
			}
		}
		return file;		
	}

	public static final void singleMethod() throws IOException, ParseException {

		float totalTotal = 0f;
		for (int year = 2005; year <= 2015; year++) {
			float total = 0f;
			String base = createString();
			FileInputStream file;
			try {
				file = createFileInputStream(base,year);
			} catch (Exception e) {
				System.out.println("Something was wrong");
			} finally {
				if (file != null) {
					try {
						file.close (); // OK
					} catch (java.io.IOException e3) {
						System.out.println("I/O Exception");
	               }
				}
			}
			HSSFWorkbook workbook = new HSSFWorkbook(file);
			Iterator<Sheet> sheet = workbook.sheetIterator();
			while (sheet.hasNext()) {
				HSSFSheet sh = (HSSFSheet) sheet.next();
				if (!Arrays.asList(MinMaxOdds.SHOTS).contains(sh.getSheetName()))
					continue;
				float profit = XlSUtils.singleMethod(sh, XlSUtils.selectAll(sh, 10), year);

				total += profit;
			}
			System.out.println("Total for " + year + ": " + total);
			workbook.close();
			totalTotal += total;
		}
		System.out.println("Avg is: " + totalTotal / 11);
	}

	public static void stored24() throws InterruptedException {
		int bestPeriod = 0;
		float bestProfit = Float.NEGATIVE_INFINITY;
		int period = 3;
		float total = 0f;
		int sizeTotal = 0;
		float totalStake = 0f;

		ArrayList<FinalEntry> all = new ArrayList<>();

		for (int i = 2005 + period; i <= 2014; i++) {
			float curr = 0f;
			int size = 0;
			float staked = 0f;
			for (String league : Results.LEAGUES) {
				if (!Arrays.asList(MinMaxOdds.DONT).contains(league)) {
					ArrayList<FinalEntry> list = XlSUtils.bestCot(league, i, period, "realdouble15");

					curr += Utils.getScaledProfit(list, 0f)[0];
					size += list.size();
					staked += Utils.getScaledProfit(list, 0f)[1];
					all.addAll(list);
				}
			}

			System.out.println(
					"For " + i + ": " + curr + "  yield: " + Results.format((curr / staked) * 100) + " from: " + size);
			total += curr;
			sizeTotal += size;
			totalStake += staked;

			if (curr > bestProfit) {
				bestProfit = curr;
				bestPeriod = i;
			}

		}

		System.out.println(
				"Total avg: " + total / (10 - period) + " avg yield: " + Results.format(100 * (total / totalStake)));
	}

	public static final void aggregateInterval() throws IOException, InterruptedException, ExecutionException {
		ArrayList<String> dont = new ArrayList<String>(Arrays.asList(MinMaxOdds.DONT));
		String base = new File("").getAbsolutePath();
		FileInputStream file;
		try {
			file = new FileInputStream(new File(base + "\\data\\all-euro-data-" + 2014 + "-" + 2015 + ".xls"));
		} catch (Exception e) {
			System.out.println("Something was wrong");
		} finally {
			if (file != null) {
				try {
					file.close (); // OK
				} catch (java.io.IOException e3) {
					System.out.println("I/O Exception");
               }
			}
		}

		ExecutorService pool = Executors.newFixedThreadPool(7);
		ArrayList<Future<Settings>> threadArray = new ArrayList<Future<Settings>>();
		HSSFWorkbook workbook = new HSSFWorkbook(file);
		Iterator<Sheet> sheet = workbook.sheetIterator();
		while (sheet.hasNext()) {
			HSSFSheet sh = (HSSFSheet) sheet.next();
			if (dont.contains(sh.getSheetName()))
				continue;
			threadArray.add(pool.submit(new RunnerAggregateInterval(2005, 2007, sh)));
		}

		for (Future<Settings> fd : threadArray)
			fd.get();

		workbook.close();
		pool.shutdown();
	}

	public static final void aggregate(int year, int n) throws IOException, InterruptedException, ExecutionException {
		ArrayList<String> dont = new ArrayList<String>(Arrays.asList(MinMaxOdds.DONT));
		String base = new File("").getAbsolutePath();
		FileInputStream file;
		try {
			file = new FileInputStream(
					new File(base + "\\data\\all-euro-data-" + year + "-" + (year + 1) + ".xls"));
		} catch (Exception e) {
			System.out.println("Something was wrong");
		} finally {
			if (file != null) {
				try {
					file.close (); // OK
				} catch (java.io.IOException e3) {
					System.out.println("I/O Exception");
               }
			}
		}

		ExecutorService pool = Executors.newFixedThreadPool(3);
		ArrayList<Future<Settings>> threadArray = new ArrayList<Future<Settings>>();
		HSSFWorkbook workbook = new HSSFWorkbook(file);
		Iterator<Sheet> sheet = workbook.sheetIterator();
		while (sheet.hasNext()) {
			HSSFSheet sh = (HSSFSheet) sheet.next();
			
			threadArray.add(pool.submit(new RunnerAggregateInterval(year - n, year - 
					1, sh)));
		}

		HashMap<String, Settings> optimals = new HashMap<>();

		for (Future<Settings> fd : threadArray) {
			Settings result = fd.get();
			optimals.put(result.league, result);
			SQLiteJDBC.storeSettings(result, year, n);
		}

		workbook.close();
		pool.shutdown();
	}

	public static void stats() throws IOException, ParseException {
		for (int year = 2005; year <= 2015; year++) {
			String base = createString();

			FileInputStream file;
			try {
				file = createFileInputStream(base, year);
			} catch (Exception e) {
				System.out.println("Something was wrong");
			} finally {
				if (file != null) {
					try {
						file.close (); // OK
					} catch (java.io.IOException e3) {
						System.out.println("I/O Exception");
	               }
				}
			}
			HSSFWorkbook workbook = new HSSFWorkbook(file);
			HSSFSheet sheet = workbook.getSheet("E0");
			ArrayList<ExtendedFixture> all = XlSUtils.selectAllAll(sheet);
			System.out.println(year + " over: " + Utils.countOverGamesPercent(all) + "% AVG: " + Utils.findAvg(all));
			System.out.println("Overs when draw: " + Utils.countOversWhenDraw(all));
			System.out.println("Overs when win/loss: " + Utils.countOversWhenNotDraw(all));
			Utils.byWeekDay(all);
			System.out.println();
			workbook.close();
		}
	}

	public static float simulation(int year, DataType alleurodata)
			throws InterruptedException, ExecutionException, IOException {
		String base = new File("").getAbsolutePath();

		try {
			FileInputStream file;
			if (alleurodata.equals(DataType.ALLEURODATA))
				file = new FileInputStream(new File(base + "\\data\\all-euro-data-" + year + "-" + (year + 1) + ".xls"));
			else
				file = new FileInputStream(new File(base + "\\data\\odds" + year + ".xls"));
	
			HSSFWorkbook workbook = new HSSFWorkbook(file);
		} catch (Exception e) {
			System.out.println("Something was wrong");
		} finally {
			if (file != null) {
				try {
					file.close (); // OK
				} catch (java.io.IOException e3) {
					System.out.println("I/O Exception");
               }
			}
		}
		Iterator<Sheet> sheet = workbook.sheetIterator();
		float totalProfit = 0.0f;

		ExecutorService pool = Executors.newFixedThreadPool(3);
		ArrayList<Future<Float>> threadArray = new ArrayList<Future<Float>>();
		while (sheet.hasNext()) {
			HSSFSheet sh = (HSSFSheet) sheet.next();

			if (!Arrays.asList(MinMaxOdds.SHOTS).contains(sh.getSheetName()))
				continue;

			threadArray.add(pool.submit(new Runner(sh, year)));
		}

		for (Future<Float> fd : threadArray) {
			totalProfit += fd.get();
		}
		System.out.println("Total profit for season " + year + " is " + String.format("%.2f", totalProfit));
		workbook.close();
		pool.shutdown();
		return totalProfit;
	}

	public static ArrayList<FinalEntry> finals(int year, DataType type)
			throws InterruptedException, ExecutionException, IOException {
		String base = new File("").getAbsolutePath();
		ArrayList<String> dont = new ArrayList<String>(Arrays.asList(MinMaxOdds.DONT));
		ArrayList<String> draw = new ArrayList<String>(Arrays.asList(MinMaxOdds.DRAW));
		
		try {
			FileInputStream file;
			if (type.equals(DataType.ALLEURODATA))
				file = new FileInputStream(new File(base + "\\data\\all-euro-data-" + year + "-" + (year + 1) + ".xls"));
			else
				file = new FileInputStream(new File(base + "\\data\\odds" + year + ".xls"));
			HSSFWorkbook workbook = new HSSFWorkbook(file);
		} catch (Exception e) {
			System.out.println("Something was wrong");
		} finally {
			if (file != null) {
				try {
					file.close (); // OK
				} catch (java.io.IOException e3) {
					System.out.println("I/O Exception");
               }
			}
		}
		Iterator<Sheet> sheet = workbook.sheetIterator();
		ArrayList<FinalEntry> all = new ArrayList<>();

		ExecutorService pool = Executors.newFixedThreadPool(3);
		ArrayList<Future<ArrayList<FinalEntry>>> threadArray = new ArrayList<Future<ArrayList<FinalEntry>>>();
		while (sheet.hasNext()) {
			HSSFSheet sh = (HSSFSheet) sheet.next();
			
			threadArray.add(pool.submit(new RunnerFinals(sh, year)));
		}

		for (Future<ArrayList<FinalEntry>> fd : threadArray) {
			all.addAll(fd.get());
		}

		workbook.close();
		pool.shutdown();


		float[] profits = new float[8];

		return all;

	}

	public static float simulationIntersect(int year) throws InterruptedException, ExecutionException, IOException {
		String base = new File("").getAbsolutePath();

		FileInputStream file;
		try {
			file = new FileInputStream(
					new File(base + "\\data\\all-euro-data-" + year + "-" + (year + 1) + ".xls"));
			HSSFWorkbook workbook = new HSSFWorkbook(file);
		} catch (Exception e) {
			System.out.println("Something was wrong");
		} finally {
			if (file != null) {
				try {
					file.close (); // OK
				} catch (java.io.IOException e3) {
					System.out.println("I/O Exception");
               }
			}
		}
		
		Iterator<Sheet> sheet = workbook.sheetIterator();
		float totalProfit = 0.0f;

		ExecutorService pool = Executors.newFixedThreadPool(3);
		ArrayList<Future<Float>> threadArray = new ArrayList<Future<Float>>();
		while (sheet.hasNext()) {
			HSSFSheet sh = (HSSFSheet) sheet.next();
			threadArray.add(pool.submit(new RunnerIntersect(sh, year)));
		}

		for (Future<Float> fd : threadArray) {
			totalProfit += fd.get();
		}
		System.out.println("Total profit for season " + year + " is " + String.format("%.2f", totalProfit));
		workbook.close();
		pool.shutdown();
		return totalProfit;
	}
	
	private static ArrayList<Future<Float>> createFutureFloat(){
		new ArrayList<Future<Float>>();
	}

	public static void optimals() throws IOException, InterruptedException, ExecutionException {
		String basePath = new File("").getAbsolutePath();
		float totalTotal = 0f;

		for (int year = 2015; year <= 2015; year++) {
			float total = 0f;
			ExecutorService pool = Executors.newFixedThreadPool(1);
			ArrayList<Future<Float>> threadArray = createFutureFloat();
			FileInputStream filedata;
			try {
				filedata = createFileInputStream(basePath, year);
				HSSFWorkbook workbookdata = new HSSFWorkbook(filedata);
			} catch (Exception e) {
				System.out.println("Something was wrong");
			} finally {
				if (filedata != null) {
					try {
						filedata.close (); // OK
					} catch (java.io.IOException e3) {
						System.out.println("I/O Exception");
	               }
				}
			}
			
			Iterator<Sheet> sh = workbookdata.sheetIterator();
			while (sh.hasNext()) {
				HSSFSheet i = (HSSFSheet) sh.next();
				// if (i.getSheetName().equals("SP2"))
				threadArray.add(pool.submit(new RunnerOptimals(i, year)));
			}

			for (Future<Float> fd : threadArray) {
				total += fd.get();
			}

			System.out.println("Total profit for " + year + " is: " + total);

			totalTotal += total;
			workbookdata.close();
			pool.shutdown();
		}
		System.out.println("Average is:" + totalTotal / 11);
	}

	private static void controlFirstOptimalsByCompetition(HSSFWorkbook workbookdata, int year,
			HashMap<String, ArrayList<Settings>> optimals) {
		Iterator<Sheet> sh = workbookdata.sheetIterator();
		while (sh.hasNext()) {
			HSSFSheet i = (HSSFSheet) sh.next();
			Settings set = XlSUtils.predictionSettings(i, year);
			if (optimals.get(i.getSheetName()) != null)
				optimals.get(i.getSheetName()).add(set);
			else {
				optimals.put(i.getSheetName(), new ArrayList<>());
				optimals.get(i.getSheetName()).add(set);
			}
		}
	}
	
	private static void controlSecondOptimalsByCompetition(HSSFWorkbook workbookdata, int year,
			HashMap<String, ArrayList<Settings>> optimals, ArrayList<String> dont, float total) {
		Iterator<Sheet> sh = workbookdata.sheetIterator();
		while (sh.hasNext()) {
			HSSFSheet i = (HSSFSheet) sh.next();
			if (dont.contains(i.getSheetName()))
				continue;
			ArrayList<Settings> setts = optimals.get(i.getSheetName());
			Settings set = Utils.getSettings(setts, year - 1);
			ArrayList<FinalEntry> fes = XlSUtils.runWithSettingsList(i, XlSUtils.selectAllAll(i), set);
			float profit = Utils.getProfit(fes);
			total += profit;
		}
	}
	
	private static void controlIfOptimalsByCompetition(FileInputStream filedata) {
		if (filedata != null) {
			try {
				filedata.close (); // OK
			} catch (java.io.IOException e3) {
				System.out.println("I/O Exception");
	       }
		}
	}
	
	public static void optimalsbyCompetition() throws IOException, ParseException {

		HashMap<String, ArrayList<Settings>> optimals = new HashMap<>();
		String basePath = new File("").getAbsolutePath();

		for (int year = 2015; year <= 2015; year++) {
			FileInputStream filedata;
			try {
				filedata = createFileInputStream(basePath, year);
				HSSFWorkbook workbookdata = new HSSFWorkbook(filedata);
			} catch (Exception e) {
				System.out.println("Something was wrong");
			} finally {
				controlIfOptimalsByCompetition(filedata);
			}

			controlFirstOptimalsByCompetition(workbookdata, year, optimals);
			workbookdata.close();
		}
		
		float totalPeriod = 0f;

		ArrayList<String> dont = new ArrayList<String>(Arrays.asList(MinMaxOdds.DONT));

		for (int year = 2006; year <= 2015; year++) {
			total = 0f;
			FileInputStream filedata;
			try {
				filedata = createFileInputStream(basePath, year);
				HSSFWorkbook workbookdata = new HSSFWorkbook(filedata);
			} catch (Exception e) {
				System.out.println("Something was wrong");
			} finally {
				controlIfOptimalsByCompetition(filedata);
			}

			controlSecondOptimalsByCompetition(workbookdata, year, optimals, dont, total);
			
			totalPeriod += total;
			System.out.println("Total for " + year + " : " + total);
			workbookdata.close();
			filedata.close();
		}

		System.out.println("Avg profit per year using last year best setts: " + totalPeriod / 10);
	}

	public static void controlIfMakePredictions(HSSFSheet i, HashMap<String, Settings> optimal) {
		if (i.getSheetName().equals("SP2"))
			optimal.put(i.getSheetName(), XlSUtils.predictionSettings(i, 2015));
	}
	
	//Todo: da risolvere??
	public static void makePredictions() throws IOException, InterruptedException, ParseException {
		String basePath = new File("").getAbsolutePath();
		FileInputStream file;
		try {
			file = new FileInputStream(new File("fixtures.xls"));
			HSSFWorkbook workbook = new HSSFWorkbook(file);
		} catch (Exception e) {
			System.out.println("Something was wrong");
		} finally {
			controlIfOptimalsByCompetition(file);
		}
		
		HSSFSheet sheet = workbook.getSheetAt(0);
		ArrayList<ExtendedFixture> fixtures = XlSUtils.selectForPrediction(sheet);

		FileInputStream filedata;
		try {
			filedata = new FileInputStream(new File("all-euro-data-2015-2016.xls"));
			HSSFWorkbook workbookdata = new HSSFWorkbook(filedata);
		} catch (Exception e) {
			System.out.println("Something was wrong");
		} finally {
			controlIfOptimalsByCompetition(filedata);
		}

		HashMap<String, Settings> optimal = new HashMap<>();
		Iterator<Sheet> sh = workbookdata.sheetIterator();
		while (sh.hasNext()) {
			HSSFSheet i = (HSSFSheet) sh.next();
			controlIfMakePredictions(i, optimal);
			/**if (i.getSheetName().equals("SP2"))
				optimal.put(i.getSheetName(), XlSUtils.predictionSettings(i, 2015));*/
		}

		for (ExtendedFixture f : fixtures) {
			HSSFSheet league = workbookdata.getSheet(f.competition);
			XlSUtils.makePrediction(sheet, league, f, optimal.get(league.getSheetName()));
		}
		workbook.close();
		workbookdata.close();
	}

	//Todo: da risolvere
	public static void asianPredictions() throws IOException, InterruptedException, ParseException {
		String basePath = new File("").getAbsolutePath();
		FileInputStream file;
		try {
			file = new FileInputStream(new File("fixtures.xls"));
			HSSFWorkbook workbook = new HSSFWorkbook(file);
		} catch (Exception e) {
			System.out.println("Something was wrong");
		} finally {
			controlIfOptimalsByCompetition(file);
		}
		
		HSSFSheet sheet = workbook.getSheetAt(0);
		ArrayList<ExtendedFixture> fixtures = XlSUtils.selectForPrediction(sheet);

		FileInputStream filedata;
		try {
			filedata = new FileInputStream(new File("all-euro-data-2015-2016.xls"));
		} catch (Exception e) {
			System.out.println("Something was wrong");
		} finally {
			controlIfOptimalsByCompetition(filedata);
		}
		HSSFWorkbook workbookdata = new HSSFWorkbook(filedata);

		ArrayList<AsianEntry> all = new ArrayList<>();
		HashMap<String, SettingsAsian> optimal = new HashMap<>();
		Iterator<Sheet> sh = workbookdata.sheetIterator();
		while (sh.hasNext()) {
			HSSFSheet i = (HSSFSheet) sh.next();
			optimal.put(i.getSheetName(), XlSUtils.asianPredictionSettings(i, 2015));
		}

		for (ExtendedFixture f : fixtures) {
			HSSFSheet league = workbookdata.getSheet(f.competition);
			all.addAll(AsianUtils.makePrediction(sheet, league, f, optimal.get(league.getSheetName())));
		}

		all.sort(new Comparator<AsianEntry>() {

			@Override
			public int compare(AsianEntry o1, AsianEntry o2) {
				return ((Float) o2.expectancy).compareTo((Float) o1.expectancy);
			}
		});

		System.out.println(all);
		workbook.close();
		workbookdata.close();
	}

	public static void printSuccessRate(ArrayList<FinalEntry> list, String listName) {
		int successOver50 = 0, failureOver50 = 0;
		for (FinalEntry fe : list) {
			if (fe.success())
				successOver50++;
			else
				failureOver50++;
		}
		System.out.println("success" + listName + ": " + successOver50 + "failure" + listName + ": " + failureOver50);
		System.out
				.println("Rate" + listName + ": " + String.format("%.2f", ((float) successOver50 / list.size()) * 100));
		System.out.println("Profit" + listName + ": " + String.format("%.2f", successOver50 * 0.9 - failureOver50));
	}

	public static float basic1(ExtendedFixture f) {
		ArrayList<ExtendedFixture> lastHomeTeam = SQLiteJDBC.selectLastAll(f.homeTeam, 5, 2014, f.matchday,
				f.competition);
		ArrayList<ExtendedFixture> lastAwayTeam = SQLiteJDBC.selectLastAll(f.awayTeam, 5, 2014, f.matchday,
				f.competition);
		ArrayList<ExtendedFixture> lastHomeHomeTeam = SQLiteJDBC.selectLastHome(f.homeTeam, 5, 2014, f.matchday,
				f.competition);
		ArrayList<ExtendedFixture> lastAwayAwayTeam = SQLiteJDBC.selectLastAway(f.awayTeam, 5, 2014, f.matchday,
				f.competition);
		float allGamesAVG = (Utils.countOverGamesPercent(lastHomeTeam) + Utils.countOverGamesPercent(lastAwayTeam)) / 2;
		float homeAwayAVG = (Utils.countOverGamesPercent(lastHomeHomeTeam)
				+ Utils.countOverGamesPercent(lastAwayAwayTeam)) / 2;
		float BTSAVG = (Utils.countBTSPercent(lastHomeTeam) + Utils.countBTSPercent(lastAwayTeam)) / 2;

		return 0.4f * allGamesAVG + 0.4f * homeAwayAVG + 0.2f * BTSAVG;
	}

}