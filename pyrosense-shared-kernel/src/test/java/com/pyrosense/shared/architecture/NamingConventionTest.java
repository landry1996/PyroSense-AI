package com.pyrosense.shared.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

/**
 * Naming convention rules for the shared-kernel module.
 * Per-service naming conventions (UseCase, Port, Adapter, etc.) are enforced
 * in each service's own HexagonalArchitectureTest where those classes exist.
 */
class NamingConventionTest {

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
    void exceptionClassesShouldEndWithException() {
        classes().that().resideInAPackage("com.pyrosense.shared.exception..")
                .and(ARE_TOP_LEVEL)
                .and().areNotEnums()
                .should().haveSimpleNameEndingWith("Exception")
                .check(classes);
    }

    @Test
    void stronglyTypedIdsShouldEndWithId() {
        classes().that().resideInAPackage("com.pyrosense.shared.id..")
                .and(ARE_TOP_LEVEL)
                .should().haveSimpleNameEndingWith("Id")
                .check(classes);
    }

    @Test
    void enumsShouldNotEndWithEnum() {
        classes().that().resideInAPackage("com.pyrosense.shared..")
                .and().areEnums()
                .and(ARE_TOP_LEVEL)
                .should().haveSimpleNameNotEndingWith("Enum")
                .check(classes);
    }

    @Test
    void valueObjectsShouldResideInValueObjectOrIdPackage() {
        classes().that().implement(com.pyrosense.shared.domain.ValueObject.class)
                .and(ARE_TOP_LEVEL)
                .should().resideInAnyPackage("..valueobject..", "..id..")
                .check(classes);
    }
}
