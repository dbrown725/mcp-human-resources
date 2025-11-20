package com.megacorp.humanresources.service;

import com.megacorp.humanresources.model.Sentiment;

public interface ReviewService {
    Sentiment classifySentiment(String review);
}
