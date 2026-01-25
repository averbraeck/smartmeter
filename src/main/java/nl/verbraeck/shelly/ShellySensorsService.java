package nl.verbraeck.shelly;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * ShellySensorsService.java.
 * <p>
 * Copyright (c) 2020-2023 Alexander Verbraeck, Delft, the Netherlands. All rights reserved. <br>
 * MIT-license.
 * </p>
 * @author <a href="https://github.com/averbraeck">Alexander Verbraeck</a>
 */
public final class ShellySensorsService
{

    private ShellySensorsService()
    {
    }

    // yyyy-mm-dd.csv in directory "shelly"
    private static final Pattern DATA_FILE_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}\\.csv");

    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Map entry for configuration (with order and room name).
     */
    private static final class MapEntry
    {
        final int order;

        final String id;

        final String room;

        MapEntry(final int order, final String id, final String room)
        {
            this.order = order;
            this.id = id;
            this.room = room;
        }
    }

    /**
     * Reading class.
     */
    private static final class Reading
    {
        final String id;

        final Instant tsUtc;

        final Double temperature;

        final Double humidity;

        Reading(final String id, final Instant tsUtc, final Double temperature, final Double humidity)
        {
            this.id = id;
            this.tsUtc = tsUtc;
            this.temperature = temperature;
            this.humidity = humidity;
        }
    }

    /**
     * Build escaped HTML table with latest readings per mapped device id.
     * @param dataDir directory with daily csv files (e.g., Paths.get("shelly"))
     * @param mappingCsv file with columns: order,id,room
     * @param displayZone ZoneId for local display (e.g., ZoneId.systemDefault() or ZoneId.of("Europe/Amsterdam"))
     * @return rendered table
     */
    public static String buildLatestReadingsTableHtml(final Path dataDir, final Path mappingCsv, final ZoneId displayZone)
    {
        try
        {
            List<MapEntry> mapping = readMapping(mappingCsv);
            if (mapping.isEmpty())
                return smallNote("No Shelly mapping entries found.");

            Optional<Path> latestDataFile = findLatestDataFile(dataDir);
            if (latestDataFile.isEmpty())
                return smallNote("No Shelly data file (yyyy-mm-dd.csv) found.");

            Map<String, Reading> latestPerId = readLatestPerDevice(latestDataFile.get());
            return renderTable(mapping, latestPerId, displayZone);
        }
        catch (IOException e)
        {
            return smallNote("Error reading Shelly data: " + htmlEscape(e.getMessage()));
        }
    }

    // ---------- Mapping ----------

    private static List<MapEntry> readMapping(final Path mappingCsv) throws IOException
    {
        if (!Files.isRegularFile(mappingCsv))
            return List.of();
        List<MapEntry> list = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(mappingCsv, StandardCharsets.UTF_8))
        {
            String line;
            boolean headerSeen = false;
            while ((line = br.readLine()) != null)
            {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#"))
                    continue;
                List<String> parts = parseCsvLine(trimmed);
                if (parts.isEmpty())
                    continue;
                if (!headerSeen && looksLikeMapHeader(parts))
                {
                    headerSeen = true;
                    continue;
                }
                if (parts.size() < 3)
                    continue;
                try
                {
                    int order = Integer.parseInt(parts.get(0).trim());
                    String id = parts.get(1).trim();
                    String room = parts.get(2).trim();
                    if (!id.isEmpty() && !room.isEmpty())
                        list.add(new MapEntry(order, id, room));
                }
                catch (NumberFormatException ignore)
                {
                }
            }
        }
        list.sort(Comparator.comparingInt(m -> m.order));
        return list;
    }

    private static boolean looksLikeMapHeader(final List<String> cols)
    {
        if (cols.size() < 3)
            return false;
        String a = cols.get(0).toLowerCase(Locale.ROOT);
        String b = cols.get(1).toLowerCase(Locale.ROOT);
        String c = cols.get(2).toLowerCase(Locale.ROOT);
        return a.contains("order") && b.contains("id") && (c.contains("room") || c.contains("name"));
    }

    // ---------- Data file ----------

    private static Optional<Path> findLatestDataFile(final Path dataDir) throws IOException
    {
        if (!Files.isDirectory(dataDir))
            return Optional.empty();
        try (var stream = Files.list(dataDir))
        {
            return stream.filter(p -> Files.isRegularFile(p) && DATA_FILE_PATTERN.matcher(p.getFileName().toString()).matches())
                    .max(Comparator.comparing(ShellySensorsService::extractDateFromName));
        }
    }

    private static LocalDate extractDateFromName(final Path p)
    {
        try
        {
            return LocalDate.parse(p.getFileName().toString().substring(0, 10));
        }
        catch (Exception e)
        {
            return LocalDate.MIN;
        }
    }

    private static Map<String, Reading> readLatestPerDevice(final Path csv) throws IOException
    {
        Map<String, Reading> latest = new HashMap<>();

        try (BufferedReader br = Files.newBufferedReader(csv, StandardCharsets.UTF_8))
        {
            String headerLine = br.readLine();
            if (headerLine == null)
                return latest;

            List<String> header = parseCsvLine(headerLine);
            Map<String, Integer> idx = indexMap(header);

            Integer iTs = idx.getOrDefault("timestamp", -1);
            Integer iDev = idx.getOrDefault("device", -1);
            Integer iTopic = idx.getOrDefault("topic", -1);
            Integer iTemp = idx.getOrDefault("tempc", -1);
            Integer iHum = idx.getOrDefault("humidity", -1);
            Integer iJson = idx.getOrDefault("raw_json", -1);

            if (iTs < 0 || iDev < 0 || iTopic < 0)
                return latest;

            String line;
            while ((line = br.readLine()) != null)
            {
                if (line.isBlank())
                    continue;

                // Parse CSV line safely; skip partial/incomplete line if parser throws
                List<String> cols;
                try
                {
                    cols = parseCsvLine(line);
                }
                catch (IllegalArgumentException e)
                {
                    // Likely a partially written line while Python is writing; skip it
                    continue;
                }
                if (cols.size() <= Math.max(Math.max(iTs, iDev), Math.max(iTopic, Math.max(iTemp, Math.max(iHum, iJson)))))
                {
                    continue; // incomplete
                }

                String tempStr = safe(cols, iTemp);
                String humStr = safe(cols, iHum);
                // Only keep rows where at least one of temp/humidity is present
                if ((tempStr == null || tempStr.isBlank()) && (humStr == null || humStr.isBlank()))
                {
                    continue;
                }

                String topic = safe(cols, iTopic);
                // Extra guard: skip obvious non-measurement topics like ".../online"
                if (topic != null && topic.endsWith("/online"))
                {
                    continue;
                }

                String deviceCol = safe(cols, iDev);
                String jsonCol = safe(cols, iJson);

                String id = deriveId(deviceCol, topic, jsonCol);
                if (id == null || id.isBlank())
                    continue;

                Instant tsUtc = parseUtcTimestamp(safe(cols, iTs));
                if (tsUtc == null)
                    continue;

                Double t = parseDoubleSafe(tempStr);
                Double h = parseDoubleSafe(humStr);

                Reading prev = latest.get(id);
                if (prev == null || tsUtc.isAfter(prev.tsUtc))
                {
                    latest.put(id, new Reading(id, tsUtc, t, h));
                }
            }
        }
        return latest;
    }

    private static Map<String, Integer> indexMap(final List<String> header)
    {
        Map<String, Integer> m = new HashMap<>();
        for (int i = 0; i < header.size(); i++)
        {
            String k = header.get(i).trim().toLowerCase(Locale.ROOT);
            m.put(k, i);
        }
        return m;
    }

    private static String safe(final List<String> cols, final int idx)
    {
        if (idx < 0 || idx >= cols.size())
            return null;
        String s = cols.get(idx);
        return s == null ? null : s.trim();
    }

    // Prefer id from topic ("shellyhtg3-.../xyz"), else from JSON "src", else normalize bare MAC in device column
    private static String deriveId(final String device, final String topic, final String rawJson)
    {
        // From topic
        if (topic != null)
        {
            int slash = topic.indexOf('/');
            String left = slash >= 0 ? topic.substring(0, slash) : topic;
            if (!left.isBlank() && left.toLowerCase(Locale.ROOT).startsWith("shelly"))
                return left.trim();
        }
        // From JSON "src"
        String src = extractJsonStringValue(rawJson, "\"src\"");
        if (src != null && src.toLowerCase(Locale.ROOT).startsWith("shelly"))
            return src.trim();
        // From device: if bare MAC, normalize to shellyhtg3-<lowercase>
        if (device != null)
        {
            String dev = device.trim();
            if (dev.matches("^[0-9A-Fa-f]{12}$"))
            {
                return "shellyhtg3-" + dev.toLowerCase(Locale.ROOT);
            }
            if (dev.toLowerCase(Locale.ROOT).startsWith("shelly"))
                return dev;
        }
        return device != null ? device.trim() : null;
    }

    // Very small JSON string extractor for a flat field like "src":"value"
    private static String extractJsonStringValue(final String json, final String keyLiteralWithQuotes)
    {
        if (json == null)
            return null;
        int k = json.indexOf(keyLiteralWithQuotes);
        if (k < 0)
            return null;
        int colon = json.indexOf(':', k + keyLiteralWithQuotes.length());
        if (colon < 0)
            return null;
        int firstQuote = json.indexOf('"', colon + 1);
        if (firstQuote < 0)
            return null;
        int i = firstQuote + 1;
        StringBuilder sb = new StringBuilder();
        boolean inStr = true;
        while (i < json.length() && inStr)
        {
            char c = json.charAt(i++);
            if (c == '\\')
            {
                if (i < json.length())
                {
                    char n = json.charAt(i++);
                    sb.append(n); // naive unescape
                }
            }
            else if (c == '"')
            {
                inStr = false;
            }
            else
            {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static Instant parseUtcTimestamp(final String s)
    {
        if (s == null || s.isBlank())
            return null;
        // Your format: 2026-01-25T16:13:52.710970+00:00 (ISO_OFFSET_DATE_TIME)
        try
        {
            return OffsetDateTime.parse(s).toInstant();
        }
        catch (DateTimeParseException e)
        {
            // Fallbacks: epoch seconds/millis
            if (s.matches("^-?\\d+$"))
            {
                try
                {
                    long v = Long.parseLong(s);
                    if (Math.abs(v) > 4_000_000_000L)
                        return Instant.ofEpochMilli(v);
                    return Instant.ofEpochSecond(v);
                }
                catch (NumberFormatException ignore)
                {
                }
            }
            return null;
        }
    }

    private static Double parseDoubleSafe(final String s)
    {
        if (s == null || s.isBlank())
            return null;
        try
        {
            return Double.valueOf(s);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    // RFC4180-ish CSV line parser (commas, double-quotes, doubled-quotes inside quoted fields)
    private static List<String> parseCsvLine(final String line)
    {
        ArrayList<String> out = new ArrayList<>(8);
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++)
        {
            char c = line.charAt(i);
            if (inQuotes)
            {
                if (c == '"')
                {
                    // Either end quote or escaped quote
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"')
                    {
                        sb.append('"'); // doubled quote -> literal quote
                        i++;
                    }
                    else
                    {
                        inQuotes = false; // end of quoted field
                    }
                }
                else
                {
                    sb.append(c);
                }
            }
            else
            {
                if (c == ',')
                {
                    out.add(sb.toString());
                    sb.setLength(0);
                }
                else if (c == '"')
                {
                    inQuotes = true;
                }
                else
                {
                    sb.append(c);
                }
            }
        }
        if (inQuotes)
        {
            // Incomplete line (likely being written right now) -> signal caller to skip
            throw new IllegalArgumentException("Unterminated quoted field");
        }
        out.add(sb.toString());
        return out;
    }

    private static String renderTable(final List<MapEntry> mapping, final Map<String, Reading> latestPerId,
            final ZoneId displayZone)
    {
        StringBuilder sb = new StringBuilder(512);
        sb.append("<table class=\"table table-striped table-sm\">\n");
        sb.append("<thead><tr>").append("<th>Room</th>").append("<th>Measurement time</th>")
                .append("<th class=\"text-end\">Temp (°C)</th>").append("<th class=\"text-end\">Hum (%RH)</th>")
                .append("</tr></thead>\n");
        sb.append("<tbody>\n");

        for (MapEntry m : mapping)
        {
            Reading r = latestPerId.get(m.id);
            String room = htmlEscape(m.room);
            String tsLocal = "—";
            String tStr = "—";
            String hStr = "—";

            if (r != null)
            {
                ZonedDateTime zdt = r.tsUtc.atZone(displayZone);
                tsLocal = htmlEscape(zdt.format(DISPLAY_FMT));
                if (r.temperature != null)
                    tStr = String.format(Locale.ROOT, "%.1f", r.temperature);
                if (r.humidity != null)
                    hStr = String.format(Locale.ROOT, "%.0f", r.humidity);
            }

            sb.append("<tr>").append("<td>").append(room).append("</td>").append("<td>").append(tsLocal).append("</td>")
                    .append("<td class=\"text-end\">").append(tStr).append("</td>").append("<td class=\"text-end\">")
                    .append(hStr).append("</td>").append("</tr>\n");
        }
        sb.append("</tbody>\n</table>\n");
        return sb.toString();
    }

    private static String smallNote(final String text)
    {
        return "<p class=\"text-muted\">" + htmlEscape(text) + "</p>";
    }

    private static String htmlEscape(final String s)
    {
        if (s == null)
            return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }
}
