package liquibase.ext.filechangelog;

import liquibase.change.CheckSum;
import liquibase.changelog.AbstractChangeLogHistoryService;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.OfflineChangeLogHistoryService;
import liquibase.changelog.RanChangeSet;
import liquibase.database.Database;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.exception.UnexpectedLiquibaseException;
import liquibase.servicelocator.LiquibaseService;
import liquibase.util.ISODateFormat;
import liquibase.util.LiquibaseUtil;
import liquibase.util.csv.CSVReader;
import liquibase.util.csv.CSVWriter;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@LiquibaseService(skip = false)
public class FileChangeLogHistoryService extends OfflineChangeLogHistoryService {

    public FileChangeLogHistoryService() {
        super(null, Config.getInstance().getChangeLogHistoryFile(), false);
    }

    @Override
    public int getPriority() {
        return 1000;
    }


    @Override
    public boolean supports(Database database) {
        return Config.getInstance().isEnabled();
    }

}
