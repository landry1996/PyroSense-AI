package com.pyrosense.shared.architecture;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

class SharedKernelArchitectureTest {

    private static com.tngtech.archunit.core.domain.JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.pyrosense.shared");
    }

    @Test
    void sharedKernelMustNotDependOnSpring() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.pyrosense.shared..")
                .should().dependOnClassesThat()
                .resideInAPackage("org.springframework..");

        rule.check(classes);
    }

    @Test
    void sharedKernelMustNotDependOnJakartaPersistence() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.pyrosense.shared..")
                .should().dependOnClassesThat()
                .resideInAPackage("jakarta.persistence..");

        rule.check(classes);
    }

    @Test
    void sharedKernelMustNotDependOnKafka() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.pyrosense.shared..")
                .should().dependOnClassesThat()
                .resideInAPackage("org.apache.kafka..");

        rule.check(classes);
    }

    @Test
    void domainClassesShouldNotDependOnApiLayer() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("com.pyrosense.shared.domain..")
                .should().dependOnClassesThat()
                .resideInAPackage("com.pyrosense.shared.api..");

        rule.check(classes);
    }

    @Test
    void valueObjectsShouldBeRecordsOrImplementMarker() {
        ArchRule rule = classes()
                .that().resideInAPackage("com.pyrosense.shared.id..")
                .should().implement(com.pyrosense.shared.domain.ValueObject.class);

        rule.check(classes);
    }

    @Test
    void exceptionsShouldExtendBusinessException() {
        ArchRule rule = classes()
                .that().resideInAPackage("com.pyrosense.shared.exception..")
                .and().areNotEnums()
                .and().areNotRecords()
                .and().doNotHaveSimpleName("BusinessException")
                .should().beAssignableTo(com.pyrosense.shared.exception.BusinessException.class);

        rule.check(classes);
    }
}
