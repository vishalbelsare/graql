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

package graql.lang.query;

import graql.lang.Graql;
import graql.lang.exception.GraqlException;
import graql.lang.query.builder.Computable;
import graql.lang.statement.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckReturnValue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import static grakn.common.util.Collections.map;
import static grakn.common.util.Collections.set;
import static grakn.common.util.Collections.pair;
import static java.util.stream.Collectors.joining;


/**
 * Graql Compute Query: to perform distributed analytics OLAP computation on Grakn
 */
public abstract class GraqlCompute extends GraqlQuery implements Computable {
    private static Logger LOG = LoggerFactory.getLogger(GraqlCompute.class);


    private Graql.Token.Compute.Method method;
    boolean includeAttributes;

    // All these condition properties need to start off as NULL,
    // they will be initialised when the user provides input
    String fromID = null;
    String toID = null;
    Set<Label> ofTypes = null;
    Set<Label> inTypes = null;
    Graql.Token.Compute.Algorithm algorithm = null;
    Arguments arguments = null;
    // But 'arguments' will also be set when where() is called for cluster/centrality

    protected GraqlCompute(Graql.Token.Compute.Method method, boolean includeAttributes) {
        this.method = method;
        this.includeAttributes = includeAttributes;
    }

    @CheckReturnValue
    public final Graql.Token.Compute.Method method() {
        return method;
    }

    @CheckReturnValue
    public Set<Label> in() {
        if (this.inTypes == null) return set();
        return inTypes;
    }

    @CheckReturnValue
    public boolean includesAttributes() {
        return includeAttributes;
    }

    @CheckReturnValue
    public final boolean isValid() {
        return !getException().isPresent();
    }

    @Override
    public final String toString() {
        StringBuilder query = new StringBuilder();

        query.append(Graql.Token.Command.COMPUTE).append(Graql.Token.Char.SPACE).append(method);
        if (!printConditions().isEmpty()) query.append(Graql.Token.Char.SPACE).append(printConditions());
        query.append(Graql.Token.Char.SEMICOLON);

        return query.toString();
    }

    private String printConditions() {
        List<String> conditionsList = new ArrayList<>();

        // It is important that we check for whether each condition is NULL, rather than using the getters.
        // Because, we want to know the user provided conditions, rather than the default conditions from the getters.
        // The exception is for arguments. It needs to be set internally for the query object to have default argument
        // values. However, we can query for .getParameters() to get user provided argument parameters.
        if (fromID != null) conditionsList.add(str(Graql.Token.Compute.Condition.FROM, Graql.Token.Char.SPACE, fromID));
        if (toID != null) conditionsList.add(str(Graql.Token.Compute.Condition.TO, Graql.Token.Char.SPACE, toID));
        if (ofTypes != null) conditionsList.add(printOf());
        if (inTypes != null) conditionsList.add(printIn());
        if (algorithm != null) conditionsList.add(printAlgorithm());
        if (arguments != null && !arguments.getParameters().isEmpty()) conditionsList.add(printArguments());

        return conditionsList.stream().collect(joining(Graql.Token.Char.COMMA_SPACE.toString()));
    }

    private String printOf() {
        if (ofTypes != null) return str(Graql.Token.Compute.Condition.OF, Graql.Token.Char.SPACE, printTypes(ofTypes));

        return "";
    }

    private String printIn() {
        if (inTypes != null) return str(Graql.Token.Compute.Condition.IN, Graql.Token.Char.SPACE, printTypes(inTypes));

        return "";
    }

    private String printTypes(Set<Label> types) {
        StringBuilder inTypesString = new StringBuilder();

        if (!types.isEmpty()) {
            if (types.size() == 1) {
                inTypesString.append(types.iterator().next());
            } else {
                inTypesString.append(Graql.Token.Char.SQUARE_OPEN);
                inTypesString.append(inTypes.stream().map(Label::toString).collect(joining(Graql.Token.Char.COMMA_SPACE.toString())));
                inTypesString.append(Graql.Token.Char.SQUARE_CLOSE);
            }
        }

        return inTypesString.toString();
    }

