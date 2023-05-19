package nl.verbraeck.smartmeter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import nl.verbraeck.smartmeter.chart.BarChart;
import nl.verbraeck.smartmeter.chart.LineChart;

/**
 * TelegramChart makes charts from provided Telegram data.
 * <p>
 * Copyright (c) 2020-2023 Alexnder Verbraeck, Delft, the Netherlands. All rights reserved. <br>
 * MIT-license.
 * </p>
 * @author <a href="https://github.com/averbraeck">Alexander Verbraeck</a>
 */
public class TelegramChart
{
    /**
     * Make a line chart of the L1 power usage of a maximum of 1440 minutes of a day.
     * @param dayMap SortedMap&lt;String, Telegram&gt;; Map of clock times to telegrams for a day
     * @param name String; name of the chart to use; has to be unique within the page
     * @return LineChart; a chart object with the L1 power usage over a day
     */
    public static LineChart powerDay(final SortedMap<String, Telegram> dayMap, final String name)
    {
        LineChart powerChart = new LineChart(name);
        try
        {
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
            powerChart.setWidth("100%").setX(xList).setY(yList).setTitle("Power (kW)").setMax(1440.0).setTickStepSize(60)
            .setHours(true).setFill(true).setFillColor("red");
        }
        catch (Exception e)
        {
            System.err.println("error in energyPrev12months: " + e.getMessage());
        }
        return powerChart;
    }

    /**
     * Make a line chart of the cumulative L1 power usage of a maximum of 1440 minutes of a day.
     * @param dayMap SortedMap&lt;String, Telegram&gt;; Map of clock times to telegrams for a day
     * @param name String; name of the chart to use; has to be unique within the page
     * @return LineChart; a chart object with the cumulative L1 power usage over a day
     */
    public static LineChart cumulativePowerDay(final SortedMap<String, Telegram> dayMap, final String name)
    {
        LineChart powerChart = new LineChart(name);
        try
        {
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
            powerChart.setWidth("100%").setX(xList).setY(yList).setTitle("Power (kW)").setMax(1440.0).setTickStepSize(60)
            .setHours(true).setFill(false);
        }
        catch (Exception e)
        {
            System.err.println("error in energyPrev12months: " + e.getMessage());
        }
        return powerChart;
    }

    /**
     * Make a line chart of the gas usage of a maximum of 1440 minutes of a day.
     * @param dayMap SortedMap&lt;String, Telegram&gt;; Map of clock times to telegrams for a day
     * @param name String; name of the chart to use; has to be unique within the page
     * @return LineChart; a chart object with the gas usage over a day
     */
    public static LineChart gasDay(final SortedMap<String, Telegram> dayMap, final String name)
    {
        LineChart gasChart = new LineChart(name);
        try
        {
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
            gasChart.setWidth("100%").setX(xList).setY(yList).setTitle("Gas (m3)").setMax(1440.0).setTickStepSize(60)
            .setHours(true).setFill(true).setFillColor("red");
        }
        catch (Exception e)
        {
            System.err.println("error in energyPrev12months: " + e.getMessage());
        }
        return gasChart;
    }

    /**
     * Make a line chart of the cumulative gas usage of a maximum of 1440 minutes of a day.
     * @param dayMap SortedMap&lt;String, Telegram&gt;; Map of clock times to telegrams for a day
     * @param name String; name of the chart to use; has to be unique within the page
     * @return LineChart; a chart object with the cumulative gas usage over a day
     */
    public static LineChart cumulativeGasDay(final SortedMap<String, Telegram> dayMap, final String name)
    {
        LineChart gasChart = new LineChart(name);
        try
        {
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
            gasChart.setWidth("100%").setX(xList).setY(yList).setTitle("Gas (m3)").setMax(1440.0).setTickStepSize(60)
            .setHours(true).setFill(false).setFillColor("blue");
        }
        catch (Exception e)
        {
            System.err.println("error in energyPrev12months: " + e.getMessage());
        }
        return gasChart;
    }

    /**
     * Make a line chart of the voltage development of a maximum of 1440 minutes of a day.
     * @param dayMap SortedMap&lt;String, Telegram&gt;; Map of clock times to telegrams for a day
     * @param name String; name of the chart to use; has to be unique within the page
     * @return LineChart; a chart object with the power development over a day
     */
    public static LineChart voltageDay(final SortedMap<String, Telegram> dayMap, final String name)
    {
        LineChart voltageChart = new LineChart(name);
        try
        {
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
            voltageChart.setWidth("100%").setX(xList).setY(yList).setTitle("Voltage L1 (V)").setMax(1440.0).setTickStepSize(60)
            .setHours(true).setFill(false).setFillColor("green");
        }
        catch (Exception e)
        {
            System.err.println("error in energyPrev12months: " + e.getMessage());
        }
        return voltageChart;
    }

    /**
     * Make a bar chart of the L1 energy usage per hour for a maximum of 24 hours.
     * @param dayMap SortedMap&lt;String, Telegram&gt;; Map of clock times to telegrams for a day
     * @param name String; name of the chart to use; has to be unique within the page
     * @return LineChart; a chart object with the L1 energy usage per hour for a maximum of 24 hours
     */
    public static BarChart energyPerHourDay(final SortedMap<String, Telegram> dayMap, final String name)
    {
        BarChart powerChart = new BarChart(name);
        try
        {
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
            powerChart.setWidth("100%").setLabels(xList).setValues(yList).setTitle("Energy (kWh)");
        }
        catch (Exception e)
        {
            System.err.println("error in energyPrev12months: " + e.getMessage());
        }
        return powerChart;
    }

