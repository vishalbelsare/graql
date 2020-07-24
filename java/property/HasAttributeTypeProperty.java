/*
 * Copyright (C) 2020 Grakn Labs
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package graql.lang.property;

import graql.lang.Graql;
import graql.lang.statement.Statement;
import graql.lang.statement.StatementType;

import java.util.stream.Stream;

import static graql.lang.Graql.Token.Char.SPACE;
import static graql.lang.Graql.Token.Property.IS_KEY;

/**
 * Represents the {@code has} and {@code key} properties on a Type.
 * This property can be queried or inserted. Whether this is a key is indicated by the
 * HasAttributeTypeProperty#isKey field.
 * This property is defined an ontological structure between a Type and a AttributeType,
 * using different structure labels (on edges) to indicate a Has versus a Key relationship between the types.
 */
public class HasAttributeTypeProperty extends VarProperty {

    private final Statement attributeType;
    private final boolean isKey;

    public HasAttributeTypeProperty(Statement attributeType, boolean isKey) {
        this.attributeType = attributeType;
        this.isKey = isKey;
    }

    public Statement attributeType() {
        return attributeType;
    }

    public boolean isKey() {
        return isKey;
    }

    @Override
    public String keyword() {
        return Graql.Token.Property.HAS.toString();
    }

    @Override
    public String property() {
        return attributeType.getPrintableName();
    }

    @Override
    public boolean isUnique() {
        return false;
    }

    @Override
    public Stream<Statement> types() {
        return Stream.of(attributeType);
    }

    @Override
    public Stream<Statement> statements() {
        return Stream.of(attributeType);
    }

    @Override
    public Class statementClass() {
        return StatementType.class;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder().append(keyword()).append(SPACE).append(property());
        if (isKey) str.append(SPACE).append(IS_KEY);
        return str.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HasAttributeTypeProperty that = (HasAttributeTypeProperty) o;

        return (this.attributeType.equals(that.attributeType) &&
                this.isKey == that.isKey);
    }

    @Override
    public int hashCode() {
        int h = 1;
        h *= 1000003;
        h ^= this.attributeType.hashCode();
        h *= 1000003;
        h ^= this.isKey ? 1231 : 1237;
        return h;
    }
}
