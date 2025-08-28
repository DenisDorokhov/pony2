package net.dorokhov.pony2.web.dto.opensubsonic;

public class OpenSubsonicItemDate {

    private int year;
    private int month;
    private int day;

    public int getYear() {
        return year;
    }

    public OpenSubsonicItemDate setYear(int year) {
        this.year = year;
        return this;
    }

    public int getMonth() {
        return month;
    }

    public OpenSubsonicItemDate setMonth(int month) {
        this.month = month;
        return this;
    }

    public int getDay() {
        return day;
    }

    public OpenSubsonicItemDate setDay(int day) {
        this.day = day;
        return this;
    }
}
