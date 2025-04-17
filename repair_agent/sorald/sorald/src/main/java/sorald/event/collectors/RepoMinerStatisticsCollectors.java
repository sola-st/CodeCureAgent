package sorald.event.collectors;

import java.util.List;

/**
 *  Wrapper class around multiple RepoMinerStatisticsCollector objects.
 *  Makes it possible to create a unified json mining report for multiple analyzed repositories, separated by repository.
  */
public class RepoMinerStatisticsCollectors {
    
    private List<RepoMinerStatisticsCollector> minedRepositories;

    public RepoMinerStatisticsCollectors(List<RepoMinerStatisticsCollector> minedRepositories){
        this.minedRepositories = minedRepositories;

    }


    public List<RepoMinerStatisticsCollector> getMinedRepositories(){
        return minedRepositories;
    }
}
