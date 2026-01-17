package response;

import ru.hogwarts.school.RuleStatsDto;

import java.util.List;

class RuleStatsResponse { private List<RuleStatsDto> stats;
    public RuleStatsResponse() {} public RuleStatsResponse(List<RuleStatsDto> stats) { this.stats = stats; }
    public List<RuleStatsDto> getStats() { return stats; } public void setStats(List<RuleStatsDto> stats) { this.stats = stats; }
}