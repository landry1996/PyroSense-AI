package com.pyrosense.reporting.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.pyrosense.reporting", importOptions = ImportOption.DoNotIncludeTests.class)
class ReportingArchitectureTest {

    @ArchTest
    static final ArchRule domainShouldNotDependOnSpring = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework..");

    @ArchTest
    static final ArchRule domainShouldNotDependOnAdapters = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("..adapter..");

    @ArchTest
    static final ArchRule domainShouldNotDependOnConfig = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("..config..");

    @ArchTest
    static final ArchRule applicationShouldNotDependOnAdapters = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..adapter..");

    @ArchTest
    static final ArchRule useCasesShouldNotDependOnConfig = noClasses()
            .that().resideInAPackage("..application.usecase..")
            .should().dependOnClassesThat().resideInAPackage("..config..");

    @ArchTest
    static final ArchRule portInShouldBeInterfaces = classes()
            .that().resideInAPackage("..application.port.in..")
            .and().areTopLevelClasses()
            .should().beInterfaces();

    @ArchTest
    static final ArchRule portOutShouldBeInterfaces = classes()
            .that().resideInAPackage("..application.port.out..")
            .and().areTopLevelClasses()
            .should().beInterfaces();

    @ArchTest
    static final ArchRule controllersShouldResideInRestPackage = classes()
            .that().haveSimpleNameEndingWith("Controller")
            .should().resideInAPackage("..adapter.in.rest..");

    @ArchTest
    static final ArchRule domainShouldNotUseJPA = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("jakarta.persistence..");

    @ArchTest
    static final ArchRule domainShouldNotUseKafka = noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework.kafka..");
}
