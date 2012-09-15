package org.rapla.plugin.occupationview;

import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.dynamictype.AttributeType;
import org.rapla.entities.dynamictype.Classification;

public class EndOfLifeArchiver {

    static public AttributeType getEndOfLifeType(Allocatable alloc) {
        Classification classification = alloc.getClassification();
        if ( classification == null)
            return null;
        final Attribute attribute = classification.getType().getAttribute("_endoflife");
        if(attribute == null)
            return null;
        AttributeType  type = attribute.getType();
        if (type.equals(AttributeType.BOOLEAN) || type.equals(AttributeType.DATE))
            return type;
        else
            return null;
    }

}
