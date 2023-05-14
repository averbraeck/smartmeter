package nl.verbraeck.smartmeter.chart;

import java.util.ArrayList;
import java.util.List;

public class LineChart
{
    private final String chartName;
    private String label;
    private String width="100%";
    private List<Double> x = new ArrayList<>();
    private List<Double> y = new ArrayList<>();
    private double max = Double.NaN;
    private int ticks = -1;
    private boolean hours = false;
    private boolean fill = false;
    private String fillColor = "blue";
    
    /**
     * Make a line chart in a div.
     * @param chartName unique name of the chart in the HTML file
     */
    public LineChart(final String chartName)
    {
        this.chartName = chartName;
    }

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
        msg.append(this.label);
        msg.append("',\n");
        msg.append("        pointRadius: 0,\n");
        msg.append("        stepped: 'before',\n");
        msg.append("        data: [");
        for (int i=0; i < this.x.size(); i++)
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
        if (!Double.isNaN(this.max))
            msg.append("                max: " + this.max + ",\n");
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
        }
        if (this.ticks > 0)
            msg.append("                    stepSize: 60,\n");
        msg.append("                    includeBounds: true,\n");
        msg.append("                    minRotation: 90,\n");
        msg.append("                    maxRotation: 90,\n");
        msg.append("                    min: 0,\n");
        if (!Double.isNaN(this.max))
            msg.append("                    max: " + this.max + ",\n");
        msg.append("                }\n");
        msg.append("            }]\n");
        msg.append("        }\n");
        msg.append("    }\n");
        msg.append("  });\n");
        msg.append("</script>\n\n");
        return msg.toString();
    }

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

    public LineChart setWidth(String width)
    {
        this.width = width;
        return this;
    }

    public LineChart setX(List<Double> x)
    {
        this.x = x;
        return this;
    }

    public LineChart setY(List<Double> y)
    {
        this.y = y;
        return this;
    }
    
    public LineChart setLabel(String label)
    {
        this.label = label;
        return this;
    }
    
    public LineChart setMax(double max)
    {
        this.max = max;;
        return this;
    }
    
    public LineChart setTicks(int ticks)
    {
        this.ticks = ticks;
        return this;
    }
    
    public LineChart setHours(boolean hours)
    {
        this.hours = hours;
        return this;
    }
    
    public LineChart setFill(boolean fill)
    {
        this.fill = fill;
        return this;
    }
    
    public LineChart setFillColor(String fillColor)
    {
        this.fillColor = fillColor;
        return this;
    }
    
}
