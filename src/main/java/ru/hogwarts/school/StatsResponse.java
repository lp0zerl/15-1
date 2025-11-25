package ru.hogwarts.school;

import java.util.List;

class StatsResponse {
    private List<RuleStat> stats;

    public StatsResponse() {}
    public StatsResponse(List<RuleStat> stats) {
        this.stats = stats;
    }

    public List<RuleStat> getStats() { return stats; }
    public void setStats(List<RuleStat> stats) { this.stats = stats; }
}
