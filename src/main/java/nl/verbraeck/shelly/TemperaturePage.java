package nl.verbraeck.shelly;

import java.time.LocalDate;

import nl.verbraeck.smartmeter.SmartMeterWeb;

/**
 * TemperaturePage.java.
 * <p>
 * Copyright (c) 2020-2023 Alexander Verbraeck, Delft, the Netherlands. All rights reserved. <br>
 * MIT-license.
 * </p>
 * @author <a href="https://github.com/averbraeck">Alexander Verbraeck</a>
 */
public final class TemperaturePage
{
    /** */
    private TemperaturePage()
    {
    }

    /**
     * Create the temperature page HTML for a given day. It shows, for each mapped Shelly sensor, two line charts in one row: -
     * Temperature (°C) over the selected day - Humidity (%RH) over the selected day The page includes the date picker
     * navigation just like the electricity page. Data source: shelly/yyyy-mm-dd.csv (UTC timestamps -> displayed in local time)
     * @param date LocalDate; the date for which to display the temperature page
     * @param cachedFrameworkHtml the html page with placeholders to fill
     * @return String; complete HTML file with the page content
     */
    public static String temperature(final LocalDate date, final String cachedFrameworkHtml)
    {
        System.out.println("loaded page /temperature");

        // No nav item for temperature in cachedFrameworkHtml; keep all empty for now
        String framework = cachedFrameworkHtml.replace("#1", "").replace("#2", "").replace("#3", "").replace("#4", "")
                .replace("#5", "active");

        StringBuilder msg = new StringBuilder();

        // --- configuration ---
        final java.time.ZoneId DISPLAY_ZONE = java.time.ZoneId.of("Europe/Amsterdam");
        final java.nio.file.Path shellyDir = java.nio.file.Paths.get("shelly");
        final java.nio.file.Path mappingCsv = shellyDir.resolve("shelly_map.csv");
        final java.nio.file.Path dataCsv = shellyDir.resolve(date.toString() + ".csv"); // yyyy-mm-dd.csv

        try
        {
            msg.append("<div class=\"container-fluid\" style=\"margin-top:50px\">\n");

            // Date navigation (same style as electricity page)
            msg.append(SmartMeterWeb.datePicker(date, "/temperature"));

            // Title
            msg.append("<div class=\"row\"><div class=\"col-md-12\">\n");
            msg.append("<h2>Room climate for " + SmartMeterWeb.makeDateString(date) + "</h2>\n");
            if (!java.nio.file.Files.isRegularFile(dataCsv))
            {
                msg.append("<p class=\"text-muted\">No Shelly data file for this day: ")
                        .append(escapeHtml(dataCsv.toAbsolutePath().toString())).append("</p>\n");
                msg.append("</div></div>\n"); // row/col
                msg.append("</div>\n"); // container
                return framework.replace("<!-- #content -->", msg.toString());
            }
            msg.append("</div></div>\n"); // row/col

            // Load mapping (order, id, room)
            java.util.List<_MapEntry> mapping = _readMapping(mappingCsv);

            if (mapping.isEmpty())
            {
                msg.append("<p class=\"text-muted\">No Shelly mapping entries found at ")
                        .append(escapeHtml(mappingCsv.toAbsolutePath().toString())).append(".</p>\n");
                msg.append("</div>\n"); // container
                return framework.replace("<!-- #content -->", msg.toString());
            }

            // Load time series for the selected day, per mapped device
            java.util.Map<String, java.util.List<_Point>> tempSeries = new java.util.LinkedHashMap<>();
            java.util.Map<String, java.util.List<_Point>> humSeries = new java.util.LinkedHashMap<>();
            for (_MapEntry me : mapping)
            {
                tempSeries.put(me.id, new java.util.ArrayList<>());
                humSeries.put(me.id, new java.util.ArrayList<>());
            }
            _readDaySeries(dataCsv, mapping, tempSeries, humSeries, DISPLAY_ZONE);

            // Build grid: for each mapped device one row, with two columns (temp, humidity)
            StringBuilder script = new StringBuilder();
            int chartCount = 0;

            for (_MapEntry me : mapping)
            {
                String id = me.id;
                String room = me.room;

                java.util.List<_Point> tList = tempSeries.getOrDefault(id, java.util.Collections.emptyList());
                java.util.List<_Point> hList = humSeries.getOrDefault(id, java.util.Collections.emptyList());

                // Prepare DOM ids
                String safeId = id.replaceAll("[^A-Za-z0-9_]", "_");
                String tempCanvasId = "Temp_" + safeId;
                String humCanvasId = "Hum_" + safeId;

                // Row title
                msg.append("<div class=\"row\">\n");

                // Temperature chart (left)
                msg.append("<div class=\"col-md-6\">\n");
                msg.append("<h3>").append(escapeHtml(room)).append(" – Temperature (°C)</h3>\n");
                if (tList.isEmpty())
                {
                    msg.append("<p class=\"text-muted\">No temperature data for this day.</p>\n");
                }
                else
                {
                    msg.append("<div style=\"height:300px\"><canvas id=\"").append(tempCanvasId)
                            .append("\"></canvas></div>\n");
                }
                msg.append("</div>\n"); // col

                // Humidity chart (right)
                msg.append("<div class=\"col-md-6\">\n");
                msg.append("<h3>").append(escapeHtml(room)).append(" – Humidity (%RH)</h3>\n");
                if (hList.isEmpty())
                {
                    msg.append("<p class=\"text-muted\">No humidity data for this day.</p>\n");
                }
                else
                {
                    msg.append("<div style=\"height:300px\"><canvas id=\"").append(humCanvasId)
                            .append("\"></canvas></div>\n");
                }
                msg.append("</div>\n"); // col

                msg.append("</div>\n"); // row
                msg.append("<p>&nbsp;</p>");

                // Build JS arrays and chart constructors (Chart.js line charts)
                if (!tList.isEmpty())
                {
                    chartCount++;
                    script.append("var lbl_").append(tempCanvasId).append(" = [");
                    for (int i = 0; i < tList.size(); i++)
                    {
                        if (i > 0)
                            script.append(",");
                        script.append("'").append(jsEscape(_fmtTime(tList.get(i).zdt))).append("'");
                    }
                    script.append("];\n");
                    script.append("var dat_").append(tempCanvasId).append(" = [");
                    for (int i = 0; i < tList.size(); i++)
                    {
                        if (i > 0)
                            script.append(",");
                        script.append(_fmtNum(tList.get(i).value));
                    }
                    script.append("];\n");
                    script.append("new Chart(document.getElementById('").append(tempCanvasId).append("').getContext('2d'), {")
                            .append("type:'line',").append("data:{labels:lbl_").append(tempCanvasId).append(",datasets:[{")
                            .append("label:'Temperature (°C)',data:dat_").append(tempCanvasId)
                            .append(",borderColor:'#007bff',backgroundColor:'rgba(0,123,255,0.1)',")
                            .append("fill:false,pointRadius:0,pointHitRadius:6,lineTension:0.2").append("}]} ,")
                            .append("options:{responsive:true,maintainAspectRatio:false,").append("legend:{display:false},")
                            .append("scales:{").append("xAxes:[{display:true,scaleLabel:{display:false}}],")
                            .append("yAxes:[{display:true,ticks:{suggestedMin:0,suggestedMax:30}}]").append("}").append("}")
                            .append("});\n");
                }

                if (!hList.isEmpty())
                {
                    chartCount++;
                    script.append("var lbl_").append(humCanvasId).append(" = [");
                    for (int i = 0; i < hList.size(); i++)
                    {
                        if (i > 0)
                            script.append(",");
                        script.append("'").append(jsEscape(_fmtTime(hList.get(i).zdt))).append("'");
                    }
                    script.append("];\n");
                    script.append("var dat_").append(humCanvasId).append(" = [");
                    for (int i = 0; i < hList.size(); i++)
                    {
                        if (i > 0)
                            script.append(",");
                        script.append(_fmtNum(hList.get(i).value));
                    }
                    script.append("];\n");
                    script.append("new Chart(document.getElementById('").append(humCanvasId).append("').getContext('2d'), {")
                            .append("type:'line',").append("data:{labels:lbl_").append(humCanvasId).append(",datasets:[{")
                            .append("label:'Humidity (%RH)',data:dat_").append(humCanvasId)
                            .append(",borderColor:'#28a745',backgroundColor:'rgba(40,167,69,0.1)',")
                            .append("fill:false,pointRadius:0,pointHitRadius:6,lineTension:0.2").append("}]} ,")
                            .append("options:{responsive:true,maintainAspectRatio:false,").append("legend:{display:false},")
                            .append("scales:{").append("xAxes:[{display:true,scaleLabel:{display:false}}],")
                            .append("yAxes:[{display:true,ticks:{suggestedMin:0,suggestedMax:100}}]").append("}").append("}")
                            .append("});\n");
                }
            }

            if (mapping.isEmpty())
            {
                msg.append("<p class=\"text-muted\">No mapped sensors to display.</p>");
            }

            msg.append("</div>\n"); // container

            // Append the single script block that creates the charts
            if (script.length() > 0)
            {
                msg.append("<script>\n").append(script.toString()).append("</script>\n");
            }
        }
        catch (Exception e)
        {
            System.err.println("Error in temperature(): " + e.getMessage());
        }

        return framework.replace("<!-- #content -->", msg.toString());
    }

