package nl.verbraeck.smartmeter;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * TelegramFile contains methods to read telegram files for today, a given date, the last 30 days before a given date, and the
 * last 12 months before a given date.
 * <p>
 * Copyright (c) 2020-2023 Alexnder Verbraeck, Delft, the Netherlands. All rights reserved. <br>
 * MIT-license.
 * </p>
 * @author <a href="https://github.com/averbraeck">Alexander Verbraeck</a>
 */
public class TelegramFile
{

    /**
     * Read all telegrams (max 1440) for today (or for the last saved date when no new files are added).
     * @return SortedMap&lt;String, Telegram&gt;; the sorted map with the date and time as the key (formatted as "yyyyMMdd
     *         HH:mm") to the corresponding Telegram
     */
    public static SortedMap<String, Telegram> getTodayTelegrams()
    {
        SortedMap<String, Telegram> telegramMap = new TreeMap<>();
        try
        {
            File dir = new File(Constants.LOCAL_FOLDER);
            final TreeMap<String, File> fileMap = new TreeMap<>();
            dir.listFiles(new FilenameFilter()
            {
                @Override
                public boolean accept(final File folder, final String name)
                {
                    File file = new File(folder, name);
                    if (file.isFile() && name.endsWith(Constants.FILE_SUFFIX))
                    {
                        fileMap.put(name, file);
                        return true;
                    }
                    return false;
                }
            });
            String lastFile = fileMap.lastEntry().getValue().getAbsolutePath();
            List<String> lines = Files.readAllLines(Path.of(lastFile));
            String line = "";
            List<String> telegramLines = new ArrayList<>();
            while (!lines.isEmpty())
            {
                telegramLines.clear();
                do
                {
                    line = lines.remove(0);
                }
                while (!line.startsWith("/") && !lines.isEmpty());
                if (!lines.isEmpty())
                {
                    do
                    {
                        telegramLines.add(line);
                        line = lines.remove(0);
                    }
                    while (!line.startsWith("!") && !lines.isEmpty());
                }
                if (line.startsWith("!")) // full telegram
                {
                    Telegram telegram = TelegramParser.parseTelegram(telegramLines);
                    telegramMap.put(telegram.getDateTime(), telegram);
                }
            }
        }
        catch (Exception e)
        {
            System.err.println("error in getTodayTelegrams(): " + e.getMessage());
        }

        return telegramMap;
    }

    /**
     * Read the last Telegram that was saved for today (the current minute if cron job is running).
     * @return Telegram; the last Telegram that was saved for today
     */
    public static Telegram getLastTelegram()
    {
        try
        {
            File dir = new File(Constants.LOCAL_FOLDER);
            final TreeMap<String, File> fileMap = new TreeMap<>();
            dir.listFiles(new FilenameFilter()
            {
                @Override
                public boolean accept(final File folder, final String name)
                {
                    File file = new File(folder, name);
                    if (file.isFile() && name.endsWith(Constants.FILE_SUFFIX))
                    {
                        fileMap.put(name, file);
                        return true;
                    }
                    return false;
                }
            });
            String lastFile = fileMap.lastEntry().getValue().getAbsolutePath();
            List<String> lines = Files.readAllLines(Path.of(lastFile));

            // get the start of the last full telegram
            int lastStart = -1;
            int preLastStart = -1;
            for (int i = 0; i < lines.size(); i++)
            {
                if (lines.get(i).startsWith("/"))
                {
                    preLastStart = lastStart;
                    lastStart = i;
                }
            }
            List<String> telegramLines = new ArrayList<>();
            for (int i = preLastStart; i < lastStart - 1; i++)
            {
                telegramLines.add(lines.get(i));
            }
            Telegram telegram = TelegramParser.parseTelegram(telegramLines);
            return telegram;
        }
        catch (Exception e)
        {
            System.err.println("error in getLastTelegram(): " + e.getMessage());
            return new Telegram();
        }
    }

    /**
     * Read all telegrams (max 1440) for the given date. In case there are no telegrams for the given date, return the telegrams
     * for the last date when telegrams were saved.
     * @param date LocalDate; the date for which the Telegrams should be retrieved
     * @return SortedMap&lt;String, Telegram&gt;; the sorted map with the date and time as the key (formatted as "yyyyMMdd
     *         HH:mm") to the corresponding Telegram
     */
    public static SortedMap<String, Telegram> getDayTelegrams(final LocalDate date)
    {
        SortedMap<String, Telegram> telegramMap = new TreeMap<>();
        try
        {
            File dir = new File(Constants.LOCAL_FOLDER);
            File file = new File(dir, Constants.FILE_PREFIX + date.toString() + Constants.FILE_SUFFIX);
            if (file == null || !file.exists())
            {
                return getTodayTelegrams();
            }

            List<String> lines = Files.readAllLines(Path.of(file.getAbsolutePath()));
            List<String> telegramLines = new ArrayList<>();
            String line = "";
            while (!lines.isEmpty())
            {
                telegramLines.clear();
                do
                {
                    line = lines.remove(0);
                }
                while (!line.startsWith("/") && !lines.isEmpty());
                if (!lines.isEmpty())
                {
                    do
                    {
                        telegramLines.add(line);
                        line = lines.remove(0);
                    }
                    while (!line.startsWith("!") && !lines.isEmpty());
                }
                if (line.startsWith("!")) // full telegram
                {
                    Telegram telegram = TelegramParser.parseTelegram(telegramLines);
                    telegramMap.put(telegram.getDateTime(), telegram);
                }
            }
        }
        catch (Exception e)
        {
            System.err.println("error in getDayTelegrams(): " + e.getMessage());
        }

        return telegramMap;
    }

