///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 11+
//JAVAC_OPTIONS -Xlint:unchecked
//DEPS com.fasterxml.jackson.core:jackson-databind:2.12.4
//DEPS com.fasterxml.jackson.dataformat:jackson-dataformat-csv:2.12.2
//DEPS org.apache.commons:commons-text:1.9
//DEPS org.knowm.xchart:xchart:3.8.0

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.markers.SeriesMarkers;

public class graphs {

    public static void main(String... args) throws IOException {

        CsvMapper csvMapper = new CsvMapper();
        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        List<Row> rows = csvMapper.readerFor(Row.class)
                .with(schema)
                .<Row>readValues(Paths.get("indicies-by-age-and-sex.csv").toFile()).readAll();

        List<String> periods = rows.stream().map(r -> r.period).distinct().collect(Collectors.toList());

        new File("./graphs").mkdirs();
        for (String period : periods) {
            List<Row> data = rows.stream().filter(r -> period.equals(r.period)).collect(Collectors.toList());

            makeGraphs(data,
                    String.format("./graphs/infected_female_amount_%s.png", period.replaceAll("[^a-zA-Z0-9]", "_")),
                    "Infected females for the period " + period, "Amount", r -> r.infected_female_amount);
            makeGraphs(data, String.format("./graphs/infected_female_percent_%s.png", period.replaceAll("[^a-zA-Z0-9]",
                            "_")),
                    "Infected females for the period " + period, "Percentage", r -> r.infected_female_percent);
            makeGraphs(data, String.format("./graphs/infected_male_amount_%s.png", period.replaceAll("[^a-zA-Z0-9]",
                            "_")),
                    "Infected males for the period " + period, "Amount", r -> r.infected_male_amount);
            makeGraphs(data, String.format("./graphs/infected_male_percent_%s.png", period.replaceAll("[^a-zA-Z0-9]",
                            "_")),
                    "Infected males for the period " + period, "Percentage", r -> r.infected_male_percent);

            makeGraphs(data,
                    String.format("./graphs/dead_female_amount_%s.png", period.replaceAll("[^a-zA-Z0-9]", "_")),
                    "Female deaths for the period " + period, "Amount", r -> r.dead_female_amount);
            makeGraphs(data,
                    String.format("./graphs/dead_female_percent_%s.png", period.replaceAll("[^a-zA-Z0-9]", "_")),
                    "Female deaths for the period " + period, "Percentage", r -> r.dead_female_percent);
            makeGraphs(data, String.format("./graphs/dead_male_amount_%s.png", period.replaceAll("[^a-zA-Z0-9]", "_")),
                    "Male deaths for the period " + period, "Amount", r -> r.dead_male_amount);
            makeGraphs(data, String.format("./graphs/dead_male_percent_%s.png", period.replaceAll("[^a-zA-Z0-9]", "_")),
                    "Male deaths for the period " + period, "Percentage", r -> r.dead_male_percent);

            makeGraphs(data, String.format("./graphs/breathe_female_amount_%s.png", period.replaceAll("[^a-zA-Z0-9]",
                            "_")),
                    "Females on ventilation for the period " + period, "Amount", r -> r.breathe_female_amount);
            makeGraphs(data, String.format("./graphs/breathe_female_percent_%s.png", period.replaceAll("[^a-zA-Z0-9]",
                            "_")),
                    "Females on ventilation for the period " + period, "Percentage", r -> r.breathe_female_percent);
            makeGraphs(data,
                    String.format("./graphs/breathe_male_amount_%s.png", period.replaceAll("[^a-zA-Z0-9]", "_")),
                    "Males on ventilation for the period " + period, "Amount", r -> r.breathe_male_amount);
            makeGraphs(data, String.format("./graphs/breathe_male_percent_%s.png", period.replaceAll("[^a-zA-Z0-9]",
                            "_")),
                    "Males on ventilation for the period " + period, "Percentage", r -> r.breathe_male_percent);

            makeGraphs(data, String.format("./graphs/severe_female_amount_%s.png", period.replaceAll("[^a-zA-Z0-9]",
                            "_")),
                    "Females classified severe for the period " + period, "Amount", r -> r.severe_female_amount);
            makeGraphs(data, String.format("./graphs/severe_female_percent_%s.png", period.replaceAll("[^a-zA-Z0-9]",
                            "_")),
                    "Females classified severe for the period " + period, "Percentage", r -> r.severe_female_percent);
            makeGraphs(data,
                    String.format("./graphs/severe_male_amount_%s.png", period.replaceAll("[^a-zA-Z0-9]", "_")),
                    "Males classified severe for the period " + period, "Amount", r -> r.severe_male_amount);
            makeGraphs(data,
                    String.format("./graphs/severe_male_percent_%s.png", period.replaceAll("[^a-zA-Z0-9]", "_")),
                    "Males classified severe for the period " + period, "Percentage", r -> r.severe_male_percent);


        }


    }

    private static void makeGraphs(List<Row> data, String filename, String title,
                                   String yAxisTitle, Function<Row, Number> extractor)
            throws IOException {
        XYChart chart = new XYChartBuilder()
                .width(1024)
                .height(768)
                .theme(Styler.ChartTheme.Matlab)
                .xAxisTitle("Date")
                .yAxisTitle(yAxisTitle)
                .title(title)
                .build();
        chart.getStyler().setDatePattern("dd-MMM-yyyy");
        List<String> ageGroups = data.stream().map(r -> r.age_group).distinct().collect(Collectors.toList());

        for (String ageGroup : ageGroups) {
            List<Date> times = new ArrayList<>(data.size() / ageGroups.size());
            List<Number> values = new ArrayList<>(data.size() / ageGroups.size());
            data.stream().filter(r -> ageGroup.equals(r.age_group)).forEach(
                    r -> {
                        times.add(new Date(OffsetDateTime.parse(r.timestamp).toInstant().toEpochMilli()));
                        values.add(extractor.apply(r));
                    }
            );
            chart.addSeries(ageGroup, times, values)
                    .setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line)
                    .setMarker(SeriesMarkers.NONE);
        }

        BitmapEncoder.saveBitmap(chart, filename, BitmapEncoder.BitmapFormat.PNG);
    }

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
    }
}
