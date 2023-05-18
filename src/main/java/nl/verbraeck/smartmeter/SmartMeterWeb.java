package nl.verbraeck.smartmeter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
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
    public SmartMeterWeb() throws IOException
    {
        super(3000);
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        System.out.println("\nRunning! Point your browsers to http://192.168.178.99:3000/ \n");
    }

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
            return response;
        }
        else if (uri.endsWith(".css"))
        {
            Response response = newFixedLengthResponse(readTextFile(uri));
            response.setMimeType("text/css");
            return response;
        }
        else if (uri.endsWith(".map"))
        {
            Response response = newFixedLengthResponse(readTextFile(uri));
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
            return newFixedLengthResponse(electricity(date));
        }
        if (uri.startsWith("/gas"))
        {
            return newFixedLengthResponse(gas(date));
        }
        if (uri.startsWith("/comparison"))
        {
            return newFixedLengthResponse(comparison());
        }

        return newFixedLengthResponse(overview());
    }

    private InputStream binaryStream(final String uri)
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

    private String readTextFile(final String uri)
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

    private String overview()
    {
        System.out.println("loaded page /");

        String framework = readTextFile("/framework.html");
        framework = framework.replace("#1", "active").replace("#2", "").replace("#3", "").replace("#4", "");

        Telegram lastTelegram = TelegramFile.getLastTelegram();
        StringBuilder msg = new StringBuilder();
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
        LineChart powerChart = powerDay(todayMap, "PowerToday");
        msg.append(powerChart.toDivHtml());
        msg.append("</div>\n"); // col

        msg.append("<div class=\"col-md-6\">\n");
        msg.append("<h2>Gas usage today [m3]</h2>\n");
        LineChart gasChart = gasDay(todayMap, "GasToday");
        msg.append(gasChart.toDivHtml());
        msg.append("</div>\n"); // col
        msg.append("</div>\n"); // row

        msg.append("<div class=\"row\">\n");
        msg.append("<div class=\"col-md-6\">\n");
        msg.append("<h2>Cumulative power usage today [kW]</h2>\n");
        LineChart cumPowerChart = cumulativePowerDay(todayMap, "CumPowerToday");
        msg.append(cumPowerChart.toDivHtml());
        msg.append("</div>\n"); // col

        msg.append("<div class=\"col-md-6\">\n");
        msg.append("<h2>Cumulative gas usage today [m3]</h2>\n");
        LineChart cumGasChart = cumulativeGasDay(todayMap, "CumGasToday");
        msg.append(cumGasChart.toDivHtml());
        msg.append("</div>\n"); // col
        msg.append("</div>\n"); // row

        msg.append("<p>&nbsp;</p>");
        msg.append("</div>\n"); // container-fluid

        msg.append(powerChart.toScriptHtml());
        msg.append(gasChart.toScriptHtml());
        msg.append(cumPowerChart.toScriptHtml());
        msg.append(cumGasChart.toScriptHtml());

        return framework.replace("<!-- #content -->", msg.toString());
    }

    private String electricity(final LocalDate date)
    {
        System.out.println("loaded page /electricity");

        String framework = readTextFile("/framework.html");
        framework = framework.replace("#1", "").replace("#2", "active").replace("#3", "").replace("#4", "");

        StringBuilder msg = new StringBuilder();
        msg.append("<div class=\"container-fluid\" style=\"margin-top:50px\">\n");

        if (date.equals(LocalDate.now()))
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

        String dateString = date.equals(LocalDate.now()) ? "today" : date.toString();
        SortedMap<String, Telegram> dayMap = TelegramFile.getDayTelegrams(date);
        msg.append("<div class=\"row\">\n");
        msg.append("<div class=\"col-md-6\">\n");
        msg.append("<h2>Power usage " + dateString + " [kW]</h2>\n");
        LineChart powerChart = powerDay(dayMap, "PowerDay");
        msg.append(powerChart.toDivHtml());
        msg.append("</div>\n"); // col

        msg.append("<div class=\"col-md-6\">\n");
        msg.append("<h2>Cumulative power usage " + dateString + " [kW]</h2>\n");
        LineChart cumPowerChart = cumulativePowerDay(dayMap, "CumPowerDay");
        msg.append(cumPowerChart.toDivHtml());
        msg.append("</div>\n"); // col
        msg.append("</div>\n"); // row
        msg.append("<p>&nbsp;</p>");

        msg.append("<div class=\"row\">\n");
        msg.append("<div class=\"col-md-6\">\n");
        msg.append("<h2>Voltage L1 " + dateString + " [V]</h2>\n");
        LineChart voltageChart = voltageDay(dayMap, "VoltageDay");
        msg.append(voltageChart.toDivHtml());
        msg.append("</div>\n"); // col

        msg.append("<div class=\"col-md-6\">\n");
        msg.append("<h2>Energy usage " + dateString + " per hour [kWh]</h2>\n");
        BarChart energyPerHourChart = energyPerHourDay(dayMap, "EnergyPerHourDay");
        msg.append(energyPerHourChart.toDivHtml());
        msg.append("</div>\n"); // col
        msg.append("</div>\n"); // row
        msg.append("<p>&nbsp;</p>");

        msg.append("<div class=\"row\">\n");
        msg.append("<div class=\"col-md-6\">\n");
        msg.append("<h2>Energy usage 30 days until " + dateString + " [kWh]</h2>\n");
        BarChart energyLast30DaysChart = energyLast30days();
        msg.append(energyLast30DaysChart.toDivHtml());
        msg.append("</div>\n"); // col

        msg.append("<div class=\"col-md-6\">\n");
        msg.append("<h2>Energy usage 12 months until " + dateString + " [kWh]</h2>\n");
        BarChart energyLast12MonthsChart = energyLast12months();
        msg.append(energyLast12MonthsChart.toDivHtml());
        msg.append("</div>\n"); // col
        msg.append("</div>\n"); // row

        msg.append("<p>&nbsp;</p>");
        msg.append("</div>\n"); // container-fluid

        msg.append(powerChart.toScriptHtml());
        msg.append(cumPowerChart.toScriptHtml());
        msg.append(voltageChart.toScriptHtml());
        msg.append(energyPerHourChart.toScriptHtml());
        msg.append(energyLast30DaysChart.toScriptHtml());
        msg.append(energyLast12MonthsChart.toScriptHtml());

        return framework.replace("<!-- #content -->", msg.toString());
    }

    private String gas(final LocalDate date)
    {
        System.out.println("loaded page /gas");

        String framework = readTextFile("/framework.html");
        framework = framework.replace("#1", "").replace("#2", "").replace("#3", "active").replace("#4", "");

        Telegram lastTelegram = TelegramFile.getLastTelegram();
        StringBuilder msg = new StringBuilder();
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
        msg.append("<h2>Gas usage today [m3]</h2>\n");
        LineChart gasChart = gasDay(todayMap, "GasToday");
        msg.append(gasChart.toDivHtml());
        msg.append("</div>\n"); // col

        msg.append("<div class=\"col-md-6\">\n");
        msg.append("<h2>Cumulative gas usage today [m3]</h2>\n");
        LineChart cumGasChart = cumulativeGasDay(todayMap, "CumGasToday");
        msg.append(cumGasChart.toDivHtml());
        msg.append("</div>\n"); // col
        msg.append("</div>\n"); // row

        msg.append("<div class=\"row\">\n");
        msg.append("<div class=\"col-md-6\">\n");
        msg.append("<h2>Gas usage last 30 days [m3]</h2>\n");
        BarChart gasLast30DaysChart = gasLast30days();
        msg.append(gasLast30DaysChart.toDivHtml());
        msg.append("</div>\n"); // col

        msg.append("<div class=\"col-md-6\">\n");
        msg.append("<h2>Gas usage last 12 months [m3]</h2>\n");
        BarChart gasLast12MonthsChart = gasLast12months();
        msg.append(gasLast12MonthsChart.toDivHtml());
        msg.append("</div>\n"); // col
        msg.append("</div>\n"); // row

        msg.append("<p>&nbsp;</p>");
        msg.append("</div>\n"); // container-fluid

        msg.append(gasChart.toScriptHtml());
        msg.append(cumGasChart.toScriptHtml());
        msg.append(gasLast30DaysChart.toScriptHtml());
        msg.append(gasLast12MonthsChart.toScriptHtml());

        return framework.replace("<!-- #content -->", msg.toString());
    }

    private String comparison()
    {
        System.out.println("loaded page /comparison");

        String framework = readTextFile("/framework.html");
        framework = framework.replace("#1", "").replace("#2", "").replace("#3", "").replace("#4", "active");

        Telegram lastTelegram = TelegramFile.getLastTelegram();
        StringBuilder msg = new StringBuilder();
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

        return framework.replace("<!-- #content -->", msg.toString());
    }

    private LineChart powerDay(final SortedMap<String, Telegram> dayMap, final String name)
    {
        LineChart powerChart = new LineChart(name);
        List<Double> xList = new ArrayList<>();
        List<Double> yList = new ArrayList<>();
        double minutes = 0.0;
        LocalDate date = null;
        for (Map.Entry<String, Telegram> entry : dayMap.entrySet())
        {
            if (date == null)
            {
                if (entry.getValue().time.isAfter(LocalTime.of(23, 0)))
                    date = entry.getValue().date.plus(1, ChronoUnit.DAYS);
                else
                    date = entry.getValue().date;
            }
            if (date.equals(entry.getValue().date))
            {
                minutes = Math.rint(entry.getValue().time.toSecondOfDay() / 60.0);
                xList.add(minutes);
                yList.add(entry.getValue().powerDeliveredkW);
            }
            else
            {
                System.out.println("Date: " + entry.getValue().date + " not equal to first date: " + date);
            }
        }
        while (minutes < 1440.0)
        {
            minutes = Math.rint(minutes + 1.000001);
            xList.add(minutes);
            yList.add(0.0);
        }
        powerChart.setWidth("100%").setX(xList).setY(yList).setLabel("Power (kW)").setMax(1440.0).setTicks(60).setHours(true)
        .setFill(true).setFillColor("red");
        return powerChart;
    }

    private LineChart cumulativePowerDay(final SortedMap<String, Telegram> dayMap, final String name)
    {
        LineChart powerChart = new LineChart(name);
        List<Double> xList = new ArrayList<>();
        List<Double> yList = new ArrayList<>();
        double minutes = 0.0;
        double cumEnergy = 0.0;
        LocalDate date = null;
        double firstEnergy =
                dayMap.get(dayMap.firstKey()).electricityTariff1kWh + dayMap.get(dayMap.firstKey()).electricityTariff2kWh;
        for (Map.Entry<String, Telegram> entry : dayMap.entrySet())
        {
            if (date == null)
            {
                if (entry.getValue().time.isAfter(LocalTime.of(23, 0)))
                    date = entry.getValue().date.plus(1, ChronoUnit.DAYS);
                else
                    date = entry.getValue().date;
            }
            if (date.equals(entry.getValue().date))
            {
                minutes = Math.rint(entry.getValue().time.toSecondOfDay() / 60.0);
                xList.add(minutes);
                cumEnergy = entry.getValue().electricityTariff1kWh + entry.getValue().electricityTariff2kWh - firstEnergy;
                yList.add(cumEnergy);
            }
            else
            {
                // ignore first entry -- it is often just before midnight
                if (minutes > 0.0)
                    System.out.println("Date: " + entry.getValue().date + " not equal to first date: " + date);
            }
        }
        while (minutes < 1440.0)
        {
            minutes = Math.rint(minutes + 1.000001);
            xList.add(minutes);
            yList.add(cumEnergy);
        }
        powerChart.setWidth("100%").setX(xList).setY(yList).setLabel("Power (kW)").setMax(1440.0).setTicks(60).setHours(true)
        .setFill(false);
        return powerChart;
    }

    private LineChart gasDay(final SortedMap<String, Telegram> dayMap, final String name)
    {
        LineChart gasChart = new LineChart(name);
        List<Double> xList = new ArrayList<>();
        List<Double> yList = new ArrayList<>();
        double minutes = 0.0;
        LocalDate date = null;
        double prev = -1.0;
        LocalTime timestamp = null;
        for (Map.Entry<String, Telegram> entry : dayMap.entrySet())
        {
            if (date == null)
            {
                if (entry.getValue().time.isAfter(LocalTime.of(23, 0)))
                    date = entry.getValue().date.plus(1, ChronoUnit.DAYS);
                else
                    date = entry.getValue().date;
            }
            if (date.equals(entry.getValue().date))
            {
                minutes = Math.rint(entry.getValue().time.toSecondOfDay() / 60.0);
                xList.add(minutes);
                yList.add(prev == -1.0 ? 0.0 : entry.getValue().gasDeliveredM3 - prev);
                if (!entry.getValue().gasCaptureTime.equals(timestamp))
                    prev = entry.getValue().gasDeliveredM3;
                timestamp = entry.getValue().gasCaptureTime;
            }
            else
            {
                System.out.println("Date: " + entry.getValue().date + " not equal to first date: " + date);
            }
        }
        while (minutes < 1440.0)
        {
            minutes = Math.rint(minutes + 1.000001);
            xList.add(minutes);
            yList.add(0.0);
        }
        gasChart.setWidth("100%").setX(xList).setY(yList).setLabel("Gas (m3)").setMax(1440.0).setTicks(60).setHours(true)
        .setFill(true).setFillColor("red");
        return gasChart;
    }

    private LineChart cumulativeGasDay(final SortedMap<String, Telegram> dayMap, final String name)
    {
        LineChart gasChart = new LineChart(name);
        List<Double> xList = new ArrayList<>();
        List<Double> yList = new ArrayList<>();
        double minutes = 0.0;
        LocalDate date = null;
        double firstGas = dayMap.get(dayMap.firstKey()).gasDeliveredM3;
        double lastGas = firstGas;
        for (Map.Entry<String, Telegram> entry : dayMap.entrySet())
        {
            if (date == null)
            {
                if (entry.getValue().time.isAfter(LocalTime.of(23, 0)))
                    date = entry.getValue().date.plus(1, ChronoUnit.DAYS);
                else
                    date = entry.getValue().date;
            }
            if (date.equals(entry.getValue().date))
            {
                minutes = Math.rint(entry.getValue().time.toSecondOfDay() / 60.0);
                xList.add(minutes);
                lastGas = entry.getValue().gasDeliveredM3;
                yList.add(lastGas - firstGas);
            }
            else
            {
                System.out.println("Date: " + entry.getValue().date + " not equal to first date: " + date);
            }
        }
        while (minutes < 1440.0)
        {
            minutes = Math.rint(minutes + 1.000001);
            xList.add(minutes);
            yList.add(lastGas - firstGas);
        }
        gasChart.setWidth("100%").setX(xList).setY(yList).setLabel("Gas (m3)").setMax(1440.0).setTicks(60).setHours(true)
        .setFill(false).setFillColor("blue");
        return gasChart;
    }

    private LineChart voltageDay(final SortedMap<String, Telegram> dayMap, final String name)
    {
        LineChart voltageChart = new LineChart(name);
        List<Double> xList = new ArrayList<>();
        List<Double> yList = new ArrayList<>();
        double minutes = 0.0;
        LocalDate date = null;
        for (Map.Entry<String, Telegram> entry : dayMap.entrySet())
        {
            if (date == null)
            {
                if (entry.getValue().time.isAfter(LocalTime.of(23, 0)))
                    date = entry.getValue().date.plus(1, ChronoUnit.DAYS);
                else
                    date = entry.getValue().date;
            }
            if (date.equals(entry.getValue().date))
            {
                minutes = Math.rint(entry.getValue().time.toSecondOfDay() / 60.0);
                xList.add(minutes);
                yList.add(entry.getValue().voltageL1);
            }
            else
            {
                System.out.println("Date: " + entry.getValue().date + " not equal to first date: " + date);
            }
        }
        while (minutes < 1440.0)
        {
            minutes = Math.rint(minutes + 1.000001);
            xList.add(minutes);
            yList.add(Double.NaN);
        }
        voltageChart.setWidth("100%").setX(xList).setY(yList).setLabel("Voltage L1 (V)").setMax(1440.0).setTicks(60)
        .setHours(true).setFill(false).setFillColor("green");
        return voltageChart;
    }

    private BarChart energyPerHourDay(final SortedMap<String, Telegram> dayMap, final String name)
    {
        BarChart powerChart = new BarChart(name);
        List<String> xList = new ArrayList<>();
        List<Double> yList = new ArrayList<>();
        List<Double> cList = new ArrayList<>();
        for (int x = 0; x < 24; x++)
        {
            xList.add(" " + x + ":00");
            yList.add(0.0);
            cList.add(0.0);
        }
        int hour = 0;
        double start =
                dayMap.get(dayMap.firstKey()).electricityTariff1kWh + dayMap.get(dayMap.firstKey()).electricityTariff2kWh;
        LocalDate date = null;
        for (Map.Entry<String, Telegram> entry : dayMap.entrySet())
        {
            if (date == null)
            {
                if (entry.getValue().time.isAfter(LocalTime.of(23, 0)))
                    date = entry.getValue().date.plus(1, ChronoUnit.DAYS);
                else
                    date = entry.getValue().date;
            }
            if (date.equals(entry.getValue().date))
            {
                hour = Math.min(23, (int) Math.round(entry.getValue().time.toSecondOfDay() / 3600.0));
                if (hour == 0)
                {
                    cList.set(hour, entry.getValue().electricityTariff1kWh + entry.getValue().electricityTariff2kWh);
                    yList.set(hour, cList.get(hour) - start);
                }
                else
                {
                    cList.set(hour, entry.getValue().electricityTariff1kWh + entry.getValue().electricityTariff2kWh);
                    yList.set(hour, cList.get(hour) - cList.get(hour - 1));
                }
            }
            else
            {
                System.out.println("Date: " + entry.getValue().date + " not equal to first date: " + date);
            }
        }
        powerChart.setWidth("100%").setLabels(xList).setValues(yList).setLabel("Energy (kWh)");
        return powerChart;
    }

    private BarChart energyLast30days()
    {
        List<Telegram> day30List = TelegramFile.getDaysTelegrams(30);
        Telegram lastTelegram = TelegramFile.getLastTelegram();
        BarChart powerChart = new BarChart("Power30BarChart");
        List<String> labelList = new ArrayList<>();
        List<Double> tariff1List = new ArrayList<>();
        List<Double> tariff2List = new ArrayList<>();
        List<Double> totalList = new ArrayList<>();
        double prevTariff1 = day30List.get(0).electricityTariff1kWh;
        double prevTariff2 = day30List.get(0).electricityTariff2kWh;
        LocalDate date;
        for (int i = 1; i < day30List.size(); i++)
        {
            Telegram telegram = day30List.get(i);
            if (telegram.time.isAfter(LocalTime.of(23, 0)))
                date = telegram.date.plus(1, ChronoUnit.DAYS);
            else
                date = telegram.date;

            labelList.add(" " + date.toString());

            double t1 = telegram.electricityTariff1kWh - prevTariff1;
            tariff1List.add(t1);
            prevTariff1 = telegram.electricityTariff1kWh;

            double t2 = telegram.electricityTariff2kWh - prevTariff2;
            tariff2List.add(t2);
            prevTariff2 = telegram.electricityTariff2kWh;

            totalList.add(t1 + t2);
        }

        // today
        labelList.add(lastTelegram.date.toString());
        double t1 = lastTelegram.electricityTariff1kWh - prevTariff1;
        tariff1List.add(t1);
        double t2 = lastTelegram.electricityTariff2kWh - prevTariff2;
        tariff1List.add(t2);
        totalList.add(t1 + t2);

        powerChart.setWidth("100%").setLabel("Power (kW)").setLabels(labelList).setValues(totalList);
        return powerChart;
    }

    private BarChart energyLast12months()
    {
        List<Telegram> months12List = TelegramFile.getMonthsTelegrams(12);
        Telegram lastTelegram = TelegramFile.getLastTelegram();
        BarChart powerChart = new BarChart("Power12BarChart");
        List<String> labelList = new ArrayList<>();
        List<Double> tariff1List = new ArrayList<>();
        List<Double> tariff2List = new ArrayList<>();
        List<Double> totalList = new ArrayList<>();
        double prevTariff1 = months12List.get(0).electricityTariff1kWh;
        double prevTariff2 = months12List.get(0).electricityTariff2kWh;
        LocalDate date;
        for (int i = 1; i < months12List.size(); i++)
        {
            Telegram telegram = months12List.get(i);
            if (telegram.time.isAfter(LocalTime.of(23, 0)))
                date = telegram.date.plus(1, ChronoUnit.DAYS);
            else
                date = telegram.date;

            labelList.add(" " + date.minusMonths(1).toString());

            double t1 = telegram.electricityTariff1kWh - prevTariff1;
            tariff1List.add(t1);
            prevTariff1 = telegram.electricityTariff1kWh;

            double t2 = telegram.electricityTariff2kWh - prevTariff2;
            tariff2List.add(t2);
            prevTariff2 = telegram.electricityTariff2kWh;

            totalList.add(t1 + t2);
        }

        // today
        labelList.add(lastTelegram.date.toString());
        double t1 = lastTelegram.electricityTariff1kWh - prevTariff1;
        tariff1List.add(t1);
        double t2 = lastTelegram.electricityTariff2kWh - prevTariff2;
        tariff1List.add(t2);
        totalList.add(t1 + t2);

        powerChart.setWidth("100%").setLabel("Power (kW)").setLabels(labelList).setValues(totalList);
        return powerChart;
    }

    private BarChart gasLast30days()
    {
        List<Telegram> day30List = TelegramFile.getDaysTelegrams(30);
        Telegram lastTelegram = TelegramFile.getLastTelegram();
        BarChart gasChart = new BarChart("Gas30BarChart");
        List<String> labelList = new ArrayList<>();
        List<Double> valueList = new ArrayList<>();
        double prevGas = day30List.get(0).gasDeliveredM3;
        LocalDate date;
        for (int i = 1; i < day30List.size(); i++)
        {
            Telegram telegram = day30List.get(i);
            if (telegram.time.isAfter(LocalTime.of(23, 0)))
                date = telegram.date.plus(1, ChronoUnit.DAYS);
            else
                date = telegram.date;

            labelList.add(" " + date.toString());
            valueList.add(telegram.gasDeliveredM3 - prevGas);
            prevGas = telegram.gasDeliveredM3;
        }

        // today
        labelList.add(lastTelegram.date.toString());
        valueList.add(lastTelegram.gasDeliveredM3 - prevGas);

        gasChart.setWidth("100%").setLabels(labelList).setValues(valueList).setLabel("Gas (m3)");
        return gasChart;
    }

    private BarChart gasLast12months()
    {
        List<Telegram> months12List = TelegramFile.getMonthsTelegrams(12);
        Telegram lastTelegram = TelegramFile.getLastTelegram();
        BarChart gasChart = new BarChart("Gas12BarChart");
        List<String> labelList = new ArrayList<>();
        List<Double> gasList = new ArrayList<>();
        double prevGas = months12List.get(0).gasDeliveredM3;
        LocalDate date;
        for (int i = 1; i < months12List.size(); i++)
        {
            Telegram telegram = months12List.get(i);
            if (telegram.time.isAfter(LocalTime.of(23, 0)))
                date = telegram.date.plus(1, ChronoUnit.DAYS);
            else
                date = telegram.date;

            labelList.add(" " + date.minusMonths(1).toString());

            double gas = telegram.gasDeliveredM3 - prevGas;
            gasList.add(gas);
            prevGas = telegram.gasDeliveredM3;
        }

        // today
        labelList.add(lastTelegram.date.toString());
        double gas = lastTelegram.gasDeliveredM3 - prevGas;
        gasList.add(gas);

        gasChart.setWidth("100%").setLabel("Gas (m3)").setLabels(labelList).setValues(gasList);
        return gasChart;
    }

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
