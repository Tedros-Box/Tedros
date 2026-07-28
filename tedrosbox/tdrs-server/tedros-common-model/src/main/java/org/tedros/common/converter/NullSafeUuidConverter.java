package org.tedros.common.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.UUID;

@Converter(autoApply = true)
public class NullSafeUuidConverter implements AttributeConverter<UUID, Object> {

    @Override
    public Object convertToDatabaseColumn(UUID attribute) {
        if (attribute == null) {
            return null;
        }
        // Como a url de conexão do banco com o postgres usa stringtype=unspecified na URL, 
        // podemos enviar como String e o Postgres aceita nativamente.
        return attribute.toString();
    }

    @Override
    public UUID convertToEntityAttribute(Object dbData) {
        if (dbData == null) {
            return null; // Aqui nós evitamos o NullPointerException!
        }
        
        // Se o banco retornar um PGobject ou String, .toString() resolve ambos
        return UUID.fromString(dbData.toString());
    }
}
