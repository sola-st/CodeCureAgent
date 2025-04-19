package sorald.event.models.miner;

import java.util.List;
import sorald.event.models.WarningLocation;

public class MinedRule {
    private final String ruleKey;
    private final String ruleName;
    private final String ruleType;
    private final List<WarningLocation> warningLocations;

    public MinedRule(String ruleKey, String ruleName, String ruleType, List<WarningLocation> warningLocations) {
        this.ruleKey = ruleKey;
        this.ruleName = ruleName;
        this.ruleType = ruleType;
        this.warningLocations = List.copyOf(warningLocations);
    }

    public List<WarningLocation> getWarningLocations() {
        return warningLocations;
    }

    public String getRuleName() {
        return ruleName;
    }

    public String getRuleKey() {
        return ruleKey;
    }

    public String getRuleType() {
        return ruleType;
    }
}
