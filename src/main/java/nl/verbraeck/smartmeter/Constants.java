package nl.verbraeck.smartmeter;

/**
 * Constants for the web server.
 * <p>
 * Copyright (c) 2020-2023 Alexnder Verbraeck, Delft, the Netherlands. All rights reserved. <br>
 * MIT-license.
 * </p>
 * @author <a href="https://github.com/averbraeck">Alexander Verbraeck</a>
 */
public class Constants
{
    /** The address of the server. */
    public static final String SERVER_ADDRESS = "http://192.168.187.99";

    /** the port of the server. */
    public static final int SERVER_PORT = 3000;

    /** the local folder of the daily files with the telegrams. */
    public static final String LOCAL_FOLDER = "E:/jar/meter";
    // public static final String LOCAL_FOLDER = "/home/alexandv/meter";

    /** the prefix of the telegram files. */
    public static final String FILE_PREFIX = "meter_";

    /** the suffix of the telegram files. */
    public static final String FILE_SUFFIX = ".txt";

    /** whether to use caching or not. */
    public static final boolean DATA_CACHING = true;

}
