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
     * @return LineChart; a chart object with the power usage over a day
     */
    public static LineChart powerDay(final SortedMap<String, Telegram> dayMap, final String name)
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
        powerChart.setWidth("100%").setX(xList).setY(yList).setTitle("Power (kW)").setMax(1440.0).setTickStepSize(60).setHours(true)
        .setFill(true).setFillColor("red");
        return powerChart;
    }

    public static LineChart cumulativePowerDay(final SortedMap<String, Telegram> dayMap, final String name)
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
        powerChart.setWidth("100%").setX(xList).setY(yList).setTitle("Power (kW)").setMax(1440.0).setTickStepSize(60).setHours(true)
        .setFill(false);
        return powerChart;
    }

    public static LineChart gasDay(final SortedMap<String, Telegram> dayMap, final String name)
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
        gasChart.setWidth("100%").setX(xList).setY(yList).setTitle("Gas (m3)").setMax(1440.0).setTickStepSize(60).setHours(true)
        .setFill(true).setFillColor("red");
        return gasChart;
    }

    public static LineChart cumulativeGasDay(final SortedMap<String, Telegram> dayMap, final String name)
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
        gasChart.setWidth("100%").setX(xList).setY(yList).setTitle("Gas (m3)").setMax(1440.0).setTickStepSize(60).setHours(true)
        .setFill(false).setFillColor("blue");
        return gasChart;
    }

    public static LineChart voltageDay(final SortedMap<String, Telegram> dayMap, final String name)
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
        voltageChart.setWidth("100%").setX(xList).setY(yList).setTitle("Voltage L1 (V)").setMax(1440.0).setTickStepSize(60)
        .setHours(true).setFill(false).setFillColor("green");
        return voltageChart;
    }

    public static BarChart energyPerHourDay(final SortedMap<String, Telegram> dayMap, final String name)
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
        powerChart.setWidth("100%").setLabels(xList).setValues(yList).setTtile("Energy (kWh)");
        return powerChart;
    }

    public static BarChart energyLast30days()
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

        powerChart.setWidth("100%").setTtile("Power (kW)").setLabels(labelList).setValues(totalList);
        return powerChart;
    }

    public static BarChart energyLast12months()
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

        powerChart.setWidth("100%").setTtile("Power (kW)").setLabels(labelList).setValues(totalList);
        return powerChart;
    }

    public static BarChart gasLast30days()
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

        gasChart.setWidth("100%").setLabels(labelList).setValues(valueList).setTtile("Gas (m3)");
        return gasChart;
    }

    public static BarChart gasLast12months()
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

        gasChart.setWidth("100%").setTtile("Gas (m3)").setLabels(labelList).setValues(gasList);
        return gasChart;
    }

}
