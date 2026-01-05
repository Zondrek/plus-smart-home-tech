package ru.yandex.practicum.telemetry.aggregator.configuration.serialization;

import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Serializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Универсальный сериализатор для Avro-сообщений.
 * Может быть использован для сериализации любых классов, сгенерированных из Avro-схем.
 *
 * @param <T> тип сообщения, расширяющий SpecificRecordBase
 */
public class AvroSerializer<T extends SpecificRecordBase> implements Serializer<T> {

    @Override
    public byte[] serialize(String topic, T data) {
        if (data == null) {
            return null;
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            var encoder = org.apache.avro.io.EncoderFactory.get()
                    .binaryEncoder(outputStream, null);
            var writer = new org.apache.avro.specific.SpecificDatumWriter<>(data.getSchema());
            writer.write(data, encoder);
            encoder.flush();
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new SerializationException("Error serializing Avro message", e);
        }
    }
}