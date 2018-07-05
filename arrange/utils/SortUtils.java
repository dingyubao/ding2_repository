package org.onosproject.arrange.utils;

import org.onosproject.mongodb.Adapter;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Comparator;
import java.util.Map;

public class SortUtils {

    public static class Sort implements Comparator {
        @Override
        public int compare(Object arg0, Object arg1) {
            Map.Entry<String, Adapter<?>> entry = (Map.Entry<String, Adapter<?>>) arg0;
            Map.Entry<String, Adapter<?>> entry1 = (Map.Entry<String, Adapter<?>>) arg1;

            checkNotNull(entry.getValue().getCommitId());
            checkNotNull(entry1.getValue().getCommitId());

            int flag = entry.getValue().getCommitId().toString().compareTo(entry1.getValue().getCommitId().toString());
            return flag;
        }
    }

    public static class SortReverse implements Comparator {
        @Override
        public int compare(Object arg0, Object arg1) {
            Map.Entry<String, Adapter<?>> entry = (Map.Entry<String, Adapter<?>>) arg0;
            Map.Entry<String, Adapter<?>> entry1 = (Map.Entry<String, Adapter<?>>) arg1;

            checkNotNull(entry.getValue().getCommitId());
            checkNotNull(entry1.getValue().getCommitId());

            int flag = entry1.getValue().getCommitId().toString().compareTo(entry.getValue().getCommitId().toString());
            return flag;
        }
    }
}

