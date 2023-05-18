package nl.verbraeck.smartmeter.chart;

import java.util.ArrayList;
import java.util.List;

/**
 * Formatting a data series as a line chart in the browser, using the chart.js library.
 * <p>
 * Copyright (c) 2020-2023 Alexnder Verbraeck, Delft, the Netherlands. All rights reserved. <br>
 * MIT-license.
 * </p>
 * @author <a href="https://github.com/averbraeck">Alexander Verbraeck</a>
 */
public class LineChart
{
    /** the name of the chart, to be used in the HTML code to link the javascript and the placeholder. */
    private final String chartName;

    /** the title to print at the top of the chart. */
    private String title;

    /** the width to use in the current element. */
    private String width = "100%";

    /** the x values of the line chart. */
    private List<Double> x = new ArrayList<>();

    /** the y values of the line chart. */
    private List<Double> y = new ArrayList<>();

    /** If a maxX value is to be used, provide the max value for the x-axis in this field. */
    private double maxX = Double.NaN;

    /** Step size for the ticks to use. -1 means automatic. */
    private int tickStepSize = -1;

    /** hours indicates whether the x-axis contains hours of the day. */
    private boolean hours = false;

    /** Whether to fill the graph or not. */
    private boolean fill = false;

    /** The HTML5 fill color to use in case the graph is to be filled. */
    private String fillColor = "blue";

    /**
     * Make a line chart in a div, where the name is used in the HTML code to link the javascript and the placeholder.
     * @param chartName unique name of the chart in the HTML file
     */
    public LineChart(final String chartName)
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
        msg.append("    type: 'line',\n");
        msg.append("    data: {\n");
        msg.append("      datasets: [{\n");
        if (this.fill)
        {
            msg.append("        fill: 'origin',\n");
            msg.append("        backgroundColor: '" + this.fillColor + "',\n");
        }
        else
        {
            msg.append("        fill: false,\n");
            msg.append("        borderColor: '" + this.fillColor + "',\n");
        }
        msg.append("        label: '");
        msg.append(this.title);
        msg.append("',\n");
        msg.append("        pointRadius: 0,\n");
        msg.append("        stepped: 'before',\n");
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
        msg.append("            x: {\n");
        msg.append("                min: 0,\n");
        if (!Double.isNaN(this.maxX))
            msg.append("                max: " + this.maxX + ",\n");
        msg.append("            },\n");
        msg.append("            xAxes: [{\n");
        msg.append("                type: 'linear',\n");
        msg.append("                position: 'bottom',\n");
        msg.append("                maxTicksLimit: 25,\n");
        msg.append("                includeBounds: true,\n");
        msg.append("                ticks: {\n");
        if (this.hours)
        {
            msg.append("                    callback: function(val, index) {\n");
            msg.append("                        return ' ' + index + ':00';\n");
            msg.append("                    },\n");
            msg.append("                    stepSize: 60,\n");
        }
        else if (this.tickStepSize > 0)
        {
            msg.append("                    stepSize: " + this.tickStepSize + ",\n");
        }
        msg.append("                    includeBounds: true,\n");
        msg.append("                    minRotation: 90,\n");
        msg.append("                    maxRotation: 90,\n");
        msg.append("                    min: 0,\n");
        if (!Double.isNaN(this.maxX))
            msg.append("                    max: " + this.maxX + ",\n");
        msg.append("                }\n");
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
    public LineChart setWidth(final String width)
    {
        this.width = width;
        return this;
    }

    /**
     * Set the x-values of the points to use for the line chart. The method calls can be chained.
     * @param x List&lt;Double&gt;; the x values of the line chart
     * @return LineChart for chaining the method calls
     */
    public LineChart setX(final List<Double> x)
    {
        this.x = x;
        return this;
    }

    /**
     * Set the y-values of the points to use for the line chart. The method calls can be chained.
     * @param y List&lt;Double&gt;; the y values of the line chart
     * @return LineChart for chaining the method calls
     */
    public LineChart setY(final List<Double> y)
    {
        this.y = y;
        return this;
    }

    /**
     * Set the title of the line chart, to be printed at the top. The method calls can be chained.
     * @param title String; the title of the line chart, to be printed at the top
     * @return LineChart for chaining the method calls
     */
    public LineChart setTitle(final String title)
    {
        this.title = title;
        return this;
    }

    /**
     * Set the maximum x-value of the line chart, e.g. 1440 for the number of minutes in a day. The method calls can be chained.
     * @param maxX double; the maximum x-value of the line chart
     * @return LineChart for chaining the method calls
     */
    public LineChart setMax(final double maxX)
    {
        this.maxX = maxX;
        return this;
    }

    /**
     * Set the tick step size for the x-axis of the line chart. The method calls can be chained.
     * @param tickStepSize int; the tick step size for the x-axis of the line chart
     * @return LineChart for chaining the method calls
     */
    public LineChart setTickStepSize(final int tickStepSize)
    {
        this.tickStepSize = tickStepSize;
        return this;
    }

    /**
     * Set whether the x-axis is in minutes and has to be displayed in hours. The method calls can be chained.
     * @param hours boolean; whether the x-axis is in minutes and has to be displayed in hours
     * @return LineChart for chaining the method calls
     */
    public LineChart setHours(final boolean hours)
    {
        this.hours = hours;
        return this;
    }

    /**
     * Set whether the line chart has to be filled. The method calls can be chained.
     * @param fill boolean; whether the line chart has to be filled
     * @return LineChart for chaining the method calls
     */
    public LineChart setFill(final boolean fill)
    {
        this.fill = fill;
        return this;
    }

    /**
     * Set the fill color as a valid HTML5 color. The method calls can be chained.
     * @param fillColor String; the fill color as a valid HTML5 color
     * @return LineChart for chaining the method calls
     */
    public LineChart setFillColor(final String fillColor)
    {
        this.fillColor = fillColor;
        return this;
    }

}
