package org.onosproject.mongodb;

import java.util.List;

/**
 * Created by root on 12/5/17.
 */
public class Student {

    private String name;
    private List<Grade> grade; // 因为grade是个数组，所以要定义成List

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Grade> getGrade() {
        return grade;
    }

    public void setGrade(List<Grade> grade) {
        this.grade = grade;
    }

    @Override
    public String toString() {
        return "Student{" +
                "name='" + name + '\'' +
                ", grade=" + grade +
                '}';
    }
}
