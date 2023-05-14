package nl.verbraeck.smartmeter.chart;

import java.util.ArrayList;
import java.util.List;

public class ScatterChart
{
    private final String chartName;
    private String label;
    private String width="100%";
    private List<Double> x = new ArrayList<>();
    private List<Double> y = new ArrayList<>();
    
    /**
     * Make a barchart in a div.
     * @param chartName unique name of the chart in the HTML file
     */
    public ScatterChart(final String chartName)
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
        msg.append("    type: 'scatter',\n");
        msg.append("    data: {\n");
        msg.append("      datasets: [{\n");
        msg.append("        label: '");
        msg.append(this.label);
        msg.append("',\n");
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

    public ScatterChart setWidth(String width)
    {
        this.width = width;
        return this;
    }

    public ScatterChart setX(List<Double> x)
    {
        this.x = x;
        return this;
    }

    public ScatterChart setY(List<Double> y)
    {
        this.y = y;
        return this;
    }
    
    public ScatterChart setLabel(String label)
    {
        this.label = label;
        return this;
    }
    
    
}
