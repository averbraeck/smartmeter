package nl.verbraeck.smartmeter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Telegram contains one P1-message from the smart meter. E.g., based on:
 * 
 * <pre>
 * /XMX5LGBBLA4415473347
 * 1-3:0.2.8(50)
 * 0-0:1.0.0(230505000259S)
 * 0-0:96.1.1(4530303435303034303832303939373137)
 * 1-0:1.8.1(010325.485*kWh)
 * 1-0:1.8.2(007339.152*kWh)
 * 1-0:2.8.1(000000.000*kWh)
 * 1-0:2.8.2(000000.000*kWh)
 * 0-0:96.14.0(0001)
 * 1-0:1.7.0(02.327*kW)
 * 1-0:2.7.0(00.000*kW)
 * 0-0:96.7.21(00003)
 * 0-0:96.7.9(00000)
 * 1-0:99.97.0(0)(0-0:96.7.19)
 * 1-0:32.32.0(00011)
 * 1-0:32.36.0(00000)
 * 0-0:96.13.0()
 * 1-0:32.7.0(228.0*V)
 * 1-0:31.7.0(010*A)
 * 1-0:21.7.0(02.327*kW)
 * 1-0:22.7.0(00.000*kW)
 * 0-1:24.1.0(003)
 * 0-1:96.1.0(4730303339303031383033353931323138)
 * 0-1:24.2.1(230505000003S)(05125.733*m3)
 * !F07C
 * </pre>
 * <p>
 * Copyright (c) 2020-2023 Alexander Verbraeck.
 * </p>
 * @author <a href="https://www.tudelft.nl/averbraeck">Alexander Verbraeck</a>
 */
public class Telegram
{
    /** 1-3:0.2.8 version, e.g. (50). */
    public int version;

    /** 0-0:1.0.0 date and time, e.g. (200815221959S). */
    public LocalDate date;

    /** 0-0:1.0.0 date and time, e.g. (200815221959S). */
    public LocalTime time;

    /** 0-0:96.1.1 meter id in hex, e.g. (4530303435303034303832303939373137). */
    public String electricityMeterId = "";

    /** 1-0:1.8.1 electricity delivered tariff 1, e.g. (004391.190*kWh). */
    public double electricityTariff1kWh;

    /** 1-0:1.8.2 electricity delivered tariff 2, e.g. (002577.851*kWh). */
    public double electricityTariff2kWh;

    /** 1-0:1.8.1 electricity delivered back tariff 1, e.g. (000000.000*kWh). */
    public double electrBackTariff1kWh;

    /** 1-0:1.8.2 electricity delivered back tariff 2, e.g. (000000.000*kWh). */
    public double electrBackTariff2kWh;

    /** 0-0:96.14.0 current tariff, e.g. (0001). */
    public int tariff;

    /** 1-0:1.7.0 power delivered, e.g. (00.471*kW). */
    public double powerDeliveredkW;

    /** 1-0:2.7.0 power received, e.g. (00.000*kW). */
    public double powerReceivedkW;

    /** 0-0:96.7.21 number of power failures in any phase, e.g. (00003). */
    public int powerFailuresAnyPhase;
    
    /** 0-0:96.7.9 number of long power failures in any phase, e.g. (00000). */
    public int longPowerFailuresAnyPhase;

    /** 1-0:32.32.0 number of voltage sags in phase L1, e.g. (00011). */
    public int voltageSagsL1;
    
    /** 1-0:52.32.0 number of voltage sags in phase L2, e.g. (00000). */
    public int voltageSagsL2;
    
    /** 1-0:72.32.0 number of voltage sags in phase L3, e.g. (00000). */
    public int voltageSagsL3;
    
    /** 1-0:32.36.0 number of voltage swells in phase L1, e.g. (00000). */
    public int voltageSwellsL1;
    
    /** 1-0:52.36.0 number of voltage swells in phase L2, e.g. (00000). */
    public int voltageSwellsL2;
    
    /** 1-0:72.36.0 number of voltage swells in phase L3, e.g. (00000). */
    public int voltageSwellsL3;
    
    /** 0-0:96.13.0 text message. */
    public String textMessage = "";
    
    /** 1-0:32.7.0 instantaneous voltage L1, e.g. (221.0*V). */
    public double voltageL1;

    /** 1-0:52.7.0 instantaneous voltage L2, e.g. (221.0*V). */
    public double voltageL2;

    /** 1-0:72.7.0 instantaneous voltage L3, e.g. (221.0*V). */
    public double voltageL3;

    /** 1-0:31.7.0 instantaneous current L1, e.g. (004*A). */
    public double currentL1;

    /** 1-0:51.7.0 instantaneous current L1, e.g. (004*A). */
    public double currentL2;

    /** 1-0:71.7.0 instantaneous current L1, e.g. (004*A). */
    public double currentL3;

    /** 1-0:21.7.0 instantaneous L1 power delivered (+P), e.g. (00.471*kW). */
    public double powerDeliveredL1kW;

    /** 1-0:41.7.0 instantaneous L2 power delivered (+P), e.g. (00.471*kW). */
    public double powerDeliveredL2kW;

    /** 1-0:61.7.0 instantaneous L3 power delivered (+p), e.g. (00.471*kW). */
    public double powerDeliveredL3kW;

    /** 1-0:22.7.0 instantaneous L1 power received (-P), e.g. (00.000*kW). */
    public double powerReceivedL1kW;

    /** 1-0:42.7.0 instantaneous L2 power received (-P), e.g. (00.000*kW). */
    public double powerReceivedL2kW;

    /** 1-0:62.7.0 instantaneous L3 power received (-P), e.g. (00.000*kW). */
    public double powerReceivedL3kW;

    /** 0-1:24.1.0 device type id, e.g. (003). */
    public int gasDeviceTypeId;
    
    /** 0-1:96.1.0 gas meter id in hex, e.g. (4730303339303031383033353931323138). */
    public String gasMeterId = "";

    /** 0-1:24.2.1 capture date for gas -- first string (230505000003S)(05125.733*m3). */
    public LocalDate gasCaptureDate;

    /** 0-1:24.2.1 capture time for gas -- first string (230505000003S)(05125.733*m3). */
    public LocalTime gasCaptureTime;

    /** 0-1:24.2.1 gas delivered in m3 -- second string (230505000003S)(05125.733*m3). */
    public double gasDeliveredM3;

    /**
     * @return the date and time as yyyyMMdd HH:mm
     */
    public String getDateTime()
    {
        return this.date.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + " "
                + this.time.format(DateTimeFormatter.ofPattern("HH:mm"));
    }
}
