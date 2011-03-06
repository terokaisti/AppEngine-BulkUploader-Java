package net.kaisti.bulkuploader.mappers;


import net.kaisti.bulkuploader.domain.UploadConfig;
import net.kaisti.bulkuploader.mappers.helper.MapperHelper;

import org.apache.hadoop.io.NullWritable;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.tools.mapreduce.AppEngineMapper;
import com.google.appengine.tools.mapreduce.BlobstoreRecordKey;
import com.google.appengine.tools.mapreduce.DatastoreMutationPool;

/**
 * MapperClass to store CSV lines as entities to datastore
 *
 * @author Tero Kaisti
 *
 */
public class ImportFromBlobstoreMapper extends
		AppEngineMapper<BlobstoreRecordKey, byte[], NullWritable, NullWritable> {

	@Override
	public void map(BlobstoreRecordKey key, byte[] segment, Context context) {
		UploadConfig conf = MapperHelper.getUploadConfig(key.getBlobKey()
				.getKeyString()); 
		String line = new String(segment);
		String[] values = line.split(conf.delimiter);

		Entity e =  conf.keyColumnIndex > -1 ? 
				new Entity(conf.entityKind, values[conf.keyColumnIndex]) :
				new Entity(conf.entityKind);

		for(int i = 0; i < values.length; i++) {
			// if the first char is "quote", then it's stored as string
			if(values[i].substring(0, 1).equals(conf.quote)) {
				e.setProperty(conf.columns.get(i), 
						values[i].substring(1, values[i].length()-1));
			}
			// else we store it as double
			else {
				e.setProperty(conf.columns.get(i), 
						Double.parseDouble(values[i]));
			}
		}

		DatastoreMutationPool mutationPool = this.getAppEngineContext(context)
			.getMutationPool();
		mutationPool.put(e);
	}
}
