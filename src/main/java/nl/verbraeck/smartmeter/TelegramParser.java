package nl.verbraeck.smartmeter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * TelegramParser parses a telegram. A Telegram contains one P1-message from the smart meter. E.g.,
 *
 * <pre>
 * /XMX5LGBBLA4415473347
 * 1-3:0.2.8(50)                                    Version information for P1 output (String)
 * 0-0:1.0.0(230505000259S)                         Date-time stamp of the P1 message (YYMMDDhhmmssX)
 * 0-0:96.1.1(4530303435303034303832303939373137)   Equipment identifier (String)
 * 1-0:1.8.1(010325.485*kWh)                        Meter Reading electricity delivered to client (Tariff 1) in 0,001 kWh
 * 1-0:1.8.2(007339.152*kWh)                        Meter Reading electricity delivered to client (Tariff 2) in 0,001 kWh
 * 1-0:2.8.1(000000.000*kWh)                        Meter Reading electricity delivered by client (Tariff 1) in 0,001 kWh
 * 1-0:2.8.2(000000.000*kWh)                        Meter Reading electricity delivered by client (Tariff 2) in 0,001 kWh
 * 0-0:96.14.0(0001)                                Tariff indicator electricity. (Tariff 1 = low, Tariff 2 = high)
 * 1-0:1.7.0(02.327*kW)                             Actual electricity power delivered (+P) in 1 Watt resolution
 * 1-0:2.7.0(00.000*kW)                             Actual electricity power received (-P) in 1 Watt resolution
 * 0-0:96.7.21(00003)                               Number of power failures in any phase
 * 0-0:96.7.9(00000)                                Number of long power failures in any phase
 * 1-0:99.97.0(0)(0-0:96.7.19)                      Power Failure Event Log (long power failures)
 * 1-0:32.32.0(00011)                               Number of voltage sags in phase L1
 * 1-0:32.36.0(00000)                               Number of voltage swells in phase L1
 * 0-0:96.13.0()                                    Text message max 1024 characters
 * 1-0:32.7.0(228.0*V)                              Instantaneous voltage L1 in V resolution
 * 1-0:31.7.0(010*A)                                Instantaneous current L1 in A resolution
 * 1-0:21.7.0(02.327*kW)                            Instantaneous active power L1 (+P) in W resolution
 * 1-0:22.7.0(00.000*kW)                            Instantaneous active power L1 (-P) in W resolution
 * 0-1:24.1.0(003)                                  Device-Type
 * 0-1:96.1.0(4730303339303031383033353931323138)   Equipment identifier (Gas)
 * 0-1:24.2.1(230505000003S)(05125.733*m3)          Last 5-minute value (temperature converted), gas delivered to client
 *                                                  in m3, including decimal values and capture time
 * !F07C
 * </pre>
 * <p>
 * Copyright (c) 2020-2023 Alexander Verbraeck.
 * </p>
 * @author <a href="https://www.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
