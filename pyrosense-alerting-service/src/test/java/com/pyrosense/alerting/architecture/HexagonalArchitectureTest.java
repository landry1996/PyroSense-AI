package com.pyrosense.alerting.architecture;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

class HexagonalArchitectureTest {

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
                .importPackages("com.pyrosense.alerting");
    }

    @Test
    void domainShouldNotDependOnSpring() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                .check(classes);
    }

    @Test
    void domainShouldNotDependOnAdapters() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..adapter..")
                .check(classes);
    }

    @Test
    void domainShouldNotDependOnApplication() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..application..")
                .check(classes);
    }

    @Test
    void applicationShouldNotDependOnAdapters() {
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAPackage("..adapter..")
                .check(classes);
    }

    @Test
    void applicationShouldNotDependOnConfig() {
        noClasses()
                .that().resideInAPackage("..application..")
                .should().dependOnClassesThat().resideInAPackage("..config..")
                .check(classes);
    }

    @Test
    void portInShouldBeInterfaces() {
        classes()
                .that().resideInAPackage("..application.port.in..")
                .and(ARE_TOP_LEVEL)
                .should().beInterfaces()
                .check(classes);
    }

    @Test
    void portOutShouldBeInterfaces() {
        classes()
                .that().resideInAPackage("..application.port.out..")
                .and(ARE_TOP_LEVEL)
                .should().beInterfaces()
                .check(classes);
    }

    @Test
    void layeredArchitectureShouldBeRespected() {
        layeredArchitecture()
                .consideringOnlyDependenciesInLayers()
                .layer("Domain").definedBy("..domain..")
                .layer("Application").definedBy("..application..")
                .layer("Adapter").definedBy("..adapter..")
                .layer("Config").definedBy("..config..")
                .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Adapter", "Config")
                .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapter", "Config")
                .whereLayer("Adapter").mayOnlyBeAccessedByLayers("Config")
                .check(classes);
    }

    @Test
    void domainEventsShouldImplementDomainEvent() {
        classes()
                .that().resideInAPackage("..domain.event..")
                .and(ARE_TOP_LEVEL)
                .should().implement(com.pyrosense.shared.domain.DomainEvent.class)
                .check(classes);
    }

    @Test
    void useCasesShouldNotHaveSpringAnnotations() {
        noClasses()
                .that().resideInAPackage("..application.usecase..")
                .should().beAnnotatedWith(org.springframework.stereotype.Service.class)
                .check(classes);
    }

    @Test
    void noFieldInjectionInApplicationLayer() {
        noFields().that().areDeclaredInClassesThat().resideInAPackage("..application..")
                .should().beAnnotatedWith(org.springframework.beans.factory.annotation.Autowired.class)
                .check(classes);
    }

    @Test
    void adaptersShouldNotDependOnDomain() {
        noClasses().that().resideInAPackage("..adapter..")
                .should().dependOnClassesThat().resideInAPackage("..application.usecase..")
                .check(classes);
    }
}
