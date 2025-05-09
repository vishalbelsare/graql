/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.typeql.lang.pattern.schema;

import com.typeql.lang.common.Reference;
import com.typeql.lang.common.TypeQLVariable;
import com.typeql.lang.common.exception.TypeQLException;
import com.typeql.lang.pattern.Conjunction;
import com.typeql.lang.pattern.Definable;
import com.typeql.lang.pattern.Disjunction;
import com.typeql.lang.pattern.Negation;
import com.typeql.lang.pattern.Pattern;
import com.typeql.lang.pattern.constraint.ThingConstraint;
import com.typeql.lang.pattern.statement.Statement;
import com.typeql.lang.pattern.statement.ThingStatement;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.typeql.lang.common.TypeQLToken.Char.COLON;
import static com.typeql.lang.common.TypeQLToken.Char.CURLY_CLOSE;
import static com.typeql.lang.common.TypeQLToken.Char.CURLY_OPEN;
import static com.typeql.lang.common.TypeQLToken.Char.NEW_LINE;
import static com.typeql.lang.common.TypeQLToken.Char.SEMICOLON;
import static com.typeql.lang.common.TypeQLToken.Char.SPACE;
import static com.typeql.lang.common.TypeQLToken.Schema.RULE;
import static com.typeql.lang.common.TypeQLToken.Schema.THEN;
import static com.typeql.lang.common.TypeQLToken.Schema.WHEN;
import static com.typeql.lang.common.exception.ErrorMessage.INVALID_RULE_THEN;
import static com.typeql.lang.common.exception.ErrorMessage.INVALID_RULE_THEN_HAS;
import static com.typeql.lang.common.exception.ErrorMessage.INVALID_RULE_THEN_ROLES;
import static com.typeql.lang.common.exception.ErrorMessage.INVALID_RULE_THEN_VARIABLES;
import static com.typeql.lang.common.exception.ErrorMessage.INVALID_RULE_WHEN_MISSING_PATTERNS;
import static com.typeql.lang.common.exception.ErrorMessage.INVALID_RULE_WHEN_NESTED_NEGATION;
import static com.typeql.lang.common.exception.ErrorMessage.INVALID_RULE_THEN_RELATION_VARIABLE;
import static com.typeql.lang.common.util.Strings.indent;

public class Rule implements Definable {
    private final String label;
    private Conjunction<? extends Pattern> when;
    private ThingStatement<?> then;
    private int hash = 0;

    public Rule(String label) {
        this.label = label;
    }

    public Rule(String label, Conjunction<? extends Pattern> when, ThingStatement<?> variable) {
        validate(label, when, variable);
        this.label = label;
        this.when = when;
        this.then = variable;
    }

    @Override
    public boolean isRule() {
        return true;
    }

    @Override
    public Rule asRule() {
        return this;
    }

    public String label() {
        return label;
    }

    public Conjunction<? extends Pattern> when() {
        return when;
    }

    public ThingStatement<?> then() {
        return then;
    }

    public IncompleteRule when(Conjunction<? extends Pattern> when) {
        return new IncompleteRule(label, when);
    }

    public static void validate(String label, Conjunction<? extends Pattern> when, ThingStatement<?> then) {
        validateWhen(label, when);
        validateThen(label, when, then);
    }

    private static void validateWhen(String label, Conjunction<? extends Pattern> when) {
        if (when == null) throw new NullPointerException("Null when pattern");
        if (when.patterns().size() == 0) throw TypeQLException.of(INVALID_RULE_WHEN_MISSING_PATTERNS.message(label));
        if (findNegations(when).anyMatch(negation -> findNegations(negation.pattern()).findAny().isPresent())) {
            throw TypeQLException.of(INVALID_RULE_WHEN_NESTED_NEGATION.message(label));
        }
    }

    private static Stream<Negation<?>> findNegations(Pattern pattern) {
        if (pattern.isNegation()) return Stream.of(pattern.asNegation());
        if (pattern.isStatement()) return Stream.empty();
        return pattern.patterns().stream().flatMap(Rule::findNegations);
    }