    private String printAlgorithm() {
        if (algorithm != null) return str(Graql.Token.Compute.Condition.USING, Graql.Token.Char.SPACE, algorithm);

        return "";
    }

    private String printArguments() {
        if (arguments == null) return "";

        List<String> argumentsList = new ArrayList<>();
        StringBuilder argumentsString = new StringBuilder();

        for (Graql.Token.Compute.Param param : arguments.getParameters()) {
            argumentsList.add(str(param, Graql.Token.Comparator.EQ, arguments.getArgument(param).get()));
        }

        if (!argumentsList.isEmpty()) {
            argumentsString.append(str(Graql.Token.Compute.Condition.WHERE, Graql.Token.Char.SPACE));
            if (argumentsList.size() == 1) argumentsString.append(argumentsList.get(0));
            else {
                argumentsString.append(Graql.Token.Char.SQUARE_OPEN);
                argumentsString.append(argumentsList.stream().collect(joining(Graql.Token.Char.COMMA_SPACE.toString())));
                argumentsString.append(Graql.Token.Char.SQUARE_CLOSE);
            }
        }

        return argumentsString.toString();
    }

    private String str(Object... objects) {
        StringBuilder builder = new StringBuilder();
        for (Object obj : objects) builder.append(obj.toString());
        return builder.toString();
    }

    public static class Builder {

        public GraqlCompute.Statistics.Count count() {
            return new GraqlCompute.Statistics.Count();
        }

        public GraqlCompute.Statistics.Value max() {
            return new GraqlCompute.Statistics.Value(Graql.Token.Compute.Method.MAX);
        }

        public GraqlCompute.Statistics.Value min() {
            return new GraqlCompute.Statistics.Value(Graql.Token.Compute.Method.MIN);
        }

        public GraqlCompute.Statistics.Value mean() {
            return new GraqlCompute.Statistics.Value(Graql.Token.Compute.Method.MEAN);
        }

        public GraqlCompute.Statistics.Value median() {
            return new GraqlCompute.Statistics.Value(Graql.Token.Compute.Method.MEDIAN);
        }

        public GraqlCompute.Statistics.Value sum() {
            return new GraqlCompute.Statistics.Value(Graql.Token.Compute.Method.SUM);
        }

        public GraqlCompute.Statistics.Value std() {
            return new GraqlCompute.Statistics.Value(Graql.Token.Compute.Method.STD);
        }

        public GraqlCompute.Path path() {
            return new GraqlCompute.Path();
        }

        public GraqlCompute.Centrality centrality() {
            return new GraqlCompute.Centrality();
        }

        public GraqlCompute.Cluster cluster() {
            return new GraqlCompute.Cluster();
        }

    }

    public static abstract class Statistics extends GraqlCompute {

        Statistics(Graql.Token.Compute.Method method, boolean includeAttributes) {
            super(method, includeAttributes);
        }

        public GraqlCompute.Statistics.Count asCount() {
            if (this instanceof GraqlCompute.Statistics.Count) {
                return (GraqlCompute.Statistics.Count) this;
            } else {
                throw GraqlException.create("This is not a GraqlCompute.Statistics.Count query");
            }
        }

        public GraqlCompute.Statistics.Value asValue() {
            if (this instanceof GraqlCompute.Statistics.Value) {
                return (GraqlCompute.Statistics.Value) this;
            } else {
                throw GraqlException.create("This is not a GraqlCompute.Statistics.Value query");
            }
        }

