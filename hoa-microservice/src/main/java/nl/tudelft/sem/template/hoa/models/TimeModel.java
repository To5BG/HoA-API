package nl.tudelft.sem.template.hoa.models;

import lombok.Data;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class TimeModel {
    public int seconds;
    public int minutes;
    public int hours;
    public int day;
    public int month;
    public int year;

    public TimeModel (Integer[] nums) {
        this.year = nums[0];
        this.month = nums[1];
        this.day = nums[2];
        this.hours = nums[3];
        this.minutes = nums[4];
        this.seconds = nums[5];
    }

    public boolean isValid() {
        return createDate() != null;
    }

    /**
     * Returns a new LocalDateTime object based on this model's attributes
     *
     * @return new LocalDateTime object
     */
    public LocalDateTime createDate() {
        try {
            return LocalDateTime.of(LocalDate.of(this.year, this.month, this.day),
                    LocalTime.of(this.hours, this.minutes, this.seconds));
        } catch (DateTimeException e) {
            return null;
        }
    }
}
