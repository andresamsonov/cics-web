package com.example.cicsapp;

import com.ibm.ctg.client.ECIRequest;
import com.ibm.ctg.client.JavaGateway;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CicsService {
    private static final Logger logger = LoggerFactory.getLogger(CicsService.class);
    private static final String CICS_HOST = "10.25.2.66";
    private static final int CICS_PORT = 2006;
    private static final String CICS_SERVER = "CICSTS56";
    private static final String PROGRAM_NAME = "CICSUNI2";
    private static final Charset EBCDIC = Charset.forName("CP037");

    public String performOperation(String operation, String key, String pass, String role) throws Exception {
        logger.info("Performing operation: {} with key: {}", operation, key);
        JavaGateway jg = null;
        try {
            jg = new JavaGateway(CICS_HOST, CICS_PORT);
            byte[] commarea = new byte[103];
            System.arraycopy(operation.getBytes(EBCDIC), 0, commarea, 0, 1);
            System.arraycopy(key.getBytes(EBCDIC), 0, commarea, 1, Math.min(10, key.length()));
            if (operation.equals("2")) {
                System.arraycopy(pass.getBytes(EBCDIC), 0, commarea, 11, 10);
                System.arraycopy(role.getBytes(EBCDIC), 0, commarea, 21, 10);
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
            jg.flow(eci);

            if (eci.getRc() == 0 && eci.Commarea != null) {
                String respCode = new String(eci.Commarea, 31, 2, EBCDIC).trim();
                String dataOut = new String(eci.Commarea, 33, 70, EBCDIC).trim();
                logger.info("Operation {} succeeded: Response code: {}, Data: {}", operation, respCode, dataOut);
                return "Response code: " + respCode + ", Data: " + dataOut;
            } else {
                logger.error("Operation {} failed with return code: {}", operation, eci.getRc());
                return "Failed with return code: " + eci.getRc();
            }
        } finally {
            if (jg != null) {
                jg.close();
            }
        }
    }

    public List<String> browseAllRecords() throws Exception {
        logger.info("Starting browseAllRecords");
        List<String> records = new ArrayList<>();
        String nextKey = "          "; // Начало с пробелов
        JavaGateway jg = null;
        int maxIterations = 100; // Ограничение на случай ошибки

        try {
            jg = new JavaGateway(CICS_HOST, CICS_PORT);
            for (int i = 0; i < maxIterations && !nextKey.trim().equals("END"); i++) {
                byte[] commarea = new byte[103];
                System.arraycopy("4".getBytes(EBCDIC), 0, commarea, 0, 1);
                System.arraycopy(nextKey.getBytes(EBCDIC), 0, commarea, 1, 10);

                ECIRequest eci = new ECIRequest(
                    ECIRequest.ECI_SYNC,
                    CICS_SERVER,
                    null,
                    null,
                    PROGRAM_NAME,
                    null,
                    commarea
                );
                logger.info("Sending BROWSE request with key: {}", nextKey);
                jg.flow(eci);

                if (eci.getRc() == 0 && eci.Commarea != null) {
                    String respCode = new String(eci.Commarea, 31, 2, EBCDIC).trim();
                    logger.info("BROWSE response code: {}", respCode);
                    if (respCode.equals("00")) {
                        String record = new String(eci.Commarea, 33, 30, EBCDIC).trim();
                        nextKey = new String(eci.Commarea, 63, 10, EBCDIC).trim();
                        logger.info("Record: {}, Next key: {}", record, nextKey);
                        if (!record.equals("NO RECORDS FOUND")) {
                            records.add(record);
                        }
                    } else {
                        logger.error("BROWSE failed with response code: {}", respCode);
                        records.add("Error: Response code " + respCode);
                        break;
                    }
                } else {
                    logger.error("BROWSE failed with return code: {}", eci.getRc());
                    records.add("Failed with return code: " + eci.getRc());
                    break;
                }
            }
            logger.info("Browse completed with {} records", records.size());
            return records;
        } finally {
            if (jg != null) {
                jg.close();
            }
        }
    }
}