public class TelegramParser
{
    /**
     * Parse the telegram into a telegram record.
     * @param lines List of strings containing telegram data
     * @return Telegram; a parsed telegram
     */
    public static Telegram parseTelegram(final List<String> lines)
    {
        Telegram telegram = new Telegram();
        for (String line : lines)
        {
            if (line.startsWith("1-3:0.2.8"))
                telegram.version = parseInt(line);
            else if (line.startsWith("0-0:1.0.0"))
            {
                telegram.date = parseDate(line);
                telegram.time = parseTime(line);
            }
            else if (line.startsWith("0-0:96.1.1"))
                telegram.electricityMeterId = parseHex(line);

            else if (line.startsWith("1-0:1.8.1"))
                telegram.electricityTariff1kWh = parseFloatUnit(line);
            else if (line.startsWith("1-0:1.8.2"))
                telegram.electricityTariff2kWh = parseFloatUnit(line);
            else if (line.startsWith("1-0:2.8.1"))
                telegram.electrBackTariff1kWh = parseFloatUnit(line);
            else if (line.startsWith("1-0:2.8.2"))
                telegram.electrBackTariff2kWh = parseFloatUnit(line);

            else if (line.startsWith("0-0:96.14.0"))
                telegram.tariff = parseInt(line);

            else if (line.startsWith("1-0:1.7.0"))
                telegram.powerDeliveredkW = parseFloatUnit(line);
            else if (line.startsWith("1-0:2.7.0"))
                telegram.powerReceivedkW = parseFloatUnit(line);

            else if (line.startsWith("0-0:96.7.21"))
                telegram.powerFailuresAnyPhase = parseInt(line);
            else if (line.startsWith("0-0:96.7.9"))
                telegram.longPowerFailuresAnyPhase = parseInt(line);

            else if (line.startsWith("1-0:32.32.0"))
                telegram.voltageSagsL1 = parseInt(line);
            else if (line.startsWith("1-0:52.32.0"))
                telegram.voltageSagsL2 = parseInt(line);
            else if (line.startsWith("1-0:72.32.0"))
                telegram.voltageSagsL3 = parseInt(line);

            else if (line.startsWith("1-0:32.36.0"))
                telegram.voltageSwellsL1 = parseInt(line);
            else if (line.startsWith("1-0:52.36.0"))
                telegram.voltageSwellsL2 = parseInt(line);
            else if (line.startsWith("1-0:72.36.0"))
                telegram.voltageSwellsL3 = parseInt(line);

            else if (line.startsWith("0-0:96.13.0"))
                telegram.textMessage = parseHex(line);

            else if (line.startsWith("1-0:32.7.0"))
                telegram.voltageL1 = parseFloatUnit(line);
            else if (line.startsWith("1-0:52.7.0"))
                telegram.voltageL2 = parseFloatUnit(line);
            else if (line.startsWith("1-0:72.7.0"))
                telegram.voltageL3 = parseFloatUnit(line);

            else if (line.startsWith("1-0:31.7.0"))
                telegram.currentL1 = parseFloatUnit(line);
            else if (line.startsWith("1-0:51.7.0"))
                telegram.currentL2 = parseFloatUnit(line);
            else if (line.startsWith("1-0:71.7.0"))
                telegram.currentL3 = parseFloatUnit(line);

            else if (line.startsWith("1-0:21.7.0"))
                telegram.powerDeliveredL1kW = parseFloatUnit(line);
            else if (line.startsWith("1-0:41.7.0"))
                telegram.powerDeliveredL2kW = parseFloatUnit(line);
            else if (line.startsWith("1-0:61.7.0"))
                telegram.powerDeliveredL3kW = parseFloatUnit(line);

            else if (line.startsWith("1-0:22.7.0"))
                telegram.powerReceivedL1kW = parseFloatUnit(line);
            else if (line.startsWith("1-0:42.7.0"))
                telegram.powerReceivedL2kW = parseFloatUnit(line);
            else if (line.startsWith("1-0:62.7.0"))
                telegram.powerReceivedL3kW = parseFloatUnit(line);

            else if (line.startsWith("0-1:24.1.0"))
                telegram.gasDeviceTypeId = parseInt(line);
            else if (line.startsWith("0-1:96.1.0"))
                telegram.gasMeterId = parseHex(line);
            else if (line.startsWith("0-1:24.2.1"))
            {
                telegram.gasCaptureDate = parseDate(line);
                telegram.gasCaptureTime = parseTime(line);
                telegram.gasDeliveredM3 = parseFloatUnit2(line);
            }
        }
        return telegram;

    }

    /**
     * Parse an integer from the telegram line. The line is formatted, e.g., as <code>0-0:96.14.0(0001)</code>. The int is
     * enclosed between brackets.
     * @param line The line to parse
     * @return int; the parsed integer value
     */
    private static int parseInt(final String line)
    {
        int bo = line.indexOf('(');
        int bc = line.indexOf(')');
        if (bo == -1 || bc == -1 || bo > bc)
            return 0;
        String nr = line.substring(bo + 1, bc);
        try
        {
            return Integer.parseInt(nr);
        }
        catch (NumberFormatException nfe)
        {
            System.err.println("parseInt. Line: " + line + ", error: " + nfe.getMessage());
            return 0;
        }
    }

