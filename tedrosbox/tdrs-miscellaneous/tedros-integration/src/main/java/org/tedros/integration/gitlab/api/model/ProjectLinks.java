package org.tedros.integration.gitlab.api.model;

import com.google.gson.annotations.SerializedName;

public record ProjectLinks(
        String self,
        String issues,
        @SerializedName("merge_requests") String mergeRequests,
        @SerializedName("repo_branches") String repoBranches,
        String labels,
        String events,
        String members,
        @SerializedName("cluster_agents") String clusterAgents) {
}