    /**
     * Make a bar chart with the daily L1 energy usage for a maximum of 30 days before the given date.
     * @param targetDate LocalDate; last date of the 30 days to be displayed
     * @return BarChart; a chart object with the L1 energy usage for a maximum of 30 days before the given date
     */
    public static BarChart energyPrev30days(final LocalDate targetDate)
    {
        BarChart powerChart = new BarChart("Power30BarChart");
        try
        {
            List<Telegram> day30List = TelegramFile.getStartOfDaysTelegrams(targetDate, 30);
            SortedMap<String, Telegram> lastDayTelegrams = TelegramFile.getDayTelegrams(targetDate);
            Telegram lastTelegram = lastDayTelegrams.get(lastDayTelegrams.lastKey());
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

                labelList.add(" " + date.minusDays(1).toString());

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

            powerChart.setWidth("100%").setTitle("Power (kW)").setLabels(labelList).setValues(totalList);
        }
        catch (Exception e)
        {
            System.err.println("error in energyPrev12months: " + e.getMessage());
        }
        return powerChart;
    }

    /**
     * Make a bar chart with the monthly L1 energy usage for a maximum of 12 months before the given date.
     * @param targetDate LocalDate; last date of the 12 months to be displayed
     * @return BarChart; a chart object with the monthly L1 energy usage for a maximum of 12 months before the given date
     */
    public static BarChart energyPrev12months(final LocalDate targetDate)
    {
        BarChart powerChart = new BarChart("Power12BarChart");
        try
        {
            SortedMap<String, Telegram> months12Map =
                    TelegramFile.getStartOfMonthsTelegrams(targetDate.getYear(), targetDate.getMonthValue(), 12);
            List<String> labelList = new ArrayList<>();
            List<Double> tariff1List = new ArrayList<>();
            List<Double> tariff2List = new ArrayList<>();
            List<Double> totalList = new ArrayList<>();
            double prevTariff1 = Double.NaN;
            double prevTariff2 = Double.NaN;
            boolean first = true;
            for (String key : months12Map.keySet())
            {
                Telegram telegram = months12Map.get(key);

                double t1 = telegram.electricityTariff1kWh - prevTariff1;
                prevTariff1 = telegram.electricityTariff1kWh;

                double t2 = telegram.electricityTariff2kWh - prevTariff2;
                prevTariff2 = telegram.electricityTariff2kWh;

                if (!first)
                {
                    labelList.add(" " + key);
                    tariff1List.add(t1);
                    tariff2List.add(t2);
                    totalList.add(t1 + t2);
                }
                first = false;
            }
            powerChart.setWidth("100%").setTitle("Power (kW)").setLabels(labelList).setValues(totalList);
        }
        catch (Exception e)
        {
            System.err.println("error in energyPrev12months: " + e.getMessage());
        }
        return powerChart;
    }

    /**
     * Make a bar chart with the daily gas usage for a maximum of 30 days before the given date.
     * @param targetDate LocalDate; last date of the 30 days to be displayed
     * @return BarChart; a chart object with the gas usage for a maximum of 30 days before the given date
     */
    public static BarChart gasPrev30days(final LocalDate targetDate)
    {
        BarChart gasChart = new BarChart("Gas30BarChart");
        try
        {
            List<Telegram> day30List = TelegramFile.getStartOfDaysTelegrams(targetDate, 30);
            SortedMap<String, Telegram> lastDayTelegrams = TelegramFile.getDayTelegrams(targetDate);
            Telegram lastTelegram = lastDayTelegrams.get(lastDayTelegrams.lastKey());
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

                labelList.add(" " + date.minusDays(1).toString());
                valueList.add(telegram.gasDeliveredM3 - prevGas);
                prevGas = telegram.gasDeliveredM3;
            }

            // today
            labelList.add(lastTelegram.date.toString());
            valueList.add(lastTelegram.gasDeliveredM3 - prevGas);

            gasChart.setWidth("100%").setLabels(labelList).setValues(valueList).setTitle("Gas (m3)");
        }
        catch (Exception e)
        {
            System.err.println("error in energyPrev12months: " + e.getMessage());
        }
        return gasChart;
    }

    /**
     * Make a bar chart with the monthly gas usage for a maximum of 12 months before the given date.
     * @param targetDate LocalDate; last date of the 12 months to be displayed
     * @return BarChart; a chart object with the monthly gas usage for a maximum of 12 months before the given date
     */
    public static BarChart gasPrev12months(final LocalDate targetDate)
    {
        BarChart gasChart = new BarChart("Gas12BarChart");
        try
        {
            SortedMap<String, Telegram> months12Map =
                    TelegramFile.getStartOfMonthsTelegrams(targetDate.getYear(), targetDate.getMonthValue(), 12);
            List<String> labelList = new ArrayList<>();
            List<Double> gasList = new ArrayList<>();
            double prevGas = Double.NaN;
            boolean first = true;
            for (String key : months12Map.keySet())
            {
                Telegram telegram = months12Map.get(key);

                double gas = telegram.gasDeliveredM3 - prevGas;
                prevGas = telegram.gasDeliveredM3;

                if (!first)
                {
                    labelList.add(" " + key);
                    gasList.add(gas);
                }
                first = false;
            }
            gasChart.setWidth("100%").setTitle("Gas (m3)").setLabels(labelList).setValues(gasList);
        }
        catch (Exception e)
        {
            System.err.println("error in energyPrev12months: " + e.getMessage());
        }
        return gasChart;
    }

}