    /**
     * Parse a floating point value with a unit from the telegram line. The line is formatted, e.g., as
     * <code>1-0:1.7.0(02.327*kW)</code>. The floating point value and unit are enclosed between brackets. The unit is stored
     * after the asterisk.
     * @param line The line to parse
     * @return double; the parsed floating point value
     */
    private static double parseFloatUnit(final String line)
    {
        int bo = line.indexOf('(');
        int bc = line.indexOf('*');
        if (bo == -1 || bc == -1 || bo > bc)
            return 0.0;
        String nr = line.substring(bo + 1, bc);
        try
        {
            return Double.parseDouble(nr);
        }
        catch (NumberFormatException nfe)
        {
            System.err.println("parseFloatUnit. Line: " + line + ", error: " + nfe.getMessage());
            return 0.0;
        }
    }

    /**
     * Parse a floating point value with a unit from the telegram line. The line is formatted, e.g., as
     * <code>0-1:24.2.1(230505000003S)(05125.733*m3)</code>. The floating point value and unit are enclosed between the second
     * pair of brackets. The unit is stored after the asterisk.
     * @param line The line to parse
     * @return double; the parsed floating point value
     */
    private static double parseFloatUnit2(final String line)
    {
        int bo = line.indexOf('(', line.indexOf('(') + 1);
        int bc = line.indexOf('*');
        if (bo == -1 || bc == -1 || bo > bc)
            return 0.0;
        String nr = line.substring(bo + 1, bc);
        try
        {
            return Double.parseDouble(nr);
        }
        catch (NumberFormatException nfe)
        {
            System.err.println("parseFloatUnit. Line: " + line + ", error: " + nfe.getMessage());
            return 0.0;
        }
    }

    /**
     * Parse a hexadecimal value from the telegram line. The line is formatted, e.g., as
     * <code>0-1:96.1.0(4730303339303031383033353931323138)</code>. The hexadecimal value is enclosed between brackets.
     * @param line The line to parse
     * @return String; the parsed hexadecimal string value
     */
    private static String parseHex(final String line)
    {
        int bo = line.indexOf('(');
        int bc = line.lastIndexOf(')');
        if (bo == -1 || bc == -1 || bo > bc)
            return "";
        String hex = line.substring(bo + 1, bc);
        StringBuilder result = new StringBuilder("");
        for (int i = 0; i < hex.length(); i += 2)
        {
            String str = hex.substring(i, i + 2);
            result.append((char) Integer.parseInt(str, 16));
        }
        return result.toString();
    }

    /**
     * Parse a date value from the telegram line. The line is formatted, e.g., as <code>0-0:1.0.0(230505000259S)</code>. The
     * date (and time) value is enclosed between brackets, and formatted as YYMMDDhhmmssX.
     * @param line The line to parse
     * @return LocalDate; the parsed date value
     */
    private static LocalDate parseDate(final String line)
    {
        int bo = line.indexOf('(');
        int bc = line.lastIndexOf(')');
        if (bo == -1 || bc == -1 || bo > bc)
            return LocalDate.now();
        try
        {
            return LocalDate.parse(line.substring(bo + 1, bo + 7), DateTimeFormatter.ofPattern("yyMMdd"));
        }
        catch (Exception exception)
        {
            return LocalDate.now();
        }
    }

    /**
     * Parse a time value from the telegram line. The line is formatted, e.g., as <code>0-0:1.0.0(230505000259S)</code>. The
     * time (and date) value is enclosed between brackets, and formatted as YYMMDDhhmmssX.
     * @param line The line to parse
     * @return LocalTime; the parsed time value
     */
    private static LocalTime parseTime(final String line)
    {
        int bo = line.indexOf('(');
        int bc = line.lastIndexOf(')');
        if (bo == -1 || bc == -1 || bo > bc)
            return LocalTime.now();
        try
        {
            return LocalTime.parse(line.substring(bo + 7, bo + 13), DateTimeFormatter.ofPattern("HHmmss"));
        }
        catch (Exception exception)
        {
            return LocalTime.now();
        }
    }

}
