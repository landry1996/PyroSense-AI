package com.pyrosense.shared.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Platform-wide architecture rules scoped to the shared-kernel classpath.
 * Cross-module rules (bounded context isolation, naming) are enforced per-service
 * in each service's HexagonalArchitectureTest where all classes are on the classpath.
 */
class PlatformArchitectureRulesTest {

    private static JavaClasses classes;

    private static final com.tngtech.archunit.base.DescribedPredicate<JavaClass> ARE_TOP_LEVEL =
            new com.tngtech.archunit.base.DescribedPredicate<>("are top level") {
                @Override
                public boolean test(JavaClass javaClass) {
                    return !javaClass.getName().contains("$");
                }
            };

    @BeforeAll
    static void setup() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.pyrosense.shared");
    }

    @Test
    void sharedKernelShouldNotDependOnSpringFramework() {
        noClasses().that().resideInAPackage("com.pyrosense.shared..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                .check(classes);
    }

    @Test
    void sharedKernelShouldNotDependOnJakartaPersistence() {
        noClasses().that().resideInAPackage("com.pyrosense.shared..")
                .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..")
                .check(classes);
    }

    @Test
    void sharedKernelShouldNotDependOnKafka() {
        noClasses().that().resideInAPackage("com.pyrosense.shared..")
                .should().dependOnClassesThat().resideInAPackage("org.apache.kafka..")
                .check(classes);
    }

    @Test
    void noCyclicDependenciesInSharedKernel() {
        slices().matching("com.pyrosense.shared.(*)..").should().beFreeOfCycles()
                .check(classes);
    }

    @Test
    void exceptionClassesShouldEndWithException() {
        classes().that().resideInAPackage("com.pyrosense.shared.exception..")
                .and(ARE_TOP_LEVEL)
                .and().areNotEnums()
                .should().haveSimpleNameEndingWith("Exception")
                .check(classes);
    }

    @Test
    void stronglyTypedIdsShouldImplementValueObject() {
        classes().that().resideInAPackage("com.pyrosense.shared.id..")
                .and(ARE_TOP_LEVEL)
                .should().implement(com.pyrosense.shared.domain.ValueObject.class)
                .check(classes);
    }

    @Test
    void domainEventInterfaceShouldBeInDomainPackage() {
        classes().that().haveSimpleName("DomainEvent")
                .should().resideInAPackage("com.pyrosense.shared.domain..")
                .check(classes);
    }
}
