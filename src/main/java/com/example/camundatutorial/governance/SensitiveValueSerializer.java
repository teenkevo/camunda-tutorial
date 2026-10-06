package com.example.camundatutorial.governance;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import java.io.IOException;

/**
 * Serializes any non-null sensitive value as the configured mask string.
 * Works for String, numbers, dates, UUIDs, etc.
 */
public class SensitiveValueSerializer extends JsonSerializer<Object> {

    private final SensitiveDataMasker masker;

    public SensitiveValueSerializer(SensitiveDataMasker masker) {
        this.masker = masker;
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            serializers.defaultSerializeNull(gen);
            return;
        }
        gen.writeString(masker.mask(value));
    }
}
