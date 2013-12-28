package liquibase.ext.filechangelog;

import liquibase.change.CheckSum;
import liquibase.changelog.AbstractChangeLogHistoryService;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.RanChangeSet;
import liquibase.database.Database;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.exception.UnexpectedLiquibaseException;
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

public class FileChangeLogHistoryService extends AbstractChangeLogHistoryService {
    private Database database;
    private int COLUMN_ID = 0;
    private int COLUMN_AUTHOR = 1;
    private int COLUMN_FILENAME = 2;
    private int COLUMN_DATEEXECUTED = 3;
    private int COLUMN_ORDEREXECUTED = 4;
    private int COLUMN_EXECTYPE = 5;
    private int COLUMN_MD5SUM = 6;
    private int COLUMN_DESCRIPTION = 7;
    private int COLUMN_COMMENTS = 8;
    private int COLUMN_TAG = 9;
    private int COLUMN_LIQUIBASE = 10;
    private Integer lastChangeSetSequenceValue;


    @Override
    public int getPriority() {
        return 1000;
    }


    @Override
    public boolean supports(Database database) {
        return Config.getInstance().isEnabled();
    }

    @Override
    public void setDatabase(Database database) {
        this.database = database;
    }

    @Override
    public void reset() {

    }

    @Override
    public void init() throws DatabaseException {
        File file = Config.getInstance().getChangeLogHistoryFile();
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            try {
                file.createNewFile();
                writeHeader(file);
            } catch (IOException e) {
                throw new UnexpectedLiquibaseException(e);
            }
        }
    }

    protected void writeHeader(File file) throws IOException {
        FileWriter writer = null;
        try {
            writer = new FileWriter(file);
            CSVWriter csvWriter = new CSVWriter(writer);
            csvWriter.writeNext(new String[]{
                    "ID",
                    "AUTHOR",
                    "FILENAME",
                    "DATEEXECUTED",
                    "ORDEREXECUTED",
                    "EXECTYPE",
                    "MD5SUM",
                    "DESCRIPTION",
                    "COMMENTS",
                    "TAG",
                    "LIQUIBASE"
            });
        } finally {
            if (writer != null) {
                writer.flush();
                writer.close();
            }
        }
    }

    @Override
    protected void replaceChecksum(final ChangeSet changeSet) throws DatabaseException {
        replaceChangeSet(changeSet, new ReplaceChangeSetLogic() {
            @Override
            public String[] execute(String[] line) {
                line[COLUMN_MD5SUM] = changeSet.generateCheckSum().toString();
                return line;
            }
        });
    }

    @Override
    public List<RanChangeSet> getRanChangeSets() throws DatabaseException {
        File file = Config.getInstance().getChangeLogHistoryFile();
        FileReader reader = null;
        try {
            reader = new FileReader(file);
            CSVReader csvReader = new CSVReader(reader);
            String[] line = csvReader.readNext();
            if (!line[COLUMN_ID].equals("ID")) {
                throw new DatabaseException("Missing header in file "+file.getAbsolutePath());
            }

            List<RanChangeSet> returnList = new ArrayList<RanChangeSet>();
            while ((line = csvReader.readNext()) != null) {
                returnList.add(new RanChangeSet(
                        line[COLUMN_FILENAME],
                        line[COLUMN_ID],
                        line[COLUMN_AUTHOR],
                        CheckSum.parse(line[COLUMN_MD5SUM]),
                        new ISODateFormat().parse(line[COLUMN_DATEEXECUTED]),
                        line[COLUMN_TAG],
                        ChangeSet.ExecType.valueOf(line[COLUMN_EXECTYPE]),
                        line[COLUMN_DESCRIPTION],
                        line[COLUMN_COMMENTS]));
            }

            return returnList;
        } catch (Exception e) {
            throw new DatabaseException(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignore) { }
            }
        }
    }

    protected void replaceChangeSet(ChangeSet changeSet, ReplaceChangeSetLogic replaceLogic) throws DatabaseException {
        File oldFile = Config.getInstance().getChangeLogHistoryFile();
        File newFile = new File(oldFile.getParentFile(), oldFile.getName()+".new");

        FileReader reader = null;
        FileWriter writer = null;

        try {
            reader = new FileReader(oldFile);
            writer = new FileWriter(newFile);
            CSVReader csvReader = new CSVReader(reader);
            CSVWriter csvWriter = new CSVWriter(writer);
            String[] line;
            while ((line = csvReader.readNext()) != null) {
                if (line[COLUMN_ID].equals(changeSet.getId()) && line[COLUMN_AUTHOR].equals(changeSet.getAuthor()) && line[COLUMN_FILENAME].equals(changeSet.getFilePath())) {
                    line = replaceLogic.execute(line);
                }
                if (line != null) {
                    csvWriter.writeNext(line);
                }
            }

            csvWriter.flush();
            csvWriter.close();
            writer = null;

            csvReader.close();
            reader = null;


            oldFile.delete();
            newFile.renameTo(oldFile);
        } catch (Exception e) {
            throw new DatabaseException(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignore) { }
            }
            if (writer != null) {
                try {
                    writer.flush();
                    writer.close();
                } catch (IOException ignore) {}
            }
        }
    }

    protected void appendChangeSet(ChangeSet changeSet, ChangeSet.ExecType execType) throws DatabaseException {
        File oldFile = Config.getInstance().getChangeLogHistoryFile();
        File newFile = new File(oldFile.getParentFile(), oldFile.getName()+".new");

        FileReader reader = null;
        FileWriter writer = null;

        try {
            reader = new FileReader(oldFile);
            writer = new FileWriter(newFile);
            CSVReader csvReader = new CSVReader(reader);
            CSVWriter csvWriter = new CSVWriter(writer);
            String[] line;
            while ((line = csvReader.readNext()) != null) {
                csvWriter.writeNext(line);
            }

            String[] newLine = new String[11];
            newLine[COLUMN_ID] = changeSet.getId();
            newLine[COLUMN_AUTHOR] = changeSet.getAuthor();
            newLine[COLUMN_FILENAME] =  changeSet.getFilePath();
            newLine[COLUMN_DATEEXECUTED] = new ISODateFormat().format(new java.sql.Timestamp(new Date().getTime()));
            newLine[COLUMN_ORDEREXECUTED] = String.valueOf(getNextSequenceValue());
            newLine[COLUMN_EXECTYPE] = execType.value;
            newLine[COLUMN_MD5SUM] = changeSet.generateCheckSum().toString();
            newLine[COLUMN_DESCRIPTION] = changeSet.getDescription();
            newLine[COLUMN_COMMENTS] = changeSet.getComments();
            newLine[COLUMN_TAG] = "";
            newLine[COLUMN_LIQUIBASE] = LiquibaseUtil.getBuildVersion().replaceAll("SNAPSHOT", "SNP");

            csvWriter.writeNext(newLine);

            csvWriter.flush();
            csvWriter.close();
            writer = null;

            csvReader.close();
            reader = null;

            oldFile.delete();
            newFile.renameTo(oldFile);
        } catch (Exception e) {
            throw new DatabaseException(e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignore) { }
            }
            if (writer != null) {
                try {
                    writer.flush();
                    writer.close();
                } catch (IOException ignore) {}
            }
        }
    }

    @Override
    public void setExecType(final ChangeSet changeSet, final ChangeSet.ExecType execType) throws DatabaseException {
        if (execType.equals(ChangeSet.ExecType.FAILED) || execType.equals(ChangeSet.ExecType.SKIPPED)) {
            return; //do nothing
        } else  if (execType.ranBefore) {
            replaceChangeSet(changeSet, new ReplaceChangeSetLogic() {
                @Override
                public String[] execute(String[] line) {
                    line[COLUMN_DATEEXECUTED] = new ISODateFormat().format(new java.sql.Timestamp(new Date().getTime()));
                    line[COLUMN_MD5SUM] = changeSet.generateCheckSum().toString();
                    line[COLUMN_EXECTYPE] = execType.value;
                    return line;
                }
            });
        } else {
            appendChangeSet(changeSet, execType);
        }
    }

    @Override
    public void removeFromHistory(ChangeSet changeSet) throws DatabaseException {
        replaceChangeSet(changeSet, new ReplaceChangeSetLogic() {
            @Override
            public String[] execute(String[] line) {
                return null;
            }
        });
    }

    @Override
    public int getNextSequenceValue() throws LiquibaseException {
        if (lastChangeSetSequenceValue == null) {
            lastChangeSetSequenceValue = 0;

            File file = Config.getInstance().getChangeLogHistoryFile();
            FileReader reader = null;
            try {
                reader = new FileReader(file);
                CSVReader csvReader = new CSVReader(reader);
                String[] line = csvReader.readNext(); //skip header line

                List<RanChangeSet> returnList = new ArrayList<RanChangeSet>();
                while ((line = csvReader.readNext()) != null) {
                    try {
                        lastChangeSetSequenceValue = Integer.valueOf(line[COLUMN_ORDEREXECUTED]);
                    } catch (NumberFormatException ignore) { }
                }
            } catch (Exception ignore) {
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ignore) { }
                }
            }

        }

        return ++lastChangeSetSequenceValue;
    }

    @Override
    public void tag(String tagString) throws DatabaseException {

    }

    @Override
    public boolean tagExists(String tag) throws DatabaseException {
        return false;
    }

    private interface ReplaceChangeSetLogic {
        public String[] execute(String[] line);
    }
}
