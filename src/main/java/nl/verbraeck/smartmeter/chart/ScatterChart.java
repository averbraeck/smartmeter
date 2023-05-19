package nl.verbraeck.smartmeter.chart;

import java.util.ArrayList;
import java.util.List;

/**
 * Formatting a data series as a scatter plot in the browser, using the chart.js library.
 * <p>
 * Copyright (c) 2020-2023 Alexander Verbraeck, Delft, the Netherlands. All rights reserved. <br>
 * MIT-license.
 * </p>
 * @author <a href="https://github.com/averbraeck">Alexander Verbraeck</a>
 */
public class ScatterChart
{
    /** the name of the chart, to be used in the HTML code to link the javascript and the placeholder. */
    private final String chartName;

    /** the title to print at the top of the chart. */
    private String title;

    /** the width to use in the current element. */
    private String width = "100%";

    /** the x values of the scatter plot. */
    private List<Double> x = new ArrayList<>();

    /** the y values of the scatter plot. */
    private List<Double> y = new ArrayList<>();

    /**
     * Make a scatter plot in a div, where the name is used in the HTML code to link the javascript and the placeholder.
     * @param chartName unique name of the chart in the HTML file
     */
    public ScatterChart(final String chartName)
    {
        this.chartName = chartName;
    }

    /**
     * Make the 'script' part of the HTML page, to be placed in a sequence of scripts for the page. The data for the graph is
     * provided in JSON format.
     * @return String with the 'script' part of the HTML page
     */
    public String toScriptHtml()
    {
        StringBuilder msg = new StringBuilder();
        msg.append("\n<script>\n");
        msg.append("  var ctx = document.getElementById('");
        msg.append(this.chartName);
        msg.append("').getContext('2d');\n");
        msg.append("  var chart" + this.chartName + " = new Chart(ctx, {\n");
        msg.append("    type: 'scatter',\n");
        msg.append("    data: {\n");
        msg.append("      datasets: [{\n");
        msg.append("        label: '");
        msg.append(this.title);
        msg.append("',\n");
        msg.append("        data: [");
        for (int i = 0; i < this.x.size(); i++)
        {
            if (i > 0)
                msg.append(", ");
            msg.append("{ x: ");
            msg.append(this.x.get(i));
            msg.append(", y: ");
            msg.append(this.y.get(i));
            msg.append("}");
        }
        msg.append("]\n");
        msg.append("      }]\n");
        msg.append("    },\n");
        msg.append("    options: {\n");
        msg.append("        scales: {\n");
        msg.append("            xAxes: [{\n");
        msg.append("                type: 'linear',\n");
        msg.append("                position: 'bottom'\n");
        msg.append("            }]\n");
        msg.append("        }\n");
        msg.append("    }\n");
        msg.append("  });\n");
        msg.append("</script>\n\n");
        return msg.toString();
    }

    /**
     * Make the 'div' part of the HTML page, at the location where the graph is to be placed.
     * @return String with the 'div' part of the HTML page
     */
    public String toDivHtml()
    {
        StringBuilder msg = new StringBuilder();
        msg.append("<div style=\"width:");
        msg.append(this.width);
        msg.append(";\">\n");
        msg.append("  <canvas id=\"");
        msg.append(this.chartName);
        msg.append("\" width=\"200\" height=\"100\" style=\"border:1px solid #000000;\"></canvas>\n");
        msg.append("</div>\n");
        return msg.toString();
    }

    /**
     * Set the width to change it from the default 100%. The method calls can be chained.
     * @param width String; the new width to use (as px, %, or other HTML5 unit).
     * @return BarChart for chaining the method calls
     */
    public ScatterChart setWidth(final String width)
    {
        this.width = width;
        return this;
    }

    /**
     * Set the x-values of the points to use for the scatter plot. The method calls can be chained.
     * @param x List&lt;Double&gt;; the x values of the scatter plot
     * @return ScatterChart for chaining the method calls
     */
    public ScatterChart setX(final List<Double> x)
    {
        this.x = x;
        return this;
    }

    /**
     * Set the y-values of the points to use for the scatter plot. The method calls can be chained.
     * @param y List&lt;Double&gt;; the y values of the scatter plot
     * @return ScatterChart for chaining the method calls
     */
    public ScatterChart setY(final List<Double> y)
    {
        this.y = y;
        return this;
    }

    /**
     * Set the title of the scatter plot, to be printed at the top. The method calls can be chained.
     * @param title String; the title of the scatter plot, to be printed at the top
     * @return ScatterChart for chaining the method calls
     */
    public ScatterChart setTitle(final String title)
    {
        this.title = title;
        return this;
    }

}
