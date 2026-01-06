package ru.yandex.practicum.kafka.telemetry.serialization;

import org.apache.avro.Schema;
import org.apache.avro.io.BinaryDecoder;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.io.IOException;

/**
 * Базовый десериализатор для Avro-сообщений.
 * Может быть использован для десериализации любых классов, сгенерированных из Avro-схем.
 *
 * @param <T> тип события, расширяющий SpecificRecordBase
 */
public class BaseAvroDeserializer<T extends SpecificRecordBase> implements Deserializer<T> {

    private final DecoderFactory decoderFactory;
    private final DatumReader<T> datumReader;
    private BinaryDecoder decoder;

    /**
     * Конструктор с указанием схемы данных.
     *
     * @param schema схема Avro для десериализации
     */
    public BaseAvroDeserializer(Schema schema) {
        this(DecoderFactory.get(), schema);
    }

    /**
     * Конструктор с указанием фабрики декодеров и схемы данных.
     *
     * @param decoderFactory фабрика для создания декодеров
     * @param schema схема Avro для десериализации
     */
    public BaseAvroDeserializer(DecoderFactory decoderFactory, Schema schema) {
        this.decoderFactory = decoderFactory;
        this.datumReader = new SpecificDatumReader<>(schema);
    }

    @Override
    public T deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }

        try {
            decoder = decoderFactory.binaryDecoder(data, decoder);
            return datumReader.read(null, decoder);
        } catch (IOException e) {
            throw new SerializationException("Error deserializing Avro message", e);
        }
    }
}