package nl.verbraeck.smartmeter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.stream.Collectors;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import nl.verbraeck.smartmeter.chart.BarChart;
import nl.verbraeck.smartmeter.chart.LineChart;

/**
 * Main program to serve a request from the browser. The following requests are served:<br>
 * <ul>
 * <li><code>https://server.ip/</code> to request the overview page that is always for today.</li>
 * <li><code>https://server.ip/electricity</code> to request the electricity overview.</li>
 * <li><code>https://server.ip/gas</code> to request the gas overview.</li>
 * <li><code>https://server.ip/comparison</code> to request the comparison of this period with previous periods.</li>
 * <li>The suffix <code>?date=yyyy-mm-dd</code> indicates the date for which the page is shown. No date means today.</li>
 * </ul>
 * Most methods in this class are static, since we cannot keep state between re
 * <p>
 * Copyright (c) 2020-2023 Alexnder Verbraeck, Delft, the Netherlands. All rights reserved. <br>
 * MIT-license.
 * </p>
 * @author <a href="https://github.com/averbraeck">Alexander Verbraeck</a>
 */
public class SmartMeterWeb extends NanoHTTPD
{
    /**
     * Create a Web server on localhost using the given TCP port in Constants.
     * @throws IOException on error (e.g., port already in use)
     */
    public SmartMeterWeb() throws IOException
    {
        super(Constants.SERVER_PORT);
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        System.out.println("\nRunning! Point your browsers to " + Constants.SERVER_ADDRESS + "\n");
    }

