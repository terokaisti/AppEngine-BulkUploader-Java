package net.kaisti.bulkuploader.mappers.helper;

import java.util.Collections;

import net.kaisti.bulkuploader.domain.UploadConfig;
import net.sf.jsr107cache.Cache;
import net.sf.jsr107cache.CacheException;
import net.sf.jsr107cache.CacheManager;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.Mapper;

import com.google.appengine.api.blobstore.BlobInfo;
import com.google.appengine.api.blobstore.BlobInfoFactory;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.appengine.api.taskqueue.TaskOptions.Method;
import com.google.appengine.tools.mapreduce.AppEngineJobContext;
import com.google.appengine.tools.mapreduce.AppEngineMapper;
import com.google.appengine.tools.mapreduce.BlobstoreInputFormat;
import com.google.appengine.tools.mapreduce.BlobstoreRecordKey;
import com.google.appengine.tools.mapreduce.ConfigurationXmlUtil;
import com.google.code.twig.ObjectDatastore;
import com.google.code.twig.annotation.AnnotationObjectDatastore;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class MapperHelper {
	static final int BLOB_SHARD_FACTOR = 300000; // more than 10 shards do not really help more than 50000 entities

	public static void StartMapReduceWithBlob(Class<? extends AppEngineMapper<BlobstoreRecordKey, byte[], NullWritable, NullWritable>> blobMapperImpl, 
			String blobKey) {
		BlobInfoFactory blobInfoFactory = new BlobInfoFactory();
		BlobInfo blobInfo = blobInfoFactory.loadBlobInfo(new BlobKey(blobKey));
		int shardCount = (int) Math.ceil(blobInfo.getSize() / BLOB_SHARD_FACTOR)+1;
		StartMapReduceWithBlob(blobMapperImpl, blobKey, shardCount);
	}
	public static void StartMapReduceWithBlob(Class<? extends AppEngineMapper<BlobstoreRecordKey, byte[], NullWritable, NullWritable>> blobMapperImpl, String blobKey, int shardCount) {
				
		Configuration conf = new Configuration(false);
		
        conf.setClass("mapreduce.map.class", blobMapperImpl, Mapper.class);
	    conf.setClass("mapreduce.inputformat.class", BlobstoreInputFormat.class, InputFormat.class);
	    conf.set(BlobstoreInputFormat.BLOB_KEYS, blobKey);
	    conf.set(BlobstoreInputFormat.SHARD_COUNT, String.valueOf(shardCount));
	    conf.set(AppEngineJobContext.DONE_CALLBACK_URL_KEY, "/mapper-completed");

	    String xml = ConfigurationXmlUtil.convertConfigurationToXml(conf);
        
		Queue queue = QueueFactory.getDefaultQueue();
		TaskOptions task = TaskOptions.Builder
			.withDefaults()
			.url("/mapreduce/start")
			.method(Method.POST)
			.param("configuration", xml);
        queue.add(task);
	}
	static Cache cache;
	static Gson gson;
	static {
        try {
            cache = CacheManager.getInstance().getCacheFactory().createCache(Collections.emptyMap());
        } catch (CacheException e) {
            // ...
        }
        gson = new GsonBuilder().serializeNulls().create();
	}
	
	public static UploadConfig getUploadConfig(String blobKey) {
		String confJson = (String) cache.get(blobKey);
        if(confJson != null)
        	return gson.fromJson(confJson, UploadConfig.class);
        UploadConfig conf = new AnnotationObjectDatastore().load(UploadConfig.class, blobKey);
        cache.put(blobKey, gson.toJson(conf));
        return conf;
	}
	public static void setUploadConfig(UploadConfig conf) {
        cache.put(conf.blobKey, gson.toJson(conf));
		ObjectDatastore twig = new AnnotationObjectDatastore();
		twig.store(conf);
	}

}
