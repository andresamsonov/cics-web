package com.example.cicsapp;

import com.ibm.ctg.client.ECIRequest;
import com.ibm.ctg.client.JavaGateway;
import java.io.IOException;
import java.nio.charset.Charset;

public class CTest {
    private static final String CICS_HOST = "10.25.2.66";
    private static final int CICS_PORT = 2006;
    private static final String CICS_SERVER = "CICSTS56";
    private static final String PROGRAM_NAME = "CICSUNI2";
    private static final Charset EBCDIC = Charset.forName("CP037");

    public static void main(String[] args) {
        String operation = args.length > 0 ? args[0] : "2";
        String key = args.length > 1 ? args[1] : "TESTKEY002";
        String data1 = args.length > 2 ? String.format("%-10s", args[2]) : "NEW_PASS  ";
        String data2 = args.length > 3 ? String.format("%-10s", args[3]) : "NEW_ROLE  ";

        JavaGateway jg = null;
        try {
            System.out.println("Attempting to connect to " + CICS_HOST + ":" + CICS_PORT);
            jg = new JavaGateway(CICS_HOST, CICS_PORT);
            System.out.println("JavaGateway connected successfully");

            byte[] commarea = new byte[103];
            System.arraycopy(operation.getBytes(EBCDIC), 0, commarea, 0, 1);
            System.arraycopy(key.getBytes(EBCDIC), 0, commarea, 1, Math.min(10, key.length()));
            if (operation.equals("2")) {
                System.arraycopy(data1.getBytes(EBCDIC), 0, commarea, 11, 10);
                System.arraycopy(data2.getBytes(EBCDIC), 0, commarea, 21, 10);
            }

            ECIRequest eci = new ECIRequest(
                ECIRequest.ECI_SYNC,
                CICS_SERVER,
                null,
                null,
                PROGRAM_NAME,
                null,
                commarea
            );
            System.out.println("ECIRequest created: server=" + CICS_SERVER + ", program=" + PROGRAM_NAME);

            jg.flow(eci);
            System.out.println("ECI request sent");

            int rc = eci.getRc();
            System.out.println("Return code: " + rc);
            if (rc == 0) {
                System.out.println("Request succeeded");
                if (eci.Commarea != null) {
                    String respCode = new String(eci.Commarea, 31, 2, EBCDIC).trim();
                    String dataOut = new String(eci.Commarea, 33, 70, EBCDIC).trim();
                    System.out.println("Response code: " + respCode);
                    System.out.println("Data returned: " + dataOut);
                } else {
                    System.out.println("No commarea returned");
                }
            } else {
                System.out.println("ECI request failed with return code: " + rc);
                if (rc == ECIRequest.ECI_ERR_NO_CICS) {
                    System.err.println("Error: No CICS system found for server " + CICS_SERVER);
                } else if (rc == ECIRequest.ECI_ERR_SYSTEM_ERROR) {
                    System.err.println("Error: System error in Gateway or CICS");
                } else if (rc == ECIRequest.ECI_ERR_INVALID_CALL_TYPE) {
                    System.err.println("Error: Invalid call type for this configuration");
                } else if (rc == ECIRequest.ECI_ERR_SECURITY_ERROR) {
                    System.err.println("Error: Security violation occurred");
                } else if (rc == -9) {
                    System.err.println("Error: User not authorized");
                }
            }
        } catch (IOException e) {
            System.err.println("IOException: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("Unexpected error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (jg != null) {
                try {
                    jg.close();
                    System.out.println("JavaGateway closed");
                } catch (Exception e) {
                    System.err.println("Error closing JavaGateway: " + e.getMessage());
                }
            }
        }
    }
}