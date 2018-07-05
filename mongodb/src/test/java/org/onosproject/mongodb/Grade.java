package org.onosproject.mongodb;

/**
 * Created by root on 12/5/17.
 */
public class Grade {
    private String course;

    private String score;

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }

    public String getScore() {
        return score;
    }

    public void setScore(String score) {
        this.score = score;
    }

    @Override
    public String toString() {
        return "Grade{" +
                "course='" + course + '\'' +
                ", score='" + score + '\'' +
                '}';
    }
}
