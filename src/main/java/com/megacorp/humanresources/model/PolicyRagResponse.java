package com.megacorp.humanresources.model;

import java.util.List;

public record PolicyRagResponse(
    String answer,
    List<String> attachmentPaths,
    List<String> matchedPolicyTitles,
    String supportingContext,
    int matchCount
) {
}