        public static class Count extends GraqlCompute.Statistics
                implements Computable.Scopeable<GraqlCompute.Statistics.Count> {

            Count() {
                super(Graql.Token.Compute.Method.COUNT, true);
            }

            @Override
            public GraqlCompute.Statistics.Count in(Collection<Label> types) {
                this.inTypes = set(types);
                return this;
            }

            @Override
            public GraqlCompute.Statistics.Count attributes(boolean include) {
                LOG.warn("Attributes are always included in Compute Count queries unless scoped, ignoring");
                return this;
            }

            @Override
            public Set<Graql.Token.Compute.Condition> conditionsRequired() {
                return set();
            }

            @Override
            public Optional<GraqlException> getException() {
                return Optional.empty();
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                GraqlCompute.Statistics.Count that = (GraqlCompute.Statistics.Count) o;

                return (this.method().equals(that.method()) &&
                        this.in().equals(that.in()) &&
                        this.includesAttributes() == that.includesAttributes());
            }

            @Override
            public int hashCode() {
                int result = Objects.hashCode(method());
                result = 31 * result + Objects.hashCode(in());
                result = 31 * result + Objects.hashCode(includesAttributes());

                return result;
            }
        }

        public static class Value extends GraqlCompute.Statistics
                implements Computable.Targetable<Value>,
                           Computable.Scopeable<Value> {

            Value(Graql.Token.Compute.Method method) {
                super(method, true);
            }

            @CheckReturnValue
            public final Set<Label> of(){
                return ofTypes == null ? set() : ofTypes;
            }

            @Override
            public GraqlCompute.Statistics.Value of(Collection<Label> types) {
                this.ofTypes = set(types);
                return this;
            }

            @Override
            public GraqlCompute.Statistics.Value in(Collection<Label> types) {
                this.inTypes = set(types);
                return this;
            }

            @Override
            public GraqlCompute.Statistics.Value attributes(boolean include) {
                LOG.warn("Attributes are always included in Compute Statistics queries unless scoped, ignoring");
                return this;
            }

            @Override
            public Set<Graql.Token.Compute.Condition> conditionsRequired() {
                return set(Graql.Token.Compute.Condition.OF);
            }

            @Override
            public Optional<GraqlException> getException() {
                if (ofTypes == null) {
                    return Optional.of(GraqlException.invalidComputeQuery_missingCondition(
                            this.method(), conditionsRequired()
                    ));
                } else {
                    return Optional.empty();
                }
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                GraqlCompute.Statistics.Value that = (GraqlCompute.Statistics.Value) o;

                return (this.method().equals(that.method()) &&
                        this.of().equals(that.of()) &&
                        this.in().equals(that.in()) &&
                        this.includesAttributes() == that.includesAttributes());
            }

            @Override
            public int hashCode() {
                int result = Objects.hashCode(method());
                result = 31 * result + Objects.hashCode(of());
                result = 31 * result + Objects.hashCode(in());
                result = 31 * result + Objects.hashCode(includesAttributes());

                return result;
            }
        }
    }

