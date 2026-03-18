package com.megacorp.humanresources.model;

/**
 * Represents a file in Google Cloud Storage with its name and a time-limited signed URL.
 *
 * @param name      the file name (object key) in GCS
 * @param signedUrl a V4-signed URL granting temporary read access
 */
public record FileItem(
    String name,
    String signedUrl
) {}
