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

package graql.lang.query.builder;

import graql.lang.Graql;
import graql.lang.exception.GraqlException;
import graql.lang.statement.Label;

import javax.annotation.CheckReturnValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface Computable {

    @CheckReturnValue
    Graql.Token.Compute.Method method();

    @CheckReturnValue
    Set<Graql.Token.Compute.Condition> conditionsRequired();

    @CheckReturnValue
    Optional<GraqlException> getException();

    interface Directional<T extends Computable.Directional> extends Computable {

        @CheckReturnValue
        String from();

        @CheckReturnValue
        String to();

        @CheckReturnValue
        T from(String fromID);

        @CheckReturnValue
        T to(String toID);
    }

    interface Targetable<T extends Computable.Targetable> extends Computable {

        @CheckReturnValue
        Set<Label> of();

        /**
         * Specify a list of unscoped types to use, as compute always uses unscoped targetable types
         */
        @CheckReturnValue
        default T of(String type, String... types) {
            ArrayList<Label> typeList = new ArrayList<>(types.length + 1);
            typeList.add(Label.of(type, null));
            for (String t : types) {
                typeList.add(Label.of(t, null));
            }

            return of(typeList);
        }
        @CheckReturnValue
        default T of(Label type, Label... types) {
            ArrayList<Label> typeList = new ArrayList<>(types.length + 1);
            typeList.add(type);
            typeList.addAll(Arrays.asList(types));

            return of(typeList);
        }

        @CheckReturnValue
        T of(Collection<Label> types);
    }

    interface Scopeable<T extends Computable.Scopeable> extends Computable {

        @CheckReturnValue
        Set<Label> in();

        @CheckReturnValue
        boolean includesAttributes();

        /**
         * Specify an unscoped list of types to use, as compute always uses unscoped types
         */
        @CheckReturnValue
        default T in(String type, String... types) {
            ArrayList<Label> typeList = new ArrayList<>(types.length + 1);
            typeList.add(Label.of(type, null));
            for (String t : types) {
                typeList.add(Label.of(t, null));
            }

            return in(typeList);
        }

        @CheckReturnValue
        default T in(Label type, Label... types) {
            ArrayList<Label> typeList = new ArrayList<>(types.length + 1);
            typeList.add(type);
            typeList.addAll(Arrays.asList(types));

            return in(typeList);
        }

        @CheckReturnValue
        T in(Collection<Label> types);

        @CheckReturnValue
        T attributes(boolean include);
    }

    interface Configurable<T extends Computable.Configurable,
            U extends Computable.Argument, V extends Computable.Arguments> extends Computable {

        @CheckReturnValue
        Graql.Token.Compute.Algorithm using();

        @CheckReturnValue
        V where();

        @CheckReturnValue
        T using(Graql.Token.Compute.Algorithm algorithm);

        @CheckReturnValue
        @SuppressWarnings("unchecked")
        default T where(U arg, U... args) {
            ArrayList<U> argList = new ArrayList<>(args.length + 1);
            argList.add(arg);
            argList.addAll(Arrays.asList(args));

            return where(argList);
        }

        @CheckReturnValue
        T where(List<U> args);

        @CheckReturnValue
        Set<Graql.Token.Compute.Algorithm> algorithmsAccepted();

        @CheckReturnValue
        Map<Graql.Token.Compute.Algorithm, Set<Graql.Token.Compute.Param>> argumentsAccepted();

        @CheckReturnValue
        Map<Graql.Token.Compute.Algorithm, Map<Graql.Token.Compute.Param, Object>> argumentsDefault();
    }

    interface Argument<T> {

        Graql.Token.Compute.Param type();

        T value();
    }

    interface Arguments {

        @CheckReturnValue
        Optional<Long> minK();

        Optional<Long> k();

        Optional<Long> size();

        Optional<String> contains();
    }
}
