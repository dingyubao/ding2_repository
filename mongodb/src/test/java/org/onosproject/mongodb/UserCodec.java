package org.onosproject.mongodb;

import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

/**
 * Created by root on 11/21/17.
 */
public class UserCodec implements Codec {
    @Override
    public Object decode(BsonReader bsonReader, DecoderContext decoderContext) {
        return null;
    }

    @Override
    public void encode(BsonWriter bsonWriter, Object o, EncoderContext encoderContext) {

    }

    @Override
    public Class getEncoderClass() {
        return null;
    }
}