    /* ===================== helpers specific to temperature() ===================== */

    // minimal struct for mapping
    private static final class _MapEntry
    {
        final int order;

        final String id;

        final String room;

        _MapEntry(final int order, final String id, final String room)
        {
            this.order = order;
            this.id = id;
            this.room = room;
        }
    }

    // point in time series
    private static final class _Point
    {
        final java.time.ZonedDateTime zdt;

        final double value;

        _Point(final java.time.ZonedDateTime zdt, final double value)
        {
            this.zdt = zdt;
            this.value = value;
        }
    }

    private static java.util.List<_MapEntry> _readMapping(final java.nio.file.Path mappingCsv) throws java.io.IOException
    {
        if (!java.nio.file.Files.isRegularFile(mappingCsv))
            return java.util.List.of();
        java.util.List<_MapEntry> out = new java.util.ArrayList<>();
        try (java.io.BufferedReader br =
                java.nio.file.Files.newBufferedReader(mappingCsv, java.nio.charset.StandardCharsets.UTF_8))
        {
            String line;
            boolean headerSeen = false;
            int autoOrder = 1;
            while ((line = br.readLine()) != null)
            {
                if (line.isBlank())
                    continue;
                if (!headerSeen)
                    line = _stripBOM(line);
                java.util.List<String> cols;
                try
                {
                    cols = _parseCsvFlexible(line);
                }
                catch (IllegalArgumentException ex)
                {
                    continue;
                } // bad/partial line
                  // trim
                for (int i = 0; i < cols.size(); i++)
                    cols.set(i, cols.get(i).trim());
                if (cols.isEmpty() || cols.get(0).startsWith("#"))
                    continue;

                if (!headerSeen && _looksLikeMapHeader(cols))
                {
                    headerSeen = true;
                    continue;
                }
                headerSeen = true;

                if (cols.size() >= 3 && _isInt(cols.get(0)))
                {
                    out.add(new _MapEntry(Integer.parseInt(cols.get(0)), cols.get(1), cols.get(2)));
                }
                else if (cols.size() >= 2)
                {
                    out.add(new _MapEntry(autoOrder++, cols.get(0), cols.get(1)));
                }
            }
        }
        out.sort(java.util.Comparator.comparingInt(m -> m.order));
        return out;
    }

