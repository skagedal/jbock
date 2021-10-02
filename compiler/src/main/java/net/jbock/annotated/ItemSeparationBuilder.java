package net.jbock.annotated;

import java.util.List;
import java.util.stream.Stream;

final class ItemSeparationBuilder {

    private final Step3 step3;
    private final List<AnnotatedParameters> repeatablePositionalParameters;

    private ItemSeparationBuilder(
            Step3 step3,
            List<AnnotatedParameters> repeatablePositionalParameters) {
        this.step3 = step3;
        this.repeatablePositionalParameters = repeatablePositionalParameters;
    }

    static Step1 create(List<AnnotatedMethod> annotatedMethods) {
        return new Step1(annotatedMethods);
    }

    static final class Step1 {

        private final List<AnnotatedMethod> annotatedMethods;

        private Step1(List<AnnotatedMethod> annotatedMethods) {
            this.annotatedMethods = annotatedMethods;
        }

        Stream<AnnotatedMethod> annotatedMethods() {
            return annotatedMethods.stream();
        }

        Step2 withNamedOptions(List<AnnotatedOption> namedOptions) {
            return new Step2(this, namedOptions);
        }
    }

    static final class Step2 {

        private final Step1 step1;
        private final List<AnnotatedOption> namedOptions;

        private Step2(Step1 step1, List<AnnotatedOption> namedOptions) {
            this.step1 = step1;
            this.namedOptions = namedOptions;
        }

        Stream<AnnotatedMethod> annotatedMethods() {
            return step1.annotatedMethods.stream();
        }

        Step3 withPositionalParameters(List<AnnotatedParameter> positionalParameters) {
            return new Step3(this, positionalParameters);
        }
    }

    static final class Step3 {

        private final Step2 step2;
        private final List<AnnotatedParameter> positionalParameters;

        private Step3(Step2 step2, List<AnnotatedParameter> positionalParameters) {
            this.step2 = step2;
            this.positionalParameters = positionalParameters;
        }

        Stream<AnnotatedMethod> annotatedMethods() {
            return step2.step1.annotatedMethods.stream();
        }

        ItemSeparationBuilder withRepeatablePositionalParameters(List<AnnotatedParameters> repeatablePositionalParameters) {
            return new ItemSeparationBuilder(this, repeatablePositionalParameters);
        }
    }

    List<AnnotatedOption> namedOptions() {
        return step3.step2.namedOptions;
    }

    List<AnnotatedParameter> positionalParameters() {
        return step3.positionalParameters;
    }

    List<AnnotatedParameters> repeatablePositionalParameters() {
        return repeatablePositionalParameters;
    }
}