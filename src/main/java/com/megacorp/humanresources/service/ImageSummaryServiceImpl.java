package com.megacorp.humanresources.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ImageSummaryServiceImpl implements ImageSummaryService {

    private final ChatClient chatClient;
    private final FileStorageService fileStorageService;

    @Autowired
    public ImageSummaryServiceImpl(ChatClient.Builder builder, FileStorageService fileStorageService) {
        this.chatClient = builder.build();
        this.fileStorageService = fileStorageService;
    }

    @Override
    public List<String> summarizeImagesInFolder(String folderName) throws IOException {
        List<String> urls = fileStorageService.listFileUrlsInFolder(folderName);
        if (urls == null || urls.isEmpty()) {
            return Collections.emptyList();
        }

        List<MimeType> mimeTypes = new ArrayList<>();
        List<Resource> resources = new ArrayList<>();

        for (String url : urls) {
            String filename = url.substring(url.lastIndexOf('/') + 1);
            String mimeTypeStr = resolveMimeType(filename);
            if (mimeTypeStr == null || mimeTypeStr.isEmpty()) continue;
            MimeType mimeType = MimeType.valueOf(mimeTypeStr);
            Resource resource = fileStorageService.getResourceFromGcsUrl(url);
            mimeTypes.add(mimeType);
            resources.add(resource);
        }

        if (resources.isEmpty()) {
            return Collections.emptyList();
        }

        String separator = "__IMAGE_SUMMARY_SEPARATOR__";
        String prompt = "Write a summary of each image's contents. Use the following separator between summaries: " + separator;
        String response = chatClient.prompt()
            .user(u -> {
                u.text(prompt);
                for (int i = 0; i < mimeTypes.size(); i++) {
                    u.media(mimeTypes.get(i), resources.get(i));
                }
            })
            .call()
            .content();

        String[] summaries = response.split(java.util.regex.Pattern.quote(separator));
        List<String> result = new ArrayList<>();
        for (String summary : summaries) {
            String trimmed = summary.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }

        return result;
    }

    @Override
    public String generateExpenseReportFromImages(String folderName) throws IOException {
        java.util.List<String> result = summarizeImagesInFolder(folderName);
        String csvTemplateHeaders = "Receipt_ID,Receipt_Date,Due_Date,Vendor,Purchaser,Recipient,Payment_Method,Subtotal,Tax_Rate,Tax_Amount,Total_Amount,Currency,Line_Item_Description,Line_Item_Quantity,Line_Item_Unit_Price,Line_Item_Total,Notes";

        String llmDirections = getLlmDirections();

        String csvResponse = chatClient.prompt()
            .user(u -> {
                u.text(llmDirections + "\n\n" + " csv headers " + csvTemplateHeaders + "\n\n" + String.join("\n", result));
            })
            .call()
            .content();

        byte[] csvBytes = csvResponse.getBytes();
        String csvFileName = folderName + "/expense_report.csv";
        

        return "Generated File. " + fileStorageService.uploadFile(csvBytes, csvFileName);
    }

    private String resolveMimeType(String filename) {
        String mimeType = "";
        if (filename != null) {
            String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
            switch (ext) {
                case "jpg":
                case "jpeg":
                    mimeType = "image/jpeg";
                    break;
                case "png":
                    mimeType = "image/png";
                    break;
                case "gif":
                    mimeType = "image/gif";
                    break;
                case "webp":
                    mimeType = "image/webp";
                    break;
                case "heic":
                    mimeType = "image/heic";
                    break;
                case "heif":
                    mimeType = "image/heif";
                    break;
            }
        }
        return mimeType;
    }

    private String getLlmDirections() {
        String llmDirections = "Instructions\n" + //
                        "\n" + //
                        "Your task is to act as a meticulous data entry agent. You will be provided with a list of expense receipt descriptions and a target CSV file format. Your goal is to accurately extract the information from each receipt and populate the CSV template.\n" + //
                        "\n" + //
                        "Here are the step-by-step instructions:\n" + //
                        "\n" + //
                        "1.  **Analyze Each Receipt**: Process each expense receipt description one by one.\n" + //
                        "\n" + //
                        "2.  **Create Rows**:\n" + //
                        "    * For each receipt that lists multiple products (line items), you must create a **separate row for each individual line item**.\n" + //
                        "    * For a single receipt with multiple line items, the information in columns `Receipt_ID` through `Currency` and the `Notes` column will be **identical** for all rows corresponding to that receipt.\n" + //
                        "    * The columns from `Line_Item_Description` to `Line_Item_Total` must contain the information for that specific item's row.\n" + //
                        "\n" + //
                        "3.  **Populate the Columns**: Fill in the data for each column according to these rules:\n" + //
                        "    * **Receipt\\_ID**: Generate a unique, sequential ID for each *distinct receipt*, starting with `R001`, `R002`, etc.\n" + //
                        "    * **Receipt\\_Date** & **Due\\_Date**: Extract the dates. If an \"invoice date\" is mentioned, use it for the `Receipt_Date`. Standardize all dates to a `MM/DD/YYYY` format.\n" + //
                        "    * **Vendor**: The name of the seller or the company issuing the receipt (e.g., \"ABC Electronics\").\n" + //
                        "    * **Purchaser**: The name of the buyer or the entity the receipt is issued to (e.g., \"XYZ Tech Solutions\").\n" + //
                        "    * **Recipient**: The person or entity receiving the goods. If a \"shipped to\" name is provided, use it. Otherwise, this can be the same as the `Purchaser` or left blank if not specified.\n" + //
                        "    * **Payment\\_Method**: Extract the method of payment (e.g., \"Credit Card\", \"Money Order\").\n" + //
                        "    * **Subtotal**, **Tax\\_Amount**, **Total\\_Amount**: Extract the monetary values. Ensure these fields contain **only numbers** (no dollar signs or commas).\n" + //
                        "    * **Tax\\_Rate**: If a percentage is explicitly stated (e.g., \"7.00%\"), use it. If not, you may calculate it using the formula `(Tax_Amount / Subtotal) * 100` and present it as a percentage string (e.g., \"7.00%\").\n" + //
                        "    * **Currency**: Assume **USD** unless another currency is specified.\n" + //
                        "    * **Line\\_Item\\_Description**: The name of the specific product or service for that row.\n" + //
                        "    * **Line\\_Item\\_Quantity**: If not specified for an item, assume the quantity is **1**.\n" + //
                        "    * **Line\\_Item\\_Unit\\_Price** & **Line\\_Item\\_Total**: If the price for individual items is not provided, leave these fields blank. Do not guess or attempt to calculate them by dividing the total.\n" + //
                        "    * **Notes**: Include any extra relevant details mentioned in the receipt.\n" + //
                        "   \n" + //
                        "\n" + //
                        "4.  **Handle Missing Data**: If a piece of information for any field is not available in the receipt's description, leave the corresponding cell in the CSV **blank**.\n" + //
                        "\n" + //
                        "5.  **Final Output**: Generate a single block of text in CSV format that starts with the provided header row, followed by the data rows you have created. Do not include any other explanatory text.\n" + //
                        "";
        return llmDirections;
    }
}