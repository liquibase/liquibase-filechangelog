package liquibase.ext.filechangelog;

import org.junit.Test;

import static org.junit.Assert.*;

public class FileChangeLogHistoryServiceTest {

    @Test
    public void getPriority() {
        assertEquals(1000, new FileChangeLogHistoryService().getPriority());
    }

}