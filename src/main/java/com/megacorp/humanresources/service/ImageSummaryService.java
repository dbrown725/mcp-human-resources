package com.megacorp.humanresources.service;

import java.io.IOException;
import java.util.List;

public interface ImageSummaryService {
    List<String> summarizeImagesInFolder(String folderName) throws IOException;
    
    String generateExpenseReportFromImages(String folderName) throws IOException;
}