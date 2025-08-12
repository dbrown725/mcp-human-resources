package com.megacorp.humanresources.service;

import com.megacorp.humanresources.service.BraveSearchServiceImpl.BraveSearchApiResponse;

public interface BraveSearchService {
    BraveSearchApiResponse braveSearch(String query);
}
