package com.ursineenterprises.calendareventsgenerator.model;

import lombok.Getter;
import lombok.Setter;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter
@Setter
public class ZoomEvent {
    private DayOfWeek dayOfWeek;
    private LocalTime time;
    private String zoomUrl;
    private String description;

    public ZoomEvent(DayOfWeek dayOfWeek, LocalTime time, String zoomUrl, String description) {
        this.dayOfWeek = dayOfWeek;
        this.time = time;
        this.zoomUrl = zoomUrl;
        this.description = description;
    }
}