    /**
     * Read a number of first telegrams of the day until the given date.
     * @param targetDate LocalDate; the last date of the days to be retrieved
     * @param days int; the number of days for which the Telegrams should be retrieved
     * @return List&lt;Telegram&gt;; the list of first telegrams of the day until the given date, oldest first
     */
    public static List<Telegram> getStartOfDaysTelegrams(final LocalDate targetDate, final int days)
    {
        List<Telegram> telegramList = new ArrayList<>();
        try
        {
            File dir = new File(Constants.LOCAL_FOLDER);
            final TreeMap<String, File> fileMap = new TreeMap<>();
            dir.listFiles(new FilenameFilter()
            {
                @Override
                public boolean accept(final File folder, final String name)
                {
                    File file = new File(folder, name);
                    if (file.isFile() && name.endsWith(Constants.FILE_SUFFIX) && name.substring(Constants.FILE_PREFIX.length())
                            .substring(0, 10).compareTo(targetDate.toString()) <= 0)
                    {
                        fileMap.put(name, file);
                        return true;
                    }
                    return false;
                }
            });
            for (File file : fileMap.descendingMap().values())
            {
                List<String> lines = Files.readAllLines(Path.of(file.getAbsolutePath()));
                List<String> telegramLines = new ArrayList<>();
                String line = "";
                do
                {
                    line = lines.remove(0);
                }
                while (!line.startsWith("/") && !lines.isEmpty());
                if (!lines.isEmpty())
                {
                    do
                    {
                        telegramLines.add(line);
                        line = lines.remove(0);
                    }
                    while (!line.startsWith("!") && !lines.isEmpty());
                }
                if (line.startsWith("!")) // full telegram
                {
                    Telegram telegram = TelegramParser.parseTelegram(telegramLines);
                    telegramList.add(telegram);
                }
                if (telegramList.size() >= days)
                    break;
            }
        }
        catch (Exception e)
        {
            System.err.println("error in getStartOfDaysTelegrams(): " + e.getMessage());
        }

        Collections.reverse(telegramList);
        return telegramList;
    }

    /**
     * Return a map of the first telegram of the first of the month for the past x months. Add the last telegram of provided
     * month to the end of the list (this might give partial information for the last month). If the last month is beyond the
     * available data, cut off at the month of the last registered data. The key is of the form yyyy-MM. The number of entries
     * in the map is (numberOfMonths + 1).
     * @param year int; the year of the last month for which to retrieve the data
     * @param month int; the last month for which to retrieve the data
     * @param numberOfMonths int; the number of months to retrieve.
     * @return Map of Telegrams for the given number of months, including the target month; oldest first; the key is yyyy-MM.
     */
    public static SortedMap<String, Telegram> getStartOfMonthsTelegrams(final int year, final int month,
            final int numberOfMonths)
    {
        SortedMap<String, Telegram> telegramMap = new TreeMap<>();
        try
        {
            File dir = new File(Constants.LOCAL_FOLDER);
            final TreeMap<String, File> fileMap = new TreeMap<>();
            dir.listFiles(new FilenameFilter()
            {
                @Override
                public boolean accept(final File folder, final String name)
                {
                    File file = new File(folder, name);
                    if (file.isFile() && name.endsWith(Constants.FILE_SUFFIX))
                    {
                        fileMap.put(name, file);
                        return true;
                    }
                    return false;
                }
            });

            // get the last entry in the given month; in other words, the last entry BEFORE the first of the next month.
            LocalDate lastDate = LocalDate.of(year, month, 1).plusMonths(1).minusDays(1);
            String lastKey = fileMap.floorKey(Constants.FILE_PREFIX + lastDate.toString());
            if (lastKey == null)
                lastKey = fileMap.lastKey();
            LocalDate date = LocalDate.parse(lastKey.substring(Constants.FILE_PREFIX.length()).substring(0, 10));
            SortedMap<String, Telegram> lastDayTelegrams = TelegramFile.getDayTelegrams(date);
            Telegram lastTelegram = lastDayTelegrams.get(lastDayTelegrams.lastKey());
            telegramMap.put(date.toString().substring(0, 7), lastTelegram);
            date = LocalDate.of(date.getYear(), date.getMonth(), 1);

            for (int i = 0; i < numberOfMonths; i++)
            {
                String fn = Constants.FILE_PREFIX + date.toString();
                File file = fileMap.ceilingEntry(fn).getValue();
                List<String> lines = Files.readAllLines(Path.of(file.getAbsolutePath()));
                List<String> telegramLines = new ArrayList<>();
                String line = "";
                do
                {
                    line = lines.remove(0);
                }
                while (!line.startsWith("/") && !lines.isEmpty());
                if (!lines.isEmpty())
                {
                    do
                    {
                        telegramLines.add(line);
                        line = lines.remove(0);
                    }
                    while (!line.startsWith("!") && !lines.isEmpty());
                }
                if (line.startsWith("!")) // full telegram
                {
                    Telegram telegram = TelegramParser.parseTelegram(telegramLines);
                    telegramMap.put(date.minusMonths(1).toString().substring(0, 7), telegram);
                }
                date = date.minusMonths(1);
            }
        }
        catch (Exception e)
        {
            System.err.println("error in getStartOfMonthsTelegrams(): " + e.getMessage());
        }

        return telegramMap;
    }

}