    private static Stream<Disjunction<?>> findDisjunctions(Pattern pattern) {
        if (pattern.isDisjunction()) return Stream.of(pattern.asDisjunction());
        if (pattern.isStatement()) return Stream.empty();
        return pattern.patterns().stream().flatMap(Rule::findDisjunctions);
    }

    private static void validateThen(String label, @Nullable Conjunction<? extends Pattern> when, ThingStatement<?> then) {
        if (then == null) throw new NullPointerException("Null then pattern");
        int numConstraints = then.constraints().size();

        // rules must contain either 1 has constraint, or an isa and relation constrain
        if (!((numConstraints == 1 && then.has().size() == 1) || (numConstraints == 2 && then.relation().isPresent() && then.isa().isPresent()))) {
            throw TypeQLException.of(INVALID_RULE_THEN.message(label, then));
        }

        // rule 'has' cannot assign both an attribute type and a named concept variable
        if (then.has().size() == 1 && then.has().get(0).type().isPresent()) {
            ThingConstraint.Has has = then.has().get(0);
            if (has.attribute().isFirst() && has.attribute().first().isNamedConcept()) {
                throw TypeQLException.of(INVALID_RULE_THEN_HAS.message(label, then, has.type().get(), has.attribute().first()));
            } else if (has.attribute().isSecond() && has.attribute().second().headVariable().isNamedConcept()) {
                throw TypeQLException.of(INVALID_RULE_THEN_HAS.message(label, then, has.type().get(), has.attribute().second().headVariable().toString()));
            }
        }

        // all user-written variables in the 'then' must be present in the 'when', if it exists.
        if (when != null) {
            Set<Reference> thenReferences = then.variables().filter(TypeQLVariable::isNamed)
                    .map(TypeQLVariable::reference).collect(Collectors.toSet());

            Set<Reference> whenReferences = when.statements().flatMap(Statement::variables)
                    .filter(TypeQLVariable::isNamed).map(TypeQLVariable::reference).collect(Collectors.toSet());

            if (!whenReferences.containsAll(thenReferences)) {
                throw TypeQLException.of(INVALID_RULE_THEN_VARIABLES.message(label));
            }
        }

        // Roles must be explicit
        if (then.relation().isPresent() && !then.relation().get().players().stream()
                .map(player -> player.roleType().isPresent())
                .reduce(true, Boolean::logicalAnd)) {
            throw TypeQLException.of(INVALID_RULE_THEN_ROLES.message(label, then));
        }

        // relation variable name in 'then' must not be present
        if (then.relation().isPresent() && then.headVariable().reference().isName()) {
            throw TypeQLException.of(INVALID_RULE_THEN_RELATION_VARIABLE.message(label, then.headVariable().reference()));
        }
    }

    @Override
    public String toString() {
        return toString(true);
    }

    @Override
    public String toString(boolean pretty) {
        StringBuilder rule = new StringBuilder("" + RULE + SPACE + label);
        if (when != null) {
            if (pretty) {
                rule.append(COLON).append(SPACE).append(WHEN).append(SPACE);
                rule.append(when.toString(pretty))
                        .append(SPACE).append(THEN).append(SPACE).append(CURLY_OPEN).append(NEW_LINE)
                        .append(indent(then.toString(true))).append(SEMICOLON).append(NEW_LINE)
                        .append(CURLY_CLOSE);
            } else {
                rule.append(COLON).append(SPACE);
                String content = String.valueOf(WHEN) + SPACE + when.toString(pretty) + THEN + SPACE + CURLY_OPEN +
                        then.toString(true) + SEMICOLON + CURLY_CLOSE;
                rule.append(content);
            }
        }
        return rule.toString();
    }

    @Override
    public int hashCode() {
        if (hash == 0) {
            this.hash = Objects.hash(label, when, then);
        }
        return hash;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rule that = (Rule) o;
        return (this.label.equals(that.label) &&
                Objects.equals(this.when, that.when) &&
                Objects.equals(this.then, that.then));
    }

    public static class IncompleteRule {
        private final String label;
        private final Conjunction<? extends Pattern> when;

        public IncompleteRule(String label, Conjunction<? extends Pattern> when) {
            this.label = label;
            this.when = when;
        }

        public Rule then(ThingStatement<?> then) {
            return new Rule(label, when, then);
        }
    }
}
