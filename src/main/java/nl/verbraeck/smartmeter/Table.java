package nl.verbraeck.smartmeter;

import java.util.ArrayList;
import java.util.List;

/**
 * Store and format a 2-column table in HTML. 
 * <p>
 * Copyright (c) 2020-2023 Alexnder Verbraeck, Delft, the Netherlands. All rights reserved. <br>
 * MIT-license. 
 * </p>
 * @author <a href="https://github.com/averbraeck">Alexander Verbraeck</a>
 */
public class Table
{
    /** the keys of the table, in order of display. */
    List<String> keys = new ArrayList<>();

    /** the values of the table, in order of display. */ 
    List<String> values = new ArrayList<>();

    /**
     * Add a row with two column values.
     * @param key String; the key in column 1
     * @param value String; the value in column 2
     */
    public void addRow(final String key, final String value)
    {
        this.keys.add(key);
        this.values.add(value);
    }

    /**
     * Add a row with two column values, where the 2nd column has a unit. The unit will be attached as '[unit]' to the value. 
     * @param key String; the key in column 1
     * @param value String; the value in column 2
     * @param unit String; the unit of the value in column 2
     */
    public void addRow(final String key, final String value, final String unit)
    {
        this.keys.add(key);
        this.values.add(value + " [" + unit + "]");
    }

    /**
     * Add a row with two column values, where the 2nd column has a unit. The unit will be attached as '[unit]' to the value. 
     * @param key String; the key in column 1
     * @param value double; the value in column 2
     * @param unit String; the unit of the value in column 2
     */
    public void addRow(final String key, final double value)
    {
        addRow(key, Double.toString(value));
    }

    /**
     * Add a row with two column values, where the 2nd column has a unit. The unit will be attached as '[unit]' to the value. 
     * @param key String; the key in column 1
     * @param value double; the value in column 2
     * @param unit String; the unit of the value in column 2
     */
    public void addRow(final String key, final double value, final String unit)
    {
        addRow(key, Double.toString(value), unit);
    }

    /**
     * Add a row with two column values, where the 2nd column has a unit. The unit will be attached as '[unit]' to the value. 
     * @param key String; the key in column 1
     * @param value int; the value in column 2
     * @param unit String; the unit of the value in column 2
     */
    public void addRow(final String key, final int value)
    {
        addRow(key, Integer.toString(value));
    }

    /**
     * Add a row with two column values, where the 2nd column has a unit. The unit will be attached as '[unit]' to the value. 
     * @param key String; the key in column 1
     * @param value int; the value in column 2
     * @param unit String; the unit of the value in column 2
     */
    public void addRow(final String key, final int value, final String unit)
    {
        addRow(key, Integer.toString(value), unit);
    }

    /**
     * Format the table in HTML.
     * @return String; the HTML-formatted table
     */
    public String table()
    {
        StringBuilder msg = new StringBuilder();
        msg.append("<table class=\"table table-striped table-condensed\">\n");
        for (int i = 0; i < this.keys.size(); i++)
        {
            msg.append("  <tr>\n");
            msg.append("    <td style=\"width:50%\">");
            msg.append(this.keys.get(i));
            msg.append("</td>\n");
            msg.append("    <td style=\"width:50%\">");
            msg.append(this.values.get(i));
            msg.append("</td>\n");
            msg.append("  </tr>\n");
        }
        msg.append("</table>\n");
        return msg.toString();
    }
}
