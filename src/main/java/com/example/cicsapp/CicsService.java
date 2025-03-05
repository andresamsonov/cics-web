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
            byte[] keyBytes = key.getBytes(EBCDIC);
            System.arraycopy(keyBytes, 0, commarea, 1, Math.min(keyBytes.length, 10));
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
        String currentKey = "          "; // Начало с пробелов
        int iteration = 0;

        JavaGateway jg = new JavaGateway(CICS_HOST, CICS_PORT);
        try {
            while (iteration < 10) { // Ограничение для безопасности
                iteration++;
                byte[] commarea = new byte[103];
                System.arraycopy("4".getBytes(EBCDIC), 0, commarea, 0, 1);
                byte[] keyBytes = currentKey.getBytes(EBCDIC);
                System.arraycopy(keyBytes, 0, commarea, 1, Math.min(keyBytes.length, 10));

                logger.info("Iteration {}: Sending BROWSE request with COMM-KEY-IN: '{}'", iteration, currentKey);

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
                    String record = new String(eci.Commarea, 33, 30, EBCDIC).trim();
                    String nextKey = new String(eci.Commarea, 63, 10, EBCDIC).trim();

                    logger.info("Iteration {}: Response code: '{}', Record: '{}', Next key: '{}'", 
                                iteration, respCode, record, nextKey);

                    if (respCode.equals("00")) {
                        if (!record.equals("NO RECORDS FOUND")) {
                            records.add(record);
                        }
                        if (nextKey.equals("END")) {
                            logger.info("Reached END of records");
                            break;
                        } else if (nextKey.equals(currentKey)) {
                            logger.warn("Next key '{}' matches current key '{}', forcing END", nextKey, currentKey);
                            break;
                        }
                        currentKey = String.format("%-10s", nextKey); // Убедимся, что ключ всегда 10 байт
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
                logger.info("JavaGateway closed");
            }
        }
    }
}