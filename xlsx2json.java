///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 11+
//JAVAC_OPTIONS -Xlint:unchecked
//DEPS com.fasterxml.jackson.core:jackson-databind:2.12.4
//DEPS org.apache.commons:commons-text:1.9
//DEPS org.apache.poi:poi-ooxml:5.0.0

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class xlsx2json {

    public static final String PERIOD = "\u05EA\u05E7\u05D5\u05E4\u05D4";

    public static final String AGE_GROUP = "\u05E7\u05D1\u05D5\u05E6\u05EA \u05D2\u05D9\u05DC";

    public static final String SEX = "\u05DE\u05D9\u05DF";

    public static final String INFECTED_AMOUNT = "\u05DE\u05E1\u05E4\u05E8 \u05DE\u05D0\u05D5\u05DE\u05EA\u05D9\u05DD";

    public static final String INFECTED_PERCENT = "\u05D0\u05D7\u05D5\u05D6 \u05DE\u05D0\u05D5\u05DE\u05EA\u05D9\u05DD";

    public static final String SEVERE_AMOUNT =
            "\u05DE\u05E1\u05E4\u05E8 \u05D7\u05D5\u05DC\u05D9\u05DD \u05E7\u05E9\u05D4 "
                    + "\u05D5\u05E7\u05E8\u05D9\u05D8\u05D9";

    public static final String SEVERE_PERCENT =
            "\u05D0\u05D7\u05D5\u05D6 \u05D7\u05D5\u05DC\u05D9\u05DD \u05E7\u05E9\u05D4 "
                    + "\u05D5\u05E7\u05E8\u05D9\u05D8\u05D9";

    public static final String BREATHE_AMOUNT = "\u05DE\u05E1\u05E4\u05E8 \u05DE\u05D5\u05E0\u05E9\u05DE\u05D9\u05DD";

    public static final String BREATHE_PERCENT = "\u05D0\u05D7\u05D5\u05D6 \u05DE\u05D5\u05E0\u05E9\u05DE\u05D9\u05DD";

    public static final String DEAD_AMOUNT = "\u05DE\u05E1\u05E4\u05E8 \u05E0\u05E4\u05D8\u05E8\u05D9\u05DD";

    public static final String DEAD_PERCENT = "\u05D0\u05D7\u05D5\u05D6 \u05E0\u05E4\u05D8\u05E8\u05D9\u05DD";

    public static final String SEX_FEMALE = "\u05E0\u05E9\u05D9\u05DD";

    public static final String SEX_MALE = "\u05D2\u05D1\u05E8\u05D9\u05DD";

    public static void main(String... args) throws Exception {
        Pattern fileNamePattern =
                Pattern.compile("^.*age\\s+(\\d+)[.](\\d+)[.](\\d+)\\s+\\((\\d+)(am|pm)\\).*xlsx$");
        Map<String, String> sectionFix = new HashMap<>();
        sectionFix.put("0-", "0-9");
        sectionFix.put("10", "10-19");
        sectionFix.put("20", "20-29");
        sectionFix.put("30", "30-39");
        sectionFix.put("40", "40-49");
        sectionFix.put("50", "50-59");
        sectionFix.put("60", "60-69");
        sectionFix.put("70", "70-79");
        sectionFix.put("80", "80-89");
        sectionFix.put("90", "90+");
        Set<String> sectionDrop = new HashSet<>();
        sectionDrop.add("10-11");
        sectionDrop.add("12-15");
        sectionDrop.add("16-19");
        sectionDrop.add("70-74");
        sectionDrop.add("75+");

        Files.list(Paths.get("manual")).forEach(path -> {
            String readFileName = path.toString();
            Instant scrapeTimestamp;
            Matcher matcher = fileNamePattern.matcher(readFileName);
            if (matcher.matches()) {
                String timestamp = String.format(
                        "20%02d-%02d-%02dT%02d:00:00Z",
                        Integer.parseInt(matcher.group(3)),
                        Integer.parseInt(matcher.group(2)),
                        Integer.parseInt(matcher.group(1)),
                        ("am".equals(matcher.group(5)) ? 0 : 12) + Integer.parseInt(matcher.group(4))
                );
                scrapeTimestamp = Instant.parse(timestamp);
            } else {
                System.err.printf("Cannot infer scrape timestamp from filename %s%n", readFileName);
                return;
            }
            System.out.printf("Reading %s...%n", readFileName);
            try {
                File myFile = new File(readFileName);
                FileInputStream fis = new FileInputStream(myFile);

                // Finds the workbook instance for XLSX file
                XSSFWorkbook myWorkBook = new XSSFWorkbook(fis);

                // Return first sheet from the XLSX workbook
                XSSFSheet mySheet = myWorkBook.getSheetAt(0);

                // Get iterator to all the rows in current sheet
                Iterator<Row> rowIterator = mySheet.iterator();

                // Traversing over each row of XLSX file
                int rowNum = 0;
                Integer periodCol = null;
                Integer sectionCol = null;
                Integer sexCol = null;
                Integer infectedAmountCol = null;
                Integer infectedPercentCol = null;
                Integer deadAmountCol = null;
                Integer deadPercentCol = null;
                Integer severeAmountCol = null;
                Integer severePercentCol = null;
                Integer breatheAmountCol = null;
                Integer breathePercentCol = null;
                DataSet infecteds = new DataSet();
                infecteds.id = "0";
                infecteds.data = new ArrayList<>();
                DataPoint infected = null;
                DataSet severes = new DataSet();
                severes.id = "3";
                severes.data = new ArrayList<>();
                DataPoint severe = null;
                DataSet deaths = new DataSet();
                deaths.id = "1";
                deaths.data = new ArrayList<>();
                DataPoint dead = null;
                DataSet breathes = new DataSet();
                breathes.id = "2";
                breathes.data = new ArrayList<>();
                DataPoint breathe = null;
                while (rowIterator.hasNext()) {
                    Row row = rowIterator.next();
                    switch (++rowNum) {
                        case 1:
                            // ignore
                            break;
                        case 2:
                            // figure out the headers
                            Iterator<Cell> cellIterator = row.cellIterator();
                            int colNum = 0;
                            while (cellIterator.hasNext()) {
                                Cell cell = cellIterator.next();
                                if (cell.getCellType() == CellType.STRING) {
                                    switch (cell.getStringCellValue()) {
                                        case PERIOD:
                                            periodCol = colNum;
                                            break;
                                        case AGE_GROUP:
                                            sectionCol = colNum;
                                            break;
                                        case SEX:
                                            sexCol = colNum;
                                            break;
                                        case INFECTED_AMOUNT:
                                            infectedAmountCol = colNum;
                                            break;
                                        case INFECTED_PERCENT:
                                            infectedPercentCol = colNum;
                                            break;
                                        case DEAD_AMOUNT:
                                            deadAmountCol = colNum;
                                            break;
                                        case DEAD_PERCENT:
                                            deadPercentCol = colNum;
                                            break;
                                        case SEVERE_AMOUNT:
                                            severeAmountCol = colNum;
                                            break;
                                        case SEVERE_PERCENT:
                                            severePercentCol = colNum;
                                            break;
                                        case BREATHE_AMOUNT:
                                            breatheAmountCol = colNum;
                                            break;
                                        case BREATHE_PERCENT:
                                            breathePercentCol = colNum;
                                            break;
                                    }
                                }
                                colNum++;
                            }
                            if (periodCol == null || sectionCol == null || sexCol == null) {
                                System.err.printf("Could not find key columns in row 2 of %s%n", readFileName);
                                return;
                            }
//                            System.out.printf("Period %d, section %d, sex %d, infected #%d %%%d, dead #%d %%%d, "
//                                            + "breathe #%d %%%d, severe #%d %%%d%n%n",
//                                    periodCol, sectionCol, sexCol, infectedAmountCol, infectedPercentCol,
//                                    deadAmountCol,
//                                    deadPercentCol, breatheAmountCol, breathePercentCol, severeAmountCol,
//                                    severePercentCol);
                            break;
                        default:
                            String period = row.getCell(periodCol).getStringCellValue();
                            String section = row.getCell(sectionCol).getStringCellValue();
                            section = sectionFix.getOrDefault(section, section);
                            if (sectionDrop.contains(section)) {
                                break;
                            }
                            String sex = row.getCell(sexCol).getStringCellValue();
                            boolean female = SEX_FEMALE.equals(sex);
                            if (infectedAmountCol != null && infectedPercentCol != null) {
                                if (infected == null || !period.equals(infected.period) || !section.equals(
                                        infected.section)) {
                                    infected = new DataPoint();
                                    infecteds.data.add(infected);
                                    infected.period = period;
                                    infected.section = section;
                                }
                                Cell ammountCell = row.getCell(infectedAmountCol);
                                Cell percentCell = row.getCell(infectedPercentCol);
                                if (ammountCell != null && percentCell != null) {
                                    DataValue value = new DataValue(
                                            ((Number) ammountCell.getNumericCellValue()).longValue(),
                                            percentCell.getNumericCellValue());
                                    if (female) {
                                        infected.female = value;
                                    } else {
                                        infected.male = value;
                                    }
                                }
                            }
                            if (deadAmountCol != null && deadPercentCol != null) {
                                if (dead == null || !period.equals(dead.period) || !section.equals(dead.section)) {
                                    dead = new DataPoint();
                                    deaths.data.add(dead);
                                    dead.period = period;
                                    dead.section = section;
                                }
                                Cell ammountCell = row.getCell(deadAmountCol);
                                Cell percentCell = row.getCell(deadPercentCol);
                                if (ammountCell != null && percentCell != null) {
                                    DataValue value = new DataValue(
                                            ((Number) ammountCell.getNumericCellValue()).longValue(),
                                            percentCell.getNumericCellValue());
                                    if (female) {
                                        dead.female = value;
                                    } else {
                                        dead.male = value;
                                    }
                                }
                            }
                            if (breatheAmountCol != null && breathePercentCol != null) {
                                if (breathe == null || !period.equals(breathe.period) || !section.equals(
                                        breathe.section)) {
                                    breathe = new DataPoint();
                                    breathes.data.add(breathe);
                                    breathe.period = period;
                                    breathe.section = section;
                                }
                                Cell ammountCell = row.getCell(breatheAmountCol);
                                Cell percentCell = row.getCell(breathePercentCol);
                                if (ammountCell != null && percentCell != null) {
                                    DataValue value = new DataValue(
                                            ((Number) ammountCell.getNumericCellValue()).longValue(),
                                            percentCell.getNumericCellValue());
                                    if (female) {
                                        breathe.female = value;
                                    } else {
                                        breathe.male = value;
                                    }
                                }
                            }
                            if (severeAmountCol != null && severePercentCol != null) {
                                if (severe == null || !period.equals(severe.period) || !section.equals(
                                        severe.section)) {
                                    severe = new DataPoint();
                                    severes.data.add(severe);
                                    severe.period = period;
                                    severe.section = section;
                                }
                                Cell ammountCell = row.getCell(severeAmountCol);
                                Cell percentCell = row.getCell(severePercentCol);
                                if (ammountCell != null && percentCell != null) {
                                    DataValue value = new DataValue(
                                            ((Number) ammountCell.getNumericCellValue()).longValue(),
                                            percentCell.getNumericCellValue());
                                    if (female) {
                                        severe.female = value;
                                    } else {
                                        severe.male = value;
                                    }
                                }
                            }
                    }
                }
                String outputFile =
                        "data/scrape-" + scrapeTimestamp.toString().replace('T', '_').replace(':', '.') + ".json";
                System.out.printf("Writing %s to %s%n", readFileName, outputFile);
                new ObjectMapper().writeValue(new File(outputFile),
                        Arrays.asList(infecteds, deaths, breathes, severes));
                System.out.println(infecteds.data.stream().map(x -> x.section).distinct().collect(Collectors.toList()));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public static class DataSet {
        @JsonProperty
        public String id;
        @JsonProperty
        public List<DataPoint> data;
    }

    public static class DataPoint {
        @JsonProperty
        public String section;
        @JsonProperty
        public String period;
        @JsonProperty
        public int order_period;
        @JsonProperty
        public DataValue female;
        @JsonProperty
        public DataValue male;

    }

    public static class DataValue {
        @JsonProperty
        public long amount;
        @JsonProperty
        public double percentage;

        public DataValue(long amount, double percentage) {
            this.amount = amount;
            this.percentage = percentage;
        }
    }
}
