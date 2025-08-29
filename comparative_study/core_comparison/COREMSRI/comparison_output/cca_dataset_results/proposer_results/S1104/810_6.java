package org.opentripplanner.routing.alertpatch;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.util.I18NString;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TransitAlert implements Serializable {
    private static final long serialVersionUID = 8305126586053909836L;

    private String id;

    private I18NString alertHeaderText;
    private I18NString alertDescriptionText;
    private I18NString alertDetailText;
    private I18NString alertAdviceText;

    // TODO OTP2 we wanted to merge the GTFS single alertUrl and the SIRI multiple URLs.
    //      However, GTFS URLs are one-per-language in a single object, and SIRI URLs are N objects with no translation.
    private I18NString alertUrl;

    private List<AlertUrl> alertUrlList = new ArrayList<>();

    //null means unknown
    private String alertType;

    //null means unknown
    private String severity;

    //null means unknown
    private int priority;

    private List<TimePeriod> timePeriods = new ArrayList<>();

    private String feedId;

    private final Set<EntitySelector> entities = new HashSet<>();

    private final Collection<StopCondition> stopConditions = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public I18NString getAlertHeaderText() {
        return alertHeaderText;
    }

    public void setAlertHeaderText(I18NString alertHeaderText) {
        this.alertHeaderText = alertHeaderText;
    }

    public I18NString getAlertDescriptionText() {
        return alertDescriptionText;
    }

    public void setAlertDescriptionText(I18NString alertDescriptionText) {
        this.alertDescriptionText = alertDescriptionText;
    }

    public I18NString getAlertDetailText() {
        return alertDetailText;
    }

    public void setAlertDetailText(I18NString alertDetailText) {
        this.alertDetailText = alertDetailText;
    }

    public I18NString getAlertAdviceText() {
        return alertAdviceText;
    }

    public void setAlertAdviceText(I18NString alertAdviceText) {
        this.alertAdviceText = alertAdviceText;
    }

    public I18NString getAlertUrl() {
        return alertUrl;
    }

    public void setAlertUrl(I18NString alertUrl) {
        this.alertUrl = alertUrl;
    }

    public List<AlertUrl> getAlertUrlList() {
        return alertUrlList;
    }

    public void setAlertUrlList(List<AlertUrl> alertUrlList) {
        this.alertUrlList = alertUrlList;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public boolean displayDuring(State state) {
        return displayDuring(state.getStartTimeSeconds(), state.getTimeSeconds());
    }

    public boolean displayDuring(long startTimeSeconds, long endTimeSeconds) {
        for (TimePeriod timePeriod : timePeriods) {
            if (endTimeSeconds >= timePeriod.startTime) {
                if (timePeriod.endTime == 0 || startTimeSeconds < timePeriod.endTime) {
                    return true;
                }
            }
        }
        return false;
    }

    public void setTimePeriods(List<TimePeriod> periods) {
        timePeriods = periods;
    }

    public void addEntity(EntitySelector entitySelector) {
        entities.add(entitySelector);
    }

    public Set<EntitySelector> getEntities() {
        return entities;
    }

    public Collection<StopCondition> getStopConditions() {
        return stopConditions;
    }

    public String getFeedId() {
        return feedId;
    }

    public void setFeedId(String feedId) {
        this.feedId = feedId;
    }

    public Date getEffectiveStartDate() {
        return timePeriods
            .stream()
            .map(timePeriod -> timePeriod.startTime)
            .min(Comparator.naturalOrder())
            .map(startTime -> new Date(startTime * 1000))
            .orElse(null);
    }

    public Date getEffectiveEndDate() {
        return timePeriods
            .stream()
            .map(timePeriod -> timePeriod.endTime)
            .max(Comparator.naturalOrder())
            .map(startTime -> new Date(startTime * 1000))
            .orElse(null);
    }

    @Override
    public String toString() {
        return "Alert('"
                + (alertHeaderText != null ? alertHeaderText.toString()
                        : alertDescriptionText != null ? alertDescriptionText.toString()
                        : alertDetailText != null ? alertDetailText.toString()
                        : alertAdviceText != null ? alertAdviceText.toString()
                                : "?") + "')";
    }

}