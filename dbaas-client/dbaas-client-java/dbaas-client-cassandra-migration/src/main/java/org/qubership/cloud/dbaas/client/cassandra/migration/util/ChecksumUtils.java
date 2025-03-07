package org.qubership.cloud.dbaas.client.cassandra.migration.util;

import org.qubership.cloud.dbaas.client.cassandra.migration.exception.SchemaMigrationException;
import org.qubership.cloud.dbaas.client.cassandra.migration.model.SchemaVersionContentChecksum;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.CRC32;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ChecksumUtils {
    public static final String UTF_8_BOM = "\uFEFF";

    public static SchemaVersionContentChecksum readContentAndCalculateChecksum(String scriptResourcePath) {
        CRC32 crc32 = new CRC32();
        try (InputStream is = ResourceUtils.openInputStreamForResource(scriptResourcePath)) {
            if (is == null) {
                throw new SchemaMigrationException("Failed to load resource for " + scriptResourcePath);
            }
            try (BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(ResourceUtils.openInputStreamForResource(scriptResourcePath))
            )) {
                String contentString;
                StringBuilder stringContentBuilder = new StringBuilder();
                while ((contentString = bufferedReader.readLine()) != null) {
                    stringContentBuilder.append(contentString).append("\n");
                    crc32.update(removeUTF8BOM(contentString).getBytes(UTF_8));
                }
                return new SchemaVersionContentChecksum(stringContentBuilder.toString(), crc32.getValue());
            } catch (IOException e) {
                throw new SchemaMigrationException("Failed to read and calculate checksum for " + scriptResourcePath, e);
            }
        }  catch (IOException e) {
            throw new SchemaMigrationException("Failed to read and calculate checksum for " + scriptResourcePath, e);
        }
    }

    private static String removeUTF8BOM(String s) {
        if (s == null || !s.startsWith(UTF_8_BOM)) {
            return s;
        } else {
            return s.substring(1);
        }
    }
}