    public static class Path extends GraqlCompute
            implements Computable.Directional<GraqlCompute.Path>,
                       Computable.Scopeable<GraqlCompute.Path> {

        Path(){
            super(Graql.Token.Compute.Method.PATH, false);
        }

        @CheckReturnValue
        public final String from() {
            return fromID;
        }

        @CheckReturnValue
        public final String to() {
            return toID;
        }

        @Override
        public GraqlCompute.Path from(String fromID) {
            this.fromID = fromID;
            return this;
        }

        @Override
        public GraqlCompute.Path to(String toID) {
            this.toID = toID;
            return this;
        }

        @Override
        public GraqlCompute.Path in(Collection<Label> types) {
            this.inTypes = set(types);
            return this;
        }

        @Override
        public GraqlCompute.Path attributes(boolean include) {
            this.includeAttributes = include;
            return this;
        }

        @Override
        public Set<Graql.Token.Compute.Condition> conditionsRequired() {
            return set(Graql.Token.Compute.Condition.FROM, Graql.Token.Compute.Condition.TO);
        }

        @Override
        public  Optional<GraqlException> getException() {
            if (fromID == null || toID == null) {
                return Optional.of(GraqlException.invalidComputeQuery_missingCondition(
                        this.method(), conditionsRequired()
                ));
            } else {
                return Optional.empty();
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            GraqlCompute.Path that = (GraqlCompute.Path) o;

            return (this.method().equals(that.method()) &&
                    this.from().equals(that.from()) &&
                    this.to().equals(that.to()) &&
                    this.in().equals(that.in()) &&
                    this.includesAttributes() == that.includesAttributes());
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(method());
            result = 31 * result + Objects.hashCode(from());
            result = 31 * result + Objects.hashCode(to());
            result = 31 * result + Objects.hashCode(in());
            result = 31 * result + Objects.hashCode(includesAttributes());

            return result;
        }
    }

    private static abstract class Configurable<T extends Configurable> extends GraqlCompute
            implements Computable.Scopeable<T>,
                       Computable.Configurable<T, GraqlCompute.Argument, GraqlCompute.Arguments> {

        Configurable(Graql.Token.Compute.Method method, boolean includeAttributes) {
            super(method, includeAttributes);
        }

        protected abstract T self();

        @CheckReturnValue
        public GraqlCompute.Arguments where() {
            GraqlCompute.Arguments args = arguments;
            if (args == null) {
                args = new GraqlCompute.Arguments();
            }
            if (argumentsDefault().containsKey(using())) {
                args.setDefaults(argumentsDefault().get(using()));
            }
            return args;
        }


        @CheckReturnValue
        public Graql.Token.Compute.Algorithm using() {
            if (algorithm == null) {
                return Graql.Token.Compute.Algorithm.DEGREE;
            } else {
                return algorithm;
            }
        }

        @Override
        public T in(Collection<Label> types) {
            this.inTypes = set(types);
            return self();
        }

        @Override
        public T attributes(boolean include) {
            this.includeAttributes = include;
            return self();
        }

        @Override
        public T using(Graql.Token.Compute.Algorithm algorithm) {
            this.algorithm = algorithm;
            return self();
        }

        @Override
        public T where(List<GraqlCompute.Argument> args) {
            if (this.arguments == null) this.arguments = new GraqlCompute.Arguments();
            for (GraqlCompute.Argument<?> arg : args) this.arguments.setArgument(arg);
            return self();
        }


        @Override
        public Set<Graql.Token.Compute.Condition> conditionsRequired() {
            return set(Graql.Token.Compute.Condition.USING);
        }

        @Override
        public Optional<GraqlException> getException() {
            if (!algorithmsAccepted().contains(using())) {
                return Optional.of(GraqlException.invalidComputeQuery_invalidMethodAlgorithm(method(), algorithmsAccepted()));
            }

            // Check that the provided arguments are accepted for the current query method and algorithm
            for (Graql.Token.Compute.Param param : this.where().getParameters()) {
                if (!argumentsAccepted().get(this.using()).contains(param)) {
                    return Optional.of(GraqlException.invalidComputeQuery_invalidArgument(
                            this.method(), this.using(), argumentsAccepted().get(this.using())
                    ));
                }
            }

            return Optional.empty();
        }
    }

    public static class Centrality extends GraqlCompute.Configurable<GraqlCompute.Centrality>
            implements Computable.Targetable<GraqlCompute.Centrality> {

        final static long DEFAULT_MIN_K = 2L;

        Centrality(){
            super(Graql.Token.Compute.Method.CENTRALITY, true);
        }

        protected GraqlCompute.Centrality self() {
            return this;
        }

        @CheckReturnValue
        public final Set<Label> of(){
            return ofTypes == null ? set() : ofTypes;
        }

        @Override
        public GraqlCompute.Centrality of(Collection<Label> types) {
            this.ofTypes = set(types);
            return this;
        }

        @Override
        public Set<Graql.Token.Compute.Algorithm> algorithmsAccepted() {
            return set(Graql.Token.Compute.Algorithm.DEGREE, Graql.Token.Compute.Algorithm.K_CORE);
        }

        @Override
        public Map<Graql.Token.Compute.Algorithm, Set<Graql.Token.Compute.Param>> argumentsAccepted() {
            return map(pair(Graql.Token.Compute.Algorithm.K_CORE, set(Graql.Token.Compute.Param.MIN_K)));
        }

        @Override
        public Map<Graql.Token.Compute.Algorithm, Map<Graql.Token.Compute.Param, Object>> argumentsDefault() {
            return map(pair(Graql.Token.Compute.Algorithm.K_CORE, map(pair(Graql.Token.Compute.Param.MIN_K, DEFAULT_MIN_K))));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            GraqlCompute.Centrality that = (GraqlCompute.Centrality) o;

            return (this.method().equals(that.method()) &&
                    this.of().equals(that.of()) &&
                    this.in().equals(that.in()) &&
                    this.using().equals(that.using()) &&
                    this.where().equals(that.where()) &&
                    this.includesAttributes() == that.includesAttributes());
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(method());
            result = 31 * result + Objects.hashCode(of());
            result = 31 * result + Objects.hashCode(in());
            result = 31 * result + Objects.hashCode(using());
            result = 31 * result + Objects.hashCode(where());
            result = 31 * result + Objects.hashCode(includesAttributes());

            return result;
        }
    }

    public static class Cluster extends GraqlCompute.Configurable<GraqlCompute.Cluster> {

        final static long DEFAULT_K = 2L;

        Cluster(){
            super(Graql.Token.Compute.Method.CLUSTER, false);
        }

        protected GraqlCompute.Cluster self() {
            return this;
        }

        @CheckReturnValue
        public Graql.Token.Compute.Algorithm using() {
            if (algorithm == null) {
                return Graql.Token.Compute.Algorithm.CONNECTED_COMPONENT;
            } else {
                return algorithm;
            }
        }

        @Override
        public Set<Graql.Token.Compute.Algorithm> algorithmsAccepted() {
            return set(Graql.Token.Compute.Algorithm.CONNECTED_COMPONENT, Graql.Token.Compute.Algorithm.K_CORE);
        }

        @Override
        public Map<Graql.Token.Compute.Algorithm, Set<Graql.Token.Compute.Param>> argumentsAccepted() {
            return map(pair(Graql.Token.Compute.Algorithm.K_CORE, set(Graql.Token.Compute.Param.K)),
                       pair(Graql.Token.Compute.Algorithm.CONNECTED_COMPONENT, set(Graql.Token.Compute.Param.SIZE, Graql.Token.Compute.Param.CONTAINS)));
        }

        @Override
        public Map<Graql.Token.Compute.Algorithm, Map<Graql.Token.Compute.Param, Object>> argumentsDefault() {
            return map(pair(Graql.Token.Compute.Algorithm.K_CORE, map(pair(Graql.Token.Compute.Param.K, DEFAULT_K))));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            GraqlCompute.Cluster that = (GraqlCompute.Cluster) o;

            return (this.method().equals(that.method()) &&
                    this.in().equals(that.in()) &&
                    this.using().equals(that.using()) &&
                    this.where().equals(that.where()) &&
                    this.includesAttributes() == that.includesAttributes());
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(method());
            result = 31 * result + Objects.hashCode(in());
            result = 31 * result + Objects.hashCode(using());
            result = 31 * result + Objects.hashCode(where());
            result = 31 * result + Objects.hashCode(includesAttributes());

            return result;
        }
    }

    /**
     * Graql Compute argument objects to be passed into the query
     *
     * @param <T>
     */
    public static class Argument<T> implements Computable.Argument<T> {

        private Graql.Token.Compute.Param param;
        private T value;

        private Argument(Graql.Token.Compute.Param param, T value) {
            this.param = param;
            this.value = value;
        }

        public final Graql.Token.Compute.Param type() {
            return this.param;
        }

        public final T value() {
            return this.value;
        }

        public static Argument<Long> minK(long minK) {
            return new Argument<>(Graql.Token.Compute.Param.MIN_K, minK);
        }

        public static Argument<Long> k(long k) {
            return new Argument<>(Graql.Token.Compute.Param.K, k);
        }

        public static Argument<Long> size(long size) {
            return new Argument<>(Graql.Token.Compute.Param.SIZE, size);
        }

        public static Argument<String> contains(String conceptId) {
            return new Argument<>(Graql.Token.Compute.Param.CONTAINS, conceptId);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Argument<?> that = (Argument<?>) o;

            return (this.type().equals(that.type()) &&
                    this.value().equals(that.value()));
        }

        @Override
        public int hashCode() {
            int result = param.hashCode();
            result = 31 * result + value.hashCode();

            return result;
        }
    }

    /**
     * Argument inner class to provide access Compute Query arguments
     */
    public class Arguments implements Computable.Arguments {

        private LinkedHashMap<Graql.Token.Compute.Param, Argument> argumentsOrdered = new LinkedHashMap<>();
        private Map<Graql.Token.Compute.Param, Object> defaults = new HashMap<>();

        private final Map<Graql.Token.Compute.Param, Supplier<Optional<?>>> argumentsMap = argumentsMap();

        private Map<Graql.Token.Compute.Param, Supplier<Optional<?>>> argumentsMap() {
            Map<Graql.Token.Compute.Param, Supplier<Optional<?>>> arguments = new HashMap<>();
            arguments.put(Graql.Token.Compute.Param.MIN_K, this::minK);
            arguments.put(Graql.Token.Compute.Param.K, this::k);
            arguments.put(Graql.Token.Compute.Param.SIZE, this::size);
            arguments.put(Graql.Token.Compute.Param.CONTAINS, this::contains);

            return arguments;
        }

        private void setArgument(Argument<?> arg) {
            argumentsOrdered.remove(arg.type());
            argumentsOrdered.put(arg.type(), arg);
        }

        private void setDefaults(Map<Graql.Token.Compute.Param, Object> defaults) {
            this.defaults = defaults;
        }

        @CheckReturnValue
        Optional<?> getArgument(Graql.Token.Compute.Param param) {
            return argumentsMap.get(param).get();
        }

        @CheckReturnValue
        public Set<Graql.Token.Compute.Param> getParameters() {
            return argumentsOrdered.keySet();
        }

        @CheckReturnValue
        @Override
        public Optional<Long> minK() {
            Long minK = (Long) getArgumentValue(Graql.Token.Compute.Param.MIN_K);
            if (minK != null) {
                return Optional.of(minK);

            } else if (defaults.containsKey(Graql.Token.Compute.Param.MIN_K)){
                return Optional.of((Long) defaults.get(Graql.Token.Compute.Param.MIN_K));

            } else {
                return Optional.empty();
            }
        }

        @CheckReturnValue
        @Override
        public Optional<Long> k() {
            Long minK = (Long) getArgumentValue(Graql.Token.Compute.Param.K);
            if (minK != null) {
                return Optional.of(minK);

            } else if (defaults.containsKey(Graql.Token.Compute.Param.K)){
                return Optional.of((Long) defaults.get(Graql.Token.Compute.Param.K));

            } else {
                return Optional.empty();
            }
        }

        @CheckReturnValue
        @Override
        public Optional<Long> size() {
            return Optional.ofNullable((Long) getArgumentValue(Graql.Token.Compute.Param.SIZE));
        }

        @CheckReturnValue
        @Override
        public Optional<String> contains() {
            return Optional.ofNullable((String) getArgumentValue(Graql.Token.Compute.Param.CONTAINS));
        }

        private Object getArgumentValue(Graql.Token.Compute.Param param) {
            return argumentsOrdered.get(param) != null ? argumentsOrdered.get(param).value() : null;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Arguments that = (Arguments) o;

            return (this.minK().equals(that.minK()) &&
                    this.k().equals(that.k()) &&
                    this.size().equals(that.size()) &&
                    this.contains().equals(that.contains()));
        }

        @Override
        public int hashCode() {
            int h = 1;
            h *= 1000003;
            h ^= this.argumentsOrdered.hashCode();

            return h;
        }
    }
}
