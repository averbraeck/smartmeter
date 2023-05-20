package nl.verbraeck.smartmeter;

/**
 * Store and format a 2-column table in HTML.
 * <p>
 * Copyright (c) 2020-2023 Alexander Verbraeck, Delft, the Netherlands. All rights reserved. <br>
 * MIT-license.
 * </p>
 * @author <a href="https://github.com/averbraeck">Alexander Verbraeck</a>
 */
public class Matrix
{
    /** the values of the matrix, in order of display. */
    private final String[][] matrix;

    /** number of columns. */
    private final int cols;

    /** number of rows. */
    private final int rows;

    /**
     * Create an empty matrix.
     * @param cols int; number of columns
     * @param rows int; number of rows
     */
    public Matrix(final int cols, final int rows)
    {
        this.matrix = new String[cols][rows];
        for (int c = 0; c < cols; c++)
            for (int r = 0; r < rows; r++)
                this.matrix[c][r] = "";
        this.cols = cols;
        this.rows = rows;
    }

    /**
     * Add a value to the column (x) and row (y)
     * @param col int x
     * @param row int y
     * @param value String value to put in the matrix
     */
    public void setValue(final int col, final int row, final String value)
    {
        this.matrix[col][row] = value;
    }

    /**
     * Add a value to the column (x) and row (y)
     * @param col int x
     * @param row int y
     * @param value int value to put in the matrix
     */
    public void setValue(final int col, final int row, final int value)
    {
        this.matrix[col][row] = Integer.toString(value);
    }

    /**
     * Add a value to the column (x) and row (y)
     * @param col int x
     * @param row int y
     * @param value double value to put in the matrix
     */
    public void setValue(final int col, final int row, final double value)
    {
        this.matrix[col][row] = Double.toString(value);
    }

    /**
     * Format the table in HTML.
     * @return String; the HTML-formatted table
     */
    public String table()
    {
        StringBuilder msg = new StringBuilder();
        msg.append("<table class=\"table table-striped table-condensed\" width=\"100%\">\n");
        for (int r = 0; r < this.rows; r++)
        {
            msg.append("  <tr>\n");
            for (int c = 0; c < this.cols; c++)
            {
                msg.append("    <td>");
                msg.append(this.matrix[c][r]);
                msg.append("</td>\n");
            }
            msg.append("  </tr>\n");
        }
        msg.append("</table>\n");
        return msg.toString();
    }
}
