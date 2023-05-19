package nl.verbraeck.smartmeter.chart;

import java.util.ArrayList;
import java.util.List;

/**
 * Formatting a data series as a bar chart in the browser, using the chart.js library.
 * <p>
 * Copyright (c) 2020-2023 Alexnder Verbraeck, Delft, the Netherlands. All rights reserved. <br>
 * MIT-license.
 * </p>
 * @author <a href="https://github.com/averbraeck">Alexander Verbraeck</a>
 */
public class BarChart
{
    /** the name of the chart, to be used in the HTML code to link the javascript and the placeholder. */
    private final String chartName;

    /** the title to print at the top of the chart. */
    private String title;

    /** the width to use in the current element. */
    private String width = "100%";

    /** the labels to use for the x-axis. */
    private List<String> labels = new ArrayList<>();

    /** the heights of the bars, where each value belongs to a label with the same index. */
    private List<Double> values = new ArrayList<>();

    /**
     * Make a barchart in a div, where the name is used in the HTML code to link the javascript and the placeholder.
     * @param chartName unique name of the chart in the HTML file
     */
    public BarChart(final String chartName)
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
        msg.append("    type: 'bar',\n");
        msg.append("    data: {\n");
        msg.append("      labels: [");
        for (int i = 0; i < this.labels.size(); i++)
        {
            if (i > 0)
                msg.append(", ");
            msg.append("'");
            msg.append(this.labels.get(i));
            msg.append("'");
        }
        msg.append("],\n");
        msg.append("      datasets: [{\n");
        msg.append("        label: '");
        msg.append(this.title);
        msg.append("',\n");
        msg.append("        backgroundColor: 'red',\n");
        msg.append("        borderColor: 'black',\n");
        msg.append("        data: [");
        for (int i = 0; i < this.labels.size(); i++)
        {
            if (i > 0)
                msg.append(", ");
            msg.append(this.values.get(i));
        }
        msg.append("],\n");
        msg.append("        borderwidth: 1\n");
        msg.append("      }]\n");
        msg.append("    },\n");
        msg.append("    options: {\n");
        msg.append("        scales: {\n");
        msg.append("            xAxes: [{\n");
        msg.append("                scaleLabel: {\n");
        msg.append("                    display: false,\n");
        msg.append("                    labelString: 'Time',\n");
        msg.append("                },\n");
        msg.append("                ticks: {\n");
        msg.append("                    autoSkip: false,\n");
        msg.append("                    autoSkipPadding: 1,\n");
        msg.append("                    maxTicksLimit: 50,\n");
        msg.append("                    includeBounds: true,\n");
        msg.append("                    minRotation: 90,\n");
        msg.append("                    maxRotation: 90\n");
        msg.append("                }\n");
        msg.append("            }],\n");
        msg.append("            yAxes: [{\n");
        msg.append("                ticks: {\n");
        msg.append("                    beginAtZero: true\n");
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
        msg.append("\" width=\"200px\" height=\"100px\" style=\"border:1px solid #000000;\"></canvas>\n");
        msg.append("</div>\n");
        return msg.toString();
    }

    /**
     * Set the width to change it from the default 100%. The method calls can be chained.
     * @param width String; the new width to use (as px, %, or other HTML5 unit).
     * @return BarChart for chaining the method calls
     */
    public BarChart setWidth(final String width)
    {
        this.width = width;
        return this;
    }

    /**
     * Set the labels for the bar chart. The method calls can be chained.
     * @param labels List&lt;String&gt;; the labels to use for the x-axis
     * @return BarChart for chaining the method calls
     */
    public BarChart setLabels(final List<String> labels)
    {
        this.labels = labels;
        return this;
    }

    /**
     * Set the bar heights for the bar chart. The method calls can be chained.
     * @param values List&lt;Double&gt;; the bar heights to use for the chart; each value belongs to a label with the same index
     * @return BarChart for chaining the method calls
     */
    public BarChart setValues(final List<Double> values)
    {
        this.values = values;
        return this;
    }

    /**
     * Set the title of the bar chart, to be printed at the top. The method calls can be chained.
     * @param title String; the title of the bar chart, to be printed at the top
     * @return BarChart for chaining the method calls
     */
    public BarChart setTitle(final String title)
    {
        this.title = title;
        return this;
    }

}