    /** {@inheritDoc} */
    @Override
    public Response serve(final IHTTPSession session)
    {
        String uri = session.getUri();
        Map<String, String> parms = session.getParms();

        LocalDate date = LocalDate.now();
        if (parms.containsKey("date"))
        {
            try
            {
                System.out.println("tried to parse date: " + parms.get("date"));
                date = LocalDate.parse(parms.get("date"));
            }
            catch (DateTimeParseException e)
            {
                // ignore
            }
        }

        if (uri.endsWith(".js"))
        {
            Response response = newFixedLengthResponse(readTextFile(uri));
            response.setMimeType("text/javascript");
            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH).withZone(ZoneId.of("GMT"));
            response.addHeader("Expires", LocalDateTime.now(ZoneId.of("GMT")).plusMonths(1).format(formatter));
            response.addHeader("Cache-Control", "max-age=2592000, public");
            return response;
        }
        else if (uri.endsWith(".css"))
        {
            Response response = newFixedLengthResponse(readTextFile(uri));
            response.setMimeType("text/css");
            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH).withZone(ZoneId.of("GMT"));
            response.addHeader("Expires", LocalDateTime.now(ZoneId.of("GMT")).plusMonths(1).format(formatter));
            response.addHeader("Cache-Control", "max-age=2592000, public");
            return response;
        }
        else if (uri.endsWith(".map"))
        {
            Response response = newFixedLengthResponse(readTextFile(uri));
            DateTimeFormatter formatter =
                    DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH).withZone(ZoneId.of("GMT"));
            response.addHeader("Expires", LocalDateTime.now(ZoneId.of("GMT")).plusMonths(1).format(formatter));
            response.addHeader("Cache-Control", "max-age=2592000, public");
            return response;
        }
        else if (uri.equals("/favicon.ico"))
        {
            InputStream is = binaryStream("/favicon.ico");
            if (is != null)
            {
                try
                {
                    Response response = newFixedLengthResponse(Status.OK, "image/x-icon", is, -1);
                    DateTimeFormatter formatter =
                            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH).withZone(ZoneId.of("GMT"));
                    response.addHeader("Expires", LocalDateTime.now(ZoneId.of("GMT")).plusMonths(1).format(formatter));
                    response.addHeader("Cache-Control", "max-age=2592000, public");
                    return response;
                }
                catch (Exception e)
                {
                    System.err.println("Error reading /favicon.ico: " + e.getMessage());
                }
            }
        }
        if (uri.startsWith("/electricity"))
        {
            Response response = newFixedLengthResponse(electricity(date));
            response.addHeader("Cache-Control", "no-store");
            return response;
        }
        if (uri.startsWith("/gas"))
        {
            Response response = newFixedLengthResponse(gas(date));
            response.addHeader("Cache-Control", "no-store");
            return response;
        }
        if (uri.startsWith("/comparison"))
        {
            Response response = newFixedLengthResponse(comparison());
            response.addHeader("Cache-Control", "no-store");
            return response;
        }

        Response response = newFixedLengthResponse(overview());
        response.addHeader("Cache-Control", "no-store");
        return response;
    }

    /**
     * Return a binary InputStream for a file indicated by the URI.
     * @param uri String; the location of the file (absolute or relative path; resource; or inside jar file)
     * @return a binary InputStream for a file indicated by the URI
     */
    private static InputStream binaryStream(final String uri)
    {
        System.out.println("loaded binary file " + uri);
        try
        {
            URL url = URLResource.getResource(uri);
            if (url == null)
            {
                url = URLResource.getResource("/resources" + uri);
                if (url == null)
                {
                    System.err.println("Binary file " + uri + " not found");
                    return null;
                }
            }
            return url.openStream();
        }
        catch (IOException exception)
        {
            System.err.println("Binary file " + uri + " not found or not readable");
            return null;
        }
    }

    /**
     * Return the full content of a text file indicated by the URI.
     * @param uri String; the location of the file (absolute or relative path; resource; or inside jar file)
     * @return String; the full content of a text file indicated by the URI
     */
    private static String readTextFile(final String uri)
    {
        System.out.println("loaded text file " + uri);
        URL url = URLResource.getResource(uri);
        if (url == null)
        {
            url = URLResource.getResource("/resources" + uri);
            if (url == null)
            {
                System.err.println("File " + uri + " not found");
                return "";
            }
        }
        try
        {
            String text;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8)))
            {
                text = reader.lines().collect(Collectors.joining("\n"));
            }
            return text;
        }
        catch (IOException exception)
        {
            System.err.println("File " + uri + " not found or not readable");
            return "";
        }
    }

    /**
     * Return a string with 'today' or a nicely formatted date for the provided LocalDate argument.
     * @param date LocalDate; the date to provide the formatted date for
     * @return 'today' or a nicely formatted date for the provided LocalDate argument
     */
    private static String makeDateString(final LocalDate date)
    {
        return date.equals(LocalDate.now()) ? "today"
                : date.getDayOfWeek().name().substring(0, 1) + date.getDayOfWeek().name().substring(1, 2).toLowerCase() + " "
                + date.getDayOfMonth() + "-" + date.getMonthValue() + "-" + date.getYear();
    }

    /**
     * Return the HTML code for the date picker.
     * @param date LocalDate; the currently displayed date
     * @param urlPrefix String; prefix of the URL; e.g., '/electricity'
     * @return String; HTML code for the date picker
     */
    private static String datePicker(final LocalDate date, final String urlPrefix)
    {
        StringBuilder msg = new StringBuilder();
        try
        {
            msg.append("  <div class=\"row\">\n");
            msg.append("    <div class=\"col-md-6\">\n");
            msg.append("      <br>\n");
            msg.append("      Choose date: \n");
            msg.append("      <a href=\"" + urlPrefix + "?date=" + date.minusDays(30) + "\" target=\"_self\">");
            msg.append("-30 </a> \n");
            msg.append("      <a href=\"" + urlPrefix + "?date=" + date.minusDays(1) + "\" target=\"_self\">");
            msg.append("-7 </a> \n");
            msg.append("      <a href=\"" + urlPrefix + "?date=" + date.minusDays(1) + "\" target=\"_self\">");
            msg.append("-1 </a> \n");
            msg.append("      " + makeDateString(date) + "\n");
            msg.append("      <a href=\"" + urlPrefix + "?date=" + date.plusDays(1) + "\" target=\"_self\">");
            msg.append("+1 </a> \n");
            msg.append("      <a href=\"" + urlPrefix + "?date=" + date.plusDays(7) + "\" target=\"_self\">");
            msg.append("+7 </a> \n");
            msg.append("      <a href=\"" + urlPrefix + "?date=" + date.plusDays(30) + "\" target=\"_self\">");
            msg.append("+30 </a> \n");
            msg.append("      <a href=\"" + urlPrefix + "?date=" + LocalDate.now() + "\" target=\"_self\">");
            msg.append("Today</a> \n");
            msg.append("      <br>\n");
            msg.append("    </div>\n"); // col
            msg.append("    <div class=\"col-md-6\">\n");
            msg.append("    </div>\n"); // col
            msg.append("  </div>\n"); // row
        }
        catch (Exception e)
        {
            System.err.println("Error in datePicker: " + e.getMessage());
        }
        return msg.toString();
    }

    /**
     * Provide an overview page for today, with the general info, power usage, cumulative power usage, gas usage, and cumulative
     * gas usage for today. The overview page is ALWAYS for today (or the last day when results were registered).
     * @return String; complete HTML file with the page content
     */
    public static String overview()
    {
        System.out.println("loaded page /");

        String framework = readTextFile("/framework.html");
        framework = framework.replace("#1", "active").replace("#2", "").replace("#3", "").replace("#4", "");
        StringBuilder msg = new StringBuilder();

        try
        {
            Telegram lastTelegram = TelegramFile.getLastTelegram();
            msg.append("<div class=\"container-fluid\" style=\"margin-top:50px\">\n");
            msg.append("<div class=\"row\">\n");
            msg.append("<div class=\"col-md-6\">\n");
            msg.append("<h2>Overview</h2>\n");

            Table overviewTable = new Table();
            overviewTable.addRow("Electricity Tariff 1", lastTelegram.electricityTariff1kWh, "kWh");
            overviewTable.addRow("Electricity Tariff 2", lastTelegram.electricityTariff2kWh, "kWh");
            overviewTable.addRow("Tariff", lastTelegram.tariff == 1 ? "1 (low)" : "2 (high)");
            overviewTable.addRow("Power", lastTelegram.powerDeliveredkW, "kW");
            overviewTable.addRow("Voltage", lastTelegram.voltageL1, "V");
            double currentL1 = 1000.0 * lastTelegram.powerDeliveredkW / lastTelegram.voltageL1;
            overviewTable.addRow("Current", String.format("%.3f", currentL1), "A");
            overviewTable.addRow("Gas Delivered", lastTelegram.gasDeliveredM3, "m<sup>3</sup>");
            msg.append(overviewTable.table());
            msg.append("</div>\n"); // col

            msg.append("<div class=\"col-md-6\">\n");
            msg.append("<h2>Devices</h2>\n");
            Table deviceTable = new Table();
            deviceTable.addRow("Electricity Device id", lastTelegram.electricityMeterId);
            deviceTable.addRow("Gas Device id", lastTelegram.gasMeterId);
            msg.append(deviceTable.table());
            msg.append("</div>\n"); // col
            msg.append("</div>\n"); // row

            SortedMap<String, Telegram> todayMap = TelegramFile.getTodayTelegrams();
            msg.append("<div class=\"row\">\n");
            msg.append("<div class=\"col-md-6\">\n");
            msg.append("<h2>Power usage today [kW]</h2>\n");
            LineChart powerChart = TelegramChart.powerDay(todayMap, "PowerToday");
            msg.append(powerChart.toDivHtml());
            msg.append("</div>\n"); // col

            msg.append("<div class=\"col-md-6\">\n");
            msg.append("<h2>Gas usage today [m3]</h2>\n");
            LineChart gasChart = TelegramChart.gasDay(todayMap, "GasToday");
            msg.append(gasChart.toDivHtml());
            msg.append("</div>\n"); // col
            msg.append("</div>\n"); // row

            msg.append("<div class=\"row\">\n");
            msg.append("<div class=\"col-md-6\">\n");
            msg.append("<h2>Cumulative power usage today [kW]</h2>\n");
            LineChart cumPowerChart = TelegramChart.cumulativePowerDay(todayMap, "CumPowerToday");
            msg.append(cumPowerChart.toDivHtml());
            msg.append("</div>\n"); // col

            msg.append("<div class=\"col-md-6\">\n");
            msg.append("<h2>Cumulative gas usage today [m3]</h2>\n");
            LineChart cumGasChart = TelegramChart.cumulativeGasDay(todayMap, "CumGasToday");
            msg.append(cumGasChart.toDivHtml());
            msg.append("</div>\n"); // col
            msg.append("</div>\n"); // row

            msg.append("<p>&nbsp;</p>");
            msg.append("</div>\n"); // container-fluid

            msg.append(powerChart.toScriptHtml());
            msg.append(gasChart.toScriptHtml());
            msg.append(cumPowerChart.toScriptHtml());
            msg.append(cumGasChart.toScriptHtml());
        }
        catch (Exception e)
        {
            System.err.println("Error in overview(): " + e.getMessage());
        }

        return framework.replace("<!-- #content -->", msg.toString());
    }

    /**
     * Create the electricity page HTML for a given day. It contains an electricity overview, instantaneous and cumulative power
     * usage, voltage development over the day, energy usage per hour of the day, end energy usage 30 days prior to the given
     * day and 12 months prior to the given day.
     * @param date LocalDate; the date for which to display the electricity page
     * @return String; complete HTML file with the page content
     */
    public static String electricity(final LocalDate date)
    {
        System.out.println("loaded page /electricity");

        String framework = readTextFile("/framework.html");
        framework = framework.replace("#1", "").replace("#2", "active").replace("#3", "").replace("#4", "");

        StringBuilder msg = new StringBuilder();

        try
        {
            msg.append("<div class=\"container-fluid\" style=\"margin-top:50px\">\n");

            SortedMap<String, Telegram> dayMap = TelegramFile.getDayTelegrams(date);
            Telegram firstTelegram = dayMap.get(dayMap.firstKey());
            LocalDate actualDate = firstTelegram.time.isAfter(LocalTime.of(23, 0)) ? firstTelegram.date.plus(1, ChronoUnit.DAYS)
                    : firstTelegram.date;
            String dateString = makeDateString(actualDate);

            msg.append(datePicker(actualDate, "/electricity"));

            if (actualDate.equals(LocalDate.now()))
            {
                Telegram lastTelegram = TelegramFile.getLastTelegram();

                msg.append("<div class=\"row\">\n");
                msg.append("<div class=\"col-md-6\">\n");
                msg.append("<h2>Overview</h2>\n");

                Table overviewTable = new Table();
                overviewTable.addRow("Electricity Tariff 1", lastTelegram.electricityTariff1kWh, "kWh");
                overviewTable.addRow("Electricity Tariff 2", lastTelegram.electricityTariff2kWh, "kWh");
                overviewTable.addRow("Tariff", lastTelegram.tariff == 1 ? "1 (low)" : "2 (high)");
                overviewTable.addRow("Power", lastTelegram.powerDeliveredkW, "kW");
                overviewTable.addRow("Voltage", lastTelegram.voltageL1, "V");
                double currentL1 = 1000.0 * lastTelegram.powerDeliveredkW / lastTelegram.voltageL1;
                overviewTable.addRow("Current", String.format("%.3f", currentL1), "A");
                msg.append(overviewTable.table());
                msg.append("</div>\n"); // col

                msg.append("<div class=\"col-md-6\">\n");
                msg.append("<h2>Devices</h2>\n");
                Table deviceTable = new Table();
                deviceTable.addRow("Electricity Device id", lastTelegram.electricityMeterId);
                deviceTable.addRow("\u00a0", " "); // &nbsp;
                deviceTable.addRow("Long power failures", lastTelegram.longPowerFailuresAnyPhase);
                deviceTable.addRow("Power failures", lastTelegram.powerFailuresAnyPhase);
                deviceTable.addRow("Voltage sags L1", lastTelegram.voltageSagsL1);
                deviceTable.addRow("Voltage swells L1", lastTelegram.voltageSwellsL1);
                msg.append(deviceTable.table());
                msg.append("</div>\n"); // col
                msg.append("</div>\n"); // row
            }

            msg.append("<div class=\"row\">\n");
            msg.append("<div class=\"col-md-6\">\n");
            msg.append("<h2>Power usage " + dateString + " [kW]</h2>\n");
            LineChart powerChart = TelegramChart.powerDay(dayMap, "PowerDay");
            msg.append(powerChart.toDivHtml());
            msg.append("</div>\n"); // col

            msg.append("<div class=\"col-md-6\">\n");
            msg.append("<h2>Cumulative power usage " + dateString + " [kW]</h2>\n");
            LineChart cumPowerChart = TelegramChart.cumulativePowerDay(dayMap, "CumPowerDay");
            msg.append(cumPowerChart.toDivHtml());
            msg.append("</div>\n"); // col
            msg.append("</div>\n"); // row
            msg.append("<p>&nbsp;</p>");

            msg.append("<div class=\"row\">\n");
            msg.append("<div class=\"col-md-6\">\n");
            msg.append("<h2>Voltage L1 " + dateString + " [V]</h2>\n");
            LineChart voltageChart = TelegramChart.voltageDay(dayMap, "VoltageDay");
            msg.append(voltageChart.toDivHtml());
            msg.append("</div>\n"); // col

            msg.append("<div class=\"col-md-6\">\n");
            msg.append("<h2>Energy usage " + dateString + " per hour [kWh]</h2>\n");
            BarChart energyPerHourChart = TelegramChart.energyPerHourDay(dayMap, "EnergyPerHourDay");
            msg.append(energyPerHourChart.toDivHtml());
            msg.append("</div>\n"); // col
            msg.append("</div>\n"); // row
            msg.append("<p>&nbsp;</p>");

            msg.append("<div class=\"row\">\n");
            msg.append("<div class=\"col-md-6\">\n");
            msg.append("<h2>Energy usage 30 days until " + dateString + " [kWh]</h2>\n");
            BarChart energyPrev30DaysChart = TelegramChart.energyPrev30days(actualDate);
            msg.append(energyPrev30DaysChart.toDivHtml());
            msg.append("</div>\n"); // col

            msg.append("<div class=\"col-md-6\">\n");
            msg.append("<h2>Energy usage 12 months until " + dateString + " [kWh]</h2>\n");
            BarChart energyPrev12MonthsChart = TelegramChart.energyPrev12months(actualDate);
            msg.append(energyPrev12MonthsChart.toDivHtml());
            msg.append("</div>\n"); // col
            msg.append("</div>\n"); // row

            msg.append("<p>&nbsp;</p>");
            msg.append("</div>\n"); // container-fluid

            msg.append(powerChart.toScriptHtml());
            msg.append(cumPowerChart.toScriptHtml());
            msg.append(voltageChart.toScriptHtml());
            msg.append(energyPerHourChart.toScriptHtml());
            msg.append(energyPrev30DaysChart.toScriptHtml());
            msg.append(energyPrev12MonthsChart.toScriptHtml());
        }
        catch (Exception e)
        {
            System.err.println("Error in electricity(): " + e.getMessage());
        }

        return framework.replace("<!-- #content -->", msg.toString());
    }

    public static String gas(final LocalDate date)
    {
        System.out.println("loaded page /gas");

        String framework = readTextFile("/framework.html");
        framework = framework.replace("#1", "").replace("#2", "").replace("#3", "active").replace("#4", "");

        StringBuilder msg = new StringBuilder();

        try
        {
            msg.append("<div class=\"container-fluid\" style=\"margin-top:50px\">\n");

            SortedMap<String, Telegram> dayMap = TelegramFile.getDayTelegrams(date);
            Telegram firstTelegram = dayMap.get(dayMap.firstKey());
            LocalDate actualDate = firstTelegram.time.isAfter(LocalTime.of(23, 0)) ? firstTelegram.date.plus(1, ChronoUnit.DAYS)
                    : firstTelegram.date;
            String dateString = makeDateString(actualDate);

            msg.append(datePicker(actualDate, "/gas"));

            if (actualDate.equals(LocalDate.now()))
            {
                Telegram lastTelegram = TelegramFile.getLastTelegram();

                msg.append("<div class=\"row\">\n");
                msg.append("<div class=\"col-md-6\">\n");
                msg.append("<h2>Overview</h2>\n");

                Table overviewTable = new Table();
                overviewTable.addRow("Gas Delivered", lastTelegram.gasDeliveredM3, "m<sup>3</sup>");
                msg.append(overviewTable.table());
                msg.append("</div>\n"); // col

                msg.append("<div class=\"col-md-6\">\n");
                msg.append("<h2>Devices</h2>\n");
                Table deviceTable = new Table();
                deviceTable.addRow("Gas Device id", lastTelegram.gasMeterId);
                msg.append(deviceTable.table());
                msg.append("</div>\n"); // col
                msg.append("</div>\n"); // row
            }

            msg.append("<div class=\"row\">\n");
            msg.append("<div class=\"col-md-6\">\n");
            msg.append("<h2>Gas usage " + dateString + " [m3]</h2>\n");
            LineChart gasChart = TelegramChart.gasDay(dayMap, "GasDay");
            msg.append(gasChart.toDivHtml());
            msg.append("</div>\n"); // col

            msg.append("<div class=\"col-md-6\">\n");
            msg.append("<h2>Cumulative gas usage " + dateString + " [m3]</h2>\n");
            LineChart cumGasChart = TelegramChart.cumulativeGasDay(dayMap, "CumGasDay");
            msg.append(cumGasChart.toDivHtml());
            msg.append("</div>\n"); // col
            msg.append("</div>\n"); // row

            msg.append("<div class=\"row\">\n");
            msg.append("<div class=\"col-md-6\">\n");
            msg.append("<h2>Gas usage 30 days until " + dateString + " [m3]</h2>\n");
            BarChart gasPrev30DaysChart = TelegramChart.gasPrev30days(actualDate);
            msg.append(gasPrev30DaysChart.toDivHtml());
            msg.append("</div>\n"); // col

            msg.append("<div class=\"col-md-6\">\n");
            msg.append("<h2>Gas usage 12 months until " + dateString + " [m3]</h2>\n");
            BarChart gasPrev12MonthsChart = TelegramChart.gasPrev12months(actualDate);
            msg.append(gasPrev12MonthsChart.toDivHtml());
            msg.append("</div>\n"); // col
            msg.append("</div>\n"); // row

            msg.append("<p>&nbsp;</p>");
            msg.append("</div>\n"); // container-fluid

            msg.append(gasChart.toScriptHtml());
            msg.append(cumGasChart.toScriptHtml());
            msg.append(gasPrev30DaysChart.toScriptHtml());
            msg.append(gasPrev12MonthsChart.toScriptHtml());
        }
        catch (Exception e)
        {
            System.err.println("Error in gas(): " + e.getMessage());
        }

        return framework.replace("<!-- #content -->", msg.toString());
    }

    public static String comparison()
    {
        System.out.println("loaded page /comparison");

        String framework = readTextFile("/framework.html");
        framework = framework.replace("#1", "").replace("#2", "").replace("#3", "").replace("#4", "active");

        StringBuilder msg = new StringBuilder();

        try
        {
            Telegram lastTelegram = TelegramFile.getLastTelegram();

            msg.append("<div class=\"container-fluid\" style=\"margin-top:50px\">\n");
            msg.append("<div class=\"row\">\n");
            msg.append("<div class=\"col-md-6\">\n");
            msg.append("<h2>Overview</h2>\n");

            Table overviewTable = new Table();
            overviewTable.addRow("Electricity Tariff 1", lastTelegram.electricityTariff1kWh, "kWh");
            overviewTable.addRow("Electricity Tariff 2", lastTelegram.electricityTariff2kWh, "kWh");
            overviewTable.addRow("Tariff", lastTelegram.tariff == 1 ? "1 (low)" : "2 (high)");
            overviewTable.addRow("Power", lastTelegram.powerDeliveredkW, "kW");
            overviewTable.addRow("Voltage", lastTelegram.voltageL1, "V");
            double currentL1 = 1000.0 * lastTelegram.powerDeliveredkW / lastTelegram.voltageL1;
            overviewTable.addRow("Current", String.format("%.3f", currentL1), "A");
            overviewTable.addRow("Gas Delivered", lastTelegram.gasDeliveredM3, "m<sup>3</sup>");
            msg.append(overviewTable.table());
            msg.append("</div>\n");

            msg.append("<div class=\"col-md-6\">\n");
            msg.append("<h2>Devices</h2>\n");
            Table deviceTable = new Table();
            deviceTable.addRow("Electricity Device id", lastTelegram.electricityMeterId);
            deviceTable.addRow("Gas Device id", lastTelegram.gasMeterId);
            msg.append(deviceTable.table());
            msg.append("</div>\n");

            msg.append("<p>&nbsp;</p>");
            msg.append("</div>\n"); // container-fluid
        }
        catch (Exception e)
        {
            System.err.println("Error in electricity(): " + e.getMessage());
        }

        return framework.replace("<!-- #content -->", msg.toString());
    }

    /**
     * The main program to start the web server.
     * @param args String[]; not used
     */
    public static void main(final String[] args)
    {
        try
        {
            new SmartMeterWeb();
        }
        catch (IOException exception)
        {
            System.err.println("Couldn't start server:\n" + exception);
        }
    }
}
