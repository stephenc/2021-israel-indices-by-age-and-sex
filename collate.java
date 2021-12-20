///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 11+
//JAVAC_OPTIONS -Xlint:unchecked
//DEPS com.fasterxml.jackson.core:jackson-databind:2.12.4
//DEPS com.fasterxml.jackson.dataformat:jackson-dataformat-csv:2.12.2
//DEPS org.apache.commons:commons-text:1.9

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class collate {

    public static final TypeReference<List<DataSet>> dataSetsType = new TypeReference<List<DataSet>>() {
    };
    public static final TypeReference<List<Row>> outputType = new TypeReference<List<Row>>() {
    };

    public static final Map<String, String> decodePeriods = new HashMap<String, String>() {{
        this.put("3 \u05D7\u05D5\u05D3\u05E9\u05D9\u05DD", "last 3 months");
        this.put("6 \u05D7\u05D5\u05D3\u05E9\u05D9\u05DD", "last 6 months");
        this.put("\u05D7\u05D5\u05D3\u05E9 \u05D0\u05D7\u05E8\u05D5\u05DF", "last month");
        this.put("\u05DE\u05EA\u05D7\u05D9\u05DC\u05EA \u05E7\u05D5\u05E8\u05D5\u05E0\u05D4", "all time");
        this.put("\u05E9\u05E0\u05D4", "last year");
    }};

    public static void main(String... args) throws IOException {
        Pattern fileNamePattern = Pattern.compile("^scrape-(\\d{4}-\\d{2}-\\d{2})_(\\d{2}\\.\\d{2}\\.\\d{2})Z\\.json$");
        ObjectMapper mapper = new ObjectMapper();
        List<Row> table = new ArrayList<>();
        Files.list(Paths.get("data"))
                .sorted()
                .forEach(p -> {
                            Matcher matcher = fileNamePattern.matcher(p.getFileName().toString());
                            if (!matcher.matches()) {
                                return;
                            }
                            String date = matcher.group(1) + "T" + (matcher.group(2).replace('.', ':')) + 'Z';
                            Instant instant = Instant.parse(date);
                            List<DataSet> value;
                            try {
                                value = mapper.readValue(p.toFile(), dataSetsType);
                            } catch (IOException e) {
                                throw new UncheckedIOException("Could not parse " + p, e);
                            }
                            Optional<DataSet> infected = value.stream().filter(s -> "0".equals(s.id))
                                    .filter(hasData(p, "infected")).findFirst();
                            Optional<DataSet> dead = value.stream().filter(s -> "1".equals(s.id))
                                    .filter(hasData(p, "dead")).findFirst();
                            Optional<DataSet> breathe = value.stream().filter(s -> "2".equals(s.id))
                                    .filter(hasData(p, "breathe")).findFirst();
                            Optional<DataSet> severe = value.stream().filter(s -> "3".equals(s.id))
                                    .filter(hasData(p, "severe")).findFirst();
                            List<String> sections = value.stream()
                                    .filter(s -> s.data != null)
                                    .flatMap(s -> s.data.stream())
                                    .map(x -> x.section)
                                    .distinct()
                                    .sorted()
                                    .collect(Collectors.toList());
                            List<String> periods = value.stream()
                                    .filter(s -> s.data != null)
                                    .flatMap(s -> s.data.stream())
                                    .map(x -> x.period)
                                    .distinct()
                                    .sorted()
                                    .collect(Collectors.toList());
                            for (String section : sections) {
                                for (String period : periods) {
                                    Row row = new Row();
                                    row.timestamp = instant.toString();
                                    row.age_group = section;
                                    row.period = decodePeriods.getOrDefault(period, period);
                                    table.add(row);
                                    infected.flatMap(d -> d.data.stream()
                                            .filter(x -> x.section.equals(section) && x.period.equals(period))
                                            .findFirst()).ifPresent(r -> {
                                        if (r.female != null) {
                                            row.infected_female_amount = r.female.amount;
                                            row.infected_female_percent = r.female.percentage;
                                        }
                                        if (r.male != null) {
                                            row.infected_male_amount = r.male.amount;
                                            row.infected_male_percent = r.male.percentage;
                                        }
                                    });
                                    dead.flatMap(d -> d.data.stream()
                                            .filter(x -> x.section.equals(section) && x.period.equals(period))
                                            .findFirst()).ifPresent(r -> {
                                        if (r.female != null) {
                                            row.dead_female_amount = r.female.amount;
                                            row.dead_female_percent = r.female.percentage;
                                        }
                                        if (r.male != null) {
                                            row.dead_male_amount = r.male.amount;
                                            row.dead_male_percent = r.male.percentage;
                                        }
                                    });
                                    breathe.flatMap(d -> d.data.stream()
                                            .filter(x -> x.section.equals(section) && x.period.equals(period))
                                            .findFirst()).ifPresent(r -> {
                                        if (r.female != null) {
                                            row.breathe_female_amount = r.female.amount;
                                            row.breathe_female_percent = r.female.percentage;
                                        }
                                        if (r.male != null) {
                                            row.breathe_male_amount = r.male.amount;
                                            row.breathe_male_percent = r.male.percentage;
                                        }
                                    });
                                    severe.flatMap(d -> d.data.stream()
                                            .filter(x -> x.section.equals(section) && x.period.equals(period))
                                            .findFirst()).ifPresent(r -> {
                                        if (r.female != null) {
                                            row.severe_female_amount = r.female.amount;
                                            row.severe_female_percent = r.female.percentage;
                                        }
                                        if (r.male != null) {
                                            row.severe_male_amount = r.male.amount;
                                            row.severe_male_percent = r.male.percentage;
                                        }
                                    });
                                }
                            }
                        }
                );

        CsvMapper csvMapper = new CsvMapper();
        CsvSchema schema = csvMapper.schemaFor(Row.class).withHeader();
        csvMapper.writerFor(outputType)
                .with(schema)
                .writeValues(Paths.get("indicies-by-age-and-sex.csv").toFile())
                .write(table);
    }

    private static Predicate<DataSet> hasData(Path p, String set) {
        return d -> {
            if (d.data != null) {
                return true;
            }
            if (d.error != null) {
                System.err.printf("WARNING! %s is missing %s data, cause: %s: %s%n", p, set, d.error.name,
                        d.error.message);
            } else {
                System.err.printf("WARNING! %s is missing %s data, cause unknown%n", p, set);
            }
            return false;
        };
    }

    public static class DataSet {
        @JsonProperty
        public String id;
        @JsonProperty
        public List<DataPoint> data;
        @JsonProperty
        public DataError error;
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
    }

    public static class DataError {
        @JsonProperty
        public String message;
        @JsonProperty
        public String name;
    }

    @JsonPropertyOrder({
            "timestamp",
            "age_group",
            "period"
    })
    public static class Row {
        @JsonProperty
        public String timestamp;
        @JsonProperty
        public String age_group;
        @JsonProperty
        public String period;
        @JsonProperty
        public Long infected_female_amount;
        @JsonProperty
        public Double infected_female_percent;
        @JsonProperty
        public Long infected_male_amount;
        @JsonProperty
        public Double infected_male_percent;
        @JsonProperty
        public Long dead_female_amount;
        @JsonProperty
        public Double dead_female_percent;
        @JsonProperty
        public Long dead_male_amount;
        @JsonProperty
        public Double dead_male_percent;
        @JsonProperty
        public Long breathe_female_amount;
        @JsonProperty
        public Double breathe_female_percent;
        @JsonProperty
        public Long breathe_male_amount;
        @JsonProperty
        public Double breathe_male_percent;
        @JsonProperty
        public Long severe_female_amount;
        @JsonProperty
        public Double severe_female_percent;
        @JsonProperty
        public Long severe_male_amount;
        @JsonProperty
        public Double severe_male_percent;

        // These generated columns can be helpful for determining what the percentages originate from
//
//        @JsonProperty
//        public Long infected_female_guestimate_population() {
//            if (infected_female_percent != null && infected_female_percent > 0.1 && infected_female_amount != null
//                    && infected_female_amount > 10) {
//                return Math.round(infected_female_amount / infected_female_percent * 100);
//            }
//            return null;
//        }
//
//        @JsonProperty
//        public Long infected_female_guestimate_population_stderr() {
//            if (infected_female_percent != null && infected_female_percent > 0.1 && infected_female_amount != null
//                    && infected_female_amount > 10) {
//                return Math.round((infected_female_amount / (infected_female_percent - 0.05) * 100 -
//                        infected_female_amount / (infected_female_percent + 0.05) * 100)/2);
//            }
//            return null;
//        }
//
//        @JsonProperty
//        public Long dead_female_guestimate_population() {
//            if (dead_female_percent != null && dead_female_percent > 0.1 && dead_female_amount != null
//                    && dead_female_amount > 10) {
//                return Math.round(dead_female_amount / dead_female_percent * 100);
//            }
//            return null;
//        }
//
//        @JsonProperty
//        public Long dead_female_guestimate_population_stderr() {
//            if (dead_female_percent != null && dead_female_percent > 0.1 && dead_female_amount != null
//                    && dead_female_amount > 10) {
//                return Math.round((dead_female_amount / (dead_female_percent - 0.05) * 100 -
//                        dead_female_amount / (dead_female_percent + 0.05) * 100)/2);
//            }
//            return null;
//        }
//
//        @JsonProperty
//        public Long breathe_female_guestimate_population() {
//            if (breathe_female_percent != null && breathe_female_percent > 0.1 && breathe_female_amount != null
//                    && breathe_female_amount > 10) {
//                return Math.round(breathe_female_amount / breathe_female_percent * 100);
//            }
//            return null;
//        }
//
//        @JsonProperty
//        public Long breathe_female_guestimate_population_stderr() {
//            if (breathe_female_percent != null && breathe_female_percent > 0.1 && breathe_female_amount != null
//                    && breathe_female_amount > 10) {
//                return Math.round((breathe_female_amount / (breathe_female_percent - 0.05) * 100 -
//                        breathe_female_amount / (breathe_female_percent + 0.05) * 100) / 2);
//            }
//            return null;
//        }
//
//        @JsonProperty
//        public Long severe_female_guestimate_population() {
//            if (severe_female_percent != null && severe_female_percent > 0.1 && severe_female_amount != null
//                    && severe_female_amount > 10) {
//                return Math.round(severe_female_amount / severe_female_percent * 100);
//            }
//            return null;
//        }
//
//        @JsonProperty
//        public Long severe_female_guestimate_population_stderr() {
//            if (severe_female_percent != null && severe_female_percent > 0.1 && severe_female_amount != null
//                    && severe_female_amount > 10) {
//                return Math.round((severe_female_amount / (severe_female_percent - 0.05) * 100 -
//                        severe_female_amount / (severe_female_percent + 0.05) * 100) / 2);
//            }
//            return null;
//        }
//
//        @JsonProperty
//        public Long infected_male_guestimate_population() {
//            if (infected_male_percent != null && infected_male_percent > 0.1 && infected_male_amount != null
//                    && infected_male_amount > 10) {
//                return Math.round(infected_male_amount / infected_male_percent * 100);
//            }
//            return null;
//        }
//
//        @JsonProperty
//        public Long infected_male_guestimate_population_stderr() {
//            if (infected_male_percent != null && infected_male_percent > 0.1 && infected_male_amount != null
//                    && infected_male_amount > 10) {
//                return Math.round((infected_male_amount / (infected_male_percent - 0.05) * 100 -
//                        infected_male_amount / (infected_male_percent + 0.05) * 100)/2);
//            }
//            return null;
//        }
//
//        @JsonProperty
//        public Long dead_male_guestimate_population() {
//            if (dead_male_percent != null && dead_male_percent > 0.1 && dead_male_amount != null
//                    && dead_male_amount > 10) {
//                return Math.round(dead_male_amount / dead_male_percent * 100);
//            }
//            return null;
//        }
//
//        @JsonProperty
//        public Long dead_male_guestimate_population_stderr() {
//            if (dead_male_percent != null && dead_male_percent > 0.1 && dead_male_amount != null
//                    && dead_male_amount > 10) {
//                return Math.round((dead_male_amount / (dead_male_percent - 0.05) * 100 -
//                        dead_male_amount / (dead_male_percent + 0.05) * 100)/2);
//            }
//            return null;
//        }
//
//        @JsonProperty
//        public Long breathe_male_guestimate_population() {
//            if (breathe_male_percent != null && breathe_male_percent > 0.1 && breathe_male_amount != null
//                    && breathe_male_amount > 10) {
//                return Math.round(breathe_male_amount / breathe_male_percent * 100);
//            }
//            return null;
//        }
//
//        @JsonProperty
//        public Long breathe_male_guestimate_population_stderr() {
//            if (breathe_male_percent != null && breathe_male_percent > 0.1 && breathe_male_amount != null
//                    && breathe_male_amount > 10) {
//                return Math.round((breathe_male_amount / (breathe_male_percent - 0.05) * 100 -
//                        breathe_male_amount / (breathe_male_percent + 0.05) * 100) / 2);
//            }
//            return null;
//        }
//
//        @JsonProperty
//        public Long severe_male_guestimate_population() {
//            if (severe_male_percent != null && severe_male_percent > 0.1 && severe_male_amount != null
//                    && severe_male_amount > 10) {
//                return Math.round(severe_male_amount / severe_male_percent * 100);
//            }
//            return null;
//        }
//
//        @JsonProperty
//        public Long severe_male_guestimate_population_stderr() {
//            if (severe_male_percent != null && severe_male_percent > 0.1 && severe_male_amount != null
//                    && severe_male_amount > 10) {
//                return Math.round((severe_male_amount / (severe_male_percent - 0.05) * 100 -
//                        severe_male_amount / (severe_male_percent + 0.05) * 100) / 2);
//            }
//            return null;
//        }

    }
}
