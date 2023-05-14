package nl.verbraeck.smartmeter;

import java.util.ArrayList;
import java.util.List;

public class Table
{
    List<String> keys = new ArrayList<>();

    List<String> values = new ArrayList<>();

    public void addRow(final String key, final String value)
    {
        this.keys.add(key);
        this.values.add(value);
    }

    public void addRow(final String key, final String value, final String unit)
    {
        this.keys.add(key);
        this.values.add(value + " [" + unit + "]");
    }

    public void addRow(final String key, final double value)
    {
        addRow(key, Double.toString(value));
    }

    public void addRow(final String key, final double value, final String unit)
    {
        addRow(key, Double.toString(value), unit);
    }

    public void addRow(final String key, final int value)
    {
        addRow(key, Integer.toString(value));
    }

    public void addRow(final String key, final int value, final String unit)
    {
        addRow(key, Integer.toString(value), unit);
    }

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
