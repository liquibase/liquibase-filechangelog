package liquibase.ext.filechangelog;

import liquibase.exception.UnexpectedLiquibaseException;

import java.io.File;
import java.io.IOException;

public class Config {
    public static final String CONFIG_PROPERTY_BASE = "liquibase.ext.filechangelog";
    private static Config instance = new Config();

    public static Config getInstance() {
        return instance;
    }

    private Config() {
    }

    public boolean isEnabled() {
        String enabledProperty = System.getProperty(CONFIG_PROPERTY_BASE +".enabled");
        if (enabledProperty == null) {
            return true;
        }
        return Boolean.valueOf(enabledProperty);
    }

    public File getChangeLogHistoryFile() {
        String fileProperty = System.getProperty(CONFIG_PROPERTY_BASE +".file");
        if (fileProperty == null) {
            fileProperty = "./databasechangelog.csv";
        }
        try {
            return new File(fileProperty).getCanonicalFile();
        } catch (IOException e) {
            throw new UnexpectedLiquibaseException(e);
        }
    }
}
