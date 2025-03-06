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
            byte[] commarea = new byte[337];
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
                if (operation.equals("4")) {
                    int recordCount = ((eci.Commarea[33] & 0xFF) << 8) + (eci.Commarea[34] & 0xFF);
                    recordCount = Math.min(recordCount, 10);
                    logger.info("Record count from CICS: {}", recordCount);

                    List<String> records = new ArrayList<>();
                    int recordsStart = 35;
                    for (int i = 0; i < recordCount; i++) {
                        int offset = recordsStart + (i * 30);
                        if (offset + 30 <= commarea.length) {
                            String recordKey = new String(eci.Commarea, offset, 10, EBCDIC);
                            String recordPass = new String(eci.Commarea, offset + 10, 10, EBCDIC);
                            String recordRole = new String(eci.Commarea, offset + 20, 10, EBCDIC);
                            logger.info("Raw record {}: key='{}', pass='{}', role='{}'", i + 1, recordKey, recordPass, recordRole);
                            String record = (recordKey + recordPass + recordRole).trim();
                            if (!record.isEmpty()) {
                                records.add(record);
                            }
                        } else {
                            logger.error("Offset {} exceeds commarea length {}", offset, commarea.length);
                            break;
                        }
                    }
                    String result = "Response code: " + respCode + ", Records: " + String.join(" | ", records);
                    logger.info("Browse succeeded: {}", result);
                    return result;
                } else {
                    String dataOut = new String(eci.Commarea, 35, 300, EBCDIC).trim();
                    logger.info("Operation {} succeeded: Response code: {}, Data: {}", operation, respCode, dataOut);
                    return "Response code: " + respCode + ", Data: " + dataOut;
                }
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
        String result = performOperation("4", "          ", "", "");
        String[] parts = result.split("Records: ");
        if (parts.length > 1) {
            return List.of(parts[1].split(" \\| "));
        }
        return new ArrayList<>();
    }
}