    private static boolean _looksLikeMapHeader(final java.util.List<String> cols)
    {
        if (cols.size() >= 3)
        {
            String a = cols.get(0).toLowerCase(java.util.Locale.ROOT);
            String b = cols.get(1).toLowerCase(java.util.Locale.ROOT);
            String c = cols.get(2).toLowerCase(java.util.Locale.ROOT);
            if (a.contains("order") && b.contains("id") && (c.contains("room") || c.contains("name")))
                return true;
        }
        if (cols.size() >= 2)
        {
            String a = cols.get(0).toLowerCase(java.util.Locale.ROOT);
            String b = cols.get(1).toLowerCase(java.util.Locale.ROOT);
            if (a.contains("id") && (b.contains("room") || b.contains("name")))
                return true;
        }
        return false;
    }

    private static boolean _isInt(final String s)
    {
        try
        {
            Integer.parseInt(s);
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    private static String _stripBOM(final String s)
    {
        if (s != null && !s.isEmpty() && s.charAt(0) == '\uFEFF')
            return s.substring(1);
        return s;
    }

    /** Parses a CSV line with auto delimiter (',' or ';') and proper quote handling. */
    private static java.util.List<String> _parseCsvFlexible(final String line)
    {
        char delim = _detectDelimiter(line);
        return _parseCsvWithDelimiter(line, delim);
    }

    private static char _detectDelimiter(final String line)
    {
        boolean inQuotes = false;
        int commas = 0, semis = 0;
        for (int i = 0; i < line.length(); i++)
        {
            char c = line.charAt(i);
            if (c == '"')
            {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"')
                {
                    i++;
                }
                else
                {
                    inQuotes = !inQuotes;
                }
            }
            else if (!inQuotes)
            {
                if (c == ',')
                    commas++;
                else if (c == ';')
                    semis++;
            }
        }
        return (semis > commas) ? ';' : ',';
    }

    private static java.util.List<String> _parseCsvWithDelimiter(final String line, final char delimiter)
    {
        java.util.ArrayList<String> out = new java.util.ArrayList<>(8);
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++)
        {
            char c = line.charAt(i);
            if (inQuotes)
            {
                if (c == '"')
                {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"')
                    {
                        sb.append('"');
                        i++;
                    }
                    else
                    {
                        inQuotes = false;
                    }
                }
                else
                {
                    sb.append(c);
                }
            }
            else
            {
                if (c == delimiter)
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
            throw new IllegalArgumentException("Unterminated quoted field");
        out.add(sb.toString());
        return out;
    }

    /** Read series for the day; only mapped devices; skips rows without measurements. */
    private static void _readDaySeries(final java.nio.file.Path csv, final java.util.List<_MapEntry> mapping,
            final java.util.Map<String, java.util.List<_Point>> tempSeries,
            final java.util.Map<String, java.util.List<_Point>> humSeries, final java.time.ZoneId displayZone)
            throws java.io.IOException
    {
        java.util.Set<String> allowed = new java.util.LinkedHashSet<>();
        for (_MapEntry me : mapping)
            allowed.add(me.id);

        try (java.io.BufferedReader br = java.nio.file.Files.newBufferedReader(csv, java.nio.charset.StandardCharsets.UTF_8))
        {
            String headerLine = br.readLine();
            if (headerLine == null)
                return;
            java.util.List<String> header = _parseCsvFlexible(headerLine);
            java.util.Map<String, Integer> idx = new java.util.HashMap<>();
            for (int i = 0; i < header.size(); i++)
                idx.put(header.get(i).trim().toLowerCase(java.util.Locale.ROOT), i);

            int iTs = idx.getOrDefault("timestamp", -1);
            int iDev = idx.getOrDefault("device", -1);
            int iTopic = idx.getOrDefault("topic", -1);
            int iTemp = idx.getOrDefault("tempc", -1);
            int iHum = idx.getOrDefault("humidity", -1);
            int iJson = idx.getOrDefault("raw_json", -1);

            if (iTs < 0 || iDev < 0 || iTopic < 0)
                return;

            String line;
            while ((line = br.readLine()) != null)
            {
                if (line.isBlank())
                    continue;
                java.util.List<String> cols;
                try
                {
                    cols = _parseCsvFlexible(line);
                }
                catch (IllegalArgumentException ex)
                {
                    continue;
                } // skip partial/incomplete

                if (cols.size() <= java.lang.Math.max(java.lang.Math.max(iTs, iDev),
                        java.lang.Math.max(iTopic, java.lang.Math.max(iTemp, iHum))))
                {
                    continue;
                }

                String tempStr = _safe(cols, iTemp);
                String humStr = _safe(cols, iHum);

                // Only measurement rows (temp or humidity present)
                if ((tempStr == null || tempStr.isBlank()) && (humStr == null || humStr.isBlank()))
                    continue;

                String topic = _safe(cols, iTopic);
                if (topic != null && topic.endsWith("/online"))
                    continue; // skip online pings

                String id = _deriveId(_safe(cols, iDev), topic, _safe(cols, iJson));
                if (id == null || !allowed.contains(id))
                    continue;

                java.time.Instant tsUtc = _parseUtcTimestamp(_safe(cols, iTs));
                if (tsUtc == null)
                    continue;

                java.time.ZonedDateTime zdt = tsUtc.atZone(displayZone);

                if (tempStr != null && !tempStr.isBlank())
                {
                    Double v = _parseDouble(tempStr);
                    if (v != null)
                        tempSeries.get(id).add(new _Point(zdt, v));
                }
                if (humStr != null && !humStr.isBlank())
                {
                    Double v = _parseDouble(humStr);
                    if (v != null)
                        humSeries.get(id).add(new _Point(zdt, v));
                }
            }
        }

        // Optional: ensure monotonic order (in case of out-of-order writes)
        java.util.Comparator<_Point> cmp = java.util.Comparator.comparing(p -> p.zdt.toInstant());
        for (java.util.List<_Point> l : tempSeries.values())
            java.util.Collections.sort(l, cmp);
        for (java.util.List<_Point> l : humSeries.values())
            java.util.Collections.sort(l, cmp);
    }

    // ----- tiny utils for parsing -----

    private static String _safe(final java.util.List<String> cols, final int idx)
    {
        if (idx < 0 || idx >= cols.size())
            return null;
        String s = cols.get(idx);
        return s == null ? null : s.trim();
    }

    private static java.time.Instant _parseUtcTimestamp(final String s)
    {
        if (s == null || s.isBlank())
            return null;
        try
        {
            return java.time.OffsetDateTime.parse(s).toInstant();
        }
        catch (java.time.format.DateTimeParseException e)
        {
            if (s.matches("^-?\\d+$"))
            {
                try
                {
                    long v = Long.parseLong(s);
                    return (Math.abs(v) > 4_000_000_000L) ? java.time.Instant.ofEpochMilli(v)
                            : java.time.Instant.ofEpochSecond(v);
                }
                catch (NumberFormatException ignore)
                {
                }
            }
            return null;
        }
    }

    // Prefer from topic prefix; else JSON "src"; else normalize bare MAC in device
    private static String _deriveId(final String device, final String topic, final String rawJson)
    {
        if (topic != null)
        {
            int slash = topic.indexOf('/');
            String left = (slash >= 0) ? topic.substring(0, slash) : topic;
            if (!left.isBlank() && left.toLowerCase(java.util.Locale.ROOT).startsWith("shelly"))
                return left.trim();
        }
        String src = _extractJsonString(rawJson, "\"src\"");
        if (src != null && src.toLowerCase(java.util.Locale.ROOT).startsWith("shelly"))
            return src.trim();
        if (device != null)
        {
            String dev = device.trim();
            if (dev.matches("^[0-9A-Fa-f]{12}$"))
                return "shellyhtg3-" + dev.toLowerCase(java.util.Locale.ROOT);
            if (dev.toLowerCase(java.util.Locale.ROOT).startsWith("shelly"))
                return dev;
        }
        return (device != null) ? device.trim() : null;
    }

    private static String _extractJsonString(final String json, final String keyLiteralWithQuotes)
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
                    sb.append(json.charAt(i++));
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

    private static Double _parseDouble(final String s)
    {
        try
        {
            return Double.valueOf(s);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    // Format time label as HH:mm local time
    private static String _fmtTime(final java.time.ZonedDateTime zdt)
    {
        return zdt.toLocalTime().toString().substring(0, 5); // "HH:mm"
    }

    private static String _fmtNum(final double v)
    {
        // Use Locale.ROOT to ensure '.' decimal
        return String.format(java.util.Locale.ROOT, "%.2f", v);
    }

    private static String escapeHtml(final String s)
    {
        if (s == null)
            return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String jsEscape(final String s)
    {
        if (s == null)
            return "";
        return s.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\r", "\\r");
    }

}
