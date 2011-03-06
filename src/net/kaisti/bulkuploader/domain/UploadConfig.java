package net.kaisti.bulkuploader.domain;

import java.util.List;

import com.google.code.twig.annotation.Id;
/**
 *
 * Upload configuration entity class 
 *
 * @author Tero Kaisti
 *
 */
public class UploadConfig {
	@Id
	public String blobKey;
	public String entityKind;

	public int keyColumnIndex = -1; // -1 not in use
	public List<String> columns;

	public String delimiter = ",";
	public String quote = "\"";

	public boolean deleteBlob = false; // remove blob after mapping 
	public String shardCount;
	
	
	public UploadConfig() {}
	public UploadConfig(String blobKey, String entityKind) {
		this.blobKey = blobKey;
		this.entityKind = entityKind;
	}
	public UploadConfig(String blobKey, String entityKind, String delimiter, String quote) {
		this(blobKey, entityKind);
		this.delimiter = delimiter;
		this.quote = quote;
	}
}
