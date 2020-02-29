package com.dxfeed.tools;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.dxfeed.config.AppConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class FileTools {

    private final @NonNull AppConfig appConfig;

    public Properties readProperties(String directoryPath) {
        String filename = directoryPath + File.separator + appConfig.getConfigFileName();
        Properties properties = new Properties();
        try (InputStream input = new FileInputStream(filename)) {
            properties.load(input);
        } catch (IOException ex) {
            log.error("Error reading properties from '{}': {}", filename, ex.getMessage());
            if (log.isTraceEnabled())
                ex.printStackTrace();
        }
        return properties;
    }

}
